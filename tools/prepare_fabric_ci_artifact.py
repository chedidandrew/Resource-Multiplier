#!/usr/bin/env python3
"""Validate and stage exactly one playable Fabric JAR for branch CI uploads."""

from __future__ import annotations

import argparse
import hashlib
import shutil
from pathlib import Path

from package_release import (
    PLAYABLE_JAR_BASE,
    ROOT,
    ReleasePackageError,
    jar_build_inputs,
    parse_properties,
    validate_release_jar,
)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Validate the exact Loom-built production JAR and copy it to an isolated CI directory."
    )
    parser.add_argument("--output-dir", type=Path, default=ROOT / "build/ci-fabric-artifact")
    args = parser.parse_args()

    properties = parse_properties(ROOT / "gradle.properties")
    version = properties["mod_version"]
    expected_name = f"{PLAYABLE_JAR_BASE}-{version}.jar"
    candidate = ROOT / "build/libs" / expected_name
    if not candidate.is_file():
        raise SystemExit(
            f"Missing exact Fabric production JAR {candidate}; run ./gradlew clean test runGameTest build first."
        )

    try:
        newest_input = max(path.stat().st_mtime for path in jar_build_inputs())
        if candidate.stat().st_mtime + 1 < newest_input:
            raise ReleasePackageError(
                f"stale Fabric production JAR {candidate}; a Java/resource/test or Gradle input is newer"
            )
        validate_release_jar(candidate, version)
    except (OSError, ReleasePackageError) as exc:
        raise SystemExit(f"Fabric CI artifact validation failed: {exc}") from exc

    output_dir = args.output_dir.resolve()
    output_dir.mkdir(parents=True, exist_ok=True)
    existing = sorted(path.name for path in output_dir.iterdir())
    if existing:
        raise SystemExit(
            "Fabric CI artifact directory must be empty to prevent stale or sources-JAR uploads: "
            + ", ".join(existing)
        )

    staged = output_dir / expected_name
    shutil.copyfile(candidate, staged)
    if candidate.stat().st_size != staged.stat().st_size or sha256(candidate) != sha256(staged):
        staged.unlink(missing_ok=True)
        raise SystemExit("Fabric CI artifact copy does not match the validated production JAR")

    print(
        f"PASS: staged exactly one playable Fabric JAR: {staged.relative_to(ROOT)} "
        f"({staged.stat().st_size} bytes, SHA-256 {sha256(staged)})"
    )


if __name__ == "__main__":
    main()
