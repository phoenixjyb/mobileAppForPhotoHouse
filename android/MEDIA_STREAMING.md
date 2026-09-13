# Long-video playback and cache policy

The current applications play through native random-access readers. Each read
fetches a bounded HTTP byte range (at most 256 KiB); the native player controls
its own buffering. Video duration alone does not require a complete download.
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
The current native MediaDataSource implementation has no Media3 dependency;
adding a cache is a separate transport/lifecycle change.

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
