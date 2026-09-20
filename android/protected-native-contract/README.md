# Protected native v2 adoption snapshot

Android-owned, byte-for-byte copy of the backend candidate.12 pack. The frozen
shared `contracts/v1` stays unchanged.

- Manifest SHA-256: `f942db21ff6ce11f348ecaf4698dbeaab953e74107e72e3445c0ce175c2495d2`
- Backend source: `45f2123ad3447213aad68010154a6d14ff3613f9`
- Pack checkout: `0789cabea29c1faa4a67bf7ac9f9a9d12abcbf34`
- Contract: `2.0.0-candidate.12`, 61 synthetic captures
- Schema: `f2a6d8b4c915`

Phone v12 adopts the current required registration `name` and nullable session
`display_name`. A separate protected decoder preserves the strict frozen legacy
wire format. Name whitespace is collapsed and limited to 64 Unicode code points;
passwords remain unchanged. Registration still requires an owner-issued invitation,
with viewer membership only and no automatic original-media permission.

Existing login, library/gallery/detail/caption reads, preview viewing and read-only
Family Stories remain in scope. This adoption does not implement the newer people,
tags, duplicates, caption writes, album or upload client features. Those routes
are not covered by the 61 captures and require their own client tests.

The protected native profile and photo delivery remain off by default. The private
configured APK enables the protected profile but leaves display delivery and
protected discovery off, retaining existing Home-mode configuration. Preview zoom
uses delivered preview pixels; original quality requires an explicit grant and action.
Protected prepared-video streaming without an original grant is a backend gap.

Run `python3 android/verify-protected-native-contract.py`. With `--backend-root`,
the verifier also checks the pinned checkout identity and all 109 source and seven
payload hashes. The Android HTTPS tests replay captured registration and sessions;
backend ASGI replay is separate from real authenticated/device acceptance.
