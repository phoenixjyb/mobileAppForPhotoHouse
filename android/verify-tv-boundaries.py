#!/usr/bin/env python3
"""Source checks for TV isolation; not a runtime/security acceptance claim."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parent
main = root / 'tv/src/main'
a = '{http://schemas.android.com/apk/res/android}'
manifest = ET.parse(main / 'AndroidManifest.xml').getroot()
assert [p.get(a+'name') for p in manifest.findall('uses-permission')] == ['android.permission.INTERNET']
features = {e.get(a+'name'): e.get(a+'required') for e in manifest.findall('uses-feature')}
assert features == {'android.software.leanback': 'false', 'android.hardware.touchscreen': 'false', 'android.hardware.faketouch': 'false'}
app = manifest.find('application')
for flag in ['allowBackup', 'fullBackupContent', 'usesCleartextTraffic']:
    assert app.get(a+flag) == 'false'
activity = app.find('activity')
assert activity.get(a+'screenOrientation') == 'landscape'
categories = {e.get(a+'name') for e in activity.findall('intent-filter/category')}
assert categories == {'android.intent.category.LAUNCHER', 'android.intent.category.LEANBACK_LAUNCHER'}
for name in ['network_security_config.xml', 'data_extraction_rules.xml']:
    assert (main/'res/xml'/name).read_bytes() == (root/'connected/src/main/res/xml'/name).read_bytes()
source = '\n'.join(p.read_text() for p in (main/'java').rglob('*.kt'))
for forbidden in ['SharedPreferences', 'rememberSaveable', 'SavedStateHandle', 'FileOutputStream', 'WebView', 'hostnameVerifier', 'sslSocketFactory', 'android.util.Log', 'java.io.File', 'MediaStore', 'dev.photohouse.fixture', 'OutlinedTextField', 'store.authenticate(']:
    assert forbidden not in source, forbidden
for required in ['FLAG_SECURE', 'store?.background()', 'store?.foreground()', 'TrustedOrigin.parse', 'detailPreviewSize = 1024', 'state.detail', 'originals_allowed']:
    assert required in source, required
build = (root/'tv/build.gradle.kts').read_text()
assert 'photohouseTvOrigin' in build and 'project(":live-core")' in build
assert 'project(":core")' not in build and 'project(":app")' not in build
assert 'it.enable = false' in build
print('PASS TV launcher/no-touch, system TLS, privacy lifecycle, no fixture or persistent storage, separate origin and debug-only build')
