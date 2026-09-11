# Connected Android development app

`dev.photohouse.connected` is a separate debug-only application. It implements the
frozen native contract through OkHttp 4.12.0: phone/password login, invited
registration, own-session and library selection, invitation acceptance, paged
gallery, authenticated thumbnails, asset details, literal captions and logout.
It supports English and Simplified Chinese interface text, page-preserving photo
navigation, and an explicit original-photo viewer with zoom/pan. UI polish is deferred.

It is not the complete PhotoHouse product. Video playback, file downloads, uploads,
search, albums, voice, owner administration and persistent sign-in are absent.
Real backend deployment and physical-device acceptance have not been verified.

## Configuration

The default build has an empty origin, displays “Server setup needed”, and cannot
collect credentials or make requests. After the operator identifies a reviewed
HTTPS deployment of the frozen backend, place `photohouseOrigin` in the ignored
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
constructor is used only by JVM tests with runtime-generated certificates on
loopback. No trust overrides or test certificate are packaged into the app.

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
image details with `originals_allowed=true`, and only an explicit user action
requests `/assets/{id}/media?library=…`. The server still authorizes every request.
Closing, changing views, backgrounding, logout and expiry cancel/clear original
state; late bytes cannot reopen the viewer. No download/export or original-media
fallback is performed. Original responses require HTTP 200 and JPEG, PNG or WebP
content type, with a 12 MiB compressed-byte cap, including unknown-length bodies.
The display decoder bounds dimensions, samples to at most four million pixels and
applies EXIF orientation. Larger photos may be displayed at reduced resolution;
unsupported or corrupt images show an unavailable state. Decoding is serialized
off the UI thread. Pinch/pan, double-tap, zoom buttons and fit-to-screen controls
operate on the in-memory bitmap, with no URI or file handed to another app.

401 clears the affected view and checks the session once. 403 remains closed.
429 blocks requests until `Retry-After` expires; retries are explicit. 503/offline
is shown as unavailable. Invitation acceptance is never automatically replayed
after an uncertain response; retry refreshes session membership instead.

Tests use synthetic inputs only. JVM tests exercise real local TLS sockets and
state transitions. Emulator tests substitute a synthetic API in the test process
to check the UI components and separately check the unconfigured app. These tests
do not establish deployed authorization or a real successful sign-in. Test-only
own-View renders leave `FLAG_SECURE` enabled; ordinary screenshots remain blocked.

Original-viewer evidence: [RETURN.md](../../docs/evidence/android/originals/RETURN.md).
Earlier connected implementation: [RESULT.md](../../docs/evidence/android/connected/RESULT.md).

The optional [actual-backend interoperability suite](../integration/README.md)
exercises this same Kotlin adapter against the pinned Python application over
local TLS with temporary migrated SQLite. It is separate from ordinary JVM tests
and deployed-server/device acceptance.
