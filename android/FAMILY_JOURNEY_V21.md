# Android family journey v21

Local source slice based on playback-recovery v20 (`3e4e907`).
This is not a deployed feature or physical-device acceptance record.

## Playback

Phone account mode, phone Home mode and TV now have explicit previous/next
video controls within the current result page. Photos are skipped; Home also
skips videos that lack a playable representation. Protected navigation reloads
asset authorization and obtains a fresh prepared-media HEAD. It does not grant
original access or cross a page boundary.

A shared pure Kotlin playback module retains up to 100 progress records in
memory. Protected records are scoped by the store's origin/session, account,
library, asset and prepared ETag; Home records use the store's independent
source, feed revision, asset and representation hash. Old callbacks cannot
write after their bookmark closes. Logout, session denial and explicit library
exit clear protected history; Home disconnect/denial/revision change clears it.
The records survive reopening a video during the same app session, not process
death. They contain no media bytes and are never written to disk or synced.

Opening a video never starts audio. Continue is an explicit action, with audio
focus and a fresh seek. Completed and very short videos restart at zero.
Original protected playback without a representation identity has no bookmark.

## Discovery

Existing phone and Home/TV people/date/tag/place filtering is retained. The
protected phone adds a date picker and combined filter summary; manual ISO date
entry and validation remain. Reviewed IDs, stale binding denial and explicit
reapply are unchanged. Themes, named albums and map-radius queries remain absent.

The backend's new offline `--review` producer can consume reviewed aliases/pins,
manual face assignments and place regions bound to a library and current source
digest. Caption/tag use requires explicit declarations. Defaults remain only
date/media. A real reviewed input, publication and runtime enablement are still
required; this change does not invent family-to-person mappings or approve faces.

## Native photo contribution

`photohousePhoneUploadEnabled=true` is separate from browsing and requires
`photohouseProtectedNativeV2Enabled=true`. Defaults remain off. This consumes the
existing candidate15 `POST /uploads` source contract as an opt-in extension; it
adds no endpoint or backend permission. Any currently approved member can submit
to their own incoming area, which is not a visible library.

- One user-selected JPEG/PNG, known size up to 25 MiB, streamed in 32 KiB buffers.
- Android document picker grants access to that URI; no broad photo/storage
  permission, persistent URI grant or app media cache is created.
- Picker results wait for foreground account reauthorization before metadata or
  bytes are opened. Metadata failure asks the user to choose again.
- Metered or unknown network requires confirmation; explicit retry rechecks the
  current network and honors server cooldown. Wi-Fi is not inferred to be free.
- Progress reflects request bytes, not processing. Cancellation/close/background
  stop the client request; a server may already have accepted it. No automatic
  write retry or promise of remote deletion is made.
- Only HTTP 201 with strict JSON and matching streamed SHA-256/size is success.
  Receipt does not grant access to the asset or mean tasks completed. Same-account
  duplicate receipts can identify an earlier batch. Auth denial clears the
  account screen; TLS/malformed responses do not offer retry.
- No background/durable queue or byte-range resume. Retry is a whole-file resend.
  A bounded two-minute upload deadline leaves gallery read timeouts unchanged.

The backend still lacks video/chunked uploads, batch story linking, audio/STT,
per-item processing status and member promotion APIs. Existing asset memories
remain available after an upload has been reviewed and placed in a library.
These are remaining implementation gaps, not capabilities supplied by this APK.

## Delivery

The configured Phone v21 candidate enables prepared browsing, discovery and
photo contribution for later qualification. It requires candidate15 activation,
a reviewed discovery index and a qualified incoming root. It is not a rollout
build for the currently unqualified server. TV v21 uses the existing Home API.
No service restart, upload, index publication, real-media test, phone/projector
installation, push or merge is part of this source slice.
