# Security and voice design

Proposed controls, not implemented protections. Audit date: 2026-09-09.
The current PhotoHouse source is a trusted-network application without inbound
user accounts or library authorization. Its runtime network exposure was not
re-audited in this planning phase. Do not expose it more broadly.

## 1. Threat model and server policy

Protect originals, previews, face crops, names, captions, location, history,
library counts, search results, conversations, and operational metadata from
anonymous visitors, unapproved registrants, another library's members, revoked
sessions, leaked credentials/links, shared-device residue, and public CI artifacts.

Recommended records: Account keyed by verified OIDC issuer/subject; Library;
Membership(account, library, status, role, revision, approver); revocable app
session/device records as needed. Person/face records are subjects in photos,
not login identities. Matching a face or a voice never grants account access.

Membership status is separate from identity session state. Proposed memberships:
requested, approved, rejected, revoked. An authenticated user may have no library.
Owner invitation must be short-lived, single-use and explicitly accepted by the
correct authenticated principal; no bearer invitation becomes a photo URL.

| Capability | Viewer | Contributor | Library owner | System operator |
| --- | --- | --- | --- | --- |
| Browse approved library / search / read-only voice | Yes | Yes | Yes | Only with explicit membership |
| Upload / curate allowed albums | No | Yes, later phase | Yes, later phase | Not implicit |
| Manage membership / destructive library changes | No | No | Explicit policy/confirmation | Recovery only through separate audited authority |
| GPU jobs / server configuration / ingest filesystem paths | No | No | Not implicit | Yes |
| Original download/export | Separate library capability, not automatic from role | Same | Configurable | Not implicit |

This matrix is a design input; v1 mobile exposes browse only. Viewing original
bytes permits saving them. Export restrictions are not DRM, and already saved
copies cannot be remotely recalled. No account/role/library supplied in a request
is trusted without independent server-side verification.

Evaluate authorization before reading files, invoking model providers, or making
mutations. Check both seed objects and results, parent-child relationships, bulk
IDs and queued actions. Derived media inherits the parent asset's visibility and
library policy. Scope people, face samples, albums, tags, covers and counts.
Search needs library partitions or truly scoped retrieval, not merely filtering
unauthorized rows from a global top-k response. Deduplication must not reveal a
foreign library's existing object IDs or associate that data without authority.

Unknown routes default to protected. Only carefully inventoried minimal health,
non-sensitive UI assets, and authentication entrypoints may be anonymous. A
registered user awaiting approval can see their own profile/request state, not
library data. Use controlled 401/403 states and non-enumerating inaccessible-object
responses; sanitize exceptions and private filesystem paths.

Implement policy in shared services as well as HTTP boundaries: voice code can
call library functions without going through route dependencies. Protect legacy
routes and all HTTP methods; a new /api/mobile prefix cannot close old bypasses.
This follows [OWASP object-level authorization guidance](https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/).

## 2. Migration and web compatibility

Security schema changes require explicit migrations, synthetic rehearsals,
backups and rollback design. Existing startup fallback schema edits are not a
safe substitute. Assign legacy data to one explicitly designated household
library without moving originals or changing their IDs; validate all parent/child
links and close access until owner bootstrap succeeds. Never grant first signup
ownership automatically. Recheck current account/membership state for each
protected request and delayed task; JWT validity alone does not prove current
membership. Session revocation and refresh rotation need explicit tests.

The existing web UI must move to the same policy before enforcement cutover:
server-managed secure HttpOnly session cookies, CSRF defense for mutations,
login/expired/access-pending states, authenticated image/video requests. Mobile
uses bearer access tokens, never URL query credentials. Authorization-provider
failure must fail closed, not restore the old anonymous mode.

Negative matrix: anonymous; signed-in unapproved; revoked; wrong library; viewer
attempting writes; malformed/expired token; nested foreign ID; expired membership;
stale pending task; direct original/thumbnail/face crop/video Range; search/counts;
old endpoint aliases; conversation IDs; diagnostics; downloads and future exports.
Test denial before provider/file access. Test approved access, migration integrity,
and unchanged authorized web behavior too. A partial security patch cannot deploy.

## 3. Device privacy

Production HTTPS with normal certificate validation. Pin trusted configured
origins; do not send tokens to arbitrary image URLs or cross-host redirects.
Access token in memory. iOS refresh credential in Keychain with device-local
accessibility; Android encrypted private storage with a Keystore-protected key
(Keystore stores keys, not arbitrary token strings). No passwords/tokens in
logs, preferences, URLs, screenshots, analytics, crash reports, or fixtures.

Version one has no persistent offline photo library: bounded memory caches,
ephemeral requests, explicit disabling of image-library disk caches. Exclude
sensitive files from backup/device-transfer rules. Partition all temporary state
by server, account, library and session generation, plus asset variant/revision.
Test native video loaders separately: byte-range retries must authenticate and
must not secretly create reusable public links or persistent media caches.

Logout, account/library/server switch or revocation first covers private UI and
advances a generation counter, then cancels requests/player work and clears
credentials, navigation, images, cookies and temporary files. Late old-generation
responses cannot reappear. Local logout works offline; do not claim the server
was notified if disconnected. No immediate revocation guarantee for previously
exported files or a future offline library. Shared-device previews and notification
text must not expose thumbnails/names; screenshot prevention is not foolproof DRM.

Future offline downloads require explicit opt-in, protected per-account files,
storage limits, retention/lease policy and cleanup tests. Minimize retained data
according to [OWASP mobile storage guidance](https://mas.owasp.org/MASVS/controls/MASVS-STORAGE-1/).

## 4. Voice: shared backend, independent implementation session

First feature: user presses Talk, grants microphone permission, asks in English
or Chinese, reviews transcript/result cards, optionally hears a short reply.
Text search remains available on permission denial, provider failure or timeout.
No always-on recording, wake-word listener, speaker provisioning or model launch
is implied. No raw audio retention by default; keep audit metadata content-light.

Pipeline: bounded audio -> replaceable STT adapter -> typed intent -> existing
authorized library service -> result cards -> optional replaceable TTS adapter.
Authentication and current membership flow through every stage. The server, not
the transcript or model, resolves account, library, capabilities and conversation
ownership. Photo captions and transcripts are untrusted content, not instructions.

Allowlist initial intents: search assets, search people, find approved person's
assets, describe authorized asset, help. Ambiguous names need clarification;
unavailable captions stay unavailable. No arbitrary SQL, shell, open-ended tool
execution, external URL fetching, rename/merge/delete, sharing or administrative
commands. Reuse authorized service functions; never call unguarded legacy routes.

Existing voice adapters are not ready-made secure mobile endpoints. The source
does have a feature-enable check on /voice/command, but no inbound user identity.
Confirmation storage is caller-client-ID based and accepts an omitted token;
provider conversation IDs lack local account ownership. Some voice-photo handlers
refer to obsolete Asset fields. Repair/retire these explicitly, with regressions.
Provider failures need consistent typed errors, not success-shaped HTTP 200.

Audio byte/duration limits, queue limits, timeouts, cancellation, per-account
rate limits, and content-minimized telemetry are required. Admission must keep
caption/face processing and room heat constraints in view: GPU availability alone
is not a resource lease. Benchmark STT/TTS separately; prefer CPU/device-local
options where feasible and retain CPU/text fallback. Any shared-GPU provider needs
explicit arbitration and measured budget before loading it on Windows. Cloud
audio/transcript processing requires a separate user data-processing decision.

## 5. Speakers and later mutations

Shared speakers come later. Owner-mediated pairing issues revocable, narrow
device credentials for specific library/actions and short sessions. A paired
device proves device identity, not the identity of whoever speaks nearby.
Default private result delivery to the authenticated phone. Audible names,
descriptions or shared-screen photos require an explicit room-disclosure policy;
voice recognition is not authentication. No hidden ambient audio retention.

Future mutations require current user/capability/object authorization plus a
single-use confirmation bound to exact immutable arguments, principal, session,
library, expiry and nonce. Require the token, reject replay, audit the outcome,
and prefer an authenticated phone confirmation for risky actions. A spoken yes
or a caller-selected client_id is not adequate authority.
