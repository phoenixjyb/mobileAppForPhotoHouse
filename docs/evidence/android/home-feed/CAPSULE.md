# PH-ANDROID-HOME-FEED-01

Owner/executor: current Android task, unchanged model; bounded integration.
Base: 8ca5f20e95aec9fb8b789f3443c3a074f2de19cf, codex/android-tv-foundation.
Write scope: android/** and docs/evidence/android/** in this existing owned worktree.
Read scope: mobile source and explicit backend home-feed return/source/fixtures.
Plan: verify pins; implement independent TV adapter/store and UI; validate transport,
lifecycle, 4K decode, remote controls and build; commit source and evidence locally.
Phone API/pin unchanged. Consume the frozen anonymous feed; no credentials, pairing,
original fallback, disk media or trust bypass. Synthetic inputs only.
Acceptance: contract hashes, focused JVM tests and existing regressions, source
boundaries, TV lint/APKs and available emulator EN/ZH/large-font checks.
Stop gates: schema/ownership changes, deployment, real data/credentials, physical
installation, push or merge. Backend owns later separately authorized LAN serving.
