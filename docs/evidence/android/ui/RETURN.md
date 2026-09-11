# PH-ANDROID-UI-01 return

Base: `0970d981985cca1363dc4e98c2d8a1087f6348eb`.
Branch: `codex/android-ui-refinement`, isolated sibling checkout `mobileAppForPhotoHouse-android-ui`.
The result commit is returned with the task; this file is part of that commit.

## Result

The connected Android app has a warm neutral palette, serif album headings,
square gallery tiles with dates and media type, and a larger uncropped detail
preview before the navigation/original controls. Admission and membership screens
use distinct cards; language and sign-out are available from the header's Settings
dialog. Large text switches the grid to one column and wraps action groups.

No server fields, routes, permissions, token handling, media transport, native
player or original decoder were changed. Dates remain source text and captions
remain literal. Preview failure never requests an original as a fallback. Settings
uses memory-only state, and sign-out invokes the existing store cleanup.
The default APK remains unconfigured and cannot accept sign-in.

## Verification

All local checks passed. `result.json`, sanitized JVM XML, `build-final.log` and
instrumentation logs record the results and artifact identities:

| Check | Observed evidence |
| --- | --- |
| Shared contract | 12 operations, 38 synthetic ASGI cases, 8 checksummed files; unchanged |
| JVM | 72 tests: 22 fixture, 28 store, 19 HTTPS adapter, 3 video reader; zero failures/errors |
| Debug builds | Fixture, connected and connected instrumentation APKs built offline |
| Lint | Zero errors; existing OldTargetApi warnings and connected ExifInterface warning |
| API 36 ARM64 emulator, normal text | 9 tests passed, 45.194 seconds |
| Same emulator, 200% text | 9 tests passed, 45.856 seconds |
| Render artifacts | 20 PNGs per text size, including separately captured video frames |
| Packaging | Fixture bytes unchanged; connected APK has no fixture assets; permissions unchanged |

The JVM/whole-project verification preceded a final UI-only navigation spacing
adjustment. The final connected build, lint and both complete emulator runs use
the same final application and instrumentation APKs identified below. Neither
live-core nor the contract changed, so the backend TLS replay was not repeated.

The new emulator scenario exercises a generated three-item gallery, detail
preview, EN/ZH switching, library return and sign-out.
The existing scenarios cover invited registration, literal captions, navigation,
original viewing/zoom/privacy, native video playback/seeking/audio focus, corrupt
media, and the unconfigured secure window.

Visual inspection covered EN/ZH gallery renders, sign-in at both text sizes,
EN/ZH large-text details, and Chinese Settings at both sizes. The enlarged labels
wrap; controls remain accessible through the scrollable page. Tests exercise the
controls beyond the captured viewport. This is portrait emulator evidence, not
TalkBack, landscape, physical-device or family acceptance.

Connected debug APK SHA-256:
`ca38584656b4d864f1d2cccce398e0e0b91c5721a2bd39149676e0cc84943e97`.
Instrumentation APK SHA-256:
`532af50910f6cce0161fa75e8747e4736c23db81c4ed340d4fa658fbe03d5bac`.
The connected APK is `android/connected/build/outputs/apk/debug/connected-debug.apk`
(8,936,400 bytes). Its server origin is empty. The fixture APK is byte-identical to
the base artifact (`d16eb8f689b8700dc1c26fde09b9e8c0e9c6ea9d57572b650fb689a1a48ad88d`).

All media is synthetic: the new preview is a generated landscape illustration,
not a family photo. Screenshots use the app's own View.draw with FLAG_SECURE still
set. Settings captures its separate owned dialog window. Native video frames are
retained separately from full UI captures, whose hardware video region may be
black. No composite screenshots or private media are used.

## Reproduce

Use the existing JDK 17, SDK platform/build-tools 34 and cached Gradle 8.10.2:

```sh
python3 scripts/verify-contracts.py
scripts/verify-android.sh --offline --no-daemon
android/gradlew -p android :connected:assembleDebugAndroidTest --offline --no-daemon
android/verify-connected-emulator.sh emulator-5580 normal docs/evidence/android/ui
android/verify-connected-emulator.sh emulator-5580 large docs/evidence/android/ui
```

Set JAVA_HOME and ANDROID_HOME to existing local installations. The runner refuses
physical targets and restores the emulator font scale. The task-owned emulator
was stopped after evidence collection. No dependency or system
installation is needed. Do not point this build or tests at a real server.

## Remaining gates

This UI slice does not make the full planned product feature-complete. Scoped
search/albums/people and voice still need reviewed backend contracts; upload,
offline storage and persistent sign-in remain later scopes. No new API was invented.

No real backend is confirmed deployed, and no HTTPS origin or test audience is
approved. Real credentials and phone installation remain gated. Phone layout,
codec/audio, actual Activity lifecycle with a configured authenticated service,
performance on the family's hardware, production signing and family acceptance
have not been established here. No push, merge, deployment or hosted CI was run.

The next independent code step is a separately scoped design/contract handoff for
search and album browsing; the delivery step is an explicitly approved HTTPS pilot
with identified audience, library and phone artifact. Neither is implied by this
local UI validation.
