# PhotoHouse shared Android mark

The user supplied `photoHouseAppIconRef.png` for the application and web identity.
`res/drawable-nodpi/photohouse_mark.png` preserves its 1254 × 1254 PNG bytes unchanged.
SHA-256: `c8fc559e39c5c4d0b9902cafca47e15ac4e6cfa2cbc18d81a40d956e8a8f8909`.

The fixture, protected phone and home-TV applications include this resource directory
through their main resource source set. An adaptive icon uses a white background and
18dp foreground inset to preserve the supplied family/photo/video artwork under launcher
masks. The TV banner places the same mark on a warm 320 × 180dp background without
stretching it. This does not introduce a library or media dependency between apps.

The current icon is full color. A separately designed monochrome themed-icon asset is
not supplied; lint reports MonochromeLauncherIcon. Do not use the opaque source PNG as
a monochrome mask, which would produce a solid square.

Artwork is application branding, not library media. This directory contains no endpoint,
account, credential or signing input. The web repository uses the same source checksum.
