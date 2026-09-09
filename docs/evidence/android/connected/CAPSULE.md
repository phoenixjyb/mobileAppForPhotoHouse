# PH-ANDROID-CONNECTED-01

Authorized by the user after physical fixture review on 2026-09-09: proceed with
real implementation; defer visual refinement. Separate local follow-up branch
`codex/android-authenticated-browsing`, based on `2d0382d` (fixture + device review).
The fixture publication remains separately reviewable.

Outcome: real HTTPS authentication and scoped read-only browsing implementation,
with a separate development app and deterministic local TLS transport/session tests.
Shared wire contract remains `1.0.0-fixture.1`; no backend/schema changes.

Ownership: Android code/tooling, assigned Android workflow/verifier and Android
evidence only. No iOS or coordinator documents/parity edits. Current session executes
this bounded slice; no delegation/model switch is needed.

The network app uses a build-configured HTTPS origin, standard certificate/hostname
validation, no redirects/cookies/refresh/persistent photos or credentials, and
memory-only generation-isolated sessions. A blank origin disables sign-in. Test
trust roots exist only in JVM tests. The fixture app retains no Internet permission
and its bundled synthetic data never enters the connected adapter.

Acceptance: unchanged contract verification; fixture regressions; real socket/TLS
request, redirect, certificate, 401/403/429/503, cancellation and byte-limit tests;
session/privacy tests; lint and debug builds for both apps. Existing-emulator checks
use only an unconfigured app. Any configured staging or physical-phone integration
requires identifying a reviewed HTTPS backend and explicit installation/access scope.
No service deployment, real credentials/data, protected-branch push or merge is part
of this local implementation slice. Return concrete live-service blockers separately.
