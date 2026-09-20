# Protected native v2 adoption snapshot

Android-owned exact copy of candidate.14. Frozen contracts/v1 stays unchanged.

Backend source: `1ee1af8d6efb1fac0546bf9013ad4e044aa554e5`.
Pack commit: `4d1de0f06bae4f2e5342134db6a4b847657d2484`.
Contract: `2.0.0-candidate.14`, 78 cases; schema `f2a6d8b4c915`.
See manifest.json and android/verify-protected-native-contract.py for exact hashes.
The verifier checks all 116 source and seven payload hashes against the backend.
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

Phone v15 adopts the additive server-paged media query under
`photohousePhoneMediaFilterEnabled` (default false; requires protected native v2).
Enable only with candidate14 or a compatible qualified backend. The text story
editor uses the existing POST/PUT revision and mutation-ID contract, with explicit
review/confirmation and manual exact-body retry. Story write access comes from
server `can_create`/`can_edit`, never from local role inference. Keystore sign-in
retains the existing 24-hour token only; no refresh route or lifetime extension.
