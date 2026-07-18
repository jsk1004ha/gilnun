(() => {
  "use strict";

  const SCHEMA_VERSION = 1;
  const PAGE_ID = "welfare-basic-class";
  const COMPATIBLE_REVISION = "2026-07";
  const BRIDGE_NAME = "gilnun";
  const EVENT_FIELDS = [
    "schemaVersion",
    "type",
    "pageId",
    "compatibleRevision",
    "stableKey",
    "role",
    "accessibleName",
    "checkpoint",
    "monotonicMs",
  ];
  const HIGHLIGHT_FIELDS = [
    "schemaVersion",
    "command",
    "pageId",
    "compatibleRevision",
    "stableKey",
    "role",
    "accessibleName",
    "expectedState",
  ];
  const CLEAR_FIELDS = ["schemaVersion", "command"];

  const form = document.getElementById("welfare-form");
  const programNext = document.getElementById("program-next");
  const detailsBack = document.getElementById("details-back");
  const detailsNext = document.getElementById("details-next");
  const safetyCheck = document.getElementById("safety-check");
  const saveDraft = document.getElementById("save-draft");
  const reviewNext = document.getElementById("review-next");
  const startOver = document.getElementById("start-over");
  const helpRequest = document.getElementById("help-request");
  const selectedProgram = document.getElementById("selected-program");
  const status = document.getElementById("flow-status");
  const layoutBadge = document.getElementById("layout-badge");
  const steps = {
    program: document.getElementById("step-program"),
    details: document.getElementById("step-details"),
    review: document.getElementById("step-review"),
    ready: document.getElementById("step-ready"),
  };

  let currentStep = "program";
  let currentCheckpoint = "program-selection";
  let highlightedTarget = null;

  const layout = new URLSearchParams(window.location.search).get("layout") === "B" ? "B" : "A";
  document.body.classList.toggle("layout-b", layout === "B");
  layoutBadge.textContent = `배치 ${layout}`;

  function setStatus(message, isError = false) {
    status.textContent = message;
    status.classList.toggle("is-error", isError);
  }

  function selectedProgramLabel() {
    const selected = form.querySelector('input[name="program"]:checked');
    if (!selected) {
      return "선택 안 함";
    }
    return selected.value === "documents" ? "모바일 서류 연습" : "스마트폰 기초교육";
  }

  function updateIndicators(step) {
    const order = ["program", "details", "review"];
    const currentIndex = step === "ready" ? order.length : order.indexOf(step);
    document.querySelectorAll("[data-step-indicator]").forEach((item, index) => {
      item.classList.toggle("is-current", index === currentIndex);
      item.classList.toggle("is-complete", index < currentIndex);
    });
  }

  function showStep(step, checkpoint, message) {
    currentStep = step;
    currentCheckpoint = checkpoint;
    Object.entries(steps).forEach(([name, element]) => {
      element.hidden = name !== step;
    });
    updateIndicators(step);
    setStatus(message);
    window.requestAnimationFrame(refreshHighlightPosition);
  }

  function bridgeAvailable() {
    const bridge = window[BRIDGE_NAME];
    return Boolean(bridge && typeof bridge.postMessage === "function");
  }

  function eventPayload(type, target) {
    const payload = {
      schemaVersion: SCHEMA_VERSION,
      type,
      pageId: PAGE_ID,
      compatibleRevision: COMPATIBLE_REVISION,
      stableKey: target.dataset.gilnunKey,
      role: target.getAttribute("role"),
      accessibleName: target.getAttribute("aria-label"),
      checkpoint: currentCheckpoint,
      monotonicMs: Math.trunc(window.performance.now()),
    };
    if (Object.keys(payload).sort().join("|") !== [...EVENT_FIELDS].sort().join("|")) {
      return null;
    }
    return payload;
  }

  function sendEvent(type, target) {
    const payload = eventPayload(type, target);
    if (!payload || !bridgeAvailable()) {
      return false;
    }
    window[BRIDGE_NAME].postMessage(JSON.stringify(payload));
    return true;
  }

  function clearHighlight() {
    document.querySelectorAll(".gilnun-highlight").forEach((element) => {
      element.classList.remove("gilnun-highlight");
    });
    highlightedTarget = null;
  }

  function exactKeys(value, expected) {
    return (
      value !== null &&
      typeof value === "object" &&
      !Array.isArray(value) &&
      Object.keys(value).sort().join("|") === [...expected].sort().join("|")
    );
  }

  function isNonEmptyString(value, maximumLength) {
    return typeof value === "string" && value.length > 0 && value.length <= maximumLength;
  }

  function failHighlight() {
    clearHighlight();
    setStatus("안내를 안전하게 불러오지 못했습니다", true);
  }

  function resolveHighlight(command) {
    if (!exactKeys(command, HIGHLIGHT_FIELDS)) {
      failHighlight();
      return;
    }
    if (
      command.schemaVersion !== SCHEMA_VERSION ||
      command.command !== "HIGHLIGHT" ||
      command.pageId !== PAGE_ID ||
      command.compatibleRevision !== COMPATIBLE_REVISION ||
      !isNonEmptyString(command.stableKey, 64) ||
      !isNonEmptyString(command.role, 32) ||
      !isNonEmptyString(command.accessibleName, 80) ||
      !isNonEmptyString(command.expectedState, 64)
    ) {
      failHighlight();
      return;
    }

    const candidates = Array.from(document.querySelectorAll("[data-gilnun-key]")).filter(
      (element) => element.dataset.gilnunKey === command.stableKey,
    );
    if (candidates.length !== 1) {
      failHighlight();
      return;
    }

    const target = candidates[0];
    const exactMatch =
      target.getAttribute("role") === command.role &&
      target.getAttribute("aria-label") === command.accessibleName &&
      target.dataset.expectedState === command.expectedState;
    if (!exactMatch) {
      failHighlight();
      return;
    }

    clearHighlight();
    highlightedTarget = target;
    target.classList.add("gilnun-highlight");
    setStatus("찾아야 할 버튼을 한 곳만 표시했습니다.");
    refreshHighlightPosition();
  }

  function refreshHighlightPosition() {
    if (!highlightedTarget || highlightedTarget.offsetParent === null) {
      return;
    }
    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    highlightedTarget.scrollIntoView({
      block: "center",
      inline: "nearest",
      behavior: reducedMotion ? "auto" : "smooth",
    });
  }

  form.querySelectorAll('input[name="program"]').forEach((input) => {
    input.addEventListener("change", () => {
      programNext.disabled = false;
      selectedProgram.textContent = selectedProgramLabel();
      setStatus("선택했습니다. 필수 항목 확인으로 이동할 수 있어요.");
    });
  });

  programNext.addEventListener("click", () => {
    if (!form.querySelector('input[name="program"]:checked')) {
      setStatus("프로그램 하나를 먼저 선택해 주세요.", true);
      return;
    }
    showStep("details", "details-check", "합성 연습 범위를 읽고 확인해 주세요.");
  });

  safetyCheck.addEventListener("change", () => {
    detailsNext.disabled = !safetyCheck.checked;
    setStatus(
      safetyCheck.checked
        ? "확인했습니다. 선택 내용 요약을 볼 수 있어요."
        : "합성 연습 범위를 확인해 주세요.",
    );
  });

  detailsBack.addEventListener("click", () => {
    showStep("program", "program-selection", "프로그램 선택으로 돌아왔습니다.");
  });

  detailsNext.addEventListener("click", () => {
    if (!safetyCheck.checked) {
      setStatus("합성 연습 범위를 먼저 확인해 주세요.", true);
      return;
    }
    selectedProgram.textContent = selectedProgramLabel();
    showStep("review", "consent-ready", "다음 단계는 “신청 내용 확인”입니다.");
  });

  saveDraft.addEventListener("click", () => {
    currentCheckpoint = "consent-ready";
    setStatus("임시 저장을 눌렀습니다. 진행 상태는 그대로예요.");
    sendEvent("TARGET_TAP", saveDraft);
  });

  reviewNext.addEventListener("click", () => {
    showStep(
      "ready",
      "review-ready",
      "신청 내용 확인 화면에 도착했습니다. 최종 신청은 이루어지지 않았습니다.",
    );
    sendEvent("TARGET_TAP", reviewNext);
  });

  helpRequest.addEventListener("click", () => {
    helpRequest.dataset.checkpoint = currentCheckpoint;
    if (sendEvent("HELP_REQUEST", helpRequest)) {
      setStatus("도움 요청을 보냈습니다. 자동으로 누르거나 제출하지 않습니다.");
    } else {
      setStatus("이 기기에서는 길눈 도움 기능을 사용할 수 없습니다. 연습은 직접 계속할 수 있어요.");
    }
  });

  startOver.addEventListener("click", () => {
    clearHighlight();
    form.reset();
    programNext.disabled = true;
    detailsNext.disabled = true;
    selectedProgram.textContent = "스마트폰 기초교육";
    showStep("program", "program-selection", "프로그램 하나를 선택해 주세요.");
  });

  window.addEventListener("message", (event) => {
    if (typeof event.data !== "string" || event.data.length > 4096) {
      return;
    }
    let command;
    try {
      command = JSON.parse(event.data);
    } catch {
      failHighlight();
      return;
    }
    if (
      exactKeys(command, CLEAR_FIELDS) &&
      command.schemaVersion === SCHEMA_VERSION &&
      command.command === "CLEAR_HIGHLIGHT"
    ) {
      clearHighlight();
      setStatus("강조 안내를 지웠습니다.");
      return;
    }
    resolveHighlight(command);
  });

  window.addEventListener("resize", () => {
    window.requestAnimationFrame(refreshHighlightPosition);
  });

  showStep(currentStep, currentCheckpoint, "프로그램 하나를 선택해 주세요.");
})();
