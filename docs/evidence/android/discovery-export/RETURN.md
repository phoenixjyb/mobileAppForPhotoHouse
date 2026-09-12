# Synthetic exporter to Android compatibility return

Android base: `63feba99db6adf6ff328987c91428345a745cad3`, branch
`codex/android-tv-foundation`. Backend implementation:
**`d8f20a073e42f93ad2b046c4ae01d0ccd68a05bf`**; backend evidence-only commit:
`3080c9cae90efab373fc0c0a218211c615020e92`, branch `codex/backend-home-tv-feed`.
The existing backend task was continued as sole backend writer under the reviewed
`PH-BACKEND-DISCOVERY-EXPORT-01` plan. Current task/model settings were retained.

## Outcome

The backend now provides an offline two-phase metadata review/export workflow.
It binds the exact publication selection, standalone snapshot, caption/tag policy,
roster, separate person-assignment evidence, reviewed regions and prepared bytes.
An explicit separate approval must match an unchanged review before a **new
disabled** bundle is created. The original candidate and database remain unchanged.
It does not create a live snapshot, generate tags, convert media or enable a service.

The Android owner read the implementation and tests, identified and returned a
review-output containment defect, and verified its correction. Both review and
publish reject output inside the original candidate; a focused test verifies its
unchanged tree. The fixture approval helper was also restricted to the CLI entry
point immediately after fresh synthetic fixture creation. Production review/publish
do not generate approval. No other blocker was found in this targeted source and
contract review; it is not an exhaustive security audit or native operational test.

Android adds a test-only bridge, not an app behavior change:

- `integration/discovery-export-pin.json` pins the two exporter/fixture sources
  separately from the frozen discovery serving implementation.
- `integration/verify-discovery-export.py` extracts 21 verified Git blobs: the
  existing 19 serving inputs plus those two files. No local settings or real DB
  are copied from the backend checkout.
- `integration/probe-discovery-export.py` creates fresh synthetic SQLite/candidate
  input, explicitly reviews/approves only that fixture, and exports a disabled
  bundle. It verifies initial denial, enables only the temporary fixture, captures
  six actual ASGI responses, restores disable and checks all output hashes.
- `DiscoveryExportCompatibilityTest` parses those generated responses through the
  real Kotlin wire parser. It verifies ordered pins, reviewed people coverage,
  excluded blocked tags, missing metadata, combined results, unprepared photos,
  ready/unprepared videos and caption mentions that are not reviewed identity.

The production discovery runtime pin remains
`54f68427058c45f6bcc5a863cc6d708f5b325e45`. V1, v2 and protected-phone contracts
are unchanged. The generated fixture is under `home-core/src/test/resources` only.

## Evidence

| Check | Observed result |
| --- | --- |
| Independently extracted backend implementation | 17 Git blobs verified against backend evidence manifest |
| Independent exporter focused tests | **23 passed**, no skips; [log](backend-focused-tests.log) |
| Independent export/ASGI replay | **13 passed**, exact backend receipt/output hashes reproduced; [receipt](backend-replay.json) |
| Exporter to Android fixture replay | **21 source hashes**, six responses, fixture bytes reproduced exactly; [receipt](android-fixture-replay.json) |
| Android home-core JVM suite | **82 passed**, zero failures/errors/skips, including two new producer/client compatibility tests; [results](jvm-tests.json), [log](android-home-tests.log) |
| Existing four contract/boundary verifiers | Passed; serving pins unchanged and no application fixture assets |
| Backend-owner regression evidence | 101 home tests and 19 inventory tests passed; 162 inventory entries unchanged; these broader suites were not repeated by Android owner |

Generated fixture SHA-256:
`4e7e9a2aa54ece03323625fc0f2652c90977790450857d333c8536cdc1927e58`.
Both replays guard socket bind/connect and process launch and use fresh synthetic
inputs. The exporter unit tests verify SQLite only connects to memory and does
not change input DB bytes/schema or create sidecars. The retained TestClient
Starlette/httpx deprecation warning does not affect passing checks.

Reproduce from the Android repository root with an existing isolated access-test
Python and backend checkout containing the pinned commits:

```sh
python3 android/home-core/integration/verify-discovery-export.py \
  --backend BACKEND_CHECKOUT --python ACCESS_PYTHON
cd android
./gradlew --offline :home-core:test
```

Ordinary replay verifies rather than rewrites the fixture. Explicit
`--update-fixture` is for a reviewed test-input pin update only. The backend's own
17-input extraction manifest and 13-check replay are in its evidence commit under
`docs/security/evidence/home-discovery-export/`.

## Boundaries and next step

Synthetic coverage is four rows, with reviewed people on two and date/place/tag/
caption metadata on three. One photo has prepared grid/display variants and one
video has verified MP4/chunks. These are fixture counts; real coverage remains
unmeasured and the seven real family IDs remain unresolved.

The exporter has explicit initial limits: 64 MiB standalone snapshot, 100,000 rows
per table/300,000 total, 64 MiB per prepared file and 128 MiB prepared bytes total.
It fails on excess and does not silently truncate or expand budgets. A coherent
Windows snapshot, native resource/I/O behavior, full-library sizing, reviewed
publication and assignments, media preparation/codec quality, compatible origin
and projector acceptance still require their separately scoped next steps.

No Windows/real-data access, private ID resolution, model work, deployment, port,
credential change, configured APK rebuild, physical installation, push or merge
occurred. No emulator or UI suites were rerun for this test-only integration.
Existing v6 APKs remain tied to `0e9ba816dc1cb0324744d7ad03351c1f1e39d311` and
do not include the later provenance-label correction. The working v1 service,
DNS/configuration and rollback APK are preserved.
