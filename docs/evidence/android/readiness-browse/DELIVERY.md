# Configured TV v13 APK ready for installation

14 September 2026. Source implementation commit:
`bd638eb674fa4d2f896693b82019bd9637ba9546`.
Required backend candidate:
`a72aa320801787cd066c6e04c33764e9271c9ab5` or a separately verified compatible successor.

The owner authorized committing/pushing the work and staging an `.apk1` in the
existing projector SMB APK folder. The configured build contains the existing
private home routing, catalog v3 and readiness browsing enabled; discovery remains
off. Private routing values and the configured APK are excluded from Git.

Configured `:tv:assembleDebug :tv:lintDebug` passed with cached Gradle 8.10.2,
JDK 17 and SDK 34. Package is `dev.photohouse.tv`, version code 13,
version name `0.13-tv-readiness-browse`. Android signature verification passed;
debug certificate SHA-256:
`56d7591b2b6c2538d506d1fe51327444f2307736cb12f5beefa08f1c410d6d28`.
No signing key was changed or published.

Artifact: `PhotoHouse-TV-v13-home.apk1` (same bytes as the earlier pending-server file)

- Size: 10,449,814 bytes.
- SHA-256: `ebfc3a58013bd83a762ea34eeb2ad6cfb0768a76b04b8dffb754c6c244c89d8f`.
- Existing share/folder: `ProjectorMedia/APKs`.
- SMB delivery receipt: 15:26 +08; hash matched after reading back through SMB
  under the operator identity; existing reader-group read/execute ACL verified.
- All three previous v10/v11/v12 APKs retained byte-for-byte.
- At 15:51 +08, renamed to the ready filename after server qualification and
  activation. SMB readback and prior-APK preservation checks passed again.
- Beside the APK: `README-v13.txt` and `SHA256SUMS-v13.txt`; old pending notes
  were archived privately.

**The matching backend is now deployed; v13 is ready for your projector test.**
The existing TV task runs backend `a72aa320801787cd066c6e04c33764e9271c9ab5`,
with its original access settings and publication preserved. Native Limited-account
qualification passed 29 tests with one privilege-dependent skip; that case passed
separately under the operator identity. Real-config ASGI checks covered 279 catalog
pages, cold/cache-hit photos and both published video ranges. Live trusted TLS and
operator-peer denial passed. Positive allowed-peer HTTPS and projector decoding
remain untested here; the owner will install later.

Ready admission at rollout: 23,588 photos and 2 published videos. More batch-ready
videos require publication before they appear. The service has a 2 GiB committed
memory cap, with approximately 68 MB RSS and 52 GB system RAM free at the final
check. Captioning and preparation remained running independently.

Backend evidence: [TV v13 server qualification](https://github.com/phoenixjyb/vlmPhotoHouse/blob/codex/media-readiness-browse/docs/security/TV_V13_SERVER_20260914.md).

Install/update the staged file on the projector. If the
file manager does not recognize `.apk1`, copy it to local Downloads and rename the
local copy to `.apk`. Do not uninstall the existing app first. No physical install
or projector playback result is claimed. The phone-readiness gap remains open.

The unconfigured APK/hash in [the implementation evidence](RETURN.md) describes
the initial synthetic validation artifact, preserved privately before this rebuild.
The configured artifact above has its own hash and delivery evidence. Source tests
were not rerun for routing-only build arguments; the configured build/lint and
artifact identity checks were run. The initial APK staging did not change runtime.
The separately authorized server rollout above preserved publication and captioning.
