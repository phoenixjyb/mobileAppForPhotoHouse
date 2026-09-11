# PhotoHouse TV — PH-ANDROID-TV-01

A separate native remote-controlled viewer, sharing `live-core` and the frozen
wire contract with the phone app. This is a TV prototype build, not a certified
JMGO or Google TV release. No Google Play services, touch screen or microphone
is required. Both ordinary and Leanback launchers are declared for Android-based
projectors that use custom launchers.

## First slice

- Landscape library selection and paged photo grid, visible gold remote focus,
  D-pad/OK buttons and Back to the selected photo tile.
- Full-screen fit-to-frame images. In full screen, left/right select photos,
  OK or media play/pause toggles the current-page slideshow, Back shows controls.
- Eight-second slideshow, stopping at the end of the current page. It stops on
  errors, loss of access, leaving the viewer, background and logout. Videos show
  a still preview; video playback is not part of this TV slice.
- Original caption text in a scrollable dialog; EN/ZH/system-default interface.
- No personal sign-in or registration fields on the TV. The viewer reuses approved
  library access, memory-only sessions/content and background clearing. Automatic
  home access is pending a backend contract: either one-time owner-approved device
  access or an explicitly selected unauthenticated LAN feed. The current backend
  supports neither TV mode. No password or invitation is embedded in the APK.

## Image quality

Grid requests keep the default cached thumbnails. Detail requests ask for an
already-cached 1024-pixel preview using the existing contract. Only a 404 falls
back to a cached default thumbnail. Permission, TLS, size, rate-limit and server
errors propagate without fallback. Missing both sizes stays a placeholder;
no model/provider work or original fetch is triggered by a missing thumbnail.

Choose **Use originals** to request full-quality images only when the server
returns `originals_allowed=true`. This choice applies to subsequent image slides
in that viewing session and clears when leaving it or on privacy/error transitions.
It never changes server permissions. The staging library currently has no original
grants, so the control is absent there. Preparing a 1024 derivative or enabling
originals for a selected audience is separately owned backend work.

LAN bandwidth helps transfer speed, but bitmap memory is still bounded: 12 MiB
compressed original limit, one decode at a time, header/dimension checks and at
most 8,847,360 displayed pixels. A 3840x2160 photo retains all pixels; larger
images are sampled down, EXIF orientation is applied and aspect ratio is preserved.
Grid bitmaps are separately capped at 262,144 pixels. This does not promise that
a projector renders the Android app surface at native 4K; firmware/output mode
and visible projection quality must be verified on the actual device.

## Build and checks

Use existing JDK 17, Android SDK 34 and cached dependencies. From `android/`:

```sh
./gradlew --offline --no-daemon :live-core:test :core:test :tv:lintDebug :tv:assembleDebug :tv:assembleDebugAndroidTest
```

With origin empty, run `android/verify-tv-emulator.sh emulator-SERIAL 1.0` from
the repository root. It refuses physical-device serials and configured builds.
An existing phone AVD rotated to landscape proves component behavior only; it
cannot establish a JMGO launcher, physical D-pad or TV keyboard acceptance.

The default APK has no server. A configured APK currently stops at home-access
setup because the no-sign-in TV backend contract is not implemented. Setting an
origin alone cannot make this build fetch a protected library automatically. For an approved private build,
set `photohouseTvOrigin` in ignored `android/local.properties` to the selected
HTTPS origin. The phone's `photohouseOrigin` is independent. No user-CA, cleartext,
certificate bypass or arbitrary URL field is added. Keep configured artifacts,
configuration and logs private because they embed the origin. Only debug is enabled.

## Home installation and next steps

1. Resolve the [home-access contract request](HOME_ACCESS_REQUEST.md). Then confirm the JMGO O3 Ultra's firmware, Android API (minimum 26), supported APK
   installation route, ABI, normal/Leanback launcher and actual remote key behavior.
2. Verify the private home hostname route and system-trusted HTTPS from that unit.
   Public IP/port forwarding is unnecessary for LAN-only access. Local DNS may
   resolve the certificate hostname to a private address; never substitute an IP
   that fails hostname validation. Network/certificate changes are not this task.
3. Prepare a synthetic audience and appropriate image derivatives. Verify the exact
   TV APK/package/signer, then authorize installation and the selected operator.
4. Test keyboard admission, remote focus, library/grid/page/back, full-screen fit,
   captions, pause/end-of-page, 4K source decoding/output, original permission,
   disconnect/revocation, sleep/wake, cold start and logout. Record real-device
   results separately from synthetic emulator/source evidence.

Named albums/search, automatic device access, screensaver
startup, offline media and video are later slices. They need explicit product or
backend contracts; the app does not invent endpoints or treat LAN membership as
identity. Real family-library cutover remains separate from synthetic staging.

[Build and test return](../../docs/evidence/android/tv/RETURN.md).
