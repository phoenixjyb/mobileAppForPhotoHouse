#!/usr/bin/env bash
# Runs only against an explicitly selected, already running emulator. Never downloads an image.
set -euo pipefail
repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_dir"
serial="${1:?Usage: android/verify-connected-emulator.sh emulator-SERIAL normal|large [evidence-directory]}"
size="${2:-normal}"
evidence_dir="${3:-docs/evidence/android/connected}"
[[ "$serial" == emulator-* ]] || { echo 'Only emulator serials are allowed.' >&2; exit 2; }
[[ "$size" == normal || "$size" == large ]] || exit 2
adb_bin="${ANDROID_HOME:?Set ANDROID_HOME to the existing SDK}/platform-tools/adb"
[[ "$("$adb_bin" -s "$serial" shell getprop ro.kernel.qemu | tr -d '\r')" == 1 ]] || { echo 'Target is not an emulator.' >&2; exit 2; }
app_apk=android/connected/build/outputs/apk/debug/connected-debug.apk
test_apk=android/connected/build/outputs/apk/androidTest/debug/connected-debug-androidTest.apk
[[ -f "$app_apk" && -f "$test_apk" ]] || { echo 'Build :connected:assembleDebug and :connected:assembleDebugAndroidTest first.' >&2; exit 2; }
previous_scale="$("$adb_bin" -s "$serial" shell settings get system font_scale | tr -d '\r')"
trap '"$adb_bin" -s "$serial" shell settings put system font_scale "$previous_scale" >/dev/null' EXIT
if [[ "$size" == large ]]; then scale=2.0; else scale=1.0; fi
"$adb_bin" -s "$serial" shell settings put system font_scale "$scale"
"$adb_bin" -s "$serial" install -r -t "$app_apk"
"$adb_bin" -s "$serial" install -r -t "$test_apk"
mkdir -p "$evidence_dir/screenshots/$size"
"$adb_bin" -s "$serial" shell am instrument -w -r dev.photohouse.connected.test/androidx.test.runner.AndroidJUnitRunner | tee "$evidence_dir/instrumentation-$size.log"
python3 - "$evidence_dir/instrumentation-$size.log" <<'PY'
from pathlib import Path
import re, sys
text = Path(sys.argv[1]).read_text()
assert re.search(r'OK \(17 tests\)', text), 'Instrumentation did not pass all seventeen tests'
PY
for name in unconfigured-en unconfigured-zh admission-en literal-caption-en literal-caption-zh photo-navigation-en photo-navigation-zh original-photo-en original-photo-zh video-en video-zh video-en-frame video-zh-frame libraries-en libraries-zh gallery-en gallery-zh detail-en detail-zh settings-zh parity-photo-fullscreen parity-photo-slideshow video-parity-fullscreen video-parity-fullscreen-frame video-long-en video-long-en-frame discovery-en discovery-zh; do
  "$adb_bin" -s "$serial" exec-out run-as dev.photohouse.connected cat "files/$name.png" > "$evidence_dir/screenshots/$size/$name.png"
  "$adb_bin" -s "$serial" shell run-as dev.photohouse.connected rm "files/$name.png"
done
printf 'PASS 17 emulator component UI tests; font scale %s; synthetic own-View renders retained.\n' "$scale"
