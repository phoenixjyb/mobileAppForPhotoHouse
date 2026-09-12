# PH-ANDROID-BACKEND-INTEGRATION-01

User requested continued implementation after the connected browsing PR. Current
owner executes this bounded integration slice; no model change or delegation.
Base: `1f42d5d93e273b4a39b09a5f1a121ff4f7fea6a6`; branch
`codex/android-backend-integration`; isolated worktree
`../mobileAppForPhotoHouse-android-integration`.

Outcome: prove the actual Android HTTPS adapter interoperates with the pinned
protected Python backend, migrations and SQLite using synthetic data. Existing
MockWebServer and UI component evidence remains distinct.

Reads: mobile repository and documented backend foundation source at
`1e394f789ff1f7cef6d9930bb541186684f5a9a0`, including its agent-memory references.
Writes: Android-owned code, scripts and evidence only. Export immutable backend
source into a disposable ignored test directory; do not edit its checkout or
publish copies of backend source. Use its existing root test interpreter.

Tests may create a short-lived loopback TLS listener and runtime-generated test
certificate, temporary migrated SQLite and generated geometric images. Python
outbound connections/process launches are blocked during serving. No production
origin, private configuration, real account/media, provider/model, persistent
listener, service deployment, device installation or merge is included.

Acceptance: pinned source/checksum checks; real TLS login, invitation admission,
scoped gallery/detail/captions/thumbnail, forbidden reads, logout, membership and
session revocation, unavailable storage and admission cooldown; existing JVM
regressions and app build/lint; cleanup and backend preservation. Public artifacts
contain only status/test identities and synthetic renders, never issued tokens,
test private keys, SQLite or machine-specific paths.

Continue local fixes within Android ownership if interoperability fails. Backend
or shared contract changes return to the coordinator. Deployed HTTPS and physical
phone acceptance remain separate gates. Publish a stacked draft PR using the
user's continuing commit/push/PR authorization; do not merge.
