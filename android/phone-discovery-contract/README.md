# Opt-in protected phone discovery

Reviewed backend source: `af8e0c8cf749f6e963dd8b196dce9aa842240387`.
The independent Android review reproduced all 33 input hashes, 18 candidate HTTP tests,
and 14 producer examples including strict ID/hash newline regression cases. The
producer guide is preserved verbatim; its historical not-yet-Android-reviewed wording
is superseded for this opt-in integration by manifest.json and the Android return.

This is a separate candidate contract. It does not repin contracts/v1, home v1/v2 or
home discovery. Backend's shared route inventory changed in the candidate; Android
retains the older home source manifest and does not claim all 19 candidate hashes
still match it. No production/default app route or live index is established.

The connected APK defaults `photohousePhoneDiscoveryEnabled=false`. A reviewed
configured build may use `-PphotohousePhoneDiscoveryEnabled=true`; this does not deploy
a server or bypass native bearer, membership, TLS or original-file permissions.
The usual public verification lane forces it false even if callers pass true.

Implemented: reviewed pinned people/aliases, paged people/tags/coarse places, Any/All
people and tags, recorded date bounds, literal caption phrase, media types, explicit
Apply, result paging, and result-to-detail/photo/video navigation under existing
original permission. Current-page slideshow semantics remain unchanged.

No themes/topics/GPS-radius taxonomy, protected prepared-media descriptor, cache,
original grant, anonymous phone transport, account or private origin is added.
Missing metadata limits matches; pins are server records, never hard-coded name-to-ID
inference. All categories combine with AND; empty filters browse scoped assets.

Lifecycle: scope change/background/logout/expiry clear query, facet labels, result
pages, navigation and bytes. HTTP409 discards all dependent search state and requires
fresh options then explicit Apply. 429 cooldown never automatically replays a query.
Search opens media only through fresh protected detail/byte authorization; Back
fetches the same search page with its prior binding and fingerprint.

Checks: `python3 android/verify-phone-discovery-contract.py`; optional
`--backend-root` checks exact Git blobs without changing or executing that checkout.
JVM tests consume examples.json only in the test source set. APKs bundle no fixtures.
