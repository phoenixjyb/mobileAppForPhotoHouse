# TV playback diagnostics return — 12 September 2026

The user still cannot play video on the physical projector with TV v9. The
projector failure is **not resolved or reproduced** by this source change.

## Source and ownership

- Base: `db7d8344d922409b6b3db5b8e9511c1c02748685` (TV v9).
- Branch: `codex/android-tv-playback-diagnostics`, isolated Android worktree.
- TV v10 is a diagnostic build, with the existing streaming/player architecture.
- Public source contains no configured endpoint, real media, credentials or logs.
- No backend edits, restart, media publication, physical installation, push or merge.

## Confirmed defects addressed

The native error callback discarded both error integers and showed only a generic
message. It now displays a fixed bilingual explanation plus a numeric support
code. Setup, control and preparation-timeout failures have distinct fixed codes;
exception text, media IDs and URLs never enter the diagnostic model. A denied
audio-focus request now shows a retry hint rather than silently doing nothing.
The error clears on retry, source/library changes and background cover.

The error explanation uses the documented native error values from
[Android OnErrorListener](https://developer.android.com/reference/android/media/MediaPlayer.OnErrorListener).
Vendor-specific errors remain numeric; the app does not claim a codec diagnosis
from an unknown error. All bytes still pass through the existing strict HTTPS
Range adapter and memory-only source; the native decoder is never given a URL.

## Runtime evidence and remaining gate

A separate backend-owner read-only check found the current two published ready
videos intact, with H.264/AAC metadata and valid HEAD/Range responses in ASGI.
Native TLS requests from a non-approved operator peer were correctly denied.
Those checks do not prove successful requests from the projector. Access logging
is disabled. The projector debug port refused a bounded LAN connection attempt,
and there is no attached ADB device from the Mac. No device decoder log is available.

Only two videos are published ready; the rest of the catalog's videos remain
unprepared. Select a video explicitly labelled ready for the next device test.
An observed screen symptom or v10 support code is still needed to establish the
projector's root cause. A server restart is not justified by current evidence.

## Checks

Validation results and artifact hashes are recorded in `verification.json`.
The new regression opens malformed synthetic media through the real TV gallery,
checks visible EN/ZH diagnostics, retries with valid synthetic media, and verifies
source teardown and error reset. Existing catalog paging, native decoding, seek,
full-screen, replacement and background tests remain relevant regression checks.

This is an emulator/source qualification, not physical-TV or full-library acceptance.
The separate SMB task owns Windows share setup and its own validation/activation.
