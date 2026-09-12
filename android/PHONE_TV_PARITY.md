# Phone and TV feature parity

The family experience should offer equivalent capabilities with touch/portrait
phone screens and remote/landscape TV screens. They are separate APKs; a TV
change is not automatically a phone change. Keep the existing warm neutral,
green and cream phone design. UI language does not rewrite caption content.

## Current implementation

| Capability | Protected phone | Home TV |
| --- | --- | --- |
| Paged browsing, details, literal captions | Implemented | Implemented; configured v1 pilot remains narrower than v2 |
| Photo fit/fill, zoom/pan, immersive display | Implemented; explicit original permission and bounded decode | Implemented on prepared display bytes |
| Next/previous within a page | Implemented in detail and original viewer | Implemented |
| Photo slideshow | Explicit start, 8 seconds, current page only; stops at page end, video, denied/unavailable originals | Implemented on prepared media |
| Video play/pause, seek, fit/fill, immersive display | Implemented; original permission and authenticated Range | Implemented for prepared v2 video |
| Page selection | Numeric touch entry, bounds checked | Remote page controls |
| People/aliases, dates, captions, tags, coarse places, combined search | Awaiting a reviewed protected contract and adapter | Source implemented; real publication/configuration/device acceptance pending |
| Themes/topics, named albums, GPS/radius search | Not implemented | Not implemented; theme/topic controls remain unavailable |

The phone keeps authentication, current library membership and original-file
permissions. It must not use the anonymous `/home` endpoints or send its bearer
there. The TV's no-sign-in home policy does not replace phone access control.
The current phone's original media path is not a prepared derivative endpoint;
prepared playback for viewers without original access needs its own protected
capability and contract. Do not grant originals merely to match TV behavior.

## This phone media slice

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
behavior into the authenticated phone module. The v8 direct player opening,
readiness-aware catalog navigation and remote focus changes remain in the home-TV
path. Phone prepared-media access and protected discovery still need their own
reviewed contracts/adapters.

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

After coordinator review/freeze: implement the authenticated phone adapter,
touch Explore editor and result navigation, then replay real producer responses
and test denial, stale results, later-page selection, EN/ZH and enlarged fonts.
High-resolution prepared media for non-original viewers is a distinct remaining
contract. See this slice's [validation return](../docs/evidence/android/phone-media-parity/RETURN.md).
