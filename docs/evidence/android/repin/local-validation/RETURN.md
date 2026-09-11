# Android validation after backend repin — 2026-09-11

The committed repin passed the remaining local Android build and interoperability
checks. No production source changes were needed in this continuation.

Mobile source: `1d4fc49c043d553df510c52b9a368ee0313398c6`, branch
`codex/android-backend-repin`. Backend source:
`87a60b475b37b1d6873cd977bcb6e7254472da7e`. The new evidence commit follows that
mobile source commit; its exact identity is returned to the user.

## Observed results

| Check | Result |
| --- | --- |
| Fixture JVM tests | 22 passed |
| Connected store JVM tests | 14 passed |
| HTTPS adapter JVM tests | 13 passed |
| Actual-backend loopback TLS tests | 6 passed, 0 failures/errors/skips, 45.001 seconds |
| Both debug APKs | Built successfully |
| Both lint checks | 0 errors; one existing `OldTargetApi` warning per app |
| Fixture APK content | New contract pack and media match source byte for byte |
| APK permissions | Fixture has no Internet permission; connected has Internet and no fixture assets |
| Connected configuration | Generated `PHOTOHOUSE_ORIGIN` is empty |
| Contract verifier | 12 operations, 38 cases, 8 checksummed files passed |

The six actual-backend tests use the real Kotlin adapter/store and protected Python
application with temporary loopback TLS, migrated SQLite and synthetic JPEGs. They
cover login/session/scoped browsing/logout, invitation registration and another
library, uniform object denials, derivative/storage failures, membership/session
revocation, and admission cooldown. The runner verified the exact backend commit
and ten source hashes from an immutable Git export, then removed the export.
Test-created databases, media and TLS material are temporary. These are local JVM
results, not deployed HTTPS or phone acceptance.

The original 38-case ASGI replay, 211 backend tests, 14 caption-boundary checks and
8 verifier regressions remain dated evidence in [the repin return](../RETURN.md).
They were not repeated here. The present run adds APK packaging, ordinary JVM
regressions and actual-backend TLS evidence for the new pin.

## Artifacts

| Debug APK | SHA-256 |
| --- | --- |
| `android/app/build/outputs/apk/debug/app-debug.apk` | `d16eb8f689b8700dc1c26fde09b9e8c0e9c6ea9d57572b650fb689a1a48ad88d` |
| `android/connected/build/outputs/apk/debug/connected-debug.apk` | `259a42051066ae862fe96a311a0fd128607b5cde185d5442d7385d35d3056ab9` |

APKs remain local ignored build outputs. The connected APK is byte-identical to
the earlier unconfigured artifact because production Android code/configuration
did not change. The fixture APK now bundles the updated backend pin and contract
documentation. Neither was installed by this task or approved for live login.

[Android build evidence](android-build.json) records source identity, artifact sizes
and hashes, suite counts and lint warnings. The sibling ordinary JUnit XML files
are sanitized of hostname, properties and standard output/error. The separate
[backend result](backend-integration-result.json) and
[backend JUnit report](backend-integration-tests.xml) identify the new pin.
Earlier `docs/evidence/android/integration` results remain unchanged and continue
to describe the previous backend pin. This directory supplements the historical
September 9 repin return; it does not rewrite that turn's checks-not-run record.

## Reproduction

Use the existing JDK 17, SDK/build-tools 34, cached Gradle 8.10.2 and backend test
Python. Select `PHOTOHOUSE_BACKEND_REPO` as a local repository containing the exact
new pin, and `PHOTOHOUSE_BACKEND_PYTHON` as the existing security-test interpreter.
From this mobile worktree, with `JAVA_HOME` and `ANDROID_HOME` set:

```sh
bash scripts/verify-android.sh --offline --no-daemon
python3 -B android/integration/verify-backend.py \
  --backend-repo "$PHOTOHOUSE_BACKEND_REPO" \
  --python "$PHOTOHOUSE_BACKEND_PYTHON"
```

The build reused the existing wrapper distribution and cached dependencies; no SDK
or system tool installation occurred. The opt-in integration runner writes its
default evidence directory; this continuation archived its fresh results here and
retained the original committed evidence in that default directory. Future runs
should preserve source identities in the same way.

## Next step: controlled staging and phone acceptance

The local Android checks are complete. The next useful milestone is a protected
synthetic staging deployment, followed by an explicitly scoped phone test. UI
polish and new search/album/voice contracts remain separate later work.

The receiving backend/coordinator owner should use the reviewed staging launcher
and return the following through a private channel before any configured APK or
live credentials are used:

1. Explicit deployment authority, exact host/service and deployed commit identity.
2. Approved HTTPS origin and system-trusted certificate chain, reachable from the
   intended phone network without a TLS or cleartext bypass.
3. Verified schema `b6e3f9a5c721`, isolated synthetic database/media, reviewed Windows
   runtime and filesystem access, provisioning and backup/rollback evidence.
4. Approved owner/viewer/library test scope and invitation delivery procedure;
   keep passwords, invitations and session tokens out of source and public notes.
5. Separate authority for a configured APK and installation on the selected phone.

Then the Android owner can build with the approved origin in ignored local
configuration, record that new APK hash, install on the authorized device, and
verify browsing, cross-library denial, revocation, background/foreground privacy,
offline behavior and logout. The default build stays unconfigured until then.

No push, merge, deployed service access, Windows changes, real credentials, real
media, emulator run or phone installation occurred here. Hosted CI was not queried
or claimed. Existing worktrees and backend source were preserved.
