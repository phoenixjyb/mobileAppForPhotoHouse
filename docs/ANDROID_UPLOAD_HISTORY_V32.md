# Phone v25: my uploads and additional library names

Base: `bfcd7188f5fd091239df8673c4ce587465d85cd9`.
Branch: `codex/android-upload-history-v32`.
Version code 24, version name `0.25-upload-history`.

Signed-in members can check their own upload receipts, refresh manually and page
ten at a time. Pending and unavailable photos cannot be opened. Available photos
use current destination membership and the ordinary detail/media path, including
cross-library navigation. Closing, backgrounding and account/library changes clear
history and reject late replies. No polling, job retry, upload retry or new grant
is implied. Existing upload submission remains usable on older servers returning
403/404/503 for history. The anonymous TV feed has no private receipt endpoint.

The wire contract is backend candidate.24, source
`a33eb936a87660271cc4fe2663ffa1f9a36bfa31`, manifest SHA-256
`c06e54b772450099ee86e78e83128cf587ee70d2a0e8003ba17cfadf82a312f0`.
`GET /uploads?page=N` accepts no page_size parameter; the page size is fixed at ten.
The test resource preserves all nine synthetic backend history exchanges and
replays the four success bodies through the Android parser. Frozen `contracts/v1`
is unchanged. This is an additive protected-native opt-in client feature.

Library labels add Home Renovation / 装修 (`home-renovation`) and Expense Receipts /
报销单 (`expense-receipts`). Family remains the default, explicit selection persists
through refresh, and membership authorization remains required.

Validation: 244 live-core and 24 core tests; frozen-contract verification and 12
contract-script tests; phone debug build and lint; TV compile/build regression.
API 36 emulator checks cover the bilingual history screen, ten-plus-two paging,
approved-photo opening and Family default/manual switching. Phone history render
inspected. Configured APK preserves the prior 13 endpoint/feature settings.

The server candidate and new empty libraries need live activation/provisioning.
No physical-phone installation or authenticated production upload acceptance is
claimed by source, adapter, fixture, emulator or APK evidence. Resumable/video
uploads, story batches and audio transcription remain separate future work.
