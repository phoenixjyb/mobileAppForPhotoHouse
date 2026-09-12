# Shared PhotoHouse Android icon

Base: `421ca8e077a511d1cbe65087d6dd892b2c7b31db`.
Branch: `codex/android-shared-brand-icon`. Source scope: shared branding resources,
three app manifests/build resource directories, TV banner, removed placeholder icons.
The preceding phone streaming work is retained by ancestry.

Validation: offline Gradle 8.10.2 / JDK 17 / SDK 34 assembleDebug + lintDebug for
app, connected and tv passed (157 tasks; clean app builds). All three have zero lint
errors. Existing SDK/Exif/API warnings remain, plus the explicitly documented absence
of a monochrome themed icon. Frozen contract and fixture/connected/TV boundary scripts
passed. Exact APK bytes/checksums and lint warnings are in validation.json.

Final connected and TV debug APKs installed on an owned API 36 phone emulator and
both MainActivity launches returned Status: ok. The launcher screenshot shows the
shared full-color adaptive icon. No physical phone/projector installation or live
server access. TV banner compiles but its projector launcher rendering is unverified.
A temporary shell-only banner-render attempt was killed and is not rendering evidence.
No media feature regression suite was rerun for this resource-only change.

APKs retain their current development version labels and empty origin configuration;
these are not the private plug-and-play TV builds. Local source commits only, with no
push, merge, deployment, service restart or caption/preparation changes.
