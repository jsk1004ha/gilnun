(() => {
  "use strict";

  const BRIDGE_SCHEMA_VERSION = 2;
  const PATCH_SCHEMA_VERSION = 1;
  const REVISION = "2026-07";
  const BRIDGE_NAME = "gilnun";
  const SUPPORTED_SERVICE_IDS = Object.freeze([
    "basic-pension",
    "resident-record",
    "health-screening",
  ]);
  const SUPPORTED_LAYOUTS = Object.freeze(["A", "B"]);
  const INTERACTION_FIELDS = Object.freeze([
    "schemaVersion",
    "type",
    "serviceId",
    "revision",
    "checkpoint",
    "stableKey",
    "role",
    "accessibleName",
    "effect",
  ]);
  const CHECKPOINT_FIELDS = Object.freeze([
    "schemaVersion",
    "type",
    "serviceId",
    "revision",
    "checkpoint",
  ]);
  const HIGHLIGHT_FIELDS = Object.freeze([
    "schemaVersion",
    "command",
    "pageId",
    "compatibleRevision",
    "stableKey",
    "role",
    "accessibleName",
    "expectedState",
  ]);
  const CLEAR_HIGHLIGHT_FIELDS = Object.freeze(["schemaVersion", "command"]);

  const SERVICES = Object.freeze({
    "basic-pension": {
      serviceId: "basic-pension",
      pageId: "bokjiro-basic-pension",
      revision: REVISION,
      title: "기초연금 신청 연습",
      description: "개인정보를 입력하지 않고 복지서비스 신청 흐름을 차근차근 연습합니다.",
      institution: "복지로형",
      institutionBadge: "복지 서비스 합성 포털",
      portalNav: ["복지서비스", "서비스 신청", "복지지도", "도움말"],
      breadcrumb: ["연습 홈", "복지서비스 신청", "기초연금"],
      tabs: ["서비스 안내", "신청 연습", "진행 확인"],
      noticeTitle: "안심하고 연습하세요",
      noticeText: "자격 판정, 기관 접수, 실제 알림 없이 정해진 선택지만 사용합니다.",
      steps: [
        {
          checkpoint: "pension-service",
          stableKey: "pension-service-select",
          role: "button",
          accessibleName: "기초연금 신청 연습",
          nextCheckpoint: "pension-applicant",
          effect: "PROGRESS",
          type: "ACTION",
          title: "연습할 복지서비스를 선택해 주세요",
          groupTitle: "노후 생활 지원 서비스",
          notice: "서비스를 골라도 실제 신청 화면이나 기관으로 연결되지 않아요.",
          choices: Object.freeze([
            "기초연금 신청 연습",
            "노령연금 안내 살펴보기",
            "복지서비스 모의 계산",
            "가까운 상담 창구 안내",
          ]),
          details: [["연습 분야", "노후 생활 지원"], ["접수 상태", "연습 전"], ["실제 기관 연결", "없음"]],
        },
        {
          checkpoint: "pension-applicant",
          stableKey: "pension-applicant-confirm",
          role: "button",
          accessibleName: "연습 사용자 정보 확인",
          nextCheckpoint: "pension-method",
          effect: "PROGRESS",
          type: "ACTION",
          title: "연습 사용자를 확인해 주세요",
          groupTitle: "신청인 확인",
          notice: "표시된 정보는 연습용이며 실제 개인 식별 정보는 받지 않아요.",
          choices: Object.freeze([
            "연습 사용자 정보 확인",
            "신청 자격 안내 보기",
            "준비 사항 확인",
            "대리 신청 안내",
          ]),
          details: [["신청인", "연습 사용자 (가상)"], ["확인 방식", "가상 정보 확인"], ["본인 확인", "실행하지 않음"]],
        },
        {
          checkpoint: "pension-method",
          stableKey: "pension-self-apply",
          role: "radio",
          accessibleName: "본인이 신청해요",
          nextCheckpoint: "pension-contact",
          effect: "PROGRESS",
          type: "ACTION",
          title: "누가 신청하는지 선택해 주세요",
          groupTitle: "신청 관계",
          notice: "이번 연습은 본인이 직접 신청하는 경로를 따라가요.",
          choices: Object.freeze([
            "본인이 신청해요",
            "배우자가 신청해요",
            "가족이 도와줘요",
            "기관 도움을 받아요",
          ]),
          details: [["연습 사용자", "연습 사용자 (가상)"], ["현재 관계", "선택 전"], ["대리 권한 확인", "실행하지 않음"]],
        },
        {
          checkpoint: "pension-contact",
          stableKey: "pension-contact-confirm",
          role: "button",
          accessibleName: "연락 방법 확인",
          nextCheckpoint: "pension-review",
          effect: "PROGRESS",
          type: "ACTION",
          title: "연락 방법 안내를 확인해 주세요",
          groupTitle: "처리 소식 받기",
          notice: "실제 연락처를 입력하거나 알림을 보내지 않아요.",
          choices: Object.freeze([
            "연락 방법 확인",
            "앱 안 연습 알림 보기",
            "우편 안내 확인",
            "연락 없이 계속 안내",
          ]),
          details: [["안내 방식", "앱 안 연습 알림"], ["실제 발송", "없음"], ["입력 항목", "없음"]],
        },
        {
          checkpoint: "pension-review",
          stableKey: "pension-review-confirm",
          role: "button",
          accessibleName: "신청 내용 확인",
          nextCheckpoint: "pension-complete",
          effect: "PROGRESS",
          type: "ACTION",
          title: "모의 신청 내용을 마지막으로 확인해 주세요",
          groupTitle: "신청 내용 요약",
          notice: "확인해도 실제 신청, 저장, 접수는 이루어지지 않아요.",
          choices: Object.freeze([
            "신청 내용 확인",
            "이전 내용 보기",
            "연습 취소 안내",
            "안내문 다시 보기",
          ]),
          details: [["서비스", "기초연금 신청 연습"], ["신청인", "연습 사용자 (가상)"], ["신청 관계", "본인 (가상)"], ["연락 방식", "연습 알림"]],
          friction: {
            checkpoint: "pension-review",
            type: "ACTION",
            stableKey: "pension-save-draft",
            accessibleName: "임시 저장",
            effect: "NON_PROGRESS",
            role: "button",
            status: "임시 저장을 눌렀어요. 연습 화면에는 아무 정보도 저장하지 않아요.",
          },
        },
      ],
      completion: {
        checkpoint: "pension-complete",
        title: "신청 연습을 마쳤어요",
        message: "기관 접수나 실제 저장 없이 다섯 단계의 기초연금 신청 흐름을 확인했습니다.",
        summary: ["서비스 선택", "연습 사용자 확인", "본인 신청 선택", "연락 방법 확인", "신청 내용 확인"],
      },
    },
    "resident-record": {
      serviceId: "resident-record",
      pageId: "gov24-resident-record",
      revision: REVISION,
      title: "주민등록표 등본 발급 연습",
      description: "정부 민원 신청서와 닮은 합성 화면에서 모의 발급 순서를 연습합니다.",
      institution: "정부24형",
      institutionBadge: "민원 서비스 합성 포털",
      portalNav: ["민원서비스", "보조금 안내", "정책정보", "고객센터"],
      breadcrumb: ["연습 홈", "민원 신청", "주민등록표 등본"],
      tabs: ["민원안내", "신청서 작성", "모의 문서"],
      noticeTitle: "민원 신청 연습 안내",
      noticeText: "모든 선택은 가상이며 문서 생성, 기관 전송, 법적 효력이 없습니다.",
      steps: [
        {
          checkpoint: "resident-type",
          stableKey: "resident-copy-select",
          role: "tab",
          accessibleName: "주민등록표 등본",
          nextCheckpoint: "resident-address",
          effect: "PROGRESS",
          type: "ACTION",
          title: "신청할 문서 종류를 선택해 주세요",
          groupTitle: "민원 문서",
          notice: "문서 탭을 선택해도 실제 민원 신청은 시작되지 않아요.",
          choices: Object.freeze([
            "주민등록표 등본",
            "주민등록표 초본",
            "영문 주민등록표 등본",
            "영문 주민등록표 초본",
          ]),
          details: [["신청인", "연습 사용자 (가상)"], ["세대", "연습 세대 (가상)"], ["문서 상태", "모의 문서"]],
        },
        {
          checkpoint: "resident-address",
          stableKey: "resident-address-confirm",
          role: "button",
          accessibleName: "주소 확인",
          nextCheckpoint: "resident-issue-type",
          effect: "PROGRESS",
          type: "ACTION",
          title: "가상 주소의 행정구역을 확인해 주세요",
          groupTitle: "주소 확인",
          notice: "고정된 가상 행정구역만 사용하며 실제 주소는 입력하지 않아요.",
          choices: Object.freeze([
            "주소 확인",
            "서울특별시(가상) 다시 선택",
            "길눈구(가상) 다시 선택",
            "주소 안내 보기",
          ]),
          details: [["시·도", "서울특별시(가상)"], ["시·군·구", "길눈구(가상)"], ["상세 위치", "사용하지 않음"]],
        },
        {
          checkpoint: "resident-issue-type",
          stableKey: "resident-standard-issue",
          role: "radio",
          accessibleName: "발급(모의)",
          nextCheckpoint: "resident-delivery",
          effect: "PROGRESS",
          type: "ACTION",
          title: "발급 형태를 선택해 주세요",
          groupTitle: "발급 구분",
          notice: "발급과 선택발급의 차이를 연습하지만 문서는 만들지 않아요.",
          choices: Object.freeze([
            "발급(모의)",
            "선택발급(모의)",
            "열람(모의)",
            "문서 종류 다시 선택",
          ]),
          details: [["문서", "주민등록표 등본"], ["표시 범위", "연습용 기본 항목"], ["법적 효력", "없음"]],
        },
        {
          checkpoint: "resident-delivery",
          stableKey: "resident-online-delivery",
          role: "combobox",
          accessibleName: "온라인발급(본인출력·연습용)",
          nextCheckpoint: "resident-review",
          effect: "PROGRESS",
          type: "ACTION",
          title: "수령 방법을 선택해 주세요",
          groupTitle: "수령 방법",
          notice: "온라인발급을 골라도 파일 저장이나 인쇄는 시작되지 않아요.",
          choices: Object.freeze([
            "온라인발급(본인출력·연습용)",
            "전자문서지갑(연습용)",
            "등기우편(연습용)",
            "방문수령(연습용)",
          ]),
          details: [["발급 구분", "발급(모의)"], ["수령 방식", "선택 전"], ["실제 출력", "없음"]],
          friction: {
            checkpoint: "resident-delivery",
            type: "HELP",
            stableKey: "resident-delivery-help",
            accessibleName: "수령 방법 안내",
            effect: "NON_PROGRESS",
            role: "button",
            status: "온라인발급, 전자문서지갑, 등기우편, 방문 중 연습 경로를 골라 주세요.",
          },
        },
        {
          checkpoint: "resident-review",
          stableKey: "resident-finish-practice",
          role: "button",
          accessibleName: "민원 신청 연습 마치기",
          nextCheckpoint: "resident-complete",
          effect: "PROGRESS",
          type: "ACTION",
          title: "모의 민원 신청서를 확인해 주세요",
          groupTitle: "민원 신청 내용",
          notice: "마치기를 눌러도 문서 발급이나 민원 접수는 이루어지지 않아요.",
          choices: Object.freeze([
            "민원 신청 연습 마치기",
            "이전 단계로",
            "임시 저장",
            "취소",
          ]),
          details: [["문서", "주민등록표 등본 (가상)"], ["주소", "서울특별시(가상) 길눈구(가상)"], ["발급 형태", "발급(모의)"], ["수령", "온라인발급 연습"]],
          watermark: "법적 효력 없음",
        },
      ],
      completion: {
        checkpoint: "resident-complete",
        title: "발급 연습을 마쳤어요",
        message: "실제 문서, 파일, 인쇄물 없이 다섯 단계의 민원 신청 흐름을 확인했습니다.",
        summary: ["등본 탭 선택", "가상 주소 확인", "발급 선택", "온라인 수령 선택", "모의 신청 마치기"],
        watermark: "법적 효력 없음",
      },
    },
    "health-screening": {
      serviceId: "health-screening",
      pageId: "nhis-health-screening",
      revision: REVISION,
      title: "건강검진 대상 조회 연습",
      description: "개인정보나 진료 정보 없이 건강검진 대상 조회 흐름을 연습합니다.",
      institution: "건강보험형",
      institutionBadge: "건강검진 합성 포털",
      portalNav: ["건강iN", "검진대상조회", "건강자료실", "이용안내"],
      breadcrumb: ["연습 홈", "건강검진", "검진 대상 조회"],
      tabs: ["검진 안내", "대상 조회 연습", "모의 결과"],
      noticeTitle: "건강검진 조회 연습 안내",
      noticeText: "건강 수치, 진료 기록, 예약 정보를 다루지 않고 가상 결과만 보여 줍니다.",
      steps: [
        {
          checkpoint: "health-service",
          stableKey: "health-service-select",
          role: "button",
          accessibleName: "건강검진 대상 조회 연습",
          nextCheckpoint: "health-person",
          effect: "PROGRESS",
          type: "ACTION",
          title: "이용할 건강서비스를 선택해 주세요",
          groupTitle: "자주 찾는 건강서비스",
          notice: "서비스를 선택해도 실제 건강보험 조회는 시작되지 않아요.",
          choices: Object.freeze([
            "건강검진 대상 조회 연습",
            "검진기관 찾기 연습",
            "건강정보 안내 보기",
            "검진 절차 알아보기",
          ]),
          details: [["서비스", "건강검진"], ["조회 상태", "연습 전"], ["기관 연결", "없음"]],
        },
        {
          checkpoint: "health-person",
          stableKey: "health-person-confirm",
          role: "button",
          accessibleName: "연습 사용자 정보 확인",
          nextCheckpoint: "health-year",
          effect: "PROGRESS",
          type: "ACTION",
          title: "조회할 연습 사용자를 확인해 주세요",
          groupTitle: "조회 대상",
          notice: "가상 사용자만 표시하며 실제 본인 확인은 하지 않아요.",
          choices: Object.freeze([
            "연습 사용자 정보 확인",
            "가족 조회 안내",
            "본인 확인 방식 안내",
            "조회 범위 알아보기",
          ]),
          details: [["조회 사용자", "연습 사용자 (가상)"], ["확인 상태", "가상 정보"], ["실제 조회", "없음"]],
        },
        {
          checkpoint: "health-year",
          stableKey: "health-year-2026",
          role: "radio",
          accessibleName: "2026년(가상)",
          nextCheckpoint: "health-kind",
          effect: "PROGRESS",
          type: "ACTION",
          title: "조회 기준 연도를 선택해 주세요",
          groupTitle: "검진 연도",
          notice: "선택한 연도는 모의 조회에만 쓰이며 실제 자격을 계산하지 않아요.",
          choices: Object.freeze([
            "2026년(가상)",
            "2025년(가상)",
            "2024년(가상)",
            "2023년(가상)",
          ]),
          details: [["조회 기준", "선택 전"], ["사용자", "연습 사용자 (가상)"], ["실제 이력", "조회하지 않음"]],
        },
        {
          checkpoint: "health-kind",
          stableKey: "health-general-screening",
          role: "radio",
          accessibleName: "일반건강검진(모의)",
          nextCheckpoint: "health-query",
          effect: "PROGRESS",
          type: "ACTION",
          title: "확인할 검진 종류를 선택해 주세요",
          groupTitle: "검진 구분",
          notice: "검진 종류만 고르며 실제 결과나 예약 정보는 보여 주지 않아요.",
          choices: Object.freeze([
            "일반건강검진(모의)",
            "구강검진(모의)",
            "암검진(모의)",
            "영유아검진(모의)",
          ]),
          details: [["기준 연도", "2026년(가상)"], ["조회 범위", "대상 여부"], ["실제 결과", "없음"]],
        },
        {
          checkpoint: "health-query",
          stableKey: "health-screening-query",
          role: "button",
          accessibleName: "건강검진 대상 조회",
          nextCheckpoint: "health-complete",
          effect: "PROGRESS",
          type: "ACTION",
          title: "모의 대상 여부를 조회해 주세요",
          groupTitle: "조회 조건",
          notice: "조회 버튼은 정해진 가상 결과만 보여 주며 기관에 연결하지 않아요.",
          choices: Object.freeze([
            "건강검진 대상 조회",
            "조회 조건 다시 보기",
            "검진 종류 바꾸기",
            "조회 연습 취소 안내",
          ]),
          details: [["조회 사용자", "연습 사용자 (가상)"], ["기준 연도", "2026년(가상)"], ["검진 종류", "일반건강검진(모의)"]],
          friction: {
            checkpoint: "health-query",
            type: "HELP",
            stableKey: "health-schedule-help",
            accessibleName: "검진 일정 안내",
            effect: "NON_PROGRESS",
            role: "button",
            status: "일정 안내는 대상 조회 연습을 마친 뒤 확인할 수 있어요.",
          },
        },
      ],
      completion: {
        checkpoint: "health-complete",
        title: "대상 조회 연습을 마쳤어요",
        message: "실제 건강 정보 조회 없이 다섯 단계의 대상 조회 흐름을 확인했습니다.",
        result: "일반건강검진 대상(모의)",
        summary: ["조회 서비스 선택", "연습 사용자 확인", "가상 연도 선택", "검진 종류 선택", "대상 여부 조회"],
      },
    },
  });

  const query = new URLSearchParams(window.location.search);
  const requestedServiceId = query.get("service");
  const requestedLayout = query.get("layout");
  const serviceId = SUPPORTED_SERVICE_IDS.includes(requestedServiceId)
    ? requestedServiceId
    : SUPPORTED_SERVICE_IDS[0];
  let activeLayout = SUPPORTED_LAYOUTS.includes(requestedLayout) ? requestedLayout : "A";
  const service = SERVICES[serviceId];

  const institutionName = document.getElementById("institution-name");
  const institutionBadge = document.getElementById("institution-badge");
  const serviceTitle = document.getElementById("service-title");
  const serviceDescription = document.getElementById("service-description");
  const portalNav = document.getElementById("portal-nav");
  const portalBreadcrumb = document.getElementById("portal-breadcrumb");
  const portalTabs = document.getElementById("portal-tabs");
  const noticeBoardTitle = document.getElementById("notice-board-title");
  const noticeBoardCopy = document.getElementById("notice-board-copy");
  const sideMenuTitle = document.getElementById("side-menu-title");
  const progressList = document.getElementById("progress-list");
  const practiceRoot = document.getElementById("practice-root");
  const practiceStatus = document.getElementById("practice-status");
  const layoutUpdateNotice = document.getElementById("layout-update-notice");

  let currentStepIndex = 0;
  let currentCheckpoint = service.steps[0].checkpoint;
  let isComplete = false;
  let highlightedTarget = null;
  let didAutomaticLayoutUpdate = false;

  document.body.classList.toggle("layout-b", activeLayout === "B");
  document.body.classList.add(`service-${service.serviceId}`);
  document.body.dataset.activeLayout = activeLayout;
  document.documentElement.dataset.pageId = service.pageId;
  institutionName.textContent = `${service.institution} 합성 화면`;
  institutionBadge.textContent = service.institutionBadge;
  serviceTitle.textContent = service.title;
  serviceDescription.textContent = service.description;
  noticeBoardTitle.textContent = service.noticeTitle;
  noticeBoardCopy.textContent = service.noticeText;
  sideMenuTitle.textContent = `${service.institution} 연습 진행 순서`;

  function exactKeys(candidate, expectedFields) {
    return (
      candidate !== null &&
      typeof candidate === "object" &&
      !Array.isArray(candidate) &&
      Object.keys(candidate).sort().join("|") === [...expectedFields].sort().join("|")
    );
  }

  function setStatus(message, isError = false) {
    practiceStatus.textContent = message;
    practiceStatus.classList.toggle("is-error", isError);
  }

  function postPayload(payload) {
    const bridge = window[BRIDGE_NAME];
    if (!bridge || typeof bridge.postMessage !== "function") {
      return false;
    }
    bridge.postMessage(JSON.stringify(payload));
    return true;
  }

  function emitInteraction(event) {
    const payload = {
      schemaVersion: BRIDGE_SCHEMA_VERSION,
      type: event.type,
      serviceId: service.serviceId,
      revision: service.revision,
      checkpoint: currentCheckpoint,
      stableKey: event.stableKey,
      role: event.role,
      accessibleName: event.accessibleName,
      effect: event.effect,
    };
    if (!exactKeys(payload, INTERACTION_FIELDS)) {
      return false;
    }
    return postPayload(payload);
  }

  function emitCheckpointChanged() {
    const payload = {
      schemaVersion: BRIDGE_SCHEMA_VERSION,
      type: "CHECKPOINT_CHANGED",
      serviceId: service.serviceId,
      revision: service.revision,
      checkpoint: currentCheckpoint,
    };
    if (!exactKeys(payload, CHECKPOINT_FIELDS)) {
      return false;
    }
    return postPayload(payload);
  }

  function clearHighlight() {
    if (highlightedTarget) {
      highlightedTarget.classList.remove("gilnun-highlight");
    }
    highlightedTarget = null;
  }

  function applyHighlight(command) {
    if (
      !exactKeys(command, HIGHLIGHT_FIELDS) ||
      command.schemaVersion !== PATCH_SCHEMA_VERSION ||
      command.command !== "HIGHLIGHT" ||
      command.pageId !== service.pageId ||
      command.compatibleRevision !== service.revision
    ) {
      clearHighlight();
      return;
    }
    const matches = Array.from(document.querySelectorAll("[data-stable-key]")).filter(
      (target) =>
        target.dataset.stableKey === command.stableKey &&
        target.dataset.expectedState === command.expectedState &&
        target.getAttribute("role") === command.role &&
        target.getAttribute("aria-label") === command.accessibleName,
    );
    if (matches.length !== 1) {
      clearHighlight();
      return;
    }
    clearHighlight();
    highlightedTarget = matches[0];
    highlightedTarget.classList.add("gilnun-highlight");
    setStatus("위치가 달라져도 의미를 다시 찾았어요. 이름·역할·다음 상태를 확인한 선택입니다.");
  }

  function createElement(tagName, className, text) {
    const element = document.createElement(tagName);
    if (className) {
      element.className = className;
    }
    if (text) {
      element.textContent = text;
    }
    return element;
  }

  function createSemanticTarget(control, step) {
    control.dataset.stableKey = step.stableKey;
    control.dataset.expectedState = step.nextCheckpoint;
    control.setAttribute("role", step.role);
    control.setAttribute("aria-label", step.accessibleName);
    return control;
  }

  function selectLocalChoice(group, control, role, label) {
    const controls = Array.from(group.querySelectorAll("button"));
    if (role === "radio") {
      controls.forEach((item) => item.setAttribute("aria-checked", String(item === control)));
    } else if (role === "tab") {
      controls.forEach((item) => item.setAttribute("aria-selected", String(item === control)));
    } else {
      controls.forEach((item) => item.setAttribute("aria-pressed", String(item === control)));
    }
    setStatus(`${label} 항목을 선택했어요. 안내된 선택을 찾으면 다음 단계로 이동해요.`);
  }

  function createChoiceButtons(step, className) {
    const group = createElement("div", className);
    if (step.role === "radio") {
      group.setAttribute("role", "radiogroup");
      group.setAttribute("aria-label", step.groupTitle);
    } else if (step.role === "tab") {
      group.setAttribute("role", "tablist");
      group.setAttribute("aria-label", "문서 종류");
    }
    step.choices.forEach((label) => {
      const control = createElement("button", "choice-control", label);
      control.type = "button";
      if (step.role === "radio") {
        control.setAttribute("role", "radio");
        control.setAttribute("aria-checked", "false");
      } else if (step.role === "tab") {
        control.setAttribute("role", "tab");
        control.setAttribute("aria-selected", "false");
      } else {
        control.setAttribute("aria-pressed", "false");
      }
      if (label === step.accessibleName) {
        createSemanticTarget(control, step);
      }
      control.addEventListener("click", () => {
        selectLocalChoice(group, control, step.role, label);
        if (label === step.accessibleName) {
          handleInteraction(step);
        }
      });
      group.append(control);
    });
    return group;
  }

  function createChoiceSelect(step) {
    const wrap = createElement("div", "fixed-select-wrap");
    const label = createElement("span", "fixed-select-label", "수령 방법 선택");
    const select = createElement("select", "fixed-choice-select");
    const prompt = createElement("option", "", "수령 방법을 선택해 주세요");
    prompt.value = "";
    prompt.disabled = true;
    prompt.selected = true;
    select.append(prompt);
    step.choices.forEach((choice) => {
      const option = createElement("option", "", choice);
      option.value = choice;
      select.append(option);
    });
    createSemanticTarget(select, step);
    select.addEventListener("change", () => {
      const selected = select.value;
      setStatus(`${selected} 항목을 선택했어요.`);
      if (selected === step.accessibleName) {
        handleInteraction(step);
      }
    });
    wrap.append(label, select);
    return wrap;
  }

  function createFrictionButton(friction) {
    const button = createElement("button", "action-button action-button--secondary", friction.accessibleName);
    button.type = "button";
    button.setAttribute("role", friction.role);
    button.setAttribute("aria-label", friction.accessibleName);
    button.addEventListener("click", () => handleInteraction(friction));
    return button;
  }

  function createStepHeading(step) {
    const headingGroup = createElement("div", "step-heading");
    const stepNumber = createElement("p", "step-number", `${currentStepIndex + 1}단계 / ${service.steps.length}단계`);
    const heading = createElement("h2", "", step.title);
    heading.id = "step-title";
    heading.tabIndex = -1;
    const copy = createElement("p", "step-copy", "정해진 선택지만 사용합니다. 안내된 의미의 항목을 직접 선택해 주세요.");
    headingGroup.append(stepNumber, heading, copy);
    return { headingGroup, heading };
  }

  function createGroupedSummary(step) {
    const summary = createElement("dl", `grouped-summary${step.watermark ? " document-preview" : ""}`);
    summary.append(createElement("div", "group-title", step.groupTitle));
    step.details.forEach(([label, value]) => {
      const row = createElement("div", "read-only-choice-row");
      row.append(createElement("dt", "", label), createElement("dd", "", value));
      summary.append(row);
    });
    if (step.watermark) {
      summary.append(createElement("span", "document-preview__watermark", step.watermark));
    }
    return summary;
  }

  function createSafetyNotice(step) {
    const notice = createElement("div", "step-notice");
    notice.append(createElement("strong", "", "연습 안전 안내"), createElement("p", "", step.notice));
    return notice;
  }

  function renderPensionStep(moveFocus) {
    const step = service.steps[currentStepIndex];
    const { headingGroup, heading } = createStepHeading(step);
    const composition = createElement("div", "pension-benefit-layout");
    const benefitCard = createElement("section", "pension-benefit-card");
    benefitCard.append(
      createElement("p", "panel-label", "따뜻한 노후생활 길잡이"),
      createElement("h3", "", step.groupTitle),
      createElement("p", "", "큰 글씨와 고정된 선택지로 신청 순서를 안전하게 익혀 보세요."),
    );
    const choiceBoard = createElement("section", "choice-board pension-choice-board");
    choiceBoard.append(createElement("h3", "", "이 단계에서 선택할 내용"), createChoiceButtons(step, "choice-grid"));
    composition.append(benefitCard, createGroupedSummary(step), choiceBoard, createSafetyNotice(step));
    if (step.friction) {
      const actions = createElement("div", "actions");
      actions.append(createFrictionButton(step.friction));
      composition.append(actions);
    }
    practiceRoot.replaceChildren(headingGroup, composition);
    setStatus(`${currentStepIndex + 1}단계 복지서비스 신청 연습을 진행하고 있어요.`);
    if (moveFocus) heading.focus();
  }

  function createFixedJurisdiction() {
    const region = createElement("section", "resident-jurisdiction");
    region.append(createElement("h3", "", "행정구역 (가상)"));
    [["시·도", ["서울특별시(가상)", "길눈시(가상)", "새봄도(가상)", "한빛도(가상)"]], ["시·군·구", ["길눈구(가상)", "연습구(가상)", "새길군(가상)", "도움구(가상)"]]].forEach(([title, values]) => {
      const wrap = createElement("div", "fixed-select-wrap");
      const label = createElement("span", "fixed-select-label", title);
      const select = createElement("select", "fixed-choice-select");
      select.setAttribute("aria-label", `${title} 가상 선택`);
      values.forEach((value) => {
        const option = createElement("option", "", value);
        option.value = value;
        select.append(option);
      });
      select.addEventListener("change", () => setStatus(`${select.value} 항목을 가상 주소로 선택했어요.`));
      wrap.append(label, select);
      region.append(wrap);
    });
    return region;
  }

  function createMockDocument(step) {
    const preview = createElement("aside", "resident-document-preview");
    preview.append(
      createElement("strong", "", "주민등록표 등본 (가상)"),
      createElement("p", "", "연습 세대 (가상) · 서울특별시(가상) 길눈구(가상)"),
      createElement("span", "document-preview__watermark", "법적 효력 없음"),
    );
    if (step.checkpoint !== "resident-review") {
      preview.append(createElement("p", "preview-muted", "선택을 마치면 이 모의 문서 요약이 완성됩니다."));
    }
    return preview;
  }

  function renderResidentStep(moveFocus) {
    const step = service.steps[currentStepIndex];
    const { headingGroup, heading } = createStepHeading(step);
    const shell = createElement("div", "resident-application-shell");
    const alert = createElement("details", "resident-alert");
    const alertTitle = createElement("summary", "", "신청 전 꼭 확인하세요");
    alert.append(alertTitle, createElement("p", "", "이 화면은 민원 신청 구조를 익히는 연습이며 실제 문서를 만들지 않습니다."));
    const application = createElement("section", "resident-application-panel");
    application.append(createElement("p", "resident-section-number", `신청 항목 ${currentStepIndex + 1}`));
    if (step.checkpoint === "resident-address") {
      application.append(createFixedJurisdiction());
    }
    const controls =
      step.role === "combobox"
        ? createChoiceSelect(step)
        : createChoiceButtons(step, step.role === "tab" ? "resident-document-tabs" : "resident-control-grid");
    application.append(createGroupedSummary(step), controls, createSafetyNotice(step));
    if (step.friction) {
      const actions = createElement("div", "actions");
      actions.append(createFrictionButton(step.friction));
      application.append(actions);
    }
    shell.append(alert, application, createMockDocument(step));
    practiceRoot.replaceChildren(headingGroup, shell);
    setStatus(`${currentStepIndex + 1}단계 모의 민원 신청서를 작성하고 있어요.`);
    if (moveFocus) heading.focus();
  }

  function renderHealthStep(moveFocus) {
    const step = service.steps[currentStepIndex];
    const { headingGroup, heading } = createStepHeading(step);
    const dashboard = createElement("div", "health-dashboard");
    const statusCard = createElement("section", "health-status-card");
    statusCard.append(
      createElement("span", "health-symbol", "건강"),
      createElement("h3", "", step.groupTitle),
      createElement("p", "", "검진 대상 여부를 알아보는 모의 조회입니다."),
    );
    const conditions = createGroupedSummary(step);
    const choiceBoard = createElement("section", "choice-board health-choice-board");
    choiceBoard.append(createElement("h3", "", "조회 조건 선택"), createChoiceButtons(step, "health-choice-grid"));
    dashboard.append(statusCard, conditions, choiceBoard, createSafetyNotice(step));
    if (step.friction) {
      const actions = createElement("div", "actions");
      actions.append(createFrictionButton(step.friction));
      dashboard.append(actions);
    }
    practiceRoot.replaceChildren(headingGroup, dashboard);
    setStatus(`${currentStepIndex + 1}단계 건강검진 조회 조건을 확인하고 있어요.`);
    if (moveFocus) heading.focus();
  }

  function renderInstitutionShell() {
    portalNav.replaceChildren();
    service.portalNav.forEach((label, index) => {
      portalNav.append(createElement("span", index === 1 ? "is-current" : "", label));
    });
    portalBreadcrumb.replaceChildren();
    service.breadcrumb.forEach((label, index) => {
      if (index > 0) {
        const separator = createElement("span", "breadcrumb-separator", "›");
        separator.setAttribute("aria-hidden", "true");
        portalBreadcrumb.append(separator);
      }
      portalBreadcrumb.append(createElement("span", "", label));
    });
    portalTabs.replaceChildren();
    service.tabs.forEach((label, index) => {
      const isCurrent = index === (isComplete ? 2 : 1);
      const tab = createElement("span", `portal-tab${isCurrent ? " is-current" : ""}`, label);
      tab.setAttribute("role", "listitem");
      if (isCurrent) tab.setAttribute("aria-current", "page");
      portalTabs.append(tab);
    });
  }

  function renderProgress() {
    progressList.replaceChildren();
    service.steps.forEach((step, index) => {
      const item = createElement("li", "", `${index + 1}. ${step.title}`);
      if (index < currentStepIndex || isComplete) item.classList.add("is-complete");
      else if (index === currentStepIndex) item.setAttribute("aria-current", "step");
      progressList.append(item);
    });
  }

  function renderCompletion(moveFocus) {
    const card = createElement("div", "completion-card");
    const heading = createElement("h2", "", service.completion.title);
    heading.id = "step-title";
    heading.tabIndex = -1;
    const summary = createElement("div", "completion-summary");
    summary.append(createElement("strong", "", "확인한 연습 순서"));
    service.completion.summary.forEach((item) => summary.append(createElement("p", "", `확인 · ${item}`)));
    if (service.completion.watermark) {
      summary.append(createElement("span", "document-preview__watermark", service.completion.watermark));
    }
    card.append(
      createElement("span", "completion-label", "연습 완료"),
      heading,
      createElement("p", "step-copy", service.completion.message),
      summary,
    );
    if (service.completion.result) {
      const result = createElement("div", "mock-result");
      result.append(createElement("strong", "", "모의 조회 결과"), createElement("p", "", service.completion.result));
      card.append(result);
    }
    practiceRoot.replaceChildren(card);
    setStatus("연습이 끝났어요. 실제 기관에는 아무 내용도 전달되지 않았어요.");
    if (moveFocus) heading.focus();
  }

  function renderCurrent(moveFocus = true) {
    clearHighlight();
    renderInstitutionShell();
    renderProgress();
    if (isComplete) {
      renderCompletion(moveFocus);
      return;
    }
    if (service.serviceId === "basic-pension") renderPensionStep(moveFocus);
    else if (service.serviceId === "resident-record") renderResidentStep(moveFocus);
    else renderHealthStep(moveFocus);
  }

  function applyAutomaticLayoutUpdate() {
    if (didAutomaticLayoutUpdate || currentStepIndex !== 1 || activeLayout !== "A") {
      return;
    }
    didAutomaticLayoutUpdate = true;
    activeLayout = "B";
    document.body.classList.add("layout-b", "layout-just-updated");
    document.body.dataset.activeLayout = activeLayout;
    layoutUpdateNotice.hidden = false;
    window.setTimeout(() => document.body.classList.remove("layout-just-updated"), 450);
  }

  function handleInteraction(event) {
    emitInteraction(event);
    if (event.effect === "NON_PROGRESS") {
      setStatus(event.status);
      return;
    }
    currentCheckpoint = event.nextCheckpoint;
    currentStepIndex += 1;
    isComplete = currentStepIndex === service.steps.length;
    applyAutomaticLayoutUpdate();
    renderCurrent();
    emitCheckpointChanged();
  }

  window.addEventListener("message", (messageEvent) => {
    if (typeof messageEvent.data !== "string" || messageEvent.data.length > 4096) return;
    let command;
    try {
      command = JSON.parse(messageEvent.data);
    } catch {
      clearHighlight();
      return;
    }
    if (
      exactKeys(command, CLEAR_HIGHLIGHT_FIELDS) &&
      command.schemaVersion === PATCH_SCHEMA_VERSION &&
      command.command === "CLEAR_HIGHLIGHT"
    ) {
      clearHighlight();
      setStatus("강조 표시를 지웠어요.");
      return;
    }
    applyHighlight(command);
  });

  renderCurrent(false);
})();
