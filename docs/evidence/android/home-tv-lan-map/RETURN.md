# PH-ANDROID-HOME-TV-LAN-MAP-01 — private APK ready

The user requested connection information inside the APK. The private v3 build
contains the verified home-feed HTTPS origin and server LAN IPv4 address. It needs
no projector DNS change. This explicitly supersedes the earlier manual-DNS-only
TV plan for this build; it does not claim a change to normal system DNS.

## Source and artifact

- Clean observed base: `887db83eb96db136d85589276bc9c6e93081bb54`.
- Source commit: `c42918a54991895a3869977f92aa29ffee2377e5`, branch `codex/android-tv-foundation`.
- Backend runtime: `e6b2827842b2c0b5223c85208299b60e8a1257f6`; contract SHA-256
  `70328a653ddaa559bad6a4d654cf9870c89c5217e8e9e6c46a3501e9dd9e7548` unchanged.
- Private artifact reference `HOME-TV-V3-LAN-MAPPED`, filename `PhotoHouse-TV-home-v3.apk`.
- APK SHA-256 `710c9d3aecd17dcc4b05f00e578504c0522cb1026652520242fe782b8b244b13`; 8,802,427 bytes.
- Package `dev.photohouse.tv`, versionCode 3, `0.3-home-lan-dev`.
- Debug signer SHA-256 `56d7591b2b6c2538d506d1fe51327444f2307736cb12f5beefa08f1c410d6d28` (same as v2).

Changes remain Android-owned. Phone/protocol/backend source is unchanged. Current
executor owns this bounded mapping/build slice; no subtask or model switch.

## Connection behavior

`photohouseTvOrigin` and optional `photohouseTvLanAddress` are private build inputs.
The mapper accepts canonical RFC1918 IPv4 literals and maps only the exact feed
hostname. Literal parsing performs no DNS resolution. Mapped connections are
direct, with no proxy, public-DNS or alternate-host fallback. URL hostname, TLS SNI
and default certificate/hostname validation stay intact. Invalid configuration
shows setup rather than falling back. The normal build without a LAN mapping still
uses normal system DNS. No credentials, HTTP exception or trust override was added.

The APK does not assign the projector an IP, change its gateway/system resolver,
or affect DNS for other apps. It stores the server address it needs to reach.
If that server address changes, a newly configured APK is required. Configured APKs,
origins, addresses and full build logs remain private; tracked source defaults empty.

## Verification

- 29 home-core JVM tests passed: prior 23 plus six address/mapping/default-trust,
  hostname-preservation and certificate-rejection checks.
- One explicitly selected live LAN test passed, making four real HTTPS requests:
  feed, exact grid JPEG, exact 3840x2160 JPEG and repeated feed. It used the public
  mapped adapter constructor and normal JVM certificate trust against the backend
  owner's running synthetic service. It confirmed only fixture feed/asset 101 and
  exact pinned bytes. No credentials or real selection were requested.
- Live LAN test is excluded from ordinary tests and requires explicit private
  origin/address environment inputs through `:home-core:lanPilotTest`.
- Unconfigured APK/test APK/lint build passed in 16 seconds. Private mapped APK/lint
  passed in 17s. Zero lint errors; three retained warnings (OldTargetApi,
  VectorRaster banner, fixed landscape DiscouragedApi).
- Verified both configured values in generated BuildConfig and APK DEX; package,
  version and unchanged signer; no test assets or phone/protocol classes in APK.
- Home-contract, TV and connected source-boundary checks passed. Existing phone
  implementation is untouched. Previous UI/emulator evidence is retained, not
  rerun or presented as validation of the new private APK on a projector.

An initial mock assertion expected HTTP/1.1 Host while HTTP/2 supplied :authority;
the check now verifies the hostname through either protocol representation. Both
wrong-name and untrusted certificate failures passed; no TLS restriction was relaxed.

## Server/device state and next step

The backend owner reports the scoped synthetic HTTPS and DNS pilot services are
running without the former 30-minute cutoff. Our new live mapped adapter check
verified the HTTPS feed was reachable independently of projector DNS. Android made
no server, firewall, resolver, task or publication changes. Existing DNS is not
stopped because the user's prior Save state is unknown; backend retains ownership.

Install this v3 APK over v2 using the projector's supported manual installer. The
same package/signer and increased version code support an update. Keep Windows
running. No app sign-in, device-approval page or projector DNS change is required
for PhotoHouse's mapped connection. Actual JMGO install, platform trust, display,
remote and sleep/wake behavior remain for user/device acceptance. No physical
installation, real media, push or merge occurred in this slice.

Machine-readable receipts: [result.json](result.json), [tests.json](tests.json).
