# Phone progressive video v18

Base: `f441ec2fa069f11308e38dc3ec53e73338992d88` on
`codex/phone-prepared-video-v13`. Version `0.19-progressive-video`, code 18.

The previous native MediaPlayer path issued tiny, interleaved decoder reads as
individual protected HTTP ranges. Hardware AVC decoding was already selected;
codec absence was not the observed problem. A one-use read-ahead alone did not
resolve the measured live failure, so the phone now uses Media3 progressive
extraction over its existing scoped range reader.

Changes: one-use 256 KiB protected prepared read-ahead; Media3 custom sequential
source with an inert URI; 12 MiB compressed-buffer target, 8–12 seconds ahead,
1.5-second start and 3-second rebuffer thresholds; no back buffer or disk cache.
Network authentication, exact Range/ETag validation, expiry and cancellation remain
in the existing transport. A prepared range GET can retry once after OFFLINE only; cancellation, expiry,
TLS, HTTP denial/rate limits and validation failures remain terminal. No automatic
player restart, credential/URL exposure,
original fallback, backend deployment or TV-player migration.

Validation: 199 live-core and 118 home-core tests, configured debug build and
Android lint passed.
Nine physical Samsung synthetic instrumentation tests passed in 22.855 seconds:
source cursors/window/EOF/failure, prepared playback without original permission,
seeks beyond one minute, English/Chinese play-pause/audio-focus/background closure,
corrupt media, fit/fill/fullscreen and Home playback/retry. Synthetic decoded-frame
and UI captures were inspected. Private artifacts are retained outside this public
repository; no real images, endpoints, credentials or device identifiers are here.

Earlier checks caught the extractor's standard ICY hint being incorrectly rejected;
it is now ignored and never forwarded. The emulator run subsequently hit a
codec/surface timeout and is not claimed as passing evidence. The physical suite
requires English for the test-only package; this does not change family-app or
device language. The configured APK and synthetic QA APK are separate artifacts.

Configured APK SHA-256:
`e4f39ff7026d30ce20927aa1faca4a32fade73f580aa96857c716b3fb7cb8b7d`.

Live server playback/throughput and family acceptance are separate from these
synthetic checks. The prepared delivery profile still needs phone/WAN bitrate
qualification; memory buffering cannot compensate for insufficient sustained
bandwidth. No persistent offline cache or adaptive renditions are claimed.

Live diagnosis: the progressive reader delivered approximately 12.3 MB in 6.7
seconds, versus about 1 MB in 22 seconds for the earlier native demux path. A
subsequent refill failed as OFFLINE (no HTTP status), matching the server's
five-second idle keep-alive plus disabled transport retry. The exact connection
race is inferred; no packet capture proof is claimed. A single identical prepared
range retry addresses that recoverable case without retrying writes or denials.

A real seek exposed a second lifecycle issue: cancelling Media3's blocking loader
interrupted runBlocking and closed the entire reader. Both protected and Home
readers now cancel just that fetch and let a new seek read proceed; owner close
still cancels all work. New deterministic blocked-fetch interruption tests passed.
The final refill threshold is eight seconds (same 12 MiB target) to leave time for
a reconnect instead of waiting for the buffer to nearly drain.

Final device qualification includes the real protected prepared MP4: hardware AVC
decoder selected; timed position advancement; buffer refills after an idle period;
forward and backward seeks without closing the viewer. These checks use the
configured artifact above, not the unconfigured synthetic test package. Gallery
initial-load errors remain a separate known issue; this change does not silently
expand non-video request retries or claim all-library readiness.

Final real-file result: playback reached 76.182 seconds / complete after forward
and backward seeks, with no terminal read/player error in the captured diagnostic
window. Timed samples before seeking advanced 3.027 → 13.077 → 23.237 seconds
across two approximately ten-second wall-clock intervals. Total process PSS was
about 238 MiB in one late-playback sample; this is not a long-run leak test.
Configured APK installation and exact SHA-256 were verified on the phone.

The final nine-test physical synthetic suite was rerun after the interrupt and
refill fixes and passed. CI, TV APK/device requalification, low-bandwidth WAN
acceptance and subjective family viewing quality are not claimed.

Publication replay: the combined CI build, lint and 357 JVM tests passed, then
the APK permission gate correctly detected Media3's merged
`ACCESS_NETWORK_STATE` permission. The connected APK allowlist now includes that
normal connectivity-observation permission explicitly. It remains an exact
allowlist; the fixture and TV permission gates are unchanged. This verification
script correction does not change the installed v18 artifact. The subsequent
GitHub run is the authority for final CI completion.
