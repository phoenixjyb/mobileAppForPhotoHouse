# Independent Codex session handoffs

Status: **handoffs prepared; standalone sessions not created**. Three temporary
design subagents completed in the coordinating conversation. The discovered
Codex list-projects route returned unavailable on this host; no usable replacement
MCP session-creation tool was exposed. Do not confuse subagents with persistent
Android/iOS/voice conversations.

Create four separate local Codex tasks when the app's project/task route is
available. Use isolated worktrees for repository work. This is consistent with
the [Codex worktree workflow](https://learn.chatgpt.com/docs/environments/git-worktrees).
Never launch multiple writers in the same checkout. The present planning branch
is local only and the remote mobile repository was empty at audit time; publish
or explicitly select the local planning baseline before relying on a default-
branch worktree. Do not assume an unpushed branch is remotely available.

All sessions must record actual base SHA/branch/worktree, assigned ownership,
diff, validation and remaining gate. Use the configured model by default; no model
switch is implied by these capsules. Shared contracts and final integration remain
with the coordinator. Each capsule below is a user-visible launch prompt, not a
command that has already run.

## Coordination order

1. Coordinator publishes/freezes a synthetic contract baseline after the backend
   route inventory. Identity provider and live deployment stay approval-gated.
2. Start Backend Security. Start Android Foundation against the frozen fixtures.
3. Start iOS Foundation against that same contract immediately afterward.
4. Voice session can refine contracts/tests in parallel; real implementation uses
   the merged authorized services. No GPU/runtime work in these first tasks.

## A. Backend Security — first authoritative prerequisite

Project: phoenixjyb/vlmPhotoHouse. Suggested branch: codex/mobile-access-foundation.
Audited implementation: 752ab3b, same tree as merged master 9322635; refresh refs
and inspect the actual branch/diff before working. Reference mobile plan from
phoenixjyb/mobileAppForPhotoHouse; read backend AGENTS and referenced memory docs.

**Initial bounded outcome:** commit a complete route/capability inventory and a
synthetic negative-test harness describing the authentication gap. No live data
or migration in the first commit. Cover legacy routes, image/video bytes, face
crops, search seed/results/aggregates, operational endpoints and voice. A route
missing from the inventory must fail the inventory check. Clearly label expected
current failures; do not hide them with permanent skips or claim enforcement.

**Next reviewed slices:** verified OIDC principal adapter + account/membership
schema and explicit migration; common policy/query helpers; closure of all legacy
route bypasses; web cookie/session/CSRF and mobile token compatibility. Coordinate
contract fields with the root owner before freezing them. A provider choice or
security migration requires review; do not implement a bespoke OAuth provider.

Write scope: backend security tests/inventory first; later explicitly reviewed
security services, schema/migration, affected routes and web authentication UI in
this repository only. Do not modify the mobile repo from this worktree.

Invariants: registration grants zero libraries; no first-signup owner; no localhost
or VPN auth bypass; object and role checks precede files/providers/writes; synthetic
test data; no existing IDs/original paths changed. No Windows/Mac mini access,
models, installation, push, deployment or network exposure in this task.

Acceptance for complete foundation: anonymous/unapproved/revoked/cross-library
denials, approved viewer reads, forbidden mutations, protected Range requests,
scoped search/counts, migration integrity and web compatibility. Partial source
work is not deployable. Return exact commits and remaining provider/TLS gate.

## B. Android Foundation — first mobile build loop

Project: phoenixjyb/mobileAppForPhotoHouse. Suggested branch:
codex/android-foundation-auth. Begin from the coordinator's exact contract baseline
in an isolated worktree. If contracts/v1 and its fixtures are absent/unfrozen,
return to the coordinator; do not invent production endpoints or independent DTOs.

Own android/**, scripts/verify-android.sh and .github/workflows/android.yml only.
Root owns contracts, shared fixture files and parity integration. Inspect root
AGENTS, this plan and the Android reference paths. Choose and record compatible
current toolchain pins; do not copy old versions/signing/cleartext from OpenGroove.

Implement the Compose fixture shell: sign-in/registration entry states, pending
membership, approved synthetic gallery and detail, English/Chinese resources,
expired/revoked/error states, generation-isolated session repository and logout.
Fixture login must be visibly non-production; release real networking remains
disabled. Keep module count small and dependency additions justified.

Acceptance: JVM tests prove registration/pending/revoked cannot fetch media;
approved synthetic member can browse; logout/account switch rejects late results;
bad restored state stays locked. Unit tests, lint and debug APK pass. Emulator
smoke and large-font EN/Chinese render checks are separate observed gates. Record
toolchain, SHA, APK digest and skipped device/signing checks. No real account,
media, endpoint, release signing, device installation, store publish or backend
changes. Stop if shared contract/security choices need alteration.

## C. iOS Foundation — same contract, independent delivery

Project: phoenixjyb/mobileAppForPhotoHouse. Suggested branch:
codex/ios-foundation. Use an isolated worktree at the same frozen contract baseline
as Android; stop for coordinator input if fixtures/schema are not ready.

Own ios/**, scripts/verify-ios.sh and .github/workflows/ios.yml only. Keep contract
and parity changes with the coordinator. Use native SwiftUI, a local Swift package,
Foundation transport boundaries, XcodeGen plus a committed generated project.
Propose iOS 17/Swift 6; record the actual available build/simulator versions.
No KMP dependency or new third-party runtime package is required for this slice.

Implement a synthetic fixture gallery/detail and account/membership state shell,
independent UI/caption language selection, readable EN/Chinese empty/error states,
session-generation isolation, private UI covering and logout/account switching.
No real backend networking. Do not copy app IDs, signing teams or network exceptions.

Acceptance: package model/session tests, simulator tests and unsigned build;
synthetic EN/Chinese, large text and accessibility render evidence; late previous-
account requests never repopulate UI. Record precise SHA/toolchain/results and
device gates. Physical device signing, provisioning changes, installation,
TestFlight, permissions to use private media and backend work remain out of scope.

## D. Voice Interaction — separate backend session

Project: phoenixjyb/vlmPhotoHouse. Suggested branch: codex/mobile-voice-foundation.
Isolated worktree, separate from Backend Security. Read root plan and the merged
authorization service contract before implementation. Without that prerequisite,
limit work to design/synthetic adapter tests and return the missing dependency.

Outcome: authenticated push-to-talk -> STT -> typed read-only intent -> authorized
result cards -> optional TTS. Own voice adapter/intent/session modules and focused
tests, not account schema, shared auth policy or mobile UI. Refactor reusable
adapters only after checking current routes; repair obsolete Asset-field access.

Initial intents: search assets/people/person assets, describe asset, help. Bind
conversations to authenticated account/library/device/session; ignore caller
identity in transcripts or client_id. Enforce audio bounds, rate/concurrency limits,
timeouts/cancellation and typed provider errors. No mutations, arbitrary model
tools, raw-audio persistence by default or cloud processing without an explicit
privacy choice. Keep provider adapters replaceable and run mocked tests first.

Acceptance: EN/Chinese query parity, clarification/no-match, unauthorized asset
denial before provider output, wrong-account conversation denial, cancellation,
provider failure and text fallback. Current /voice/command feature gating is not
user authentication. Do not reuse its optional confirmation token as authority.

No Windows/Mac mini access, GPU launches, STT/TTS model installation, speakers,
wake words, ambient capture, public endpoints or deployments. A later benchmark
must explicitly coordinate GPU budget/heat with ongoing caption/face work. Paired
speaker design must distinguish device identity from who can hear the response.

## Integration return format

Report: task ID; base/branch/worktree; files/commits; contract version; passed and
skipped checks; synthetic versus live evidence; screenshots/artifact digest if
applicable; open security gates; next smallest action. Do not merge or change
another session's files automatically. The coordinator updates PARITY.md after
inspecting actual evidence, not from a completion claim alone.
