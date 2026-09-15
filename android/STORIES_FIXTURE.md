# Family Stories: Android synthetic presentation slice

Owner: Android task. Base `1185327f7cfbae13eabfe0781a602b7c5e27a265`, branch `codex/home-search-parity-v16`. Calendar v18/v9 release remains a separate, already delivered checkpoint. This work is local-only and is not in those delivered APKs.

## Scope

`story-fixture-core` contains in-memory presentation models and a synthetic controller. `story-fixture-ui` is compiled only when `-PphotohouseStoryFixtureEnabled=true`, into the **debug source set only** of phone (`connected`) and TV (`tv`). The default is false: ordinary builds contain neither the lab activity nor its synthetic data. Its non-exported `StoryFixtureActivity` has no launcher or production navigation entry. Instrumentation launches this explicit test activity. The existing network adapters, protected/Home navigation and frozen contract files are unchanged. There is no story HTTP adapter, DTO parser, persisted draft, account credential, media fetch or production access decision here.

The common screen provides source-labelled story viewing, full literal EN/ZH writing, sample text/source/media filtering, empty results, viewer/contributor/owner controls, memory-only drafts, uncertain-save retry, conflict comparison and discard confirmation. The sample byline is self-supplied, not verified identity. Earlier family notes remain distinct from AI and account-authored stories. TV exposes only fixture entries carrying the current synthetic publication revision; private entries and write controls are omitted. This simulated revision check is not an implemented export policy or real authorization.

After an uncertain save, fields are locked so retry preserves exactly the original body and UUID. A definitive conflict retains the draft, shows the current text and requires explicit review before a new revision-based save. A successful retry displays the returned current version, which may be newer than the attempted edit. Discarding a local uncertain draft does not undo a possible server save. Access loss, background, library switch and publication withdrawal clear visible state and invalidate old request tickets.

## Proposed adoption work for coordinator and API owner

Reviewed immutable backend source: `97e9cc4eaa2c0ff34aa60837e84099c017927ef1` (evidence HEAD `2ad064fb845fc077bb4ecfdb38494023022e3c0a`). The local presentation proposal is aligned with that reviewed source; no network DTOs or shared snapshot have been adopted.

The adjacent [proposal manifest](story-fixture-core/proposed-contract.json) records exact producer input hashes, operation/field proposals and needed wire vectors. These are requirements for review, not silent additions to the frozen contract:

1. Adopt immutable producer source plus checksummed synthetic fixtures for six protected routes, numeric response revisions/times/pages and string request revision/page. Preserve full story fields, author ID, capability flags and literal language/byline semantics. History deletion currently uses numeric 0/1; story deletion is boolean.
2. Verify additive 512 KiB request / 3 MiB response budgets with worst-case JSON escaping, including five history rows plus current story. Existing caption budgets must remain unchanged. Android needs bounded streaming reads before JSON allocation.
3. Include viewer/author contributor/other contributor/owner, wrong-library, revoked access, history privacy, conflict, lost reply, exact retry returning a newer current version, invalid surrogate and long mixed-language cases.
4. Offset pages are a current view, not a snapshot. Production consumers must deduplicate IDs, restart at page 1 after mutations and never claim that no item moved during pagination. This lab has a bounded synthetic list; production pagination/history/removal are not implemented here.
5. Search adoption must retain the producer's one-result-per-asset grouping, ranking, excerpts and scoped media descriptors. This lab filters story cards directly for presentation tests; its local matching is not an API/search parity claim.
6. TV needs a separate approved published-story projection, bound to audience, catalog/publication revision and withdrawal/revalidation behavior. No protected story route may become reachable through anonymous Home access. The lab's published sample markers are not an agreed wire format.
7. Networking stays disabled until coordinator adoption. Then integrate protected phone read/search first with existing session/library generations and privacy; TV networking waits for its reviewed publication contract. History, deletion, server pagination, real media integration and installed-device acceptance remain later bounded slices.

## Validation procedure

Use the existing Gradle/JDK/SDK. Run `:story-fixture-core:test`, `:connected:lintDebug`, `:tv:lintDebug`, both debug and AndroidTest builds. For synthetic build inputs set `-PphotohouseStoryFixtureEnabled=true` and explicitly set empty `photohouseOrigin`, `photohousePhoneHomeOrigin`, `photohousePhoneHomeLanAddress`, `photohouseTvOrigin`, `photohouseTvLanAddress`; set TV catalog 1 and discovery/browse false. Do not use private release settings or signing assets.

The shared `StoryFixtureUiTest` runs on either test package. Verify emulator serial, qemu identity and exact target APK package before installation. Exercise the phone layout and landscape TV layout, including native D-pad center/up/down/Back, draft discard, source labels, long text, language, permission loss and lifecycle clearing. Restore emulator display overrides; preserve unrelated apps/data. No physical device install, reset or distribution is authorized by this slice.

The originating coordinator explicitly clarified that standard debug signing and the existing designated emulator are allowed for these synthetic tests. Production signing, uploading these test APKs, release deployment and real family-story publication remain outside this slice.
