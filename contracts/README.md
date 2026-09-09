# Shared contract proposal

Status: **not implemented, not frozen, not an OpenAPI specification**.
Backend owner and coordinator must settle and version this contract before the
platform sessions create compatible fixture implementations. Do not point these
names at the current unauthenticated PhotoHouse server.

The authoritative API schema remains in vlmPhotoHouse. Eventually place its
reviewed consumer snapshot, source SHA/checksum and synthetic JSON fixtures in
contracts/v1/. Both clients parse the same fixtures in their native languages.
Never generate a snapshot containing home paths, credentials or real photo data.

## Common types and states

- Account: opaque ID, display name, account lifecycle; no implicit family role.
- Membership: library ID, role, requested/approved/rejected/revoked state, revision.
- Session: signed-out, authenticating, authenticated, expired/revoked; carries an
  independent selected-library membership state. UI waiting approval is not auth.
- AssetSummary: opaque ID, media kind, optional capture timestamp + offset/known
  timezone semantics, dimensions, authorized variant references and revision.
- AssetDetail: summary plus permitted metadata and structured caption items.
- Caption: language, text, human/AI source, generation/revision and current status;
  translation availability separate from interface locale. Do not regex-split raw
  bilingual strings in both clients independently; backend adapter normalizes them.
- Album: opaque library-scoped ID, title, authorized cover and item count;
  distinguish saved drafts, dynamic stories and pending generation.
- Page: stable cursor/next cursor and authorized items; deterministic ordering.
  No private global counts. A search cursor is bound to principal/library/query.
- Error: stable code, safe localized-display key, retryability, request ID; never
  raw SQL, filesystem path, stack trace, token or full private query.
- VoiceResult: session-owned request ID, normalized transcript if consented,
  typed intent, authorized cards, clarification/error state, optional reply audio.

## Proposed operation families (names only)

| Family | Minimum contract behavior |
| --- | --- |
| Identity/session | Hosted login/register through approved OIDC flow; own-profile, logout and device-session management |
| Membership | Own status, accept invitation/request access, owner approval/revocation; anti-enumeration |
| Library/assets | Cursor list, detail, thumbnail/preview/video; checks on every byte/Range request |
| Search | Authorized text/time/album filters, empty result/ambiguous person semantics |
| Albums | Read-only list/detail first; future writes versioned and capability-gated |
| Account deletion | Reauthentication, request/status, documented account/shared-library effects |
| Voice | Bounded push-to-talk, typed read-only intents, cancellation and owned conversation IDs |

Final paths, response fields and pagination compatibility need backend approval.
Version negotiation must fail visibly when unsupported rather than guessing a
legacy unauthenticated route. API/server switching cancels and clears old sessions.

## Fixtures and negative cases to author in P0

Use invented accounts A/B and libraries L1/L2 with generated shapes, no personal
photos. Cover no identity, registered with no membership, waiting approval,
approved viewer, rejected/revoked membership, token expiry, wrong-library asset,
empty page, cursor expiry, EN-only/Chinese-only/bilingual/missing captions,
unavailable video, HTTP 401/403/404/429/503, cancellation and late old-session
responses. Assert that pre-approval clients never request gallery/media.
These client tests supplement, never replace, backend authorization tests.
