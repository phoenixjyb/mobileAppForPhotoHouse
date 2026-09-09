# PH-ANDROID-BACKEND-INTEGRATION-01

2026-09-09. Branch `codex/android-backend-integration`, isolated worktree
`../mobileAppForPhotoHouse-android-integration`, based on
`1f42d5d93e273b4a39b09a5f1a121ff4f7fea6a6` (connected browsing PR #2).
The result commit is returned in the PR/handoff. Production Android source and
the frozen shared contract remain unchanged in this slice.

## New evidence

Six opt-in JVM tests passed against the **actual protected Python backend** at
`1e394f789ff1f7cef6d9930bb541186684f5a9a0`. The tested path is Kotlin
`HttpsPhotoHouseApi` → real loopback TLS → Uvicorn → protected ASGI routes →
temporary Alembic-migrated SQLite and generated JPEG derivatives. It uses neither
MockWebServer responses nor the canned fixture repository.

- Real returning login, own session, scoped gallery pages, detail, literal caption,
  JPEG thumbnail and acknowledged logout, followed by denial of the old bearer.
- Invalid invitation denial; real owner-issued, phone-bound registration with
  viewer-only membership; second-library invitation acceptance and scoped reads.
- Foreign, deleted, unmapped and missing assets all yield the same denial class;
  those denials do not imply an invalid account session.
- A removed derivative gives a placeholder; unavailable temporary storage gives 503.
- The actual connected store clears private views/cache after membership revocation
  and signs out after server-side session expiry.
- The real admission limiter returns 429 with Retry-After after repeated password
  attempts; admission succeeds after advancing the synthetic server clock.

**6 tests, 0 failures, 0 errors, 0 skips**, in 67.451 seconds. Details are retained
in `backend-integration-tests.xml` and `result.json`. This establishes local JVM
adapter interoperability, not Android device TLS or deployed-service acceptance.

The mobile manifest's ten backend source checksums and all eight frozen pack file
checksums matched. The runner uses an immutable Git export and removes it afterward.
Each test also verifies removal of its own temporary database/media/certificate
directory after terminating its backend process. The original backend worktree
remained clean at the pinned commit. No backend code was copied into this commit.

## Reproduction and regression checks

Set `JAVA_HOME`, `ANDROID_HOME` and `PHOTOHOUSE_BACKEND_REPO` to existing local
installations. Commands from the mobile repository root:

```sh
python3 android/integration/verify-backend.py \
  --backend-repo "$PHOTOHOUSE_BACKEND_REPO" \
  --python "$PHOTOHOUSE_BACKEND_REPO/.venv/bin/python"
scripts/verify-android.sh
```

The ordinary verifier passes all 49 existing JVM tests, both debug APK builds and
lint (0 errors, one known `OldTargetApi` warning per app). Its source/package checks
confirm the fixture APK has no Internet permission and the connected APK contains
no fixture assets or trust override. See `regression-build.log` and `artifacts.json`.
`git diff --check`, Python source parsing and temporary export cleanup passed.

The opt-in task is excluded from ordinary `:live-core:test`. It requires the
explicit runner inputs and fails if they are absent; it never silently skips or
substitutes mock responses. Existing UI/device evidence remains attached to its
earlier artifacts. UI tests were not repeated because this slice changes no app UI
or production behavior.

Observed test environment: Python 3.14.7, Uvicorn 0.42.0, FastAPI 0.135.2,
Starlette 1.0.0, SQLAlchemy 2.0.52, Alembic 1.19.2, HTTPX 0.28.1 and Pillow 12.1.1.
The existing backend root `.venv` uses host packages; it is not a hermetic Windows
deployment environment. Android toolchain remains JDK 17 / Gradle 8.10.2 / AGP 8.5.2
/ Kotlin 1.9.24 / SDK 34. No dependency/system installation was performed.

## Publication and remaining gates

Parent PR #2's push and pull-request CI checks were observed green at its exact
head. This follow-up's ordinary CI must be checked separately. Actual-backend
integration is local-only until the pinned backend commit and an agreed CPU test
environment are available to CI; the workflow never fetches a moving backend ref.

The protected deployment HTTPS origin remains unspecified. Next: identify that
deployment and its exact reviewed backend version, then prepare a configured APK
and perform separately scoped account/invitation, revocation, media and logout
checks on the phone. No live service, real credentials/photos, Windows deployment,
production signing, physical installation or merge occurred in this slice.
The fixture and connected applications remain development builds. Full feature
completion and UI polish remain outside this integration step.
