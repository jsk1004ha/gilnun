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
    fun `five-step routes have one exact progress action and patch per step`() {
        expectedSteps.forEach { expected ->
            val service = ServiceCatalog.require(expected.serviceId)
            val checkpoint = service.requireCheckpoint(expected.checkpoint)
            val progressEvents = checkpoint.events.filter { it.effect == EventEffect.PROGRESS }

            assertEquals(1, progressEvents.size)
            assertEquals(expected.narration, checkpoint.narration)
            assertEquals(expected.stableKey, checkpoint.primaryAction?.stableKey)
            assertEquals(ServiceEventType.ACTION, checkpoint.primaryAction?.type)
            assertEquals(expected.role, checkpoint.primaryAction?.role)
            assertEquals(expected.accessibleName, checkpoint.primaryAction?.accessibleName)
            assertEquals(expected.nextCheckpoint, checkpoint.primaryAction?.expectedCheckpoint)
            assertEquals(
                PatchV1(
                    pageId = service.pageId,
                    compatibleRevision = service.revision,
                    stableKey = expected.stableKey,
                    role = expected.role,
                    accessibleName = expected.accessibleName,
                    expectedState = expected.nextCheckpoint,
                ),
                checkpoint.patch,
            )
        }

        ServiceCatalog.services.forEach { service ->
            assertEquals(5, service.steps.size)
            assertEquals(6, service.route.size)
            assertEquals(service.completionCheckpoint, service.route.last())
            assertTrue(service.completionCheckpoint.narration.any { it in '\uAC00'..'\uD7A3' })
            assertTrue(service.completionCheckpoint.events.isEmpty())
            assertNull(service.completionCheckpoint.primaryAction)
            assertNull(service.completionCheckpoint.patch)
        }
        assertEquals(15, ServiceCatalog.services.flatMap(ServiceContract::steps).mapNotNull(CheckpointContract::patch).size)
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
        val narration: String,
        val stableKey: String,
        val role: String,
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
                "pension-service",
                "기초연금 신청 연습을 찾아 선택해 주세요.",
                "pension-service-select",
                "button",
                "기초연금 신청 연습",
                "pension-applicant",
            ),
            ExpectedStep(
                ServiceId.BASIC_PENSION,
                "pension-applicant",
                "가상 신청자 정보를 확인해 주세요.",
                "pension-applicant-confirm",
                "button",
                "연습 사용자 정보 확인",
                "pension-method",
            ),
            ExpectedStep(
                ServiceId.BASIC_PENSION,
                "pension-method",
                "본인이 신청하는 연습 경로를 선택해 주세요.",
                "pension-self-apply",
                "radio",
                "본인이 신청해요",
                "pension-contact",
            ),
            ExpectedStep(
                ServiceId.BASIC_PENSION,
                "pension-contact",
                "가상 연락 방법을 확인해 주세요.",
                "pension-contact-confirm",
                "button",
                "연락 방법 확인",
                "pension-review",
            ),
            ExpectedStep(
                ServiceId.BASIC_PENSION,
                "pension-review",
                "가상 신청 내용을 마지막으로 확인해 주세요.",
                "pension-review-confirm",
                "button",
                "신청 내용 확인",
                "pension-complete",
            ),
            ExpectedStep(
                ServiceId.RESIDENT_RECORD,
                "resident-type",
                "주민등록표 등본 탭을 선택해 주세요.",
                "resident-copy-select",
                "tab",
                "주민등록표 등본",
                "resident-address",
            ),
            ExpectedStep(
                ServiceId.RESIDENT_RECORD,
                "resident-address",
                "가상 주소를 확인해 주세요.",
                "resident-address-confirm",
                "button",
                "주소 확인",
                "resident-issue-type",
            ),
            ExpectedStep(
                ServiceId.RESIDENT_RECORD,
                "resident-issue-type",
                "모의 발급 형태를 선택해 주세요.",
                "resident-standard-issue",
                "radio",
                "발급(모의)",
                "resident-delivery",
            ),
            ExpectedStep(
                ServiceId.RESIDENT_RECORD,
                "resident-delivery",
                "연습용 온라인 발급 방법을 선택해 주세요.",
                "resident-online-delivery",
                "combobox",
                "온라인발급(본인출력·연습용)",
                "resident-review",
            ),
            ExpectedStep(
                ServiceId.RESIDENT_RECORD,
                "resident-review",
                "민원 신청 연습을 마쳐 주세요.",
                "resident-finish-practice",
                "button",
                "민원 신청 연습 마치기",
                "resident-complete",
            ),
            ExpectedStep(
                ServiceId.HEALTH_SCREENING,
                "health-service",
                "건강검진 대상 조회 연습을 찾아 선택해 주세요.",
                "health-service-select",
                "button",
                "건강검진 대상 조회 연습",
                "health-person",
            ),
            ExpectedStep(
                ServiceId.HEALTH_SCREENING,
                "health-person",
                "가상 사용자 정보를 확인해 주세요.",
                "health-person-confirm",
                "button",
                "연습 사용자 정보 확인",
                "health-year",
            ),
            ExpectedStep(
                ServiceId.HEALTH_SCREENING,
                "health-year",
                "2026년 가상 조회 기준을 선택해 주세요.",
                "health-year-2026",
                "radio",
                "2026년(가상)",
                "health-kind",
            ),
            ExpectedStep(
                ServiceId.HEALTH_SCREENING,
                "health-kind",
                "일반건강검진 모의 항목을 선택해 주세요.",
                "health-general-screening",
                "radio",
                "일반건강검진(모의)",
                "health-query",
            ),
            ExpectedStep(
                ServiceId.HEALTH_SCREENING,
                "health-query",
                "가상 건강검진 대상 여부를 조회해 주세요.",
                "health-screening-query",
                "button",
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
