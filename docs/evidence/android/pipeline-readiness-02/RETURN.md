# PH-ANDROID-PIPELINE-READINESS-02 return

Local Android readiness slice complete; locked-profile backend replay remains
blocked. No production implementation change was needed after the audit.

## Identity and preservation

Mobile base: `12a3529837aa6dcea2d97da5887af41a82532a69`, clean latest UI checkout,
not the historical repin checkout. New isolated branch:
`codex/android-pipeline-readiness-02`, sibling worktree
`mobileAppForPhotoHouse-android-readiness`. The final response supplies this return's
exact result commit. Other mobile checkouts and backend work were preserved.

Backend implementation handoff: `4d6c4c5d982212b403d04845e1fd8df0c9e61556`;
documentation checkpoint: `75b702f17a3e460a632d580e8ee8ea12dba0d47f`.
The backend task owns subsequent recovery/reopening work; this slice did not edit it.

The API pin stays `87a60b475b37b1d6873cd977bcb6e7254472da7e`.
All ten pinned source hashes match both the exact pinned checkout and the reviewed
backend documentation checkpoint. The unchanged shared verifier rejects the later
backend HEAD. No repin, checksum rewrite, schema addition or origin configuration.
`result.json` records source/document/lock identities; `jvm.json` records exact
compiled source and cached dependency hashes.

## Audit and changes

| Priority | Existing implementation and this slice's evidence |
| --- | --- |
| Phone as login, owner-issued invitation | Admission normalizes an international phone label; registration requires code/password. Five compiled adapter checks include exact native request fields, no SMS/WeChat/OIDC field or ambient credential. This is not proof of phone ownership. |
| Invalid/used/wrong-phone invitation | Added store refusal checks and adapter 401 checks: no authenticated state, library request or automatic mutation retry. These are canned generic denials; only backend acceptance can prove one-use/phone-binding enforcement. |
| Revoked/closed/expired/unavailable membership | Added session-refresh checks using server `available=false`, including an approved-but-unavailable library; cached content clears and prior approval cannot open it. |
| Disabled account/revoked session | Added protected-read denial followed by session denial; credential, gallery/detail/captions/cache disappear, with no retry. Client cannot distinguish those server reasons. |
| Logout/expiry/in-flight cleanup | Added cancellation-resistant late-thumbnail checks for logout, expiry and library change; existing tests cover late gallery/original responses, original/video cleanup, foreground revalidation, failed logout and expiry timer. |
| Missing thumbnail | Existing store check plus new adapter check prove no automatic original request. CPU runtime requires cached previews; the Android client cannot create them. |
| Unset origin | New adapter check rejects empty/non-origin configuration. Existing MainActivity yields a null store for unset origin; UI disables admission. Existing secure-window/unconfigured EN/ZH renders remain prior evidence. |

Four new store tests and five new adapter tests were added. The new direct-compiler
verification lane provides a reproducible no-listener check using cached JARs.
No UI, transport, store, protocol, manifest, permission or app dependency changed.

## Observed verification tiers

- **Offline shared fixture validation:** 12 operations, 38 stored cases and eight
  checksummed files pass. This did not execute 38 backend requests.
- **Compiled JVM tests:** **62 passed** in 0.29 seconds after direct compilation:
  22 fixture core, 32 connected store, three video reader and five adapter
  interceptor checks. Nine tests are new. JDK 17.0.20.1, Kotlin 1.9.24,
  coroutines 1.8.1, serialization 1.6.3, OkHttp 4.12.0 and JUnit 4.13.2.
  Full JAR SHA-256 inventory is in `jvm.json`; log in `jvm.log`.
- **Source guards:** fixture isolation, normal system TLS, no credential/media
  persistence/logging and backup/lifecycle boundaries pass.
- **Actual in-process backend under the new lock:** not run, blockers below.
- **Actual Kotlin-to-backend/TLS tests:** not run; existing integration lane opens
  a listener and is outside this authorization. Interceptor tests do not prove TLS
  certificate validation, deployment or server authorization.
- **APK/build/device:** no Gradle/build/device execution in this slice. Gradle itself
  starts a local connection, so even `--no-daemon` was not used. Prior connected APK
  from `12a3529...` has SHA-256
  `ca38584656b4d864f1d2cccce398e0e0b91c5721a2bd39149676e0cc84943e97`;
  prior nine emulator checks per normal/200% text are recorded in `../ui/RETURN.md`.
  Production sources remain byte-identical; these are prior artifact/device results.

The backend handoff reports 262 security tests and 51-file source package
`ade79983129bd6b338df9b1b4c5d0255dc1463512d04fc6665eb41d90e1fae4c`.
Those results were read, not independently rerun or relabeled as Android evidence.
No Windows execution or deployment is established by them.

## Locked-profile qualification blockers

Runtime lock SHA-256:
`b4e4e92f67dd3740986494ff3c8cc4b10557fd41329efee030dda03e05897a42`.
Test lock SHA-256:
`ded98cb724b9ad408683824bab31c249fbf1a905658dbfc90776bce07ce1c1c5`.
The reviewed profile has 18 runtime / 21 test packages, Starlette 1.3.1.

A fresh isolated mobile-owned environment was attempted with cached CPython 3.12.12
and `uv pip install --offline --require-hashes --only-binary :all:`. The resolver
reported missing cached compatible MarkupSafe 3.0.3 and pydantic-core 2.41.5 wheels;
installation failed. No network fallback, backend venv mutation or extra package
installation occurred. Supply a separately authorized, hash-verified compatible
wheelhouse or an existing exact test-profile environment for a later replay.

Separately, the shared pinned 38-case replay harness imports Pillow to generate
synthetic JPEGs. Pillow is deliberately absent from the CPU lock. The coordinator
must choose a fixture-only replay arrangement (for example, a reviewed harness
change using already checked synthetic media) without adding Pillow to the serving
runtime or weakening the strict source pin. This slice did not modify the shared
verifier, fake Pillow, copy packages from another environment or claim qualification.

## Concrete end-to-end acceptance sequence (not executed)

1. Independently identify the deployed immutable release, source manifest, runtime
   lock, interpreter and schema revision `b6e3f9a5c721`; verify backup/provenance and
   legacy/standalone ingress isolation. A package on disk or DNS record is not a
   running protected service. Backend recovery/selective reopening and reviewed
   cutover/rollback must be ready first; reopening only the reviewed audience is
   essential because a candidate starts with accounts/libraries closed.
2. Approve the exact HTTPS origin, served trusted certificate chain/hostname and
   route from the test client, host/service identity, audience and test library.
   Separately verify certificate reload/renewal behavior. DNS/certificate preparation
   does not establish a served origin, routing or deployment.
3. Prepare a separate synthetic owner/library/assets database with existing cached
   previews. Keep viewer original access off initially. Use a reviewed protected
   owner/new-password workflow and manually issue one phone-bound viewer invitation;
   never put the password, bearer or invitation in a report/URL/log/committed config.
4. After the origin/build authorization, configure and identify the exact APK.
   Fresh registration with that invitation -> explicit sign-out -> phone/password
   sign-in -> own library -> gallery -> detail -> literal caption -> protected
   thumbnail. Check bilingual controls, missing-thumbnail placeholder and original
   denial; no provider identity or original fallback should be inferred.
5. Verify a used/invalid/wrong-phone invitation refuses access, another library's
   assets are denied, and unavailable/closed membership never exposes content.
   Logout must clear local protected state and the server must reject the old
   session. Separately revoke/disable the synthetic viewer and verify protected
   requests/session refresh fail and cached/in-flight UI content cannot return.
   Reconcile server receipt and client state; a local logout alone is not proof
   of server revocation.
6. Phone installation/run and outside-home routing are later, separately approved
   gates: identify phone and exact installed artifact; check foreground/background
   privacy, actual codecs/audio, logout/revocation, network transitions, performance
   and family acceptance. This slice ran neither an emulator nor a phone.

Remaining backend inputs are owner recovery/selective reopening, reviewed service
cutover/rollback, Windows CPU/host/filesystem validation, served HTTPS identity and
routing, approved synthetic audience/library/previews and protected provisioning.
No real media/database/credentials, origin, remote host, listener, deployment,
push or merge was used. Stop after this verified local slice.
