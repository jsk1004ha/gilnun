package com.gilnun.app

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import com.gilnun.app.catalog.EventEffect
import com.gilnun.app.catalog.ServiceCatalog
import com.gilnun.app.catalog.ServiceId
import com.gilnun.app.data.ActionReceipt
import com.gilnun.app.data.DemoState
import com.gilnun.app.data.DemoStateStore
import com.gilnun.app.data.GuidanceSource
import com.gilnun.app.data.PatchV1
import com.gilnun.app.data.ReceiptOutcome
import com.gilnun.app.data.ServiceProgress
import com.gilnun.app.guidance.AndroidKoreanSpeechEngine
import com.gilnun.app.guidance.GuidanceReceiptCoordinator
import com.gilnun.app.guidance.GuidanceSpeechCoordinator
import com.gilnun.app.guidance.HelpPolicy
import com.gilnun.app.guidance.NonProgressObservation
import com.gilnun.app.guidance.PatchEngine
import com.gilnun.app.guidance.PatchResolution
import com.gilnun.app.guidance.ReceiptTransition
import com.gilnun.app.guidance.SemanticTarget
import com.gilnun.app.guidance.SpeechRequestResult
import com.gilnun.app.guidance.StruggleDetector
import com.gilnun.app.web.BridgeEventV2
import com.gilnun.app.web.BridgeStatus
import com.gilnun.app.web.PracticeLayout
import com.gilnun.app.web.WebCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class GilnunScreen {
    HOME,
    PRACTICE,
    HELPER_CONFIRM,
    HAND_BACK,
}

data class GilnunUiState(
    val screen: GilnunScreen = GilnunScreen.HOME,
    val selectedService: ServiceId? = null,
    val layout: PracticeLayout = PracticeLayout.A,
    val checkpoint: String? = null,
    val helpPromptVisible: Boolean = false,
    val helpPromptFromFriction: Boolean = false,
    val guidanceShown: Boolean = false,
    val webCommand: WebCommand? = null,
    val notice: String? = null,
    val receiptMessage: String? = null,
    val speechUnavailable: Boolean = false,
)

/**
 * Volatile journey reducer over the fixed catalog. Only helper patches, help level, and truthful
 * final receipts are durable; every process start returns to HOME.
 */
class GilnunViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val store = DemoStateStore(application)
    private var durableState = store.load()
    private val detector = StruggleDetector()
    private val patchEngine = PatchEngine()
    private val helpPolicy = HelpPolicy()
    private val receiptCoordinator = GuidanceReceiptCoordinator()
    private val speechCoordinator =
        GuidanceSpeechCoordinator(AndroidKoreanSpeechEngine(application))

    private val _uiState = MutableStateFlow(GilnunUiState())
    val uiState: StateFlow<GilnunUiState> = _uiState.asStateFlow()

    fun selectService(
        serviceId: ServiceId,
        layout: PracticeLayout = PracticeLayout.A,
    ) {
        val service = ServiceCatalog.require(serviceId)
        resetVolatileGuidance()
        _uiState.value =
            GilnunUiState(
                screen = GilnunScreen.PRACTICE,
                selectedService = serviceId,
                layout = layout,
                checkpoint = service.steps.first().id,
                webCommand = WebCommand.Reset(serviceId, layout),
            )
    }

    /** Instrumentation seam retained for explicit layout contract tests. */
    fun selectServiceForTest(
        serviceId: ServiceId,
        layout: PracticeLayout,
    ) {
        selectService(serviceId, layout)
    }

    fun togglePracticeLayout() {
        val context = currentContext() ?: return
        val nextLayout =
            if (_uiState.value.layout == PracticeLayout.A) {
                PracticeLayout.B
            } else {
                PracticeLayout.A
            }
        resetVolatileGuidance()
        _uiState.update {
            it.copy(
                layout = nextLayout,
                checkpoint = context.service.steps.first().id,
                helpPromptVisible = false,
                helpPromptFromFriction = false,
                guidanceShown = false,
                webCommand = WebCommand.Reset(context.serviceId, nextLayout),
                notice = "화면 위치가 바뀌었어요. 길눈은 같은 의미의 버튼을 다시 찾을 수 있어요.",
                receiptMessage = null,
                speechUnavailable = false,
            )
        }
    }

    fun goHome() {
        resetVolatileGuidance()
        _uiState.value = GilnunUiState()
    }

    fun requestHelp() {
        val context = currentContext() ?: return
        if (context.service.requireCheckpoint(context.checkpoint).primaryAction == null) {
            _uiState.update {
                it.copy(
                    helpPromptVisible = false,
                    notice = COMPLETION_HELP_MESSAGE,
                )
            }
            return
        }
        detector.directHelp(
            serviceId = context.serviceId,
            revision = context.service.revision,
            checkpoint = context.checkpoint,
            atMonotonicMs = nowMs(),
        )
        updateHelpLevel(context.serviceId, direct = true)
        _uiState.update {
            it.copy(
                helpPromptVisible = true,
                helpPromptFromFriction = false,
                notice = null,
            )
        }
    }

    fun chooseAutomaticGuidance() {
        val context = currentContext() ?: return
        val patch = ServiceCatalog.builtInPatch(context.serviceId, context.checkpoint)
        if (!beginGuidance(context, patch, GuidanceSource.PREVERIFIED)) {
            stopGuidanceWithNotice()
            return
        }
        _uiState.update {
            it.copy(
                helpPromptVisible = false,
                guidanceShown = true,
                webCommand = WebCommand.Highlight(checkNotNull(patch)),
                notice = "노란 테두리를 확인한 뒤 직접 선택해 주세요.",
            )
        }
    }

    fun chooseHelperHandoff() {
        val context = currentContext() ?: return
        if (ServiceCatalog.builtInPatch(context.serviceId, context.checkpoint) == null) {
            _uiState.update {
                it.copy(
                    helpPromptVisible = false,
                    notice = COMPLETION_HELP_MESSAGE,
                )
            }
            return
        }
        receiptCoordinator.clear()
        speechCoordinator.stop()
        _uiState.update {
            it.copy(
                screen = GilnunScreen.HELPER_CONFIRM,
                helpPromptVisible = false,
                guidanceShown = false,
                webCommand = WebCommand.ClearHighlight(),
                notice = null,
            )
        }
    }

    fun confirmHelperTarget() {
        val context = currentContext(allowHelperScreen = true) ?: return
        val patch = ServiceCatalog.builtInPatch(context.serviceId, context.checkpoint) ?: return
        updateProgress(context.serviceId) { progress ->
            progress.copy(
                helperPatchesByCheckpoint =
                    progress.helperPatchesByCheckpoint + (context.checkpoint to patch),
            )
        }
        _uiState.update {
            it.copy(
                screen = GilnunScreen.HAND_BACK,
                notice = null,
            )
        }
    }

    fun returnToLearner() {
        val context = currentContext(allowHelperScreen = true) ?: return
        val storedPatch =
            durableState
                .services
                .getValue(context.serviceId)
                .helperPatchesByCheckpoint[context.checkpoint]
        if (!beginGuidance(context, storedPatch, GuidanceSource.SAME_DEVICE_HELPER)) {
            stopGuidanceWithNotice()
            return
        }
        _uiState.update {
            it.copy(
                screen = GilnunScreen.PRACTICE,
                guidanceShown = true,
                webCommand = WebCommand.Highlight(checkNotNull(storedPatch)),
                notice = "도우미가 확인한 곳을 표시했어요. 어르신이 직접 선택해 주세요.",
            )
        }
    }

    fun cancelHelperHandoff() {
        receiptCoordinator.clear()
        _uiState.update {
            it.copy(
                screen = GilnunScreen.PRACTICE,
                guidanceShown = false,
                webCommand = WebCommand.ClearHighlight(),
            )
        }
    }

    fun declineHelp() {
        detector.rejectCandidate(nowMs())
        _uiState.update {
            it.copy(
                helpPromptVisible = false,
                notice = "지금은 도움 안내를 닫았어요.",
            )
        }
    }

    fun readGuidance() {
        val context = currentContext() ?: return
        when (speechCoordinator.read(context.serviceId, context.checkpoint)) {
            SpeechRequestResult.STARTED,
            SpeechRequestResult.DUPLICATE_SUPPRESSED,
            -> _uiState.update { it.copy(speechUnavailable = false) }

            SpeechRequestResult.UNAVAILABLE ->
                _uiState.update {
                    it.copy(
                        speechUnavailable = true,
                        notice = TTS_UNAVAILABLE_MESSAGE,
                    )
                }
        }
    }

    fun stopNarration() {
        speechCoordinator.stop()
    }

    fun onBridgeEvent(event: BridgeEventV2) {
        val context = currentContext() ?: return
        if (event.serviceId != context.serviceId ||
            event.revision != context.service.revision
        ) {
            rejectGuidedContext()
            return
        }

        when (event) {
            is BridgeEventV2.ActionOrHelp -> onActionOrHelp(context, event)
            is BridgeEventV2.CheckpointChanged -> onCheckpointChanged(context, event)
        }
    }

    fun onBridgeStatus(status: BridgeStatus) {
        when (status) {
            BridgeStatus.Available,
            BridgeStatus.PageReady,
            -> Unit

            BridgeStatus.Unavailable ->
                _uiState.update {
                    it.copy(notice = "이 기기에서는 자동 안내 표시를 사용할 수 없어요.")
                }

            is BridgeStatus.Rejected -> rejectGuidedContext()
            is BridgeStatus.PageFailed ->
                _uiState.update {
                    it.copy(notice = "연습 화면을 불러오지 못했어요. 홈에서 다시 시작해 주세요.")
                }
        }
    }

    fun onSecurityEvent(code: String) {
        @Suppress("UNUSED_VARIABLE")
        val ignored = code
        rejectGuidedContext()
    }

    private fun onActionOrHelp(
        context: CurrentContext,
        event: BridgeEventV2.ActionOrHelp,
    ) {
        if (event.checkpoint != context.checkpoint) {
            rejectGuidedContext()
            return
        }
        if (event.effect == EventEffect.NON_PROGRESS) {
            val candidate =
                detector.observeNonProgress(
                    NonProgressObservation(
                        serviceId = event.serviceId,
                        revision = event.revision,
                        stableKey = event.stableKey,
                        checkpoint = event.checkpoint,
                        monotonicMs = nowMs(),
                    ),
                )
            if (candidate != null) {
                updateHelpLevel(context.serviceId, direct = false)
                _uiState.update {
                    it.copy(
                        helpPromptVisible = true,
                        helpPromptFromFriction = true,
                        notice = null,
                    )
                }
            }
            return
        }

        detector.onProgress()
        if (!_uiState.value.guidanceShown) return
        when (val transition = receiptCoordinator.onAction(event)) {
            is ReceiptTransition.Pending -> Unit
            is ReceiptTransition.Rejected -> finishFailedGuidance(context.serviceId, transition.receipt)
            ReceiptTransition.NoGuidance -> rejectGuidedContext()
            is ReceiptTransition.Verified -> rejectGuidedContext()
        }
    }

    private fun onCheckpointChanged(
        context: CurrentContext,
        event: BridgeEventV2.CheckpointChanged,
    ) {
        if (event.checkpoint == context.checkpoint) return
        val expected =
            context.service
                .requireCheckpoint(context.checkpoint)
                .primaryAction
                ?.expectedCheckpoint
        if (event.checkpoint != expected) {
            rejectGuidedContext()
            return
        }

        val receipt =
            if (_uiState.value.guidanceShown) {
                when (val transition = receiptCoordinator.onCheckpointChanged(event)) {
                    is ReceiptTransition.Verified -> transition.receipt
                    is ReceiptTransition.Rejected -> transition.receipt
                    is ReceiptTransition.Pending,
                    ReceiptTransition.NoGuidance,
                    -> failedReceipt()
                }
            } else {
                null
            }
        detector.onCheckpointChanged()
        speechCoordinator.stop()

        if (receipt != null) {
            updateProgress(context.serviceId) { progress ->
                progress.copy(
                    helpLevel = helpPolicy.onReceipt(progress.helpLevel, receipt),
                    lastReceipt = receipt,
                )
            }
        }
        _uiState.update {
            it.copy(
                checkpoint = event.checkpoint,
                guidanceShown = false,
                webCommand = WebCommand.ClearHighlight(),
                receiptMessage =
                    if (receipt?.outcome == ReceiptOutcome.VERIFIED) {
                        HUMAN_RECEIPT_MESSAGE
                    } else {
                        null
                    },
                notice =
                    if (receipt?.outcome == ReceiptOutcome.FAILED) {
                        "다음 화면은 열렸지만 안내 확인 기록은 남기지 않았어요."
                    } else {
                        null
                    },
                speechUnavailable = false,
            )
        }
    }

    private fun beginGuidance(
        context: CurrentContext,
        patch: PatchV1?,
        source: GuidanceSource,
    ): Boolean {
        val target =
            patch?.let {
                SemanticTarget(
                    pageId = it.pageId,
                    compatibleRevision = it.compatibleRevision,
                    stableKey = it.stableKey,
                    role = it.role,
                    accessibleName = it.accessibleName,
                    expectedState = it.expectedState,
                )
            }
        return patch != null &&
            target != null &&
            patchEngine.resolve(patch, listOf(target)) == PatchResolution.RESOLVED &&
            receiptCoordinator.begin(
                context.serviceId,
                context.checkpoint,
                patch,
                source,
            )
    }

    private fun rejectGuidedContext() {
        val context = currentContext(allowHelperScreen = true)
        val transition = receiptCoordinator.onTimeout()
        if (context != null && transition is ReceiptTransition.Rejected) {
            finishFailedGuidance(context.serviceId, transition.receipt)
        } else {
            stopGuidanceWithNotice()
        }
    }

    private fun finishFailedGuidance(
        serviceId: ServiceId,
        receipt: ActionReceipt,
    ) {
        updateProgress(serviceId) { progress ->
            progress.copy(
                helpLevel = helpPolicy.onReceipt(progress.helpLevel, receipt),
                lastReceipt = receipt,
            )
        }
        receiptCoordinator.clear()
        _uiState.update {
            it.copy(
                guidanceShown = false,
                webCommand = WebCommand.ClearHighlight(),
                notice = "안내를 안전하게 중단했어요. 현재 화면에서 다시 도움을 요청해 주세요.",
                receiptMessage = null,
            )
        }
    }

    private fun stopGuidanceWithNotice() {
        receiptCoordinator.clear()
        _uiState.update {
            it.copy(
                guidanceShown = false,
                helpPromptVisible = false,
                webCommand = WebCommand.ClearHighlight(),
                notice = "안내 대상을 확인하지 못해 표시를 중단했어요.",
            )
        }
    }

    private fun resetVolatileGuidance() {
        detector.reset()
        receiptCoordinator.clear()
        speechCoordinator.stop()
    }

    private fun updateHelpLevel(
        serviceId: ServiceId,
        direct: Boolean,
    ) {
        updateProgress(serviceId) { progress ->
            progress.copy(
                helpLevel =
                    if (direct) {
                        helpPolicy.onHelpRequested(progress.helpLevel)
                    } else {
                        helpPolicy.onStruggleCandidate(progress.helpLevel)
                    },
            )
        }
    }

    private fun updateProgress(
        serviceId: ServiceId,
        transform: (ServiceProgress) -> ServiceProgress,
    ) {
        val services = durableState.services.toMutableMap()
        services[serviceId] = transform(services.getValue(serviceId))
        durableState = DemoState(services)
        store.save(durableState)
    }

    private fun currentContext(allowHelperScreen: Boolean = false): CurrentContext? {
        val state = _uiState.value
        val allowedScreen =
            state.screen == GilnunScreen.PRACTICE ||
                (allowHelperScreen &&
                    state.screen in setOf(GilnunScreen.HELPER_CONFIRM, GilnunScreen.HAND_BACK))
        if (!allowedScreen) return null
        val serviceId = state.selectedService ?: return null
        val checkpoint = state.checkpoint ?: return null
        val service = ServiceCatalog.find(serviceId) ?: return null
        if (service.checkpoint(checkpoint) == null) return null
        return CurrentContext(serviceId, service, checkpoint)
    }

    private fun failedReceipt(): ActionReceipt =
        ActionReceipt(
            guidanceShown = true,
            userActionObserved = false,
            postconditionVerified = false,
            outcome = ReceiptOutcome.FAILED,
            source = GuidanceSource.PREVERIFIED,
        )

    private fun nowMs(): Long = SystemClock.elapsedRealtime()

    override fun onCleared() {
        speechCoordinator.shutdown()
        super.onCleared()
    }

    private data class CurrentContext(
        val serviceId: ServiceId,
        val service: com.gilnun.app.catalog.ServiceContract,
        val checkpoint: String,
    )

    companion object {
        const val HUMAN_RECEIPT_MESSAGE =
            "예전 위치가 아니라, 이름과 역할을 확인해 찾았어요 · 안내 표시 → 직접 선택 → 다음 화면 확인"
        const val TTS_UNAVAILABLE_MESSAGE = "이 기기에서는 안내 읽기를 사용할 수 없어요"
        const val COMPLETION_HELP_MESSAGE = "연습을 모두 마쳤어요. 홈으로 돌아가 다른 연습을 시작할 수 있어요."
    }
}
