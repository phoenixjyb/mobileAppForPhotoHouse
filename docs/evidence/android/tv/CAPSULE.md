# PH-ANDROID-TV-01

Base ae91426b54faab1d12d494152e3b5473c457e977; branch
codex/android-tv-foundation, isolated Android TV worktree. Current task owns writes.
Route: bounded implementation; current executor retained, no delegated agents.

Plan: (1) separate TV module and remote UI, (2) bounded high-quality image path,
(3) JVM/build/lint and synthetic emulator checks, (4) exact artifacts and handoff.
Writes: android/** and docs/evidence/android/tv/**. Shared contracts stay frozen.
Read existing mobile agreements/design/security/contracts and Android implementation.
Reuse native auth, library checks and generation-bound state; no backend writes.

TV viewer: no personal sign-in screen (user correction during implementation),
library and paged grid, full-screen photo,
remote Back/OK/arrows, EN/ZH, literal captions and explicit current-page slideshow.
Detail prefers cached size 1024 with cached default fallback on 404 only. Explicit
original mode requires server originals_allowed, retains 12 MiB transfer bound,
and decodes to at most 8,847,360 pixels, preserving 3840x2160 source resolution.
No manufactured album/search APIs, original permission, refresh or pairing tokens.

Acceptance: focused media regressions, unchanged phone tests, TV build/lint,
synthetic remote/emulator/decoder/privacy tests where existing tooling permits,
unconfigured artifact with signature and SHA receipts; a configured automatically
connecting build is deferred until the home-access contract is selected and implemented. Existing phone AVD
may establish landscape component evidence, never actual JMGO compatibility.

No physical projector install/run, real media, credentials, backend/routing changes,
SDK downloads, public ingress, push or merge. LAN access retains HTTPS and auth.
Next gate: exact JMGO firmware/API/ABI, install route, normal LAN DNS/system trust,
operator approval, prepared high-resolution previews or reviewed original grants.

User correction: everyday TV access must connect automatically, without sign-in.
Personal admission UI was removed. The choice between one-time approved device
access and an unauthenticated selected LAN feed is pending user input. Neither
contract exists in the pinned backend. Do not embed credentials, silently invent
endpoints or weaken the phone/protected API. Continue independent TV UI/build/tests
and return the concrete backend dependency.
