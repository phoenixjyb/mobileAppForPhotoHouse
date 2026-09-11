# Android PhotoHouse development apps

The connected invitation-based browsing MVP is locally implemented. Start with
[current MVP status and requirement mapping](MVP_STATUS.md), then
[the pilot acceptance/configuration handoff](PILOT_ACCEPTANCE.md). Live deployment
and phone acceptance remain pending; the default APK origin is empty.

Three separate apps: `connected` is the phone HTTPS viewer described in
[connected/README.md](connected/README.md); `app` is the offline fixture demonstrator;
[`tv`](tv/README.md) is the remote-controlled landscape viewer with a private build
origin of its own, 1024-pixel cached detail previews and permission-gated originals.
See the [TV plan and evidence](../docs/evidence/android/tv/RETURN.md) before installing.
The shared `protocol` module contains wire DTOs only. The completed locked-profile
replay is current; historical fixture/readiness returns remain evidence of their
own commits. Use the [no-listener lane](readiness/README.md) for the current local
scope; Gradle/TLS/device scripts require a separately authorized lane.

The sections below document the offline fixture app/toolchain. Their no-network,
metadata-only-video statements apply to `app`, not the connected viewer.

## Offline fixture app

Task `PH-ANDROID-FIXTURE-01`: a development-only Kotlin/Compose app consuming the
unchanged `1.0.0-fixture.1` shared pack. App ID: `dev.photohouse.fixture`.
Release variants are disabled. The source manifest requests no permissions.
The packaged APK adds only AndroidX’s app-internal signature permission; it has
no Internet, storage, camera or microphone permission. There is no HTTP adapter, endpoint setting, real credential
entry, player, image disk cache or persistent session.

## Build

Use an existing JDK 17, Android platform 34 and build-tools 34.0.0. Set `JAVA_HOME`
and `ANDROID_HOME` to those installations, then from the repository root:

```sh
scripts/verify-android.sh
android/gradlew -p android :app:assembleDebugAndroidTest
```

The independent verifier checks shared contracts, wrapper checksums, fixture
boundaries, JVM tests, lint and debug APK, then verifies that every bundled contract
file and image matches the shared source bytes. It prints the APK SHA-256.
`android.builder.sdkDownload=false` prevents automatic SDK installation. The wrapper
keeps its distribution under `android/.gradle/distributions`; dependencies use the
normal Gradle cache. Tool/dependency downloads may need connectivity to public build
repositories. The app itself cannot network.

Pinned toolchain: Gradle 8.10.2, AGP 8.5.2, Kotlin 1.9.24, Compose compiler 1.5.14,
Compose BOM 2024.06.00, activity-compose 1.9.2, lifecycle 2.8.4, coroutines 1.8.1,
serialization 1.6.3 and JUnit 4.13.2. Minimum API 26 is provisional. Target/compile
34 preserves compatibility with the existing AGP/toolchain and produces the known
`OldTargetApi` lint warning; this is not a store submission configuration.

Official compatibility/checksum sources consulted:

- [AGP 8.5 compatibility](https://developer.android.com/build/releases/agp-8-5-0-release-notes): JDK 17, minimum Gradle 8.7, API/build-tools 34.
- [Compose/Kotlin map](https://developer.android.com/jetpack/androidx/releases/compose-kotlin): compiler 1.5.14 with Kotlin 1.9.24.
- [Gradle distribution and wrapper checksums](https://gradle.org/release-checksums/).

## Structure and privacy

`protocol` contains strictly decoded wire DTOs. `core` contains the injected local fixture repository,
and the memory-only `PhotoHouseStore`. `app` owns Compose UI, image decoding and
Activity lifecycle. Both JVM resources and APK assets point directly to
`contracts/v1`; no platform-maintained fixture copies are edited.

Every request carries its generation, opaque account ID and selected library.
The adapter namespace is permanently local fixture mode; there is no server switch.
Logout, library/account changes, new photo reads and backgrounding cancel pending
jobs and drop the complete private view and bounded image map. A late callback
that ignores cancellation still fails the generation check. All state changes use
the UI dispatcher. No process-restoration state is accepted.

`onPause` covers content and clears previews; `FLAG_SECURE` protects window capture
and task snapshots. `onResume` revalidates before exposing the account/library UI.
Revalidation does not automatically restore photos. A 401 read hides content and
checks the session once; an invalid session signs out. A 503 keeps the cover without
claiming revocation. Local logout never claims live server acknowledgement.

The gallery holds at most 100 preview entries for the current page and detail holds
one; the frozen pack has two small PNGs. Switching views discards these maps and
Compose image references. Future real media would need a separately reviewed
byte-limited loader. No original request or native video player is implemented.
Video remains a clearly labeled metadata-only unavailable state.

UI language follows the system initially, with explicit EN/ZH choices. It never
modifies captions. Dates and caption text are shown literally; unknown timezone
and absent caption-language metadata are not inferred. Large text wraps language
controls and switches the gallery to one column. Buttons and synthetic images
have text/content descriptions. TalkBack user acceptance remains a separate gate.

## Demo controls

Sign-in and registration buttons use only reserved `+12025550102` / `+12025550103`
inputs and conspicuously synthetic password/invitation constants. Invalid invitation
remains signed out. The demo access panel offers requested, rejected, revoked,
expired, empty and two-library membership states plus rate-limit/unavailability.

Only `available=true` permits browsing. Family B intentionally has no gallery in
this pack, so the app shows that limitation without relabeling Family A's response.
Asset 101 opens the image detail; asset 102 opens the separate video-metadata fixture.
The fixture snapshots describe different illustrative states, not a continuous
backend timeline. Photo demo controls expose missing/empty/delayed states, object
401/session expiry, missing captions and unavailable foreground/logout behavior.
A rate-limit response has a local five-second cooldown because the frozen fixture
contains no Retry-After header. The tested parser supports delta seconds and HTTP
dates for a later adapter; no server header is invented in this pack.

## Existing emulator only

Build the two APKs first. To run six UI/lifecycle tests against an already running
emulator and retain synthetic renders:

```sh
android/verify-emulator.sh emulator-5580 normal
android/verify-emulator.sh emulator-5580 large
```

The script requires an explicit emulator serial and verifies `ro.kernel.qemu=1`.
It installs only those debug APKs, temporarily sets font scale to 1.0 or 2.0, restores
that setting on exit and runs instrumentation. It removes its test-only render files after copying them to evidence. It neither launches nor downloads
an emulator and rejects physical-device serials. Test captures draw the app's own
View in the instrumentation process while `FLAG_SECURE` stays enabled. They are
render evidence, not evidence that screenshots are allowed in the ordinary app.
The Linux workflow performs independent source/JVM/lint/build checks with read-only
permissions and SHA-pinned actions. Defining it does not mean hosted CI ran.

For a UI-tree-driven cold-start/process-death and secure-window smoke check, run
`python3 android/smoke-existing-emulator.py emulator-5580` with the same SDK environment.
It captures recents only when every task belongs to the fixture app or its test package.

See `docs/evidence/android/RESULT.md` for exact local evidence and remaining gates.
