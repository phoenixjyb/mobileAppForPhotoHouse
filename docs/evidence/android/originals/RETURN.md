# Original-photo viewer — 2026-09-11

Completed local slice: `PH-ANDROID-ORIGINAL-PHOTO-01`.
Base `93e9716d5cae2dc2086695d6b83fdbc3daa76934`; branch
`codex/android-original-photo-viewer` in the isolated
`mobileAppForPhotoHouse-android-originals` worktree.
Backend pin remains `87a60b475b37b1d6873cd977bcb6e7254472da7e`.
The final commit is returned separately; [result.json](result.json) binds every
changed Android source file, test and APK to its SHA-256.

## Delivered behavior

Image details with original permission now offer **Open original photo**.
The app requests the fixed same-origin, bearer-protected
`/assets/{id}/media?library=…` endpoint only after that explicit action.
It never uses an arbitrary media URL, silently replaces a missing thumbnail with
an original, or exports the file.

The full-screen viewer supports pinch and pan, double-tap zoom, zoom-in/out
buttons and fit-to-screen reset, with English/Chinese labels. It displays the
original image bytes, with EXIF orientation applied. Back/Close returns to detail
and preserves the existing page navigation. Closing, navigation, backgrounding,
expiry and logout clear original state and cancel pending reads. Late replies
cannot restore the viewer. A denied original clears private content and rechecks
session once. After an offline failure, explicit retry reloads metadata and
permission before another original can be requested.

## Deliberate limits

- JPEG, PNG and WebP only; video and HEIF/RAW are not included.
- Compressed response limit: 12 MiB, enforced for declared and unknown lengths.
- Original responses must be HTTP 200; unsolicited partial responses, redirects,
  empty bodies and unsupported content types fail.
- Bitmap bounds are inspected first. Dimensions above 32768 are rejected;
  power-of-two sampling limits displayed bitmaps to four million pixels.
  Large images show a reduced-resolution notice.
- Decoding is serialized off the UI thread. Corrupt/unsupported images show an
  unavailable message. Orientation supports all eight EXIF transforms.
- Zoom ranges from 1× to 5×. Pan is bounded by the fitted image and viewport.
- No disk cache, downloads, sharing, persistent session or external viewer was added.
  This is bounded original-photo viewing, not unrestricted native-resolution decoding.

The 512 KiB JSON and 1 MiB thumbnail limits, shared contract and system TLS policy
are unchanged. Original permission is enforced by the server in addition to the
client's detail-state check. Default APK origin remains empty.

## Validation

| Check | Observed result |
| --- | --- |
| Ordinary JVM tests | 63 passed: 22 fixture, 25 store, 16 HTTPS adapter |
| New JVM coverage | 6 store cases and 3 transport cases |
| Actual-backend loopback TLS | 6 passed; added original grant/deny, cross-library denial and live store cleanup |
| Emulator UI/decoder | 6 passed at 1.0 text scale and 6 at 2.0 |
| Builds | Fixture APK, connected APK and connected instrumentation APK succeeded |
| Lint | Zero errors; existing `OldTargetApi` warning per app |
| Shared contract/permission/source checks | Passed; existing fixture bytes and backend pin unchanged |
| Diff, source/artifact and evidence checks | Passed |

The transport tests include exactly 12 MiB accepted, one byte over rejected for
both declared/chunked bodies, invalid MIME/partial/empty responses, 401/403/404,
redirect refusal and cancellation. Store tests cover explicit permission, no
thumbnail fallback, video exclusion, closing during loading, stale completion at
privacy boundaries, denial, missing/oversized files and offline metadata retry.

The actual protected backend is tested through ephemeral loopback TLS and temporary
migrated SQLite/synthetic JPEGs. The test harness adds only finite owner-mediated
original grant/deny controls over its private parent pipe. It verifies denied
original reads, successful granted reads, cross-library denial, and revoked
original permission while ordinary library membership remains available. Backend
source and the original checkouts are unchanged. Temporary source/data/TLS material
is removed; earlier integration reports are preserved.

Emulator tests exercise pinch, pan, double-tap, buttons, Back/Close, reopening with
reset zoom, and background clearing. Decoder checks cover corrupted bytes, bounded
downsampling, all eight pixel orientation mappings and a generated JPEG carrying
an actual orientation-6 EXIF tag. Existing admission, navigation, secure-window and
literal-caption checks also pass. A test-only Float-literal compile error in the
pinch gesture was corrected before the successful instrumentation build.

## Artifacts and reproduction

Connected APK: `android/connected/build/outputs/apk/debug/connected-debug.apk`.

SHA-256:
`a05eb90ba576d4b649094b2c5982cc547f2f03c81ac55a1a19c230af56c5bf3a`.

The APK is local, debug-only and unconfigured. [Structured evidence](result.json)
includes all APK hashes, source hashes, lint, JVM and emulator counts.
[Backend result](backend-integration-result.json) and sibling sanitized JUnit XML
files contain the local interoperability evidence.

Use existing JDK 17, SDK/build-tools 34, cached Gradle 8.10.2 and the existing
backend test Python. With the usual local tool variables set, from this worktree:

```sh
bash scripts/verify-android.sh --offline --no-daemon
android/gradlew -p android :connected:assembleDebugAndroidTest --offline --no-daemon
python3 -B android/integration/verify-backend.py \
  --backend-repo "$PHOTOHOUSE_BACKEND_REPO" --python "$PHOTOHOUSE_BACKEND_PYTHON"
bash android/verify-connected-emulator.sh emulator-SERIAL normal docs/evidence/android/originals
bash android/verify-connected-emulator.sh emulator-SERIAL large docs/evidence/android/originals
```

The existing API 36 emulator was started, tested and stopped. The verification
script restored font settings. No image, SDK or dependency installation occurred.
The backend runner's fresh output was archived here and its prior evidence kept.

## Synthetic screenshots

[English, zoomed](screenshots/normal/original-photo-en.png) ·
[Chinese, fitted](screenshots/normal/original-photo-zh.png) ·
[English, large text](screenshots/large/original-photo-en.png) ·
[Chinese, large text](screenshots/large/original-photo-zh.png).

These four renders were visually inspected. Controls remain readable at 200% text
size. The colored shapes are generated test images, not real family media.

## Outstanding work after this slice

| Next capability | Remaining work |
| --- | --- |
| Authorized video | Bounded authenticated Range streaming, player lifecycle/seek/cancellation and device privacy tests |
| UI refinement | Gallery/detail composition and broader accessibility/device review |
| Search, albums, people filters | Reviewed scoped backend operations and contracts, then Android wiring |
| Persistent sign-in | Separate secure-storage and revocation lifecycle implementation |
| Uploads, offline/export, sharing | Backend/ownership contracts and retention/privacy decisions before client implementation |
| Voice | Reviewed search/intent/audio contracts and provider/runtime scope |
| Account deletion and owner tools | Backend operations and ownership/recovery policy |
| Live pilot and release | Approved protected deployment/origin/audience, configured APK, phone tests and signing/distribution |

Original viewing is one completed feature slice, not completion of the whole
roadmap. No push, merge, deployment, real credentials/media, physical phone
installation or store submission occurred. No live service or hosted CI was queried.
The unchanged backend's full security suite and caption-specific probe were not
repeated; the relevant native/TLS suites were rerun for this implementation.
