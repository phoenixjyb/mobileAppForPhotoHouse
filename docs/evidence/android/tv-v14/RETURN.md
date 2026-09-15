# TV v14: compact gallery and bounded video loading

Base: `0442a4bed05c8b3e6b56334be549cab5d7a9ff4b` (merged phone Home and TV v13).
Branch: `codex/tv-compact-playback-v14`. Isolated checkout: `mobileAppForPhotoHouse-tv-v14`.

The TV gallery now has one compact control row. Secondary actions move into More;
media/readiness/order and page controls remain directly reachable. At large font
sizes the row scrolls horizontally rather than wrapping. EN/ZH render captures at
100% and 200% are in `screenshots/` and were visually inspected.

The shared Home video reader previously fetched HTTP bytes for every native read,
including tiny MP4-header probes. It now retains one bounded read-ahead window,
cleared on close, with unchanged exact Range/length/type/revision/TLS validation.
One thousand tiny reads require one fetch in the regression test; seek replacement,
multi-gigabyte offsets, cancellation, malformed responses and credential isolation
remain covered. Phone Home inherits this reader change; no phone APK is delivered here.

The TV timeout previously sat on the same looper as potentially blocking setup.
Its watchdog now runs independently, starts before `setDataSource`, cancels owned
reads and presents the existing PREPARE_TIMEOUT diagnostic. A native instrumented
blocked-extractor test verifies cancellation and error delivery; no timeout increase
or whole-video download was introduced. Playback still requires explicit Play.

## Verification

- Frozen shared contract, TV boundaries, on-demand and readiness contract checks pass.
- 96 Home JVM tests and 2 TV JVM tests pass, zero failures/skips.
- TV lint, TV debug/test APK builds and phone connected debug build pass.
- Existing API36 ARM64 emulator at 1920x1080, density320; synthetic media only.
- 8 focused checks pass: one-row EN/ZH controls, actual decoded video frame,
  play/pause/seek/fullscreen, background/replace/close, malformed/read errors,
  preview retry and blocked-setup timeout.
- 22 navigation checks: 21 initially pass; one retained the old Explore focus
  expectation after that action moved into More. The corrected expectation passes
  in its isolated rerun. No application change was needed for that test correction.
- 6 large-font (200%) browse/catalog checks pass. Toolbar height and single-row
  control alignment are asserted. Logs retain the initial failure and corrected run.
- `source-checks.json` records JVM totals and the unconfigured APK hash.

## Private artifact and remaining acceptance

TV package `dev.photohouse.tv`, versionCode14, `0.14-tv-compact-playback`.
Configured private artifact: `PhotoHouse-TV-v14-home.apk1`.
SHA256: `ae283d0c956e73c512272b3041b3f95cd509312acdab9c9a7ac00eadef1e46db`.
Debug signer SHA256: `56d7591b2b6c2538d506d1fe51327444f2307736cb12f5beefa08f1c410d6d28`.
Private artifact is 10,458,677 bytes. SMB operator readback matches its checksum;
the existing reader group retains read/execute only, and four older APKs are unchanged.
Private endpoint/routing stay outside source control. No public endpoint or contract
change, phone sign-in change, device install, push or merge was performed.

No projector was attached through ADB. The reported real asset still requires
device confirmation after installing v14; emulator playback and server byte checks
cannot establish that the projector now decodes that asset. If it stalls, retain
the new visible TV diagnostic and time-to-failure for the next investigation.
