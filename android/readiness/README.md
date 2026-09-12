# No-listener Android readiness checks

Current [MVP status](../MVP_STATUS.md) · [live acceptance gates](../PILOT_ACCEPTANCE.md).
The locked-profile in-process replay blocker is resolved.

This lane compiles the protocol, fixture core and connected core directly from
source with the existing Kotlin 1.9.24 compiler, then runs selected JUnit suites.
It does not run Gradle, MockWebServer, an Android device or a backend listener.
The five adapter tests use a terminal OkHttp application interceptor: real request
serialization/response parsing, synthetic responses, no TLS/socket/backend evidence.
A test-only JDK 17 SecurityManager denies connect/listen/accept in compiler and test
processes. Its deprecation warning is expected; this lane deliberately requires
JDK 17 and the guard is never included in the app.

Use existing local installations/cache, from the mobile repository root:

```sh
python3 android/readiness/verify-jvm.py \
  --java-home "$JAVA_HOME" \
  --cache "$HOME/.gradle/caches/modules-2/files-2.1" \
  --evidence-dir "$PHOTOHOUSE_NEW_EVIDENCE_DIR"
```

Set PHOTOHOUSE_NEW_EVIDENCE_DIR to a new path below docs/evidence/android; preserve
existing returns/logs. Replay only when source/dependency changes warrant it.

No artifact is downloaded. A missing/ambiguous cached artifact fails the check.
The receipt records every compiled source hash and each cached JAR's exact version
and SHA-256. Temporary compiler outputs are discarded. The ordinary Gradle suite
still includes these tests when that separate lane is authorized.

The prior missing-cache attempt is retained as historical evidence. The exact
21-package environment and fixed JPEG follow-up passed all 38 in-process cases.
Do not run the ordinary HTTPS tests or
`verify-backend.py` as part of a no-listener authorization.

The coordinator-authorized fixed JPEG follow-up closes the CPU locked-profile
in-process replay blocker. See [locked-profile return](../../docs/evidence/android/locked-profile-replay/RETURN.md)
for the exact 38-case replay and dependency identities; TLS/device gates remain.
