# Home discovery with current media

Independent additive pin for discovery/v2 and v3 media results. Existing discovery/v1, catalog and on-demand contract packs remain frozen. This pack does not enable any server or APK by default.

Run `python3 android/verify-discovery-delivery-contract.py --backend-root PATH` against the pinned backend checkout to verify committed and working producer bytes. `DiscoveryDeliveryTest` replays the exact synthetic producer examples through loopback TLS, including photo originals and video capabilities. See [contract](CONTRACT.md) and [return](../../docs/evidence/android/home-search-v16/RETURN.md).
