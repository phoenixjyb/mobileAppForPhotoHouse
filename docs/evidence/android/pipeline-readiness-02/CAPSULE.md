# PH-ANDROID-PIPELINE-READINESS-02

Current Android task owns isolated branch `codex/android-pipeline-readiness-02`,
sibling checkout `mobileAppForPhotoHouse-android-readiness`.
Base: `12a3529837aa6dcea2d97da5887af41a82532a69` (latest UI branch, clean at inspection).
Current model/settings retained; no delegation.

Audit existing behavior, close missing synthetic readiness checks, qualify the
pinned backend and compiled adapter under separate, explicit evidence tiers.
Write only Android tests/readiness tooling and this evidence directory. Preserve
all other worktrees. Backend documentation observed at
`75b702f17a3e460a632d580e8ee8ea12dba0d47f`, implementation
`4d6c4c5d982212b403d04845e1fd8df0c9e61556`; ten API source hashes remain at the
unchanged strict pin `87a60b475b37b1d6873cd977bcb6e7254472da7e`.

Only cached dependencies, synthetic data, local edits/tests/commits. No listener,
Gradle daemon, backend HTTPS server, emulator/phone, remote host, deployment, real
origin/credentials/media/database, push or merge. Stop for contract drift or missing
tooling; do not weaken the verifier or modify backend checkouts. Use direct cached
Kotlin compiler/JUnit and in-process ASGI for this no-listener scope.
