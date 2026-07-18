"use client";

import { useEffect, useRef, useState } from "react";

type RoleMode = "learner" | "helper";
type LayoutMode = "A" | "B";
type Checkpoint = "consent-ready" | "review-ready";
type ReplayState = "idle" | "matched" | "blocked" | "verified";

type SemanticAction = {
  pageId: "welfare-basic-class";
  compatibleRevision: "2026-07";
  stableKey: "save-draft" | "review-next";
  role: "button";
  accessibleName: string;
  expectedState: Checkpoint;
};

type PatchV1 = {
  pageId: "welfare-basic-class";
  compatibleRevision: "2026-07";
  stableKey: "review-next";
  role: "button";
  accessibleName: "신청 내용 확인";
  expectedState: "review-ready";
};

type ActionContract = {
  guidanceShown: boolean;
  userActionObserved: boolean;
  postconditionVerified: boolean;
};

type VerifiedReceipt = {
  status: "VERIFIED";
  stableKey: "review-next";
  checkpoint: "review-ready";
  patchVersion: "PatchV1";
  verifiedAt: string;
  round: number;
};

const PAGE_ID = "welfare-basic-class";
const REVISION = "2026-07";
const HELP_COOLDOWN_MS = 30_000;
const TAP_WINDOW_MS = 6_000;

const SAVE_ACTION = {
  pageId: PAGE_ID,
  compatibleRevision: REVISION,
  stableKey: "save-draft",
  role: "button",
  accessibleName: "임시 저장",
  expectedState: "consent-ready",
} satisfies SemanticAction;

const REVIEW_ACTION = {
  pageId: PAGE_ID,
  compatibleRevision: REVISION,
  stableKey: "review-next",
  role: "button",
  accessibleName: "신청 내용 확인",
  expectedState: "review-ready",
} satisfies SemanticAction;

const AVAILABLE_ACTIONS: readonly SemanticAction[] = [
  SAVE_ACTION,
  REVIEW_ACTION,
];

const EMPTY_CONTRACT: ActionContract = {
  guidanceShown: false,
  userActionObserved: false,
  postconditionVerified: false,
};

function findExactSemanticMatch(
  patch: PatchV1,
  actions: readonly SemanticAction[],
) {
  return actions.filter(
    (action) =>
      action.pageId === patch.pageId &&
      action.compatibleRevision === patch.compatibleRevision &&
      action.stableKey === patch.stableKey &&
      action.role === patch.role &&
      action.accessibleName === patch.accessibleName &&
      action.expectedState === patch.expectedState,
  );
}

function BooleanState({
  label,
  value,
}: {
  label: keyof ActionContract;
  value: boolean;
}) {
  return (
    <li className={value ? "contract-row is-true" : "contract-row"}>
      <span className="contract-indicator" aria-hidden="true">
        {value ? "✓" : "–"}
      </span>
      <code>{label}</code>
      <strong>{value ? "TRUE" : "FALSE"}</strong>
    </li>
  );
}

export default function Home() {
  const [roleMode, setRoleMode] = useState<RoleMode>("learner");
  const [layoutMode, setLayoutMode] = useState<LayoutMode>("A");
  const [checkpoint, setCheckpoint] =
    useState<Checkpoint>("consent-ready");
  const [selectedTarget, setSelectedTarget] = useState<
    SemanticAction["stableKey"] | null
  >(null);
  const [patch, setPatch] = useState<PatchV1 | null>(null);
  const [guidedTarget, setGuidedTarget] = useState<
    SemanticAction["stableKey"] | null
  >(null);
  const [replayState, setReplayState] = useState<ReplayState>("idle");
  const [contract, setContract] =
    useState<ActionContract>(EMPTY_CONTRACT);
  const [receipt, setReceipt] = useState<VerifiedReceipt | null>(null);
  const [helpLevel, setHelpLevel] = useState(3);
  const [requestCount, setRequestCount] = useState(0);
  const [frictionCount, setFrictionCount] = useState(0);
  const [failureCount, setFailureCount] = useState(0);
  const [round, setRound] = useState(1);
  const [tapCount, setTapCount] = useState(0);
  const [helpDialogOpen, setHelpDialogOpen] = useState(false);
  const [helpSource, setHelpSource] = useState<"direct" | "friction">(
    "direct",
  );
  const [cooldownUntil, setCooldownUntil] = useState(0);
  const [cooldownSeconds, setCooldownSeconds] = useState(0);
  const [announcement, setAnnouncement] = useState(
    "학습자 모드, 동의 준비 단계에서 시작합니다.",
  );

  const tapWindowRef = useRef({
    stableKey: "" as SemanticAction["stableKey"] | "",
    count: 0,
    startedAt: 0,
  });
  const acceptHelpRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!helpDialogOpen) return;
    acceptHelpRef.current?.focus();
  }, [helpDialogOpen]);

  useEffect(() => {
    if (!cooldownUntil) {
      setCooldownSeconds(0);
      return;
    }

    const updateCooldown = () => {
      setCooldownSeconds(
        Math.max(0, Math.ceil((cooldownUntil - Date.now()) / 1000)),
      );
    };

    updateCooldown();
    const timer = window.setInterval(updateCooldown, 500);
    return () => window.clearInterval(timer);
  }, [cooldownUntil]);

  useEffect(() => {
    if (tapCount === 0) return;

    const elapsed = Date.now() - tapWindowRef.current.startedAt;
    const timer = window.setTimeout(() => {
      tapWindowRef.current = {
        stableKey: "",
        count: 0,
        startedAt: 0,
      };
      setTapCount(0);
    }, Math.max(0, TAP_WINDOW_MS - elapsed) + 30);

    return () => window.clearTimeout(timer);
  }, [tapCount]);

  const openHelpConfirmation = (source: "direct" | "friction") => {
    if (helpDialogOpen) return;

    if (Date.now() < cooldownUntil) {
      setAnnouncement(
        `도움 제안을 잠시 쉬고 있습니다. ${Math.max(
          1,
          Math.ceil((cooldownUntil - Date.now()) / 1000),
        )}초 뒤 다시 요청할 수 있습니다.`,
      );
      return;
    }

    setHelpSource(source);
    setHelpDialogOpen(true);
    setAnnouncement("도움이 필요한지 한 번만 확인합니다.");
  };

  const handleSaveDraft = () => {
    if (roleMode === "helper") {
      setAnnouncement(
        "save-draft는 진행을 만들지 않아 도움 대상으로 저장할 수 없습니다.",
      );
      return;
    }

    setAnnouncement(
      "임시 저장됨: 체크포인트는 consent-ready에 그대로 머뭅니다.",
    );

    if (checkpoint !== "consent-ready") return;

    const now = Date.now();
    const previous = tapWindowRef.current;
    const continuesWindow =
      previous.stableKey === SAVE_ACTION.stableKey &&
      now - previous.startedAt <= TAP_WINDOW_MS;
    const nextCount = continuesWindow ? previous.count + 1 : 1;

    tapWindowRef.current = {
      stableKey: SAVE_ACTION.stableKey,
      count: nextCount,
      startedAt: continuesWindow ? previous.startedAt : now,
    };
    setTapCount(nextCount);

    if (nextCount < 3) return;

    tapWindowRef.current = {
      stableKey: "",
      count: 0,
      startedAt: 0,
    };
    setTapCount(0);

    if (Date.now() < cooldownUntil || helpDialogOpen) return;

    setFrictionCount((count) => count + 1);
    openHelpConfirmation("friction");
  };

  const handleReviewNext = () => {
    if (roleMode === "helper") {
      setSelectedTarget(REVIEW_ACTION.stableKey);
      setAnnouncement(
        "review-next를 의미 기반 도움 대상으로 선택했습니다. 아직 실행하지 않았습니다.",
      );
      return;
    }

    if (checkpoint === "review-ready") {
      setAnnouncement("이미 신청 내용 확인 단계에 도착했습니다.");
      return;
    }

    const nextCheckpoint: Checkpoint = "review-ready";
    setCheckpoint(nextCheckpoint);
    tapWindowRef.current = {
      stableKey: "",
      count: 0,
      startedAt: 0,
    };
    setTapCount(0);

    const isVerifiedReplay =
      replayState === "matched" &&
      guidedTarget === REVIEW_ACTION.stableKey &&
      patch !== null &&
      findExactSemanticMatch(patch, AVAILABLE_ACTIONS).length === 1;

    if (!isVerifiedReplay) {
      setAnnouncement(
        "신청 내용 확인 단계로 이동했습니다. 활성 도움 계약이 없어 검증 영수증은 만들지 않습니다.",
      );
      return;
    }

    setContract({
      guidanceShown: true,
      userActionObserved: true,
      postconditionVerified: nextCheckpoint === "review-ready",
    });
    setReplayState("verified");

    if (!receipt && nextCheckpoint === "review-ready") {
      setReceipt({
        status: "VERIFIED",
        stableKey: REVIEW_ACTION.stableKey,
        checkpoint: nextCheckpoint,
        patchVersion: "PatchV1",
        verifiedAt: new Intl.DateTimeFormat("ko-KR", {
          hour: "2-digit",
          minute: "2-digit",
          second: "2-digit",
        }).format(new Date()),
        round,
      });
      setHelpLevel((level) => Math.max(0, level - 1));
    }

    setAnnouncement(
      "사용자의 직접 클릭과 review-ready 도착을 확인해 VERIFIED 영수증을 만들었습니다.",
    );
  };

  const acceptHelp = () => {
    setHelpDialogOpen(false);
    setRequestCount((count) => count + 1);
    setRoleMode("helper");
    setSelectedTarget(null);
    setReplayState("idle");
    setGuidedTarget(null);
    setContract(EMPTY_CONTRACT);
    setAnnouncement(
      "도움 요청을 수락했습니다. 도움 주는 사람 모드에서 review-next를 선택하세요.",
    );
  };

  const rejectHelp = () => {
    const until = Date.now() + HELP_COOLDOWN_MS;
    setHelpDialogOpen(false);
    setCooldownUntil(until);
    setCooldownSeconds(Math.ceil(HELP_COOLDOWN_MS / 1000));
    setAnnouncement(
      "도움 제안을 닫았습니다. 30초 동안 같은 제안을 다시 띄우지 않습니다.",
    );
  };

  const savePatch = () => {
    if (roleMode !== "helper" || selectedTarget !== "review-next") return;

    const nextPatch: PatchV1 = {
      ...REVIEW_ACTION,
    };
    setPatch(nextPatch);
    setGuidedTarget(null);
    setReplayState("idle");
    setContract(EMPTY_CONTRACT);
    setAnnouncement(
      "PatchV1을 세션 메모리에만 저장했습니다. B 레이아웃에서 의미 일치를 재생할 수 있습니다.",
    );
  };

  const replayPatchInLayoutB = () => {
    if (!patch) return;

    setLayoutMode("B");
    setRoleMode("learner");
    setSelectedTarget(null);

    const matches = findExactSemanticMatch(patch, AVAILABLE_ACTIONS);
    if (matches.length !== 1) {
      setReplayState("blocked");
      setGuidedTarget(null);
      setContract(EMPTY_CONTRACT);
      setFailureCount((count) => count + 1);
      setAnnouncement(
        "정확한 의미 대상이 하나가 아니어서 재생을 중단했습니다. 어떤 동작도 강조하거나 실행하지 않았습니다.",
      );
      return;
    }

    setReplayState("matched");
    setGuidedTarget(matches[0].stableKey);
    setContract({
      guidanceShown: true,
      userActionObserved: false,
      postconditionVerified: false,
    });
    setAnnouncement(
      "B 레이아웃에서 유일한 정확 일치를 찾았습니다. 강조된 버튼은 사용자가 직접 눌러야 합니다.",
    );
  };

  const demonstrateFailClosed = () => {
    if (!patch) return;

    const incompatiblePatch = {
      ...patch,
      compatibleRevision: "2026-06",
    };
    const matches = AVAILABLE_ACTIONS.filter(
      (action) =>
        action.pageId === incompatiblePatch.pageId &&
        action.compatibleRevision === incompatiblePatch.compatibleRevision &&
        action.stableKey === incompatiblePatch.stableKey &&
        action.role === incompatiblePatch.role &&
        action.accessibleName === incompatiblePatch.accessibleName &&
        action.expectedState === incompatiblePatch.expectedState,
    );

    if (matches.length !== 1) {
      setReplayState("blocked");
      setGuidedTarget(null);
      setContract(EMPTY_CONTRACT);
      setFailureCount((count) => count + 1);
      setAnnouncement(
        "검증용 리비전 불일치를 감지했습니다. 실패 횟수만 1 올리고 안전하게 중단했습니다.",
      );
    }
  };

  const changeRole = (nextRole: RoleMode) => {
    setRoleMode(nextRole);
    setSelectedTarget(null);

    if (nextRole === "helper") {
      setGuidedTarget(null);
      setReplayState("idle");
      setContract(EMPTY_CONTRACT);
      setAnnouncement(
        "도움 주는 사람 모드입니다. 화면의 신청 내용 확인 버튼을 눌러 의미 대상을 선택하세요.",
      );
    } else {
      setAnnouncement(
        "학습자 모드입니다. 신청 화면의 동작은 사용자가 직접 실행합니다.",
      );
    }
  };

  const startNextRound = () => {
    setRound((value) => value + 1);
    setRoleMode("learner");
    setLayoutMode("A");
    setCheckpoint("consent-ready");
    setSelectedTarget(null);
    setGuidedTarget(null);
    setReplayState("idle");
    setContract(EMPTY_CONTRACT);
    setReceipt(null);
    setTapCount(0);
    tapWindowRef.current = {
      stableKey: "",
      count: 0,
      startedAt: 0,
    };
    setAnnouncement(
      "PatchV1, 측정값, 도움 단계는 유지하고 새 연습 회차를 시작했습니다.",
    );
  };

  const resetDemo = () => {
    setRoleMode("learner");
    setLayoutMode("A");
    setCheckpoint("consent-ready");
    setSelectedTarget(null);
    setPatch(null);
    setGuidedTarget(null);
    setReplayState("idle");
    setContract(EMPTY_CONTRACT);
    setReceipt(null);
    setHelpLevel(3);
    setRequestCount(0);
    setFrictionCount(0);
    setFailureCount(0);
    setRound(1);
    setTapCount(0);
    setHelpDialogOpen(false);
    setCooldownUntil(0);
    setCooldownSeconds(0);
    tapWindowRef.current = {
      stableKey: "",
      count: 0,
      startedAt: 0,
    };
    setAnnouncement("데모를 초기 상태로 되돌렸습니다.");
  };

  const replayLabel = {
    idle: "대기",
    matched: "정확 일치",
    blocked: "안전 중단",
    verified: "검증 완료",
  }[replayState];

  return (
    <div className="site-shell">
      <a className="skip-link" href="#demo-main">
        본문 바로가기
      </a>

      <header className="topbar">
        <div className="brand-lockup" aria-label="길눈 AI">
          <span className="brand-waypoint" aria-hidden="true" />
          <span className="brand-name">길눈 AI</span>
          <span className="brand-caption">안전한 복지 신청 길찾기</span>
        </div>
        <div className="topbar-actions">
          <span className="local-badge">로컬 시연 · 저장 안 함</span>
          <button className="reset-button" type="button" onClick={resetDemo}>
            Demo Reset
          </button>
        </div>
      </header>

      <main id="demo-main">
        <section className="control-deck" aria-labelledby="demo-title">
          <div className="title-block">
            <p className="eyebrow">통제된 합성 연습 · {round}회차</p>
            <h1 id="demo-title">기초생활보장 모의 신청</h1>
            <p>
              실제 개인정보와 최종 제출 없이, 막힌 지점만 의미로 기록해
              다른 화면 배치에서 다시 안내합니다.
            </p>
          </div>

          <div className="mode-controls">
            <fieldset className="segmented-control">
              <legend>역할</legend>
              <div className="segment-buttons">
                <button
                  type="button"
                  aria-pressed={roleMode === "learner"}
                  onClick={() => changeRole("learner")}
                >
                  학습자
                </button>
                <button
                  type="button"
                  aria-pressed={roleMode === "helper"}
                  onClick={() => changeRole("helper")}
                >
                  도움 주는 사람
                </button>
              </div>
            </fieldset>

            <fieldset className="segmented-control layout-control">
              <legend>화면 배치</legend>
              <div className="segment-buttons">
                {(["A", "B"] as const).map((mode) => (
                  <button
                    type="button"
                    key={mode}
                    aria-pressed={layoutMode === mode}
                    onClick={() => {
                      setLayoutMode(mode);
                      setAnnouncement(
                        `레이아웃 ${mode}로 바꿨습니다. 내용과 의미는 같고 순서, 위치, 글자 크기만 다릅니다.`,
                      );
                    }}
                  >
                    레이아웃 {mode}
                  </button>
                ))}
              </div>
            </fieldset>
          </div>

          <div className="metric-board" aria-label="연습 측정값">
            <div className="level-meter">
              <span>현재 도움 단계</span>
              <strong>레벨 {helpLevel}</strong>
              <div className="level-track" aria-label={`도움 레벨 ${helpLevel}`}>
                {[3, 2, 1, 0].map((level) => (
                  <span
                    key={level}
                    className={
                      level === helpLevel
                        ? "level-stop is-current"
                        : "level-stop"
                    }
                  >
                    {level}
                  </span>
                ))}
              </div>
              <small>VERIFIED 1회마다 3 → 2 → 1 → 0</small>
            </div>
            <dl className="counter-list">
              <div>
                <dt>요청</dt>
                <dd>{requestCount}</dd>
              </div>
              <div>
                <dt>마찰</dt>
                <dd>{frictionCount}</dd>
              </div>
              <div>
                <dt>실패</dt>
                <dd>{failureCount}</dd>
              </div>
            </dl>
          </div>
        </section>

        <nav className="journey-path" aria-label="연습 체크포인트">
          <ol>
            <li
              className={
                checkpoint === "consent-ready"
                  ? "waypoint is-current"
                  : "waypoint is-complete"
              }
            >
              <span aria-hidden="true">1</span>
              <div>
                <strong>동의 준비</strong>
                <code>consent-ready</code>
              </div>
            </li>
            <li
              className={
                replayState === "matched"
                  ? "waypoint is-current"
                  : replayState === "verified"
                    ? "waypoint is-complete"
                    : "waypoint"
              }
            >
              <span aria-hidden="true">2</span>
              <div>
                <strong>의미 안내</strong>
                <code>PatchV1</code>
              </div>
            </li>
            <li
              className={
                checkpoint === "review-ready"
                  ? "waypoint is-current"
                  : "waypoint"
              }
            >
              <span aria-hidden="true">3</span>
              <div>
                <strong>내용 확인</strong>
                <code>review-ready</code>
              </div>
            </li>
          </ol>
        </nav>

        <div className="workspace-grid">
          <section
            className={`application-panel layout-${layoutMode.toLowerCase()}`}
            aria-labelledby="application-title"
          >
            <header className="panel-heading">
              <div>
                <p className="panel-kicker">가상 신청 화면</p>
                <h2 id="application-title">복지 기본 분류</h2>
              </div>
              <div className="panel-meta">
                <span>레이아웃 {layoutMode}</span>
                <code>{checkpoint}</code>
              </div>
            </header>

            <div className="safety-notice">
              <span aria-hidden="true">가상</span>
              <p>
                모든 항목은 합성된 연습 값입니다. 로그인, 외부 전송, 최종
                제출은 없습니다.
              </p>
            </div>

            <div className="application-surface">
              <section className="app-block applicant-block">
                <div className="block-heading">
                  <span>01</span>
                  <div>
                    <h3>신청 기본 정보</h3>
                    <p>실제 사람을 나타내지 않는 연습용 값</p>
                  </div>
                </div>
                <dl className="synthetic-fields">
                  <div>
                    <dt>가구 이름</dt>
                    <dd>가상 가구 A</dd>
                  </div>
                  <div>
                    <dt>가구 구성</dt>
                    <dd>2인 가구 · 연습값</dd>
                  </div>
                  <div>
                    <dt>신청 유형</dt>
                    <dd>기초생활보장 모의 분류</dd>
                  </div>
                </dl>
              </section>

              <section className="app-block consent-block">
                <div className="block-heading">
                  <span>02</span>
                  <div>
                    <h3>연습 정보 확인</h3>
                    <p>현재 체크포인트의 준비 조건</p>
                  </div>
                </div>
                <div className="consent-check">
                  <span className="check-box" aria-hidden="true">
                    ✓
                  </span>
                  <div>
                    <strong>합성 정보 사용에 동의함</strong>
                    <small>실제 개인정보는 입력하지 않습니다.</small>
                  </div>
                </div>
                <p className="checkpoint-note">
                  준비됨 <code>consent-ready</code>
                </p>
              </section>

              <div className="application-actions">
                <div className="action-wrap save-action">
                  <button
                    type="button"
                    role="button"
                    aria-label="임시 저장"
                    data-stable-key="save-draft"
                    onClick={handleSaveDraft}
                  >
                    <span>임시 저장</span>
                    <code aria-hidden="true">save-draft</code>
                  </button>
                  <small>저장만 하며 다음 단계로 가지 않음</small>
                </div>

                <div
                  className={`action-wrap review-action ${
                    guidedTarget === "review-next" ? "is-guided" : ""
                  } ${
                    selectedTarget === "review-next" ? "is-selected" : ""
                  }`}
                >
                  {guidedTarget === "review-next" && (
                    <span
                      className="guidance-flag"
                      id="review-guidance"
                      aria-hidden="true"
                    >
                      직접 눌러 다음 길로
                    </span>
                  )}
                  <button
                    type="button"
                    role="button"
                    aria-label="신청 내용 확인"
                    aria-describedby={
                      guidedTarget === "review-next"
                        ? "review-guidance"
                        : undefined
                    }
                    data-stable-key="review-next"
                    data-page-id={PAGE_ID}
                    data-compatible-revision={REVISION}
                    data-expected-state="review-ready"
                    onClick={handleReviewNext}
                  >
                    <span>신청 내용 확인</span>
                    <code aria-hidden="true">review-next</code>
                  </button>
                  <small>
                    {roleMode === "helper"
                      ? selectedTarget === "review-next"
                        ? "도움 대상으로 선택됨"
                        : "눌러서 의미 대상 선택"
                      : "사용자가 직접 눌러 review-ready로 이동"}
                  </small>
                </div>
              </div>
            </div>

            <footer className="application-footer">
              <span className="state-lamp" aria-hidden="true" />
              <p>
                <strong>현재 상태</strong>
                {checkpoint === "consent-ready"
                  ? "동의 준비 완료 · 아직 검토 전"
                  : "신청 내용 확인 준비 완료"}
              </p>
              <code>{PAGE_ID} · {REVISION}</code>
            </footer>
          </section>

          <aside className="guidance-rail" aria-label="도움 계약과 검증">
            <section className="rail-card help-station">
              <div className="rail-card-heading">
                <span className="station-number">도움 정류장</span>
                <strong>
                  {roleMode === "learner"
                    ? "막혔을 때만 멈춰 묻기"
                    : "정답 대신 다음 동작을 기록"}
                </strong>
              </div>
              <p>
                {roleMode === "learner"
                  ? "consent-ready에서 ‘임시 저장’을 6초 안에 세 번 누르면 확인창을 한 번만 띄웁니다."
                  : "신청 화면에서 ‘신청 내용 확인’을 선택한 뒤 PatchV1을 저장하세요."}
              </p>
              <div className="tap-meter" aria-label={`같은 동작 ${tapCount}회 감지`}>
                {[1, 2, 3].map((step) => (
                  <span
                    key={step}
                    className={tapCount >= step ? "is-filled" : ""}
                  />
                ))}
                <small>{tapCount}/3 · 6초 창</small>
              </div>
              <button
                className="direct-help-button"
                type="button"
                onClick={() => openHelpConfirmation("direct")}
                disabled={cooldownSeconds > 0}
              >
                {cooldownSeconds > 0
                  ? `도움 제안 쉬는 중 · ${cooldownSeconds}초`
                  : "직접 도움 요청"}
              </button>
            </section>

            <section className="rail-card patch-card">
              <div className="rail-card-heading inline-heading">
                <div>
                  <span className="station-number">의미 패치</span>
                  <strong>{patch ? "PatchV1 저장됨" : "아직 패치 없음"}</strong>
                </div>
                <span className={patch ? "status-dot is-ready" : "status-dot"} />
              </div>

              {!patch ? (
                <>
                  <p>
                    좌표나 화면 순서를 저장하지 않습니다. 도움 주는 사람은
                    정확한 동작 하나만 고릅니다.
                  </p>
                  <dl className="semantic-preview">
                    <div>
                      <dt>pageId</dt>
                      <dd>{PAGE_ID}</dd>
                    </div>
                    <div>
                      <dt>compatibleRevision</dt>
                      <dd>{REVISION}</dd>
                    </div>
                    <div>
                      <dt>stableKey</dt>
                      <dd>review-next</dd>
                    </div>
                    <div>
                      <dt>role</dt>
                      <dd>button</dd>
                    </div>
                    <div>
                      <dt>accessibleName</dt>
                      <dd>신청 내용 확인</dd>
                    </div>
                    <div>
                      <dt>expectedState</dt>
                      <dd>review-ready</dd>
                    </div>
                  </dl>
                  <button
                    className="primary-rail-button"
                    type="button"
                    onClick={savePatch}
                    disabled={
                      roleMode !== "helper" ||
                      selectedTarget !== "review-next"
                    }
                  >
                    PatchV1 저장
                  </button>
                </>
              ) : (
                <>
                  <dl className="semantic-preview complete">
                    <div>
                      <dt>pageId</dt>
                      <dd>{patch.pageId}</dd>
                    </div>
                    <div>
                      <dt>compatibleRevision</dt>
                      <dd>{patch.compatibleRevision}</dd>
                    </div>
                    <div>
                      <dt>stableKey</dt>
                      <dd>{patch.stableKey}</dd>
                    </div>
                    <div>
                      <dt>role</dt>
                      <dd>{patch.role}</dd>
                    </div>
                    <div>
                      <dt>accessibleName</dt>
                      <dd>{patch.accessibleName}</dd>
                    </div>
                    <div>
                      <dt>expectedState</dt>
                      <dd>{patch.expectedState}</dd>
                    </div>
                  </dl>
                  <button
                    className="primary-rail-button"
                    type="button"
                    onClick={replayPatchInLayoutB}
                  >
                    B로 전환 · 도움 재생
                  </button>
                  <button
                    className="text-rail-button"
                    type="button"
                    onClick={demonstrateFailClosed}
                  >
                    불일치 안전 중단 확인
                  </button>
                </>
              )}
            </section>

            <section className="rail-card contract-card">
              <div className="rail-card-heading inline-heading">
                <div>
                  <span className="station-number">Action Contract</span>
                  <strong>동작과 결과를 따로 확인</strong>
                </div>
                <span
                  className={`replay-badge replay-${replayState}`}
                  aria-label={`재생 상태 ${replayLabel}`}
                >
                  {replayLabel}
                </span>
              </div>
              <ul className="contract-list">
                <BooleanState
                  label="guidanceShown"
                  value={contract.guidanceShown}
                />
                <BooleanState
                  label="userActionObserved"
                  value={contract.userActionObserved}
                />
                <BooleanState
                  label="postconditionVerified"
                  value={contract.postconditionVerified}
                />
              </ul>
              <p className="fail-closed-note">
                6개 의미 조건이 모두 같고 대상이 정확히 1개일 때만
                강조합니다. 0개 또는 2개 이상이면 안전 중단합니다.
              </p>
            </section>

            <section
              className={`rail-card receipt-card ${receipt ? "has-receipt" : ""}`}
              aria-labelledby="receipt-title"
            >
              <div className="rail-card-heading inline-heading">
                <div>
                  <span className="station-number">검증 영수증</span>
                  <strong id="receipt-title">
                    {receipt ? "VERIFIED" : "발급 전"}
                  </strong>
                </div>
                <span className="receipt-seal" aria-hidden="true">
                  {receipt ? "V" : "—"}
                </span>
              </div>
              {receipt ? (
                <>
                  <dl className="receipt-data">
                    <div>
                      <dt>사용자 동작</dt>
                      <dd>{receipt.stableKey}</dd>
                    </div>
                    <div>
                      <dt>확인 상태</dt>
                      <dd>{receipt.checkpoint}</dd>
                    </div>
                    <div>
                      <dt>기록</dt>
                      <dd>
                        {receipt.round}회차 · {receipt.verifiedAt}
                      </dd>
                    </div>
                  </dl>
                  <p>
                    안내만으로는 발급되지 않습니다. 사용자의 직접 클릭 뒤
                    review-ready가 확인되어 발급됐습니다.
                  </p>
                  <button
                    className="primary-rail-button next-round-button"
                    type="button"
                    onClick={startNextRound}
                  >
                    PatchV1 유지 · 다음 연습
                  </button>
                </>
              ) : (
                <p>
                  <code>guidanceShown</code>, <code>userActionObserved</code>,{" "}
                  <code>postconditionVerified</code>가 분리되어 남습니다.
                </p>
              )}
            </section>
          </aside>
        </div>
      </main>

      <div className="announcement" role="status" aria-live="polite">
        {announcement}
      </div>

      {helpDialogOpen && (
        <div className="dialog-scrim">
          <section
            className="help-dialog"
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="help-dialog-title"
            aria-describedby="help-dialog-description"
            onKeyDown={(event) => {
              if (event.key === "Escape") {
                event.preventDefault();
                rejectHelp();
                return;
              }

              if (event.key !== "Tab") return;
              const controls = Array.from(
                event.currentTarget.querySelectorAll<HTMLButtonElement>(
                  "button:not([disabled])",
                ),
              );
              const first = controls[0];
              const last = controls.at(-1);

              if (!first || !last) return;
              if (event.shiftKey && document.activeElement === first) {
                event.preventDefault();
                last.focus();
              } else if (
                !event.shiftKey &&
                document.activeElement === last
              ) {
                event.preventDefault();
                first.focus();
              }
            }}
          >
            <span className="dialog-waypoint" aria-hidden="true" />
            <p className="eyebrow">
              {helpSource === "friction"
                ? "같은 동작 3회 감지"
                : "직접 도움 요청"}
            </p>
            <h2 id="help-dialog-title">도움이 필요하신가요?</h2>
            <p id="help-dialog-description">
              도움 주는 사람은 다음 동작의 의미만 기록합니다. 대신
              클릭하거나 신청을 제출하지 않습니다.
            </p>
            <div className="dialog-actions">
              <button type="button" className="dialog-decline" onClick={rejectHelp}>
                지금은 괜찮아요
              </button>
              <button
                ref={acceptHelpRef}
                type="button"
                className="dialog-accept"
                onClick={acceptHelp}
              >
                도움 받기
              </button>
            </div>
            <small>거절하면 30초 동안 같은 제안을 쉬어갑니다.</small>
          </section>
        </div>
      )}
    </div>
  );
}
