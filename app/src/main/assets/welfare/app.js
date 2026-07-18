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
        message: "실제 신청 처리 없이 세 단계의 연습만 마쳤습니다.",
      },
    },
    "resident-record": {
      serviceId: "resident-record",
      pageId: "gov24-resident-record",
      revision: REVISION,
      title: "주민등록표 등본 발급 연습",
      description: "실제 문서가 아닌 모의 화면으로 발급 순서를 연습합니다.",
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
        },
      ],
      completion: {
        checkpoint: "resident-complete",
        title: "발급 연습을 마쳤어요",
        message: "실제 문서 발급 없이 세 단계의 연습만 마쳤습니다.",
      },
    },
    "health-screening": {
      serviceId: "health-screening",
      pageId: "nhis-health-screening",
      revision: REVISION,
      title: "건강검진 대상 조회 연습",
      description: "개인정보나 진료 정보 없이 모의 조회 순서만 연습합니다.",
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

  const serviceTitle = document.getElementById("service-title");
  const serviceDescription = document.getElementById("service-description");
  const progressList = document.getElementById("progress-list");
  const practiceRoot = document.getElementById("practice-root");
  const practiceStatus = document.getElementById("practice-status");

  let currentStepIndex = 0;
  let currentCheckpoint = service.steps[0].checkpoint;
  let isComplete = false;
  let highlightedTarget = null;

  document.body.classList.toggle("layout-b", layout === "B");
  document.documentElement.dataset.pageId = service.pageId;
  serviceTitle.textContent = service.title;
  serviceDescription.textContent = service.description;

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
    setStatus("길눈이 선택할 곳을 한 군데 표시했어요.");
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
    const mark = createElement("div", "completion-mark", "✓");
    mark.setAttribute("aria-hidden", "true");
    const heading = createElement("h2", "", service.completion.title);
    heading.id = "step-title";
    heading.tabIndex = -1;
    const message = createElement("p", "step-copy", service.completion.message);
    card.append(mark, heading, message);

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

  function renderStep(moveFocus) {
    const step = service.steps[currentStepIndex];
    const fragment = document.createDocumentFragment();
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
      "아래 내용을 읽고 알맞은 버튼을 직접 선택해 주세요.",
    );
    const context = createElement(
      "div",
      step.watermark ? "document-preview" : "context-card",
    );
    const contextLabel = createElement("strong", "", step.contextLabel);
    const contextText = createElement("p", "", step.contextText);
    context.append(contextLabel, contextText);
    if (step.watermark) {
      context.append(createElement("span", "document-preview__watermark", step.watermark));
    }

    const actions = createElement("div", "actions");
    actions.append(createActionButton(step, "action-button--primary"));
    if (step.friction) {
      actions.append(createActionButton(step.friction, "action-button--secondary"));
    }

    fragment.append(stepNumber, heading, guidance, context, actions);
    practiceRoot.replaceChildren(fragment);
    setStatus(`${currentStepIndex + 1}단계를 연습하고 있어요.`);
    if (moveFocus) {
      heading.focus();
    }
  }

  function renderCurrent(moveFocus = true) {
    clearHighlight();
    renderProgress();
    if (isComplete) {
      renderCompletion(moveFocus);
      return;
    }
    renderStep(moveFocus);
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
