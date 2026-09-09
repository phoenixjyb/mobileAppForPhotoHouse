# PH-ANDROID-FIXTURE-01 local return

Fixture implementation and local acceptance complete. This is a synthetic development
app, not live service authorization or family/device acceptance. The result commit
is the commit containing this evidence; resolve it with `git log -1 --format=%H`.

## Identity and ownership

- Frozen base: `refs/tags/photohouse-mobile-fixture-v1`.
- Observed base SHA: `5db14f38d3ff7872420f4c5ed16ff54b2cf9b4ac`.
- Branch: `codex/android-foundation-auth`.
- Isolated worktree: sibling `mobileAppForPhotoHouse-android` of the original mobile checkout.
- Contract: `1.0.0-fixture.1`; backend source pin remains `1e394f789ff1f7cef6d9930bb541186684f5a9a0`.
- Contract manifest SHA-256: `12bb28062d6ef1c2ae764bf0344debfc2f5377d267ffe455b0a0c861adfbc678`.
- All writes are in `android/**`, `scripts/verify-android.sh`,
  `.github/workflows/android.yml`, and `docs/evidence/android/**`.
- Coordinator checkout, shared fixtures/schema/verifier, parity, iOS and backend
  are unchanged. No code was copied from reference repositories. No delegation,
  model switch, push, merge, backend access or deployment occurred.

## Implementation

Two small modules: typed Kotlin contract/session/fixture core and a Compose app.
The app has demo admission, membership/library selection, two-column photos
(one column at large font), detail/literal captions, placeholders and explicit
video-unavailable states. UI language supports system/English/Simplified Chinese.
Demo panels expose admission, membership, empty/error, delayed-read and privacy cases.
Only reserved synthetic inputs are available; there are no editable real credentials.

The adapter bundles shared assets directly, performs no real HTTP and never requests
original bytes. The packaged APK has only the AndroidX app-internal signature
permission, with no Internet/storage/camera/microphone permission. Release variants
are disabled. All session/gallery/detail/image state is in memory. Lifecycle and
new-generation checks reject stale responses, including cancellation-resistant
fixtures; unavailable memberships never initiate gallery/media loads.

## Observed validation

| Check | Result / evidence |
| --- | --- |
| Shared offline contract | PASS: 12 operations, 38 ASGI cases, 8 checksummed files, unchanged |
| Shared verifier regression tests | PASS: 5 tests (`python3 scripts/test-contracts.py`) |
| Wrapper JAR and distribution | SHA-256 pinned to official Gradle 8.10.2 checksums; see `verification.log` |
| JVM parser/session/privacy | PASS: 22 tests, 0 failures/errors/skips; `jvm-tests.xml` |
| Android lint | PASS: 0 errors, 1 `OldTargetApi` warning; `lint.txt` |
| Debug app and instrumentation APK | PASS; exact hashes below and in `artifacts.json` |
| APK fixture bundle | Every shared file/image compared byte-for-byte by `scripts/verify-android.sh` |
| APK permissions/signature | No network/media permissions; `apksigner verify` succeeded for the local debug APK |
| Emulator at font scale 1.0 | PASS: 6 UI/lifecycle tests; `instrumentation-normal.log` |
| Emulator at font scale 2.0 | PASS: same 6 tests; `instrumentation-large.log` |
| Manual adb smoke | PASS: cold start, UI-tree-driven admission/gallery, foreground revalidation, process-death sign-out; `manual/result.txt` and XML |
| Privacy screenshots | Actual gallery window capture black; app-switcher PhotoHouse preview blank; `manual/secure-foreground.png`, `manual/secure-recents.png` |
| Render review | EN/ZH, 200% type, literal captions, placeholders, video unavailable and private cover inspected; `screenshots/normal/` and `screenshots/large/` |

The initial missing-preview UI assertion queried a merged/offscreen child. The test
now scrolls to its card and checks the unmerged semantics; both final runs passed.
Render review also prompted separate language chips and outlined account controls
for clearer separation at 200% text. No contract change was needed.

Instrumentation render captures draw only the app's own synthetic View; they do
not disable `FLAG_SECURE`. Test render files are copied out and removed from the
emulator. Ordinary system screenshot and task-preview evidence is retained separately.
Screen-reader labels were observed in semantics/UI trees; spoken TalkBack use was
not tested. Existing emulator only: `Medium_Phone_API_36.0`, API 36 / Android 16,
`emulator-5580`, model `sdk_gphone64_arm64`, emulator 35.6.11.0 (build 13610412).
It ran with `-read-only -no-snapshot-save -no-snapshot-load`; no new image was installed.

## Scenario coverage

| Scenario | Observed coverage |
| --- | --- |
| APP-01 | JVM + emulator: valid invited viewer admission, only invited library |
| APP-02 | JVM + emulator: invalid invitation remains signed out; no gallery/media |
| APP-03 | JVM: requested/rejected/revoked/expired/empty/approved-but-unavailable; emulator disabled expired-membership control |
| APP-04 | JVM + emulator: approved gallery, detail, synthetic image labels, literal bilingual caption |
| APP-05 | JVM + emulator: missing preview, no original fallback, metadata-only video unavailable |
| APP-06 | JVM: delayed old-library response discarded; Family B never receives Family A fixtures |
| APP-07 | JVM: immediate logout/cache/token/navigation clearing with late response/server failure; emulator logout flow |
| APP-08 | JVM: one revalidation after object denial; valid session retained, invalid signed out, 503 covered, 403 no fallback |
| APP-09 | JVM + emulator/manual: background cover, revalidation, no covered navigation bypass, session expiry on foreground/read, process restart signed out |
| APP-10 | JVM + emulator: EN/ZH, 200% text, accessible labels, literal markup caption, empty/missing/error/retry distinctions |

## Reproduction and artifacts

From the isolated worktree root, with the existing JDK 17 and Android SDK set in
`JAVA_HOME` and `ANDROID_HOME`:

```sh
scripts/verify-android.sh --offline
android/gradlew -p android :app:assembleDebugAndroidTest --offline
android/verify-emulator.sh emulator-5580 normal
android/verify-emulator.sh emulator-5580 large
python3 android/smoke-existing-emulator.py emulator-5580
```

Remove `--offline` when public build dependencies are not cached. The initial
wrapper distribution transfer timed out and required resuming with curl; the
complete official ZIP was checksum-verified before wrapper execution. This was a
build-tool download issue, not an Android SDK install or app backend connection.

Exact local toolchain: Temurin JDK 17.0.20.1+1; Gradle 8.10.2; AGP 8.5.2;
Kotlin 1.9.24; Compose compiler 1.5.14 / BOM 2024.06.00; Android compile/target 34,
build-tools 34.0.0; provisional minimum API 26. More detail in `artifacts.json` and
`android/README.md`, including official compatibility references.

Debug APK: `android/app/build/outputs/apk/debug/app-debug.apk`, 8,360,843 bytes.
SHA-256: `652991721ca085242fea2c57d8b3efba96b2d53a5b7aa8a55afac922d7d64eb4`.

Instrumentation APK: `android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.
SHA-256: `16d57f35604c886d73d5d756c17b14768e39c369cb921dcebe7058ff206aab1f`.

APKs and all Gradle outputs remain local/ignored; signing keys are not committed.
Rebuilding elsewhere uses that machine's development key, so an independently
signed APK can differ even with the same source. `artifacts.json` pins this run's
APK and each retained screenshot. Raw startup/device logs and local machine names
are excluded or sanitized in committed evidence.

## Remaining gates

No remaining local capsule blocker. Hosted Linux CI is defined but was not run.
Minimum-API/family device inventory, physical phones, TalkBack spoken acceptance,
release signing/store target-SDK updates, real HTTPS/session/media integration,
real video/Range/cache behavior and family acceptance remain unrun/separate scopes.
There is no Family B gallery or playable video in the frozen pack; those limitations
are displayed explicitly. Screenshot protection is observed on this emulator,
not claimed as universal device enforcement.

Next smallest step: coordinator reviews this branch and evidence alongside the iOS
return before any integration or parity update. No shared contract revision is proposed.
