# Mobile contract handoff verification

2026-09-09. Backend source: `1e394f789ff1f7cef6d9930bb541186684f5a9a0`.
Mobile parent: `6088a35dde86d24b0cc64faba31c71d867293420`.

| Check | Observed result |
| --- | --- |
| `python3 scripts/verify-contracts.py` | PASS: 12 native operations, 38 synthetic ASGI cases, 8 checksummed contract/fixture/media files |
| `python3 scripts/test-contracts.py` | PASS: 5 regressions for freeze drift, missing invitation, types/IDs and unsupported caption fields |
| Shared verifier with `--backend-root` using the backend root `.venv` Python | PASS: regenerated responses exactly match all 38 recorded cases; pinned backend HEAD/source checksums agree |
| Synthetic data boundary | Temporary migrated SQLite and generated 8x8 JPEG only; sockets/external processes blocked; no live settings/database/media/model use |
| Media fixture rendering | Two 640x480 PNGs rendered from included original SVG shapes in existing Chromium over pipes; visually inspected; no network requests/listeners |
| Documentation | Updated Markdown rendered to HTML and local links checked; no app UI/build acceptance implied |
| Backend preservation | Backend worktree and original checkout untouched; mobile changes only |

Recorded cases cover native login, valid/invalid invitation registration, invited
viewer, existing-account invitation acceptance, approved/two-library/unavailable
memberships, gallery/detail/captions, missing text/preview, plain bilingual and
markup-like caption text, foreign-object denial, original permission denial,
authorized GET/HEAD/Range, session and membership expiry, logout, 429 and 503.
Video is metadata/unavailable-state only; range bytes come from a tiny JPEG.

Five additional facts remain explicit: no Android or iOS implementation/build yet;
no native video/cache/device acceptance; no deployed TLS/proxy proof; no persistent
sessions/store distribution; no standalone Codex sessions were created. The backend's
198-test security result is earlier evidence at the pinned SHA, not a new full-suite
run in this documentation/contract task. The local frozen tag pins this handoff;
no tag, branch or commit was pushed.
