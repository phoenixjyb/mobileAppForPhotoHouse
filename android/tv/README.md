# PhotoHouse TV — anonymous home feed

The home projector browses without sign-in, pairing, cookies or personal account
credentials. `home-core` supports the frozen selected-photo v1 feed and the new
whole-catalog v2 API through separate adapters. The phone contract is unchanged.
Build-time catalog selection is explicit; there is no endpoint/version fallback.

## Implemented flow

- Configured cold start/foreground fetch the selected API automatically: v1
  `/home/v1/feed`, or v2 `/home/v2/catalog` starting at page 1.
- V2 supports up to 100,000 published assets, 50 per page. Later pages carry the
  snapshot revision. Remote page selection offers first/last and ±1/±10 jumps.
  Photo/video/unsupported kinds and unprepared/missing/failed states are explicit.
  No URL means no media request; it never falls back to originals or legacy routes.
- Paged grid, remote D-pad/OK focus, Back to the selected tile, literal captions,
  EN/ZH/system-default UI and fit-to-frame display images.
- Photo controls: Fit preserves the whole image; Fill crops to the viewport. Zoom
  enters full screen at 2×. Full-screen OK cycles 1×/2×/4×; arrows pan enlarged
  photos and left/right change photos at 1×. Back resets zoom before returning to
  controls. Transforms reset on asset/revision/coverage changes. Hints hide after
  four seconds and return on a remote key. No higher-resolution bytes are fetched
  when zooming. The Play page button/media key controls an eight-second slideshow.
  Slideshow stops at the current page's end, missing
  display image, a video/unsupported item, errors, background, disconnect and leaving the viewer.
- Metadata refresh every 60 seconds while visible. Changed metadata clears the
  old page and images. Revision conflicts clear and refetch after bounded retry.
- Network/server failures clear images and retry at 2/5/15/30/60 seconds with
  up to 20% positive jitter, capped at 60 seconds. Server Retry-After may impose
  a longer minimum and survives background/reconnect. Denial, invalid responses
  and TLS failure require explicit retry. There is no authentication fallback.
- Background cancels requests and clears content; foreground fetches afresh.
  Disconnect stays locally paused through background/foreground until Reconnect;
  a new process starts a fresh anonymous connection. No media/session persistence.

## Native video and full-catalog integration

The independent `TvVideoPlayer` and `HomeVideoReader` now implement explicit native
play/pause, 10-second seeking, elapsed/duration display, aspect-fit rendering and
full screen. OK plays/pauses in video full screen; left/right seek. Changing full
screen preserves the surface/player. Audio focus loss pauses; background, close,
surface destruction and decode error release the source/player. No audio autoplay,
URL delegated to MediaPlayer, disk media cache or personal account adapter.

The reader serializes bounded random reads, cancels an in-flight read on close,
rejects wrong chunk lengths and suppresses stale copies/errors. The v2 adapter validates exact 206 status, Content-Range, Content-Length,
content type, no-store and bounded received bytes for every read. It rejects a 200
fallback, changed totals, redirects, truncation and malformed ranges. Server-side
publication/chunk integrity remains distinct from client transport validation;
the client does not download a whole movie to verify its whole-file hash.

The native component is exercised with a test-APK-only synthetic H.264/AAC clip.
The v2 gallery is wired through catalog metadata, availability and video opening
into this player. Selecting Open video prepares it silently; Play starts playback.
Closing returns to the selected viewer, then Back restores its grid tile. Decoder
failure stays in the viewer; network denial/revision failure clears the generation.
V1 remains photo-only. Real v2 media preparation and deployment are backend gates;
see [FULL_LIBRARY_PLAN.md](FULL_LIBRARY_PLAN.md) and [CATALOG_V2_CAPSULE.md](CATALOG_V2_CAPSULE.md).

## Frozen contract and image quality

V2 runtime pin: `a5d0f595d7cd26379ed2845a944ec1d58d7885cc`.
V2 contract SHA-256: `13cf10892dc4e91631ad71b5ee4bed21baa44697f1e779851c19026dbe606120`.
V2 schema/example and 16 source-input hashes: `../home-core/contract-v2/`.

V1 runtime pin: `e6b2827842b2c0b5223c85208299b60e8a1257f6`.
Contract SHA-256: `70328a653ddaa559bad6a4d654cf9870c89c5217e8e9e6c46a3501e9dd9e7548`.
Exact Android-owned copies and backend input hashes: `../home-core/contract/`.
Independent phone pin: `87a60b475b37b1d6873cd977bcb6e7254472da7e`.

The server prepares grid and display baseline JPEGs. The client validates byte
length, SHA-256, dimensions, metadata framing and revision-bound relative URLs.
V1 makes no Range requests. Neither adapter requests originals or account/provider fallback media. A missing
variant stays a placeholder. Captions are plain text, not HTML.

Display limits: 4,096 maximum edge, 8,847,360 pixels and 12 MiB compressed bytes.
The supplied 3840x2160 fixture decodes at full resolution. Grid images are limited
to 512 pixels per edge, 262,144 pixels and 2 MiB each. Compressed grid retention
is capped at 16 MiB per page; grids exceeding that total show placeholders, while
any selected asset can still load its display image. Decoding is sequential and
off the UI thread, with bounded dimensions before allocation. Images are fitted
and clipped to their view. The prepared images are already normalized/oriented.

This does not establish the JMGO's Android surface resolution or visible 4K
projection quality. The source fixture, decoded bitmap and projector output are
separate evidence.

## Build and checks

Existing JDK 17, SDK/build tools 34 and cached dependencies, from `android/`:

```sh
./gradlew --offline --no-daemon :home-core:test :core:test :live-core:test :tv:lintDebug :tv:assembleDebug :tv:assembleDebugAndroidTest
```

From the repository root, `python3 android/verify-tv-boundaries.py` checks source
isolation. `android/verify-tv-emulator.sh emulator-SERIAL 1.0 EVIDENCE_DIR` runs
synthetic component tests; repeat with 2.0 for large fonts. It refuses physical
serials and configured APKs. Fixtures exist only in JVM/test APK resources.

`android/home-core/integration/verify-backend.py --backend CHECKOUT --python PYTHON`
verifies pinned Git blobs and replays actual in-process backend ASGI checks in a
disposable synthetic publication. It does not start a listener. Kotlin HTTPS
adapter tests use a separate loopback-only synthetic TLS server.

## Delivery gates

The default debug APK has an unset origin and shows setup. A private build can
set `photohouseTvOrigin`, optional `photohouseTvLanAddress`, and
`photohouseTvCatalogVersion` (`1` default, or `2`) through ignored
local properties or Gradle environment inputs. The address is the server's
canonical RFC1918 IPv4 address. It maps only the configured HTTPS hostname inside
PhotoHouse and uses a direct connection. The URL hostname, TLS SNI and platform
certificate/hostname verification remain in place. Unexpected hosts and invalid
addresses fail closed; there is no public-DNS/proxy fallback for the mapped feed.
When no LAN mapping is supplied, ordinary system DNS remains the default.

The user explicitly requested this APK-contained LAN configuration after the
manual projector DNS plan. The mapped APK requires no projector DNS change and
does not change the device's IP, gateway or DNS settings. Other TV apps continue
to use their system settings. If the server's LAN address changes, rebuild with
the newly verified address. Keep configured APKs and logs private. The phone's
`photohouseOrigin` remains independent; never substitute its server for /home/v1.

Backend owns separately authorized synthetic LAN deployment, normal local DNS/TLS,
peer isolation, selection publication and lifecycle/rollback. The user-requested
app mapping is a separate resolution path from ordinary projector system DNS. No public IP or port
forwarding is needed for LAN-only routing. Actual JMGO firmware/API (minimum 26),
launcher, remote keys, sleep/wake, image quality and installation/operator window
still need device evidence. A landscape phone AVD is only a component test surface.
Named albums/search, screensaver startup and offline storage are not implemented.
V2 catalog/video source integration is implemented; real prepared-media coverage
and the v2 origin/publication remain a coordinated backend continuation.

[App-contained LAN configuration return](../../docs/evidence/android/home-tv-lan-map/RETURN.md).

[Original integration return](../../docs/evidence/android/home-feed/RETURN.md).
[Historical prototype return](../../docs/evidence/android/tv/RETURN.md) predates
the implemented backend and this anonymous adapter; its pending-backend statements
are superseded by the current return.

Verify v2 with `python3 android/verify-catalog-contract.py` from the repository root.
`android/home-core/integration/verify-catalog-backend.py --backend CHECKOUT --python PYTHON`
extracts all 16 pinned Git blobs and replays the actual backend's synthetic ASGI
checks with listeners, subprocesses and SQLite blocked inside the replay. The
reviewed replay came from backend evidence commit
`868cbb48aec50fa9c01689ee071c9d999e0b8e0d`; the runtime pin above is independent.
The v2 response contract and half-second synthetic MP4 are identical in JVM and
instrumentation resources. A separate twenty-second synthetic clip covers native
playback/seek; neither clip is packaged in the application APK.

[Current v2 integration and artifact evidence](../../docs/evidence/android/catalog-v2/RETURN.md).
