# Named-place browsing — source slice

Base: `b752e53a62da1ee7e04a983575cf4c438795c757`.
Branch: `codex/android-places-v24`.

Protected phone discovery now shows a Browse by place card with named-place
coverage and a Choose a place action. Facet choices sit directly below it;
selected places still combine with date, people, tag and media filters using
the existing protected discovery API. This is inside discovery, not a new
library-level tab. The Home-mode phone editor has its own named-place coverage
card and retains its existing anonymous Home choices and contract.

TV puts Places first in the existing horizontal exploration row and shows
named-place coverage/unavailable text. Its Home catalog remains separate from
the authenticated library. Neither client interprets missing named-place
coverage as missing GPS. No maps, radius search, new endpoint, location permission,
third-party SDK, network origin or feature-default changes are included.

## Local evidence

- 351 affected JVM tests: home-core 122, live-core 223, TV 6; zero failures/errors.
- Connected and TV Kotlin compilation, debug APK assembly and debug lint passed.
- Home discovery contract verifier and `git diff --check` passed.
- Existing API 36 phone emulator: focused Compose test
  `ConnectedUiTest#discoverySelectionPagingMediaReturnAndBilingualLayout` passed.
  It exercises place entry/selection, combined filters, paging, return and language.
- English place-card and selected-choice screenshots inspected. Synthetic data
  only, no physical phone/projector install. The task emulator was stopped.

Build artifacts are unconfigured local debug builds, not home-install releases.
Existing backend native candidate.16 and Home snapshots remain unchanged: all
new controls use their existing operations and types. Backend source enhancements
are documented separately in `docs/security/PLACE_BROWSING_V24.md` in vlmPhotoHouse.

## Remaining gates

Populate and qualify recorded/reviewed place metadata on Windows; enable a fresh
protected index and independently reviewed Home regions. Then test the signed-in
phone, Home-mode phone and projector with configured release artifacts. Actual
Windows scale, geographic coverage, projector D-pad acceptance, map/area/radius
search, and automatic index refresh are not proven by these local checks.
