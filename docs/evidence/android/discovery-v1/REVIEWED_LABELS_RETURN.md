# Reviewed provenance label correction

Base: `a2c48fb727402ccd6beead855a3faa78f552c973` on
`codex/android-tv-foundation`. This narrow follow-up addresses the backend source
review; the earlier v6 APK receipt remains unchanged.

`TvDiscovery.choiceHint` now displays `reviewed_assignments` as **Reviewed people /
已确认人物**, and `reviewed_region` as **Reviewed region / 已确认地区**. Actual unknown
and unrecognized provenance remain **Unknown source / 来源未知**. Caption-derived
tags retain their separate label and are never described as reviewed identity.

Changed source: `android/tv/src/main/java/dev/photohouse/tv/TvDiscovery.kt`.
`choiceHint` is internal so the TV unit test can check the exact strings used by
`ChoiceRow`. `android/tv/build.gradle.kts` adds the already-cached JUnit 4.13.2 as a
test-only dependency. `DiscoveryLabelsTest.kt` verifies EN/ZH people and place
labels, aliases/counts, true/unknown provenance, zero-count omission and caption
provenance separation.

Validation from `android/`:

```sh
./gradlew --offline :tv:testDebugUnitTest :tv:lintDebug
```

**2 tests passed**, 0 failures/errors/skips; test execution 0.024 seconds.
Build/lint successful in 7 seconds. [Build log](reviewed-labels-build.log).
The four v1/v2/discovery contract and TV boundary verifiers also passed;
`git diff --check` passed.

No contract/pin change, family mapping, endpoint, networking or lifecycle change.
The existing backend replay was not repeated for this label-only correction.
Emulator/render suites were not rerun; this check exercises the actual label
formatter, not a new rendered/device acceptance claim. No configured APK was
rebuilt or installed, and no operational action, push or merge occurred.
