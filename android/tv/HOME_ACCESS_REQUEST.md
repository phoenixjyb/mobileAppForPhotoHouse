# Home TV automatic-access contract — decision required

User requirement: the home projector opens directly into photo browsing with no
personal sign-in screen. TV UI now contains no phone/password/registration inputs.
The existing Android browsing contract pins backend
`87a60b475b37b1d6873cd977bcb6e7254472da7e`; the backend owner reported the separately served synthetic candidate
as `f15e50753a09b46c8b8748ec87a26f15853bcf0c`. Its protected reads require an opaque
24-hour bearer, and it has no TV pairing/device-session or anonymous home-feed
contract. Origin configuration alone cannot satisfy automatic startup.

A user question is pending between these models. This document does not choose or
authorize either backend change. No real server request or mutation was performed.

## A. Once-approved projector, automatic subsequent access

No account form on the TV. Owner approves the display once through a separate
private setup flow. Backend needs a named, revocable, library-scoped device grant,
short-lived browsing sessions, bounded renewal and expiration, denial/rate-limit
behavior, owner revocation and original/preview capability checks. Define secure
local device-key/credential handling and reset/reprovision behavior before coding
persistence. Never persist an account password or put a grant in the APK/QR URL.

## B. Deliberately unauthenticated selected home feed

Any client that can reach that feed can read it; it must be an explicit owner
sharing decision, not a claim that an IP address establishes account identity.
Backend owner must define the exact selected library/assets/derivatives, permitted
metadata and image sizes, dedicated read-only routes/listener and LAN isolation,
unknown/direct/Range path denials, no invitation/account/operational exposure,
revocation/disable policy and accidental public-ingress tests. Preserve existing
phone/protected API authentication. Keep HTTPS for transport unless separately
reviewed; local routing does not require public-IP/port-forwarding changes.

## Required return to Android

- Chosen policy and owner-approved audience/library scope, kept private.
- Source/contract pin, schemas and synthetic allow/deny tests for the new TV flow.
- Automatic startup/reconnect semantics, denied/expired/disabled states and rate limits.
- Prepared 1024 or larger display derivatives, or explicit original access policy.
  The current original grant is off; LAN bandwidth does not create that permission.
- Deployed synthetic evidence and private origin/configuration references; exact
  JMGO firmware, installation/operator authorization and local DNS/trust evidence.

Until then the APK is a tested viewer prototype with a setup screen, not a working
automatically connected home library. Synthetic UI tests inject a memory-only test
adapter; none of those adapters or sample accounts are part of the shipped APK.
