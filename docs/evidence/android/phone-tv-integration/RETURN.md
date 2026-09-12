# Phone and TV integration return

Date: 2026-09-12. Branch: `codex/android-phone-tv-integration`.
Base: `5db14f38d3ff7872420f4c5ed16ff54b2cf9b4ac` (repository default branch).
The source merge `52899ad68c44d519ec9bf41fc41078f4220da0ef` retains the complete
phone and TV histories; exact tips and checksums are in [validation.json](validation.json).

## Scope

Combines the authenticated phone media application and home-TV v8 application.
Updates the shared verification script and GitHub workflow to check/build all
three Android applications, including the original offline fixture. Updates
README and parity documents to identify current implementations and remaining gaps.
No contract pins, backend services, private configuration or installed apps change.

## Fresh local validation

`scripts/verify-android.sh --offline --no-daemon` passed with JDK 17, Gradle
8.10.2 and Android SDK/build-tools 34. All 184 JVM tests passed (22 fixture,
76 protected phone, 84 home catalog/discovery, 2 TV labels), with no skips.
Frozen phone, home-feed, catalog and discovery checks and all source boundary
checks passed. All three debug APKs assembled and lint passed with zero errors
(existing warnings: fixture 1, phone 2, TV 3; TV also has 4 informational items).
Fixture packaging is byte-identical to shared fixtures. Connected and TV APKs
have no fixture assets and only Internet plus the internal AndroidX permission.
Their origins and the TV LAN address are empty. APK hashes are local debug-build
evidence, not release signatures or a configured installation package.

All local Android branch tips were verified as ancestors of the source merge.
`git diff --check` passed. A history audit inspected 738 unique blobs and found
no prohibited APK/key/database paths or known private connection/credential
patterns. Gitleaks 8.30.1 reported 37 generic-key findings: 33 public Git pins
or checksums, three public-key fingerprints and one ordinary prose line. Each
was reviewed against its historical blob; no actual secret was identified.
Scanner rules were not weakened and history was not rewritten.

## Remaining gates

Hosted GitHub checks and merge are recorded by the integration PR. Prior emulator
and live-canary returns remain historical evidence; they were not rerun for this
integration. Physical JMGO acceptance, complete media preparation, protected phone
discovery and prepared-media access still require their respective work. This
integration does not make the phone and TV feature sets identical. No backend
publication, service restart, captioning change or device installation occurred.
