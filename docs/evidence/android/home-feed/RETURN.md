# PH-ANDROID-HOME-FEED-01 — local integration complete

The TV app now consumes the frozen anonymous selected home feed automatically,
without personal sign-in or device approval. This supersedes the historical TV
prototype's pending-backend statements. No serving origin is approved or configured,
and no live home feed or projector acceptance is claimed.

## Source identity and scope

- Branch/worktree: `codex/android-tv-foundation`, `mobileAppForPhotoHouse-android-tv`.
- Observed clean base: `8ca5f20e95aec9fb8b789f3443c3a074f2de19cf`.
- Android source commit: `e8ab9be3c5497778783edf1b4a009707922248d1`.
- Backend implementation: `e6b2827842b2c0b5223c85208299b60e8a1257f6`.
- Backend readiness: `90c2e2461f480c1ae49daab4ac83c629b44a2148`.
- Contract SHA-256: `70328a653ddaa559bad6a4d654cf9870c89c5217e8e9e6c46a3501e9dd9e7548`.
- Independent phone pin unchanged: `87a60b475b37b1d6873cd977bcb6e7254472da7e`.

Writes stay in Android-owned code/configuration and Android evidence. No backend,
shared phone contract/fixture, iOS or other worktree implementation was changed.
The current executor remained the owner; no model switch or subordinate task.

## Implemented behavior

Separate `home-core` DTO/parser, HTTPS transport and lifecycle store. Configured
cold start/foreground fetch directly; visible metadata refreshes every 60 seconds.
No account/session object, cookie, bearer, original, Range or personal API fallback.
Strict UTF-8/JSON/duplicate-key/field/pagination validation; scoped revision-bound
preview URLs; compressed-size/hash/JPEG framing/dimension checks before display.

Prepared display images load automatically. The 3840x2160 fixture decodes at full
resolution; decoder rejects malformed/oversized input and confines drawing to the
image bounds. Remote grid/detail/fullscreen/captions and eight-second current-page
slideshow are wired. Back restores tile focus. EN/ZH and large text remain usable.

Background, disconnect, denial and connection errors clear content and stop the
slideshow. Connection generations and separate detail generations discard late
responses, including deliberately uncancellable test responses. Retry follows
2/5/15/30/60 seconds with jitter, honors longer Retry-After floors across reconnect,
and avoids tight revision-conflict loops. 403 requires explicit retry; 409 clears
and refetches; 404 stays a variant placeholder; 503 retries after clearing. Missing
display images stop slideshow advancement. No persistent photos or credentials.

## Validation performed

| Evidence tier | Result |
| --- | --- |
| Frozen inputs | All 12 backend working-file and pinned-Git hashes match; Android contract/test fixtures match; phone frozen verifier passes 12 operations, 38 retained cases, eight file hashes |
| New JVM tests | 23 passed: six parser/JPEG/origin checks, seven real adapter over synthetic loopback TLS checks, ten lifecycle/retry/generation checks |
| Existing JVM regression | 87 passed in a fresh `--rerun-tasks` run: 22 fixture and 65 connected/transport/readiness/reader/TV-preview tests |
| Actual pinned backend | Ten fresh in-process ASGI checks and launcher config syntax passed using disposable synthetic publication; no listener, sockets, SQLite or real media |
| TV build | Debug APK and test APK built; final build/lint run 21 seconds, 78 tasks (18 executed, 60 up-to-date) |
| Lint | Zero errors; three existing warnings: OldTargetApi, VectorRaster banner and fixed landscape DiscouragedApi |
| Emulator | Eight tests passed at font 1.0 in 16.019 seconds and at font 2.0 in 16.249 seconds |
| Packaging | Correct package/version/signature; empty origin; no phone/protocol classes or synthetic feed assets in main APK |

JVM suite details: [jvm-tests.json](jvm-tests.json). Backend replay:
[backend-asgi.json](backend-asgi.json); its checked-in Android replay harness verifies
Git source hashes and uses the backend owner's exact synthetic smoke operations.
This proves actual backend behavior separately from Kotlin mock-TLS transport; it
is not a served LAN end-to-end run. The backend owner's earlier 296-test result is
not included in this Android rerun count.

Toolchain: existing JDK 17, Gradle 8.10.2, AGP 8.5.2, Kotlin 1.9.24, Compose compiler
1.5.14, compile/target SDK 34, minimum API 26. Existing `Medium_Phone_API_36.0` ARM64
AVD at landscape 1920x1080/320dpi; no new SDK/image installation. Native key injection
checks focus/navigation. Own-view synthetic captures retain FLAG_SECURE. The
emulator was started only for this task and stopped after tests.

Visual review found image drawing beyond its viewport; explicit matching/clipping
fixed it and a containment assertion was added. Denied-state wording was corrected
and duplicate Retry removed. The updated artifact passed both font-scale runs.
Earlier implementation iterations also corrected a Kotlin mock DNS type error and
a delayed-response mock-server teardown timeout. No failed check remains unresolved.

## APK identity

`android/tv/build/outputs/apk/debug/tv-debug.apk`, 8,779,354 bytes.
Package `dev.photohouse.tv`, version code 2, version `0.2-home-feed-dev`.
SHA-256: `3fba5917cd0215f4696d4bdc8ae1e962e787409273c7ed92f574301aaec3c0d1`.

Debug signer SHA-256:
`56d7591b2b6c2538d506d1fe51327444f2307736cb12f5beefa08f1c410d6d28`.
Test APK: 1,583,430 bytes; SHA-256
`fd92248304065f340d5b9c42fa6496a1c4bfaca105fe39d2bf28dc7d42685f84`.

The unconfigured APK displays setup; tests inject synthetic APIs into the test
composition. It is not a real-home-ready installation artifact. Exact machine-
readable receipt: [result.json](result.json). APK files remain local build outputs.

## Remaining delivery gates and limits

Backend owns the separately authorized synthetic LAN deployment: operator/window,
verified interface/port, actual projector/home/guest peers, normal local DNS and
system-trusted HTTPS, isolation from public/proxy ingress, selection publication,
scoped lifecycle and rollback. The older protected origin is not a /home/v1 server.
No real selection or configured TV origin was used.

After that handoff, build a private configured APK and obtain the selected JMGO
firmware/device/install window. Verify launcher, physical remote, cold start,
sleep/wake, offline/reconnect, disable/removal and visible projection quality.
An exact 4K decode on a phone AVD does not prove a 4K Android surface on the JMGO.

The grid byte cache retains at most 16 MiB per page; overflow tiles use placeholders,
while any selected asset can load its display derivative. There is no automatic
cross-page slideshow, named-album/search endpoint, video, screensaver or offline
storage in this slice. Backend changes are observed on requests; admitted reads
may finish, and downloaded copies cannot be recalled.

No physical installation, deployment, host/network change, credential/real-media
access, push or merge occurred. Next smallest step is backend-owned synthetic LAN
serving readiness, followed by the explicitly authorized projector test.
