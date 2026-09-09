# PhotoHouse Mobile

Native Android and iOS frontends for a private family PhotoHouse library.

**Status: design and handoff only, 9 September 2026.** No application, account
system, API contract implementation, CI workflow, or live deployment is delivered
by this initial planning commit. Standalone Codex sessions have not been created.

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
- [Contract proposal](contracts/README.md): shared semantics, not a live API spec.
- [Session handoffs](docs/SESSION_CAPSULES.md): bounded backend, Android, iOS, and
  voice assignments with separate write ownership.
- [Platform parity](docs/PARITY.md): honest implementation and validation status.
- [Reference audit](docs/REFERENCE_AUDIT.md): observed source identities and
  reusable OpenGroove/ReCoMo patterns.

The central rule is **registered identity does not imply access to a library**.
An owner-approved or invited membership is required, enforced by the backend on
every protected operation, including direct image and video bytes.

Initial recommendations: Kotlin/Compose and SwiftUI; shared schemas and fixtures,
not a shared UI runtime; private-network HTTPS pilot; synthetic media until the
backend authorization gate passes; no persistent offline library in version one.
These are planning defaults, not claims of user-approved deployment choices.
