package com.gilnun.app.catalog

import com.gilnun.app.data.PatchV1

enum class ServiceId(
    val persistedKey: String,
) {
    BASIC_PENSION("basic-pension"),
    RESIDENT_RECORD("resident-record"),
    HEALTH_SCREENING("health-screening"),
    ;

    companion object {
        private val byPersistedKey = entries.associateBy(ServiceId::persistedKey)

        fun fromPersistedKey(value: String?): ServiceId? = byPersistedKey[value]
    }
}

enum class ServiceEventType {
    ACTION,
    HELP,
}

enum class EventEffect {
    PROGRESS,
    NON_PROGRESS,
}

data class EventContract(
    val type: ServiceEventType,
    val stableKey: String,
    val role: String,
    val accessibleName: String,
    val effect: EventEffect,
    val expectedCheckpoint: String,
)

data class CheckpointContract(
    val id: String,
    val narration: String,
    val primaryAction: EventContract? = null,
    val frictionEvent: EventContract? = null,
    val patch: PatchV1? = null,
) {
    val events: List<EventContract>
        get() = listOfNotNull(primaryAction, frictionEvent)
}

data class ServiceContract(
    val id: ServiceId,
    val pageId: String,
    val revision: String,
    val route: List<CheckpointContract>,
) {
    val steps: List<CheckpointContract>
        get() = route.dropLast(1)

    val completionCheckpoint: CheckpointContract
        get() = route.last()

    fun checkpoint(id: String): CheckpointContract? = route.singleOrNull { it.id == id }

    fun requireCheckpoint(id: String): CheckpointContract =
        checkNotNull(checkpoint(id)) { "Unknown checkpoint: $id" }
}

/**
 * Compile-time contracts for the three synthetic public-service practice journeys.
 *
 * Routes end before submission, payment, or any real-world side effect. The only patchable
 * targets are the nine primary progress actions.
 */
object ServiceCatalog {
    const val REVISION = "2026-07"

    val services: List<ServiceContract> =
        listOf(
            service(
                id = ServiceId.BASIC_PENSION,
                pageId = BASIC_PENSION_PAGE_ID,
                steps =
                    listOf(
                        step(
                            pageId = BASIC_PENSION_PAGE_ID,
                            checkpoint = "pension-applicant",
                            narration = "가상 신청자 정보를 확인해 주세요.",
                            stableKey = "pension-applicant-confirm",
                            accessibleName = "가상 신청자 정보 확인",
                            nextCheckpoint = "pension-method",
                        ),
                        step(
                            pageId = BASIC_PENSION_PAGE_ID,
                            checkpoint = "pension-method",
                            narration = "본인이 신청하는 연습 경로를 선택해 주세요.",
                            stableKey = "pension-self-apply",
                            accessibleName = "본인이 신청해요",
                            nextCheckpoint = "pension-review",
                        ),
                        step(
                            pageId = BASIC_PENSION_PAGE_ID,
                            checkpoint = "pension-review",
                            narration = "가상 신청 내용을 마지막으로 확인해 주세요.",
                            stableKey = "pension-review-confirm",
                            accessibleName = "신청 내용 확인",
                            nextCheckpoint = "pension-complete",
                            friction =
                                friction(
                                    checkpoint = "pension-review",
                                    type = ServiceEventType.ACTION,
                                    stableKey = "pension-save-draft",
                                    accessibleName = "임시 저장",
                                ),
                        ),
                    ),
                completion =
                    completion(
                        checkpoint = "pension-complete",
                        narration = "기초연금 신청 연습을 완료했어요.",
                    ),
            ),
            service(
                id = ServiceId.RESIDENT_RECORD,
                pageId = RESIDENT_RECORD_PAGE_ID,
                steps =
                    listOf(
                        step(
                            pageId = RESIDENT_RECORD_PAGE_ID,
                            checkpoint = "resident-type",
                            narration = "연습용 증명서 종류를 선택해 주세요.",
                            stableKey = "resident-copy-select",
                            accessibleName = "주민등록표 등본",
                            nextCheckpoint = "resident-delivery",
                        ),
                        step(
                            pageId = RESIDENT_RECORD_PAGE_ID,
                            checkpoint = "resident-delivery",
                            narration = "연습용 온라인 발급 방법을 선택해 주세요.",
                            stableKey = "resident-online-delivery",
                            accessibleName = "온라인 발급(연습용)",
                            nextCheckpoint = "resident-review",
                            friction =
                                friction(
                                    checkpoint = "resident-delivery",
                                    type = ServiceEventType.HELP,
                                    stableKey = "resident-delivery-help",
                                    accessibleName = "수령 방법 안내",
                                ),
                        ),
                        step(
                            pageId = RESIDENT_RECORD_PAGE_ID,
                            checkpoint = "resident-review",
                            narration = "가상 등본의 미리보기를 확인해 주세요.",
                            stableKey = "resident-preview",
                            accessibleName = "모의 등본 미리보기",
                            nextCheckpoint = "resident-complete",
                        ),
                    ),
                completion =
                    completion(
                        checkpoint = "resident-complete",
                        narration = "주민등록표 등본 발급 연습을 완료했어요.",
                    ),
            ),
            service(
                id = ServiceId.HEALTH_SCREENING,
                pageId = HEALTH_SCREENING_PAGE_ID,
                steps =
                    listOf(
                        step(
                            pageId = HEALTH_SCREENING_PAGE_ID,
                            checkpoint = "health-person",
                            narration = "가상 사용자 정보를 확인해 주세요.",
                            stableKey = "health-person-confirm",
                            accessibleName = "가상 사용자 정보 확인",
                            nextCheckpoint = "health-year",
                        ),
                        step(
                            pageId = HEALTH_SCREENING_PAGE_ID,
                            checkpoint = "health-year",
                            narration = "2026년 조회 기준을 확인해 주세요.",
                            stableKey = "health-year-2026",
                            accessibleName = "2026년 조회 기준 확인",
                            nextCheckpoint = "health-query",
                        ),
                        step(
                            pageId = HEALTH_SCREENING_PAGE_ID,
                            checkpoint = "health-query",
                            narration = "가상 건강검진 대상 여부를 조회해 주세요.",
                            stableKey = "health-screening-query",
                            accessibleName = "건강검진 대상 조회",
                            nextCheckpoint = "health-complete",
                            friction =
                                friction(
                                    checkpoint = "health-query",
                                    type = ServiceEventType.HELP,
                                    stableKey = "health-schedule-help",
                                    accessibleName = "검진 일정 안내",
                                ),
                        ),
                    ),
                completion =
                    completion(
                        checkpoint = "health-complete",
                        narration = "건강검진 대상 조회 연습을 완료했어요.",
                    ),
            ),
        )

    private val byId = services.associateBy(ServiceContract::id)
    private val byPageId = services.associateBy(ServiceContract::pageId)

    fun find(id: ServiceId): ServiceContract? = byId[id]

    fun require(id: ServiceId): ServiceContract = checkNotNull(find(id))

    fun findByPageId(pageId: String): ServiceContract? = byPageId[pageId]

    fun builtInPatch(
        serviceId: ServiceId,
        checkpoint: String,
    ): PatchV1? =
        find(serviceId)
            ?.steps
            ?.singleOrNull { it.id == checkpoint }
            ?.patch

    private fun service(
        id: ServiceId,
        pageId: String,
        steps: List<CheckpointContract>,
        completion: CheckpointContract,
    ): ServiceContract =
        ServiceContract(
            id = id,
            pageId = pageId,
            revision = REVISION,
            route = steps + completion,
        )

    private fun step(
        pageId: String,
        checkpoint: String,
        narration: String,
        stableKey: String,
        accessibleName: String,
        nextCheckpoint: String,
        friction: EventContract? = null,
    ): CheckpointContract =
        CheckpointContract(
            id = checkpoint,
            narration = narration,
            primaryAction =
                EventContract(
                    type = ServiceEventType.ACTION,
                    stableKey = stableKey,
                    role = BUTTON_ROLE,
                    accessibleName = accessibleName,
                    effect = EventEffect.PROGRESS,
                    expectedCheckpoint = nextCheckpoint,
                ),
            frictionEvent = friction,
            patch =
                PatchV1(
                    pageId = pageId,
                    compatibleRevision = REVISION,
                    stableKey = stableKey,
                    role = BUTTON_ROLE,
                    accessibleName = accessibleName,
                    expectedState = nextCheckpoint,
                ),
        )

    private fun friction(
        checkpoint: String,
        type: ServiceEventType,
        stableKey: String,
        accessibleName: String,
    ): EventContract =
        EventContract(
            type = type,
            stableKey = stableKey,
            role = BUTTON_ROLE,
            accessibleName = accessibleName,
            effect = EventEffect.NON_PROGRESS,
            expectedCheckpoint = checkpoint,
        )

    private fun completion(
        checkpoint: String,
        narration: String,
    ): CheckpointContract =
        CheckpointContract(
            id = checkpoint,
            narration = narration,
        )

    private const val BUTTON_ROLE = "button"
    private const val BASIC_PENSION_PAGE_ID = "bokjiro-basic-pension"
    private const val RESIDENT_RECORD_PAGE_ID = "gov24-resident-record"
    private const val HEALTH_SCREENING_PAGE_ID = "nhis-health-screening"
}
