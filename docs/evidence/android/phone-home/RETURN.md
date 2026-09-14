# Phone Home mode — 14 September 2026

Branch `codex/phone-home-mode`, isolated worktree
`mobileAppForPhotoHouse-phone-home`, mobile base
`438bbc6fe98a0ffb71b2b9db435455399982594d`.
The result commit is the commit containing this return.

## Delivered locally

Phone versionCode **6**, versionName **0.7-phone-home**, package
`dev.photohouse.connected`. The launcher offers **At home** and **Sign in**.
Home uses the existing published TV v3 catalog, touch paging/page jump,
Ready first / Ready only / All / Photos / Videos, caption details, preview retry,
photo original/fit/fill/zoom/pan/fullscreen, current-page slideshow, and streaming
video play/pause/seek/fullscreen. EN/ZH controls retain literal caption content.

The account and Home Activities own separate stores/transports; the launcher owns
neither. Home requires both its private origin and numeric private server address.
No automatic account fallback, protected permission grant, audience expansion,
disk media cache or backend change is included. Backgrounding clears Home media
and cancels readers. The shared native phone player closes failed sources promptly
and reports a retryable error; callbacks are bound to the selected reader.

See [mode design and configuration](../../../../android/PHONE_HOME_MODE.md) and
[parity ledger](../../../../android/PHONE_TV_PARITY.md). Existing TV source and
Windows services, captioning and video preparation were not changed in this slice.

## Validation

- Debug app/test APK builds and connected lint passed with Gradle 8.10.2,
  Temurin 17 and existing Android SDK/build tools 34. Lint: **0 errors, 8 warnings**
  (ExifInterface, target SDK, dependency-version advisories and monochrome icon).
  Missing local dependency caches were restored from official repositories using
  the existing pinned versions; subsequent builds worked offline. No SDK installed.
- **198 JVM tests passed**: 95 home-core and 103 live-core, no failures/errors/skips.
  Includes native-error handling after source closure and rejection of a stale
  reader's callback. TV/fixture/iOS suites were not rerun for this phone-only change.
- **10 offline contract/boundary verifiers passed**. On-demand and readiness
  producer checks also passed against an exact detached checkout of backend pin
  `a72aa320801787cd066c6e04c33764e9271c9ab5`. No pin/checksum changes were needed.
  Checking the active producer worktree first exposed a later docs-only HEAD;
  that checkout was preserved and the exact pin was used for replay.
- API 36 phone emulator, 1080×2400, density 420: **all 22 normal-font scenarios
  passed across the suite and focused rerun**. The suite passed 21 of 22; the
  access-navigation test initially failed because an Android System UI ANR dialog
  held window focus and intercepted Back. After clearing that emulator dialog,
  the focused test passed using system Back after destination-window focus.
  This is aggregate evidence, not a single green 22-test invocation.
- **6 affected scenarios passed at 200% font size in one run**: chooser routing,
  Home filters/paging/photo/originals, Home streaming/seek/malformed-video retry,
  denied-Home clearing, protected on-demand photo delivery, and shared native
  video fit/fill/fullscreen. Font scale was restored; the task-owned emulator was
  stopped after testing. Physical Android devices were not installed or tested.
- EN/ZH Home, chooser and retry-error renders inspected at normal and 200% font
  sizes. Large text uses scrollable cards/one-column media; all controls remained
  reachable. Screenshots are synthetic, own-View Canvas captures under FLAG_SECURE.
  TextureView video pixels are separate from the Canvas UI capture; the fullscreen
  screenshot shows the overlay only. Native player behavior has instrumentation
  evidence, not physical phone/TV codec acceptance.
- Shell syntax and `git diff --check` passed. No real credentials/media were used
  in tests; no real server access, deployment, restart, push or merge in this slice.

Retained logs: [normal suite](instrumentation-normal-suite.log),
[focused navigation rerun](instrumentation-normal-access.log),
[200% affected tests](instrumentation-large.log), [source guards](guards.log).
Renders: [normal gallery](screenshots/normal/home-phone-en.png),
[Chinese gallery](screenshots/normal/home-phone-zh.png),
[large gallery](screenshots/large/home-phone-zh.png),
[large error recovery](screenshots/large/home-phone-video-error.png).

Reproduction (use the existing SDK/JDK and an explicitly selected emulator):

```sh
gradle -p android :home-core:test :live-core:test :connected:assembleDebug \
  :connected:assembleDebugAndroidTest :connected:lintDebug --offline \
  -PphotohouseOrigin= -PphotohousePhoneHomeOrigin= \
  -PphotohousePhoneHomeLanAddress=
android/verify-connected-emulator.sh emulator-SERIAL normal EVIDENCE_DIRECTORY
python3 android/verify-on-demand-contract.py --backend-root EXACT_BACKEND_PIN
python3 android/verify-readiness-browse-contract.py --backend-root EXACT_BACKEND_PIN
```

The complete verifier expects 22 tests. The retained large-font run selected
`AccessModeTest,HomePhoneTest,OnDemandPhotoTest,ConnectedUiTest#nativeVideoFitFillAndFullscreenKeepTheSameReaderAndRestoreControls`
with `am instrument -w -r -e class` after setting font_scale to 2.0.

## Artifacts and remaining gate

The unconfigured test app is 10,637,097 bytes, SHA-256
`df08dd7043a53980c7026514755a20c44b0b00dd3906220b955e97949f0964e7`.
The final test APK is 4,933,996 bytes, SHA-256
`2862139a3d4b0a7333ab416bdd0fe80b5d2ac4c6ba0475406dc2977f884f7440`.
The app uses the existing debug signer, SHA-256
`56d7591b2b6c2538d506d1fe51327444f2307736cb12f5beefa08f1c410d6d28`.

A separate private configured Home APK is prepared for local delivery; its receipt
is [DELIVERY.md](DELIVERY.md). APKs, private routing and signing files stay outside
this public repository. The configured build is not installed on the emulator.
Its protected account endpoint remains unconfigured.

Next: verify the intended phone's LAN address against the server's approved peer
list, then install the candidate and test real paging, previously failing media,
long-video seek, photo originals and Wi-Fi loss/recovery. Installing an APK alone
does not admit a new peer. Same-Wi-Fi detection and broad-subnet admission are absent.

Protected WAN access/production acceptance, protected prepared-media/readiness
parity, Home search/people/date/tag integration, themes/topics/named albums,
GPS/radius search and negotiated 4K remain outside this completed slice.
