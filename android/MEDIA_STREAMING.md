# Long-video playback and cache policy

The phone uses Media3 progressive extraction over its authorized range reader;
the TV retains its native random-access player. Each network
fetch uses an authenticated HTTP byte range (at most 256 KiB). Protected prepared
phone playback may retain the unconsumed suffix of one such response for contiguous
forward decoder reads; consumed bytes are zeroed immediately. A repeated or
noncontiguous reader request discards the suffix and performs a fresh authenticated
request. The player may seek inside samples already admitted to its own buffer. Closing,
expiry, backgrounding, logout and scope changes clear it. The original-video path
retains its existing per-decoder-read network behavior. The player also
controls its own buffering. Video duration alone does not require a complete download.
The phone accepts up to 32 GiB over its existing protected original endpoint;
the TV uses the frozen prepared-video catalog and revision-bound Range adapter.
Neither application persists media bytes or hands a bearer/URL to the decoder.
A 32 GiB file limit is an acceptance bound, not an allocation or codec guarantee.

## Recommended delivery

Prepare compatible display images and fast-start H.264/AAC SDR videos once on the
server, retaining originals. Stream the prepared version as playback starts and
fetch another bounded range for a seek. This avoids requiring a full phone/TV
copy of a multi-gigabyte camera original. Source formats, HDR and problematic
files require explicit backend dispositions; do not silently truncate videos.
The server profile's former 60-second limit and its new 900-second candidate are
preparation policies, not an Android duration restriction.

The first implementation retains memory-only buffering and per-request access
checks. A protected viewer without original permission still needs a reviewed
prepared-derivative endpoint. It cannot borrow anonymous home-feed access or gain
original permission just to play a video. The provider's output and native target
codec support must both be verified for real files.

## Optional bounded disk cache — proposal, not enabled

An on-the-fly disk cache can improve repeated viewing and tolerate short network
interruptions. Downloading an entire file before Play should be an explicit
future offline-download feature, not a prerequisite for normal playback.
Android Media3 documents byte caching with a maximum size and LRU eviction:
[network stacks and disk caching](https://developer.android.com/media/media3/exoplayer/network-stacks).
The phone now uses Media3 without its disk-cache or HTTP data sources;
adding persistent caching remains a separate transport/lifecycle change.

If implemented, use application-private storage excluded from backups; determine
a cap from device free space, evict least-recently-used data and expose Clear cache.
Bind entries to origin, account/library/session generation for phone, and to exact
publication revision, asset, variant and verified content identity for TV. Never
store credentials in filenames or reuse content across scopes. Cache lookup must
not bypass fresh access checks: server `no-store` and current every-read phone
reauthorization policy need an explicit reviewed cache capability before any
persistent hit can be served. Closing/backgrounding/logout/library changes and
revocation must cancel readers and remove/invalidate private data. Shared TV
publication replacement/disable also needs freshness checks before cached bytes
are reused. Retained bytes cannot be remotely recalled from an offline device.

HLS/adaptive bitrate is a possible later step for unreliable links and multiple
quality levels. Existing progressive MP4 plus Range is the initial LAN path; do
not introduce a new streaming wire contract solely because a video exceeds one
minute. Measure startup delay, sustained playback, seek latency, memory/disk and
Wi-Fi throughput on the actual projector before choosing another transport.

## Prepared phone read-ahead v17

The short-lived suffix belongs to one reader, asset and session generation, is never
written to disk, and cannot satisfy a repeated read of consumed data. Network
Range authentication, exact range/length and ETag validation remain unchanged.
This is buffering of bytes already admitted by the current response, not a promise
that each native decoder callback creates a new server request. Server revocation
cannot recall already admitted bytes (also true of the native decoder's own buffer).
The next network fetch must authorize again, and local expiry/close blocks every
copy, including one from the remaining suffix. No anonymous or original fallback.

Debug APKs emit bounded numeric video-read timing counters only (calls, byte counts,
request sizes and elapsed time); they never include IDs, URLs, credentials or media.

## Progressive phone player v18

Media3 1.3.1 (compatible with the existing compile SDK) replaces the phone's native
MediaPlayer demux path. A custom sequential DataSource delegates all reads to the
existing protected or home reader; its only URI is an inert in-memory identifier.
No alternate network data source, credential, original fallback or disk cache is
provided to Media3. Each reopen/seek still uses the same scoped reader. A data-source
close releases the cursor, while viewer exit/background/logout closes the owner,
cancels pending HTTP and releases the player. Player retries are disabled: typed
transport/authorization errors remain owned by the reader and Store. Prepared
range reads can repeat the exact request once after a transport OFFLINE failure,
with a cancellable 100 ms delay and fresh deadline/lifetime checks. TLS, HTTP
responses (including denial/rate limits), invalid media and mutations never enter
this recovery path. HEAD and original-video behavior remain unchanged.

Load control targets 12 MiB of compressed samples, 8–12 seconds ahead, with 1.5
seconds for initial Play and 3 seconds after rebuffering. Byte thresholds take
priority; this is a loading target with sample/allocation granularity, not a hard
process-memory ceiling. Codec buffers, surfaces and decoded frames are additional.
There is no back buffer for replay and no full-file download requirement. Hardware
decoder selection remains platform-managed. Audio focus, explicit Play, fit/fill,
fullscreen and background closure are preserved. Debug counters report numeric
read/playback timing, buffer duration, dropped frames, codec name and sanitized
failure kind/status only.

The observed prepared profile is still high bitrate. Better extraction/buffering
does not make a slow uplink sufficient: a lower-bitrate phone rendition and later
adaptive delivery need separate backend qualification. Preserve high-quality LAN
renditions and originals. Playback acceptance requires measured advancement and
seeking on a physical target, separately from build and synthetic tests.

Media3's loader interrupt during a seek cancels only the current fetch. It does
not close the scoped protected/Home reader; the new cursor can fetch the sought
position. Viewer exit still cancels and closes the owner. This distinction is
covered by blocked-fetch thread-interruption tests, because instant synthetic
responses alone do not exercise a seek during network I/O.
