# PH-ANDROID-UI-01

Owner: current Android task; no delegation or model change.
Base: `0970d981985cca1363dc4e98c2d8a1087f6348eb`.
Branch: `codex/android-ui-refinement`, isolated sibling checkout ending `-android-ui`.

Outcome: refine the connected Android app's gallery, detail, admission and library
screens with a warm palette, photo-led hierarchy, readable controls and EN/ZH
layouts at normal and 200% text. Preserve existing native media and access behavior.

Write scope: Android UI, Android tests/scripts, and `docs/evidence/android/ui/**`.
Do not change the shared contract, live-core, fixture app, backend, iOS or root docs.
No new dependencies, backend access, real data/credentials, physical installation,
push, merge, deployment or release signing. All screenshots use generated media.

Acceptance: contract verification, focused UI regression coverage, offline debug
build/lint and emulator EN/ZH renders at both text sizes. Inspect the renders and
record artifact identity, checks, skipped gates and exact commit in the return.
Contract preflight passed: 12 operations, 38 ASGI cases, 8 checksummed files.
