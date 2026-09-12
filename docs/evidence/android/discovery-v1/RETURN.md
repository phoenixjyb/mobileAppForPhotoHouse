# TV discovery v1 implementation return

Source commit: **`0e9ba816dc1cb0324744d7ad03351c1f1e39d311`**.
Branch: `codex/android-tv-foundation`; base `d2b224015ee84d12d1e44929cb34893780046793`.
Package `dev.photohouse.tv`, versionCode **6**, versionName `0.6-tv-discovery-dev`.
All changes are local. No push, merge, real-data access, index export, service
deployment/restart, new credential use or physical installation occurred.

## Delivered behavior

- Warm green/cream/gold TV layout, bilingual Explore screen, clear remote focus,
  server-driven ordered family shortcuts and aliases. No household names or
  private ID mappings in public source, test fixtures or compiled configuration.
- Recorded capture-date year/range selection, literal caption phrase search,
  published tags, reviewed coarse locations, photo/video type and combined AND
  criteria. People/tags each support Any/All. Caption mentions do not prove identity.
- Explicit draft Apply/Cancel, removable categories, clear filters, bounded facet
  pagination, metadata coverage and honest unavailable/partial states.
- Isolated paged search results reuse photo fit/fill/zoom/full-screen and native
  video play/pause/seek/full-screen. Viewer Back restores the selected result tile;
  result-grid Back returns to browsing with Explore focus. Empty/failed searches
  never silently show the unfiltered library.
- Edit revalidates metadata; previously loaded facet pages survive only under an
  identical verified metadata binding, revisions and totals. A changed search
  snapshot requests a fresh search rather than automatically retrying old criteria.
- Memory-only queries/results; background/disconnect clear private state. Denial,
  invalid response and TLS failure clear both result and browse generations.

Discovery requires explicit build properties `photohouseTvCatalogVersion=2` and
`photohouseTvDiscoveryEnabled=true`; the default is false. No endpoint discovery or
version fallback. The existing v1 home profile shows search as unavailable.
The backend's frozen contract leaves themes/topics disabled until reviewed
taxonomy exists. They have future-facing UI routes, not functioning production
filters. Raw GPS/radius/map search, semantic search, named albums and tag generation
remain outside this slice. Zoom uses the existing prepared display image; it does
not retrieve an original or increase source resolution.

## Contract integration

Independent discovery runtime pin: `54f68427058c45f6bcc5a863cc6d708f5b325e45`.
Reviewed backend evidence: `35ebe8d465283244c0dae5aa87969ade54279379`.
Contract SHA-256: `754c9edcb7d8dec250d900d942f8a7a6318a1e54680d06d3a1743ff27e05d9cd`.
Schema: `bc15d646ef83a6c5ee91199b3ba212ceaec068cb769541e7373cd2303cf8b92d`.
Examples: `bebb456c9acb4e386400b13888c0c407681c19dbd63903fd9edf39e935c4cdff`.
The 19-input manifest is copied and checked together with the pin. Existing
selected-feed v1, full-catalog v2 and protected-phone contracts remain unchanged.

GET facets and POST search use the same configured HTTPS origin, no cookies/auth,
no redirects, bounded bodies and no-store. Typed IDs, exact fields, metadata
bindings, facet totals/order, page sizes, revisions and search fingerprints are
validated. V2 preview/video transport is reused with unchanged revision-bound URLs.

## Validation

| Evidence | Result |
| --- | --- |
| Home-core JVM | **80 passed**, 0 failures/errors/skips, including 27 discovery draft/controller/wire/TLS checks |
| Phone regression tasks | **22 core + 65 live-core**, existing results confirmed by unchanged Gradle up-to-date tasks |
| TV component tests, normal font | **24 passed**, 34.534 seconds |
| TV component tests, 2× font | **24 passed**, 35.065 seconds |
| Actual backend discovery ASGI | **11 passed**, all 19 pinned source hashes verified |
| Debug application/test APK + lint | Successful; final TV build/lint in `final-tv-build-lint.log` |
| V1 + discovery enabled | Expected build rejection: `Discovery requires catalog v2` |
| Contract/boundary checks | Four passed, including independent pins and no fixture assets in application |

The ASGI replay extracts the pinned Git blobs into a disposable temporary source
tree and uses synthetic records only. Its guards block network listeners/connections,
subprocess launch and database access. It emitted a Starlette/httpx deprecation
warning but all checks passed. [Replay receipt](backend-asgi.json).
Kotlin HTTPS tests use a separate loopback synthetic TLS server. They are not
evidence of deployed backend behavior. [JVM results](jvm-tests.json).

The component surface was an owned API 36 phone AVD in **1920×1080 landscape**,
density 240 (1280×720 dp), not a physical Android TV. Runtime CPU/memory overrides
used four cores/3 GiB after the one-core startup suffered ANRs. No saved AVD config
was changed, and the owned emulator was stopped after testing. Earlier native-key
attempts failed while a System UI ANR dialog held focus and while the test surface
was in touch mode; tests now explicitly establish remote/non-touch input mode.
An additional integration test exposed stale Compose state when replacing the
result store with the browse store, which erased Back focus restoration; keyed
store subscriptions fixed it. Final full suites pass without skipped tests.

46 synthetic screenshots are retained with hashes. The UI/UX subagent reviewed
the normal 1920×1080 discovery render with no blocking issues. The integration
owner inspected normal results plus enlarged Chinese discovery and English results.
The enlarged layouts scroll rather than shrink text. Focus restoration, keyboard
Back, explicit Apply, empty/error/denied results, metadata retry, photo zoom and
native video reuse are covered by the editor/results component tests.

- [Explore, normal](screenshots/1.0/discovery-home.png)
- [Chinese Explore, 2×](screenshots/2.0/discovery-zh.png)
- [Results, 2×](screenshots/2.0/discovery-results.png)
- [Full normal suite](instrumentation-1.0.log), [full 2× suite](instrumentation-2.0.log)

## Exact APKs

All were built from the clean source commit above, with no `assets/` entries in
the application APK. Synthetic JPEG/MP4 media remain in test APK/JVM resources.
Configured APKs stay in the private artifact directory used by earlier TV returns.
No APK was installed on a real device in this slice.

| APK | Purpose | SHA-256 |
| --- | --- | --- |
| `PhotoHouse-TV-home-v6.apk` | Existing private v1 LAN profile; discovery networking disabled | `ae8f6f194f4e9d0f72c51f146af68ecded4a940258f4087aabf27ae76aeef38a` |
| `PhotoHouse-TV-discovery-v6-unconfigured.apk` | Catalog v2 + discovery enabled; origin unset | `792f9a28b9bf3c4a3ffd3798292f21711ea5c8f28a3cd549ecbbda2f397aba83` |
| `tv-v1-unconfigured-v6-0e9ba81.apk` | Default unconfigured v1 build | `6d4a21aa80cf4fed16615a80e87e19e726d4910c35192308e01cfa675ccb2857` |

Existing debug signer SHA-256:
`56d7591b2b6c2538d506d1fe51327444f2307736cb12f5beefa08f1c410d6d28`.
Sizes/profile flags: [artifact receipt](artifacts.json). These are debug APKs;
release variants remain disabled. Existing private v1 configuration is preserved,
not newly validated against a live listener. The discovery APK shows setup until
rebuilt with a separately approved discovery-capable origin/LAN mapping.

## Remaining backend and device gates

1. Publish a reviewed real catalog/discovery index. Captioning progress does not
   establish tag completion; missing person/date/place/tag metadata stays unknown.
2. Resolve and review the seven requested bilingual household shortcuts to stable
   person IDs and operator pin order. Backend owner has the private unresolved
   mapping note; Android never guesses from name text, captions or array order.
3. Approve a compatible v2/discovery LAN origin and publication, then build the
   configured discovery APK. Preserve the working v1 route and rollback path.
4. Validate real prepared photo/video coverage, codecs, TV remote navigation,
   sleep/wake, full-screen output and image quality on the JMGO. Local decoding,
   native-player component tests and an APK build do not establish projector quality.
5. Later reviewed theme/topic taxonomy and geospatial/radius contracts require
   additional implementation. No raw coordinates or speculative taxonomy are used.

The Android source is ready for backend-owner review. No deployment, installation,
push or merge authority is requested or implied by this return.
