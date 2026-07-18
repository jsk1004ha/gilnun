package com.gilnun.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gilnun.app.catalog.ServiceCatalog
import com.gilnun.app.catalog.ServiceId
import com.gilnun.app.web.BridgeEventV2
import com.gilnun.app.web.PracticeLayout
import com.gilnun.app.web.WebCommand
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GuidanceJourneyInstrumentationTest {
    private lateinit var viewModelStore: ViewModelStore
    private lateinit var viewModel: GilnunViewModel

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        application
            .getSharedPreferences(STATE_PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        viewModelStore = ViewModelStore()
        viewModel =
            ViewModelProvider(
                viewModelStore,
                ViewModelProvider.AndroidViewModelFactory.getInstance(application),
            )[GilnunViewModel::class.java]
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
    }

    @Test
    fun basicPensionCompletesWithoutHelp() = completeWithoutHelp(ServiceId.BASIC_PENSION)

    @Test
    fun residentRecordCompletesWithoutHelp() = completeWithoutHelp(ServiceId.RESIDENT_RECORD)

    @Test
    fun healthScreeningCompletesWithoutHelp() = completeWithoutHelp(ServiceId.HEALTH_SCREENING)

    @Test
    fun automaticGuidanceHighlightsButWaitsForLearnerAction() {
        val service = ServiceCatalog.require(ServiceId.HEALTH_SCREENING)
        val first = service.steps.first()
        viewModel.selectService(service.id)

        viewModel.requestHelp()
        viewModel.chooseAutomaticGuidance()

        assertTrue(viewModel.uiState.value.guidanceShown)
        assertTrue(viewModel.uiState.value.webCommand is WebCommand.Highlight)
        assertEquals(first.id, viewModel.uiState.value.checkpoint)

        advance(service.id, first.id)

        assertEquals(first.primaryAction?.expectedCheckpoint, viewModel.uiState.value.checkpoint)
        assertEquals(GilnunViewModel.HUMAN_RECEIPT_MESSAGE, viewModel.uiState.value.receiptMessage)
    }

    @Test
    fun helperHandoffReturnsControlBeforeVerifiedProgress() {
        val service = ServiceCatalog.require(ServiceId.RESIDENT_RECORD)
        val first = service.steps.first()
        viewModel.selectService(service.id)

        viewModel.requestHelp()
        viewModel.chooseHelperHandoff()
        assertEquals(GilnunScreen.HELPER_CONFIRM, viewModel.uiState.value.screen)

        viewModel.confirmHelperTarget()
        assertEquals(GilnunScreen.HAND_BACK, viewModel.uiState.value.screen)

        viewModel.returnToLearner()
        assertEquals(GilnunScreen.PRACTICE, viewModel.uiState.value.screen)
        assertTrue(viewModel.uiState.value.guidanceShown)
        assertEquals(first.id, viewModel.uiState.value.checkpoint)

        advance(service.id, first.id)
        assertEquals(GilnunViewModel.HUMAN_RECEIPT_MESSAGE, viewModel.uiState.value.receiptMessage)
    }

    @Test
    fun layoutsAAndBKeepTheSameSemanticTarget() {
        val serviceId = ServiceId.BASIC_PENSION
        val patch = ServiceCatalog.builtInPatch(serviceId, "pension-applicant")

        viewModel.selectServiceForTest(serviceId, PracticeLayout.A)
        val commandA = viewModel.uiState.value.webCommand as WebCommand.Reset
        viewModel.selectServiceForTest(serviceId, PracticeLayout.B)
        val commandB = viewModel.uiState.value.webCommand as WebCommand.Reset

        assertEquals(PracticeLayout.A, commandA.layout)
        assertEquals(PracticeLayout.B, commandB.layout)
        assertNotNull(patch)
        assertEquals(patch, ServiceCatalog.builtInPatch(serviceId, "pension-applicant"))
    }

    @Test
    fun crossServiceEventStopsGuidanceWithoutProgress() {
        val pension = ServiceCatalog.require(ServiceId.BASIC_PENSION)
        val resident = ServiceCatalog.require(ServiceId.RESIDENT_RECORD)
        val pensionCheckpoint = pension.steps.first().id
        viewModel.selectService(pension.id)
        viewModel.requestHelp()
        viewModel.chooseAutomaticGuidance()

        viewModel.onBridgeEvent(actionEvent(resident.id, resident.steps.first().id))

        assertEquals(pensionCheckpoint, viewModel.uiState.value.checkpoint)
        assertFalse(viewModel.uiState.value.guidanceShown)
        assertTrue(viewModel.uiState.value.notice.orEmpty().contains("중단"))
    }

    @Test
    fun completionHelpNeverOpensAnEmptyHandoffScreen() {
        val serviceId = ServiceId.BASIC_PENSION
        completeWithoutHelp(serviceId)

        viewModel.requestHelp()
        viewModel.chooseHelperHandoff()

        assertEquals(GilnunScreen.PRACTICE, viewModel.uiState.value.screen)
        assertFalse(viewModel.uiState.value.helpPromptVisible)
        assertEquals(GilnunViewModel.COMPLETION_HELP_MESSAGE, viewModel.uiState.value.notice)
    }

    private fun completeWithoutHelp(serviceId: ServiceId) {
        val service = ServiceCatalog.require(serviceId)
        viewModel.selectService(serviceId)
        service.steps.forEach { step -> advance(serviceId, step.id) }

        assertEquals(service.completionCheckpoint.id, viewModel.uiState.value.checkpoint)
        assertFalse(viewModel.uiState.value.guidanceShown)
        assertEquals(null, viewModel.uiState.value.receiptMessage)
    }

    private fun advance(
        serviceId: ServiceId,
        checkpoint: String,
    ) {
        val service = ServiceCatalog.require(serviceId)
        val action = requireNotNull(service.requireCheckpoint(checkpoint).primaryAction)
        viewModel.onBridgeEvent(actionEvent(serviceId, checkpoint))
        viewModel.onBridgeEvent(
            BridgeEventV2.CheckpointChanged(
                schemaVersion = 2,
                serviceId = serviceId,
                revision = service.revision,
                checkpoint = action.expectedCheckpoint,
            ),
        )
    }

    private fun actionEvent(
        serviceId: ServiceId,
        checkpoint: String,
    ): BridgeEventV2.ActionOrHelp {
        val service = ServiceCatalog.require(serviceId)
        val action = requireNotNull(service.requireCheckpoint(checkpoint).primaryAction)
        return BridgeEventV2.ActionOrHelp(
            schemaVersion = 2,
            type = action.type,
            serviceId = serviceId,
            revision = service.revision,
            checkpoint = checkpoint,
            stableKey = action.stableKey,
            role = action.role,
            accessibleName = action.accessibleName,
            effect = action.effect,
        )
    }

    private companion object {
        const val STATE_PREFERENCES = "gilnun_demo_state_v1"
    }
}
