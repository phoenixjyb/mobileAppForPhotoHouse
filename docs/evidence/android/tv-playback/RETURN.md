# TV photo controls and native playback components

Source `10711dc63f9404911cf3c8b1df509c78fb38dc95`, base
`b2fcdadafb070c32c3d7a8657eb537043ad1acbb`, branch `codex/android-tv-foundation`.

Implemented photo Fit/Fill, 1×/2×/4× remote zoom, bounded panning, Back reset,
full-screen hints that hide after four seconds, and slideshow transform reset.
The independent native video component has explicit play/pause, 10-second seek,
aspect-fit rendering, duration, full screen without surface replacement, audio
focus and owned teardown on background/error/close. Replacing a video source
closes the old player and suppresses its callbacks without autoplaying the next.
HomeVideoReader serializes bounded memory-only random reads and cancels on close.

Validation: 35 home-core JVM tests (six new reader cases), 13 Android component
tests at each of 1.0 and 2.0 font scale, debug/test APK builds and lint passed.
Actual native synthetic frames were inspected and non-uniform pixels asserted;
preparation alone was not counted as playback. Emulator API 36, landscape
1920×1080 phone AVD, not a physical TV image. Six retained screenshots were visually
reviewed; large-font toolbars scroll horizontally through remote focus.
The immutable v1/phone contract and TV boundary verifiers passed.

Private v4 APK: versionCode 4, versionName `0.4-tv-viewer-dev`, package
`dev.photohouse.tv`, 8,868,243 bytes, SHA-256
`299532b557fafea9bb97e97374a7feef21fc6ca593207aaf9b43a41c0357b1ae`.
Exact source and public unconfigured artifact hash are in `result.json`. The private
build retains the existing home origin and exact-host LAN mapping; no system DNS
or server state changed. Synthetic MP4 exists only in the test APK.

This checkpoint's production gallery still consumes v1 photos; the native video
component is not a claim of v2 catalog integration. The backend returned frozen
v2 during packaging, so the next source slice will wire and synthetically test
mixed assets and validated Range reads. Real v2 derivative preparation, live
origin/publication and JMGO codec/audio/remote/sleep-wake acceptance remain open.
V4 was not installed on a physical device. No push, merge or backend deployment.
