# Phone photo viewer: protected previews and sizing controls

Base: `f24fc41cd1484c1f89389fcc523cc4d4459e411e`.
Branch: `codex/android-photo-viewer-v11`. Android-only source change.

## Problem and behavior

The phone already had a fullscreen/pinch-zoom viewer, shared by Home mode and
original/display photo delivery. However, the configured protected profile had
display delivery disabled and did not grant originals. Tapping an image therefore
stopped at a static thumbnail detail and could not reach the existing viewer.

The current protected profile can now open its authorized thumbnail preview in
that viewer. Gallery taps and the detail's **View preview / 查看预览** action both
recheck asset detail and scoped preview delivery. Preview viewing never implicitly
requests `/display` or original media. An unavailable preview stays unavailable;
denial, TLS and other failures are not bypassed. The default legacy profile's
behavior remains unchanged.

Protected detail requests the existing contract's 1024-pixel thumbnail variant.
The existing adapter falls back to the 256 thumbnail only on a missing variant,
not on authorization or other errors. This neither configures a renderer nor
promises that a larger preview is present on the live server.

Both phone browsing modes use the same new viewer controls: fit whole photo,
fill, fit width, fit height, actual size, zoom in/out, pinch/pan, double tap,
fullscreen and return to controls. Actual size means one **decoded image pixel**
per physical screen pixel at base zoom. Preview and reduced-resolution images
are labeled honestly; zoom cannot recover unavailable source detail. Layout is
bounded to the viewport before graphics-layer scaling, including thin panoramas.

The original-quality action still requires server permission and explicit user
selection. Page navigation, slideshow limits, background covering, logout,
session expiry and generation-bound late-response rejection remain enforced.

## Evidence and delivery

Final results are recorded in `verification.json`. The UI evidence uses generated
solid-color images and synthetic accounts only. Physical device installation,
authenticated viewing and family acceptance are separate from these checks.

App ID stays `dev.photohouse.connected`; version code 11, version name
`0.12-photo-viewer`. The configured private artifact preserves the previous Home
and protected origins, with protected display and discovery disabled. No backend,
WebUI, Windows service, data, permissions, or contract snapshot is changed here.

WebUI viewer behavior and legacy person ownership repair belong to their owning
PhotoHouse task; this Android change does not resolve or bypass those policies.
