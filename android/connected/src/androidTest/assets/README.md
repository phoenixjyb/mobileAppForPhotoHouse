# Synthetic video fixture

`synthetic-video.mp4` is generated from FFmpeg mathematical test patterns and a
440 Hz tone. It contains no camera capture, people or private recording. It is
packaged only in the instrumentation APK; the application APK has no assets.
The same file is copied into disposable backend test storage for Range tests.

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


`synthetic-long-video.mp4` uses the same mathematical source and codec settings,
with `-t 125 -threads 1`. It is a 125-second fast-start H.264/AAC MP4 used only in
instrumentation to verify preparation before full download, seeks beyond one
minute, near-end/backward seeks, playback and lifecycle cancellation. It is not a
multi-gigabyte performance benchmark. Large offsets are tested separately with
bounded synthetic Range responses; no multi-gigabyte file is allocated.
