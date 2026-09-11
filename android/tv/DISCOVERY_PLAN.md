# PH-ANDROID-TV-DISCOVERY-01

Base `d2b224015ee84d12d1e44929cb34893780046793`, branch
`codex/android-tv-foundation`. Parent owns Android implementation/integration;
the requested UI/UX subagent performed a read-only review. Current model retained;
no model switch or new user-owned task was created. Android write scope remains
`android/**` and `docs/evidence/android/**`. Backend discovery/indexing belongs to
the existing backend task and its separate worktree.

## Product and design

The user requested full-library discovery by people and bilingual aliases, dates,
themes/topics, caption-derived tags, and advanced combined location/text/media
criteria. No personal sign-in on the home TV. Actual household aliases and identity
mappings belong in private backend configuration, never public source or fixtures.

Keep the media grid as the default. A compact header and Explore entry lead to a
full-height scrollable discovery page: family shortcuts, date/theme/topic/tag
browsing and Advanced search. Use deep green, cream and muted gold; serif only for
the small brand, readable sans-serif elsewhere. Focus is a strong gold border/fill,
controls have at least 52dp targets, text cards grow with text and rows scroll.
Avoid adding fixed discovery rows above the grid or shrinking text at large fonts.

Each shortcut references one reviewed stable person ID and displays its primary
name plus aliases. Caption mentions do not prove identity. Themes and topics are
distinct from generated tags; generated metadata needs provenance and coverage.
Date semantics and unknown dates must come from the contract, not inferred labels.

Advanced filters combine categories with AND. Any/all within a category is explicit
and only offered if supported. Draft edits do not query until applied; cancellation
restores the previous query. Empty results follow an actual completed search, never
an unavailable endpoint. Missing capability/index data has an honest state and a
path back to browsing. Do not emulate full-library search by filtering one page.

## Work and acceptance

1. Refine the shared TV visual hierarchy and implement remote discovery navigation.
2. Review the backend-owned versioned metadata/search return before any transport
   implementation. Preserve current v1/v2/phone pins and never revive legacy routes.
3. Implement bounded query, facets and lifecycle against the returned capability
   contract where available; keep unconfigured/unsupported states explicit.
4. Test D-pad/OK/Back, aliases, combined filters, empty/error/cancel behavior and
   background clearing. Render EN/ZH at 1×/2× fonts, then build and identify APKs.

Back closes picker/keyboard before leaving a form; leaving a form restores Explore
focus, and leaving Explore returns to the library entry. Results retain criteria
and selected tile through photo/video viewing. Every control is remote-reachable.

Android guidance consulted: [TV navigation](https://developer.android.com/training/tv/get-started/navigation)
and [TV design](https://developer.android.com/design/ui/tv). Layout, visible focus
and two-axis navigation are the relevant principles; no new dependency is needed.

No deployment, live indexing, family-ID assignment, real-data reads, physical
installation, push or merge is implied. Source, synthetic tests, APKs, live search
coverage and projector acceptance remain separate evidence.

## Implemented and validated

The discovery v1 adapter pins backend runtime `54f68427058c45f6bcc5a863cc6d708f5b325e45`.
The requested read-only UI/UX subagent reviewed the source and 1920×1080 render;
a second bounded adapter subagent owned only the new transport/parser/test files.
The parent integrated both, fixed result-store focus transitions and retained
later facet pages only under revalidated identical metadata bindings.

Local validation: 80 home-core JVM tests; phone regression tasks confirm 22 core
and 65 live-core tests (unchanged tasks up-to-date); 24 TV component tests at each
of 1× and 2× fonts; debug build/lint; four contract/boundary verifiers; 11 actual
in-process backend discovery checks from 19 verified Git inputs. Screenshots and
artifact receipts are recorded in `docs/evidence/android/discovery-v1/`.

These are synthetic component/transport/source checks. No real discovery index,
reviewed household ID mapping, discovery-capable origin, taxonomy, radius search,
projector installation or device acceptance is claimed. Production themes/topics
remain disabled by the frozen contract, and the existing v1 home APK profile
leaves discovery networking disabled. The new v2 discovery profile is explicit.
