# On-demand media candidate

Independent TV v3 and protected phone display extension. Existing v1, v2 and
discovery snapshots are unchanged. The backend commit and source checksums are
in manifest.json; CONTRACT.md describes the implemented policy and rollout gates.
The catalog example is emitted by the backend synthetic ASGI test, not a guessed
client response. No family files, addresses or credentials are included.

Enable the matching candidate with `photohouseTvCatalogVersion=3` and
`photohousePhonePhotoDeliveryEnabled=true`. Defaults retain prior protocols.
Origins remain unset in public builds. There is no automatic protocol downgrade,
anonymous phone fallback, original permission upgrade or persistent device cache.

Run `python3 android/verify-on-demand-contract.py` offline. Add `--backend-root`
to verify the pinned Git blobs and current producer source before staging.
