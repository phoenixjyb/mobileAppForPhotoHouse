# Phone v26: bounded photo and video batches

Base: `44e21897bf5a540c541a7a5530f7121465cb18db`.
Branch: `codex/android-batch-upload-v33`.
Version code 25, version name `0.26-batch-uploads`.

An authenticated member can select JPEG, PNG, MP4 or MOV files with the Android
document picker, or choose a folder through the Storage Access Framework. The app
reviews accepted and skipped files before starting, warns before using a metered
connection, and sends one file at a time. Selection accepts at most 100 files and
64 GiB per batch. Individual images are limited to 256 MiB and videos to 16 GiB.
The existing 25 MiB one-photo action remains for compatibility.

The app streams each original to calculate its SHA-256, then sends at most 4 MiB
per upload request. It stores account-scoped transfer metadata and persistable
document grants, not copies of the media. A transfer can be paused, retried or
cancelled. Reopening the app never starts a transfer automatically: the member
must tap Resume. The server confirms its committed offset before resumed chunks;
completion can be reconciled after a lost reply. Account changes and backgrounding
pause the queue. The member's existing upload history shows the server's pending
review and available receipts for both photo and video.

The wire contract is backend `2.0.0-candidate.25`, source
`b18f4bb59e3802e846ef401bbe108cf12bbe008f`, with schema revision
`a8d4c2e6f901`. It requires separate Windows migration and service activation.
The receiving server keeps new files private for owner review. A received video
does not become immediately prepared for playback or anonymous TV access.

Validation used synthetic media and an unconfigured emulator APK: live-core unit
tests, connected build/lint, TV build regression and the upload-queue instrumentation
screen passed. The configured phone debug APK retained the previous 13 endpoint
and feature profile fields. No authenticated Windows transfer or physical phone
installation is claimed here.
