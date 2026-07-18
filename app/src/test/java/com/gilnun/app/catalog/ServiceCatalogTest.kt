package com.gilnun.app.catalog

import com.gilnun.app.data.PatchV1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceCatalogTest {
    @Test
    fun `catalog exposes exactly three persisted services with fixed pages and revision`() {
        assertEquals(
            listOf("basic-pension", "resident-record", "health-screening"),
            ServiceId.entries.map(ServiceId::persistedKey),
        )
        assertEquals(ServiceId.entries.toList(), ServiceCatalog.services.map(ServiceContract::id))
        assertEquals(
            listOf(
                "bokjiro-basic-pension",
                "gov24-resident-record",
                "nhis-health-screening",
            ),
            ServiceCatalog.services.map(ServiceContract::pageId),
        )
        assertTrue(ServiceCatalog.services.all { it.revision == "2026-07" })

        ServiceId.entries.forEach { id ->
            assertEquals(id, ServiceId.fromPersistedKey(id.persistedKey))
        }
        assertNull(ServiceId.fromPersistedKey("unknown"))
    }

    @Test
    fun `three-step routes have one exact progress action and patch per step`() {
        expectedSteps.forEach { expected ->
            val service = ServiceCatalog.require(expected.serviceId)
            val checkpoint = service.requireCheckpoint(expected.checkpoint)
            val progressEvents = checkpoint.events.filter { it.effect == EventEffect.PROGRESS }

            assertEquals(1, progressEvents.size)
            assertEquals(expected.stableKey, checkpoint.primaryAction?.stableKey)
            assertEquals(ServiceEventType.ACTION, checkpoint.primaryAction?.type)
            assertEquals("button", checkpoint.primaryAction?.role)
            assertEquals(expected.accessibleName, checkpoint.primaryAction?.accessibleName)
            assertEquals(expected.nextCheckpoint, checkpoint.primaryAction?.expectedCheckpoint)
            assertTrue(checkpoint.narration.any { it in '\uAC00'..'\uD7A3' })
            assertEquals(
                PatchV1(
                    pageId = service.pageId,
                    compatibleRevision = service.revision,
                    stableKey = expected.stableKey,
                    role = "button",
                    accessibleName = expected.accessibleName,
                    expectedState = expected.nextCheckpoint,
                ),
                checkpoint.patch,
            )
        }

        ServiceCatalog.services.forEach { service ->
            assertEquals(3, service.steps.size)
            assertEquals(4, service.route.size)
            assertEquals(service.completionCheckpoint, service.route.last())
            assertTrue(service.completionCheckpoint.narration.any { it in '\uAC00'..'\uD7A3' })
            assertTrue(service.completionCheckpoint.events.isEmpty())
            assertNull(service.completionCheckpoint.primaryAction)
            assertNull(service.completionCheckpoint.patch)
        }
        assertEquals(9, ServiceCatalog.services.flatMap(ServiceContract::steps).mapNotNull(CheckpointContract::patch).size)
    }

    @Test
    fun `service ids pages checkpoints and targets are globally unique`() {
        val services = ServiceCatalog.services
        val checkpoints = services.flatMap { service -> service.route.map(CheckpointContract::id) }
        val targets = services.flatMap { service -> service.route.flatMap(CheckpointContract::events) }

        assertEquals(services.size, services.map { it.id.persistedKey }.toSet().size)
        assertEquals(services.size, services.map(ServiceContract::pageId).toSet().size)
        assertEquals(checkpoints.size, checkpoints.toSet().size)
        assertEquals(targets.size, targets.map(EventContract::stableKey).toSet().size)
    }

    @Test
    fun `each service has one fixed non-progress friction event`() {
        expectedFriction.forEach { expected ->
            val service = ServiceCatalog.require(expected.serviceId)
            val frictionEvents =
                service.route
                    .flatMap(CheckpointContract::events)
                    .filter { it.effect == EventEffect.NON_PROGRESS }
            val friction = frictionEvents.single()

            assertEquals(expected.type, friction.type)
            assertEquals(expected.stableKey, friction.stableKey)
            assertEquals("button", friction.role)
            assertEquals(expected.accessibleName, friction.accessibleName)
            assertEquals(expected.checkpoint, friction.expectedCheckpoint)
            assertTrue(
                service.requireCheckpoint(expected.checkpoint).events.contains(friction),
            )
        }
    }

    @Test
    fun `catalog contains no submit or payment action`() {
        val prohibited = listOf("submit", "payment", "결제", "제출")
        val allEventText =
            ServiceCatalog.services
                .flatMap { service -> service.route.flatMap(CheckpointContract::events) }
                .joinToString("|") { event -> "${event.stableKey}|${event.accessibleName}" }
                .lowercase()

        prohibited.forEach { token ->
            assertFalse("Prohibited action token: $token", allEventText.contains(token))
        }
    }

    private data class ExpectedStep(
        val serviceId: ServiceId,
        val checkpoint: String,
        val stableKey: String,
        val accessibleName: String,
        val nextCheckpoint: String,
    )

    private data class ExpectedFriction(
        val serviceId: ServiceId,
        val checkpoint: String,
        val type: ServiceEventType,
        val stableKey: String,
        val accessibleName: String,
    )

    private val expectedSteps =
        listOf(
            ExpectedStep(
                ServiceId.BASIC_PENSION,
                "pension-applicant",
                "pension-applicant-confirm",
                "가상 신청자 정보 확인",
                "pension-method",
            ),
            ExpectedStep(
                ServiceId.BASIC_PENSION,
                "pension-method",
                "pension-self-apply",
                "본인이 신청해요",
                "pension-review",
            ),
            ExpectedStep(
                ServiceId.BASIC_PENSION,
                "pension-review",
                "pension-review-confirm",
                "신청 내용 확인",
                "pension-complete",
            ),
            ExpectedStep(
                ServiceId.RESIDENT_RECORD,
                "resident-type",
                "resident-copy-select",
                "주민등록표 등본",
                "resident-delivery",
            ),
            ExpectedStep(
                ServiceId.RESIDENT_RECORD,
                "resident-delivery",
                "resident-online-delivery",
                "온라인 발급(연습용)",
                "resident-review",
            ),
            ExpectedStep(
                ServiceId.RESIDENT_RECORD,
                "resident-review",
                "resident-preview",
                "모의 등본 미리보기",
                "resident-complete",
            ),
            ExpectedStep(
                ServiceId.HEALTH_SCREENING,
                "health-person",
                "health-person-confirm",
                "가상 사용자 정보 확인",
                "health-year",
            ),
            ExpectedStep(
                ServiceId.HEALTH_SCREENING,
                "health-year",
                "health-year-2026",
                "2026년 조회 기준 확인",
                "health-query",
            ),
            ExpectedStep(
                ServiceId.HEALTH_SCREENING,
                "health-query",
                "health-screening-query",
                "건강검진 대상 조회",
                "health-complete",
            ),
        )

    private val expectedFriction =
        listOf(
            ExpectedFriction(
                ServiceId.BASIC_PENSION,
                "pension-review",
                ServiceEventType.ACTION,
                "pension-save-draft",
                "임시 저장",
            ),
            ExpectedFriction(
                ServiceId.RESIDENT_RECORD,
                "resident-delivery",
                ServiceEventType.HELP,
                "resident-delivery-help",
                "수령 방법 안내",
            ),
            ExpectedFriction(
                ServiceId.HEALTH_SCREENING,
                "health-query",
                ServiceEventType.HELP,
                "health-schedule-help",
                "검진 일정 안내",
            ),
        )
}
