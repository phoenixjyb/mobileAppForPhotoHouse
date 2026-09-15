# Phone and TV feature parity

Latest Home rollout: [deployment evidence](../docs/evidence/android/home-search-v16/DEPLOYMENT.md).
Caption/date/media search is deployed; people/place review and the tag-roster limit remain open.

The family experience should offer equivalent capabilities with touch/portrait
phone screens and remote/landscape TV screens. They are separate APKs; a TV
change is not automatically a phone change. Keep the existing warm neutral,
green and cream phone design. UI language does not rewrite caption content.

## Home search parity candidate — 15 September 2026

TV v16 and phone versionCode 7 now consume an independently pinned discovery/v2
candidate with current v3 media. Phone Home gains a touch search editor and
server-provided quick-person shortcuts; TV reuses its existing remote editor.
Both retain v3 photo/video behavior in search. Search remains opt-in; this source
and emulator work does not enable the feature on the installed projector.

| Capability | Phone Home | Home TV |
| --- | --- | --- |
| Caption, dates, reviewed people/aliases, tags, recorded regions, combined filters | New source and emulator checks | Existing remote UI, updated v3 result adapter |
| On-demand photos, permitted originals, prepared/direct video in search | Shared strict discovery/v2 adapter | Same adapter |
| Ready-only and ready-first in album browsing | Existing | Existing |
| Ready-only or ready-first within search | Not in current contract | Not in current contract |
| Themes/topics, named albums, GPS radius, protected prepared video | Remaining | Relevant Home discovery features remaining |

[Source return and validation](../docs/evidence/android/home-search-v16/RETURN.md)
records the independent backend pin and rollout gates. The protected account
comparison below remains separate from anonymous Home parity.

## Phone Home mode continuation — 14 September 2026

The owner has now requested an explicit anonymous Home mode in the phone APK.
[Phone Home mode](PHONE_HOME_MODE.md) adds the same published v3 catalog, readiness
filters and media access as TV through a touch UI, separately from account sign-in.
The table below describes **protected account mode**; its prepared-video/readiness
gap remains. Phone Home mode does not grant protected-library access or broaden the
server audience. See its validation return for build/device/deployment boundaries.

## Current implementation

| Capability | Protected phone | Home TV |
| --- | --- | --- |
| Paged browsing, details, literal captions | Implemented | Implemented; configured v1 pilot remains narrower than v2 |
| Photo fit/fill, zoom/pan, immersive display | Implemented; explicit original permission and bounded decode | Implemented on prepared display bytes |
| Next/previous within a page | Implemented in detail and original viewer | Implemented |
| Photo slideshow | Explicit start, 8 seconds, current page only; stops at page end, video, denied/unavailable originals | Implemented on prepared media |
| Video play/pause, seek, fit/fill, immersive display | Implemented; original permission, authenticated Range, 32 GiB bounded reader and long-video feedback | Implemented for prepared v2 video |
| Page selection | Numeric touch entry, bounds checked | Remote page controls |
| People/aliases, dates, captions, tags, coarse places, combined search | Implemented behind an opt-in candidate build flag; live backend/device acceptance pending | Source implemented; real publication/configuration/device acceptance pending |
| Ready-first, ready-only, All/Photos/Videos | Outstanding: protected prepared-media contract required | Implemented in v13; backend deployed, physical TV acceptance pending |
| Themes/topics, named albums, GPS/radius search | Not implemented | Not implemented; theme/topic controls remain unavailable |

The phone keeps authentication, current library membership and original-file
permissions. It must not use the anonymous `/home` endpoints or send its bearer
there. The TV's no-sign-in home policy does not replace phone access control.
The current phone's original media path is not a prepared derivative endpoint;
prepared playback for viewers without original access needs its own protected
capability and contract. Do not grant originals merely to match TV behavior.

## Phone continuation, 12 September 2026

Branch `codex/android-phone-parity-streaming` starts from integrated default
`8fdd280be55724ca020d12113cacc7a40463d08b`. Phone build 3 (`0.4-phone-streaming-dev`)
adds direct gallery-to-viewer opening, a separate Details action, 32 GiB Range
handling and long-video seek/buffering feedback. Returned metadata controls media
kind and original permission; a video still requires Play. Persistent caching is
not enabled. See [streaming/cache policy](MEDIA_STREAMING.md).

Protected discovery now has a separately pinned opt-in HTTP candidate and phone adapter/editor.
Prepared derivatives remain blocked on their own reviewed HTTP capability. The backend owner is progressing discovery transport source and
real full-library qualification separately. No phone route is inferred from TV.

## Protected discovery continuation, 12 September 2026

Phone versionCode 4 (`0.5-phone-discovery-dev`) retains phone streaming and shared
branding, adding the separately pinned protected search contract, adapter and editor.
The feature defaults off until explicit reviewed server/build configuration. Source
checks, mock TLS, actual producer replay and emulator tests remain distinct from a
live protected discovery service or family-device acceptance.

## Earlier phone media slice

Base `838d392861a474ad8b9e2faf0994f35f26b085cd`; isolated branch
`codex/android-phone-media-parity`. Application `dev.photohouse.connected`,
versionCode 2, versionName `0.3-phone-media-dev`, debug only and origin unset.

- Whole-photo fit or aspect-preserving fill with a cropped-edge indication;
  zoom/pan remain bounded, and changing photos resets zoom and pan.
- Immersive photo/video display with a visible Show controls action. Back first
  restores controls, then closes the viewer. Native video uses the same reader
  and player across fit/fullscreen changes, with no audio restart.
- Photo next/previous and slideshow reuse protected detail/original reads. Each
  item rechecks the returned permission and media kind before original access.
  No page crossing, wrap, denied-item skip or video autoplay. A stopped pending
  slideshow cannot restart when its response arrives.
- Decode completion starts each 8-second interval. Undecodable photos stop the
  timer. A manual navigation, Back from fullscreen, close, failure, background,
  logout, library switch or expiry stops it. No durable query/media state.
- Viewer controls scroll at large font sizes; the image/video always retains a
  viewport. Native video is scaled in both axes without stretching.
- Screen-awake and immersive navigation flags are temporary; secure-window and
  normal TLS/authentication behavior remain unchanged.

## Integrated source and TV test boundary

Phone media source `fd7a72ac400c604bf078d04f3fb62cbdee4be806` and TV v8 source
`efadba4702c322f14d097640fad93fc6f07f46d0` are now combined in the integration
history. This preserves both APKs; it does not automatically port TV-only UI
behavior into the authenticated phone module. Phone now also supports direct permitted-media opening. The v8
readiness-aware catalog navigation and remote focus changes remain in the home-TV
path. Phone prepared-media access still needs its own reviewed contract. Protected
discovery now has an opt-in candidate adapter; its live service remains pending.

The private configured v8 APK targets catalog v2 with discovery disabled. It has
four passing API-36 live-canary tests: all 32 previews for 16 prepared entries,
photo zoom, video play and remote page navigation. The live publication recorded
by that return contains 15 photos and one video; a later backend candidate is not
proof of an activated release. Full-library processing and physical JMGO acceptance
remain distinct gates. Public CI APKs have empty origins and cannot connect to the
private service without an explicitly configured local build.

See [v8 evidence](../docs/evidence/android/real-media-v8/RETURN.md) and its
[full-library acceptance plan](../docs/evidence/android/real-media-v8/FULL_LIBRARY_ACCEPTANCE.md).

## Search continuation

Backend proposal `8d8e88974b8be515ad5a9ab088b91a94652b1e71` documents protected
library-scoped discovery. The existing backend owner implemented its unmounted
service slice at `ddfb0fb893d96482eb1b4b1015328c66ec96077c`; Android independently
verified 15 committed source blobs, 25 focused tests and 10 internal replay checks.
This adds no callable endpoint. Proposed HTTP/field names are not frozen.
No guessed phone routes, global people list, caption-to-identity inference,
or shared-contract edits belong in this media change.

The authenticated phone adapter, touch search editor and result navigation now consume
backend candidate `af8e0c8cf749f6e963dd8b196dce9aa842240387` through the separate
[opt-in phone discovery contract](phone-discovery-contract/README.md). The default APK
keeps the feature disabled; protected server wiring, provenance/index admission and
physical-device acceptance remain separate.
High-resolution prepared media for non-original viewers is a distinct remaining
contract. See this slice's [validation return](../docs/evidence/android/phone-media-parity/RETURN.md).

## On-demand delivery candidate — 13 September 2026

Both app formats now have an opt-in photo delivery path. TV v3 advertises
on-demand previews and explicitly permitted originals; the phone opens optimized
protected display images first, including for viewers without original grants.
Both expose an explicit original-quality action where permitted and keep bounded
EXIF-aware decoding, fit/zoom/fullscreen, navigation and memory-only state.
TV now also consumes conservatively indexed H.264 original streams without
conversion; existing prepared v2 video and phone authenticated original Range
remain available. This does not add protected prepared-video access for phone
viewers without original permission, HEVC/4K direct-play negotiation, tiled zoom,
or automatic full-library preparation. The live v2 feed and physical projector
playback remain separate rollout/acceptance gates.

See [candidate contract](on-demand-contract/README.md) and
[validation return](../docs/evidence/android/on-demand-media/RETURN.md).
