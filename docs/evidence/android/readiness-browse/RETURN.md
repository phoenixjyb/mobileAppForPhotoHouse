# Readiness browsing: local TV candidate and protected-phone gap

14 September 2026. Branch `codex/android-readiness-browse`, isolated worktree
`mobileAppForPhotoHouse-android-readiness-browse`.
Mobile base: `531f12796d2433347f07b53940d1148317c8ad14`.
Backend: branch `codex/media-readiness-browse`, base
`b9f7383ea8fcdbca86210463b7f5c674c68ea92f`, result
`a72aa320801787cd066c6e04c33764e9271c9ab5`.

## Result

TV v13 adds bilingual Ready first / Ready only and All / Photos / Videos controls.
Selection is applied by the backend before pagination, with ready/matching counts
and stable newest-added order within each group. Selection resets page/revision,
cancels old requests, and survives refresh in memory. Stale responses are ignored.
Strict wire validation checks query echo, counts, media kinds, per-page uniqueness,
readiness ordering and ready-first page boundaries. No library-wide client fetch or
media decode is used to compute filtering. Original permission remains unchanged.

The independent on-demand pin/checksums were updated together, and a new browse
contract packet retains six actual backend-generated synthetic response pages.
Default browse switch is false, and it can only be enabled with catalog v3. The
public verifier explicitly clears the switch as well as endpoint configuration.

## Evidence

- Backend security suite: 515 tests, zero failures/errors, four platform skips.
  After adding the retained-fixture replay assertion, the six focused browse tests
  passed again. The original 515 total included five of those six tests; do not add
  these totals together. Inventory check passed.
- Android JVM: home-core 95, live-core 102, TV 2; all passed, zero skips (199 total).
  The final parser hardening was followed by another successful home-core test,
  TV APK build and TV lint run. Protected phone has no behavioral change.
- TV and protected phone debug APK builds and both lint checks passed, using cached
  Gradle 8.10.2, Temurin JDK 17, SDK/build tools 34; no SDK installation.
- All ten offline contract/boundary verifiers passed. The on-demand and browse
  verifiers additionally checked actual Git blobs/current files at the backend pin.
- `TvBrowseTest`: existing API 36 phone AVD, landscape 1920x1080/density 320,
  English/Chinese, native D-pad center activation, selected semantics, filtering,
  empty-result recovery and background clearing. Passed at font scales 1.0 and 1.5.
  The earlier touch run also passed. This is emulator evidence, not a JMGO result.
- Rendered English, Chinese and 150% Chinese screenshots were inspected. All media
  in the screenshots is generated/synthetic. Final parser-only change was covered
  by JVM fixture tests after UI evidence capture.

Commands (existing JAVA_HOME/ANDROID_HOME and cached Gradle required):

```sh
gradle -p android :home-core:test :live-core:test :tv:testDebugUnitTest \
  :tv:assembleDebug :tv:assembleDebugAndroidTest :tv:lintDebug \
  :connected:assembleDebug :connected:lintDebug --offline --console=plain \
  -PphotohouseTvCatalogVersion=3 -PphotohouseTvBrowseEnabled=true
python3 android/verify-readiness-browse-contract.py --backend-root BACKEND_WORKTREE
python3 android/verify-on-demand-contract.py --backend-root BACKEND_WORKTREE
```

Final local artifacts (empty origin; not a plug-and-play home installation):

| Artifact | SHA-256 |
| --- | --- |
| `android/tv/build/outputs/apk/debug/tv-debug.apk` (v13, browse enabled) | `cbafcca3608a48c448b89de9fb100744c92d1d183ebc961debbf5f523fefc948` |
| `android/connected/build/outputs/apk/debug/connected-debug.apk` (existing phone behavior) | `3ece6bd8c1cfcec271a5a63ff97a73447a75dd41c00ac8ef9e7e1da8d3f5ca96` |

## Remaining work / delivery gates

1. Protected phone readiness is **not implemented**. Its current gallery/discovery
   contract has no prepared-video descriptor or protected prepared stream for
   viewers without original grants. Implement a library-scoped derivative provider,
   descriptors/Range route, revision/source binding and revocation coverage first;
   then wire the same selectors into phone browsing. Do not reuse anonymous TV URLs
   or classify videos from thumbnail presence or file extension alone.
2. Readiness plus people/date/tag search requires a coordinated search-contract
   extension. TV v3 discovery remains disabled; the new selectors currently combine
   readiness with media type only. No advanced-search parity is claimed.
3. Stage/qualify the matched backend under the existing Limited Windows principal,
   then enable its paired configured APK. Preserve the old runtime for rollback.
   Physical projector navigation/playback and phone acceptance are unrun.
4. No live Windows changes, publication, preparation-job restart, phone/projector
   installation, SMB delivery, push or merge was performed. Existing worktrees,
   media, captioning and the memory-bounded batch are untouched by this source slice.

Screenshots: [English](ready-browse-en.png), [Chinese](ready-browse-zh.png),
[Chinese at 150%](ready-browse-zh-large.png).
