#!/usr/bin/env python3
"""Source/package policy checks supplement behavioral TLS and privacy tests."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parent
ns = '{http://schemas.android.com/apk/res/android}'
manifest = ET.parse(root / 'connected/src/main/AndroidManifest.xml').getroot()
assert {p.get(ns + 'name') for p in manifest.findall('uses-permission')} == {'android.permission.INTERNET'}
app = manifest.find('application')
for name in ('allowBackup', 'fullBackupContent', 'usesCleartextTraffic'):
    assert app.get(ns + name) == 'false'
network = ET.parse(root / 'connected/src/main/res/xml/network_security_config.xml').getroot()
assert network.find('base-config').get('cleartextTrafficPermitted') == 'false'
assert [cert.get('src') for cert in network.iter('certificates')] == ['system']
assert network.find('debug-overrides') is None
for module in ('connected', 'live-core'):
    config = (root / module / 'build.gradle.kts').read_text()
    assert 'project(":core")' not in config and 'project(":app")' not in config
    for source in (root / module / 'src/main').rglob('*.kt'):
        text = source.read_text()
        for forbidden in ('FixtureRepository', 'SharedPreferences', 'SavedStateHandle', 'rememberSaveable', 'FileOutputStream', 'hostnameVerifier(', 'sslSocketFactory(', 'WebView(', 'HttpLoggingInterceptor'):
            assert forbidden not in text, f'{source.name}: unexpected {forbidden}'
activity = (root / 'connected/src/main/java/dev/photohouse/connected/MainActivity.kt').read_text()
assert 'FLAG_SECURE' in activity and 'model.store?.background()' in activity and 'model.store?.foreground()' in activity
print('PASS connected app system TLS trust, no fixture dependencies, no credential/media persistence or logging')
