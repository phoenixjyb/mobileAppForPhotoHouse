# Home TV full-library continuation

The user observed a successful projector connection and the synthetic color-stripe
image, then requested browsing all PhotoHouse photos and video assets like the web
experience. This supersedes the briefly selected latest-100 option. The product
remains no-sign-in browsing on the home LAN; there is no new public sharing scope.

## Current boundary

The installed candidate consumes frozen home-feed v1: at most 2,000 selected assets,
prepared grid/display JPEGs, no video metadata/Range endpoint. Merely replacing the
synthetic manifest cannot implement arbitrary full-catalog video browsing.
Keep that working publication until the reviewed replacement is ready.

Backend owns discovery of the existing PhotoHouse catalog/library, aggregate asset
counts/formats, bounded paged catalog contract, photo derivative availability,
read-only video/seek semantics, scope checks and publication/deployment. Preserve
originals, database memberships and ongoing caption/model work. No eager whole-
library copy or conversion is implied. A revised contract returns exact source,
schema/fixture checksums, media limits and error/revision behavior to Android.

## Android implementation after the contract return

1. Consume the new version through an independent TV adapter; preserve protected
   phone contracts and v1 evidence. Keep the private in-app LAN mapping and TLS.
2. Browse bounded pages containing image/video kinds, explicit missing/unsupported
   states, lazy previews, remote focus and Back restoration. Do not fetch the entire
   catalog or every original into memory.
3. Add explicit remote video play/pause, seek and close, aspect-fit surface, audio
   focus, and stop/release on background, disconnect, denial or content revision.
4. Validate synthetic parser/pagination/Range and lifecycle boundaries; exact
   configured APK, authorized real feed reads, then JMGO user/device acceptance.

The existing owned phone VideoPlayer and VideoReader provide a reviewed native
MediaPlayer/MediaDataSource pattern: serialized bounded random reads, cancellation,
owned audio/surface lifetime and no URL handed to the platform player. Adapt that
pattern for TV controls and the returned home-video contract without reusing phone
bearers or inventing server wire fields. Existing test-only synthetic H.264/AAC MP4
can cover native playback before real-video acceptance. Actual codec support and
visible/audio output remain device checks.

Backend task: 01a08410-d390-7632-86f2-1812b1dd5917. Android owns its existing TV
worktree and APK/player only. Backend was sent the corrected full-library scope and
asked to preserve current services/data while returning the next bounded contract.
No full-library feed, TV video playback or real-video acceptance is claimed yet.
