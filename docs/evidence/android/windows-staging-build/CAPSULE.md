# PH-ANDROID-WINDOWS-STAGING-BUILD-01

- Owner/executor: current Android task; bounded build and handoff, no delegation or model switch.
- Base: `90e296922e3c84b35a93abda7d6fc1d2c53808fb`.
- Branch: `codex/android-pipeline-readiness-02`; existing owned readiness worktree.
- Outcome: a private configured connected debug APK, focused offline build/lint,
  exact source/APK/signer identities and a redacted return.
- Read scope: mobile AGENTS and referenced design/contract/pilot documents,
  backend public Windows staging return and selected private operator handoff,
  existing local Android toolchain/cache. Backend source is read-only.
- Writes: ignored local Android origin/build/cache outputs, private artifact/log
  directory outside the repository, and this Android evidence directory.
- Preserve app implementation and frozen API pin
  `87a60b475b37b1d6873cd977bcb6e7254472da7e`; retain normal system TLS trust.
  No credentials are build inputs. Initial server original grants stay off.
- Acceptance: frozen checksum/source compatibility verification, connected boundary
  guard, `:connected:lintDebug :connected:assembleDebug` offline, packaged identity,
  signature verification, private origin match and no fixture assets in APK.
- Stop after verified private artifact or a concrete build blocker. No installation,
  device run, backend requests/mutations, credentials, routing, trust exceptions,
  new features, push, merge or deployment. Phone/operator and route remain pending.
