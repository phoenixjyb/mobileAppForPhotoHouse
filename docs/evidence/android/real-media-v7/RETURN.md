# TV v7 real-media delivery

Source commit: `ae975cb1e26db67bc5a3d1a0d1b5ce73673c7821`.
Base: `838d392861a474ad8b9e2faf0994f35f26b085cd`.
Branch: `codex/android-tv-real-media`; isolated worktree, earlier TV and phone work preserved.

Version 7 is a private configured catalog-v2 debug APK, with server origin and app-scoped LAN mapping embedded. It uses the existing debug signer, supports updating v6, and has discovery disabled. The configured v6 home build still targets the separate synthetic v1 service; install v7 for real photo/video testing.

APK private reference: `PhotoHouse-TV-real-media-v7.apk`.
SHA-256: `35da54aedba3bdd4e044b93f5da5164aa7ee86fac856b3446853407bccbec267`.
Size: 9,067,966 bytes. Signer SHA-256: `56d7591b2b6c2538d506d1fe51327444f2307736cb12f5beefa08f1c410d6d28`.
No application assets are packaged. Private connection values and operator receipts remain outside Git.

## Validation

- 82 home-core and 2 TV JVM tests passed without failures/errors/skips.
- Four frozen contract verifiers and TV source-boundary verifier passed.
- 12 independent pinned backend ASGI catalog checks passed; all 16 source inputs verified.
- Debug build and lint passed, zero errors and three retained warnings (OldTargetApi, VectorRaster, DiscouragedApi).
- One explicitly approved live production-adapter test passed (0.75 seconds; Gradle invocation 6 seconds). It checked normal TLS/hostname validation with app-scoped mapping, strict catalog parsing, both preview variants for one real photo and video, complete prepared video SHA-256 through bounded Range reads, EOF, backward seek, and repeated revision-pinned metadata. Media remained in memory and was not saved in reports or files.
- Backend owner returned 84 passing native Windows tests and a passing native synthetic preparation benchmark. Android reviewed the test-only portability delta and raw receipt; it did not independently rerun those Windows tests.

The [verification receipt](verification.json) separates the evidence tiers. Prior TV emulator/player tests are retained; no new emulator or physical-projector run is claimed for this version/configuration change. No ADB device was connected. The live adapter test verifies transport and integrity, not Android decoding or physical video playback.

## Published scope and remaining work

The backend reports a revision-1 catalog of 27,842 visible entries with 15 real photos and one prepared H.264/AAC video ready, all on page 1. This is a partial media-coverage publication, not full-library playback. The remaining 27,826 entries have unavailable media; the snapshot does not automatically follow later imports/hides or caption changes. Discovery/search is not enabled in this build.

One full-range MOV conversion was refused by existing output validation on Windows; its failed attempt is retained by the backend. Full coverage requires further bounded preparation and compatibility work for full-range, HDR/10-bit, long/oversized and high-resolution sources. No format validation was weakened.

The original files, database schema/selected metadata, existing v1/API/caption services and credentials were preserved. Caption records continued advancing during preparation. The new service is a separate limited manual task; durable reboot/logon startup and real-projector acceptance remain outstanding. No push or merge occurred.

## Final service verification

After the backend owner disabled/stopped/restored only the new v2 service, Android independently repeated the trusted HTTPS catalog request. It returned revision 1, the same 27,842-entry total, and exactly 15 ready photos plus one ready video on page 1. The video is tile 50. No metadata body was retained by this final check. The owner also verified denial while disabled, removed/recreated only the new scoped firewall rules during rollback, and confirmed unchanged existing v1/API/caption process identities. These lifecycle/denial checks were reviewed from the backend return, not rerun by Android.

Backend operational evidence is in its `docs/security/HOME_REAL_MEDIA_SERVE_RETURN.md`; private deployment inputs and raw receipts remain with the backend owner. Real-media publication and Android transport now pass. Physical JMGO installation, decoding/rendering, remote controls and family acceptance still require the user's TV test.

Final backend evidence commit: `6b9c24c7ad978c7f3f73e826fd9527ad25a4ce98`, reviewed after delivery. Its worktree was clean. The backend recorded caption rows advancing from 41,762 to 41,810 without a pause or restart. This evidence update changes neither the APK nor running services.
