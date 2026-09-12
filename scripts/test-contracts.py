#!/usr/bin/env python3
"""Focused regressions for shared handoff drift/type failures; stdlib only."""
from contextlib import redirect_stdout
import copy
import importlib.util
import io
import json
from pathlib import Path
import shutil
import tempfile
import unittest
from unittest.mock import patch

spec=importlib.util.spec_from_file_location('contract_check',Path(__file__).with_name('verify-contracts.py'))
check=importlib.util.module_from_spec(spec);spec.loader.exec_module(check)


class ContractChecks(unittest.TestCase):
    def test_frozen_contract_and_scenario_links(self):
        with redirect_stdout(io.StringIO()): check.verify()

    def test_replay_image_identity_and_frozen_response_lengths(self):
        data=check.replay_image_bytes()
        self.assertEqual(len(data),632)
        self.assertEqual(data[:2],b'\xff\xd8');self.assertEqual(data[-2:],b'\xff\xd9')
        lengths={c['id']:c['byte_length'] for c in check.read('fixtures.json')['cases'] if 'byte_length' in c}
        self.assertEqual(lengths,{'thumbnail':632,'thumbnail-head':0,'original-authorized':632,'original-range':6,'original-head':0})

    def test_replay_image_missing_truncated_or_same_length_mutation_is_refused(self):
        original=check.replay_image_bytes()
        with tempfile.TemporaryDirectory(prefix='photohouse-replay-image-') as tmp:
            image=Path(tmp)/'image.jpg'
            with patch.object(check,'REPLAY_IMAGE',image):
                with self.assertRaises(FileNotFoundError):check.replay_image_bytes()
                for bad in (original[:-1],original[:100]+bytes([original[100]^1])+original[101:]):
                    image.write_bytes(bad)
                    with self.assertRaisesRegex(AssertionError,'Replay image identity differs'):check.verify()

    def test_backend_replay_refuses_bad_image_before_backend_imports(self):
        with patch.object(check.subprocess,'check_output',return_value=check.BACKEND_SHA+'\n'), patch.object(check,'sha',side_effect=lambda path:check.read('manifest.json')['backend_sources'][str(path.relative_to('/unused-synthetic-backend'))]):
            with patch.object(check,'replay_image_bytes',side_effect=AssertionError('Replay image identity differs')):
                with self.assertRaisesRegex(AssertionError,'Replay image identity differs'):
                    check.backend_cases(Path('/unused-synthetic-backend'))

    def test_external_io_is_guarded_after_git_identity_checks(self):
        import os,socket,subprocess
        def guarded(_):
            with socket.socket() as probe:
                with self.assertRaisesRegex(AssertionError,'External I/O forbidden'):probe.bind(('127.0.0.1',0))
                with self.assertRaisesRegex(AssertionError,'External I/O forbidden'):probe.connect(('127.0.0.1',9))
            with self.assertRaisesRegex(AssertionError,'External I/O forbidden'):subprocess.Popen(['unused-synthetic-command'])
            with self.assertRaisesRegex(AssertionError,'External I/O forbidden'):os.system('unused-synthetic-command')
            return 'guarded'
        with patch.object(check.subprocess,'check_output',return_value=check.BACKEND_SHA+'\n'), patch.object(check,'sha',side_effect=lambda path:check.read('manifest.json')['backend_sources'][str(path.relative_to('/unused-synthetic-backend'))]):
            with patch.object(check,'_guarded_backend_cases',side_effect=guarded):
                self.assertEqual(check.backend_cases(Path('/unused-synthetic-backend')),'guarded')

    def test_missing_invitation_field_is_not_a_registration_request(self):
        api=check.read('openapi.json')
        with self.assertRaises(AssertionError):
            check.validate({'phone':'+12025550199','password':'synthetic password only','transport':'native'},api['components']['schemas']['RegisterRequest'],api)

    def test_available_is_boolean_and_asset_id_is_lossless_string(self):
        api=check.read('openapi.json');cases={c['id']:c for c in check.read('fixtures.json')['cases']}
        member=copy.deepcopy(cases['approved']['body']);member['memberships'][0]['available']='true'
        with self.assertRaises(AssertionError):check.validate(member,api['components']['schemas']['Session'],api)
        detail=copy.deepcopy(cases['detail']['body']);detail['asset']['id']=101
        with self.assertRaises(AssertionError):check.validate(detail,api['components']['schemas']['Detail'],api)

    def test_old_proposed_caption_translation_fields_fail(self):
        api=check.read('openapi.json');case=next(c for c in check.read('fixtures.json')['cases'] if c['id']=='captions-bilingual')
        body=copy.deepcopy(case['body']);body['items'][0]['language']='en'
        with self.assertRaises(AssertionError):check.validate(body,api['components']['schemas']['Captions'],api)

    def test_modified_or_missing_fixture_fails_checksum_gate(self):
        with tempfile.TemporaryDirectory(prefix='photohouse-contract-check-') as tmp:
            target=Path(tmp)/'v1';shutil.copytree(check.CONTRACT,target)
            with patch.object(check,'CONTRACT',target):
                fixture=target/'fixtures.json';original=fixture.read_bytes()
                fixture.write_bytes(original+b' ')
                with self.assertRaises(AssertionError):check.verify()
                fixture.write_bytes(original);(target/'media/amber.svg').unlink()
                with self.assertRaises(FileNotFoundError):check.verify()

    def test_mixed_backend_pins_fail_even_with_updated_pack_checksums(self):
        old_pin = '1e394f789ff1f7cef6d9930bb541186684f5a9a0'
        for change_manifest, change_openapi in ((True, False), (False, True), (True, True)):
            with self.subTest(manifest=change_manifest, openapi=change_openapi):
                with tempfile.TemporaryDirectory(prefix='photohouse-pin-check-') as tmp:
                    target=Path(tmp)/'v1';shutil.copytree(check.CONTRACT,target)
                    manifest=json.loads((target/'manifest.json').read_text())
                    if change_manifest: manifest['backend_commit']=old_pin
                    if change_openapi:
                        api=json.loads((target/'openapi.json').read_text())
                        api['x-backend-commit']=old_pin
                        check.dump(target/'openapi.json',api)
                        manifest['files']['openapi.json']=check.sha(target/'openapi.json')
                    check.dump(target/'manifest.json',manifest)
                    with patch.object(check,'CONTRACT',target):
                        with self.assertRaises(AssertionError):check.verify()

    def test_replay_rejects_a_different_backend_before_importing_it(self):
        with patch.object(check.subprocess,'check_output',return_value='1e394f789ff1f7cef6d9930bb541186684f5a9a0\n'):
            with self.assertRaisesRegex(AssertionError,'Backend SHA differs'):
                check.backend_cases(Path('/unused-synthetic-backend'))

    def test_replay_rejects_source_drift_at_the_pinned_head(self):
        with patch.object(check.subprocess,'check_output',return_value=check.BACKEND_SHA+'\n'):
            with patch.object(check,'sha',return_value='0'*64):
                with self.assertRaisesRegex(AssertionError,'backend/app/main.py'):
                    check.backend_cases(Path('/unused-synthetic-backend'))


if __name__=='__main__':unittest.main()
