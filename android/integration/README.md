# Actual-backend Android interoperability

This opt-in test exercises `HttpsPhotoHouseApi` and `ConnectedStore` against the
actual pinned Python application, with real TLS, Alembic migrations, SQLite,
password verification, invitations, authorization and generated JPEG thumbnails.
It complements the canned-response transport tests and synthetic UI component
tests. It is not a deployed-backend or physical-device acceptance result.

## Run locally

Use the existing JDK 17 / Android toolchain. Set `PHOTOHOUSE_BACKEND_REPO` to the
local backend repository containing the commit pinned by
`contracts/v1/manifest.json`. Its existing root `.venv/bin/python` must have the
backend's security-test dependencies plus Uvicorn and Pillow. No packages, model
weights, emulator images or server configuration are installed by this command.

From the mobile repository root:

```sh
python3 android/integration/verify-backend.py \
  --backend-repo "$PHOTOHOUSE_BACKEND_REPO" \
  --python "$PHOTOHOUSE_BACKEND_REPO/.venv/bin/python"
```

The runner verifies the frozen mobile contract and ten backend source checksums,
then exports the required source from that exact Git object into an ignored,
temporary build directory. Dirty or newer backend checkout files are never used
or overwritten. It runs `:live-core:backendIntegrationTest` and removes the export.
The ordinary `:live-core:test` excludes this opt-in suite and requires no backend.
Invoking the integration task without the runner's explicit inputs fails.

Every test launches an isolated Python process and a random loopback-only TLS port.
Python uses `-I -B`, a minimal environment and an explicitly supplied synthetic
database. The database, original/thumbnail shapes and generated TLS key/certificate
belong to a private temporary directory and are removed after process shutdown.
Only a test client trusts the generated certificate. The connected APK's system
trust policy and unconfigured default are unchanged.

The backend cannot start additional listeners, make outgoing connections or launch
processes during serving. There is no arbitrary SQL/control endpoint: finite
synthetic state changes arrive only over the test parent's stdin pipe. Real backend
routes still perform all HTTP authorization. Dynamically issued test invitations
and sessions are never included in public reports.

## Scenarios

1. Returning login, session, scoped pages/details/captions/JPEG and acknowledged
   logout followed by server rejection of the old bearer.
2. Invalid invitation rejection, owner-issued registration with viewer-only access,
   and a second-library invitation followed by authorized browsing.
3. Uniform denial of foreign, deleted, unmapped and missing asset reads while the
   account session remains valid.
4. Missing derivative placeholder versus unavailable-storage 503.
5. The actual connected store clearing cached content after membership revocation,
   then clearing the session after server-side expiry.
6. Real password-attempt admission limits, Retry-After, and recovery after the
   server's cooldown window.

Sanitized results go to `docs/evidence/android/integration`; transient Gradle logs
stay under ignored `android/build/backend-integration`. Any failure is an actual
test failure, never a skip or fallback to canned responses.

Hosted CI still runs ordinary contract/JVM/lint/build checks. This cross-repository
suite is local-only until the pinned backend commit and an agreed CPU test runtime
are available to CI. It does not silently fetch a moving backend branch.

The next live gate requires an identified protected HTTPS deployment, an explicitly
authorized test account/invitation and separately scoped phone installation/access.
There is no localhost/TLS bypass or staging credential in the APK.
