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
 * targets are the fifteen primary progress actions.
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
                            checkpoint = "pension-service",
                            narration = "기초연금 신청 연습을 찾아 선택해 주세요.",
                            stableKey = "pension-service-select",
                            accessibleName = "기초연금 신청 연습",
                            nextCheckpoint = "pension-applicant",
                            friction =
                                friction(
                                    checkpoint = "pension-service",
                                    type = ServiceEventType.ACTION,
                                    stableKey = "pension-service-misstep",
                                    accessibleName = "복지서비스 선택 다시 확인",
                                ),
                        ),
                        step(
                            pageId = BASIC_PENSION_PAGE_ID,
                            checkpoint = "pension-applicant",
                            narration = "가상 신청자 정보를 확인해 주세요.",
                            stableKey = "pension-applicant-confirm",
                            accessibleName = "연습 사용자 정보 확인",
                            nextCheckpoint = "pension-method",
                            friction =
                                friction(
                                    checkpoint = "pension-applicant",
                                    type = ServiceEventType.ACTION,
                                    stableKey = "pension-applicant-misstep",
                                    accessibleName = "신청인 정보 다시 확인",
                                ),
                        ),
                        step(
                            pageId = BASIC_PENSION_PAGE_ID,
                            checkpoint = "pension-method",
                            narration = "본인이 신청하는 연습 경로를 선택해 주세요.",
                            stableKey = "pension-self-apply",
                            role = "radio",
                            accessibleName = "본인이 신청해요",
                            nextCheckpoint = "pension-contact",
                            friction =
                                friction(
                                    checkpoint = "pension-method",
                                    type = ServiceEventType.ACTION,
                                    stableKey = "pension-method-misstep",
                                    accessibleName = "신청 관계 다시 확인",
                                ),
                        ),
                        step(
                            pageId = BASIC_PENSION_PAGE_ID,
                            checkpoint = "pension-contact",
                            narration = "가상 연락 방법을 확인해 주세요.",
                            stableKey = "pension-contact-confirm",
                            accessibleName = "연락 방법 확인",
                            nextCheckpoint = "pension-review",
                            friction =
                                friction(
                                    checkpoint = "pension-contact",
                                    type = ServiceEventType.ACTION,
                                    stableKey = "pension-contact-misstep",
                                    accessibleName = "연락 방법 다시 확인",
                                ),
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
                            narration = "주민등록표 등본 탭을 선택해 주세요.",
                            stableKey = "resident-copy-select",
                            role = "tab",
                            accessibleName = "주민등록표 등본",
                            nextCheckpoint = "resident-address",
                            friction =
                                friction(
                                    checkpoint = "resident-type",
                                    type = ServiceEventType.ACTION,
                                    stableKey = "resident-type-misstep",
                                    accessibleName = "문서 종류 다시 확인",
                                ),
                        ),
                        step(
                            pageId = RESIDENT_RECORD_PAGE_ID,
                            checkpoint = "resident-address",
                            narration = "가상 주소를 확인해 주세요.",
                            stableKey = "resident-address-confirm",
                            accessibleName = "주소 확인",
                            nextCheckpoint = "resident-issue-type",
                            friction =
                                friction(
                                    checkpoint = "resident-address",
                                    type = ServiceEventType.ACTION,
                                    stableKey = "resident-address-misstep",
                                    accessibleName = "행정구역 다시 확인",
                                ),
                        ),
                        step(
                            pageId = RESIDENT_RECORD_PAGE_ID,
                            checkpoint = "resident-issue-type",
                            narration = "모의 발급 형태를 선택해 주세요.",
                            stableKey = "resident-standard-issue",
                            role = "radio",
                            accessibleName = "발급(모의)",
                            nextCheckpoint = "resident-delivery",
                            friction =
                                friction(
                                    checkpoint = "resident-issue-type",
                                    type = ServiceEventType.ACTION,
                                    stableKey = "resident-issue-misstep",
                                    accessibleName = "발급 형태 다시 확인",
                                ),
                        ),
                        step(
                            pageId = RESIDENT_RECORD_PAGE_ID,
                            checkpoint = "resident-delivery",
                            narration = "연습용 온라인 발급 방법을 선택해 주세요.",
                            stableKey = "resident-online-delivery",
                            role = "combobox",
                            accessibleName = "온라인발급(본인출력·연습용)",
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
                            narration = "민원 신청 연습을 마쳐 주세요.",
                            stableKey = "resident-finish-practice",
                            accessibleName = "민원 신청 연습 마치기",
                            nextCheckpoint = "resident-complete",
                            friction =
                                friction(
                                    checkpoint = "resident-review",
                                    type = ServiceEventType.ACTION,
                                    stableKey = "resident-review-misstep",
                                    accessibleName = "민원 내용 다시 확인",
                                ),
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
                            checkpoint = "health-service",
                            narration = "건강검진 대상 조회 연습을 찾아 선택해 주세요.",
                            stableKey = "health-service-select",
                            accessibleName = "건강검진 대상 조회 연습",
                            nextCheckpoint = "health-person",
                            friction =
                                friction(
                                    checkpoint = "health-service",
                                    type = ServiceEventType.ACTION,
                                    stableKey = "health-service-misstep",
                                    accessibleName = "건강서비스 다시 확인",
                                ),
                        ),
                        step(
                            pageId = HEALTH_SCREENING_PAGE_ID,
                            checkpoint = "health-person",
                            narration = "가상 사용자 정보를 확인해 주세요.",
                            stableKey = "health-person-confirm",
                            accessibleName = "연습 사용자 정보 확인",
                            nextCheckpoint = "health-year",
                            friction =
                                friction(
                                    checkpoint = "health-person",
                                    type = ServiceEventType.ACTION,
                                    stableKey = "health-person-misstep",
                                    accessibleName = "조회 대상 다시 확인",
                                ),
                        ),
                        step(
                            pageId = HEALTH_SCREENING_PAGE_ID,
                            checkpoint = "health-year",
                            narration = "2026년 가상 조회 기준을 선택해 주세요.",
                            stableKey = "health-year-2026",
                            role = "radio",
                            accessibleName = "2026년(가상)",
                            nextCheckpoint = "health-kind",
                            friction =
                                friction(
                                    checkpoint = "health-year",
                                    type = ServiceEventType.ACTION,
                                    stableKey = "health-year-misstep",
                                    accessibleName = "조회 연도 다시 확인",
                                ),
                        ),
                        step(
                            pageId = HEALTH_SCREENING_PAGE_ID,
                            checkpoint = "health-kind",
                            narration = "일반건강검진 모의 항목을 선택해 주세요.",
                            stableKey = "health-general-screening",
                            role = "radio",
                            accessibleName = "일반건강검진(모의)",
                            nextCheckpoint = "health-query",
                            friction =
                                friction(
                                    checkpoint = "health-kind",
                                    type = ServiceEventType.ACTION,
                                    stableKey = "health-kind-misstep",
                                    accessibleName = "검진 종류 다시 확인",
                                ),
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
        role: String = BUTTON_ROLE,
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
                    role = role,
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
                    role = role,
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
