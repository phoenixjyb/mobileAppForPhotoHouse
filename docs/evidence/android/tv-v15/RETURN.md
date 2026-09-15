# TV v15: bounded recovery for interrupted video reads

Base: `bb1f5adac8edb466b5c3d6347fb010064b3c940f`.
Branch: `codex/tv-read-recovery-v15`.
Worktree: `/Users/yanbo/Projects/mobileAppForPhotoHouse-tv-v15`.

The user confirms longer videos play on v14, but occasional interrupted/offline
errors require reopening. Current catalog reads close the source after one network
interruption. The shared Home catalog adapter now retries only OFFLINE failures,
at most twice (250/750ms), within one 20-second deadline per exact byte range.
All retries keep the same frozen asset/revision/range and TLS/framing checks.
No partial failed response enters the decoder buffer. Cancellation stops HTTP and
backoff. Unknown IO failures, TLS, denial, changed revision, malformed media,
rate limiting and server-unavailable responses retain fail-closed behavior.
The reader retains only its existing 256 KiB window. No player replacement, whole
movie download, backend change or protected-phone transport change is included.
Phone Home mode shares this source change; no phone APK is delivered in this slice.

Verification: 103 Home JVM + 2 TV JVM checks pass; zero failures/errors/skips.
The real loopback-TLS regression drops a response midway, verifies a repeated
identical range recovers exact bytes and keeps the source open; three dropped
responses fail without leaking partial bytes. Virtual-time checks cover attempt/
deadline bounds, cancellation and excluded error categories. Seven synthetic
API 36 ARM64 emulator checks pass: actual frame/play/pause/seek/fullscreen,
background/source replacement/close, malformed media, interrupted reads retained
in the viewer, preview retry, and independent native setup timeout.
The captured paused synthetic frame was visually inspected. TV lint, TV debug/
test APK and phone connected debug builds pass. Frozen contract, TV boundaries,
v2 catalog, v3 on-demand and readiness-browse pin checks pass. No dependencies
or toolchain versions changed. See checks.json, build.log and native.log.

Configured private TV v15 is built separately with unchanged home connection
settings; delivery/signature/checksum receipts stay outside this public repository.
The source diagnosis is a reproduced recovery gap, not proof of the projector's
underlying network cause. Actual v15 projector recovery remains to be tested.
No device installation, push or merge performed.
