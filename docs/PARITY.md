# Platform and backend parity

**Historical foundation ledger.** Current Android phone/TV implementation and
remaining capability differences are recorded in [Android parity](../android/PHONE_TV_PARITY.md).
The table below is the 9 September foundation baseline, not current Android status.

Updated 2026-09-09. Contract `1.0.0-fixture.1` is frozen for app work. Android and
iOS remain **not implemented**. Backend source checkpoint:
`1e394f789ff1f7cef6d9930bb541186684f5a9a0`; 198 local security tests, no deployment.

| ID | Shared scenario | Android | iOS | Backend/contract evidence |
| --- | --- | --- | --- | --- |
| AUTH-01 | Valid invited registration; invalid invitation denied | Not implemented | Not implemented | Local backend tested; frozen registration/viewer fixtures |
| AUTH-02 | Available/requested/rejected/revoked/expired/empty memberships | Not implemented | Not implemented | Local backend tested; frozen session cases |
| AUTH-03 | Logout, expiry and library/account/server switch | Not implemented | Not implemented | Revocation backend tested; native lifecycle remains open |
| AUTH-04 | Late response cannot restore private UI | Not implemented | Not implemented | Client APP-06–09 acceptance; web evidence does not close native gate |
| AUTH-05 | Account deletion and recovered-owner/library reopening | Deferred | Deferred | Not in native contract; recovered access remains closed |
| MEDIA-01 | Page-number gallery, detail and literal captions | Not implemented | Not implemented | Scoped backend/ASGI fixtures verified; no cursor/structured translation |
| MEDIA-02 | Authenticated thumbnail and original GET/HEAD/Range | Not implemented | Not implemented | Backend tests and synthetic JPEG range cases; native video/device unverified |
| MEDIA-03 | Missing preview/offline and private cache cleanup | Not implemented | Not implemented | Frozen error cases plus client-only lifecycle scenarios |
| LANG-01 | EN/ZH UI, unchanged literal caption content | Not implemented | Not implemented | Plain-text bilingual/XSS/missing-caption fixtures |
| A11Y-01 | Large text, accessible labels and private app preview | Not implemented | Not implemented | Native render/accessibility evidence required |
| FIND-01 | Scoped search | Deferred | Deferred | Closed; no supported operation in v1 snapshot |
| ALBUM-01 | Library-scoped albums | Deferred | Deferred | Closed; no supported operation in v1 snapshot |
| VOICE-01 | Push-to-talk, owned intents, cancel and fallback | Deferred | Deferred | Legacy voice closed; reviewed service contract required |

Coordinator updates this ledger only after inspecting platform source/test/build/
render evidence. Platform sessions write their reports under their assigned
`docs/evidence/android/` or `docs/evidence/ios/` directories. Neither edits this ledger.
Record base/result SHA, contract checksums, commands, exact artifacts and separate
simulator/emulator, signing, installed-device, live-service and family acceptance.
A green fixture test never closes a server, TLS, physical-device or deployment gate.
