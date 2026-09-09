#!/usr/bin/env bash
set -euo pipefail
repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_dir"
python3 scripts/verify-contracts.py
python3 android/verify-fixture-boundaries.py
if [[ -z "${JAVA_HOME:-}" && "$(uname -s)" == Darwin ]]; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
fi
java_bin="${JAVA_HOME:+$JAVA_HOME/bin/}java"
java_version="$("$java_bin" -version 2>&1)"
if [[ "$java_version" != *'version "17.'* ]]; then
  echo 'JDK 17 required. Set JAVA_HOME to an already installed JDK 17.' >&2
  exit 2
fi
sdk_dir="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$sdk_dir" || ! -f "$sdk_dir/platforms/android-34/android.jar" || ! -d "$sdk_dir/build-tools/34.0.0" ]]; then
  echo 'Existing SDK platform 34 and build-tools 34.0.0 required. No SDK installation is performed.' >&2
  exit 2
fi
printf '%s\n' "$java_version"
android/gradlew -p android --version
android/gradlew -p android :core:test :app:lintDebug :app:assembleDebug --console=plain "$@"
python3 - <<'PY'
import hashlib, pathlib, zipfile, xml.etree.ElementTree as ET
root = pathlib.Path('.')
apk = root / 'android/app/build/outputs/apk/debug/app-debug.apk'
with zipfile.ZipFile(apk) as z:
    for p in (root/'contracts/v1').rglob('*'):
        if p.is_file():
            name = 'assets/' + p.relative_to(root/'contracts/v1').as_posix()
            assert z.read(name) == p.read_bytes(), f'Bundled fixture drift: {name}'
print('PASS APK bundles the shared contract and media byte-for-byte')
print('APK SHA-256:', hashlib.sha256(apk.read_bytes()).hexdigest())
reports = sorted((root/'android/core/build/test-results/test').glob('TEST-*.xml'))
assert reports, 'No JVM test reports'
for p in reports:
    s = ET.parse(p).getroot()
    assert s.attrib['failures'] == '0' and s.attrib['errors'] == '0', p
    print(f"JVM: {s.attrib['tests']} tests, {s.attrib['failures']} failures, {s.attrib['errors']} errors")
PY
"$sdk_dir/build-tools/34.0.0/aapt" dump permissions android/app/build/outputs/apk/debug/app-debug.apk > android/app/build/outputs/apk/debug/permissions.txt
python3 - <<'PY_PERMISSIONS'
from pathlib import Path
import re
text = Path('android/app/build/outputs/apk/debug/permissions.txt').read_text()
permissions = set(re.findall(r"uses-permission: name='([^']+)'", text))
assert permissions <= {'dev.photohouse.fixture.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'}, permissions
print('PASS APK has no Internet, storage, camera or microphone permissions; only AndroidX app-internal signature permission')
PY_PERMISSIONS
