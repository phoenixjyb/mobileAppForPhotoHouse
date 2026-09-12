# Locked-profile replay return

**The actual in-process 38-case backend replay now passes under the exact
21-package CPU test lock, with every frozen response unchanged.** This closes the
local replay blocker reported in the previous readiness slice. It is not a
Kotlin-to-backend TLS, deployed-service, APK or device result.

Mobile base: `ad6b9d9e079383c7e062fe4ba8138117022dc0e3`.
Branch/worktree: `codex/android-pipeline-readiness-02` /
`mobileAppForPhotoHouse-android-readiness`. The final task response identifies the
result commit. The coordinator explicitly authorized this fixture-only shared
harness change; no wire/schema repin was requested or performed.

## Change and fixture identity

The harness now reads `scripts/fixtures/replay-8x8.jpg`, validates length and
SHA-256, and writes the same bytes into its existing temporary original/thumbnail
paths. It no longer imports Pillow. External-I/O guards now cover backend imports
as well as the actual replay, after the unchanged Git/source identity checks.
The response capture, normalizer, exact fixture comparison and case expectations
are unchanged.

The JPEG is 632 bytes, SHA-256:
`c311363ddcc33e304b4f657d7fb3353c839bcdbdb9bdbd09bea62b1152fac42d`.
It was generated once using the already installed historical synthetic Pillow
12.1.1 environment, JPEG codec reported as 6.2. Input is precisely 8x8 RGB
`(160,180,140)`, with default JPEG save settings. Both historical file-save
operations (original and thumbnail) were reproduced and compared byte-for-byte
to the committed fixture. Its JPEG validity/dimensions were also checked once.
See `scripts/fixtures/README.md` for provenance. Pillow is not used for replay and
was not added to either dependency lock.

Frozen binary expectations remain thumbnail/original 632 bytes, both HEAD bodies
zero bytes, Range body six bytes. Every contracts/v1 file, including the manifest,
is byte-identical to the parent. The fixed input is outside that frozen file set.

## Observed validation

- Environment prerequisite probe passed before and after replay: CPython 3.12.12,
  21 test packages, Starlette 1.3.1, SQLite 3.50.4, OpenSSL 3.5.4, Mac ARM64.
  Supplied interpreter/packages were used read-only with `-I -B`; no installation.
- Test lock SHA-256:
  `ded98cb724b9ad408683824bab31c249fbf1a905658dbfc90776bce07ce1c1c5`.
  Runtime lock SHA-256:
  `b4e4e92f67dd3740986494ff3c8cc4b10557fd41329efee030dda03e05897a42`.
- Detached backend HEAD `87a60b475b37b1d6873cd977bcb6e7254472da7e` and all ten
  required source hashes passed the strict existing guard.
- **38 actual in-process ASGI cases passed** the exact comparison with frozen
  fixtures. This includes admission, scoped reads, thumbnails/original/Range,
  unavailable memberships, expiration, logout, rate limiting and service-unavailable
  behavior. Temporary synthetic SQLite/media only; no served listener or live data.
- **12 focused verifier regressions passed**, including four new checks for fixture
  identity/lengths, missing/truncated/same-length corruption, refusal before backend
  import, and external-I/O guards after identity checks. Existing mixed-pin and
  source-drift refusal checks continue to pass.
- Offline validation still passes 12 operations / 38 stored cases / eight frozen
  checksummed files. No Kotlin source changed, so the preceding 62-test JVM evidence
  remains prior evidence and was not rerun here. No APK or device test was run.

The existing Starlette HTTPX TestClient deprecation warning remains test-only.
`replay.log`, `regressions.log`, `environment.json` and `result.json` retain the
observed evidence. The earlier missing-wheel installation attempt and blocker are
preserved unchanged in `../pipeline-readiness-02/`; the supplied environment and
fixed fixture resolve replay without rewriting that history.

## Reproduction

Set PHOTOHOUSE_LOCKED_PYTHON to the existing exact test-profile interpreter and
PHOTOHOUSE_PINNED_BACKEND to the detached 87a60b4 checkout. Do not select the moving
backend readiness worktree for this replay:

```sh
"$PHOTOHOUSE_LOCKED_PYTHON" -I -B scripts/test-contracts.py
"$PHOTOHOUSE_LOCKED_PYTHON" -I -B scripts/verify-contracts.py \
  --backend-root "$PHOTOHOUSE_PINNED_BACKEND"
```

The environment probe is a prerequisite inventory/runtime check, not a deployment
or vulnerability attestation. No installed wheel-byte rehash or Windows execution
is claimed. Fixture regeneration is unnecessary and must not be used to reconcile
unexpected response drift.

## Latest backend handoff and remaining gates

Read-only backend checkpoint `5103abdd0861c2dbb7553d6edeccddbb170deed7` now implements
first-owner recovery and selective reopening. It opens only one reviewed bootstrap
owner/library with a fresh protected password, revokes other restored memberships
and all original grants, rotates the plan key, and retains revoked old sessions
and invitations. Normal invitation supports a new phone account; an existing
disabled viewer or another owner/library needs a separate recovery path. Android
must not treat a new invitation as recovery for those disabled accounts.

The coordinator reports 277 backend security tests and a 53-file package with
SHA-256 `bcb7767f9493084c0a1b9c6bf0f9401f2d9c20b4998291a99be25ba0d287ebe1`, including
seven ASGI checks, nine operator commands and eight preparation invocations.
Those broader results were not rerun here. All ten Android-pinned source hashes
were independently compared with that checkpoint and still match. This does not
repin the mobile app to it or qualify the whole newer deployment package.

Service cutover/rollback, Windows execution and filesystem/host verification,
trusted served HTTPS and ingress, approved synthetic audience/library and physical
phone acceptance remain unverified. Use the end-to-end acceptance sequence in the
prior return, updated for the now-implemented first-owner recovery workflow.
DNS/certificate preparation alone still proves no served origin or routing.
No backend/environment writes, real origin/credentials/media/database, remote-host
access, listener, push, merge, deployment or phone action occurred in this follow-up.
