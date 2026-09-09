# PH-ANDROID-CONNECTED-01 local implementation

Date: 2026-09-09. Branch: `codex/android-authenticated-browsing` in isolated worktree
`../mobileAppForPhotoHouse-android-live`. Base is fixture/device-review commit
`2d0382d22ad3ef1e15a55931765ad43e0cbd3ec7`, descending from frozen tag
`photohouse-mobile-fixture-v1` at `5db14f38d3ff7872420f4c5ed16ff54b2cf9b4ac`.
The exact result commit is returned in the PR/handoff; artifact identities are in
`artifacts.json`. This evidence belongs to the connected follow-up, not the APK
previously installed on the physical phone.

## Delivered

- A separate `dev.photohouse.connected` debug app with real HTTPS login, invited
  registration, invitation acceptance, session/library selection, paged gallery,
  authenticated thumbnails, details/captions and acknowledged-vs-local logout.
- Shared typed wire models extracted into a transport-free `protocol` module.
  Explicit native request transport. The fixture module remains offline and bundles
  the unchanged shared fixtures byte-for-byte. No shared schema or backend changes.
- Platform TLS trust, no redirects/cookies/disk cache or logging, scoped bearer
  requests, bounded response/image memory, cancellation and generation isolation,
  private cover/revalidation, expiry and explicit rate-limited retries.
- EN/ZH interface and literal captions. Minor spacing correction for large text;
  product layout/visual refinement remains deferred following user feedback.
- Independent verification and CI extended to both apps using synthetic data only.

Changed ownership: `android/**`, `scripts/verify-android.sh`,
`.github/workflows/android.yml`, `docs/evidence/android/connected/**`. iOS, root
documents, shared contracts/fixtures and parity ledger are unchanged.

## Observed checks

Command from repository root with installed JDK 17 and Android SDK:

```sh
scripts/verify-android.sh :connected:assembleDebugAndroidTest
android/verify-connected-emulator.sh emulator-5580 normal
android/verify-connected-emulator.sh emulator-5580 large
```

- Frozen contract verifier: 12 operations, 38 synthetic ASGI cases, 8 checksummed
  files; no checksum drift.
- JVM: 22 fixture regressions, 13 real loopback TLS/HTTP tests, 14 connected state
  tests; 49 total, no failures/errors. Sanitized JUnit reports retained here.
- TLS tests use generated local certificates and loopback only. They exercise
  exact native/scoped wire requests against pinned synthetic responses, system
  trust rejection, hostname validation, redirect refusal, cookie isolation,
  cancellation, response limits, MIME checks, 401/403/422/429/503 and Retry-After.
- State tests cover admission and arbitrary library IDs, unavailable membership,
  literal captions/missing preview, page deduplication and 8 MiB cache bound,
  background/foreground privacy, late auth/read/logout, 24-hour expiry, single
  session recheck after 401, server outage, rate limits and invitation retry without
  mutation replay.
- Both debug apps build. Lint: 0 errors and one `OldTargetApi` warning per app.
  Compile/target 34 is retained for the existing pinned development toolchain;
  this is not a store submission configuration.
- Connected APK: only Internet and the AndroidX app-internal permission, no fixture
  assets. Fixture APK: no Internet/storage/camera/microphone permission.
- Existing API 36 ARM64 emulator, `Medium_Phone_API_36.0`, booted read-only without
  snapshot writes. Three connected component tests at font scales 1.0 and 2.0,
  including unconfigured admission lock, synthetic login/browsing/literal captions,
  and required invited-registration inputs. Six fixture emulator regression tests
  also passed after extracting the shared protocol. Font scale restored afterward.
- Synthetic own-View renders are retained under `screenshots/normal` and `large`;
  `FLAG_SECURE` remains set. Render review checked normal admission and Chinese
  captions, and the large-font caption layout. These are test-process component
  renders, not real-service/device evidence.
- `git diff --check`, shell syntax and source boundary guards pass.

Toolchain: Temurin 17.0.20.1+1, Gradle 8.10.2, AGP 8.5.2, Kotlin 1.9.24,
Compose compiler 1.5.14/BOM 2024.06.00, installed platform/build-tools 34.0.0,
OkHttp/MockWebServer/okhttp-tls 4.12.0. Existing wrapper checksums remain pinned.

## Remaining gates

The default APK has no configured origin and cannot sign in. No protected staging
origin was supplied or accessed; no real credentials/photos were used. The next
gate is a reviewed HTTPS deployment of the frozen backend, then explicitly scoped
account/invitation, revocation, missing media and logout testing on a physical phone.
Backend source tests do not establish that an existing live service is protected.
No deployment, merge, release signing or new physical installation is claimed.

This is a browsing slice, not full feature completion. Video/original download,
uploads, albums/search, voice, owner tools, persistent sign-in and UI polish remain
outside this slice. Real Android TLS/network behavior on the target phone, TalkBack
acceptance and device-specific lifecycle behavior are unverified. Hosted CI for
this follow-up must be checked at its exact published head; local evidence above
does not substitute for that check.
