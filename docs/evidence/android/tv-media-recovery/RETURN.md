# TV v12 media recovery — 13 September 2026

Base `327272f0ed903d1f44932ccda026bc49ce714c7e`, branch
`codex/android-on-demand-media`. Backend producer remains
`5269257b4b2a791b59f9181aa83d8e70a5d03b4d`; no shared contract changes.

## Repair

Catalog v2/v3 recoverable media-read failures previously went through the global
failure handler, clearing selection and returning to the catalog after retry.
They now preserve the selected asset/page and present a retryable media error.
Denial, revision change and TLS failures retain fail-closed handling; v1 keeps its
existing foundation behavior. TV shows fixed, non-sensitive TV-READ error codes.
Unexpected native surface loss and receiver setup errors also produce diagnostics.

V3 preview reads are serialized, transient busy/unavailable responses retry twice
with Retry-After honored, and failed thumbnails remain explicitly retryable.
A failed video poster does not prevent opening its advertised video. Returning to
the grid resumes pending previews without discarding the current catalog page.
No streaming URL bypass or full-video download cache was introduced.

TV versionCode 12 / `0.12-tv-media-recovery` is visible in the header. The configured
home installer preserves the existing private origin/LAN routing and signer;
configuration is embedded only in the local APK, not committed to this public repo.
The protected phone module and its credentials are unchanged by this TV repair.

## Checks

- 91 home-core JVM tests and 2 TV JVM tests: zero failures, errors or skips.
- API 36 emulator: four native player tests pass (play/frame/seek, background,
  source replacement, malformed data). Two new recovery UI tests pass, including
  native range-read failure staying in the viewer and thumbnail retry without
  catalog reload. The first retry test used merged semantics incorrectly; final
  passing run locates the image in the unmerged tree and checks recovered bytes.
- Recovery UI checked in Chinese at font scale 2.0; inspected synthetic
  [screenshot](zh-font2.png). It is not a projector or real-media screenshot.
- Configured debug APK build and lint pass: zero lint errors, four existing warnings.
- Independent v3 producer/checksum verifier passes for all pinned source blobs.
- APK signature and version verified; exact bytes/hash in [verification.json](verification.json).

## Runtime findings and remaining gates

The Windows operator's administrative cache checks had missed a service-account
ACL problem. Two reported photos failed under the actual Limited account until
the private cache ACL explicitly included that account. Cold previews also hit the
unchanged 4 GiB free-memory guard while an unrelated Windows Terminal used about
50 GB RAM. After the user approved closing that terminal, fresh previews passed
under the Limited account. Backend runtime evidence is documented separately.

Video 28157's backend previews and partial MP4 ranges pass, including under the
service account. This does not establish the projector decoder root cause or prove
v12 fixes its playback. Install v12 on the JMGO, verify the header, retest photos
28206/28205 and video 28157, and capture the full TV code if video still fails.
Later-page navigation and physical remote controls remain device acceptance gates.

Only TV was packaged here; this does not claim new phone UI parity. Full-library
video preparation, protected phone activation and v3 discovery remain separate
outstanding work. No push, merge, caption restart or real-phone installation.
