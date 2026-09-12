# Photo navigation implementation — 2026-09-11

Task `PH-ANDROID-PHOTO-NAVIGATION-01` is complete for local Android development.
Base: `0cabd3efcb1ebeaab63deef864dbc56f515991d3`. Isolated branch:
`codex/android-photo-navigation`. Backend pin remains
`87a60b475b37b1d6873cd977bcb6e7254472da7e`. The exact result commit is returned
with this report; [structured evidence](result.json) binds the tested source files
and artifacts by SHA-256.

## User-visible result

Opening a photo on gallery page 2 and pressing either **Back to Photos** or Android
Back now reloads page 2. Previously both returned to page 1. Photo detail also has
**Previous photo** / **Next photo** controls and an indicator such as “Photo 2 of 2
· Page 2”, with English and Simplified Chinese labels.

Navigation follows the deduplicated order of the current page and stops at that
page's boundaries. It does not invent a cursor or load the next gallery page.
Every selected photo gets fresh authenticated detail, captions and thumbnail
requests. Controls are disabled at the first/last photo and during a pending read.
The Back action can cancel a pending photo load. New navigation starts at the top
of the scrollable screen; exact gallery scroll-position restoration is deferred.

Only the current page's IDs and position are retained in memory. Logout, expiry,
backgrounding, library selection and return to the library list clear that
context. A failed read clears photo content and navigation; an explicit offline
retry can reload the selected photo and restore its return page. A 401 clears
context and rechecks the session once without automatically retrying the denied
photo. Late replies cannot restore detail after Back or a library switch.

## Validation

| Check | Observed result |
| --- | --- |
| Ordinary JVM suites | 54 passed: 22 fixture, 19 connected store, 13 HTTPS adapter |
| New store regressions | 5 passed, including boundaries, races, privacy clearing, denial and offline retry |
| Actual-backend TLS suite | 6 passed; navigation and between-photo membership revocation added to the existing state test |
| Connected emulator component suite | 4 passed at font scale 1.0 and 4 passed at 2.0 |
| Android build | Both debug APKs and connected instrumentation APK built |
| Lint | Zero errors; existing `OldTargetApi` warning in each app |
| Contract and package guards | Passed; unchanged pin/fixtures, correct permissions, empty connected origin |
| Diff/evidence checks | Passed; source hashes, JSON/XML and synthetic screenshot identity recorded |

The emulator checks exercise previous/next buttons, page/position labels in both
languages, button boundaries, both Back actions and logout. Existing admission,
invited registration, literal caption and secure/unconfigured-screen checks also
pass. Normal and large-text navigation screenshots were visually inspected:
controls and labels remain readable; content below the viewport is scrollable.

The backend tests use the actual pinned protected app through temporary loopback
TLS, migrated SQLite and synthetic media. They are local JVM interoperability
evidence. This task did not repeat the unchanged backend's full 211-test suite or
the caption-only 14-case probe.

## Artifacts and reproduction

Connected debug APK, local ignored output:
`android/connected/build/outputs/apk/debug/connected-debug.apk`.

SHA-256:
`83e7cc1dd36f68352d116a63749ef349c3695689d0d9b6999706dff523dbfcda`.

[All APK hashes and source identities](result.json),
[backend result](backend-integration-result.json), and sibling sanitized JUnit XML
files record the checks. Earlier repin and integration evidence was preserved.
The emulator runner now accepts an optional evidence directory so a new run can
retain its results without overwriting an earlier slice.

Use existing JDK 17, SDK/build-tools 34 and cached Gradle 8.10.2. Set `JAVA_HOME`,
`ANDROID_HOME`, `PHOTOHOUSE_BACKEND_REPO` and `PHOTOHOUSE_BACKEND_PYTHON` to the
selected local tools/repository. From this worktree:

```sh
bash scripts/verify-android.sh --offline --no-daemon
android/gradlew -p android :connected:assembleDebugAndroidTest --offline --no-daemon
python3 -B android/integration/verify-backend.py \
  --backend-repo "$PHOTOHOUSE_BACKEND_REPO" --python "$PHOTOHOUSE_BACKEND_PYTHON"
bash android/verify-connected-emulator.sh emulator-SERIAL normal docs/evidence/android/navigation
bash android/verify-connected-emulator.sh emulator-SERIAL large docs/evidence/android/navigation
```

The backend runner's default result files were archived here and their previous
committed versions retained. The explicitly selected local API 36 emulator was
started from an existing image, tested, and stopped. Font settings were restored
by the verification script. No emulator image, SDK or system tools were installed.

## Synthetic UI evidence

[English, normal text](screenshots/normal/photo-navigation-en.png) ·
[Chinese, normal text](screenshots/normal/photo-navigation-zh.png) ·
[English, 200% text](screenshots/large/photo-navigation-en.png) ·
[Chinese, 200% text](screenshots/large/photo-navigation-zh.png).

These captures use the component-test adapter with unavailable synthetic previews.
They verify navigation and layout; they do not show a live family library.

## Remaining gates

No push, merge, backend deployment, real credentials, real photos or physical-phone
installation occurred. Default connected configuration remains empty. UI styling,
video/originals, search, albums and other new contract capabilities remain deferred;
this result adds navigation to the implemented browsing flow.

Controlled staging still requires explicit host/deployment authority, an approved
system-trusted HTTPS origin and test audience. A configured APK and real-phone
acceptance follow that independent gate. The shared contract, backend, iOS and
coordinator-owned parity ledger were not changed.
