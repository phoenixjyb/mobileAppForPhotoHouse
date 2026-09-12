# TV v8 paging and media-opening fixes

Source commit: `efadba4702c322f14d097640fad93fc6f07f46d0`. Base: `97cd45d716573b9beecf0992a1b9a16a96edfb70`.
Branch: `codex/android-tv-real-media`. Previous TV/phone worktrees were preserved.

## Changes

- Keep enabled TV buttons focusable after touch/air-mouse input. Restore the selected or first viewable tile; focus navigation when a page has no viewable media.
- Replace the hidden horizontal page steps with six visible controls in two rows. Start on +1 (or -1 at the end), make remote movement explicit, and disable unchanged-page confirmation. A wider dialog retains the page number above its scrollable controls at enlarged EN/ZH text sizes.
- Open ready video tiles directly into the native player. Play remains explicit. Returning or backgrounding during asynchronous opening cannot reopen playback.
- Disable entries without display/playable media, distinguish loading from unavailable thumbnails, and show preparation coverage for the current page. Photo navigation/slideshow skips unavailable catalog entries; zoom/fullscreen controls wait for an image.
- Add explicit opt-in live Android tests, gated before Activity/network startup. Ordinary component runs exclude them. Real media stays in memory; screenshots contain synthetic fixtures only.

## Diagnosis and evidence

The server returned catalog pages successfully, including later pages with no prepared media. On the original v7 source, live Android photo opening/zoom and prepared-video playback worked; its touch-based page jump also passed. The initial zoom assertion ran before an asynchronous decode finished and passed with a completion wait. This was not evidence of a persistent photo-decoder defect. Remote focus and the misleading treatment of unprepared entries were client issues addressed here. The exact installed JMGO version and its reported decoder behavior could not be inspected over ADB.

- **84 home-core + 2 TV JVM tests passed**, zero failures/errors/skips.
- Four frozen contract verifiers and TV source-boundary checks passed. Backend pins/checksums were unchanged; no new backend ASGI replay was needed for this UI/store change.
- **25 synthetic UI tests passed at each of 1.0 and 2.0 font scale**, including catalog, discovery, native player, focus/Back and privacy lifecycle. After visual review and the final dialog-only adjustment, its bilingual remote test was rerun at both scales and passed. It navigates first to last page and back using real D-pad events. The normal-size suite log is retained; its initial screenshot-copy wrapper encountered an editing-related EOF after all tests passed. Final dialog screenshots were collected successfully in the focused reruns.
- **4 live Android tests passed on API 36**, installed from the configured APK below: all 16 prepared entries' grid/display JPEGs decoded (32 images); a real photo opened and zoomed; the prepared video entered the native player, played and advanced past one second; remote page jump reached page 2 and returned to page 1. No family-media screenshots/files were saved. These results certify the Android emulator, not the JMGO decoder/display/audio or physical remote.
- Configured debug build and lint passed: zero errors, three existing warnings (OldTargetApi, VectorRaster, DiscouragedApi). APK metadata and signer were verified; no app assets are packaged.

Raw component/live result logs and final synthetic dialog images accompany this return. [Verification receipt](verification.json) records the evidence tiers and artifact identity.

## APK

Private artifact: `PhotoHouse-TV-real-media-v8.apk`; package `dev.photohouse.tv`, version 8 (`0.8-tv-catalog-dev`).
SHA-256: `9f1f3ba676e5fc88e0e3f2651a5a17e4ed41e96856449ecbf35556b31e0ad0a0`; 9,073,439 bytes.
Signer SHA-256: `56d7591b2b6c2538d506d1fe51327444f2307736cb12f5beefa08f1c410d6d28` (same debug signer as v7).
The existing private HTTPS origin and app-scoped LAN mapping are included; catalog v2 is selected and discovery remains disabled. Connection values, APK and raw build receipts stay outside Git. Install over the previous TV build for a physical retest.

## Remaining blockers and backend handoff

The live revision still exposes 27,842 catalog entries but prepares only **15 photos and one video**, all on page 1. The other 27,826 entries cannot display/play until the backend generates and publishes their derivatives. This APK does not increase that coverage. Page counts retain the server's complete catalog; the readiness count is explicitly per page.

The backend task returned source candidate `50391b6db8a7c41dc276f6ec36c28dfc0b0a934d` and evidence/plan commit `6469cfc991325a82e4f70e3eab36d843fe05b5cb`. Its full-range MOV sample conversion preserves limited-range output and tightens validation. The owner reports 86 passing Mac tests, zero skips; Android reviewed its return rather than rerunning those tests. The endpoint/wire contract is unchanged. Windows FFmpeg and the real failed MOV are untested with that source, and it is not deployed.

The backend's `docs/security/HOME_VIDEO_RANGE_RETURN.md` proposes separately approved offline Windows staging/native tests, then a disabled candidate containing the existing 16 items plus the failed MOV. Activation/restart follows candidate review. Larger sustainable coverage also needs a reviewed carry-forward helper; current checkpoints cannot be rewritten or imported across changed script/base revisions. Captioning should remain running while only the owned preparation job is stopped if contention appears.

No physical projector was reachable over ADB, so no physical installation or acceptance is claimed. No service restart, caption pause, publication change, push or merge occurred in this Android slice. Phone parity work was preserved and no new phone APK was delivered.
