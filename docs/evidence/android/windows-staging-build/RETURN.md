# PH-ANDROID-WINDOWS-STAGING-BUILD-01 — complete locally

A private configured connected debug APK was built from unchanged app source
`90e296922e3c84b35a93abda7d6fc1d2c53808fb` on `codex/android-pipeline-readiness-02`.
This return and its [machine-readable receipt](result.json) are the only public
outputs alongside the [capsule](CAPSULE.md). The configured APK, full build/lint
logs and private receipt remain outside the repository with owner-only access.
The ignored local origin and generated build outputs are private, too.

## Exact artifact

- APK SHA-256: `22c20ea2f5de6aec7edef61429256a57083798e7c8d182a42790eafaa70bc2c3`; 8,833,542 bytes.
- Debug signer certificate SHA-256: `56d7591b2b6c2538d506d1fe51327444f2307736cb12f5beefa08f1c410d6d28`.
- `apksigner verify --verbose --print-certs`: verified, one signer, v2 signature.
- Package `dev.photohouse.connected`, version `0.2-connected-dev` / code `1`.
- Minimum API 26, target/compile API 34. This is a private debug artifact.
- The selected private origin matches both generated BuildConfig and packaged DEX.
  No password, invitation or bearer token was supplied to the build.

## Fresh verification

1. `python3 scripts/verify-contracts.py`: pass, 12 operations, 38 stored ASGI cases,
   eight checksummed files. This validates stored fixtures, not a fresh ASGI replay.
2. All ten backend source hashes in the frozen manifest match both pinned commit
   `87a60b475b37b1d6873cd977bcb6e7254472da7e` and deployed candidate
   `f15e50753a09b46c8b8748ec87a26f15853bcf0c`. No repin/schema change was needed.
3. `python3 android/verify-connected-boundaries.py`: pass.
4. From `android/`, with existing JDK 17 and SDK, run
   `./gradlew --offline --no-daemon --console=plain :connected:lintDebug :connected:assembleDebug`:
   **BUILD SUCCESSFUL in 28 seconds, 54 tasks executed**.
   Lint: zero errors, two existing warnings (`ExifInterface`, `OldTargetApi`).
5. Packaged manifest/network config checks pass: backup and cleartext disabled,
   system trust only, Internet plus AndroidX internal receiver permission only.
   No fixture assets are packaged. Private APK copy matches the build output hash.

Toolchain: JDK 17.0.20.1, Gradle 8.10.2, AGP 8.5.2, Kotlin 1.9.24,
Android build tools 34.0.0. Existing cached wrapper/dependencies were used offline;
no SDK/dependency installation or backend request was performed.

Production code, shared contracts, verifier and prior evidence are unchanged.
This documentation commit follows the recorded build source; it does not produce
a second APK or change the artifact identity above. No new JVM suite, backend
ASGI/HTTPS replay, emulator run or physical-device action was performed. Prior
62 JVM checks and 38 in-process replay results remain historical evidence in the
[readiness](../pipeline-readiness-02/RETURN.md) and
[locked replay](../locked-profile-replay/RETURN.md) returns.

## Backend report and next gate

The backend owner's Windows staging return reports a separately served synthetic
release, 26 initial HTTPS checks, seven restart checks and a second home-LAN
normal-trust UI check using explicit address mapping. Those are backend-reported
observations, not new Android or normal phone DNS/NAT evidence. Synthetic original
access remains off under server policy; no client override was added.

Next: select the phone/OS, acceptance operator and test window; verify the normal
phone hostname/network route; privately supply the pilot inputs, renewing the
invitation through the authorized owner if expired. Then obtain the device/run
approval and execute the [pilot matrix](../../../../android/PILOT_ACCEPTANCE.md)
on this exact installed APK, with redacted server receipts and separate phone
observations. No installation is performed by this build handoff.

Managed backend startup and observed certificate renewal/reload automation remain
operational gates. Outside-home access, real-photo use, original/video grants,
publication, merge and real-library cutover remain separate work. No Windows,
certificate, credentials, routing, database, caption or media state was changed.
