# Authenticated video playback — 2026-09-11

Task `PH-ANDROID-VIDEO-01`, isolated branch `codex/android-video-playback` in
`mobileAppForPhotoHouse-android-video`. Base
`fe6c72fea25274aa297132cb06d4e8467a36a7b5`; final commit returned separately.
Backend remains pinned to `87a60b475b37b1d6873cd977bcb6e7254472da7e`.
No shared contract, backend source or other worktree was changed.

## Behavior

Video details with `originals_allowed=true` offer **Open video**. Opening prepares
an Android MediaPlayer; Play starts it explicitly. English/Chinese controls provide
play/pause, ten-second backward/forward seek, a seek slider, elapsed/duration text
and Close. Back/Close preserves the detail and current gallery return page.
The native player runs on its own looper, with a fitted TextureView inside the
existing secure Activity. Audio focus loss and headphone disconnection pause;
regaining focus does not automatically resume. No background playback is provided.

The player receives a borrowed MediaDataSource, never a URL or bearer. Its owner
uses the existing authenticated `/assets/{id}/media?library=…` operation. Each
nonempty read, including repeated reads and seeks, is freshly authorized through
the same trusted HTTPS adapter. Requests send one bearer, no-store, identity encoding
and exactly one bounded Range, with no If-Range, redirect or full-download fallback.
The adapter requires 206, MP4/WebM MIME and an exact finite Content-Range/body match.
It rejects changing lengths, compressed ranges, partial/malformed responses and
oversized bodies. There is no application chunk cache, media file or new dependency.

Close, navigation, logout, expiry and backgrounding close the reader immediately,
cancel pending calls, drop credential-bearing callbacks and release player/surface
and audio focus. Late reads cannot restore state or copy into buffers after close.
Callbacks from an old player cannot close its replacement. Transport 401 clears
private state and rechecks session once; offline/429/503 retry reloads detail and
permission before another explicit Open video. Unsupported/corrupt media exits the
player with an unavailable message. The adapter is borrowed: framework closure during
failed preparation cannot prematurely close the owner and suppress error delivery.

## Limits and remaining acceptance

- MP4 and WebM MIME accepted; actual codecs depend on Android/device support.
  Only the generated H.264/AAC MP4 is decoded in this emulator evidence.
- Maximum 256 KiB response per read and 4 GiB file size; no complete video download.
  Android's internal decoder/buffer memory is separate from the transport bound.
- Range calls time out after 20 seconds; player preparation has a 30-second guard.
- Buffered frames/audio cannot be recalled on remote revocation until a subsequent
  request observes denial. There is no push revocation channel or DRM guarantee.
- No MOV/HEIF/RAW guarantee, casting, playlists, background service, picture-in-picture,
  sharing, downloads, persistent sign-in or offline media.
- Emulator audio output is disabled. Focus behavior is exercised; audible output,
  physical headphone disconnection, broad codecs, large/long-video performance and
  physical-device acceptance remain unverified.

## Validation

| Check | Result |
| --- | --- |
| JVM | 72 passed: 22 fixture, 28 store, 19 transport, 3 reader |
| Actual pinned backend over local TLS | 7 passed |
| Full emulator suite, API 36 | 8 passed at 1.0 text scale; 8 at 2.0 |
| Final capture-only harness replay | 2 video tests passed at each scale; application APK unchanged |
| Build | Fixture, connected and connected instrumentation APKs succeeded |
| Lint | Zero errors; OldTargetApi warnings and a platform ExifInterface warning in the unchanged original-photo viewer |
| Contract | 12 operations, 38 cases, 8 checksummed files passed; backend pin unchanged |
| Artifact/privacy checks | Current APK permission/asset checks, source/APK hashes, diff/syntax checks passed |

The full-suite logs identify the same final application APK. After the test-only
frame extraction change, the two affected video tests were replayed at both text
sizes with the final instrumentation APK. Structured [result.json](result.json)
records both evidence sets and current artifact hashes. There are 26 synthetic
images: full UI renders and separately extracted decoded frames. Four final EN/ZH
UI renders and two decoded frames were visually reviewed. The emulator font scale
was restored to 1.0, and the task-owned emulator was stopped.

Connected APK: `android/connected/build/outputs/apk/debug/connected-debug.apk`.
SHA-256: `7f43cf2691f683b7376d1539c7dc06b42a5e2ca57fb6c90040e2150c8c27c58e`.

[English UI](screenshots/normal/video-en.png) · [Chinese UI](screenshots/normal/video-zh.png) ·
[English large text](screenshots/large/video-en.png) · [Chinese large text](screenshots/large/video-zh.png) ·
[Decoded English-test frame](screenshots/normal/video-en-frame.png) · [Decoded Chinese-test frame](screenshots/large/video-zh-frame.png).
The black video region in the software UI renders is a capture limitation; the
separate frames and pixel/aspect assertions verify actual decoding.

The initial emulator run found one failing corrupt-media regression: Android closed
the adapter during failed preparation, suppressing the owner's error callback and
leaving loading visible. The borrowed-adapter ownership fix is covered by the final
replay. A later assertion was synchronized with the completed error state rather
than racing the intermediate player-clear update.

Software View.draw can omit the hardware TextureView layer. Decoded synthetic
texture frames are therefore saved separately from the full UI renders; no frame
is composited into a screenshot, and FLAG_SECURE stays enabled. Tests check varied
pixels and both native-texture and Compose-surface aspect ratios. Review also
added handling for video-size callbacks delivered after preparation, retaining a
valid initial aspect ratio. Earlier attempts to composite frames were discarded
because AndroidView coordinates produced unreliable evidence images.

JVM checks cover Range/header/body validation, no retries, cancellation, bounded
random reads, stable file size, EOF, zero-length reads, closed callbacks, expiry,
permission gates, privacy boundaries, stale-player callbacks and metadata-first retry.
The actual backend uses a temporary migrated SQLite database, loopback TLS and the
same generated video, verifying denied/granted reads, tail ranges, cross-library
denial and loss of original permission while ordinary membership remains available.
The exported source and temporary backend data are removed after testing.

The synthetic 20-second, 320×180 H.264/AAC clip is generated from mathematical color
patterns and a sine tone, not private media. It lives only in Android test assets.
[Generation instructions](../../../../android/connected/src/androidTest/assets/README.md).

## Reproduction

Use the existing JDK 17, SDK/build-tools 34, cached Gradle 8.10.2 and backend test
Python. With JAVA_HOME/ANDROID_HOME set, from this worktree:

```sh
bash scripts/verify-android.sh --offline --no-daemon
android/gradlew -p android :connected:assembleDebugAndroidTest --offline --no-daemon
python3 -B android/integration/verify-backend.py \
  --backend-repo "$PHOTOHOUSE_BACKEND_REPO" --python "$PHOTOHOUSE_BACKEND_PYTHON" \
  --evidence-dir docs/evidence/android/video/backend
bash android/verify-connected-emulator.sh emulator-SERIAL normal docs/evidence/android/video
bash android/verify-connected-emulator.sh emulator-SERIAL large docs/evidence/android/video
```

The backend runner now accepts an Android-owned evidence directory, preserving
historical results. No fixtures or shared checksums were regenerated. No SDK,
emulator image, model, dependency or system installation was needed.

Implementation references: Android's [MediaDataSource contract](https://developer.android.com/reference/android/media/MediaDataSource)
and [MediaPlayer lifecycle](https://developer.android.com/reference/android/media/MediaPlayer).
No reference implementation code was copied.

## Next work and unchanged gates

UI refinement is the next client-only slice. Scoped search, albums and people filters
need reviewed backend operations/contracts. Persistent sign-in, uploads/export,
voice and owner/account tools remain separate work. A trusted deployed HTTPS origin,
approved audience/test library and configured artifact are still prerequisites for
real credentials and a phone pilot. This unconfigured debug APK is not a release.
No push, merge, deployment, real service/media/credentials, phone installation,
production signing or store submission occurred. Hosted CI was not queried/run.
