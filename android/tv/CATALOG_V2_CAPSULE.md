# PH-ANDROID-TV-CATALOG-V2-01

Base source/evidence: `10711dc63f9404911cf3c8b1df509c78fb38dc95` / `181927e`.
Branch `codex/android-tv-foundation`; one Android writer. Same Android/docs scope
and preservation rules as `PLAYBACK_CAPSULE.md`.

Frozen backend runtime `a5d0f595d7cd26379ed2845a944ec1d58d7885cc`; contract SHA-256
`13cf10892dc4e91631ad71b5ee4bed21baa44697f1e779851c19026dbe606120`.
The independent 16-input manifest, schema/example and test fixtures are verified
from Git blobs. Backend evidence return/replay commit
`868cbb48aec50fa9c01689ee071c9d999e0b8e0d` is separate from the runtime pin.

1. Strict v2 adapter: bounded UTF-8/exact keys, kind/availability, revision URLs,
   descending paged IDs, prepared JPEG integrity, exact bounded MP4 Range reads.
2. Mixed gallery: explicit unavailable reasons, page jumps, video opening, native
   controls, Back/focus restoration and generation-owned cancellation. No auto audio.
3. Validate JVM parser/HTTPS/reader/store cases, actual backend ASGI replay,
   synthetic emulator at normal/large fonts, and exact APK artifact identity.
4. Keep build-time v1 default compatible with the current publication. V2 is an
   explicit build option and has no live origin/publication yet. Do not infer v2
   deployment or media coverage from compiled capability or a synthetic MP4.

No phone contract update, backend edit, live source-media copying, public ingress,
push or merge. Backend owns real preparation and canary/deployment planning.
