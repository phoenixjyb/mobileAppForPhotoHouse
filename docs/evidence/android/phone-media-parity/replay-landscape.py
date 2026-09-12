#!/usr/bin/env python3
"""Run three media checks on an existing owned emulator, restoring display settings."""
import os
from pathlib import Path
import re
import subprocess
import sys

serial = sys.argv[1]
assert re.fullmatch(r"emulator-\d+", serial), "Emulator only"
adb = Path(os.environ["ANDROID_HOME"]) / "platform-tools/adb"
def call(*args):
    return subprocess.run([str(adb), "-s", serial, *args], check=True, capture_output=True, timeout=180).stdout

assert call("shell", "getprop", "ro.kernel.qemu").strip() == b"1"
keys = ("font_scale", "accelerometer_rotation", "user_rotation")
previous = {key: call("shell", "settings", "get", "system", key).decode().strip() for key in keys}
root = Path(__file__).resolve().parent
methods = (
    "photoFitFillFullscreenAndSlideshowPreservePageAndClearOnBackground",
    "nativeVideoFitFillAndFullscreenKeepTheSameReaderAndRestoreControls",
    "originalViewerZoomCloseAndPrivacyUseSyntheticImageBytes",
)
try:
    for key, value in zip(keys, ("2.0", "0", "1")):
        call("shell", "settings", "put", "system", key, value)
    result = call("shell", "am", "instrument", "-w", "-r", "-e", "class",
                  ",".join("dev.photohouse.connected.ConnectedUiTest#" + method for method in methods),
                  "dev.photohouse.connected.test/androidx.test.runner.AndroidJUnitRunner")
    (root / "instrumentation-landscape-2.0.log").write_bytes(result)
    assert re.search(rb"OK \(3 tests\)", result), "Landscape media tests failed"
    output = root / "screenshots/landscape-2.0"
    output.mkdir(parents=True, exist_ok=True)
    for name in ("parity-photo-fullscreen", "parity-photo-slideshow", "original-photo-en", "original-photo-zh",
                 "video-parity-fullscreen", "video-parity-fullscreen-frame"):
        image = call("exec-out", "run-as", "dev.photohouse.connected", "cat", "files/" + name + ".png")
        assert image.startswith(b"\x89PNG\r\n\x1a\n"), "Missing synthetic render"
        (output / (name + ".png")).write_bytes(image)
        call("shell", "run-as", "dev.photohouse.connected", "rm", "files/" + name + ".png")
finally:
    for key, value in previous.items():
        call("shell", "settings", "put", "system", key, value)
print("PASS 3 landscape media checks at 2x font; prior runtime settings restored")
