# PH-ANDROID-ORIGINAL-PHOTO-01

Owner/executor: current Android session; no delegation or model switch.
Base: `93e9716d5cae2dc2086695d6b83fdbc3daa76934`.
Branch/worktree: `codex/android-original-photo-viewer`, isolated sibling
`mobileAppForPhotoHouse-android-originals`.
Backend pin: `87a60b475b37b1d6873cd977bcb6e7254472da7e`.

Outcome: an explicit, permission-aware original-photo viewer with zoom and pan,
using the existing authenticated original-media operation. No new wire contract.

Scope: Android UI, API adapter, state, synthetic test harnesses and Android evidence.
Read scope: mobile AGENTS, plan/security/contract references, previous capsules and
source; pinned backend authorization/media source only as needed to verify the
existing operation. One writer owns this worktree. Other worktrees, shared contract,
backend source, iOS and coordinator parity remain unchanged.

Plan: add bounded transport and lifecycle state; implement memory-only decoder and
viewer; verify permission, cancellation, metadata/byte limits, EXIF, gestures and
privacy; build, record artifacts, commit locally.

Invariants: no automatic original fallback; detail permission plus server-enforced
authorization; fixed same-origin media path; existing TLS policy; no disk cache,
downloads, gallery export or credential persistence. Closing/navigation/privacy
boundaries cancel the request and clear original data; late replies cannot restore
it. Compressed input is capped at 12 MiB; displayed bitmaps at four million pixels.

Acceptance: JVM regressions, real pinned-backend loopback TLS checks, Android build
and lint, synthetic emulator UI/decoder tests at normal and 200% text sizes, and
visual inspection. Existing image/toolchain installations only. Source/runtime
evidence is separate from real deployment and phone acceptance.

Return exact commit, source/APK hashes, tests and remaining limits. Live origin,
real credentials/media, deployment, physical phone installation, push and merge
remain gated. Search/albums/voice need separately reviewed backend contracts.
