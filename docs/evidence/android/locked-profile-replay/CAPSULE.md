# PH-ANDROID-LOCKED-PROFILE-REPLAY-01

Follow-up to `ad6b9d9e079383c7e062fe4ba8138117022dc0e3`, continuing the same clean
mobile-owned `codex/android-pipeline-readiness-02` worktree. Current model retained.

Coordinator explicitly authorizes fixture-only changes to scripts/verify-contracts.py,
its regression tests and fixed synthetic JPEG input. Preserve all contracts/v1 bytes,
accepted backend SHA/hash checks, frozen response fields/statuses/lengths and prior
missing-cache evidence. Use supplied exact 21-package test interpreter read-only
with -I -B. No installs, downloads, fake Pillow, --record, backend writes, listeners,
real data/origin/credentials, remote hosts, build/device, push, merge or deployment.

Acceptance: exact historical JPEG generation equality, fixture integrity/refusal
regressions and complete unchanged 38-response comparison against detached
87a60b475b37b1d6873cd977bcb6e7254472da7e in process under the reviewed CPU test lock.
Stop if equality or exact dependency/source identity cannot be established.
