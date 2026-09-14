# PhotoHouse TV — anonymous home feed

The home projector browses without sign-in, pairing, cookies or personal account
credentials. `home-core` supports the frozen selected-photo v1 feed and the new
whole-catalog v2 API through separate adapters. The phone contract is unchanged.
Build-time catalog selection is explicit; there is no endpoint/version fallback.

## Implemented flow

V15 recovers interrupted catalog-video HTTPS reads with at most two retries
(250 ms then 750 ms backoff), sharing one 20-second deadline per range. Each
attempt uses the same asset, revision, offset and strict response checks. TLS,
denial, revision, malformed-data and server-availability failures are not retried.
Closing/backgrounding cancels the active request or backoff. Phone Home mode uses
the same adapter; protected phone networking is unchanged.

V14 keeps gallery controls on one compact horizontal row. Media type, readiness,
ordering and paging stay directly accessible; More contains Explore, refresh,
preview retry, language and disconnect. Large text can scroll horizontally without
wrapping controls onto another row. Remote focus and the selected tile are retained.

Video reads now reuse one memory-only window (at most 256 KiB), avoiding an HTTP
request for every small native MP4-header probe. The shared reader also serves
phone Home mode. Close/revision changes discard the window and cancel reads.
The TV preparation deadline runs independently of the player looper, including
blocked `setDataSource`, and returns the existing retryable diagnostic after 30s.
This is a tested client improvement; real projector decoding still needs acceptance.

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

## Discovery and advanced search

The TV now has a dedicated Explore screen with a warm green/cream/gold palette,
bilingual people shortcuts, date browsing, caption text, tags, reviewed places and
media type. Advanced search combines categories with AND; people and tags each
offer Any/All. Year shortcuts and inclusive date ranges use recorded capture dates.
Search results reuse the existing photo fit/fill/zoom/full-screen and native video
controls. Back returns to the selected asset, then to browsing; clearing search
revalidates the catalog. Empty matches never fall back to all media.

Shortcuts use server-published reviewed person IDs, labels, aliases and pin order.
No family names or private person mappings are compiled into the app or test
fixtures. Caption mentions never establish identity. Only existing published tags
are searchable; captioning/tag generation is a separate backend process. Coverage
counts describe available metadata, not an assertion that tagging is complete.

The independent frozen discovery v1 pin is
`54f68427058c45f6bcc5a863cc6d708f5b325e45`, with contract/schema/examples and
19 source-input hashes in `../home-core/discovery-contract/`. The adapter uses
same-origin HTTPS GET facets and POST search, revision-bound pages, an isolated
result store and unchanged v2 media URLs. Editing revalidates metadata and retains
loaded facet pages only under the identical verified metadata binding. Revision
conflicts ask for a fresh search instead of retrying an obsolete query. Filters,
metadata and results are memory-only and clear on background/disconnect.

Enable only for a separately approved discovery-capable v2 server using
`photohouseTvCatalogVersion=2` and `photohouseTvDiscoveryEnabled=true`.
Discovery defaults to **false**, including the existing v1 home profile. There is
no endpoint probing, version fallback or fixture fallback. On an older server,
Explore explains that search is not available yet and browsing remains usable.
The production contract deliberately leaves themes/topics unavailable until a
reviewed taxonomy exists. Places are reviewed coarse regions; map/radius search,
raw GPS queries, named albums, semantic search and tag generation are not
implemented in this slice. See [DISCOVERY_PLAN.md](DISCOVERY_PLAN.md).

Verify with `python3 android/verify-discovery-contract.py` from the repository root.
`android/home-core/integration/verify-discovery-backend.py --backend CHECKOUT --python PYTHON`
replays the pinned backend's actual discovery ASGI against disposable synthetic
inputs with network, subprocess and database access blocked. This is separate
from Kotlin loopback TLS tests, emulator tests and real server/device acceptance.

## Frozen media contracts and image quality

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
Named albums, screensaver startup and offline storage are not implemented.
Discovery/search source is implemented behind the explicit v2 discovery switch;
real reviewed metadata publication and projector acceptance remain pending.
V2 catalog/video source integration is implemented; real prepared-media coverage
and the v2 origin/publication remain a coordinated backend continuation.

### Approved real-media canary

Version code 7 allows an in-place update from the v6 pilot using the existing
debug signer. Default builds still have no server configured. A private catalog
build explicitly selects version 2 and the backend's verified origin/address;
discovery remains independently opt-in. A v1-configured APK cannot play v2 video.

After the backend publishes an approved canary, `:home-core:catalogLanPilotTest`
uses the production HTTPS adapter and video reader with normal TLS validation.
It requires `PHOTOHOUSE_CATALOG_LIVE_APPROVED=true`, private
`PHOTOHOUSE_CATALOG_TEST_ORIGIN` / `PHOTOHOUSE_CATALOG_TEST_ADDRESS`, and the
reviewed `PHOTOHOUSE_CATALOG_PHOTO_ID`, `PHOTOHOUSE_CATALOG_PHOTO_PAGE`,
`PHOTOHOUSE_CATALOG_VIDEO_ID`, and `PHOTOHOUSE_CATALOG_VIDEO_PAGE` inputs.
Keep values and test outputs private. Ordinary tests exclude this test.

The check reads only page one and the explicitly selected pages, verifies both
preview variants for the photo/video, streams at most 64 MiB of prepared video
through bounded Range reads, checks its full SHA-256, and exercises a backward
seek. Media remains in memory; it is not written to files or included in reports.
This proves client transport/integrity, not Android decoding or projector playback.
Full-library coverage must be reported separately from a successful two-item test.

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
[Discovery implementation and artifact evidence](../../docs/evidence/android/discovery-v1/RETURN.md).

## Synthetic metadata exporter compatibility

The separate offline exporter is now implemented by the backend owner at
`d8f20a073e42f93ad2b046c4ae01d0ccd68a05bf`. This is a test-input source pin;
the app's discovery serving pin remains `54f68427058c45f6bcc5a863cc6d708f5b325e45`.
No APK or production origin is changed by this integration.

`android/home-core/integration/verify-discovery-export.py --backend CHECKOUT --python ACCESS_PYTHON`
extracts and verifies the 19 frozen serving inputs plus two exporter/fixture
sources, creates fresh synthetic SQLite and a reviewed disabled bundle, captures
six actual ASGI responses and compares them to the JVM fixture. Only its own
temporary fixture is enabled for requests, then disabled again; network and
subprocess access are guarded. `--update-fixture` explicitly regenerates the
test-only fixture after reviewing the exporter pin. Ordinary verification never
overwrites it. `DiscoveryExportCompatibilityTest` sends those responses through
the actual Android parser, including reviewed identity coverage, blocked tags,
missing metadata and v2 photo/video availability.

[Exporter integration evidence](../../docs/evidence/android/discovery-export/RETURN.md).

## Readiness browsing candidate (v13)

Use `photohouseTvCatalogVersion=3` and `photohouseTvBrowseEnabled=true` only
with the producer pinned in `android/readiness-browse-contract/manifest.json`.
Default is off. The candidate adds Ready first, Ready only and All/Photos/Videos
controls, applies them before server pagination, and reports published readiness
counts. Existing private origin configuration is still required; the public APK
is unconfigured. No source edit installs or activates this candidate.

See [evidence and remaining phone parity](../../docs/evidence/android/readiness-browse/RETURN.md).
