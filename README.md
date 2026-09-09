# PhotoHouse Mobile

Native Android and iOS frontends for a private family PhotoHouse library.

**Status: fixture contract frozen and app handoffs ready, 9 September 2026.**
Android and iOS can start from local tag `refs/tags/photohouse-mobile-fixture-v1`.
Neither app is implemented yet. The separate backend has locally tested invitation,
session, scoped browsing and recovery services; nothing here claims deployment.
Standalone Codex sessions have not been created by this handoff update.

Mobile source belongs in
[mobileAppForPhotoHouse](https://github.com/phoenixjyb/mobileAppForPhotoHouse).
API, authorization, library membership, web UI, voice orchestration, and media
services remain in [vlmPhotoHouse](https://github.com/phoenixjyb/vlmPhotoHouse).
Caption inference and face embeddings remain their existing separate runtimes.
GitHub hosts source and reviewed build artifacts, not the family photo service.

Start here:

- [Development plan](docs/DEVELOPMENT_PLAN.md): architecture, product scope, order,
  infrastructure, decisions, and acceptance gates.
- [Privacy and voice boundaries](docs/SECURITY_AND_VOICE.md): current gaps and
  required backend/mobile protections.
- [Frozen fixture contract](contracts/README.md): source-pinned OpenAPI subset,
  synthetic ASGI responses and native client scenarios; real networking disabled.
- [Session handoffs](docs/SESSION_CAPSULES.md): bounded backend, Android, iOS, and
  voice assignments with separate write ownership.
- [Platform parity](docs/PARITY.md): honest implementation and validation status.
- [Reference audit](docs/REFERENCE_AUDIT.md): observed source identities and
  reusable OpenGroove/ReCoMo patterns.

The central rule is **only an owner-approved or invited membership grants access**.
Your selected flow is manually sent phone-bound invitation plus phone/password.
Valid invited registration creates a viewer in that library; there is no open
signup or automatic access to existing photos. Every protected request rechecks
current membership, including direct image/video bytes.

Initial recommendations: Kotlin/Compose and SwiftUI; shared schemas and fixtures,
not a shared UI runtime; private-network HTTPS pilot; synthetic media until the
backend authorization gate passes; no persistent offline library in version one.
These are planning defaults, not claims of user-approved deployment choices.
