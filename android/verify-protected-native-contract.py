#!/usr/bin/env python3
"""Verify the Android snapshot and, optionally, the pinned backend pack."""
from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
from pathlib import Path

ANDROID = Path(__file__).resolve().parent
SNAPSHOT = ANDROID / "protected-native-contract"
MANIFEST_SHA256 = "3c222c9525f89ff753353b17e7741e200d16061cd042df5afbd03618ed42ce3e"
BACKEND_PACK = Path("docs/contracts/protected-native-v2")
BACKEND_COMMIT = "97c5d620b2eaf8bc5e50c48d3d8c5985bec8396d"
SOURCE_COMMIT = "4022a57f56e6b2f976931a20569e15c879871d93"
SNAPSHOT_PAYLOADS = {
    "CONTRACT.md": "9f59ef34daab0b6d58fb0f54f9acd2d770f085951d1100d8b71ec0aebb5eea5a",
    "UPLOAD_NEXT.md": "61a1f3a6b53a683451efe83bc1bc7f31ccf50e96007ce50fc1bf750ce8237ea8",
    "VALIDATION.md": "21a1b6179de21ea8f38b809bd4cb5edb433bf1b2468c2173b51fbb9260c88a01",
    "cases.json": "cb16cb611530fe4bd4d425d9db1c539543c298666e394a3ca81d5866c441b1d8",
}


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def verify_snapshot() -> dict:
    manifest_path = SNAPSHOT / "manifest.json"
    if digest(manifest_path) != MANIFEST_SHA256:
        raise ValueError("snapshot manifest digest mismatch")
    manifest = json.loads(manifest_path.read_text())
    if manifest["backend_source_commit"] != SOURCE_COMMIT:
        raise ValueError("unexpected backend source commit")
    if manifest["case_count"] != 60:
        raise ValueError("unexpected case metadata")
    if manifest["client_profile_defaults"] != {
        "protected_native_v2": False,
        "protected_photo_display": False,
        "protected_story_read": False,
    }:
        raise ValueError("profile defaults are not explicitly off")
    for name, expected in SNAPSHOT_PAYLOADS.items():
        actual = digest(SNAPSHOT / name)
        if actual != expected or manifest["payload_sha256"].get(str(BACKEND_PACK / name)) != expected:
            raise ValueError(f"snapshot payload mismatch: {name}")
    cases = json.loads((SNAPSHOT / "cases.json").read_text())
    if cases["contract_version"] != manifest["contract_version"] or cases["synthetic_only"] is not True:
        raise ValueError("case metadata mismatch")
    if len(cases["cases"]) != manifest["case_count"]:
        raise ValueError("case count mismatch")
    return manifest


def verify_backend(root: Path, snapshot_manifest: dict) -> None:
    root = root.resolve()
    if subprocess.run(["git", "-C", str(root), "rev-parse", "HEAD"], check=True, capture_output=True, text=True).stdout.strip() != BACKEND_COMMIT:
        raise ValueError("backend checkout is not the pinned pack commit")
    manifest_path = root / BACKEND_PACK / "manifest.json"
    if digest(manifest_path) != MANIFEST_SHA256:
        raise ValueError("backend manifest digest mismatch")
    backend = json.loads(manifest_path.read_text())
    if backend != snapshot_manifest:
        raise ValueError("backend manifest differs from Android snapshot")
    for group in ("source_sha256", "payload_sha256"):
        for name, expected in backend[group].items():
            path = root / name
            if not path.is_file() or digest(path) != expected:
                raise ValueError(f"backend {group} mismatch: {name}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--backend-root", type=Path)
    args = parser.parse_args()
    manifest = verify_snapshot()
    if args.backend_root:
        verify_backend(args.backend_root, manifest)
    print(f"PASS {manifest['contract_version']}: {manifest['case_count']} cases; snapshot verified")


if __name__ == "__main__":
    main()
