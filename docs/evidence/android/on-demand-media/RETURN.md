# Phone and TV on-demand media return

13 September 2026. Local implementation, debug builds and synthetic emulator
validation; not a Windows rollout or family-device acceptance claim.

Branch `codex/android-on-demand-media`, isolated from
`9e3be2ba2c60477e522d8bd9201db12cf56f7038`. Application source:
`e9ce77b2ed068bd734b1098366043c543ab1cf46`. A later test/evidence commit adds
TV decoder regressions; it does not change the APK application source.
Backend producer pin: `f131c3edfb55092df6ce8a85e7a520fb5515c011`.

## Implemented

TV v3 accepts bounded on-demand preview descriptors, explicit JPEG/PNG originals
and conservatively indexed original H.264 streams, while preserving v1/v2 clients.
Original quality is a deliberate remote action; the photo viewer normalizes EXIF
orientation and bounds its decoded bitmap. A busy thumbnail does not erase the
catalog. Background/generation changes discard late media responses.

The opt-in phone candidate opens a protected optimized display image first,
including for members without original-file permission. Original quality remains
explicit and permission-checked. Fit/fill, zoom, fullscreen and photo navigation
retain the existing viewer and lifecycle controls. No original fallback on denial
or preview failure, anonymous phone route, disk cache, or player URL bypass is added.

Known-length media responses allocate one bounded buffer instead of growing and
copying a second full photo buffer. Large original requests require a declared
length; both formats retain header-first bounded decoding and safe failure paths.
The phone's separate authenticated original-video range reader remains intact.

## Evidence

- **192 JVM tests**: 88 home-core, 102 live-core, 2 TV; zero failures/errors/skips.
- Both debug APKs build. Lint has zero errors: four TV warnings (including the new
  platform ExifInterface advisory) and three phone warnings. No warning suppression.
- API 36 existing emulator: new TV original/zoom/background flow and phone
  optimized-to-original/fullscreen flow pass at font scales 1.0 and 2.0. Phone
  flow covers English and Chinese. Final font-2 manual instrumentation preserved
  screenshots and the exact installed artifacts for inspection.
- Additional TV and phone decoder checks pass: malformed bytes, sampled bitmap
  limits, embedded JPEG EXIF rotation and all eight orientation transforms.
- Rendered synthetic font-2 screens inspected: [phone EN](phone-en-font2.png),
  [phone ZH](phone-zh-font2.png), [TV fullscreen](tv-font2.png).
- Frozen v1 contract: 12 operations, 38 ASGI replay cases, 8 checksummed files.
  Existing fixture/connected/home/v2/discovery/phone-discovery/TV guards pass.
  Independent v3 contract and all 16 pinned backend source blobs verified.
- Both APK signatures verified; installed emulator APK hashes match the exported
  candidates exactly. Artifact bytes, hashes and signer fingerprint are in
  [verification.json](verification.json).

Build flags for these artifacts: `photohouseTvCatalogVersion=3` and
`photohousePhonePhotoDeliveryEnabled=true`; both origins are unset, discovery is
disabled, and no private LAN address is embedded. TV versionCode 11 / 0.11-tv-on-demand;
phone versionCode 5 / 0.6-phone-on-demand. Delivery filenames use `.apk1`, containing
ordinary signed APK bytes. These candidates are not plug-and-play home installers.

Initial checks caught and fixed a photo-upgrade state guard and a nullable v3 UI
branch. A test-only Android-compressed JPEG was replaced with the frozen normalized
preview fixture because the prepared-image parser correctly rejected its metadata.
A combined instrumentation invocation included a class from the other APK; final
runs explicitly targeted each package. No failing run is counted as a pass.

## Remaining delivery gates

Windows services/catalog/originals and captioning were not changed. The existing
live feed therefore retains its previous coverage. Stage the matched source-index
and lazy-cache backend, then build APKs with reviewed private server configuration
before installing on the projector or a physical phone.

HEVC/4K/HDR and other profiles outside direct-play admission still need the existing
preparation pipeline. No automatic bulk queue or persistent Android download cache
was added. Protected prepared-video access for phone viewers without original
permission remains outstanding. TV discovery is still a v2 candidate; its v3
source-reference integration remains outstanding and the v3 build keeps it disabled.
Original delivery does not imply tiled full-resolution zoom. Actual JMGO video
playback and full real-library coverage remain unverified.

No push, merge, physical installation, service restart or deployment occurred.
