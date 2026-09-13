# Phone direct media and long-video return

2026-09-12. Source commit `4c124e003f5f329c5d77b4726a29b2d9ba233d1e` on
`codex/android-phone-parity-streaming`, based on merged default
`8fdd280be55724ca020d12113cacc7a40463d08b`. Worktree is the isolated mobile
`-android-phone-streaming` checkout; prior checkouts remain untouched.

## Behavior

A gallery tap opens a permitted image viewer or prepares a video. The separate
Details button keeps captions/metadata accessible. The server's fresh detail
controls media kind and original access; denied users remain in details without
original reads. Play remains explicit. Navigation page context survives viewer
close, and late results cannot reopen media after privacy boundaries.

Phone video accepts file sizes through 32 GiB with 64-bit offsets and at most
256 KiB per authenticated read. No whole-file allocation/download or persistent
cache is added. Time labels use minutes/hours, buffering and seeking have visible
feedback, and a stalled seek uses a 30-second failure guard. Phone build 3 is
unconfigured and debug-signed; no physical phone/TV installation occurred.

## Fresh evidence

- 80 protected-phone JVM tests and 85 home-core JVM tests passed, zero skips.
  The added large-offset cases exercise beginning, beyond 4 GiB, tail and backward
  reads against an 8.19 GB logical file using small buffers. Repeat phone reads
  reauthorize and a denial closes the reader without copying bytes.
- Phone app and instrumentation APK builds passed; lint has zero errors and two
  existing warnings. Frozen protected/home/catalog/discovery manifests and the
  phone/TV source boundary checks passed. `git diff --check` passed.
- All 15 API-36 emulator tests passed at 1x and 2x text sizes. This includes fresh
  detail permission/kind behavior, direct photo/video opening, captions, page
  navigation, photo zoom/slideshow, EN/ZH, fullscreen, player lifecycle and privacy.
- The 125-second mathematical H.264/AAC fixture presents Play before the decoder
  has requested its full file, seeks to 90 seconds, 123 seconds and back to two
  seconds, plays/pauses and closes on background. It is packaged only in the test
  APK. The app APK has no assets and has an empty origin.
- Reviewed gallery and video-control renders at both sizes. Software View drawing
  omits the hardware video layer (black rectangle); separate `-frame.png` captures
  verify the decoded mathematical pattern and correct aspect ratio. No secure-window
  bypass or real-media screenshot was used.
- APK permissions remain Internet plus the internal AndroidX signature permission.
  Staged source Gitleaks scan reported no findings. Exact APK/test identities are in
  [validation.json](validation.json); the APK is retained privately outside Git.

Commands: JDK 17 / Android SDK 34, Gradle 8.10.2; `:live-core:test`,
`:home-core:test`, `:connected:lintDebug`, `:connected:assembleDebug`,
`:connected:assembleDebugAndroidTest` with `--offline --no-daemon`; then
`android/verify-connected-emulator.sh emulator-5580 normal|large`.

## Continuing work

The backend owner is qualifying real full-library preparation independently;
this change neither publishes its output nor changes a service. Real 8K/long-file
throughput and physical-projector playback are not proven by the small synthetic
fixture. Fault-injected native buffering/seek-timeout acceptance remains separate
from the successful seek and transport-denial tests here.

Protected phone discovery HTTP is a backend candidate in progress; prepared
viewer image/video delivery needs another reviewed capability/contract. People,
date/tag/place search cannot be enabled by routing the phone to anonymous home
endpoints. Original images still retain their 12 MiB compressed-byte bound.
[Streaming/cache policy](../../../../android/MEDIA_STREAMING.md) recommends
progressive Range playback first and records the requirements for an optional
bounded disk cache; persistent caching is not enabled by this slice.

No real credentials, new public origin, physical-device installation, backend
activation or captioning change is part of this Android commit. The source and
evidence are local; hosted verification and a new PR have not been run for it.
