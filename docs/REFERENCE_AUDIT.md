# Read-only reference audit

Historical initial-planning audit. Backend source findings below describe the old
checkpoint; see [current session handoffs](SESSION_CAPSULES.md) and the
[frozen contract](../contracts/README.md) for the implemented local foundation.
Remote repository, toolchain and deployment observations remain point-in-time.

Date: 2026-09-09. Three design subagents plus coordinator; no reference-repository
edits, builds, device access, model loads, database reads or live Windows checks.
Paths below are repository-relative; no private connection configuration is needed.

## Observed source identities

| Repository / checkout | SHA | Interpretation |
| --- | --- | --- |
| trulyFreeMusic, codex/android-settings-card | 377d877fa705f464adc10007193f6965124aa665 | Clean local OpenGroove reference |
| recomo-app-monorepo, local main | 2af2c492ba50fbbb1c047ebe46c4300e835e3acb | Clean but 106 commits behind its cached remote; not current CI proof |
| ReCoMo mobile-ios-foundation worktree | d91abb731f645b5d810de79e6600b2136d344a16 | Clean native iOS foundation reference |
| vlmPhotoHouse local feature branch | 752ab3bfb2ec48558bae747948185ceabb66a229 | Clean; tree matches merged master 9322635 |
| mobileAppForPhotoHouse remote | No commits | Public empty repository when inspected; cloned for this planning pack |

These are source observations, not fresh build/runtime/device acceptance.

## Reuse the patterns, not the whole applications

OpenGroove: .github/workflows/verify.yml, scripts/verify-all.sh, iosApp/project.yml,
shared/build.gradle.kts, docs/feature-parity.md, scripts/run-ios-device.sh,
iosApp/OpenGroove/Models/UiLanguage.swift and Android localization/queue policy.
Its CI separates Android Linux and iOS macOS verification. Its SwiftUI application
uses a small KMP policy framework, so iOS also needs Gradle/JDK integration.
PhotoHouse should reuse independent verification and parity ideas, not that
unneeded runtime coupling. The reference's global cleartext/backup settings for
public radio and old wrapper/toolchain pins are not suitable privacy defaults.

ReCoMo native iOS reference: src/software/recomo-remote-control/app/ios/README.md,
project.yml, Packages/RecomoMobileCore/Package.swift. Native SwiftUI modules use
separate contracts/transport and cross-platform schemas/fixtures. The generated
project is committed, with explicit simulator versus signing/device gates.

ReCoMo Android reference: src/software/recomo-remote-control/app/android/app-user/
build.gradle.kts and construction/.gitlab-ci.yml. Reuse deliberate signing inputs,
artifact identity and parity discipline; do not copy Robot dependencies, private
CI infrastructure, release identifiers, fixed version codes or account secrets.
Check licenses before any implementation copying.

## Backend findings that drive the plan

All paths here are in vlmPhotoHouse, audited source above:

- backend/app/main.py:52 and dependencies.py:55 do not establish inbound identity.
  db.py Asset, Person and Album have no library/account authorization model.
- main.py:524/539 serve original and thumbnail bytes by asset ID;
  routers/people.py:245 serves face crops. Login on a new mobile prefix alone
  would not protect these legacy routes.
- main.py search/detail/location/caption routes and routers/albums.py return
  library metadata and sometimes filesystem paths without membership filtering.
  Search indices/seed objects, counts, album covers and face samples all need scope.
- main.py:1518 globally deduplicates uploads; future multi-library intake must not
  leak or attach another library's matching asset merely because hashes agree.
- main.py:185 skips Alembic at startup; fallback schema handling needs an explicit
  migration/owner-bootstrap boundary for security changes.
- routers/voice.py:983 does call _require_enabled(): this is a feature/provider
  gate, not authentication. _pop_pending_action at :68 allows missing confirmation
  tokens; caller client_id and provider conversation_id do not establish ownership.
- routers/voice_photo.py references obsolete Asset fields, so its integration is
  not established by the existence of voice routes or documentation.
- UI HTML/JS/CSS use explicit routes in routers/ui.py. No StaticFiles media mount
  was found in inspected source; direct media routes still require protection.
  Reverse proxies and file-sharing exposure were not inspected.

One preliminary agent claim that voice_command lacked the feature gate was
rechecked against HEAD and corrected before integration. The actual issue is lack
of inbound identity/authorization, not absence of that feature-enable call.

## Primary guidance consulted

- [Native OAuth](https://www.rfc-editor.org/rfc/rfc8252.html) and
  [OAuth security BCP](https://www.rfc-editor.org/rfc/rfc9700.html).
- [Object-level authorization](https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/)
  and [mobile sensitive storage](https://mas.owasp.org/MASVS/controls/MASVS-STORAGE-1/).
- [Android architecture](https://developer.android.com/topic/architecture/recommendations),
  [Keystore](https://developer.android.com/privacy-and-security/keystore),
  [backup rules](https://developer.android.com/identity/data/autobackup).
- [Apple Keychain accessibility](https://developer.apple.com/documentation/security/ksecattraccessiblewhenunlockedthisdeviceonly),
  [ephemeral networking](https://developer.apple.com/documentation/foundation/urlsessionconfiguration/ephemeral),
  [ATS](https://developer.apple.com/documentation/bundleresources/information-property-list/nsapptransportsecurity).
- [Kotlin iOS integration](https://kotlinlang.org/docs/multiplatform-ios-integration-overview.html)
  and [GitHub Actions security](https://docs.github.com/en/actions/reference/security/secure-use).

Exact toolchain/library versions, identity provider and store distribution choices
must be verified at implementation/release time, not inferred from old projects.
