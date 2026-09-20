# Protected native v2 adoption snapshot

Android-owned exact copy of candidate.13. Frozen contracts/v1 stays unchanged.

Backend source: `1d9248e577947c4b8fea1a1551f11b2f78bc2781`.
Pack commit: `7af4498e6a55b4c17f7223e815d145e4a595ce0c`.
Contract: `2.0.0-candidate.13`, 70 cases; schema `f2a6d8b4c915`.
See manifest.json and android/verify-protected-native-contract.py for exact hashes.
The verifier checks all 115 source and seven payload hashes against the backend.
PREPARED_MEDIA.md is an exact copy of the additional backend-owned streaming contract,
verified against its source hash rather than altering the original pack documents.

Phone v13 retains v12 named registration/session compatibility and adds explicit
protected prepared-video playback. Its separate switch defaults false and requires
the protected profile. A configured candidate can use library-read playback without
original permission; the original action remains separate and permission-gated.
HEAD establishes size/strong ETag. Each bounded range authenticates and must match
that identity; changed bytes, denial or invalid responses close playback. There is
no automatic retry, original fallback, anonymous Home fallback or persistent cache.

This pack does not enable a live backend, discovery, uploads or high-quality photo
rendering. Those services still need separate runtime qualification. Emulator/local
TLS checks and APK packaging do not establish physical device acceptance.
