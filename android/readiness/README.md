# No-listener Android readiness checks

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
  --evidence-dir docs/evidence/android/pipeline-readiness-02
```

No artifact is downloaded. A missing/ambiguous cached artifact fails the check.
The receipt records every compiled source hash and each cached JAR's exact version
and SHA-256. Temporary compiler outputs are discarded. The ordinary Gradle suite
still includes these tests when that separate lane is authorized.

See the slice return for the CPU dependency-profile replay blocker and the separate
future HTTPS/device acceptance gates. Do not run the ordinary HTTPS tests or
`verify-backend.py` as part of a no-listener authorization.
