# Full-library TV acceptance plan

Recorded 2026-09-12. This is a reviewed backend handoff and an Android validation
plan, not new APK, conversion, publication or device evidence.

Android base: `18c797c832adabeacf0be5230556e330065a0eaf`; branch
`codex/android-tv-real-media`. TV v8 source remains
`efadba4702c322f14d097640fad93fc6f07f46d0` and its configured APK is unchanged.
Phone parity remains separate and was not updated by this handoff.

## Reviewed backend return

The backend owner returned source `0872cf8ae799b366eb80da7b6d77ff9ad79d56b7`
and evidence/plan `2383162249b5bfcb79cf391ac87a64d39c58d69a`, documented in its
`docs/security/HOME_FULL_COVERAGE_RETURN.md`. Android read that return; it did
not independently rerun the backend's reported 108 passing Windows tests.

The coordinator automatically traverses a visible snapshot, checkpoints each
item, verifies sources/derivatives before reuse, resumes interrupted conversion
and disabled publication, and carries reviewed outputs into newer immutable
revisions. It no longer requires manual selection of each 16-item batch.

The read-only audit covers 27,842 entries: 23,599 photos and 4,243 videos.
Current-profile header candidates total 26,947; this does not prove decoding or
readiness. There are overlapping gaps: 598 videos exceed the 60-second worker
budget, 185 exceed 512 MiB input, and 257 images exceed 40 MP. Proposed larger
SDR and bounded JPEG profiles could address many gaps, but have not been
implemented or measured in this return. Twenty-one header/probe/MPO/missing
cases remain unresolved even under that projection. No successful probe showed
HDR transfer/10-bit input; unresolved cases must not be assumed SDR.

The observed live baseline remains revision 1 with 16 ready items. Disabled
revision 2 has 17. No new real bulk conversion or activation occurred. Captioning
continued during the audit. These operational observations belong to the backend
receipt and are not a new live check by Android.

The current client accepts 100,000 entries, 2,000 pages of 50, video duration up
to 24 hours and prepared video size up to 32 GiB. These are parser ceilings,
not a recommended encoding budget or proof of native playback. The observed
library size and longest/largest inputs fit those wire ceilings. No Android
contract/pin change is indicated by the current audit.

## Acceptance sequence

| Stage | Required checks | Evidence still needed |
| --- | --- | --- |
| Backend profile completion | Long/large SDR hash/decode deadlines and stop behavior; large JPEG subsampling with orientation/color and bounded memory; explicit unresolved-file reasons | New source and focused/native tests; no silent budget increase or arbitrary MPO frame choice |
| Representative qualification | Short and long H.264/HEVC/MOV sources, large JPEGs, resource/disk usage, caption progress, source immutability and verified resume | Measured real candidate; replace weak short-clip time/storage extrapolations |
| Android synthetic catalog | First/middle/last page of 27,842 entries; selections on later pages; empty/unavailable pages; no duplicates, omissions or focus loss across Back | v8 already has synthetic page-557/Back coverage; add transitions with mixed availability and revision replacement |
| Android long video | Generated multi-minute MP4 exceeding the 4 MiB transport chunk; forward/back seek across chunk boundaries and near EOF; fit/fullscreen, pause/resume, Back and background close | Production HTTPS adapter plus native player, correct time/frame progression and bounded in-memory reads; short current real clips are insufficient |
| Revision refresh | Old revision rejected during grid, photo and video reads; old image/audio cleared; new snapshot loaded; removed/hidden items never retained; later-page selection reset safely | Synthetic race/lifecycle checks first, then actual HTTPS behavior after a reviewed activation |
| Published candidate | Verify catalog count/revision, every prepared object's integrity/decode, preserved old ready entries, long-video Range/HEAD and no legacy route exposure | Backend bulk completion and disabled candidate receipt; queue drained alone must not count as full coverage |
| Physical JMGO | Exact installed v8 identity, real photos of varied orientation, large-image fit/zoom/pan, long-video seek/audio/aspect ratio, remote paging/Back, EN/ZH and enlarged layout | Connected projector or user device feedback after activation; emulator/API success cannot replace this |

All local media fixtures must be synthetic. Any approved real Android tests keep
family media in memory and omit screenshots/files. Keep current authentication,
LAN peer boundary, normal TLS, original permissions and frozen protocol checks.
If qualification reveals a contract issue, return its exact failing case to the
backend owner before changing either side.

## Rollout and completion accounting

Preparation must report verified-ready, pending, deferred, failed and missing
counts with explicit reasons. Preserve reusable output after interruption.
Do not repeatedly retry unchanged format/resource failures. A partial publication
must be clearly labeled partial; it must not be described as a complete album.

Bulk processing, activation and projector acceptance are separate. A new live
revision must be an immutable reviewed publication with the old target retained
for rollback. No service or caption action is authorized by this plan. The
backend's illustrative 129–187 worker-hour / 152–659 GB workspace-plus-publication
projection is based on 15 small photos and two very short clips and is not an ETA
or validated capacity requirement. Representative qualification must replace it.

This planning change changes documentation only. Existing [v8 verification](RETURN.md)
remains the exact source/build/emulator/live-canary evidence. New long-media,
full-publication and physical-device checks above have not been run here.
