# PhotoHouse independent session handoffs

**Historical foundation document.** Platform/backend status statements below describe
that handoff's baseline. For current Android implementation and remaining live gates,
use [Android MVP status](../android/MVP_STATUS.md) and
[pilot acceptance](../android/PILOT_ACCEPTANCE.md). Do not restart the Android
foundation capsule or treat an earlier blocker as current without those records.

Updated 2026-09-09 after the backend foundation and recovery slices. **Android and
iOS fixture app work can start directly from the frozen local baseline below.**
This document prepares assignments; it does not create sessions or start agents.
No mobile app build, simulator/device result, deployment, push or merge is claimed.

## Shared baseline and launch

| Item | Exact baseline |
| --- | --- |
| Mobile repository | `phoenixjyb/mobileAppForPhotoHouse` |
| Mobile frozen ref | Local tag `refs/tags/photohouse-mobile-fixture-v1`; resolve and record its full SHA before working |
| Coordinator branch | `codex/mobile-foundation-plan`; use the frozen tag, not a moving branch or remote default |
| Backend source | `phoenixjyb/vlmPhotoHouse`, `codex/mobile-access-foundation`, `1e394f789ff1f7cef6d9930bb541186684f5a9a0` |
| Consumer contract | `1.0.0-fixture.1`; `contracts/v1/manifest.json` pins backend sources and all fixture/contract checksums |
| Backend evidence | 198 local security tests; 152 inventory entries, 23 active routes; source/test evidence only |
| App implementation | Neither platform implemented yet; both can consume the same checked fixtures independently |

The tag and backend branch are **local**, not assumed published or available on
another machine. On this Mac, begin from the mobile checkout, inspect status and
`git worktree list`, then resolve `git rev-parse refs/tags/photohouse-mobile-fixture-v1`.
Never reset, clean, stash, overwrite or reuse somebody else's worktree. If an assigned
branch/path already exists, inspect its owner/base and continue only if it is your
matching task; otherwise select a fresh isolated path and report it.

Example commands, from the mobile repository, when those names do not already exist:

```sh
git worktree add -b codex/android-foundation-auth ../mobileAppForPhotoHouse-android refs/tags/photohouse-mobile-fixture-v1
git worktree add -b codex/ios-foundation ../mobileAppForPhotoHouse-ios refs/tags/photohouse-mobile-fixture-v1
```

Create only your assigned worktree. If the tag/files are absent or checksums fail,
report the missing baseline; never clone an empty remote/default branch and invent
DTOs. Both sessions first run `python3 scripts/verify-contracts.py`. Neither needs a
backend service or database. Contract verification is offline and stdlib-only.

## Decisions that supersede the initial planning pack

- Owner manually sends a phone-bound invitation. New account registration requires
  that invitation plus phone/password and grants viewer access **only to that invited
  library**. Returning login uses phone/password. Phone is unverified; internal account
  ID is opaque. No OIDC, SMS, WeChat, open signup or first-signup owner in this scope.
- An invalid invitation is failed registration, not an account awaiting approval.
  Existing requested/rejected/revoked/empty/expired memberships still need clear UI.
  Use the server's `available` flag; approved status alone does not permit browsing.
- The frozen wire format uses offset pages, opaque 24-hour sessions without refresh,
  numeric membership revisions, decimal-string asset IDs and plain caption text.
  Do not implement the old cursor, JWT/OIDC, refresh-token or structured-translation
  proposals. Read `contracts/v1/CONTRACT.md` before designing DTOs.
- First apps: sign-in/invited registration, library selection, synthetic gallery,
  detail/captions, EN/ZH interface, privacy/session lifecycle and error states.
  Only Photos and account/library controls need navigation. Albums/search/voice,
  uploads, export, persistent offline photos and owner/admin tools are deferred.
- Real HTTP is disabled throughout both first platform slices. Fixture mode must be
  visibly labeled and excluded from any production networking implementation. Do not
  collect real phone numbers/passwords/invitations. No endpoint configuration screen
  that can silently enable real networking in this first build.

## Coordination and routing

Coordinator owns contracts, fixtures, root configuration, cross-platform decisions,
parity and integration. Android owns its assigned files; iOS owns its own. One writer
per worktree; neither platform changes shared fixtures/schema or the other platform.

Routing classification: coordinator/integration is `owner`; each app foundation is
`bounded`. Use the current configured executor/model; availability was not queried
and no model switch or persistent session creation is implied. Apply the repository's
`route-codex-work` capsule/return rules. No delegation is required to run these tasks.

Start Android's first build loop, then iOS immediately from the same tag. iOS does
not wait for Android completion. Backend recovery/deployment work does not block
fixture app implementation. Coordinator inspects both returns before any integration.

## A. Backend continuation — separate from mobile app work

Task ID: `PH-BACKEND-RECOVERY-REOPEN`. Repository: `vlmPhotoHouse`; existing backend
branch/worktree at the SHA above. Read its AGENTS, required agent-memory documents,
`docs/security/OFFLINE_RECOVERY.md` and `OFFLINE_PROVISIONING_APPLY.md` before changes.
Refresh actual Git identity/status and preserve every existing worktree.

**Already complete locally:** account/invitation admission; common membership/object
checks; closure of legacy handlers; protected scoped gallery/captions/media/Range;
web cookie/CSRF UI; explicit migrations/runtime; audited provisioning and restored
access quarantine. Do not restart the initial route inventory task or add OIDC.
The 97 retired handlers remain unsafe and unmounted; 32 standalone model/diagnostic
routes remain separate exposure gaps. Search/albums/voice are closed, not secured
implementations. The foundation is not deployed or a claim about today's live service.

**Next bounded outcome:** design/test explicit owner recovery and selective library
reopening with protected new-password input, membership/original-grant review and
atomic audit. Never reactivate a restored audience wholesale. Missing post-backup
revocation history needs independent review. Keep unreviewed accounts/libraries closed.
This backend assignment does not authorize mobile edits, real credentials/data,
Windows/Mac mini access, models, listeners, deployment, push or merge.

Return implementation/tests and remaining operator/deployment gates. Mobile sessions
continue from the frozen fixture contract; proposed API changes return to coordinator.

## B. Android Foundation — copy into an Android session

Task ID: `PH-ANDROID-FIXTURE-01`. Repository: `mobileAppForPhotoHouse`.
Base: `refs/tags/photohouse-mobile-fixture-v1`; branch `codex/android-foundation-auth`.
Create/verify your isolated worktree and record the resolved full base SHA and path.

**Outcome:** runnable Kotlin/Jetpack Compose fixture app for the frozen browsing flow,
with a reproducible debug build and session/privacy tests. No real backend networking.

**Read first:** AGENTS.md, README.md, docs/DEVELOPMENT_PLAN.md, docs/SECURITY_AND_VOICE.md,
contracts/README.md, contracts/v1/CONTRACT.md, OpenAPI/fixtures/client scenarios,
manifest and this capsule. Run `python3 scripts/verify-contracts.py` before editing.
Maximum read scope: this repo and the documented reference source paths when needed
for architecture/license checks. No live endpoints, databases or private configs.

**Write ownership:** `android/**`, `scripts/verify-android.sh`,
`.github/workflows/android.yml`, `docs/evidence/android/**` only. Keep Gradle root,
wrapper, app and modules inside `android/`. Do not modify root AGENTS/README/gitignore,
shared verifier/fixtures, PARITY.md, iOS, backend or another session's checkout.

**Implement:**

1. Inspect available JDK/Android SDK/toolchain. Choose and pin compatible Gradle/AGP/
   Kotlin/Compose versions using official tooling guidance; verify wrapper checksum.
   Keep modules/dependencies small. Minimum API 26 is proposed, not a family-device
   guarantee. Use a clearly development-only app identifier; no borrowed signing IDs.
2. Parse shared JSON responses into typed models; add an injected fixture repository,
   memory-only session and generation-bound requests. Do not implement real transport,
   refresh tokens or independent wire fields. Bundle or deterministically consume the
   shared fixture assets from this worktree; do not modify/copy-edit their contents.
3. Build visibly labeled demo sign-in/invited registration, own membership/library
   selection, gallery/detail, literal captions, missing preview/video-unavailable,
   empty/error/retry and logout states. Use reserved synthetic demo inputs only.
4. Implement scenarios APP-01 through APP-10. No gallery/media for unavailable
   memberships; no original fallback; no HTML captions; no late prior-generation
   responses; immediate private UI/cache clearing on logout/switch. Cover app-switcher
   previews, cold-start signed-out behavior and foreground session revalidation.
5. Supply independent verification script and Linux CI definition using synthetic
   data only, read-only permissions and pinned action revisions. Commit source locally;
   defining CI is not a claim that hosted CI ran. No signing secrets or publishing.

**Acceptance:** contract check; JVM parser/session/privacy tests; lint; debug APK.
Record exact toolchain and APK SHA-256. With an already available emulator, run a
synthetic smoke/render check for EN/ZH, large font and accessible labels; report the
AVD/API and observed result. Do not install new emulator images or touch a physical
phone without separate authorization. If no emulator/toolchain is available, retain
source/unit/build evidence that is possible and report that specific gate as unrun;
do not claim completion of the unavailable gate or rewrite contracts around it.

**Stop/escalate:** shared schema/API changes, writes outside ownership, unfamiliar
user changes, SDK/image/system installation, real networking/data/credentials,
release signing, physical installation, remote CI operations, push/merge/deployment.
Continue independent in-scope work and return concrete blockers with the diff intact.

## C. iOS Foundation — copy into an iOS session

Task ID: `PH-IOS-FIXTURE-01`. Repository: `mobileAppForPhotoHouse`.
Base: the same `refs/tags/photohouse-mobile-fixture-v1`; branch `codex/ios-foundation`.
Create/verify your isolated worktree and record resolved full SHA/path. Do not wait
for Android implementation or depend on its Gradle build.

**Outcome:** runnable SwiftUI fixture app with a local Swift package, unsigned
simulator build and session/privacy tests against the same contract as Android.

**Read first / maximum read scope:** the same shared documents/contracts listed in
B plus this capsule. Run `python3 scripts/verify-contracts.py` before editing. Read
only documented reference source when needed; never copy signing/network identities.

**Write ownership:** `ios/**`, `scripts/verify-ios.sh`, `.github/workflows/ios.yml`,
`docs/evidence/ios/**` only. Root/shared files, PARITY.md, Android and backend remain
coordinator/other-session owned.

**Implement:**

1. Record installed Xcode, Swift, simulator runtimes and XcodeGen availability. Start
   from proposed iOS 17/Swift 6 when the installed toolchain supports it. Put the local
   Swift package, XcodeGen spec and committed generated project under `ios/`; verify
   generation drift. No signing-team or provisioning changes; no KMP dependency.
2. Use Swift Codable contract models, injected fixture repository, memory-only session,
   generation isolation and bundled synthetic assets. No URLSession calls to a real
   host, refresh-token implementation or persisted password/session in this first slice.
3. Implement the same flow and APP-01 through APP-10 as Android: labeled fixture sign-in
   and invited registration; membership/library selection; gallery/detail/literal
   captions; placeholders and errors; EN/ZH UI; logout and private lifecycle behavior.
   Preserve unknown timestamp/timezone and unsupported caption-language information.
4. Add independent Swift package tests and macOS CI definition with read-only
   permissions, pinned actions and synthetic artifacts. Do not make iOS builds depend
   on Android tooling or claim hosted CI ran from the workflow definition alone.

**Acceptance:** contract check; Swift package parser/session/privacy tests; generated
project drift check; unsigned simulator build. On an already installed simulator,
run synthetic UI smoke/render checks for EN/ZH, Dynamic Type, accessible labels,
background privacy and late responses. Record Xcode/runtime/device model, commands,
artifact identity and actual screenshots/results. No physical iPhone installation,
new simulator runtime, production signing, TestFlight or provisioning changes.
If a tool/runtime is missing, complete independent source/package work and identify
that exact unrun build/UI gate; never claim a simulator pass from source tests.

**Stop/escalate:** same boundaries as B. Return contract changes to coordinator;
keep partial work and evidence. Do not switch model or launch other sessions implicitly.

## D. Voice — deferred, not a mobile foundation prerequisite

Task ID: `PH-VOICE-DESIGN-NEXT`. Repository: `vlmPhotoHouse`, separate worktree.
The old voice and search endpoints remain closed. Until reviewed authorized search/
voice contracts exist, limit a separately requested voice session to design and
synthetic intent/ownership/cancellation/error tests; do not expose an endpoint or
invent calls in either app. Later scope is explicit push-to-talk EN/Chinese,
authorized read-only results and optional STT/TTS adapters. No ambient audio,
speakers, Windows/Mac mini, GPU/model loading, cloud processing or deployment.

## Return format and integration

Every session returns task ID, observed base/ref/full SHA, branch/worktree, result
commit(s), changed files, contract version/checksum result, exact validation and
artifact identities, screenshots when rendered, skipped/unavailable gates, scope
expansions and next smallest step. Separate source, tests, simulator/emulator,
signed build, installed device, live service and family acceptance.

Keep evidence in your assigned platform directory, never real photos or credentials.
The coordinator alone reviews diffs, runs the shared checks, integrates compatible
changes and updates PARITY.md from observed evidence. No automatic push, merge,
contract revision, model switch or session creation is part of these handoffs.
