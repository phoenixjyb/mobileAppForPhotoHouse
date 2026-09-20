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
MANIFEST_SHA256 = "0836dc8b632254e9c92e471a2a07b0a72e4687c53353c8b100c0834e8731e385"
BACKEND_PACK = Path("docs/contracts/protected-native-v2")
BACKEND_COMMIT = "98d87f3df7cbfdb78367e60fcfa8fa0a117a2e25"
SOURCE_COMMIT = "789bdf0ca9f69787fa3e5d13db3d666f4931e0a3"
SNAPSHOT_PAYLOADS = {
    "CONTRACT.md": "f73308b0dd086b6c2f3debb23319e40c60ab75d8f791564096604d7fe98de1ef",
    "UPLOAD_NEXT.md": "81b72802c2b2ea1ed0795bd0fc335323110853016acf75958e520f85a11d0936",
    "VALIDATION.md": "fff66c65c2a6d7106aa38c48b02a741218c8c042eeb31ad6f8398908f7bacf5a",
    "cases.json": "8440fc84e64685f333d2137006279bdd4824a9e89b22333d29acd3fa882ab597"
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
    if manifest["case_count"] != 86:
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
    if digest(SNAPSHOT / "PREPARED_MEDIA.md") != manifest["source_sha256"]["docs/security/PROTECTED_PREPARED_MEDIA.md"]:
        raise ValueError("prepared playback contract reference changed")
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
