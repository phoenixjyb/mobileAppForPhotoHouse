# Protected native v2 adoption snapshot

This directory is an Android-owned, byte-for-byte snapshot of the backend
candidate pack adopted for compatibility review.

- Contract manifest SHA-256: `3c222c9525f89ff753353b17e7741e200d16061cd042df5afbd03618ed42ce3e`
- Backend source commit in the manifest: `4022a57f56e6b2f976931a20569e15c879871d93`
- Backend pack commit supplying this snapshot: `97c5d620b2eaf8bc5e50c48d3d8c5985bec8396d`
- Contract version: `2.0.0-candidate.1`
- Case pack: synthetic only, 60 cases.

The client profile remains explicitly disabled by default. In Android,
`photohouseProtectedNativeV2Enabled` maps the backend
`protected_native_v2` opt-in and bundles the current native auth admission
(login 1–128 code points, registration 8–128) with read-only Family Stories.
`photohousePhonePhotoDeliveryEnabled` separately maps
`protected_photo_display` and remains false for the configured live Phone v10
because production has no `PhotoCache`; thumbnails are the available photo
surface. There is no separate Android `protected_story_read` flag.

This snapshot's Android adoption is the read-only subset plus the invited
registration/session/logout baseline: native auth/session, library/detail/
caption reads, thumbnail-only photo evidence, and story listing. It contains
no owner management, story writes, history, or search feature claim. The
captured gallery DTO uses `page_size: 1`; that is compatibility evidence only,
because the adapter always requests `page_size=50` and the capture is not
altered. The broader backend pack contains 60 cases, including mutation,
history, deletion, negative authorization, rate-limit, and other cases that
remain backend evidence rather than Android feature claims.

Run `python3 android/verify-protected-native-contract.py` from the repository
root to verify the local snapshot. Pass `--backend-root` with the pinned backend
checkout to additionally verify its complete 98-source/7-payload hash closure
and commit identity.
