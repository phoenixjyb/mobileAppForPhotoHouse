# Platform and backend parity

Date: 2026-09-09. Every row is **planned**, not implemented. A design review is
not a test pass. Backend authorization gates are independent of native UI tests.

| ID | Shared scenario | Android | iOS | Backend gate |
| --- | --- | --- | --- | --- |
| AUTH-01 | Register/sign in creates identity, no automatic library | Planned | Planned | OIDC validation + zero implicit membership |
| AUTH-02 | Invitation/approval/rejection/revocation states | Planned | Planned | Owner and membership policy |
| AUTH-03 | Logout, expired credentials, switch account/library/server | Planned | Planned | Session and membership recheck |
| AUTH-04 | Late old-session response cannot restore private UI | Planned | Planned | Revocation + cache behavior |
| AUTH-05 | Account deletion and shared-owner transfer | Planned | Planned | Defined data lifecycle, no silent shared deletion |
| MEDIA-01 | Authorized cursor gallery and detail | Planned | Planned | Object-level access, no paths |
| MEDIA-02 | Thumbnail, preview, original, crop, video Range | Planned | Planned | All byte-serving routes protected |
| MEDIA-03 | Offline/unavailable server and memory cache cleanup | Planned | Planned | Private response/cache policy |
| FIND-01 | Scoped text/time/album/people search and empty state | Planned | Planned | Scoped index/query/aggregates |
| ALBUM-01 | Read authorized saved albums and stories | Planned | Planned | Covers/items/counts scoped |
| LANG-01 | English/Chinese UI independent of caption language | Planned | Planned | Structured captions and missing-language state |
| A11Y-01 | Large text, accessible controls, screen reader | Planned | Planned | Safe localized errors |
| VOICE-01 | Push-to-talk, denied mic, cancel, text fallback | Planned | Planned | Bounded authenticated adapters |
| VOICE-02 | Read-only intent and owned conversations | Planned | Planned | Shared authorization, no mutation tools |

For each implementation update record: source SHA, contract fixture version,
unit tests, simulator/emulator render tests, signed artifact identity, physical
device results, live service results and family acceptance. Leave unrun cells
explicitly open. One platform's acceptance never automatically closes the other.
