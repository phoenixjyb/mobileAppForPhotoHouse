# Android backend repin review — 2026-09-09

Task: `PH-ANDROID-BACKEND-REPIN-01`. Result: **local source review and contract
repin passed; real backend and phone acceptance remain gated**.

## Identity and ownership

- Mobile base: `5dd01eb48f3f9a0f1d4f81e3ae7cd22fe790d628`.
- Isolated mobile branch: `codex/android-backend-repin`.
- Previous backend pin: `1e394f789ff1f7cef6d9930bb541186684f5a9a0`.
- Reviewed and newly pinned backend: `87a60b475b37b1d6873cd977bcb6e7254472da7e`.
- Backend review used a separate detached worktree at that exact commit. The
  supplied readiness checkout was already at `45f497e0bb0dde00ff79664ebc30d05e1a964ab4`;
  its later changes are documentation/evidence, and that checkout was preserved.
- Current session owns this isolated mobile worktree. The user's explicit request
  authorizes the coordinated shared pin/checksum change and its regression tests.
  No delegation or model switch occurred. Existing worktrees and historical
  evidence remain intact.
- Completed plan: review the exact candidate; update the shared pin and checksums;
  replay and record local checks. No backend source changes were required.

## Review result

No blocking source finding in the candidate delta from the previous pin.

The caption change measures the complete JSONResponse-encoded UTF-8 response,
including escaping and the envelope. It retains whole rows in their existing
order, uses `has_more` for row or byte omission, and preserves the separate
8192-code-point meaning of `truncated`. Existing authorization and scoped SQL are
unchanged. Android's `JSON_LIMIT` remains 524288 bytes.

The launcher requires an explicit configuration and an explicit choice of
`--check-config` or `--serve`. Import and configuration checking do not build the
app or start Uvicorn. Serving uses the protected runtime with direct TLS, a private
explicit bind address, one worker, disabled proxy-header trust and disabled access
logging. The default unconfigured application remains closed (session returns 503).
The 211-test suite includes all 10 launcher tests with mocked serving; this does
not validate actual Windows serving, certificates, filesystem ACLs or reparse-point
handling. The documented operator-controlled configuration-directory assumption
still needs deployment review.

Documentation caveat: `ANDROID_STAGING_DECISION.json` inside the exact candidate
still names the earlier caption-only commit and lists launcher review as pending.
The later readiness return corrects that history. Neither document is launch
authorization or evidence that a backend is deployed. This mobile return records
the exact source reviewed, without changing the backend's historical documents.

## Coordinated changes

The manifest, OpenAPI source identity, behavioral contract, contract README and
verifier now select the same exact backend commit. All ten source checksums were
verified; only these two changed:

| Backend source | New SHA-256 |
| --- | --- |
| `backend/app/access/library.py` | `5c280e0047771a43274615b77617f896ef2ef075b4fa3f926ae05bf8c49372fe` |
| `tests/security/test_library_reads.py` | `44be0da6baa759d06138c5f430464a6ccb0e21dac3c39201660e9ff5a7e7aadd` |

Within the eight-file contract pack, only `CONTRACT.md` and `openapi.json` changed;
their checksums were updated in the same change. Both document the aggregate
caption budget. The fixture responses, client scenarios and all four media files
remain byte-identical. There is no wire-field or operation change, so the fixture
wire version stays `1.0.0-fixture.1`; the manifest identifies the updated source
snapshot. The original `photohouse-mobile-fixture-v1` tag was not moved.

Three added regressions reject mixed old/new identities even after recalculating
pack checksums, a different backend HEAD, and source checksum drift at the exact
pinned HEAD. The verifier has one accepted backend pin; no candidate exception or
fallback was added. Existing actual-backend integration tooling reads the manifest
dynamically and therefore selects the new commit on its next authorized run.

## Validation observed in this review

| Check | Result |
| --- | --- |
| Shared contract verification | PASS: 12 operations, 38 cases, 8 checksummed files |
| Shared verifier regressions | PASS: 8 tests, no failures or skips |
| Exact new backend replay | PASS: all 38 existing synthetic ASGI cases matched unchanged |
| Backend security suite at `87a60b4` | PASS: 211 tests in 25.554 seconds |
| Actual Kotlin adapter boundary checks against `87a60b4` | PASS: 14 cases, known and unknown response lengths |
| Compiled mobile source identity | All 3 source hashes match this repinned worktree |
| Whitespace/diff check | PASS |

The Kotlin check accepts a 524288-byte response and rejects valid JSON with one
extra trailing whitespace byte. The former large Unicode case is now 493328 bytes,
15 complete rows, `has_more=true`. Escaped control text is 492748 bytes with 10
rows. Empty, small and individually truncated captions also passed. It uses the
actual adapter with an in-memory OkHttp interceptor and denied network operations;
it does not exercise TLS. Python contract calls use in-process ASGI and temporary
migrated SQLite/generated media, with socket and external-process guards.

[Structured repin evidence](result.json) contains the new pin, all source/pack
hashes, harness hashes and runtime versions. [Original Kotlin review output](caption-candidate-review.json)
is retained without rewriting its provenance: the backend's pre-repin probe
requires the old consumer manifest, so it ran against the unchanged mobile base
at `5dd01eb4` and the **new backend candidate**. Its three compiled mobile source
hashes were then verified against this repinned worktree. The new strict pin was
separately replayed for all 38 cases. The output's `consumer_repin_completed=false`
describes that pre-repin probe only. Its existing APK hash is incidental; no APK
was built or installed by this review.

## Reproduction

Select `BACKEND_REVIEW` as a clean detached checkout of `87a60b4…`, `BACKEND_PYTHON`
as the existing backend test interpreter, and `MOBILE_BASE` as the unchanged
`5dd01eb4…` mobile checkout. Use the existing Java 17 and Gradle Maven cache; no
dependencies are installed or downloaded. From this mobile worktree:

```sh
python3 -B scripts/verify-contracts.py
python3 -B scripts/test-contracts.py
"$BACKEND_PYTHON" -B scripts/verify-contracts.py --backend-root "$BACKEND_REVIEW"
(cd "$BACKEND_REVIEW" && "$BACKEND_PYTHON" -B -m unittest discover -s tests/security -q)
"$BACKEND_PYTHON" -B "$BACKEND_REVIEW/scripts/android_caption_budget_check.py" \
  --mobile-root "$MOBILE_BASE" --java "$JAVA_HOME/bin/java" \
  --maven-cache "$GRADLE_USER_HOME/caches/modules-2/files-2.1" \
  --evidence-output "$CAPTION_REPORT"
git diff --check
```

The original probe is specifically a pre-repin comparison tool, not a replacement
for the new consumer's strict verifier. Future changes to adapter source require a
fresh adapter check; this hash equivalence must not be assumed across code changes.

## Remaining gates and next step

No push, merge, deployment, service start, real credential use or phone installation
was performed. No new APK, full Android build/lint/JVM suite, loopback TLS suite or
physical-device flow was run; earlier results on the previous backend pin do not
establish those results for this candidate.

The coordinator can now review this local repin. The next operational task needs
separate authorization and explicit host/service identity, an approved HTTPS
origin with a system-trusted certificate chain, and an approved synthetic test
audience. It must verify the actually deployed commit and schema
`b6e3f9a5c721`, isolated database/media provisioning, Windows runtime, access rules,
backup/rollback and denied-request behavior. Backend deployment remains unknown;
origin and audience remain unset. Configured APK creation and phone installation
follow their own authorization and acceptance gate. UI refinement remains deferred.
