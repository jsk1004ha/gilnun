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
      description: "개인정보를 입력하지 않고 신청 순서만 안전하게 연습합니다.",
      institution: "복지로형",
      institutionBadge: "복지 서비스 합성 포털",
      portalNav: ["복지서비스 안내", "신청 연습", "이용 도움"],
      breadcrumb: ["연습 홈", "복지 서비스", "기초연금"],
      tabs: ["서비스 안내", "신청서 연습", "완료 확인"],
      noticeTitle: "복지 서비스 연습 전 안내",
      noticeText: "실제 개인정보를 입력하지 않으며 자격 판정이나 기관 접수를 하지 않아요.",
      steps: [
        {
          checkpoint: "pension-applicant",
          stableKey: "pension-applicant-confirm",
          accessibleName: "가상 신청자 정보 확인",
          nextCheckpoint: "pension-method",
          title: "가상 신청자 정보를 확인해 주세요",
          contextLabel: "신청자",
          contextText: "연습 사용자 (가상)",
          effect: "PROGRESS",
          type: "ACTION",
          role: "button",
          groupTitle: "신청인 확인 정보",
          details: [
            ["신청인", "연습 사용자 (가상)"],
            ["신청 경로", "본인 신청 연습"],
            ["처리 상태", "아직 접수되지 않음"],
          ],
          choices: [
            ["연습 사용자 정보", "확인 대상"],
            ["실제 개인 식별 정보", "입력하지 않음"],
          ],
          notice: "이 단계에서는 실제 개인정보 없이 신청인 구분만 확인해요.",
        },
        {
          checkpoint: "pension-method",
          stableKey: "pension-self-apply",
          accessibleName: "본인이 신청해요",
          nextCheckpoint: "pension-review",
          title: "신청 방법을 골라 주세요",
          contextLabel: "신청 관계",
          contextText: "본인 (가상)",
          effect: "PROGRESS",
          type: "ACTION",
          role: "button",
          groupTitle: "신청 관계 선택 정보",
          details: [
            ["신청 관계", "본인 (가상)"],
            ["진행 방법", "직접 신청 연습"],
            ["다음 순서", "신청 내용 확인"],
          ],
          choices: [
            ["본인 신청", "이번 연습 경로"],
            ["가족·대리 신청", "이번 연습 제외"],
          ],
          notice: "가족·대리 신청은 이번 연습에서 다루지 않아요.",
        },
        {
          checkpoint: "pension-review",
          stableKey: "pension-review-confirm",
          accessibleName: "신청 내용 확인",
          nextCheckpoint: "pension-complete",
          title: "신청 내용을 확인해 주세요",
          contextLabel: "연습 신청",
          contextText: "기초연금 신청 내용 (가상)",
          effect: "PROGRESS",
          type: "ACTION",
          role: "button",
          groupTitle: "신청 내용 최종 확인",
          details: [
            ["서비스", "기초연금 신청 연습"],
            ["신청인", "연습 사용자 (가상)"],
            ["신청 관계", "본인 (가상)"],
          ],
          choices: [
            ["연습 내용 다시 보기", "읽기 전용"],
            ["실제 기관 접수", "진행하지 않음"],
          ],
          notice: "신청 내용 확인을 눌러도 실제 신청이나 접수는 이루어지지 않아요.",
          friction: {
            checkpoint: "pension-review",
            type: "ACTION",
            stableKey: "pension-save-draft",
            accessibleName: "임시 저장",
            effect: "NON_PROGRESS",
            role: "button",
            status: "임시 저장을 눌렀어요. 현재 단계에 그대로 머물러요.",
          },
        },
      ],
      completion: {
        checkpoint: "pension-complete",
        title: "신청 연습을 마쳤어요",
        message: "실제 신청·접수·자격 판정 없이 세 단계의 연습만 마쳤습니다.",
        summary: ["가상 신청자 확인", "본인 신청 경로 선택", "신청 내용 확인"],
      },
    },
    "resident-record": {
      serviceId: "resident-record",
      pageId: "gov24-resident-record",
      revision: REVISION,
      title: "주민등록표 등본 발급 연습",
      description: "실제 문서가 아닌 모의 화면으로 발급 순서를 연습합니다.",
      institution: "정부24형",
      institutionBadge: "민원 서비스 합성 포털",
      portalNav: ["민원서비스 안내", "증명서 연습", "발급 도움"],
      breadcrumb: ["연습 홈", "민원 서비스", "등본 발급"],
      tabs: ["민원 안내", "발급 연습", "문서 확인"],
      noticeTitle: "증명서 발급 연습 전 안내",
      noticeText: "실제 문서나 파일을 만들지 않으며 모든 모의 화면에는 법적 효력이 없어요.",
      steps: [
        {
          checkpoint: "resident-type",
          stableKey: "resident-copy-select",
          accessibleName: "주민등록표 등본",
          nextCheckpoint: "resident-delivery",
          title: "연습할 문서 종류를 골라 주세요",
          contextLabel: "발급 대상",
          contextText: "연습 세대 (가상)",
          effect: "PROGRESS",
          type: "ACTION",
          role: "button",
          groupTitle: "민원 서비스 선택 정보",
          details: [
            ["문서 종류", "주민등록표 등본"],
            ["주소 기준", "서울특별시 > 길눈구 (가상)"],
            ["발급 형태", "발급 (모의)"],
            ["문서 상태", "모의 문서"],
          ],
          choices: [
            ["주민등록표 등본", "이번 연습 문서"],
            ["주민등록표 초본", "이번 연습 제외"],
            ["영문 주민등록표", "이번 연습 제외"],
          ],
          notice: "실제 문서를 선택하거나 발급하지 않고 순서만 익혀요.",
        },
        {
          checkpoint: "resident-delivery",
          stableKey: "resident-online-delivery",
          accessibleName: "온라인 발급(연습용)",
          nextCheckpoint: "resident-review",
          title: "수령 방법을 골라 주세요",
          contextLabel: "수령 방식",
          contextText: "온라인 발급 연습 (가상)",
          effect: "PROGRESS",
          type: "ACTION",
          role: "button",
          groupTitle: "수령 방법 선택 정보",
          details: [
            ["수령 방식", "온라인 발급(연습용)"],
            ["파일 생성", "실제 파일을 만들지 않음"],
            ["다음 순서", "모의 문서 확인"],
          ],
          choices: [
            ["온라인발급(본인출력)", "이번 연습 경로"],
            ["전자문서지갑", "이번 연습 제외"],
            ["등기보통우편", "이번 연습 제외"],
          ],
          notice: "온라인 발급을 골라도 저장이나 인쇄는 이루어지지 않아요.",
          friction: {
            checkpoint: "resident-delivery",
            type: "HELP",
            stableKey: "resident-delivery-help",
            accessibleName: "수령 방법 안내",
            effect: "NON_PROGRESS",
            role: "button",
            status: "온라인 발급 연습 버튼을 직접 선택해 주세요.",
          },
        },
        {
          checkpoint: "resident-review",
          stableKey: "resident-preview",
          accessibleName: "모의 등본 미리보기",
          nextCheckpoint: "resident-complete",
          title: "모의 등본을 확인해 주세요",
          contextLabel: "문서 미리보기",
          contextText: "주민등록표 등본 (가상)",
          watermark: "법적 효력 없음",
          effect: "PROGRESS",
          type: "ACTION",
          role: "button",
          groupTitle: "모의 문서 확인 정보",
          details: [
            ["문서명", "주민등록표 등본 (가상)"],
            ["세대 구분", "연습 세대 (가상)"],
            ["발급 상태", "아직 발급되지 않음"],
          ],
          choices: [
            ["모의 문서", "미리보기 준비"],
            ["선택 발급", "이번 연습 제외"],
            ["실제 파일", "생성하지 않음"],
          ],
          notice: "미리보기를 열어도 실제 문서 파일은 만들어지지 않아요.",
        },
      ],
      completion: {
        checkpoint: "resident-complete",
        title: "발급 연습을 마쳤어요",
        message: "실제 문서·파일·인쇄물 없이 세 단계의 연습만 마쳤습니다.",
        summary: ["문서 종류 선택", "온라인 발급 연습 선택", "모의 등본 미리보기"],
        watermark: "법적 효력 없음",
      },
    },
    "health-screening": {
      serviceId: "health-screening",
      pageId: "nhis-health-screening",
      revision: REVISION,
      title: "건강검진 대상 조회 연습",
      description: "개인정보나 진료 정보 없이 모의 조회 순서만 연습합니다.",
      institution: "건강보험형",
      institutionBadge: "건강검진 합성 포털",
      portalNav: ["건강검진 안내", "대상 조회 연습", "이용 도움"],
      breadcrumb: ["연습 홈", "건강검진", "대상 조회"],
      tabs: ["조회 안내", "대상 조회 연습", "모의 결과"],
      noticeTitle: "건강검진 조회 연습 전 안내",
      noticeText: "실제 의료 정보·검진 결과·예약 정보를 조회하지 않고 대상 여부 순서만 연습해요.",
      steps: [
        {
          checkpoint: "health-person",
          stableKey: "health-person-confirm",
          accessibleName: "가상 사용자 정보 확인",
          nextCheckpoint: "health-year",
          title: "가상 사용자 정보를 확인해 주세요",
          contextLabel: "조회 사용자",
          contextText: "연습 사용자 (가상)",
          effect: "PROGRESS",
          type: "ACTION",
          role: "button",
          groupTitle: "조회 사용자 확인 정보",
          details: [
            ["조회 사용자", "연습 사용자 (가상)"],
            ["조회 종류", "본인 대상 여부 연습"],
            ["확인 상태", "실제 본인 확인 없음"],
          ],
          choices: [
            ["연습 사용자 정보", "확인 대상"],
            ["실제 본인 확인", "진행하지 않음"],
          ],
          notice: "개인정보나 진료 정보는 입력하지 않아요.",
        },
        {
          checkpoint: "health-year",
          stableKey: "health-year-2026",
          accessibleName: "2026년 조회 기준 확인",
          nextCheckpoint: "health-query",
          title: "조회 기준 연도를 확인해 주세요",
          contextLabel: "조회 기준",
          contextText: "2026년 (가상)",
          effect: "PROGRESS",
          type: "ACTION",
          role: "button",
          groupTitle: "조회 기준 확인 정보",
          details: [
            ["기준 연도", "2026년 (가상)"],
            ["조회 항목", "일반건강검진 대상 여부"],
            ["제외 항목", "결과·예약·의료 수치"],
          ],
          choices: [
            ["2026년", "이번 연습 기준"],
            ["다른 연도", "이번 연습 제외"],
          ],
          notice: "기준 연도만 확인하며 실제 자격 계산은 하지 않아요.",
        },
        {
          checkpoint: "health-query",
          stableKey: "health-screening-query",
          accessibleName: "건강검진 대상 조회",
          nextCheckpoint: "health-complete",
          title: "모의 대상 여부를 조회해 주세요",
          contextLabel: "조회 범위",
          contextText: "일반건강검진 모의 조회",
          effect: "PROGRESS",
          type: "ACTION",
          role: "button",
          groupTitle: "모의 조회 범위",
          details: [
            ["조회 대상", "연습 사용자 (가상)"],
            ["조회 기준", "2026년 (가상)"],
            ["조회 범위", "일반건강검진 대상 여부 모의 조회"],
          ],
          choices: [
            ["대상 여부", "모의 조회"],
            ["검진 결과·예약", "조회하지 않음"],
          ],
          notice: "대상 조회를 눌러도 실제 기관 조회나 예약은 이루어지지 않아요.",
          friction: {
            checkpoint: "health-query",
            type: "HELP",
            stableKey: "health-schedule-help",
            accessibleName: "검진 일정 안내",
            effect: "NON_PROGRESS",
            role: "button",
            status: "대상 조회 연습을 마친 뒤 일정을 확인할 수 있어요.",
          },
        },
      ],
      completion: {
        checkpoint: "health-complete",
        title: "대상 조회 연습을 마쳤어요",
        message: "실제 건강 정보 조회 없이 세 단계의 연습만 마쳤습니다.",
        result: "일반건강검진 대상(모의)",
        summary: ["가상 사용자 확인", "2026년 기준 확인", "대상 여부 모의 조회"],
      },
    },
  });

  const query = new URLSearchParams(window.location.search);
  const requestedServiceId = query.get("service");
  const requestedLayout = query.get("layout");
  const serviceId = SUPPORTED_SERVICE_IDS.includes(requestedServiceId)
    ? requestedServiceId
    : SUPPORTED_SERVICE_IDS[0];
  const layout = SUPPORTED_LAYOUTS.includes(requestedLayout) ? requestedLayout : "A";
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

  let currentStepIndex = 0;
  let currentCheckpoint = service.steps[0].checkpoint;
  let isComplete = false;
  let highlightedTarget = null;

  document.body.classList.toggle("layout-b", layout === "B");
  document.body.classList.add(`service-${service.serviceId}`);
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
    setStatus(
      "위치가 달라져도 의미를 다시 찾았어요. 이름·역할·다음 상태를 확인한 버튼입니다.",
    );
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

  function renderInstitutionShell() {
    portalNav.replaceChildren();
    service.portalNav.forEach((label, index) => {
      const item = createElement("span", index === 1 ? "is-current" : "", label);
      portalNav.append(item);
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
      if (isCurrent) {
        tab.setAttribute("aria-current", "page");
      }
      portalTabs.append(tab);
    });
  }

  function createActionButton(event, className) {
    const button = createElement("button", `action-button ${className}`, event.accessibleName);
    button.type = "button";
    button.setAttribute("role", event.role);
    button.setAttribute("aria-label", event.accessibleName);
    button.dataset.stableKey = event.stableKey;
    button.dataset.expectedState =
      event.effect === "PROGRESS" ? event.nextCheckpoint : event.checkpoint;
    button.addEventListener("click", () => handleInteraction(event));
    return button;
  }

  function renderProgress() {
    progressList.replaceChildren();
    service.steps.forEach((step, index) => {
      const item = createElement("li", "", `${index + 1}. ${step.title}`);
      if (index < currentStepIndex || isComplete) {
        item.classList.add("is-complete");
      } else if (index === currentStepIndex) {
        item.setAttribute("aria-current", "step");
      }
      progressList.append(item);
    });
  }

  function renderCompletion(moveFocus) {
    const card = createElement("div", "completion-card");
    const label = createElement("span", "completion-label", "연습 완료");
    const heading = createElement("h2", "", service.completion.title);
    heading.id = "step-title";
    heading.tabIndex = -1;
    const message = createElement("p", "step-copy", service.completion.message);
    const summary = createElement("div", "completion-summary");
    summary.append(createElement("strong", "", "확인한 연습 순서"));
    service.completion.summary.forEach((item) => {
      summary.append(createElement("p", "", `확인 · ${item}`));
    });
    if (service.completion.watermark) {
      summary.append(
        createElement("span", "document-preview__watermark", service.completion.watermark),
      );
    }
    card.append(label, heading, message, summary);

    if (service.completion.result) {
      const result = createElement("div", "mock-result");
      const resultLabel = createElement("strong", "", "모의 조회 결과");
      const resultText = createElement("p", "", service.completion.result);
      result.append(resultLabel, resultText);
      card.append(result);
    }

    practiceRoot.replaceChildren(card);
    setStatus("연습이 끝났어요. 실제 기관에는 아무 내용도 전달되지 않았어요.");
    if (moveFocus) {
      heading.focus();
    }
  }

  function createGroupedSummary(step) {
    const className = step.watermark
      ? "grouped-summary document-preview"
      : "grouped-summary";
    const summary = createElement("dl", className);
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

  function createChoiceBoard(step) {
    const board = createElement("section", "choice-board");
    board.append(createElement("h3", "", "이 단계의 선택 범위"));
    step.choices.forEach(([label, state], index) => {
      const row = createElement(
        "div",
        index === 0 ? "choice-row" : "choice-row inert-decoy",
      );
      if (index > 0) {
        row.setAttribute("aria-disabled", "true");
      }
      row.append(createElement("strong", "", label), createElement("span", "", state));
      board.append(row);
    });
    return board;
  }

  function renderInstitutionStep(moveFocus) {
    const step = service.steps[currentStepIndex];
    const fragment = document.createDocumentFragment();
    const headingGroup = createElement("div", "step-heading");
    const stepNumber = createElement(
      "p",
      "step-number",
      `${currentStepIndex + 1}단계 / ${service.steps.length}단계`,
    );
    const heading = createElement("h2", "", step.title);
    heading.id = "step-title";
    heading.tabIndex = -1;
    const guidance = createElement(
      "p",
      "step-copy",
      "아래 안내는 읽기 전용입니다. 마지막 버튼을 직접 선택해 주세요.",
    );
    headingGroup.append(stepNumber, heading, guidance);

    const notice = createElement("div", "step-notice");
    notice.append(
      createElement("strong", "", "연습 안전 안내"),
      createElement("p", "", step.notice),
    );
    const actions = createElement("div", "actions");
    const primaryButton = createActionButton(step, "action-button--primary");
    actions.append(primaryButton);
    if (step.friction) {
      actions.append(createActionButton(step.friction, "action-button--secondary"));
    }

    fragment.append(
      headingGroup,
      createGroupedSummary(step),
      createChoiceBoard(step),
      notice,
      actions,
    );
    practiceRoot.replaceChildren(fragment);
    setStatus(`${currentStepIndex + 1}단계를 연습하고 있어요.`);
    if (moveFocus) {
      heading.focus();
    }
  }

  function renderCurrent(moveFocus = true) {
    clearHighlight();
    renderInstitutionShell();
    renderProgress();
    if (isComplete) {
      renderCompletion(moveFocus);
      return;
    }
    renderInstitutionStep(moveFocus);
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
    renderCurrent();
    emitCheckpointChanged();
  }

  window.addEventListener("message", (messageEvent) => {
    if (typeof messageEvent.data !== "string" || messageEvent.data.length > 4096) {
      return;
    }
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
