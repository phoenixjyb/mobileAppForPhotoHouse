# Phone registration v30

Base: `6c6ab4e11e330f571c1a22e48eb3780978b8c09a`.
App: `dev.photohouse.connected`, version 22 / `0.23-registration`.

## Change

The protected phone form previously required an explicit international prefix and silently disabled submission for an 11-digit Chinese number that the secured WebUI accepted. The form now defaults such numbers to +86 and sends the canonical international number. Explicit international input continues to work. The strict wire validator and legacy profile do not infer a country.

The form explains required inputs and directs members who already registered through the website to Sign in. Transport connection failures are distinguished from HTTP server failures; existing TLS, admission cooldown and no-automatic-registration-retry rules remain. Signed-in media errors retain their contextual retry message.

## Qualification

- All 229 live-core JVM tests passed, including phone normalization, strict legacy/wire behavior, protected registration payloads and distinct offline/server/TLS authentication failures.
- Configured debug APK built with the same server settings, feature flags and signing certificate as the existing v27 artifact. Protected native v2 is explicitly enabled. Runtime endpoints remain in ignored local configuration, outside this public repository.
- API 36 emulator: named registration with bare Chinese phone input passes in English and Chinese, and emitted phone is canonical +86. Rendered screenshots inspected. Legacy invited-registration and signed-in media retry checks pass.
- All 6 targeted emulator journeys passed: 5 in the initial run and the remaining login/story journey after correcting stale gallery-heading and fullscreen-close assertions to match the existing UI. No application change was needed for those test failures.

Configured APK SHA256: `6e457a399bd2d46edf16cf8b3db69efad371a4258aa83cb662aa53dc8ee44ec8`.

The original reported failure on the installed physical phone is not confirmed: the owner could not connect it to ADB. This source repair and synthetic qualification are not real-account registration acceptance. No live invitation was consumed to test; no physical-device installation, SMB publication, push or merge was performed.
