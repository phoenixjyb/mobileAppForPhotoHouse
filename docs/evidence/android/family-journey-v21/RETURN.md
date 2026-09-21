# Family journey v21 return

Date: 2026-09-21. Mobile branch: `codex/android-family-journey-v21`.
Base: `3e4e907f69a8aed7ce04944440224359b2dc6f3e` (v20).
The v19/v20 history is retained. No unrelated checkout was reset or cleaned.

## Delivered source

- Phone account/Home and TV previous/next playable video within the current page,
  fresh protected authorization, and explicit session-memory playback resume.
- Protected phone calendar date selection and combined discovery filter summary.
- Opt-in native phone JPEG/PNG contribution with document selection, streamed
  progress, network consent, explicit retry/cooldown, strict receipt validation,
  and account/lifecycle cancellation. Existing asset memories remain separate.
- Backend optional reviewed discovery input producer on branch
  `codex/discovery-review-v21`, commit
  `b61a20896ea1f45dca34954e70af0cdf49dbaae9`, based on candidate15
  `5b7926c298116fbb9384962f8d39a18af9a59990`. It does not alter runtime contracts,
  automatically approve identities, or publish an index.

See [scope, boundaries and remaining gaps](../../../../android/FAMILY_JOURNEY_V21.md).

## Validation

[Machine-readable results](validation.json) distinguish the runs and modules.

- 353 Android JVM tests: playback-core 3, live-core 223, home-core 122, TV 5.
- API 36 emulator: 28 unique phone checks passed across the initial broad run and
  targeted rechecks; 40 TV checks passed in one run, with four live-only checks
  excluded. Phone initial failures were stale translated labels, ambiguous duplicate
  page controls and Back-dispatch settling. The new date-picker check was corrected
  to select the full accessible date. All affected checks subsequently passed.
- Phone checks include navigation/resume with decoded synthetic video, upload
  entry/network warning/receipt, calendar Apply/Cancel, discovery combinations,
  gallery/photo controls, pagination, memories and privacy transitions.
- 46 backend tests: discovery producer, upload and protected native contract.
  The producer group contains 16 tests, including scoped review validation.
- Protected candidate15 snapshot (86 cases, 116 source hashes, 7 payload hashes),
  phone discovery examples and base fixture checks passed. Runtime remains default-off.
- Both APKs assembled and lint passed: zero errors; four phone and five TV warnings
  (target API, platform EXIF, player-state switch and icon/TV platform guidance).
- Connected/TV boundary guards and verification-shell syntax passed.

Representative synthetic UI captures:
[choose](upload-choose.png), [data warning](upload-warning.png),
[receipt](upload-received.png), [resume controls](video-journey-resume.png).
View-rendered captures do not include the hardware video surface; playback tests
inspect decoded synthetic frames separately. Secure-window behavior remains enabled.

Reproduction (configured flags and origins are kept outside this public repository):

```sh
# Android directory, JDK 17, locally provisioned Gradle 8.10.2 / Android SDK
# Use gradle --offline --no-daemon --max-workers=2 with these tasks:
# :playback-core:test :live-core:test :home-core:test :tv:testDebugUnitTest
# :connected:assembleDebug :tv:assembleDebug :connected:lintDebug :tv:lintDebug
python3 android/verify-connected-boundaries.py
python3 android/verify-tv-boundaries.py
bash -n scripts/verify-android.sh
# Backend producer worktree, project Python dependencies, synthetic tests only:
PYTHONPATH=tests/security python -m unittest tests.security.test_discovery_index_producer tests.security.test_upload tests.security.test_protected_native_contract
```

## Artifact and delivery gates

Configured debug artifacts are private handoff files, not committed to this public
repository: `PhotoHouse-Phone-v21-candidate15.apk` and
`PhotoHouse-TV-v21-home.apk1`, both versionCode 21. Their source revision, SHA-256,
byte sizes and signing verification are recorded alongside them.

Phone candidate enables prepared browsing, discovery and uploads for qualification.
Before family use it needs candidate15 activation, a reviewed current discovery
index and a qualified incoming root. These are not confirmed live by this return.
TV uses the existing Home API. No physical phone/projector installation, real SAF
provider upload, real server acceptance, Windows deployment/restart, publication,
push or merge occurred in this slice. No encoding worker was changed.

Remaining implementation: durable playback history, cross-page video navigation,
background/resumable or video upload, batch memories/audio transcription, ingestion
status and a member-visible publication workflow. Themes and geo-radius/map search
also remain beyond the existing reviewed place filters. Full-screen photo tools and
existing asset-memory editing are preserved rather than reimplemented here.
