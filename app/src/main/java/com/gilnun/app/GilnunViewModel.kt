package com.gilnun.app

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import com.gilnun.app.data.ActionReceipt
import com.gilnun.app.data.DemoState
import com.gilnun.app.data.DemoStateStore
import com.gilnun.app.data.InteractionEvent
import com.gilnun.app.data.InteractionEventTypes
import com.gilnun.app.data.PatchV1
import com.gilnun.app.guidance.HelpPolicy
import com.gilnun.app.guidance.HelpPolicyEvent
import com.gilnun.app.guidance.PatchEngine
import com.gilnun.app.guidance.PatchResolution
import com.gilnun.app.guidance.ReceiptEvaluator
import com.gilnun.app.guidance.ReceiptObservation
import com.gilnun.app.guidance.SemanticTarget
import com.gilnun.app.guidance.StruggleCandidateSource
import com.gilnun.app.guidance.StruggleDetector
import com.gilnun.app.web.BridgeStatus
import com.gilnun.app.web.WebCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class DemoRole {
    LEARNER,
    HELPER,
}

enum class DemoLayout {
    A,
    B,
}

data class GilnunUiState(
    val role: DemoRole = DemoRole.LEARNER,
    val layout: DemoLayout = DemoLayout.A,
    val patch: PatchV1? = null,
    val helpLevel: Int = 3,
    val receipt: ActionReceipt? = null,
    val helpPromptVisible: Boolean = false,
    val helpPromptFromFriction: Boolean = false,
    val guidanceShown: Boolean = false,
    val checkpoint: String = GilnunViewModel.CHECKPOINT_CONSENT_READY,
    val bridgeAvailable: Boolean = false,
    val bridgeLabel: String = "연결 준비",
    val message: String = "학습자 · 레이아웃 A에서 시작합니다.",
    val webCommand: WebCommand? = null,
)

/**
 * A single deterministic reducer for the competition demo.
 *
 * No model, network service, accessibility service, or cross-application API participates in
 * this state machine. The user always performs the page action.
 */
class GilnunViewModel(application: Application) : AndroidViewModel(application) {
    private val store = DemoStateStore(application)
    private val detector = StruggleDetector()
    private val patchEngine = PatchEngine()
    private val helpPolicy = HelpPolicy()
    private val receiptEvaluator = ReceiptEvaluator()
    private var webClockAnchorAndroidMs = SystemClock.elapsedRealtime()
    private var pendingPromptMonotonicMs: Long? = null

    private val restored = store.load()
    private val _uiState =
        MutableStateFlow(
            GilnunUiState(
                patch = restored.patch,
                helpLevel = restored.helpLevel.coerceIn(0, 3),
                receipt = restored.lastReceipt,
                message =
                    if (restored.patch == null) {
                        "학습자 · 레이아웃 A에서 시작합니다."
                    } else {
                        "저장된 의미 패치를 불러왔습니다. 레이아웃 B에서 다시 안내할 수 있습니다."
                    },
            ),
        )
    val uiState: StateFlow<GilnunUiState> = _uiState.asStateFlow()

    fun onEvent(event: InteractionEvent) {
        if (event.pageId != PAGE_ID || event.compatibleRevision != REVISION) {
            _uiState.update {
                it.copy(
                    message = "지원하지 않는 화면 계약입니다. 안내를 안전하게 중단했습니다.",
                    guidanceShown = false,
                    webCommand = WebCommand.ClearHighlight(),
                )
            }
            return
        }

        when {
            event.type == InteractionEventTypes.HELP_REQUEST ->
                requestHelp(
                    direct = true,
                    eventMonotonicMs = event.monotonicMs,
                )
            event.type == InteractionEventTypes.TARGET_TAP &&
                event.stableKey == SAVE_DRAFT_KEY -> observeFriction(event)
            event.type == InteractionEventTypes.TARGET_TAP &&
                event.stableKey == REVIEW_NEXT_KEY -> observeReviewAction(event)
        }
    }

    fun requestHelp(
        direct: Boolean = true,
        eventMonotonicMs: Long? = null,
    ) {
        val state = _uiState.value
        pendingPromptMonotonicMs = eventMonotonicMs ?: estimatedWebMonotonicMs()
        val nextHelp =
            helpPolicy.transition(
                state.helpLevel,
                if (direct) {
                    HelpPolicyEvent.HELP_REQUESTED
                } else {
                    HelpPolicyEvent.STRUGGLE_CANDIDATE
                },
            )
        _uiState.update {
            it.copy(
                helpLevel = nextHelp,
                helpPromptVisible = true,
                helpPromptFromFriction = !direct,
                message =
                    if (direct) {
                        "직접 도움 요청을 받았습니다. 도움 여부를 먼저 확인합니다."
                    } else {
                        "같은 비진행 동작이 세 번 관찰됐습니다. 도움 여부를 먼저 확인합니다."
                    },
            )
        }
        persistMinimalState()
    }

    fun acceptHelp() {
        pendingPromptMonotonicMs = null
        _uiState.update {
            it.copy(
                role = DemoRole.HELPER,
                helpPromptVisible = false,
                guidanceShown = false,
                webCommand = WebCommand.ClearHighlight(),
                message = "도우미 모드입니다. ‘신청 내용 확인’ 한 지점을 선택해 주세요.",
            )
        }
    }

    fun declineHelp() {
        detector.rejectCandidate(pendingPromptMonotonicMs ?: estimatedWebMonotonicMs())
        pendingPromptMonotonicMs = null
        _uiState.update {
            it.copy(
                helpPromptVisible = false,
                message = "도움 제안을 닫았습니다. 30초 동안 자동으로 다시 묻지 않습니다.",
            )
        }
    }

    fun setRole(role: DemoRole) {
        pendingPromptMonotonicMs = null
        _uiState.update {
            it.copy(
                role = role,
                helpPromptVisible = false,
                guidanceShown = false,
                webCommand = WebCommand.ClearHighlight(),
                message =
                    if (role == DemoRole.HELPER) {
                        "도우미가 다음 한 지점의 의미만 기록합니다."
                    } else {
                        "학습자가 모든 화면 동작을 직접 수행합니다."
                    },
            )
        }
    }

    fun setLayout(layout: DemoLayout) {
        detector.onCheckpointChanged()
        _uiState.update {
            it.copy(
                layout = layout,
                checkpoint = CHECKPOINT_CONSENT_READY,
                guidanceShown = false,
                webCommand = WebCommand.Reset(layoutVariant = layout.name),
                message = "레이아웃 ${layout.name}로 바꿨습니다. 의미 계약은 그대로입니다.",
            )
        }
    }

    fun replayPatch() {
        val state = _uiState.value
        if (!state.bridgeAvailable) {
            failClosed("이 기기에서는 안전한 로컬 안내 브리지를 사용할 수 없습니다.")
            return
        }
        val patch = state.patch
        val resolution = patchEngine.resolve(patch, currentTargets())
        if (resolution != PatchResolution.RESOLVED || patch == null) {
            failClosed("안내를 안전하게 불러오지 못했습니다. 자동 실행은 하지 않습니다.")
            return
        }

        _uiState.update {
            it.copy(
                role = DemoRole.LEARNER,
                layout = DemoLayout.B,
                checkpoint = CHECKPOINT_CONSENT_READY,
                guidanceShown = true,
                webCommand = WebCommand.Highlight(patch),
                message = "레이아웃 B의 유일한 의미 일치를 강조했습니다. 사용자가 직접 눌러야 합니다.",
            )
        }
    }

    fun demonstrateMismatch() {
        val patch = _uiState.value.patch ?: PatchEngine.PRELOADED_REVIEW_PATCH
        val mismatchedTargets =
            currentTargets().map { target ->
                if (target.stableKey == REVIEW_NEXT_KEY) {
                    target.copy(compatibleRevision = "2026-06")
                } else {
                    target
                }
            }
        if (patchEngine.resolve(patch, mismatchedTargets) == PatchResolution.PATCH_UNAVAILABLE) {
            failClosed("리비전 불일치를 감지해 강조와 실행을 모두 중단했습니다.")
        }
    }

    fun resetDemo() {
        val current = _uiState.value
        detector.reset()
        pendingPromptMonotonicMs = null
        webClockAnchorAndroidMs = SystemClock.elapsedRealtime()
        store.clear()
        _uiState.value =
            GilnunUiState(
                bridgeAvailable = current.bridgeAvailable,
                bridgeLabel = current.bridgeLabel,
                webCommand = WebCommand.Reset(layoutVariant = DemoLayout.A.name),
                message = "Demo Reset 완료: 저장 상태와 화면 상태를 모두 지웠습니다.",
            )
    }

    fun onBridgeStatus(status: BridgeStatus) {
        _uiState.update { state ->
            when (status) {
                BridgeStatus.Available ->
                    state.copy(
                        bridgeAvailable = true,
                        bridgeLabel = "연결됨",
                    )

                BridgeStatus.PageReady -> {
                    webClockAnchorAndroidMs = SystemClock.elapsedRealtime()
                    state.copy(
                        bridgeLabel =
                            if (state.bridgeAvailable) {
                                "로컬 화면 준비"
                            } else {
                                "수동 화면 준비 · 도움 브리지 미지원"
                            },
                    )
                }

                BridgeStatus.Unavailable ->
                    state.copy(
                        bridgeAvailable = false,
                        bridgeLabel = "지원 안 됨",
                        guidanceShown = false,
                    )

                is BridgeStatus.Rejected ->
                    state.copy(bridgeLabel = "이벤트 거부 · ${status.error.name}")

                is BridgeStatus.PageFailed ->
                    state.copy(
                        bridgeAvailable = false,
                        bridgeLabel = "로컬 화면 오류 · ${status.code}",
                        guidanceShown = false,
                    )
            }
        }
    }

    fun onSecurityEvent(code: String) {
        failClosed("보안 경계가 요청을 차단했습니다: $code")
    }

    private fun observeFriction(event: InteractionEvent) {
        val candidate = detector.observe(event) ?: return
        requestHelp(
            direct = candidate.source == StruggleCandidateSource.DIRECT_HELP,
            eventMonotonicMs = candidate.monotonicMs,
        )
    }

    private fun estimatedWebMonotonicMs(): Long =
        (SystemClock.elapsedRealtime() - webClockAnchorAndroidMs).coerceAtLeast(0L)

    private fun observeReviewAction(event: InteractionEvent) {
        val state = _uiState.value
        if (state.role == DemoRole.HELPER) {
            val patch = PatchEngine.PRELOADED_REVIEW_PATCH
            _uiState.update {
                it.copy(
                    patch = patch,
                    message = "PatchV1을 저장했습니다. 좌표·선택자·입력값은 포함하지 않습니다.",
                    checkpoint = event.checkpoint,
                )
            }
            persistMinimalState()
            return
        }

        detector.onProgress()
        val receipt =
            receiptEvaluator.evaluate(
                patch = state.patch,
                observation =
                    ReceiptObservation(
                        guidanceShown = state.guidanceShown,
                        userActionObserved = true,
                        pageId = event.pageId,
                        compatibleRevision = event.compatibleRevision,
                        checkpoint = event.checkpoint,
                    ),
            )
        val nextHelp = helpPolicy.onReceipt(state.helpLevel, receipt)
        _uiState.update {
            it.copy(
                receipt = receipt,
                helpLevel = nextHelp,
                checkpoint = event.checkpoint,
                guidanceShown = false,
                webCommand = WebCommand.ClearHighlight(),
                message =
                    when (receipt.outcome.name) {
                        "VERIFIED" -> "사용자 행동과 review-ready 사후조건을 확인했습니다."
                        "UNVERIFIED" -> "클릭은 보였지만 사후조건이 없어 완료로 기록하지 않았습니다."
                        else -> "계약이 맞지 않아 실패 안전으로 종료했습니다."
                    },
            )
        }
        persistMinimalState()
    }

    private fun failClosed(message: String) {
        val state = _uiState.value
        val failedReceipt =
            ActionReceipt(
                guidanceShown = state.guidanceShown,
                userActionObserved = false,
                postconditionVerified = false,
                outcome = com.gilnun.app.data.ReceiptOutcome.FAILED,
            )
        _uiState.update {
            it.copy(
                receipt = failedReceipt,
                helpLevel = helpPolicy.onReceipt(it.helpLevel, failedReceipt),
                guidanceShown = false,
                webCommand = WebCommand.ClearHighlight(),
                message = message,
            )
        }
        persistMinimalState()
    }

    private fun persistMinimalState() {
        val state = _uiState.value
        store.save(
            DemoState(
                patch = state.patch,
                helpLevel = state.helpLevel,
                lastReceipt = state.receipt,
            ),
        )
    }

    private fun currentTargets(): List<SemanticTarget> =
        listOf(
            SemanticTarget(
                pageId = PAGE_ID,
                compatibleRevision = REVISION,
                stableKey = REVIEW_NEXT_KEY,
                role = "button",
                accessibleName = "신청 내용 확인",
                expectedState = CHECKPOINT_REVIEW_READY,
            ),
        )

    companion object {
        const val PAGE_ID = "welfare-basic-class"
        const val REVISION = "2026-07"
        const val SAVE_DRAFT_KEY = "save-draft"
        const val REVIEW_NEXT_KEY = "review-next"
        const val CHECKPOINT_CONSENT_READY = "consent-ready"
        const val CHECKPOINT_REVIEW_READY = "review-ready"
    }
}
