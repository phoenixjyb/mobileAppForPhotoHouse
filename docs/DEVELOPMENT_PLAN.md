# Development plan

Date: 2026-09-09. Status: proposed architecture, ready for contract/bootstrap work.
Three read-only subagents reviewed iOS, Android, and backend/voice. The coordinator
integrated their findings; no app or backend implementation was performed.

## 1. Repository and implementation choices

Use one independent mobile repository, not a folder inside the inference stack.
Keep both mobile platforms together so shared contracts, fixtures, and parity
changes are reviewable in the same pull request. Keep backend application logic
in vlmPhotoHouse. Do not fork the photo database or create a second mobile-only
authorization backend beside the existing API.

Use **Kotlin/Jetpack Compose for Android** and **SwiftUI for iOS**. Share versioned
API schemas, synthetic fixtures, error codes, and behavioral scenarios. Do not
introduce Kotlin Multiplatform, Flutter, React Native, or a WebView wrapper in
the first slice. This follows the native ReCoMo iOS contract pattern and keeps
the two build loops independent. Reconsider KMP only after stable duplicated
domain logic demonstrably justifies the Kotlin/Native build coupling.

Proposed layout; only the planning documents exist at this checkpoint:

```text
android/                    Android wrapper, app, small core modules
ios/                        SwiftUI app, local Swift package, XcodeGen project
contracts/v1/               reviewed backend OpenAPI snapshot and synthetic fixtures
docs/                       architecture, parity, evidence, session capsules
scripts/                    independent verify-android / verify-ios / contract checks
.github/workflows/           independent Android, iOS, and contract checks
```

The backend owns the authoritative API schema. The mobile repository consumes a
versioned snapshot with backend SHA, checksum, and compatibility version. The
coordinator owns snapshot updates; clients must not invent incompatible endpoints.
Existing PhotoHouse endpoints are inputs to the design, not an auth-ready API.

## 2. Family product, not an admin console

First useful release: sign in/register, invitation or approval status, one
authorized family library, timeline/photo grid, albums, text search, photo/video
detail, bilingual captions, and account/privacy settings. A warm neutral palette,
generous imagery, clear dates, and accessible controls should make it feel like a
family album. Keep worker queues, disk paths, model IDs, and GPU operations out of
the family navigation.

Use three primary destinations: Photos, Albums, Search; account controls live in
the profile menu. People filters may appear when backend assignment quality and
permissions support them. Do not label unconfirmed face matches as relatives.
Handle empty library, missing caption, missing translation, revoked access,
offline server, and expired sign-in as distinct states.

UI languages: system default, English, Simplified Chinese initially. Caption
language preference is separate. Switching UI language does not rewrite captions
or filter away media with another language. Preserve original text and label AI
output; do not promise that all older captions are already refreshed.

Deferred: automatic phone backup/upload, permanent offline albums, public sharing,
destructive library management, photo printing/export workflows, advanced themed
album generation, always-listening voice, and speakers. Theme browsing and existing
album drafts can follow the authenticated read-only release; generation/export
must later use permission-scoped jobs and confirmations.

## 3. Access model and network

Recommended pilot: invitation/owner approval and HTTPS over home/private VPN
connectivity. Network reachability is not identity. A VPN address alone must never
grant photo access, and GitHub Pages is not a backend or private-media host.
Choose a stable trusted HTTPS origin and certificate lifecycle before real login.
No global cleartext exception or accept-any-certificate code in either app.

Recommend a maintained OIDC provider, system-browser authorization code + PKCE,
and verified identity mapped to local PhotoHouse account/membership records.
Do not build an OAuth server or copy passwords into both apps. Provider hosting,
identity recovery, verification delivery, domain, and certificate setup are a
decision gate; no provider has been selected, installed, or paid for.

Registration produces an identity, not permission to the household. Approval is a
server-owned library membership. The first owner is established by an explicit
operator bootstrap, never by racing to register first. Begin with one explicitly
owned family library while designing IDs/queries so a second library cannot leak.

Public internet access is deferred until authorization, rate limits, recovery,
TLS, monitoring, proxy controls, and the deployment rehearsal pass. No planned
mobile work changes today's Windows listener or existing private tunnel.

Native authorization follows [RFC 8252](https://www.rfc-editor.org/rfc/rfc8252.html)
and the [OAuth security BCP](https://www.rfc-editor.org/rfc/rfc9700.html). Concrete
provider integration must validate issuer, audience, token type/signature/expiry,
redirect bindings, and refresh/revocation behavior; client secrets cannot be kept
secret in distributed mobile apps.

## 4. Implementation sequence and gates

| Phase | Work | Exit evidence |
| --- | --- | --- |
| P0 | Contract draft, capability/route inventory, privacy threat model, synthetic fixtures | Coordinator approves schemas and negative scenarios; no production network |
| P1 | Backend account/membership and all legacy-route closure; Android fixture foundation; iOS fixture foundation | Server denial matrix plus native tests and simulator/emulator evidence; no live access yet |
| P2 | Approved identity provider and TLS staging integration; update web UI to use same policy | Two-account/cross-library/revocation/media-range/end-to-end tests on synthetic data |
| P3 | Small authorized real-library read-only pilot on both platforms | Exact signed build, installed device, pagination/video/performance/logout and family acceptance |
| P4 | Push-to-talk voice with read-only authorized intents | EN/Chinese tests, ownership/cancel/error/privacy tests, measured provider latency and resource budget |
| P5 | Curated albums, controlled uploads/offline/export; paired room devices | Separate product/privacy and deployment approval for each capability |

Start backend security before live mobile work. **Android leads the first technical
slice** because a Linux CI/debug APK path gives a cheap reproducible contract loop.
Start iOS immediately after the fixture contract is fixed, in a separate worktree;
do not wait for Android product completion. This sequencing does not assume the
family prefers Android. The iOS app should be available for an early Mac/iPhone
feedback loop. Voice design can proceed in parallel, but its implementation uses
the merged authorization services rather than inventing a second policy.

## 5. Development infrastructure

Borrow OpenGroove's separate Linux Android/macOS iOS verification jobs and
ReCoMo's contract/parity and exact-artifact handoffs. Do not import Robot code,
private GitLab/JFrog settings, music providers, signing IDs, or outdated pins.

Android: small app with Compose screens/ViewModels, immutable observable state,
repositories and native session/media adapters. Pin a compatible JDK/Gradle/AGP/
Kotlin set at bootstrap after checking current tooling. Verify wrapper checksum;
unit tests, lint, debug APK, and selected emulator tests. Initial minimum Android
API 26 is a proposal to confirm against the family's devices, not a release promise.

iOS: SwiftUI, Foundation transport, a local Swift package for contracts/session
logic, pinned XcodeGen plus committed generated project and generation drift
check. Propose iOS 17 / Swift 6, subject to device inventory. Record actual Xcode
and simulator versions; package tests and unsigned simulator build/UI tests.
Do not make iOS depend on Android Gradle in this first version.

Public PR CI: hosted runners, read-only permissions, pinned action revisions,
bounded artifact retention, synthetic images/accounts only, no home-network
access or signing secrets. Separate platform jobs must not block one another's
local builds. Root contract changes require both clients' fixture tests. A green
mock test does not prove server authorization or installed-device behavior.

Production signing is a later protected/manual lane with dedicated application
identities and increasing build numbers. Never commit keys or export real-photo
screenshots in public CI. TestFlight/Play distribution, store privacy disclosures,
device registration, and installation each need their own authorization/evidence.

Account creation also requires an account-deletion experience before store
distribution; Apple requires an in-app initiation path, and Google Play requires
both in-app and outside-app request paths. Define deletion of account data,
identity-provider records, membership, caches and relevant retained data, including
shared-library ownership transfer, without silently deleting everybody's photos.
See [Apple](https://developer.apple.com/support/offering-account-deletion-in-your-app/)
and [Google Play](https://support.google.com/googleplay/android-developer/answer/13327111).
Review the applicable policy again at release; this plan is not store approval.

## 6. Decisions still open

- Confirm private-network pilot versus internet-without-VPN requirement.
- Confirm invitation/approval membership policy; do not weaken it implicitly.
- Choose maintained identity provider, recovery method, verified domain and TLS.
- Inventory target devices; finalize minimum OS versions and app identifiers.
- Decide explicit export/offline retention and shared-room disclosure policy later.

None prevents synthetic contract/UI work. They do prevent claims of a production
login setup or permission to expose the live library.
