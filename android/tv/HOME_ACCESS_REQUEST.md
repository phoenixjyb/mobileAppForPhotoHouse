# Home TV access — settled and integrated locally

The user's selected policy is **no approval; share selected photos with any device
reaching the LAN feed**. There is no personal sign-in or one-time device approval.
The independent protected phone API remains unchanged.

The backend contract is implemented and frozen at runtime commit
`e6b2827842b2c0b5223c85208299b60e8a1257f6`; readiness follow-up
`90c2e2461f480c1ae49daab4ac83c629b44a2148` adds the deployment plan without changing
runtime/contract. Contract SHA-256:
`70328a653ddaa559bad6a4d654cf9870c89c5217e8e9e6c46a3501e9dd9e7548`.

Android consumes that contract through its separate home-core adapter and TV UI.
Automatic startup, revalidation, retry, privacy clearing and prepared 4K display
images are now locally implemented. See [TV README](README.md) and the
[current return](../../docs/evidence/android/home-feed/RETURN.md) for validation.
Earlier prototype notes saying the backend/adapter was pending are historical.

## Remaining backend/device handoff

- Explicit synthetic deployment operator/window and verified host/interface/port.
- Actual projector/home/guest peer ranges and isolation from public/proxy ingress.
- Normal client DNS and system-trusted HTTPS, certificate lifecycle and key ACLs.
- Separate task/runtime/publication identity, selected synthetic manifest and
  tested disable/remove/revision/rollback behavior. Prepared byte replacement
  uses a fresh publication and a stopped TV-service config switch, not a claimed
  atomic hot switch of independent file trees.
- Approved serving origin and exact service/package identities, then a private
  configured APK and separately authorized JMGO installation/acceptance.

No serving origin or real selection is approved. The older protected staging
origin is not a /home/v1 server. Public evidence and the current APK contain no
private origin, credentials or real media. No deployment, push or merge is implied.
