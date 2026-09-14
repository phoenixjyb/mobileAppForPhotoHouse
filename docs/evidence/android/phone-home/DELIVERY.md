# Phone v6 private Home candidate

Home-configured `PhotoHouse-Phone-v6-home.apk1` is a normal APK with the requested
alternate suffix. Stored outside this public repository; no SMB upload or device
installation in this slice.

- Package: `dev.photohouse.connected`
- Version: 6 / `0.7-phone-home`
- Bytes: 10,637,096
- SHA-256: `3aadaa10d64a02819322c16d50a227010e5c4cbda5dfe6dc5f85d6cb0a015583`
- Signer SHA-256: `56d7591b2b6c2538d506d1fe51327444f2307736cb12f5beefa08f1c410d6d28`
- Offline configured build succeeded; `apksigner verify` passed; package/version,
  separate Home routing in generated config and APK dex, and absence of assets
  checked. Protected origin is empty; protected discovery/photo-delivery flags off.
- The unconfigured sibling APK was used for synthetic emulator tests. The private
  candidate differs by Home routing only and has not been run on a real phone.
- Verify/admit the intended phone peer before a live installation test. No server
  allowlist expansion, real credentials, backend changes, push or merge performed.

Full source and test evidence: [RETURN.md](RETURN.md).
