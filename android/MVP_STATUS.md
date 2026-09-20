# Android family browsing MVP — current status

Current continuation: [Playback recovery v20](../docs/evidence/android/playback-recovery-v20/RETURN.md)
and [gallery/prepared browsing v19](../docs/evidence/android/playback-parity-v19/RETURN.md).
These are local source/build/emulator results; candidate15 server activation and
physical phone/TV acceptance remain separate gates.

Earlier continuation: [Phone v15 evidence](evidence/family-memories-v15/RETURN.md).
Cold-start sign-in and story-editing statements below describe the earlier baseline.
V15 adds optional Keystore persistence (same 24-hour limit), reviewed story writes
and an opt-in server-side media filter; live rollout remains a separate gate.

For the later phone/TV media and opt-in search additions, start with
[phone/TV parity](PHONE_TV_PARITY.md) and the
[protected discovery candidate](phone-discovery-contract/README.md). The original
browsing-MVP audit below remains historical evidence for its stated scope.

**Local implementation is complete for the agreed first browsing MVP. Live pipeline
and family/device acceptance are pending.** Current audit: PH-ANDROID-MVP-COMPLETION-01,
on top of `5773ec27db19012dd5d6de3d2717b2d781e6d664`. No source gap was demonstrated,
so this audit adds no implementation, API or dependency.

Start here for current status, then use [pilot acceptance](PILOT_ACCEPTANCE.md).
[Completion evidence](../docs/evidence/android/mvp-completion/RETURN.md) records the
identity checks. Earlier returns are immutable historical evidence, including the
now-resolved missing-wheel/Pillow blocker. They are not the current task queue.

Scope: owner-issued phone-bound invitation redemption, phone/password login, own
memberships/library selection, paged photo grid, detail/captions/cached thumbnails,
EN/ZH, accessible text/controls, error/privacy handling and logout/revocation.
The owner issues invitations through the separately controlled backend workflow;
the Android viewer does not administer owners or issue invitations. Phone is a login
label, not verified phone possession. No open signup, SMS, WeChat or OIDC is implied.

## Requirement-to-source and evidence map

All rows have implemented source and the listed local evidence. This table does
not promote synthetic UI, canned responses or in-process requests to live acceptance.
Links point to current source; method names identify the concrete checks.

| Requirement | Implementation | Test/evidence and acceptance limit |
| --- | --- | --- |
| Valid invited registration; invalid/used code refusal | [AdmissionForm](connected/src/main/java/dev/photohouse/connected/ConnectedApp.kt), [authenticate and acceptInvitation](live-core/src/main/kotlin/dev/photohouse/connected/core/ConnectedStore.kt), [native request adapter](live-core/src/main/kotlin/dev/photohouse/connected/core/HttpsPhotoHouseApi.kt) | `syntheticInvitedRegistrationRequiresAllInputs`; `refusedInvitationNeverCreatesSessionOrTriggersAutomaticAdmissionRetry`; `invalidAndUsedInvitationResponsesAreGenericDenialWithoutReplay`. The actual 38-case replay proves valid/invalid backend admission; used/wrong-phone client tests simulate generic server denial. Live one-use/phone-binding acceptance remains required. |
| Phone/password sign-in; explicit native fields | [Admission and Bearer](live-core/src/main/kotlin/dev/photohouse/connected/core/PhotoHouseApi.kt), adapter login/register, AdmissionForm | `admissionUsesPhoneLoginAndOwnerInvitationWithoutIdentityProviderFields`, `invitedRegistrationAndColdStartAreIndependentOfFixtureState`; real in-process login case. Password/code fields clear on submit/toggle; session remains memory-only. |
| Own memberships and library selection, unavailable access denied | ConnectedApp library cards; ConnectedStore `selectLibrary`, `foreground` | `signInRechecksOwnSessionAndClosedMembershipNeverReads`, `refreshedRevokedExpiredOrClosedLibraryCannotUsePreviousApproval`; actual approved/requested/rejected/revoked/expired/no-memberships cases. Server `available` controls entry. |
| Paged grid, empty page, refresh and return navigation | ConnectedApp gallery; ConnectedStore `loadPage`, `adjacentPhoto`, `backToPhotos` | `galleryPagesDeduplicateAndBoundMemory`, `detailNavigationStaysOnCurrentPageAndFetchesEachPhoto`, `photoNavigationReturnsToSelectedPageAndSupportsBothLanguages`; actual gallery/empty-page cases. |
| Details and literal captions, unknown source date/language preserved | ConnectedApp detail/caption cards; [wire DTOs](protocol/src/main/kotlin/dev/photohouse/protocol/WireModels.kt) | `syntheticLoginBrowsingAndLiteralCaptionComponents`; actual detail/bilingual/untrusted/missing-caption cases. No HTML rendering or invented translation/timezone. |
| Protected cached preview; no original fallback | Adapter `thumbnail`; store bounded preview map; ConnectedApp `Preview` | `absentThumbnailDoesNotFetchOriginalAndUnavailableMembershipIsPreserved`, `originalRequiresExplicitImagePermissionAndNeverFallsBackFromMissingPreview`; actual thumbnail/HEAD/missing-thumbnail cases. Cached previews must already exist on the server. |
| EN/ZH/system language, readable controls and layout | ConnectedApp `Words`, Settings, wrapping controls, one-column large-text grid; native text inputs/buttons/image labels | Prior nine API 36 emulator component tests passed at 1.0 and 2.0 text scale, including EN/ZH navigation and gallery/settings. TalkBack, landscape and physical-device accessibility acceptance remain open. |
| Busy, retry, offline, unavailable and denied states | ConnectedStore `problem`, `readFailure`, cooldown; ConnectedApp busy/problem/covered branches | `rateLimitSurvivesNavigationAndRequiresExplicitRetry`, `session401SignsOutAnd503KeepsCoverForExplicitRetry`, `object401RechecksOnceWithoutRetryingForeignRead`; actual 401/429/503 cases. Retries are explicit; ambiguous admission/invitation mutations are not automatically replayed. |
| Logout, expiry, revocation and in-flight cleanup | ConnectedStore `invalidate`, `expire`, `logout`, generation-bound reads | `accountDisabledOrSessionRevokedDuringReadDropsCredentialsAndAllContent`, `lateThumbnailCannotRestoreAfterLogoutExpiryOrLibraryChange`, late-gallery/original tests, `logoutFailureIsLocalAndLateAcknowledgementCannotReplaceNewAccount`; real expiry/logout/logged-out cases. Remote revocation is observed on request/revalidation, not pushed instantly. |
| Background privacy, cold start and no persistent media/credentials | [MainActivity](connected/src/main/java/dev/photohouse/connected/MainActivity.kt), store background/foreground, [manifest](connected/src/main/AndroidManifest.xml), backup/network XML | `backgroundClearsPrivateStateAndRevalidatesBeforeUncovering`; source guards; prior secure-window/unconfigured UI check. Injected-store component privacy tests are separate from a configured app's real Activity/service lifecycle. |
| Unset origin fails closed; trusted HTTPS only | BuildConfig origin defaults empty; MainActivity creates no store for invalid origin; TrustedOrigin and [system trust config](connected/src/main/res/xml/network_security_config.xml) | `unsetOrNonOriginConfigurationIsRejectedBeforeAdapterConstruction`, `unconfiguredAppDisablesAdmissionAndKeepsSecureWindow`; prior TLS/redirect/hostname tests. No current deployed origin or certificate acceptance. |
| Original viewing/video additions remain permission-gated | Store `openOriginalPhoto` / `openVideo`; [original viewer](connected/src/main/java/dev/photohouse/connected/OriginalPhotoViewer.kt), [player](connected/src/main/java/dev/photohouse/connected/VideoPlayer.kt), [reader](live-core/src/main/kotlin/dev/photohouse/connected/core/VideoReader.kt) | Prior original/decoder/native-video tests and bounded reader tests. Both require `originals_allowed=true` and explicit action. Initial pilot sets original grants to zero; no original/video requests should occur. |

Test definitions: [store](live-core/src/test/kotlin/dev/photohouse/connected/core/ConnectedStoreTest.kt),
[no-listener adapter](live-core/src/test/kotlin/dev/photohouse/connected/core/ReadinessAdapterTest.kt),
[UI](connected/src/androidTest/java/dev/photohouse/connected/ConnectedUiTest.kt),
[fixture APP-01–10](core/src/test/kotlin/dev/photohouse/fixture/core/FixtureTest.kt).

## Evidence tiers and exact identities

| Tier | Established evidence | What it does not establish |
| --- | --- | --- |
| Directly compiled JVM | [62 passed](../docs/evidence/android/pipeline-readiness-02/RETURN.md) at `ad6b9d9e079383c7e062fe4ba8138117022dc0e3`: 22 fixture, 32 store, 3 reader, 5 adapter-interceptor; JDK 17.0.20.1 / Kotlin 1.9.24 | No network/TLS/server or Android runtime |
| Actual pinned backend in process | [38 exact response comparisons and 12 verifier regressions passed](../docs/evidence/android/locked-profile-replay/RETURN.md) at `5773ec27db19012dd5d6de3d2717b2d781e6d664`; CPython 3.12.12 / 21 packages / Starlette 1.3.1 | No listener, Kotlin-to-backend TLS, new backend deployment package, Windows or phone |
| Prior build and emulator UI | [UI return](../docs/evidence/android/ui/RETURN.md), source `12a3529837aa6dcea2d97da5887af41a82532a69`: debug APKs and lint passed, nine tests per 1.0/2.0 font scale; EN/ZH synthetic renders | No configured-origin sign-in or physical phone acceptance; target-API and EXIF lint warnings remain |
| Earlier actual adapter/local TLS | [Video return](../docs/evidence/android/video/RETURN.md): seven synthetic pinned-backend TLS checks at `0970d981985cca1363dc4e98c2d8a1087f6348eb` | Historical interpreter profile; not replayed under the new CPU lock and not a served pilot |
| Live pipeline | Pending the [ordered acceptance gates](PILOT_ACCEPTANCE.md) | No deployment, real audience or family acceptance is claimed |

The mobile API pin remains `87a60b475b37b1d6873cd977bcb6e7254472da7e`, independent
of the newer backend's recovery/provisioning additions. CPU test lock SHA-256 is
`ded98cb724b9ad408683824bab31c249fbf1a905658dbfc90776bce07ce1c1c5`; runtime lock is
`b4e4e92f67dd3740986494ff3c8cc4b10557fd41329efee030dda03e05897a42`.

Latest built connected debug APK, source `12a3529...`, SHA-256:
`ca38584656b4d864f1d2cccce398e0e0b91c5721a2bd39149676e0cc84943e97`.
Application ID `dev.photohouse.connected`, versionCode 1, versionName
`0.2-connected-dev`, minSdk 26, target/compile 34; origin is empty. Production
Android sources/build inputs have not changed since that UI build. The current
readiness worktree need not contain its build outputs. This is not a configured
pilot artifact or production signing/distribution lane.

## Completion boundary

No implementation/test replay occurred in this documentation audit. Recorded
source/evidence identities and links were checked locally; historical tests were
not presented as reruns. The first browsing MVP is locally ready for integration.
Full PhotoHouse product completion, live safety and user acceptance are not implied.

Search, albums, people filters, voice, upload, persistent sign-in, offline albums,
export/sharing/printing, owner administration and store distribution remain deferred.
They do not block this scoped viewer MVP, and no new API is invented for them.
