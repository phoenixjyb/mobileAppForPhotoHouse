# Phone gallery and TV playback v19

Based on mobile `8d5221c94d20f6863942e2b994d15414c4d9c076`, branch
`codex/android-playback-parity-v19`. Backend candidate 15 pack:
`98d87f3df7cbfdb78367e60fcfa8fa0a117a2e25`; schema remains `f2a6d8b4c915`.
Frozen v1 contracts are unchanged. Exact artifact hashes and counts are in
[validation.json](validation.json).

## Behavior

Phone repeats a gallery GET once after a transport OFFLINE failure, with a
cancellable 350 ms delay and generation/expiry checks. It does not retry TLS,
HTTP errors, rate limits, malformed data or mutations. Transient thumbnail
failures leave the authorized gallery usable and allow subsequent thumbnails;
authentication, TLS and integrity failures keep the existing fail-closed behavior.

The optional `photohousePhonePreparedBrowseEnabled` flag adds a bilingual prepared
video filter. Candidate 15 applies catalog membership before pagination and count,
inside the existing library-read authorization. This identifies published prepared
entries; opening still checks current access, source and derivative integrity.
The flag defaults false and requires media filtering plus prepared playback.
The configured Phone v19 candidate APK enables it and therefore needs candidate 15
active before this new filter can be used. Candidate 14 still supports ordinary
video browsing/playback.

TV replaces MediaPlayer demuxing with Media3 progressive extraction over the same
Home reader. Buffer targets match the phone: 12 MiB compressed samples, 8–12 s ahead,
1.5 s initial and 3 s rebuffer threshold. Decoder memory is additional. Range and
revision checks, reader-owned bounded transport recovery, explicit Play, remote
seek, fit/fullscreen, audio focus and privacy teardown remain. Media3 receives no
HTTP URL or credentials; no disk cache or whole-file download is introduced.

## Validation

- 331 JVM tests pass: 211 protected phone, 118 Home, 2 TV; no failures or skips.
- Both app/instrumentation APKs build; phone/TV lint passes. Configured debug APKs
  build separately and pass signature, version and permission inspection.
- Candidate 15 snapshot verifies 86 ASGI cases, 116 backend source hashes and
  seven payload hashes. All 78 previous captured cases are unchanged.
- API 36 emulator: two prepared-browse EN/ZH phone journeys pass. TV suite discovers
  42 tests: 38 synthetic tests pass; four explicitly gated live tests are skipped.
  TV covers datasource seek/reopen, playback, frame, pause/seek, fullscreen,
  completion/replay, failure, replacement and background release.
- Inspected [English](prepared-en.png), [Chinese](prepared-zh.png) and
  [decoded synthetic TV frame](tv-paused.png). No family media is included.
- The first emulator configuration was unstable. Final evidence uses a fresh 4 GiB
  emulator with host graphics. Initial TV test failures also exposed invalid test
  setup (unconsumed standalone texture) and a stale UI-state assertion; final tests
  use the actual attached texture and await the observed seek position. An
  overlapping instrumentation run was discarded; final runners ran serially.
- `git diff --check`, protected snapshot and TV boundary checks pass.

Phone versionCode 19 / `0.20-gallery-prepared`; TV versionCode 19 /
`0.19-progressive-video`. Artifacts are retained privately outside this public
repository; private server routing stays out of source. No physical phone or
projector was tested in this slice. Device installation, live backend activation
and family acceptance are not implied by these results.

## Backend preparation and next gate

The separate backend branch adds a guarded opt-in 720p phone encoding profile and
stages a hash-verified batch of 100 prepared videos for protected publication.
The staged batch is not active. Activate and qualify candidate 15 plus the new
index before enabling the prepared filter for family use. Phone-profile real-media
qualification and publication are also pending; it does not alter existing TV
renditions or originals. No caption/encoding workers were restarted by this slice.
