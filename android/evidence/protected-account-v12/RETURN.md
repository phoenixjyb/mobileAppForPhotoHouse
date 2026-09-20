# Phone v12: deployed protected-account compatibility

Base: `126863e4a87622081a89dad72f9a4ef27aeb9f7e`.
Branch: `codex/android-protected-v12-20260920`.

## Fixed behavior

Candidate.12 adds `display_name` to every authenticated session response, nullable
for older accounts, and requires `name` when registering. The v11 protected client
used the frozen strict session decoder, so even successful login could fail when
it fetched the profile. Invited registration also omitted the required name.

The protected profile now has a separate strict session decoder and named
registration request. It accepts null names without inventing an account name,
retains library membership/revocation checks and rejects unexpected fields,
duplicate keys, malformed Unicode and invalid name shapes. Name whitespace is
collapsed consistently with the backend and capped at 64 Unicode code points.
Passwords are never trimmed. Invitation-only membership and original-media grants
are unchanged. The frozen v1 serializer and shared contracts are unchanged.

English/Chinese registration collects the name, blocks invalid input and clears
form fields on submission, mode change and privacy lifecycle transitions. Submission
and mode changes also explicitly dismiss the keyboard so it does not cover the
next library or photo screen. A valid
name appears in the library greeting. The v11 photo viewer, fit/width/height/actual
size, fullscreen and zoom/pan remain available. Preview pixels are not original
quality, and display delivery remains disabled in the private configured build.

## Contract identity and verification

Android snapshot: `2.0.0-candidate.12`, backend source
`45f2123ad3447213aad68010154a6d14ff3613f9`, pack checkout
`0789cabea29c1faa4a67bf7ac9f9a9d12abcbf34`, schema `f2a6d8b4c915`.
All five copied pack files are byte-for-byte backend-owned artifacts. The verifier
pins the manifest, 109 source hashes and seven backend payload hashes together.
No backend source or schema was changed by this Android work.

The local HTTPS tests replay captured invited registration and named/revoked
sessions through the real Android adapter. Separate backend ASGI replay validates
all 61 captures using generated accounts/media and temporary SQLite only.

Build/lint passed. JVM: 282 tests (22 fixture core, 143 live core, 117 Home core),
zero failures/errors/skips. Backend ASGI: seven tests, all 61 captures replayed.
Normal UI: eight distinct cases passed across the recorded runs and targeted
replays. Final registration/keyboard/viewer checks: two passed at normal scale
and two at 200% font, including English/Chinese. One video progress timeout passed
on unchanged replay; no claim about physical decoder reliability follows from it.
Screenshots were visually inspected; enlarged content remains scrollable.

See `verification.json` and accompanying test logs for run details. Emulator and
local HTTPS evidence do not establish physical phone or real-account acceptance.
The initial snapshot-restored emulator reported a process-startup ANR before any
assertion completed; the cold-boot run is recorded separately.

## Remaining product work

- Real phone acceptance of login, preview controls and authorized video playback.
  The user requested the APK ready rather than installed.
- Protected prepared-video delivery without original access is not implemented by
  the backend. Existing playback uses separately authorized original MP4/WebM
  ranges; arbitrary MOV/codec compatibility is not claimed.
- Higher-quality protected photo display needs its backend provider qualified;
  increasing zoom alone cannot improve a thumbnail's detail.
- Protected client adoption of people, tags, dates, albums, uploads and story
  editing is separate. Updating the snapshot does not implement these features.
- Map/location privacy and combined discovery need backend-owned contracts;
  uploads remain disabled and no model/worker/service was touched.

Private server routing and artifact identity stay in the local handoff, outside
this public repository. No real credentials, media or biometric data were used.
