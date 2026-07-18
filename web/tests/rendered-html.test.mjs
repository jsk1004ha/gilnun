import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

async function render() {
  const workerUrl = new URL("../dist/server/index.js", import.meta.url);
  workerUrl.searchParams.set("test", `${process.pid}-${Date.now()}`);
  const { default: worker } = await import(workerUrl.href);

  return worker.fetch(
    new Request("http://localhost/", {
      headers: { accept: "text/html" },
    }),
    {
      ASSETS: {
        fetch: async () => new Response("Not found", { status: 404 }),
      },
    },
    {
      waitUntil() {},
      passThroughOnException() {},
    },
  );
}

test("server-renders the interactive Gilnun demo", async () => {
  const response = await render();
  assert.equal(response.status, 200);
  assert.match(response.headers.get("content-type") ?? "", /^text\/html\b/i);

  const html = await response.text();
  assert.match(html, /길눈 AI/);
  assert.match(html, /기초생활보장 모의 신청/);
  assert.match(html, /Demo Reset/);
  assert.match(html, /신청 내용 확인/);
  assert.match(html, /실제 개인정보와 최종 제출 없이/);
  assert.doesNotMatch(html, /Your site is taking shape|Codex is working/i);
});

test("keeps the semantic patch and privacy boundary explicit", async () => {
  const [page, css, layout] = await Promise.all([
    readFile(new URL("../app/page.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/globals.css", import.meta.url), "utf8"),
    readFile(new URL("../app/layout.tsx", import.meta.url), "utf8"),
  ]);

  assert.match(page, /const HELP_COOLDOWN_MS = 30_000/);
  assert.match(page, /stableKey:\s*"review-next"/);
  assert.match(page, /compatibleRevision:\s*REVISION/);
  assert.match(page, /expectedState:\s*"review-ready"/);
  assert.match(page, /guidanceShown/);
  assert.match(page, /userActionObserved/);
  assert.match(page, /postconditionVerified/);
  assert.doesNotMatch(page, /localStorage|sessionStorage|fetch\(|XMLHttpRequest/);
  assert.match(css, /min-height:\s*48px/);
  assert.match(css, /prefers-reduced-motion:\s*reduce/);
  assert.match(layout, /lang="ko"/);
});
