# Shared mobile contract

**Frozen for fixture app development:** `1.0.0-fixture.1`, consuming backend commit
`1e394f789ff1f7cef6d9930bb541186684f5a9a0` on `codex/mobile-access-foundation`.
This is a native browsing subset of locally implemented behavior, not a deployed
service, complete backend API export or promise of production compatibility.

Start with [the behavioral contract](v1/CONTRACT.md), [OpenAPI snapshot](v1/openapi.json),
[synthetic responses](v1/fixtures.json), [client scenarios](v1/client-scenarios.json)
and [checksums/source identity](v1/manifest.json). Run:

```sh
python3 scripts/verify-contracts.py
```

The default verifier uses only Python's standard library. It checks the exact file
set/checksums, source identity, operation references and response fixtures against
the JSON Schema subset used by this snapshot. It is not a general OpenAPI validator.

The coordinator additionally replays the cases with the backend root `.venv` Python:

```sh
python scripts/verify-contracts.py --backend-root /absolute/path/to/pinned/backend-worktree
```

That optional check requires the backend's existing test dependencies. It verifies
HEAD and source checksums, imports only the synthetic security fixture harness,
creates temporary migrated SQLite/generated media, and makes in-process ASGI calls.
Network sockets, external processes, models, live settings and real data are not
used. `--record` is coordinator-only fixture maintenance; platform sessions must not
regenerate fixtures or change shared contracts to make their tests pass.

The full backend route inventory remains authoritative for all other surfaces.
Native clients implement the twelve frozen operations only; initial fixture apps
perform **zero real HTTP requests**. Missing search, albums, voice, upload, refresh,
password-reset and account-deletion operations must not be invented or routed to
legacy endpoints. A missing optional feature does not block the fixture browsing shell.
