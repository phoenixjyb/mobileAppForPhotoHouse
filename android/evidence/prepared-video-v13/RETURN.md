# Phone v13: protected prepared-video playback

Base: `baa113c18fc955305a76af9d2e6b75dfc19da2b3`.
Branch: `codex/phone-prepared-video-v13`.

## Behavior

A protected library viewer can open a prepared H.264 MP4 without original-file
permission. The separate `photohousePhonePreparedVideoEnabled` build switch
requires the protected profile and defaults off. A candidate APK enables it for
later backend/device qualification. Anonymous Home/TV access is unchanged.

HEAD checks media type, exact bounded size, no-store policy, byte ranges and a
strong SHA-256 ETag before opening a decoder. Each authenticated read sends Range
and If-Range, reads at most 256 KiB, and rejects changed identity/length or a full
200 response. The player gets a memory-only reader, never a URL or bearer. No
persistent cache, whole-file preload, automatic retry or original fallback is
introduced. Original video remains an explicit, separately authorized action.

Unavailable, changed, busy and unconfigured playback have English/Chinese messages.
Retry is manual; a busy response disables actions for the server delay. Closing,
backgrounding, sign-out and access denial close the reader. Late HEAD results
cannot reopen a hidden viewer. A transport failure retains its classified error
even when a native decoder error arrives before the queued callback.

## Contract and evidence

Adopted candidate.13 source `1d9248e577947c4b8fea1a1551f11b2f78bc2781`,
pack `7af4498e6a55b4c17f7223e815d145e4a595ce0c`, schema `f2a6d8b4c915`.
All 70 captures, 115 source hashes and seven payload hashes verify together.
Frozen v1 contract verification passed; shared contracts were not edited.

- JVM: 292 tests, zero failures/errors/skips (22 fixture, 153 live, 117 Home).
  Includes local TLS readiness/ranges, malformed metadata, validator changes,
  denied access, no redirects/fallback, cooldown and privacy races.
- Build, test APK assembly and connected lint passed.
- Backend: 27 targeted tests passed, including complete 70-case ASGI replay,
  prepared streaming and index export. A first invocation omitted the existing
  test module path; rerun with `PYTHONPATH=tests/security` passed.
- API 36 emulator: seven selected UI journeys at explicit font scale 1.0 passed;
  two prepared-video journeys at scale 2.0 passed. An initial seven-test run also
  passed but its inherited scale was not recorded, so is not normal-scale proof.
- The 2:05 synthetic video decodes, plays and seeks to 90s, 123s and back to 2s
  without downloading all bytes before readiness; reads remain <=256 KiB.
  Original access is denied in the prepared journey. Original-video regression,
  registration, bilingual photo controls and private story journeys also passed.
- Readiness 404, explicit retry, 429 cooldown, recovery, background and logout
  exercised in emulator; denial/change and decoder/transport races covered in JVM.
- Screenshots and separate decoded frames were visually inspected. Software
  window captures omit the hardware video layer; frame captures are separate
  evidence, never composited. Large text wraps and remains scrollable.

The private APK is versionCode 13 / `0.14-prepared-video`, debug-signed with the
existing certificate. The packaged DEX confirms protected/prepared switches on,
protected photo delivery/discovery off. SHA and private routing are recorded in
the local handoff outside this public repository. It was not installed on a phone.

## Remaining delivery gates

Candidate.13 provider/index activation on Windows, actual service-identity file
permissions and long-video/revocation qualification remain required. The current
server was not contacted or changed. This candidate will report unavailable when
prepared delivery is not configured; it is not a claim of working live service.

Physical phone acceptance, WAN interruption/throughput, actual family video codec
compatibility and device memory behavior remain untested. High-quality protected
photos, discovery/location, uploads and story editing remain separate work.
No push, merge, deployment, worker restart, real credentials or media were used.
