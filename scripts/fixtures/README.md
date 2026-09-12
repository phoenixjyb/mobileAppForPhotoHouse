# Fixed synthetic backend replay image

`replay-8x8.jpg` is fixture-only input for the in-process contract replay. It is
not family media, a production asset, or a change to `contracts/v1`.

- Input: 8 x 8 RGB pixels, every pixel `(160, 180, 140)`.
- Encoding: Pillow 12.1.1, reported JPEG codec 6.2, default JPEG save settings.
- Length: 632 bytes.
- SHA-256: `c311363ddcc33e304b4f657d7fb3353c839bcdbdb9bdbd09bea62b1152fac42d`.

Generated once with the already installed historical synthetic-test Pillow
interpreter. It reproduces the previous `Image.new('RGB', (8, 8), (160, 180, 140))`
and default `.save(...jpg)` operations. Two independent saves were byte-identical;
the 632-byte result matches both frozen original and thumbnail response lengths.
The frozen HEAD lengths remain zero and the Range result remains six bytes.

The harness validates exact length and SHA-256 before using the same immutable
bytes for temporary original/thumbnail files. Normal validation/replay uses only
stdlib file I/O; Pillow is neither imported nor added to a runtime/test lock.
Never regenerate this asset to accommodate an unexpected response difference.
