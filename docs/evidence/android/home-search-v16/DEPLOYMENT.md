# Home search deployment follow-through

The independently pinned backend is now
`590cc52ceeb7cdef1d2cae0bbc6aa36c16d75cd6`. It includes current default-branch
readiness browsing, an explicit discovery/v2 launcher, bounded request-body
admission, metadata-only export and reviewed route inventory. The independent
Android pin and all 43 producer checksums moved together; older frozen packs remain
unchanged. Actual synthetic examples still match the shared adapter tests.

Backend validation: 532 security tests green with four platform-specific skips.
Native Windows export/qualification passed as the actual Limited service account
within the 2 GiB process-tree cap and 8 GiB available-memory floor. Peak job memory
was 417,648,640 bytes. Export took 9.672 seconds; the slowest measured metadata
request took 2.297 seconds. Tests covered search pages, dates/caption/media filters,
existing readiness browse, cold/cache-hit photos, original Range, four short/long
videos, changed revision, peer denial and current selected scope.

The existing Home service was switched on 15 September 2026 with rollback retained.
The original catalog/source index, media files, TLS settings, device audience and
memory cap were preserved. Captioning and encoding were not restarted or modified.
Trusted HTTPS from the operator remained denied, as expected. Actual allowed-peer
HTTPS and physical phone/projector behavior remain device acceptance gates.

The immutable search snapshot covers all 27,842 catalog assets: 21,871 have an
unambiguous eligible caption and 20,389 a recorded date. There are 139 ambiguous
caption selections and 5,832 without an eligible caption. People and regions remain
disabled pending reviewed mappings. The selected 6,213-tag roster exceeds the
current 5,000 limit; the whole tag capability stays disabled instead of truncating
results. Caption/date/media search is available. Later caption changes require a
new snapshot, and every future media publication requires a matching metadata pin.

Configured debug APKs were built successfully and signed with the existing debug
certificate; no private configuration was committed:

| Artifact | SHA-256 |
| --- | --- |
| PhotoHouse-TV-v16-home.apk1 | `5c6b96d9c1ce4b8bd554db9f42eaab9f20be466823514dc40e58461d5db8274a` |
| PhotoHouse-Phone-v7-home.apk1 | `1af77c9adf973992dc2dddd25d2d669d250799b582c9f7b1f2fec8bafba3dc83` |

Phone Home uses the same explicit LAN access policy. This Home test APK has no WAN
sign-in origin configured. The source still keeps account and Home transport separate.
Neither APK is a production/store-signed release or proof of installation.

Both configured APKs were delivered to the existing SMB APK folder at 10:46 on
15 September. Operator SMB readback matched both SHA-256 values; the existing
reader group retained ReadAndExecute permissions. TV v15 was preserved for rollback.
No physical-device installation was performed. Copy the apk1 file locally and
rename it to apk if the installer does not recognize the transport suffix.

Next product gaps: scalable tag discovery beyond the current roster cap; reviewed
family-person shortcuts and assignments; measured location coverage; metadata
refresh bound to each later media publication. Test the deployed caption/date/media
search and longer-video playback on both devices before claiming family acceptance.
