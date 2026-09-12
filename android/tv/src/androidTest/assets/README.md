# Synthetic video fixture

`synthetic-video.mp4` is generated from FFmpeg mathematical test patterns and a
440 Hz tone. It contains no camera capture, people or private recording. It is
packaged only in the instrumentation APK; the application APK has no assets.
This byte-identical fixture is reused from the owned connected-phone tests.
SHA-256: `efb71000b8dc2807396c0d342dd7efe8823d4281e125b9281b980ddf1ae2f9b0`.

Generated using the already installed FFmpeg, without downloads:

```sh
ffmpeg -hide_banner -loglevel error \
  -f lavfi -i testsrc2=size=320x180:rate=15 \
  -f lavfi -i sine=frequency=440:sample_rate=44100 -t 20 \
  -c:v libx264 -profile:v baseline -pix_fmt yuv420p -g 15 -crf 26 \
  -c:a aac -b:a 32k -movflags +faststart synthetic-video.mp4
```

This is a 20-second, 320×180, H.264/AAC MP4; general device/codec performance and
physical speaker output are separate acceptance gates. No FFmpeg code or runtime
is included in either Android APK.

`catalog-video.mp4` is the byte-identical half-second 320×180 H.264/AAC synthetic
fixture from backend runtime `a5d0f595d7cd26379ed2845a944ec1d58d7885cc`.
SHA-256: `5ab7d5d27cc6b3f557068ce21cc07e332939ae92f8240233b5eb087c1012afe8`.
It proves exact catalog fixture integration; the twenty-second clip above is used
for the longer native seek/playback checks.
