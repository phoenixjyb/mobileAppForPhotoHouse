# Phone Family default

Base: `ca74e96562c59b735d86a3cce6efd4f71f8769f7`.
Branch: `codex/android-family-default-v31`.

The protected phone client automatically opens the accessible library with ID
`family` after login, invited registration and cold credential restoration.
Family is first in the library picker. When Family is not accessible, selection
falls back to the first available membership; none means no media request.
Only the stable ID is preferred, never a translated display label.

An explicit choice survives foreground session revalidation, including an offline
failure followed by Retry. The selection hint stays in memory outside the private
UI state and clears on logout/expiry. Revoked choices fall back only after fresh
session authorization. A gallery authorization failure does not automatically
reopen the gallery or loop. Remember-session storage warnings remain visible.
The initial gallery load is now automatic, so existing read-count tests no longer
simulate a redundant initial Open library tap.

Validation on 22 September 2026:

- Frozen contracts verified: 12 operations, 38 ASGI cases, 8 checksummed files.
- `:live-core:test`: 234 tests, zero failures/errors/skips. Includes Family after
  another alphabetic ID, inaccessible Family, no memberships, cold restoration,
  manual selection/revocation, offline retry, bounded 401 and privacy regressions.
- `:connected:assembleDebug :connected:lintDebug`: passed using installed JDK17,
  SDK34 and cached Gradle8.10.2 in offline mode.
- APK signature verification passed. Version code23, name`0.24-family-default`.
- All13 PHOTOHOUSE build configuration fields (feature flags and origins) match
  the prior registration APK. Private values remain outside source/evidence.

Configured debug APK SHA256:
`046bc268fd355e83de06cc434f328b3b2257e96fe817c43341c0499e1ae2910a`.

No backend contract change, service deployment/restart, real-device installation,
push or merge. No phone/emulator UI acceptance is claimed. Anonymous Home/TV
selection behavior is unchanged. Web parity is separately implemented and tested
in backend commit`75796ceda7fa65ddc78df14980528181fab92cbe`.
