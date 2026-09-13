# Protected phone discovery — local implementation return

Base `6b794c835339a63a3b6f0bc70f32a3de1bb03b3e`; branch
`codex/android-phone-discovery-v1`. Implementation commit
`4fdec0cfe47938b7dd2c00e0f188c6b7afc89355` retains the preceding phone streaming
and shared app artwork. Local source only; no push or merge.

## Implemented behavior

A separate opt-in protected discovery contract/adapter and warm EN/ZH touch editor:
server-reviewed pinned people and aliases; paged people, tags and coarse places;
Any/All people/tag selections; recorded date bounds, literal caption phrase and
media-type filters. Selections survive facet paging/category changes. Apply is
explicit, including a convenient action next to the family shortcuts. Search
results reuse paged browsing/detail/photo/video controls, and Back returns through
the same bound query and result page. Fresh detail and each byte request retain
existing original-file authorization. No prepared-media access is invented.

Strict bounded JSON rejects duplicate keys, malformed UTF-8/surrogates, oversized or
unexpected shapes, wrong library/binding/fingerprint, invalid scoped thumbnails and
out-of-range 64-bit decimal IDs. POST has its own 20 KiB limit; login keeps 2 KiB.
System TLS, native bearer, same-origin requests, no cookies/redirect/cache and
per-request cancellation remain in force. Default builds disable discovery.
Scope changes, logout, expiry, background, denial and stale results clear private
query/facet/result/navigation state. A 409 requires new options and explicit Apply;
429 cooldown never automatically replays a query.

## Contract and source review

Backend candidate `af8e0c8cf749f6e963dd8b196dce9aa842240387`: independently checked
33 committed source blobs and replayed 18 HTTP tests plus all 14 actual ASGI producer
examples/schema checks from an extracted committed snapshot. The prior trailing
newline regex mismatch is corrected. The retained Starlette/httpx deprecation warning
is test-environment evidence, not a changed runtime dependency.

The four-file Android import has its own manifest and verifier. Existing protected
phone and anonymous home contracts/pins are unchanged. The candidate intentionally
changes the shared backend inventory, so this does not claim all 19 old home-discovery
source hashes match the new candidate. No live index or protected server mount is
established by this import.

## Validation

Full offline Android verification passed: 98 phone JVM tests (18 new discovery tests),
85 home-core tests, 22 fixture tests and 2 TV tests; no failures/errors/skips. All
three debug APK builds and lint passed with zero errors. Existing SDK, Exif/API and
full-color-icon warnings remain. Shared/frozen contract and platform boundary checks
passed. Staged source secret scanning reported no findings. Phone APK signature
verification passed; no fixture assets are bundled. Detailed artifact identities and
exact final emulator results are in validation.json and retained logs/screenshots.
Final API 36 emulator runs: all 17 tests at 1.0 text scale (158.367 seconds), and
all 17 at 2.0 (161.634 seconds). EN/ZH search renders at both scales were inspected.

The emulator uses only synthetic component adapters and its own View rendering;
no FLAG_SECURE weakening, physical phone/projector installation, approved real HTTPS
origin, credential or live backend access occurred. Video/playback regressions are
included in the component suite. These tests do not establish live family acceptance.

## Delivery and remaining work

Phone versionCode 4, versionName `0.5-phone-discovery-dev`, package
`dev.photohouse.connected`, debug signed. Default origin is empty and discovery false.
A configured candidate build can opt in with `-PphotohousePhoneDiscoveryEnabled=true`,
after the protected server/provider/audience are reviewed. No server deployment,
service restart, push, merge or persistent media cache was introduced.

Remaining: trusted production-size discovery index/provenance/provider and protected
HTTP deployment; authenticated prepared-media capability; real phone/projector tests;
themes/topics and GPS-radius search; later multi-quality/4K playback.

Backend preparation status is separate: backend owner recorded source/evidence
`3674c555df6e5cab50be8640821126c6bcab54f0` at 17:07 +08. Largest 13-minute 8K case
passed normalization/validation. Qualification stopped at 16:58 under the unchanged
4 GiB available-RAM guard during the next long video. Five results are ready;
one persisted working entry is interrupted, not an active encoder. Bulk has not
started. Captions/services/publications were preserved. Sustained memory headroom,
remaining qualification cases and storage/time review are required before bulk.
No preparation restart or caption pause was performed by this Android task.
