# PH-ANDROID-TV-PLAYBACK-01

Base: `b2fcdadafb070c32c3d7a8657eb537043ad1acbb`, branch
`codex/android-tv-foundation`. One Android writer; backend remains a separate owner.

User outcome: browse the whole home photo/video library without sign-in; view
photos with fit/fill, zoom/pan and full screen; explicitly play/pause/seek video.
The prior selected-photo/latest-100 limitation is superseded as product scope.

## Verifiable steps

1. Add photo transforms and D-pad interaction to the existing v1 viewer, retaining
   its bounded prepared JPEG decode, lifecycle clearing and private LAN routing.
2. Adapt the owned phone native-player architecture into an independent TV
   component, with a cancellable source interface and synthetic-only test media.
   Prove decoded frames, playback, seeking, full-screen surface preservation,
   native error cleanup and background close. No network/video URL is invented.
3. After the backend owner freezes the separately versioned catalog/video return,
   verify its exact source and input hashes, then add bounded adapter/store/UI
   integration. Preserve v1 and the protected phone pin. Missing/unprepared media
   must remain explicit states rather than fallback reads.
4. Record builds, focused tests and synthetic emulator evidence independently of
   a private configured APK and real projector acceptance. Keep existing APKs.

Write scope: `android/**`, `docs/evidence/android/**`. No backend writes, original
media copying, database changes, public ingress, push or merge. Full-library data
and video transport integration remain pending the backend's frozen return, not
satisfied by compiling a player component.
