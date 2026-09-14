# Phone Android development app

Phone v6 adds an explicit **At home / Sign in** launcher. The new
[Home mode](../PHONE_HOME_MODE.md) uses the existing anonymous home v3 publication
with its own private routing and server audience. The account-mode history below
retains its original protected scope; Home mode is not an account fallback.


Current [MVP status and source/test mapping](../MVP_STATUS.md) ·
[pilot acceptance/configuration](../PILOT_ACCEPTANCE.md). The scoped browsing MVP is
locally implemented; deployed-service and phone acceptance remain pending.

`dev.photohouse.connected` is a separate debug-only application. It implements the
frozen native contract through OkHttp 4.12.0: phone/password login, invited
registration, own-session and library selection, invitation acceptance, paged
gallery, authenticated thumbnails, asset details, literal captions and logout.
It supports English and Simplified Chinese interface text, page-preserving photo
navigation, an original-photo viewer with zoom/pan/fit/fill/fullscreen and a
current-page slideshow, plus permission-aware native video playback with
play/pause/seek/fit/fill/fullscreen. The gallery, details, admission and Settings
layouts have completed the local UI refinement slice.

It is not the complete PhotoHouse product. File downloads, uploads,
search, albums, voice, owner administration and persistent sign-in are absent.
Real backend deployment and physical-device acceptance have not been verified.

## Configuration

These are instructions for a later approved pilot, not authorization to build,
open listeners or run a device in the current no-listener scope. Initial viewer
original access must remain off. See the ordered pilot handoff above.

Default builds show the mode chooser; each destination has empty routing and cannot
collect credentials or make requests. After the operator identifies a reviewed
HTTPS deployment and approves the configuration/build lane, place `photohouseOrigin` in the ignored
`android/local.properties` file. Its value must be an HTTPS origin with no
credentials, path prefix, query or fragment. Do not commit that file or a configured
APK containing a private origin. Passwords, tokens and invitations are never build
inputs. There is no runtime endpoint editor or fixture-to-network switch.

Use the same installed JDK 17 / Android SDK 34 toolchain documented in
[the Android README](../README.md). From the repository root:

```sh
scripts/verify-android.sh
android/gradlew -p android :connected:assembleDebugAndroidTest
android/verify-connected-emulator.sh emulator-5580 normal
android/verify-connected-emulator.sh emulator-5580 large
```

The last two commands require an already-running emulator, reject physical devices,
and restore its font scale. Keep the origin empty for these tests. The connected
APK is `android/connected/build/outputs/apk/debug/connected-debug.apk`.

## Transport and privacy

The `protocol` module shares only typed wire fields with the fixture app. The
`live-core` module contains the real adapter and generation-bound state. The
connected APK has no shared fixture assets or fixture adapter dependency; the
fixture APK retains no Internet permission. Default-valued request fields are
encoded explicitly, including `transport: "native"`.

Production construction uses system certificate roots and normal hostname
validation. Cleartext, redirects, cookies, disk cache, HTTP logging, automatic
authentication retries and refresh tokens are disabled. The internal custom-client
constructor is used only by JVM tests: either synthetic in-process interceptors
or runtime-generated certificates on loopback in the separately authorized TLS lane. No trust overrides or test certificate are packaged into the app.

Bearer credentials stay in memory. Logout, account/library changes and background
events cancel outstanding calls and clear private views. Late responses cannot
cross generations. Foreground session revalidation must succeed before content is
uncovered. The 24-hour deadline clears state even while idle. Cold process starts
are signed out. Local logout is immediate; server revocation is labeled confirmed
only after acknowledgement. A cancelled/lost login or registration response may
leave a server session until its expiry; there is no invented recovery endpoint.

Each JSON response is limited to 512 KiB, each thumbnail to 1 MiB, and retained
thumbnail bytes to 8 MiB. Thumbnail decoding rejects dimensions above 1024 pixels.
Only the exact same-origin, library-scoped thumbnail path is accepted. A missing
thumbnail becomes a placeholder and never falls back to an original or provider.
No photos, credentials or navigation are persisted; backup and saved-state
restoration are disabled, with `FLAG_SECURE` protecting task snapshots.

The original viewer is separate from thumbnail loading. It is offered only for
image details with `originals_allowed=true`; a gallery tap or Open original photo
action opens `/assets/{id}/media?library=…`. Explicit next/previous and an explicitly
started current-page slideshow may request subsequent permitted photos, with
fresh detail/permission checks. The server still authorizes every request.
Closing, changing views, backgrounding, logout and expiry cancel/clear original
state; late bytes cannot reopen the viewer. No download/export or original-media
fallback is performed. Original responses require HTTP 200 and JPEG, PNG or WebP
content type, with a 12 MiB compressed-byte cap, including unknown-length bodies.
The display decoder bounds dimensions, samples to at most four million pixels and
applies EXIF orientation. Larger photos may be displayed at reduced resolution;
unsupported or corrupt images show an unavailable state. Decoding is serialized
off the UI thread. Pinch/pan, double-tap, zoom buttons and fit-to-screen controls
operate on the in-memory bitmap, with no URI or file handed to another app.

The video player requires fresh video details with original permission and an
explicit gallery tap or Open video action. It accepts MP4/WebM over authenticated single Range reads, up
to 256 KiB per read and 32 GiB per file. Sizes and seek offsets remain 64-bit;
the file limit does not reserve or download that much memory. Every nonempty read goes through the same
fixed HTTPS origin and bearer adapter, including seeks. Full 200 fallbacks,
redirects, encoded/malformed/mismatched ranges and changing lengths are rejected.
The reader stores no chunk cache and gives the native player neither URLs nor
tokens. Codec support and internal playback buffering remain platform-dependent.

Playback prepares off the UI thread and starts only after Play. Audio focus loss
and headphone disconnection pause it; it does not resume automatically. Back
first restores controls when immersive display is active. Otherwise Back,
Close, navigation, logout, expiry and backgrounding close the reader, cancel its
calls and release the player/surface/audio focus. Failure retries reload detail
and permission first, never silently resume playback. There is no background
service, picture-in-picture, download, playlist or casting support.

401 clears the affected view and checks the session once. 403 remains closed.
429 blocks requests until `Retry-After` expires; retries are explicit. 503/offline
is shown as unavailable. Invitation acceptance is never automatically replayed
after an uncertain response; retry refreshes session membership instead.

Tests use synthetic inputs only. The no-listener JVM lane exercises state and
interceptor responses; the broader historical transport lane uses local TLS sockets. Emulator tests substitute a synthetic API in the test process
to check the UI components and separately check the unconfigured app. These tests
do not establish deployed authorization or a real successful sign-in. Test-only
own-View renders leave `FLAG_SECURE` enabled; ordinary screenshots remain blocked.

Video evidence: [RETURN.md](../../docs/evidence/android/video/RETURN.md).
Original-viewer evidence: [RETURN.md](../../docs/evidence/android/originals/RETURN.md).
Earlier connected implementation: [RESULT.md](../../docs/evidence/android/connected/RESULT.md).

The optional [actual-backend interoperability suite](../integration/README.md)
exercises this same Kotlin adapter against the pinned Python application over
local TLS with temporary migrated SQLite. It is separate from ordinary JVM tests
and deployed-server/device acceptance.

## Interface refinement

The connected UI uses a warm neutral theme and a photo-led grid. Open **Settings**
from the header for app language and sign-out; **Libraries** returns to membership
selection. Gallery tiles crop previews, while detail previews fit the complete
image. A gallery tap opens the permitted photo viewer or prepares the video player;
Play still starts audio explicitly. Each tile also has a Details action for captions.
If original access is denied, the tap opens details without fetching original bytes.
Phone video controls show minutes/hours and seeking/buffering feedback, including
fullscreen. A stalled seek exits through the existing failure lifecycle after 30 seconds.
At large text sizes the gallery uses one column and action groups wrap.

The UI remains a development build with memory-only sign-in. Dates are source text,
captions stay literal, and unavailable previews never fall back to original files.
See [UI evidence](../../docs/evidence/android/ui/RETURN.md) for the independently recorded UI check.

The [phone/TV parity record](../PHONE_TV_PARITY.md) describes the media continuation,
its privacy behavior, and the remaining protected discovery and prepared-media
contracts. Discovery is not enabled by copying the TV's anonymous API adapter.
