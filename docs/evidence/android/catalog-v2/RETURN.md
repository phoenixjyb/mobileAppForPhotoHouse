# TV full-catalog photo and video return

Source commit `6077904f4d2b5c160e600f88a44dc11c396bbcaa`, branch
`codex/android-tv-foundation`, isolated worktree `mobileAppForPhotoHouse-android-tv`.
Catalog slice base `181927ed12787a4f1edc72f9621d5d80a63b5854`; preceding photo/player
source `10711dc63f9404911cf3c8b1df509c78fb38dc95`, overall base
`b2fcdadafb070c32c3d7a8657eb537043ad1acbb`. All source changes are under `android/`;
this return adds Android-owned evidence only. Existing phone/iOS/shared contracts
and other worktrees were preserved.

## Implemented behavior

- Photos: Fit/Fill, 1×/2×/4× zoom, bounded remote panning, full screen, timed hints,
  slideshow, captions and Back reset. Prepared display images remain bounded to
  4K; zoom does not fetch an original or manufacture additional detail.
- Videos: native decoded playback, explicit Play/Pause, ±10-second seek, progress
  and duration, aspect-fit full screen, audio-focus handling and release on exit,
  background, surface loss or failure. A replacement video does not autoplay.
- Catalog: mixed photo/video pages, remote page jump, explicit unavailable states,
  selection/focus restoration, revision-aware cancellation and bounded memory.
- Transport: independent frozen v2 adapter, strict catalog parsing, verified JPEGs
  and bounded single-range MP4 reads. TLS validation stays enabled; wrong status,
  range, size, revision, redirects and truncated responses fail closed. There is
  no original-media or legacy-route fallback.

## Observed validation

| Evidence | Result |
| --- | --- |
| Home-core JVM | 53 tests, zero failures/errors; suite/case names in `unit-tests.json` |
| Synthetic Android instrumentation, font 1.0 | 16 passed, 122.738 seconds |
| Synthetic Android instrumentation, font 2.0 | 16 passed, 287.434 seconds |
| Actual backend in-process ASGI replay | 12 checks passed against 16 hash-verified source inputs |
| Debug APK, test APK and lint | Passed; final source APK builds repeated after commit |
| Frozen v1/v2 contracts and TV boundaries | Passed |
| Shared protected contract verifier | Passed; its 38 retained ASGI cases are not 38 newly executed backend tests |
| Diff whitespace and emulator-script syntax | Passed |

Instrumentation ran on the already available API 36 phone AVD in landscape
1920×1080, not a physical Android TV. It exercised decoded synthetic video frames,
play/seek/full-screen/background/source replacement, mixed catalog navigation,
zoom and native OK on dialogs. Ten retained screenshots were visually inspected,
including large-font controls and the page-557 jump dialog. Toolbars scroll through
remote focus. The owned emulator was stopped after testing; no attached devices
remained. Synthetic media is confined to tests and absent from application APKs.

The backend replay used the frozen Git blobs in-process with real-media, listener,
subprocess and SQLite access blocked. It is separate evidence from Kotlin mock-TLS
tests and from native rendering. See `backend-asgi.log` for all twelve checks.
No fresh phone-module test run, hosted CI, Windows runtime test or physical-device
installation was performed in this slice.

Toolchain: JDK 17, Gradle 8.10.2, AGP 8.5.2, Kotlin 1.9.24, Compose BOM
2024.06.00; Android minimum API 26, target API 34. Builds used cached dependencies
offline. Reproduction commands and emulator harness are in `android/tv/README.md`.

## Frozen backend identity

- Runtime: `a5d0f595d7cd26379ed2845a944ec1d58d7885cc`
- Replay/evidence source: `868cbb48aec50fa9c01689ee071c9d999e0b8e0d`
- Contract SHA-256: `13cf10892dc4e91631ad71b5ee4bed21baa44697f1e779851c19026dbe606120`
- Input manifest SHA-256: `77586ebb028751d9600b0f04356c2e943f89f432253b3bb647916acec9bf0cb0`

The pinned contract, example and per-source hashes are retained under
`android/home-core/contract-v2/`. Later backend preparation work does not silently
move this serving-contract pin.

## Built artifacts

All artifacts use development package `dev.photohouse.tv`, version code 5,
version name `0.5-tv-catalog-dev`, and the existing debug signer SHA-256
`56d7591b2b6c2538d506d1fe51327444f2307736cb12f5beefa08f1c410d6d28`.
Artifacts remain outside the public repository. `result.json` records exact sizes,
hashes, build profiles and source; configured endpoint values are excluded.

| APK | Profile | Bytes | SHA-256 |
| --- | --- | ---: | --- |
| `PhotoHouse-TV-home-v5.apk` | Existing home origin/LAN mapping, v1 | 8900979 | `9520d3a119334b083747c200747f00d5cd0956a2c221082371ea45e747941d32` |
| `PhotoHouse-TV-catalog-v5-unconfigured.apk` | v2, no origin | 8900932 | `12daa39b2f4c85d92b8bab86cf8cf68c5ea8064a28e34ea26ab9b8836aed3af9` |
| `tv-v1-unconfigured-v5-6077904.apk` | v1, no origin | 8900926 | `7cf8ab01cf0827a290fa40a590cae01b705c8c1bb32fa5c89bc261a3336b77a6` |

The configured home APK can exercise the new photo controls against the existing
v1 publication. It will not automatically switch to v2 or gain catalog video
through a backend update alone. V2 selection is explicit at build time; once the
backend supplies an approved v2 origin and mapping, build a matching configured
v2 APK before the projector trial. No v5 APK was installed on the projector.

## Remaining operational and acceptance gates

The backend owner reports an offline preparer implementation at
`0c3230a71a23bdc74b4f913a3f92c360268edbca`, evidence
`e424ca0ccf2432c27441382247dbe3b53f8890f4`, with 62 focused tests and a synthetic
Mac canary. Those are owner-reported preparation results, not tests independently
replayed in this Android return. No live v2 origin has been returned.

Next steps are backend-owned Windows synthetic replay, a measured real photo and
short-video preparation canary, and a reviewed v2 publication/listener that
preserves the working v1 service. Then configure the v2 APK and verify real JMGO
photo quality, codecs, audio, remote controls, seeking and sleep/wake behavior.
Prepared-media coverage, long/HDR/oversized video handling and physical acceptance
remain open. Current video contract targets prepared H.264/AAC up to 1080p; it does
not claim native playback of every original format or 4K/HDR video.

No real media was read or converted by this task; no Windows deployment, DNS or
firewall change, new public ingress, push or merge occurred.
