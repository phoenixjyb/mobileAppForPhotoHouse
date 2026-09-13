# TV v9 configured install build

Source: `0a4291ec2b02ec0668e823eb7d2d660484e63327`.
Base: `eb59766d41b47864a6792f50a0c8bae240843774`.
Branch: `codex/android-tv-v9-install`; isolated checkout `mobileAppForPhotoHouse-android-tv-v9`.

This packages the reviewed v8 paging/photo/video controls and the shared PhotoHouse
branding as version 9 (`0.9-tv-catalog-dev`). The source change only advances the TV
version. Phone discovery remains separate and opt-in; no phone APK was built here.
Previous worktrees and APKs were preserved. No push, merge or physical installation.

## Artifact

`PhotoHouse-TV-v9.apk`: 10,411,068 bytes.
SHA-256: `6535fa69e852671443f943be85bef1f54bb1b494f993a3299b84befa55df6c82`.
Package `dev.photohouse.tv`, min API 26, target API 34. Verified v2 APK signature;
signer SHA-256 `56d7591b2b6c2538d506d1fe51327444f2307736cb12f5beefa08f1c410d6d28`
matches v8, so this is an update build. It remains a debug-signed development APK.

The private artifact is staged in `Downloads/PhotoHouse-TV-v9-20260912`, with
`INSTALL_EN_ZH.md`, `SHA256SUMS` and `artifact.json`. The `forTV` volume was not
mounted. The existing reviewed HTTPS origin and app-only LAN mapping are compiled
in; catalog v2 is selected, discovery remains false. Actual generated BuildConfig
values were checked. No credentials, trust bypass or system DNS change is included.
Private connection settings and APK bytes stay outside this public repository.

## Validation

- Five offline contract/boundary checks pass: shared phone, home v1, catalog v2,
  discovery v1 and TV boundaries. Backend catalog schema and example match byte for
  byte. Existing backend pins/checksums were not changed.
- 85 home-core plus 2 TV JVM tests pass: zero failures, errors or skips.
- Offline unconfigured build/test-APK/lint and configured build/lint pass with the
  existing Gradle 8.10.2/JDK 17/SDK 34 toolchain. Lint has zero errors; three existing
  warnings remain: OldTargetApi, DiscouragedApi and MonochromeLauncherIcon.
- All 25 synthetic instrumentation tests pass at font scales 1.0 (275.710 seconds)
  and 2.0 (319.173 seconds). Coverage includes real D-pad page jumps, later-page
  selection, photo decoding/zoom, native video decode/play/pause/seek/fullscreen,
  Back, background disposal and bilingual discovery/error flows.
- Rendered synthetic gallery, Chinese page dialog and native-video controls were
  inspected at both font scales. Enlarged gallery rows use scrolling; this is not
  a claim that every control is simultaneously visible.
- The configured APK installed over the synthetic test build on the owned API 36
  `Medium_Phone_API_36.0` emulator, then cold-started with `Status: ok` in 5,675 ms.
  Emulator Wi-Fi/data were disabled for this startup check. This proves configured
  artifact installation/startup, not live server access. This is a phone AVD in
  landscape, not a JMGO device or a TV-launcher acceptance test.
- APK inspection found the shared mark, TV launcher entry, no required touchscreen,
  no packaged assets directory and no ABI-specific native libraries. Test media is
  only in the separate test APK, which is not included in the install folder.

[Verification](verification.json), instrumentation logs and selected synthetic
screenshots accompany this return. The source scan found no staged secrets.

## Backend and physical gates

Backend runtime writes belong to the API task. This Android task reviewed its
proposed immutable revision-2 clone: preserve the 15 served photos and existing
video, add the already verified second video, and leave active qualification files
alone. The reviewed plan is not a publication receipt. No backend service, audience,
original media or preparation job was changed from this Android task.

Actual home-network access, projector installation, D-pad feel, photo quality,
video decoding/audio and long-video behavior remain physical acceptance gates.
Search stays disabled until the separate approved service exists. New video
coverage must come from a coherent backend publication; installing v9 does not
make unprepared assets playable or provide new persistent caching.

## Publication activation follow-up

The backend owner activated revision 2 while packaging completed, reporting
15 photos and 2 short videos ready out of 27,842 catalog entries. At
2026-09-12 21:20:51 +08 this Android task independently read back enabled revision 2,
matching config/control hashes and catalog SHA-256
`7fd30c43a677236e6bfe8ac62221514c1842d796370428701af9966ee1903eb4`.
The v2 listener belonged to pythonw PID 8580; v1, API and caption process identities
were preserved. Native curl verified the hostname/TLS chain and returned expected
403 from the unapproved server peer. This is live publication identity/TLS evidence,
not successful media access from an approved TV peer. The ready-item counts and
57 complete-video Range checks are the backend owner's evidence. Longer qualified
videos and the rest of the library still need their own coherent publication.
