# Phone media navigation v16

The authenticated gallery now shows asset numbers on cards and detail screens.
“Open by number” opens an authorized asset in the selected library through the
existing detail/captions/media path; it does not add an endpoint, permission,
hardcoded family asset, or automatic playback. Back returns to the prior gallery
page or discovery context.

Page access appears above the gallery as well as below it. The page dialog starts
empty with the current page as a placeholder, so entering a new page cannot append
to the old number. The numeric keyboard Done action submits a valid page. The top
controls are usable while thumbnails are loading; a new selection cancels stale
reads through the existing generation boundary.

Direct lookup accepts only canonical positive signed-64-bit asset numbers.
Session/library changes dismiss the dialog, and server authorization remains
required for each read. The gallery lists library assets; it does not claim every
listed video has a prepared playback copy.

Regression coverage: direct lookup beyond the current page, no autoplay, return to
the prior page, malformed IDs, no-session access and server denial. Physical phone
navigation and playback evidence is recorded separately in the private handoff.
