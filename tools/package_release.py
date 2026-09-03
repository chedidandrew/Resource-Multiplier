#!/usr/bin/env python3
"""Create a deterministic, validated dual-loader release bundle."""

from __future__ import annotations

import argparse
import hashlib
import importlib.util
import os
from pathlib import Path, PurePosixPath
import shutil
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PUBLIC_MOD_NAME = "Smart Resource Multiplier"
PUBLIC_ARCHIVE_BASE = "SmartResourceMultiplier"
PLAYABLE_JAR_BASE = "smart-resource-multiplier"
NEOFORGE_PLAYABLE_JAR_BASE = "smart-resource-multiplier-neoforge"
EXPECTED_CONTACT = {
    "homepage": "https://www.curseforge.com/minecraft/mc-mods/resource-multiplier",
    "issues": "https://github.com/chedidandrew/Resource-Multiplier/issues",
    "sources": "https://github.com/chedidandrew/Resource-Multiplier",
}
EXPECTED_MODMENU_LINKS = {
    "smart_resource_drops.modmenu.link.kofi": "https://ko-fi.com/andrewchedid",
    "smart_resource_drops.modmenu.link.paypal": "https://www.paypal.com/paypalme/chedidandrew",
    "smart_resource_drops.modmenu.link.cash_app": "https://cash.app/%24AndrewChedid",
}
EXPECTED_ENTRYPOINTS = {
    "main": ["com.chedidandrew.smartresourcedrops.platform.fabric.FabricEntrypoint"],
    "client": ["com.chedidandrew.smartresourcedrops.platform.fabric.client.FabricClientEntrypoint"],
    "modmenu": ["com.chedidandrew.smartresourcedrops.platform.fabric.client.FabricModMenuIntegration"],
}
EXPECTED_MIXIN_DECLARATIONS = ["smart_resource_drops.mixins.json"]
PRODUCTION_RESOURCES = frozenset(
    {
        "assets/smart_resource_drops/icon.png",
        "assets/smart_resource_drops/lang/en_us.json",
        *(f"data/smart_resource_drops/tags/blocks/categories/{name}.json" for name in (
            "building_blocks", "crops", "end", "leaves", "logs", "nether", "ores",
            "plants", "raw_resource_blocks", "soil", "stone",
        )),
        *(f"data/smart_resource_drops/tags/entity_types/categories/{name}.json" for name in (
            "ambient", "aquatic", "bosses", "golems", "hostile", "miscellaneous",
            "neutral", "passive", "villagers_npcs",
        )),
        "data/smart_resource_drops/tags/entity_types/shearing/special.json",
        "data/smart_resource_drops/tags/entity_types/shearing/standard_resources.json",
        "data/smart_resource_drops/tags/items/protected_entity_loot.json",
    }
)

REQUIRED_SOURCE_FILES = frozenset(
    {
        ".github/workflows/build.yml",
        ".github/workflows/release.yml",
        "BUILD_STATUS.md",
        "CHANGELOG.md",
        "CONTRIBUTING.md",
        "LICENSE",
        "README.md",
        "SECURITY.md",
        "build.gradle",
        "gradle.properties",
        "gradlew",
        "gradlew.bat",
        "gradle/wrapper/gradle-wrapper.jar",
        "gradle/wrapper/gradle-wrapper.properties",
        "docs/TESTING.md",
        "docs/PUBLIC_RELEASE_CHECKLIST.md",
        "docs/releases/1.3.0+mc1.20.1.md",
        "src/main/resources/fabric.mod.json",
        "src/main/resources/pack.mcmeta",
        "src/main/resources/smart_resource_drops.mixins.json",
        "src/main/resources/assets/smart_resource_drops/icon.png",
        "src/main/resources/data/smart_resource_drops/tags/blocks/categories/ores.json",
        "src/main/resources/data/smart_resource_drops/tags/entity_types/categories/passive.json",
        "src/main/resources/data/smart_resource_drops/tags/entity_types/shearing/special.json",
        "src/main/resources/data/smart_resource_drops/tags/items/protected_entity_loot.json",
        "src/main/java/com/chedidandrew/smartresourcedrops/network/ConfigTransferCodec.java",
        "src/main/java/com/chedidandrew/smartresourcedrops/network/ConfigPatchFragmentPayload.java",
        "src/main/java/com/chedidandrew/smartresourcedrops/network/ConfigSnapshotFragmentPayload.java",
        "src/clienttest/java/com/chedidandrew/smartresourcedrops/client/FabricClientSmokeTest.java",
        "src/clienttest/java/com/chedidandrew/smartresourcedrops/platform/fabric/FabricPlacementPersistenceSmokeTest.java",
        "tools/validate_package.py",
        "tools/validate_fabric_jar.py",
        "tools/validate_neoforge_jar.py",
        "tools/run_core_tests.sh",
        "tools/run_core_tests.ps1",
        "tools/run_fabric_multiplayer_smoke.sh",
        "tools/run_neoforge_multiplayer_smoke.sh",
        "tools/run_neoforge_optional_channel_smoke.sh",
        "tools/run_neoforge_oversized_wire_smoke.sh",
        "tools/run_neoforge_production_server_smoke.sh",
        "neoforge/build.gradle",
        "neoforge/gradle.properties",
        "neoforge/src/main/templates/META-INF/mods.toml",
        "neoforge/src/main/resources/smart_resource_drops.neoforge.mixins.json",
        "neoforge/src/gametest/resources/data/smart_resource_drops_gametest/structures/wide.nbt",
        "neoforge/src/clienttest/java/com/chedidandrew/smartresourcedrops/platform/neoforge/NeoForgePlacementPersistenceSmokeTest.java",
    }
    | {f"src/main/resources/{name}" for name in PRODUCTION_RESOURCES}
)
REQUIRED_RELEASE_JAR_ENTRIES = frozenset(
    {
        "fabric.mod.json",
        "pack.mcmeta",
        "smart_resource_drops.mixins.json",
        "smart_resource_drops.refmap.json",
        "LICENSE_smart-resource-multiplier",
        "com/chedidandrew/smartresourcedrops/SmartResourceDrops.class",
        "com/chedidandrew/smartresourcedrops/network/ConfigTransferCodec.class",
        "com/chedidandrew/smartresourcedrops/network/ConfigPatchFragmentPayload.class",
        "com/chedidandrew/smartresourcedrops/network/ConfigSnapshotFragmentPayload.class",
        "com/chedidandrew/smartresourcedrops/platform/fabric/FabricEntrypoint.class",
        "com/chedidandrew/smartresourcedrops/platform/fabric/client/FabricClientEntrypoint.class",
    }
    | PRODUCTION_RESOURCES
)

FORBIDDEN_TOP_LEVEL = {
    ".git",
    ".gradle",
    ".gradle-wrapper",
    ".idea",
    ".vs",
    ".vscode",
    "build",
    "dist",
    "logs",
    "mods",
    "out",
    "run",
    "saves",
    "screenshots",
    "world",
}
FORBIDDEN_FILE_NAMES = {
    ".env",
    "credentials.json",
    "server.properties",
    "usercache.json",
    "usernamecache.json",
}
FORBIDDEN_SUFFIXES = {".class", ".log", ".pyc", ".pem", ".key"}


class ReleasePackageError(RuntimeError):
    pass


def parse_properties(path: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            result[key.strip()] = value.strip()
    return result


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()


def is_forbidden_source_path(relative: Path | PurePosixPath) -> bool:
    posix = PurePosixPath(relative.as_posix())
    if not posix.parts:
        return True
    parts = [part.casefold() for part in posix.parts]
    if parts[0] in FORBIDDEN_TOP_LEVEL or "__pycache__" in parts:
        return True
    name = parts[-1]
    if name in FORBIDDEN_FILE_NAMES or name.startswith(".env"):
        return True
    return any(name.endswith(suffix) for suffix in FORBIDDEN_SUFFIXES)


def git_file_names(*arguments: str) -> tuple[str, ...]:
    command = ["git", "ls-files", "-z", *arguments]
    try:
        value = subprocess.check_output(command, cwd=ROOT)
    except (OSError, subprocess.CalledProcessError) as exc:
        raise ReleasePackageError(f"cannot read Git source manifest: {exc}") from exc
    return tuple(sorted(item.decode("utf-8") for item in value.split(b"\0") if item))


def validate_source_entries(relative_names: tuple[str, ...] | list[str]) -> tuple[str, ...]:
    normalized = tuple(sorted(relative_names))
    missing = sorted(REQUIRED_SOURCE_FILES - set(normalized))
    if missing:
        raise ReleasePackageError("missing required source files: " + ", ".join(missing))
    forbidden = [name for name in normalized if is_forbidden_source_path(PurePosixPath(name))]
    if forbidden:
        raise ReleasePackageError("forbidden generated/runtime source entries: " + ", ".join(forbidden))
    if len({name.casefold() for name in normalized}) != len(normalized):
        raise ReleasePackageError("source manifest contains duplicate/case-colliding paths")
    return normalized


def source_files() -> list[Path]:
    untracked = git_file_names("--others", "--exclude-standard")
    if untracked:
        raise ReleasePackageError(
            "untracked non-ignored files must be reviewed and committed or removed before packaging: "
            + ", ".join(untracked)
        )
    names = validate_source_entries(git_file_names("--cached"))
    files = [ROOT / name for name in names]
    missing = [path.relative_to(ROOT).as_posix() for path in files if not path.is_file()]
    if missing:
        raise ReleasePackageError("tracked source files missing from worktree: " + ", ".join(missing))
    return files


def jar_build_inputs() -> list[Path]:
    roots = [
        ROOT / "build.gradle",
        ROOT / "gradle.properties",
        ROOT / "src/main",
        ROOT / "src/client",
    ]
    return _collect_inputs(roots)


def neoforge_jar_build_inputs() -> list[Path]:
    roots = [
        ROOT / "gradle.properties",
        ROOT / "src/main",
        ROOT / "src/client",
        ROOT / "neoforge/build.gradle",
        ROOT / "neoforge/gradle.properties",
        ROOT / "neoforge/settings.gradle",
        ROOT / "neoforge/src/main",
    ]
    return _collect_inputs(roots)


def _collect_inputs(roots: list[Path]) -> list[Path]:
    result: set[Path] = set()
    for root in roots:
        if root.is_file():
            result.add(root)
        elif root.is_dir():
            result.update(path for path in root.rglob("*") if path.is_file())
    return sorted(result, key=lambda path: path.relative_to(ROOT).as_posix())


def _load_validator(name: str, relative: str):
    spec = importlib.util.spec_from_file_location(name, ROOT / relative)
    if spec is None or spec.loader is None:
        raise ReleasePackageError(f"cannot load {relative}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def validate_release_jar(jar_path: Path, expected_version: str | None = None) -> tuple[str, ...]:
    if expected_version is not None and expected_version != "1.3.0+mc1.20.1":
        raise ReleasePackageError(f"unexpected Fabric version {expected_version}")
    validator = _load_validator("validate_fabric_jar", "tools/validate_fabric_jar.py")
    try:
        validator.validate(jar_path)
    except Exception as exc:
        raise ReleasePackageError(f"invalid Fabric playable JAR: {exc}") from exc
    with zipfile.ZipFile(jar_path) as archive:
        names = tuple(info.filename for info in archive.infolist() if not info.is_dir())
    missing = sorted(REQUIRED_RELEASE_JAR_ENTRIES - set(names))
    if missing:
        raise ReleasePackageError("Fabric playable JAR missing required entries: " + ", ".join(missing))
    return names


def validate_neoforge_release_candidate(jar_path: Path, expected_version: str) -> tuple[str, ...]:
    validator = _load_validator("validate_neoforge_jar", "tools/validate_neoforge_jar.py")
    try:
        validator.validate(jar_path, expected_version)
    except Exception as exc:
        raise ReleasePackageError(f"invalid NeoForge playable JAR: {exc}") from exc
    with zipfile.ZipFile(jar_path) as archive:
        return tuple(info.filename for info in archive.infolist() if not info.is_dir())


def _zip_info(name: str, mode: int = 0o644) -> zipfile.ZipInfo:
    info = zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0))
    info.compress_type = zipfile.ZIP_DEFLATED
    info.external_attr = (mode & 0xFFFF) << 16
    return info


def build_source_zip(output: Path, top_level: str, files: list[Path]) -> tuple[str, ...]:
    names = validate_source_entries(
        tuple(path.relative_to(ROOT).as_posix() for path in files)
    )
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name in names:
            mode = 0o755 if name == "gradlew" or name.endswith(".sh") else 0o644
            archive.writestr(_zip_info(f"{top_level}/{name}", mode), (ROOT / name).read_bytes())
    return names


def validate_source_zip(path: Path, top_level: str) -> tuple[str, ...]:
    with zipfile.ZipFile(path) as archive:
        names = []
        for info in archive.infolist():
            if info.is_dir():
                continue
            prefix = f"{top_level}/"
            if not info.filename.startswith(prefix):
                raise ReleasePackageError(f"source ZIP entry outside {top_level}: {info.filename}")
            names.append(info.filename[len(prefix):])
    return validate_source_entries(tuple(names))


def require_empty_output_directory(path: Path) -> None:
    path.mkdir(parents=True, exist_ok=True)
    stale = sorted(item.name for item in path.iterdir())
    if stale:
        raise ReleasePackageError(
            f"output directory contains stale files ({', '.join(stale)}); use a fresh empty directory"
        )


def _assert_fresh(jar: Path, inputs: list[Path], label: str) -> None:
    newest = max((path.stat().st_mtime_ns for path in inputs), default=0)
    if jar.stat().st_mtime_ns < newest:
        raise ReleasePackageError(f"{label} JAR is older than one or more production inputs; rebuild it")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=ROOT / "dist")
    args = parser.parse_args()
    output = args.output.resolve()
    try:
        root_props = parse_properties(ROOT / "gradle.properties")
        neo_props = parse_properties(ROOT / "neoforge/gradle.properties")
        version = root_props["mod_version"]
        if version != "1.3.0+mc1.20.1" or neo_props.get("mod_version") != version:
            raise ReleasePackageError("Fabric and NeoForge must both use 1.3.0+mc1.20.1")
        if root_props.get("minecraft_version") != "1.20.1" or root_props.get("java_version") != "17":
            raise ReleasePackageError("release packager is locked to Minecraft 1.20.1 / Java 17")
        require_empty_output_directory(output)

        fabric = ROOT / "build/libs" / f"{PLAYABLE_JAR_BASE}-{version}.jar"
        neoforge = ROOT / "neoforge/build/libs" / f"{NEOFORGE_PLAYABLE_JAR_BASE}-{version}.jar"
        if not fabric.is_file() or not neoforge.is_file():
            raise ReleasePackageError("both final loader JARs must be built before packaging")
        _assert_fresh(fabric, jar_build_inputs(), "Fabric")
        _assert_fresh(neoforge, neoforge_jar_build_inputs(), "NeoForge")
        validate_release_jar(fabric, version)
        validate_neoforge_release_candidate(neoforge, version)

        copied_fabric = output / fabric.name
        copied_neoforge = output / neoforge.name
        shutil.copy2(fabric, copied_fabric)
        shutil.copy2(neoforge, copied_neoforge)
        top_level = f"{PUBLIC_ARCHIVE_BASE}-{version}"
        source_zip = output / f"{top_level}-source.zip"
        build_source_zip(source_zip, top_level, source_files())
        validate_source_zip(source_zip, top_level)

        checksums = output / "SHA256SUMS.txt"
        checksum_files = (copied_fabric, copied_neoforge, source_zip)
        checksums.write_text(
            "".join(f"{sha256(path)}  {path.name}\n" for path in checksum_files),
            encoding="utf-8",
            newline="\n",
        )
        shutil.copy2(ROOT / "BUILD_STATUS.md", output / f"{top_level}-BUILD_STATUS.md")
        (output / "README.txt").write_text(
            f"{PUBLIC_MOD_NAME} {version}\n"
            "Minecraft Java 1.20.1 / Java 17\n\n"
            f"Fabric: {copied_fabric.name} (requires Fabric API)\n"
            f"NeoForge: {copied_neoforge.name}\n\n"
            "Install exactly one loader-specific JAR. This backport does not migrate "
            "placed-block provenance between Fabric and NeoForge.\n",
            encoding="utf-8",
            newline="\n",
        )
    except (KeyError, OSError, zipfile.BadZipFile, ReleasePackageError) as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        return 1

    print(f"PASS: validated dual-loader bundle written to {output}")
    for path in sorted(output.iterdir()):
        if path.is_file():
            print(f"{path.name}: {path.stat().st_size} bytes")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
