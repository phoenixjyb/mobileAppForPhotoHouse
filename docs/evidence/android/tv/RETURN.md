# PH-ANDROID-TV-01 — viewer built, automatic connection pending

The remote-controlled TV viewer is implemented and tested. **The requested
no-sign-in automatic home connection is not implemented by the current backend.**
The shipped prototype contains no personal sign-in/registration screen and stops
at setup; it does not open a real photo library merely because an origin is set.
The pending access-model choice and exact backend dependency are in the
[home-access request](../../../../android/tv/HOME_ACCESS_REQUEST.md).

## Source and artifact

- Isolated branch `codex/android-tv-foundation`, base
  `ae91426b54faab1d12d494152e3b5473c457e977`.
- App source commit: `f3a6dfea0377f0b2813ac4475cf5ef050aa4dfe8`.
- Package `dev.photohouse.tv`, version `0.1-tv-dev` / code 1; min API 26, target 34.
- Debug APK: `android/tv/build/outputs/apk/debug/tv-debug.apk`, 8,860,005 bytes.
- APK SHA-256: `6177439cf709a9721f9b70af5dc6787ca5b319e94c1db07a80c3699084c80717`.
- Debug signer SHA-256: `56d7591b2b6c2538d506d1fe51327444f2307736cb12f5beefa08f1c410d6d28`; v2 signature verified.
- Origin unset, no credentials or fixture assets packaged. Release variants disabled.
  No configured private build was made after the user's no-sign-in correction.
- This evidence commit follows the app source commit and changes no app inputs.
  [Source hashes](source-inputs.json), [result](result.json), [signature](signature.txt),
  [package](package.txt) and [permissions](permissions.txt) bind the artifact.

## Implemented scope

[TV plan/build guide](../../../../android/tv/README.md): landscape library and paged
grid, visible remote focus, OK/arrows/Back with selected-tile restoration,
full-screen image fit, eight-second current-page slideshow, EN/ZH, literal captions,
background clearing, session revocation handling and explicit disconnect. No TV
phone/password/invitation fields. Named albums, search, video playback, pairing,
automatic startup authentication and persistent device credentials remain deferred.

TV detail requests prefer cached size 1024. A 404 may fall back to a cached default
thumbnail; denial/TLS/rate/size/server failures do not fall back. Missing previews
never fetch originals or run providers. Explicit **Use originals** requires current
server permission and applies to subsequent image slides in that viewing session.
Transfer remains bounded to 12 MiB; serialized header-first/EXIF-aware decoding
preserves 3840x2160 pixels and caps larger output at 8,847,360 pixels. Grid bitmaps
have a separate 262,144-pixel cap. Existing staging original grants remain off.
LAN bandwidth is not a server grant or proof of native 4K projector output.

## Fresh evidence

- **87 JVM tests pass:** 22 fixture, 32 store, 19 synthetic local HTTPS adapter,
  five no-listener adapter, three video reader and six new TV preview checks.
  [Suite counts](jvm.json), [JVM/phone build log](jvm-phone-build.log).
- Phone and TV debug APK builds/lint pass. Final TV build: **21 seconds, 81 tasks,
  15 executed and 66 up-to-date**. [Final TV build](tv-build.log).
- TV lint: zero errors, four warnings: platform `ExifInterface`, pinned target API,
  vector banner size and intentional landscape orientation. Phone retains its two
  existing EXIF/target warnings. JDK 17.0.20.1, Gradle 8.10.2, AGP 8.5.2,
  Kotlin 1.9.24, existing Android build tools 34.0.0; offline builds only.
- **Seven emulator tests pass at each of 1.0 and 2.0 font scale**:
  16.001 and 48.367 seconds.
  [Normal](instrumentation-1.0.log), [large text](instrumentation-2.0.log).
  Tests cover actual injected remote keys, focus/back restoration, full-screen
  previous/next, no personal login UI, original opt-in and disconnect clearing,
  slideshow advancement/background stop, 4K decode, oversized/malformed input.
- Existing API 36 ARM64 phone AVD, task-owned read-only instance, landscape
  1920x1080 at density 320. This is component evidence, not a TV OS/JMGO test.
  Sixteen synthetic own-View renders retained under `screenshots/`; normal and
  large EN/ZH grid, full-screen and original renders inspected. No real media.
  The task-owned emulator was stopped afterward; no physical device was touched.
- Frozen contract verifier passes: 12 operations, 38 stored cases, eight hashes.
  This was checksum/fixture validation, not a fresh backend ASGI replay.
  Phone/fixture/TV source guards pass. Packaged system trust, disabled backup/
  cleartext, launcher entries, permissions and absence of assets verified.

Retained text logs normalize trailing whitespace; build logs replace the local
worktree path with a placeholder. Initial attempts are retained in `attempts/`. A real caption-dialog focus request
ran before dialog composition; it was moved into the dialog's confirm content.
A System UI ANR dialog in the disposable emulator intercepted remote keys; the
normal and hardware-graphics attempts record that failure. After selecting Wait,
remote tests passed. A later system-bars-hiding experiment again failed remote-key
checks and was removed. Final tests require the Activity to have window focus
before injecting keys. None of those failed attempts is counted as acceptance.
A render check also caught a capture made before original decoding completed;
final coverage waits for the displayed image, and the UI now shows decode progress.

## Remaining blockers and next action

1. Decide between once-approved projector access and a deliberately unauthenticated
   selected LAN feed. Both avoid personal sign-in, but require different backend
   policy/contracts. The Android-owned handoff defines the needed return; no shared
   contract, authentication bypass, embedded password or backend change was made.
2. Implement/replay/deploy the chosen synthetic TV contract, then wire automatic
   startup/reconnect and produce a private configured artifact. The existing API pin
   remains `87a60b475b37b1d6873cd977bcb6e7254472da7e`.
3. Confirm JMGO firmware/API/ABI, installation route/operator, LAN DNS/system trust,
   high-resolution derivative/original policy, then separately approve installation.
   Actual remote/keyboard, sleep/wake, projection quality and household acceptance
   remain unverified. No public IP or router forwarding is needed for LAN-only use.

No push, merge, real server/credential/media access, backend deployment or physical
projector/phone installation occurred. Existing worktrees and shared contracts are
preserved. The original phone app retains its existing authenticated behavior.
