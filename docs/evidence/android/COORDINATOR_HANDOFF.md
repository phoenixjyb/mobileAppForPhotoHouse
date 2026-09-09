# Android → PhotoHouse backend/coordinator handoff

Date: 2026-09-09. Prepared at the user's request for another Codex session.
Purpose: report the Android state and resolve deployment unknowns before real-phone
acceptance. This Markdown is a handoff artifact; it does not create or notify a
session or grant new deployment/merge authority.

## Main point

Android fixture browsing and the first real HTTPS browsing implementation are
complete for their bounded scopes. The Kotlin adapter interoperates with the
actual pinned protected backend in local synthetic TLS tests. **A deployed,
reviewed HTTPS origin and the real-phone test setup are still unknown.**

Next, establish backend deployment readiness. Do not restart the Android fixture
implementation or treat an existing legacy PhotoHouse web UI as proof that the
protected account/library API is deployed.

## Exact implementation state

Repository: `phoenixjyb/mobileAppForPhotoHouse`. All three PRs are open drafts,
stacked in this order; none has been merged.

| PR | Branch | Implementation commit | Base branch |
| --- | --- | --- | --- |
| [#1: fixture](https://github.com/phoenixjyb/mobileAppForPhotoHouse/pull/1) | `codex/android-foundation-auth` | `2d0382d22ad3ef1e15a55931765ad43e0cbd3ec7` | `codex/mobile-foundation-plan` |
| [#2: HTTPS browsing](https://github.com/phoenixjyb/mobileAppForPhotoHouse/pull/2) | `codex/android-authenticated-browsing` | `1f42d5d93e273b4a39b09a5f1a121ff4f7fea6a6` | `codex/android-foundation-auth` |
| [#3: actual-backend tests](https://github.com/phoenixjyb/mobileAppForPhotoHouse/pull/3) | `codex/android-backend-integration` | `2bfb218a71e807e3531ebdcca79525574966aa5c` | `codex/android-authenticated-browsing` |

This note may follow those commits as documentation only. Preserve implementation
identities separately from any later handoff commit or moving PR head.

The frozen tag `photohouse-mobile-fixture-v1` resolves to
`5db14f38d3ff7872420f4c5ed16ff54b2cf9b4ac`. Contract: `1.0.0-fixture.1`.
Pinned backend: `phoenixjyb/vlmPhotoHouse`, `codex/mobile-access-foundation`,
`1e394f789ff1f7cef6d9930bb541186684f5a9a0`.

Local worktrees are siblings of the mobile repository:

- `../mobileAppForPhotoHouse-android`: fixture branch.
- `../mobileAppForPhotoHouse-android-live`: HTTPS browsing branch.
- `../mobileAppForPhotoHouse-android-integration`: integration branch and this note.
- `../vlm-photo-engine/_worktrees/mobile-access-foundation`: pinned backend source.

These worktrees already exist and have owners. Inspect status before use; do not
reset, clean, stash, switch their branches or overwrite another session's work.
Backend/shared changes require the appropriate owner and a separate worktree.

## Delivered behavior and evidence tiers

- **Fixture app:** `dev.photohouse.fixture`, no Internet permission. Installed on
  the user's Samsung SM-S9280 / Android 16 at source commit
  `0acd58fb8185a328ce2e92cbad7de3dd0502d8d6`. Installed APK hash matched the build.
  User accepted the basic fixture flow, called the layout unattractive and
  explicitly deferred UI polish while requesting real implementation.
- **Connected app:** `dev.photohouse.connected`. Real phone/password login,
  phone-bound invited registration, invitation acceptance, own libraries, paged
  gallery, authenticated thumbnails, detail/captions and logout. EN/ZH interface;
  captions remain literal. No deployed endpoint is configured in the default APK.
- **Privacy/transport:** system TLS trust and hostname validation, no redirects,
  cookies, persistent sessions or disk photo cache; bounded image/response memory;
  cancellation and generation guards; private cover and foreground revalidation;
  24-hour expiry; immediate local logout with separate server acknowledgement.
- **Local verification:** frozen contract checks pass; 49 ordinary JVM tests plus
  six actual-backend TLS/ASGI/migrated-SQLite tests pass, with no skips. Both APKs
  build. Lint: zero errors and one known `OldTargetApi` warning per app.
- **Emulator:** connected component tests passed at 1.0 and 2.0 font scales; fixture
  UI/lifecycle regressions passed. These used synthetic data. Integration-only
  changes did not repeat UI testing because production app source was unchanged.
- **Actual backend integration:** proves local JVM adapter interoperability with
  the pinned real Python application. Covers admission, invitations, scoped reads,
  uniform denials, missing media, storage outage, membership/session revocation,
  logout and admission cooldown. Temporary servers, data, media and TLS keys were
  removed. No production service, real credentials/photos or backend checkout
  changes were involved.
- **Hosted CI:** PR #1 and #2 checks passed at their implementation heads. PR #3
  push and PR checks were rechecked green at `2bfb218…`, including GitGuardian.
  The actual-backend suite is local-only; ordinary hosted CI runs the independent
  contract/JVM/lint/build checks. Recheck any later PR head separately.
- **Still unverified:** connected app on the physical phone against a deployed
  protected backend, real certificate/network behavior, real account/media access,
  deployed revocation/logout and full device privacy/TalkBack acceptance.

Full feature completion is still ahead: video/original download, uploads,
albums/search, voice, owner tools, persistent sign-in and UI polish are deferred.
iOS parity was not evaluated by this Android session.

## Unknowns for the receiving session

1. **Deployment identity:** Is the protected backend deployed anywhere? If so,
   what exact source revision is running? If not, state that explicitly. Local
   security tests and a working legacy photo service do not answer this question.
2. **HTTPS origin:** What exact scheme/host/port should the app trust? Does it use
   a certificate trusted by Android's system store, and is it reachable from the
   intended phone network? No cleartext or certificate bypass is supported.
3. **Database and authorization readiness:** Has the intended deployment completed
   the reviewed migrations, owner provisioning, selected library/asset mapping and
   any restore-quarantine/reopening review? The pinned runtime expects migration
   revision `b6e3f9a5c721`; verify the actual target rather than assuming readiness.
4. **Exposure boundary:** Is the protected entry point the one being served, with
   legacy/model/diagnostic endpoints isolated as required by the backend handoff?
   Is TLS termination/proxy behavior compatible with the protected transport?
5. **First test audience:** Which owner will issue the invitation, and which
   deliberately scoped account/library should be used for the first phone test?
   Initial browsing needs viewer membership and cached thumbnails, not original
   access. Keep actual phone numbers, passwords, invitations and photos out of
   public notes, logs, commits and CI artifacts.
6. **Operational authority:** Which deployment, service, account and physical-phone
   operations are currently authorized in the receiving session? The existing
   record authorizes local mobile work, publication of draft PRs and the earlier
   fixture installation. No production service write or merge was performed here.
7. **Integration ownership:** Who will review/integrate the stacked mobile PRs and
   update shared parity? Is the pinned backend commit available to the intended
   CI environment for a later reproducible cross-repository test job?

Please answer from source/operator evidence. Mark unverified facts as unknown;
do not discover a target by probing remembered endpoints or opening real databases
outside the current session's authorized scope.

## Recommended next task: PH-BACKEND-ANDROID-READINESS-01

**Owner:** receiving backend/coordinator session. **Class:** owner review, followed
by bounded implementation if needed. No automatic model/session transfer.

**Initial read scope:** backend AGENTS and its required agent-memory references;
`docs/security/FOUNDATION_HANDOFF_2026-09-09.md`, `EXPLICIT_RUNTIME_ADAPTER.md`,
`OFFLINE_PROVISIONING_APPLY.md`, `OFFLINE_RECOVERY.md`, and the current runtime
source. Also read mobile AGENTS, the shared contract and this note. Historical
session capsules predate these Android implementations and are not current status.

**First outcome:** answer the unknowns above and return either a verified existing
deployment identity or a concrete staging-deployment proposal with target, source
ref, configuration, backup/migration/provisioning sequence, rollback and acceptance
checks. Prepare that proposal before seeking any missing external-operation
approval. This handoff itself requests no deployment, account change or merge.

**Then:**

1. Resolve the protected backend and reviewed test audience before enabling real
   credentials in a build. Preserve the closed default and invitation-only access.
2. Once the origin and access scope are established, the Android owner sets
   `photohouseOrigin` in ignored `android/local.properties`, builds the connected
   debug APK and records its new source/hash. A configured APK can contain a
   private hostname; do not upload it to public CI or commit that configuration.
3. Install on the identified, authorized phone and verify invited registration or
   login, correct-library photos/captions, unavailable/foreign access, revocation,
   background/foreground, offline behavior and logout. Keep real-data evidence
   local and separate from public synthetic evidence.
4. Review the stacked PRs and update parity from observed results. Apply the
   repository's merge authority; publication did not grant a merge instruction.
   Prioritize UI refinement and remaining features after the real browsing gate.

**Return to Android:** deployed yes/no/unknown; exact backend revision; HTTPS
configuration through a suitable private/local channel; migration/provisioning
readiness; intended test role/library and operator; authorized next operation;
remaining blockers. Do not put secrets or private hostnames in a public handoff.

## Evidence and reproduction pointers

- [Fixture result](RESULT.md) and [physical fixture feedback](DEVICE_REVIEW.md).
- [Connected implementation result](connected/RESULT.md).
- [Actual-backend integration result](integration/RESULT.md) and
  [latest local artifact hashes](integration/artifacts.json).
- [Connected app configuration](../../../android/connected/README.md).
- [Actual-backend runner](../../../android/integration/README.md).

Latest unconfigured connected APK in the integration worktree:
`android/connected/build/outputs/apk/debug/connected-debug.apk`.
SHA-256: `259a42051066ae862fe96a311a0fd128607b5cde185d5442d7385d35d3056ab9`.
This is a local build identity, not the earlier phone-installed fixture artifact.
