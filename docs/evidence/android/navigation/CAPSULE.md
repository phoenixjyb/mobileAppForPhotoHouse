# PH-ANDROID-PHOTO-NAVIGATION-01

- Owner/executor: current Android session; no delegation or model switch.
- Class: bounded mobile implementation using the existing browsing contract.
- Base: `0cabd3efcb1ebeaab63deef864dbc56f515991d3`.
- Branch: `codex/android-photo-navigation`, isolated sibling worktree
  `mobileAppForPhotoHouse-android-navigation`.
- Backend pin: `87a60b475b37b1d6873cd977bcb6e7254472da7e`.

Outcome: previous/next photo navigation within the selected gallery page, with
both Back actions returning to that page. Each photo is fetched through the
authenticated detail/captions/thumbnail operations. New screens start at the top.

Scope: Android connected UI, connected store, corresponding JVM/backend/UI tests,
emulator verification script and `docs/evidence/android/navigation/**`.
Read scope includes mobile AGENTS, README, development/security docs, existing
capsules, contracts and prior Android handoffs. Shared contracts, backend code,
iOS, production configuration and previously recorded evidence remain unchanged.

Plan: implement page-scoped navigation; verify race/privacy/error behavior; build
and test against the synthetic backend and emulator; record artifacts and commit.

Invariants: retain only current-page IDs in memory; clear navigation at privacy
boundaries; stale responses cannot restore detail after returning to the gallery
or changing libraries; revoked access clears photo context and rechecks session;
no persistent history, originals fallback, new endpoint or trust override.

Acceptance: focused store regressions, ordinary JVM/build/lint/package checks,
existing actual-backend TLS suite extended with navigation/revocation, and English/
Chinese emulator component tests at normal and large font sizes. These checks use
synthetic data. They do not establish protected staging or real-phone acceptance.

Return the exact commit, build hashes, test results, screenshots and unresolved
gates. Host/service changes, live credentials, real media, physical installation,
push and merge remain outside this task's authority. Return to the coordinator if
the contract or backend must change.
