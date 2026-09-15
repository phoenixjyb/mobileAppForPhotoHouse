# Home search parity source return

Deployment continuation: [qualified backend and configured APKs](DEPLOYMENT.md).
The source-only gates below describe the original local-development checkpoint.

Date: 15 September 2026. Android branch: `codex/home-search-parity-v16`.
Android base: `5bde252d9bfa74e4574942769190c4afd41a6a77`.
Backend candidate: `98b92702b4bf687f8bb0ed745a0dfe235a15b98e`, branch
`codex/home-discovery-v3`, base `b9f7383ea8fcdbca86210463b7f5c674c68ea92f`.
Both isolated source worktrees preserve previous development and live runtime work.

## Result

Phone Home now offers server-reviewed quick-person buttons and a combined search
editor for caption words, dates, people, tags, recorded regions and media type.
The editor follows the existing cream/green theme, has Chinese/English labels,
reports partial metadata, validates input and scrolls to all filter controls.
Queries and paged results are memory-only, isolated from the album and protected
account mode; clear search explicitly returns to browsing. Background/denial
clears or covers content. No private names or live metadata are bundled.

Phone and TV independently select the new discovery/v2 contract for v3 media,
retaining on-demand photos, explicit permitted originals, and prepared/admitted
direct video in search results. TV reuses its existing remote search interface.
Older discovery/v1 remains available for catalog/v2 and its frozen pin is unchanged.
Search has no readiness-order/filter capability, so those browse controls are not
presented within search. Themes/topics, GPS-radius and protected prepared-video
parity remain future work.

Changed source areas: shared Home parser/adapter and new TLS tests; phone Home
composition/editor and native tests; TV composition; separate contract pack and
verification lane; version/configuration and parity evidence. Existing protected
transport and runtime workers are untouched.

## Validation

- Backend: 46 discovery/export/delivery tests, including 7 new composition checks;
  15 on-demand-media regression tests. Actual synthetic media decode, original
  permission, prepared/direct Range, peer denial, disable and input drift checked.
- JVM: 106 Home, 103 protected/live-core and 2 TV tests; zero failures or skips.
- API-36 emulator: 6 phone Home/search tests passed. Covers quick people, result
  paging, clearing, combined filters/date validation, offline retry, denial and
  background behavior, plus existing phone media/browse checks. Synthetic only.
- Default phone/TV debug builds, phone test APK and explicitly enabled search
  builds pass. Phone and TV lint pass. 11 contract/boundary verifier scripts pass;
  new contract replay verifies 37 committed producer inputs and working bytes.
- Chinese editor screenshot visually inspected: [preview](home-search-zh.png).
  No physical phone/projector or large-font acceptance is claimed.
- [Machine-readable evidence](checks.json) records APK hashes and counts;
  [native output](native-tests.txt) records instrumentation completion.

JDK 17, Android platform/build-tools 34, cached Gradle 8.10.2 were used. The fresh
worktree wrapper download did not complete; the same installed Gradle version
successfully ran all reported builds. No toolchain versions were changed.

## Delivery gates

Phone versionCode 7 (`0.8-phone-home-search`); TV versionCode 16
(`0.16-home-search`). All built origins are empty. Search defaults off and requires
its own reviewed configuration: `photohousePhoneHomeDiscoveryEnabled=true`, or TV
catalog 3 plus `photohouseTvDiscoveryEnabled=true`. These source test APKs are not
plug-and-play home releases and were not copied to SMB or installed on family devices.

Before live enablement: privately review actual family names/aliases and person
assignments; measure caption/tag/place coverage; build and pin the admitted index;
qualify full-library memory, latency, slow-client limits and the actual service
account; then deploy the reviewed composition and test configured APKs on phone
and projector. Backend source alone does not create or activate that metadata.

This development slice made no Windows service, captioning, encoding, database,
publication or audience changes. Commits are local; no push, merge or deployment
was performed for this slice.
