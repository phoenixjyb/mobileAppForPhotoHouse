#!/usr/bin/env bash
# Synthetic component tests only, on an explicitly selected existing emulator.
set -euo pipefail
cd "$(dirname "$0")/.."
serial="${1:?Pass emulator-SERIAL}"
scale="${2:-1.0}"
evidence="${3:-docs/evidence/android/tv}"
[[ "$serial" == emulator-* && ( "$scale" == 1.0 || "$scale" == 2.0 ) ]] || exit 2
adb="${ANDROID_HOME:?}/platform-tools/adb"
[[ "$("$adb" -s "$serial" shell getprop ro.kernel.qemu | tr -d '\r')" == 1 ]] || exit 2
python3 - <<'PY'
from pathlib import Path
p=Path('android/tv/build/generated/source/buildConfig/debug/dev/photohouse/tv/BuildConfig.java')
assert 'PHOTOHOUSE_ORIGIN = "";' in p.read_text(), 'Use an unconfigured synthetic-test build'
PY
"$adb" -s "$serial" shell input keyevent KEYCODE_WAKEUP
"$adb" -s "$serial" shell wm dismiss-keyguard
previous="$("$adb" -s "$serial" shell settings get system font_scale | tr -d '\r')"
trap '"$adb" -s "$serial" shell settings put system font_scale "$previous" >/dev/null' EXIT
"$adb" -s "$serial" shell settings put system font_scale "$scale"
"$adb" -s "$serial" install -r -t android/tv/build/outputs/apk/debug/tv-debug.apk
"$adb" -s "$serial" install -r -t android/tv/build/outputs/apk/androidTest/debug/tv-debug-androidTest.apk
mkdir -p "$evidence/screenshots/$scale"
"$adb" -s "$serial" shell am instrument -w -r dev.photohouse.tv.test/androidx.test.runner.AndroidJUnitRunner | tee "$evidence/instrumentation-$scale.log"
python3 - "$evidence/instrumentation-$scale.log" <<'PY'
import re,sys
from pathlib import Path
s=Path(sys.argv[1]).read_text()
assert re.search(r'OK \(8 tests\)',s) and 'FAILURES!!!' not in s, 'TV component tests failed'
PY
for name in setup connection-needed grid-en grid-zh detail-en fullscreen covered display-caption denied empty; do
    "$adb" -s "$serial" exec-out run-as dev.photohouse.tv cat "files/$name.png" > "$evidence/screenshots/$scale/$name.png"
    "$adb" -s "$serial" shell run-as dev.photohouse.tv rm "files/$name.png"
done
