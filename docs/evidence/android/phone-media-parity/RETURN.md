# Phone media parity and TV test readiness

Android source commit: **`fd7a72ac400c604bf078d04f3fb62cbdee4be806`**.
Base: `838d392861a474ad8b9e2faf0994f35f26b085cd`.
Isolated branch: `codex/android-phone-media-parity`; worktree is the sibling
`mobileAppForPhotoHouse-android-phone-parity` checkout. The existing TV worktree,
artifacts, contracts and all other worktrees were preserved.

## Delivered phone behavior

- Photo fit/fill, bounded zoom/pan, fullscreen and next/previous inside the original
  viewer. Fill pans the full bitmap behind the viewport rather than a clipped
  rectangle. A synthetic pixel assertion checks that panning exposes no black gaps.
- Explicit 8-second photo slideshow within the current page, starting its interval
  after successful decoding. Each next photo uses fresh protected detail/permission
  checks. It stops at page end, video, denied originals, decode/read failure, manual
  navigation, close or a privacy boundary. It neither skips denied media nor
  starts video audio or crosses pages.
- Native video fit/fill and fullscreen preserve the same reader/player and aspect
  ratio. Existing play/pause, seek and audio-focus behavior remain. Back first shows
  controls; ordinary Back then closes. Temporary screen-awake and system-bar state
  is restored on leaving the viewer.
- Numeric page selection with bounds and lifecycle clearing; scrollable controls
  for enlarged fonts; existing warm green/cream phone styling and EN/ZH text.

Original access remains explicit and permission-gated, with the existing 12 MiB
response/four-million-pixel display limits. Phone video still requires original
permission and the existing authenticated Range adapter. No anonymous TV route,
new phone endpoint, original grant, stored query, credential or media cache was
added. Search and prepared-media parity for non-original viewers remain separate.
See [capability record](../../../../android/PHONE_TV_PARITY.md).

## Observed validation

| Check | Result |
| --- | --- |
| Live-core JVM | **76 passed**, zero failures/errors/skips; includes six slideshow/navigation cases and five geometry cases |
| Portrait, normal font | **13 passed**, 148.159 seconds |
| Portrait, 2× font | **13 passed**, 132.487 seconds |
| Landscape, 2× font | **3 media tests passed**, 56.315 seconds |
| Debug APK, test APK and lint | Passed on installed JDK 17 / Gradle 8.10.2 / AGP 8.5.2 / SDK 34 tooling |
| Four contract/boundary verifiers | Passed; protected phone, v1 home, v2 catalog and discovery pins unchanged |
| APK isolation | Empty configured origin asserted by emulator test; no application `assets/` entries; debug signer verified |
| Physical phone/TV or deployed service | Not exercised in this slice |

The owned API 36 `Medium_Phone_API_36.0` emulator used 1080×2400 portrait and
2400×1080 landscape at density 420, with four runtime CPU cores and 3 GiB memory.
The emulator tests use an injected synthetic API and generated photo/test-only video;
they are component evidence, not real authentication or a deployed backend test.
The existing test script was updated to require all 13 tests and collect renders.
The landscape replay explicitly selects three media tests and restores prior
rotation/font settings. Test packages were removed and the owned emulator stopped.
No saved AVD configuration was edited and no physical device was changed.

The JVM HTTPS-adapter tests use disposable synthetic TLS servers bound only to
loopback. They are not deployed-service evidence. The separate extracted backend
internal replay guards against external network/process/file access.

54 synthetic images/frames are retained and hashed in [UI receipt](ui-tests.json).
The integration owner inspected normal photo/video UI, Chinese 2× photo controls,
the separately captured decoded video frame, and 2× landscape media controls.
Own-View renders leave `FLAG_SECURE` enabled. Software window draws may omit the
hardware video layer; separate native TextureView frames are captured and checked
for colored pixels and aspect ratio. Black software video areas are not claimed
to be decoded frames. Large control panels scroll while retaining a media viewport.

The first slideshow UI run exposed a real state-snapshot race: a newly completed
decoder flag could be combined with the prior null bitmap and stop the slideshow.
Capturing one decoder result per composition fixed it; focused and final suites
passed. The test also yields real worker time while advancing its virtual clock.
Earlier failed/focused and pre-pan run logs are retained separately from the final
normal/large logs. They are not counted as passing final runs.

Receipts: [JVM](jvm-tests.json), [build/lint](final-build.log),
[portrait](instrumentation-normal.log), [enlarged portrait](instrumentation-large.log),
[landscape](instrumentation-landscape-2.0.log).

## Exact phone artifact

Application `dev.photohouse.connected`, versionCode **2**, versionName
`0.3-phone-media-dev`, origin unset, debug only.

`PhotoHouse-Phone-media-v2-unconfigured-6cb1b192.apk` — **8,988,354 bytes**.

SHA-256: `6cb1b19296be11135f55b0bbfef15ba6151c52614434e362515290b6b1b7bb40`.

Debug signer SHA-256:
`56d7591b2b6c2538d506d1fe51327444f2307736cb12f5beefa08f1c410d6d28`.

An immutable copy is retained in the private phone-parity artifact directory;
the ordinary build output remains under `android/connected/build/outputs/apk/debug`.
[Artifact receipt](artifact.json). It displays server setup until an approved
protected HTTPS profile is configured. It is not a signed production release or
a physical-phone acceptance result. No configured TV APK was rebuilt.

## Protected discovery progress

The existing backend task produced proposal
`8d8e88974b8be515ad5a9ab088b91a94652b1e71` and service source
**`ddfb0fb893d96482eb1b4b1015328c66ec96077c`** in its own worktree, with evidence
commit `738863d3a87708b4dbf14d7ef779c08e6ce3c364`. Android remained
the only writer in this phone checkout. Current task/model settings were retained.

The service adds current library authorization before metadata access, reviewed
library people/aliases/pins, all six combined filters, scoped counts, lossless
64-bit string IDs, source/session/member/index binding, paging and explicit
resource/cancellation budgets. It is **unmounted internal service code**, with no
frozen HTTP contract and no trusted real index loader. Its Python replay results
are not an Android wire fixture. Original permissions are not expanded.

Android reviewed the new service and returned a source-projection defect:
SQL text truncation could turn oversized dates or NUL-containing values into
apparently valid metadata. The correction selects complete bounded values,
retains invalid controls for rejection, and prevents oversized caption candidates
from silently selecting a fallback. Those tests are included in the source pin.

Independent Android-side replay extracted **15 committed blobs**, verified every
hash and ran **25 focused tests + 10 internal service checks**, all passing.
See [input manifest](backend-inputs.json), [focused log](backend-focused-tests.log),
and [internal replay](backend-internal-replay.json). The backend owner separately
reports 158 selected regression tests, 162 unchanged inventory entries, and all
10 protected/19 home discovery frozen inputs unchanged; that broader regression
was not repeated by Android. This is a targeted review, not an exhaustive audit.

## TV test answer and remaining work

The existing configured `PhotoHouse-TV-home-v6.apk` is available for testing the
existing v1 home feed. Its hash was rechecked as
`ae8f6f194f4e9d0f72c51f146af68ecded4a940258f4087aabf27ae76aeef38a`.
Current server health/content was not queried. This profile has discovery off.
The discovery v6 APK has v2/discovery enabled but **no configured origin**. Neither
the phone media change nor internal search service makes full TV discovery live.

Next gates:

1. TV: reviewed real catalog/index and people mapping, measured prepared media,
   approved compatible served profile/configuration, then projector playback,
   remote, sleep/wake and quality acceptance. Preserve the working v1 rollback.
2. Phone search: reviewed protected HTTP/schema, authenticated gateway and touch
   Explore/results UI, actual producer/client replay and lifecycle tests.
3. Phone prepared media: reviewed display/video capability for viewers without
   original permission; no grant or anonymous bypass to emulate TV behavior.
4. Both: later theme/topic taxonomy, named-album and GPS/radius contracts. These
   features are not implemented by the current search model.

No real database/media, Windows runtime, credential, live-service deployment/restart,
physical installation, push or merge operation occurred.
