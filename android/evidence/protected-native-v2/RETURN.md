# Protected phone access and Family Stories

Android-owned local implementation, 2026-09-16. Base
`b35ce77b0cdfaa9ca30640497a32d8dd1bc914e5`, branch
`codex/android-protected-stories-20260916`. No backend source/runtime change,
real credentials, physical-device installation, SMB distribution, push or merge.

## Change

An explicit, default-off current-server profile accepts login passwords of 1–128
Unicode code points and invited-registration passwords of 8–128, without trimming.
The editable phone field starts with +86. The frozen v1 profile retains its
15–128 admission rule and `contracts/v1` is unchanged.

Authenticated asset details can read Family Stories in pages of five, independently
of AI captions. Literal bilingual text, author byline, empty/error/retry/refresh
states and a separate long-story reader are implemented. There are no story writes,
history/search, audio, upload or model requests. Story bodies are capped at 3 MiB;
the existing 512 KiB cap remains for other JSON. Wrong scopes, duplicate fields,
malformed Unicode and invalid fields fail closed.

Stories stay in memory and clear on background, navigation, library/account change,
logout, expiry and authorization failures. Generation checks reject late responses.
Home/TV transport remains separate. Existing source support for protected display
is tested synthetically but is **off in the configured live-target APK** because
the integration owner reports no production PhotoCache configuration. Browsing
uses available protected thumbnails and reports unavailable previews honestly.
No original-media capability is granted or inferred by the client.

## Contract identity

- Backend pack commit: `97c5d620b2eaf8bc5e50c48d3d8c5985bec8396d`.
- Backend application source: `4022a57f56e6b2f976931a20569e15c879871d93`.
- Manifest SHA-256: `3c222c9525f89ff753353b17e7741e200d16061cd042df5afbd03618ed42ce3e`.
- Case SHA-256: `cb16cb611530fe4bd4d425d9db1c539543c298666e394a3ca81d5866c441b1d8`.
- Migration: `d8e5b2f7a904`; version `2.0.0-candidate.1`.

Coordinator explicitly adopted this exact identity for the bounded Android profile.
The upstream candidate status and all snapshot bytes remain unchanged. The snapshot
contains 60 synthetic ASGI exchanges, including broader backend capabilities that
are not Android feature claims. The offline verifier checks the snapshot and can
verify the backend's 98 source and 7 payload hashes. Named captured responses are
also consumed by Kotlin compatibility tests; these do not reproduce a live server.

## Build profile

All feature switches default off. `photohouseProtectedNativeV2Enabled` bundles
current admission rules and read-only Stories. `photohousePhonePhotoDeliveryEnabled`
is separate. A configured local debug APK uses the new profile with photo delivery,
protected discovery and offline Story Fixture disabled, retaining the previous
explicit Home origin/LAN route and Home discovery/tag/calendar features. Origins
and private LAN settings are build inputs only, omitted from public source.

The private handoff records the exact source commit, APK SHA-256, generated
BuildConfig and signing certificate. App ID remains `dev.photohouse.connected`,
version code 10, version name `0.11-protected-stories`. This is not a store release.

## Verification and next gates

See `verification.json` for final checks. Screenshots are synthetic fixtures only.
TLS adapter tests use an ephemeral local test certificate and no family credentials.
Normal and large-font emulator evidence is separate from physical-phone acceptance.

The next device gate is an explicitly authorized install and private user-entered
sign-in/invitation, followed by library, thumbnail, story paging, background and
logout checks. Do not place credentials or real-media screenshots in this repository.
Production display readiness needs backend-owned renderer configuration and its
own validation before enabling that switch.

The next contribution feature needs a separately reviewed upload capability,
resumable transfer/finalization protocol, account/date incoming layout and durable
ingestion events. Audio requires speech-to-text (not text-to-speech), local resource
admission and human review of optional polish proposals. Original audio/text,
transcripts and accepted story revisions must remain distinct. No upload endpoint
or automatic pipeline execution is implied by this read-only client change.
