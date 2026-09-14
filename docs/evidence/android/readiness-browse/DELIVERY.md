# Configured TV v13 APK staged for later installation

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

Artifact: `PhotoHouse-TV-v13-readiness-pending-server.apk1`

- Size: 10,449,814 bytes.
- SHA-256: `ebfc3a58013bd83a762ea34eeb2ad6cfb0768a76b04b8dffb754c6c244c89d8f`.
- Existing share/folder: `ProjectorMedia/APKs`.
- SMB delivery receipt: 15:26 +08; hash matched after reading back through SMB
  under the operator identity; existing reader-group read/execute ACL verified.
- All three previous v10/v11/v12 APKs retained byte-for-byte.
- Beside the APK: `README-v13-pending-server.txt` and
  `SHA256SUMS-v13-pending-server.txt`.

**Keep using v12 until the matching backend update is confirmed.** The running
service still referenced its older source during this delivery check. This task
did not deploy or restart it. V13 readiness requests need the new server contract;
staging this file does not make those requests work on the old server.

After the server update, install/update the staged file on the projector. If the
file manager does not recognize `.apk1`, copy it to local Downloads and rename the
local copy to `.apk`. Do not uninstall the existing app first. No physical install
or projector playback result is claimed. The phone-readiness gap remains open.

The unconfigured APK/hash in [the implementation evidence](RETURN.md) describes
the initial synthetic validation artifact, preserved privately before this rebuild.
The configured artifact above has its own hash and delivery evidence. Source tests
were not rerun for routing-only build arguments; the configured build/lint and
artifact identity checks were run. Runtime, publication and captioning were untouched.
