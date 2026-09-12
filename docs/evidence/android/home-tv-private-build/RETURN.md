# PH-ANDROID-HOME-TV-PRIVATE-BUILD-01

A private TV APK now embeds the origin supplied by the backend owner after live
LAN HTTPS diagnostics. Network/projector acceptance remains pending. The service
was stopped by its owner after diagnostics; Android has not restarted it or
installed this APK on a physical device.

## Identity and verification

- Runtime source: `e8ab9be3c5497778783edf1b4a009707922248d1`.
- Build checkout/evidence base: `c866c74de881a24714282bbb8c426f7c29d03103`.
- Backend runtime pin: `e6b2827842b2c0b5223c85208299b60e8a1257f6`.
- Contract SHA-256: `70328a653ddaa559bad6a4d654cf9870c89c5217e8e9e6c46a3501e9dd9e7548`.
- APK SHA-256: `15680809a11ab00639046bf2cca30953d66fea4b84061bd4437c1edfdbcca795`; 8,834,278 bytes.
- Package `dev.photohouse.tv`, versionCode 2, `0.2-home-feed-dev`.
- Debug signer SHA-256: `56d7591b2b6c2538d506d1fe51327444f2307736cb12f5beefa08f1c410d6d28`.

Only a private Gradle environment input supplied the origin. No tracked source,
TLS policy, backend, phone pin or local.properties change. Existing offline Gradle
`:tv:assembleDebug :tv:lintDebug` passed in 16s; zero lint errors and
three retained warnings (OldTargetApi, VectorRaster, fixed landscape).

Verified the configured origin in generated BuildConfig and the APK DEX; signer,
package/version, absence of main APK assets and phone/protocol classes. Offline
home contract and TV boundary verifiers passed. The earlier 110 JVM, ten ASGI and
eight emulator tests per font scale remain source-bound retained evidence; they
were not rerun for this origin-only build. No configured network test is claimed.

The configured APK, full build/badging/signer logs, origin and provenance receipt
are private under reference `HOME-TV-E8AB9BE-CONFIGURED-NETWORK-PENDING`. Do not
publish the APK or copy its origin to public logs. The original unconfigured APK
was preserved privately with its prior checksum before the build. Current ignored
TV build outputs contain the configured variant; synthetic emulator checks must
use an unconfigured rebuild, as their existing guard requires.

## Delivery status

ADB inspection found no connected device. Projector IP, manual DNS availability
and debugging input remain pending. The hostname's normal home-LAN DNS still
resolves publicly according to the backend return; its forced diagnostic mapping
was not a normal-projector-DNS check. There is no TLS bypass or public forwarding.

Backend alone owns restoring the reviewed client admission/firewall rules and
resuming the stopped independent TV service after DNS/peer choices are verified.
Android then owns the requested JMGO install/launch, trusted connection, remote,
4K source/visible image, sleep/wake and lifecycle acceptance. This APK does not
prove those device outcomes. No real media/accounts, physical installation,
backend/server write, push or merge occurred in this build step.

Machine-readable evidence: [result.json](result.json).
