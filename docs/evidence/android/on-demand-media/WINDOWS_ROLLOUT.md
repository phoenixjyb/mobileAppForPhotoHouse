# Configured TV v11 rollout — 13 September 2026

Backend source `5269257b4b2a791b59f9181aa83d8e70a5d03b4d` now serves the opt-in
v3 feed on a separate private TLS listener. Android producer pin commit:
`65e40e486d5e35a14f07d692ff5664ba096b7399`; application source remains
`e9ce77b2ed068bd734b1098366043c543ab1cf46`.

The configured TV v11 candidate has its reviewed private origin/LAN mapping built
in, catalog version 3 and discovery disabled. It is staged as
`PhotoHouse-TV-v11-home.apk1` in the owner's existing SMB APK directory.
APK size 10,431,322 bytes; SHA-256:
`3b8f1258a39473096b8d432e1c2310730ca886697fd6f381a3f4a886674b8e5b`.
Existing debug signer retained. APK bytes are ordinary Android APK format despite
its requested .apk1 filename. Copy locally and rename to .apk if the file manager
requires it. No physical installation occurred; ADB had no attached device.

Configured debug build/lint, signature, 16-source producer verifier and SMB operator
read-back hash passed. Reader ACL is read-only. The earlier unconfigured candidates
and synthetic 192-JVM/emulator evidence remain distinct; no full instrumentation
repeat is claimed for this configuration-only package.

Server admission: 23,588 photos (23,573 on demand plus 15 prepared); 11 exceptions.
Four real-photo samples including approximately 200 MP sources passed grid/display,
cache-hit and original-range checks. Two prepared video range checks passed.
The actual listener survives SSH disconnect and rejects an unapproved operator peer
under trusted TLS. Allowed-peer semantic checks are ASGI evidence; physical JMGO
browsing/playback is still required. This does not expand prepared-video coverage.

Old TV feed and APK retained. The new manual Windows task has no automatic reboot
trigger and requires the Windows user's login. Direct older-APK installation may
be rejected as a downgrade; prepare a same-or-higher-version v2 build for rollback.

Phone v5 remains locally built and tested but unconfigured: no approved deployed
protected endpoint/audience or credentials were supplied. It must not use the
anonymous TV feed. Video preparation, protected phone video access and v3 discovery
remain outstanding. No push or merge occurred.
