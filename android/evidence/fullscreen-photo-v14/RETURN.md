# Phone v14: full-window photos and transient photo-read recovery

Base: `7ec6843ba90ce440da3fc0fbaaaa6b1cf9eb8126`.
Branch/worktree: `codex/phone-prepared-video-v13`, existing owner checkout.

Photos open in an immersive window. Fit, fill, width, height, actual-size, pinch,
double-tap and pan measure against the full window, including when controls are
visible. Controls overlay the image instead of occupying a separate layout panel.
Tap the image to toggle the panel; previous/next remain available in the immersive
view. The shared viewer covers protected and Home phone modes. Fit preserves the
whole image with aspect-ratio bars; Fill crops edges. Preview quality is unchanged.

Photo transitions get one cancellable retry after 350 ms for offline transport or
HTTP 502/503/504 without a positive server delay. Every retried read reauthorizes;
there is no trust override, persistent image cache or original fallback. Explicit
Retry retains the requested photo/viewer/quality rather than returning only to its
detail page. A stopped slideshow stays stopped. TLS, denial, invalid data and
rate limits do not trigger automatic recovery. Server Retry-After is honored;
429 still defaults to a five-second cooldown when its header is absent.
Authenticated transient errors no longer tell the user to register/sign in.

The client defects were reproduced synthetically. The precise cause of the
reported live read failure (network, HTTP failure or server contention) has not
been established. A running service and passing synthetic tests do not establish
family-device acceptance. Runtime operational observations belong in the private
handoff; no family media, accounts, routing or credentials are retained here.

## Validation

- Frozen contract verifier and candidate.13 verifier passed (70 captured cases).
- 159 live-core + 117 Home JVM tests passed, zero failures/errors/skips.
- APK/test APK assembly and connected lint passed using installed Gradle 8.10.2,
  JDK 17 and Android build tools 34.0.0, with offline dependency resolution.
- API 36 phone emulator, 1080x2400 at density 420, font scale 1.0:
  eight affected UI journeys passed across the initial run and focused rerun.
  The new tap assertion initially ran before the double-tap timeout; the test now
  waits for that gesture window. Its final run passed. Seven other normal-scale
  journeys passed unchanged, including two failed reads followed by explicit
  Retry into the same full-screen photo, bilingual modes, Home mode and privacy.
- At font scale 2.0, the full-window bounds/overlay/navigation/tap check passed.
  The full-window capture had missing image pixels, so large-text rendering is
  not accepted from that bounds-only result. The longer bilingual preview test
  hit a screenshot redraw timeout. Subsequent
  attempts failed to attach the instrumentation process; cold-starting the
  emulator reached boot completion but ADB/app startup stayed unresponsive. The
  emulator started for this task was stopped. Large-text and tablet-size render
  acceptance remain unverified.
- Synthetic renders were inspected. No physical-tablet acceptance is claimed.
- Private Phone v14 APK and apk1: SHA-256
  `6cac791c8d3883afe2a61300d791875e9d9b8488bc725da03ce5489698efe608`.
  Version code 14 / `0.15-fullscreen-photos`, same debug signer as v13. Installed
  with replacement on the connected phone without clearing app data; the installed
  APK hash matches. Launcher opened. Real-photo owner acceptance is pending.

The initial wrapper tried to retrieve its distribution and timed out; verification
used the already installed matching Gradle distribution. Interim compile/test
failures were repaired before the final successful build/JVM run. Emulator
failures are retained separately rather than counted as passing checks.

## Delivery boundaries

No backend source/configuration, worker or service restart is part of this fix.
Protected prepared-video activation and high-quality photo-provider activation
remain separate pending work. The existing protected/anonymous mode boundaries
and candidate.13 contract remain unchanged. No push or merge in this slice.
