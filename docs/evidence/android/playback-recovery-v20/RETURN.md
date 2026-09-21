# Phone and TV playback recovery v20

Branch `codex/android-playback-recovery-v20`, based on
`a7fe26af14485850538497962a2af02080448e32`. The v19 gallery recovery,
prepared filter and Media3 player work is preserved. No backend or wire-contract
change is needed for this slice; protected snapshot remains candidate15.

## Changed behavior

Both formats now use a shared monotonic 30-second deadline for continuous
preparation, seeking or active buffering. The watchdog runs on the main looper,
independent of blocked media reads. On timeout it closes the reader, requests player
release and emits one reason-specific error. Changing waiting phases cannot extend
the deadline. A normal paused or ended video has no active wait; a paused seek or
unfinished preparation remains bounded. Close cancels pending timeout work.

TV diagnostics now interpret the pinned Media3 codes rather than old MediaPlayer
negative codes. Phone and TV show fixed bilingual messages for unsupported format,
decoder/output failure, invalid media, read interruption and timeout. Phone Home
mode retains the diagnosis alongside the selected video; server errors take
precedence. No raw exception text, media URL or credential is displayed.

The protected phone retains the selected details after native failure. Recoverable
timeout/read errors offer manual Retry, which creates a fresh reader and repeats
prepared HEAD. Nothing reopens automatically. Transport denial, revision changes
and rate limits retain their existing classification and privacy behavior; stale
old-reader callbacks cannot overwrite a replacement viewer.

## Evidence

- 338 JVM tests pass: 212 protected phone, 121 Home, 5 TV; zero failures/skips.
- Nine focused phone emulator checks pass: blocked setup cancellation, exact-once
  error, close-before-timeout, paused idle vs blocked seek, pinned error mapping,
  EN/ZH native playback and error screens, prepared seeking, fullscreen and Home
  retry/background handling.
- All 39 TV synthetic emulator tests pass, including blocked preparation/seek,
  successful play/seek/replay, decoder errors, source replacement and background
  cleanup. Four live-catalog tests are explicitly excluded. Exact counts and
  artifact identities are in [validation.json](validation.json).
- Both application/test APKs build; both app lint checks pass. Private configured
  APKs build separately; debug signature and versionCode 20 are verified.
- Protected candidate15 snapshot and phone/TV boundary verifiers pass.
- Inspected synthetic [English](error-en.png) and [Chinese](error-zh.png) error
  screens. The fixture intentionally has no thumbnail. No family media is captured.
- New native tests use short injected deadlines. The actual attached TextureView
  blocked-seek cases distinguish intentional pause from a seek waiting on I/O.
  These tests do not measure real-network throughput or projector codecs.

Phone versionName `0.21-playback-recovery`; TV `0.20-playback-recovery`.
APKs and full build logs stay outside the public repository. Existing buffer
budgets, transport retries, permissions and memory-only media policy are unchanged.

## Remaining delivery gates

This slice does not install on physical devices, publish/merge source, restart a
service or change caption/video workers. The prepared filter in the configured
Phone v20 candidate APK still requires candidate15 activation. The prior staged
100-video index and optional 720p phone-profile qualification remain separate
backend delivery work. The TV APK uses the existing Home contract.

Timeout coverage applies to reported loading, seeking and buffering states. It
is not proof that every possible codec stall is detected or that playback is
smooth on the real phone/projector. Physical long-video playback, seeking and
interrupted-Wi-Fi acceptance remain necessary.
