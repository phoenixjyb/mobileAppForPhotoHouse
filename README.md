# PhotoHouse Mobile

Native Android and iOS frontends for a private family PhotoHouse library.

**Android status, 12 September 2026:** this repository contains the authenticated
phone media app and the separate home-TV v8 app, plus the original offline fixture
app. Phone and TV support photo/video viewing with separate access policies and
platform-specific controls. See [phone/TV capability parity](android/PHONE_TV_PARITY.md),
[phone validation](docs/evidence/android/phone-media-parity/RETURN.md), and
[TV v8 validation](docs/evidence/android/real-media-v8/RETURN.md).

The public verification lane builds unconfigured debug APKs for all three apps.
Private server-configured APKs remain outside Git. TV live-canary checks do not
establish complete library preparation or physical-projector acceptance; phone
protected discovery and some media-access parity remain outstanding.

The frozen `photohouse-mobile-fixture-v1` tag and foundation documents below are
historical starting points, not the current Android resume point. Existing iOS work
is not assessed by this Android integration. Runtime evidence remains scoped to
the exact service and artifact described in each validation return.

Mobile source belongs in
[mobileAppForPhotoHouse](https://github.com/phoenixjyb/mobileAppForPhotoHouse).
API, authorization, library membership, web UI, voice orchestration, and media
services remain in [vlmPhotoHouse](https://github.com/phoenixjyb/vlmPhotoHouse).
Caption inference and face embeddings remain their existing separate runtimes.
GitHub hosts source and reviewed build artifacts, not the family photo service.

Start here:

- [Development plan](docs/DEVELOPMENT_PLAN.md): architecture, product scope, order,
  infrastructure, decisions, and acceptance gates.
- [Privacy and voice boundaries](docs/SECURITY_AND_VOICE.md): foundation security
  design and required backend/mobile protections.
- [Frozen fixture contract](contracts/README.md): source-pinned OpenAPI subset,
  synthetic ASGI responses and native client scenarios; no live origin in the pack.
- [Foundation session handoffs](docs/SESSION_CAPSULES.md): historical starting scopes
  and ownership; current Android work starts from the MVP status above.
- [Foundation parity record](docs/PARITY.md): coordinator-owned baseline; current
  Android evidence is linked above.
- [Reference audit](docs/REFERENCE_AUDIT.md): observed source identities and
  reusable OpenGroove/ReCoMo patterns.

The central rule is **only an owner-approved or invited membership grants access**.
Your selected flow is manually sent phone-bound invitation plus phone/password.
Valid invited registration creates a viewer in that library; there is no open
signup or automatic access to existing photos. Every protected request rechecks
current membership, including direct image/video bytes.

The current Android build is debug-only, keeps sign-in/media in memory and has an
unset server origin. The approved next delivery step is still to be established:
a reviewed served HTTPS backend, synthetic audience/library and exact configured
artifact, followed by separately authorized physical-phone acceptance. Source,
local tests and DNS/certificate preparation do not establish that live pipeline.
