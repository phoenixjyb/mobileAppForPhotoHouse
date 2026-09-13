# PhotoHouse Mobile working agreements

Use the assigned capsule when continuing a task. Read `contracts/README.md` for
shared interface/fixture changes, `docs/SECURITY_AND_VOICE.md` for authorization or
voice, and `docs/DEVELOPMENT_PLAN.md` for ownership/parity planning. A small local
change does not require every overview document.

## Ownership

- Coordinator owns shared contracts, fixtures, root configuration, cross-platform
  design decisions, parity, and integration.
- Android session owns android/** and its specifically assigned scripts/workflow.
- iOS session owns ios/** and its specifically assigned scripts/workflow.
- Backend security and voice work belongs in the separate vlmPhotoHouse repo,
  in different worktrees. Voice depends on the authorization foundation.
- One writer per worktree. Do not edit another session's checkout or silently
  change shared schemas. Return contract changes to the coordinator.
- Use route-codex-work when preparing or transferring bounded work capsules.

## Privacy and external state

- This repository is public. Never commit credentials, private hostnames or
  addresses, account records, invitation tokens, signing assets, real photos,
  voice recordings, biometric embeddings, databases, or model weights.
- Use synthetic fixtures for offline tests. A fixture authorization state is not
  real access control. Fixture-only slices keep networking disabled; an authorized
  real-networking slice follows its current contract and access controls.
- Keep anonymous selected-media TV access separate from authenticated protected
  phone/library access. Do not reuse public-media exceptions for private libraries.
- Registration without a valid owner-issued, phone-bound invitation is denied.
  Valid invited registration grants viewer membership only in that invited library;
  no open signup or implicit household access. No debug/localhost/VPN bypass may
  enter the protected production API.
- Do not copy application identifiers, signing configuration, runtime endpoints,
  cleartext exceptions, or public-media caching policies from reference apps.
- Check source licenses before copying reference implementation code; borrow
  architecture patterns without importing unrelated projects or dependencies.
- No service changes, provider setup, model downloads, public ingress, device
  installation, store submission, production signing, push, or merge without
  authorization for that operation. A design task is not a deployment request.
- Keep user-owned edits intact. Never reset, clean, stash, or discard them.

A requested fix includes the local edit/check/repair loop within the assigned
scope. Use affected platform checks; broaden to parity checks for shared changes.
Routine choices within the capsule do not require a coordinator round trip.

## Evidence

Record base SHA, branch, changed paths, checks, skipped checks, and next gate.
Keep source, unit tests, simulator/emulator, signed artifacts, installed builds,
live authenticated service, and family acceptance separate. Update parity only
with observed evidence. Never claim that a saved handoff created a Codex session.
