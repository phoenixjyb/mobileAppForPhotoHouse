#!/usr/bin/env python3
"""Focused regressions for shared handoff drift/type failures; stdlib only."""
from contextlib import redirect_stdout
import copy
import importlib.util
import io
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


if __name__=='__main__':unittest.main()
