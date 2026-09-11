# PH-ANDROID-VIDEO-01

Owner/executor: current task and configured model; no delegation or model switch.
Task class: bounded. Base `fe6c72fea25274aa297132cb06d4e8467a36a7b5`.
Branch `codex/android-video-playback`, isolated `mobileAppForPhotoHouse-android-video`.
Backend pin `87a60b475b37b1d6873cd977bcb6e7254472da7e` stays unchanged.

Outcome: explicit permission-aware native video playback using authenticated,
bounded single Range reads; play/pause/seek and lifecycle/privacy cleanup.
Read scope: mobile source/contracts, exact pinned backend media implementation,
official Android API documentation. Write scope: android/** and this evidence.
No shared schema, other worktree, backend source, real credentials/media, phone,
push/merge/deployment or dependency/SDK installation. Preserve historical evidence.

Plan: (1) verify contract and implement strict Range transport/source; (2) wire
native player and EN/ZH controls; (3) JVM, actual-backend TLS, emulator playback
and render checks; (4) record limits/artifacts and commit locally.
Acceptance: strict headers/ranges and size caps, per-request bearer checks,
permission/denial/cancellation/generation tests, synthetic playable video,
build/lint and emulator lifecycle/seek checks at normal/large text size.
Unavailable codec/device/live gates must be reported separately.
