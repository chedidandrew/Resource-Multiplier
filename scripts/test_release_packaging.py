#!/usr/bin/env python3
"""Focused regression tests for the 1.20.1 release packager and JAR policy."""

from __future__ import annotations

import importlib.util
import json
import os
from pathlib import Path, PurePosixPath
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def load(name: str, path: str):
    spec = importlib.util.spec_from_file_location(name, ROOT / path)
    if spec is None or spec.loader is None:
        raise AssertionError(f"could not load {path}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


package = load("package_release", "tools/package_release.py")
neo = load("validate_neoforge_jar", "tools/validate_neoforge_jar.py")
fabric = load("validate_fabric_jar", "tools/validate_fabric_jar.py")

assert package.PUBLIC_MOD_NAME == "Smart Resource Multiplier"
assert package.PUBLIC_ARCHIVE_BASE == "SmartResourceMultiplier"
assert package.PLAYABLE_JAR_BASE == "smart-resource-multiplier"
assert package.NEOFORGE_PLAYABLE_JAR_BASE == "smart-resource-multiplier-neoforge"
assert package.parse_properties(ROOT / "gradle.properties")["mod_version"] == "1.3.0+mc1.20.1"
assert package.parse_properties(ROOT / "neoforge/gradle.properties")["mod_version"] == "1.3.0+mc1.20.1"
assert "com/chedidandrew/smartresourcedrops/config/ConfigValidator.class" in fabric.EXPECTED_PRODUCTION_CLASSES
assert "com/chedidandrew/smartresourcedrops/config/ConfigValidator.class" in neo.EXPECTED_PRODUCTION_CLASSES
assert "com/chedidandrew/smartresourcedrops/platform/fabric/FabricNetworking.class" not in neo.EXPECTED_PRODUCTION_CLASSES

for allowed in (
    "gradlew",
    "gradlew.bat",
    "gradle/wrapper/gradle-wrapper.jar",
    "src/main/resources/data/smart_resource_drops/tags/entity_types/categories/passive.json",
):
    assert not package.is_forbidden_source_path(PurePosixPath(allowed)), allowed

for forbidden in (
    ".gradle/cache.bin",
    ".gradle-wrapper/gradle-9.5.1/bin/gradle",
    "build/classes/Leak.class",
    "dist/stale.jar",
    "run/config/smart_resource_drops.json",
    "logs/latest.log",
    "mods/private.jar",
    "saves/World/level.dat",
    "screenshots/private.png",
    "credentials.json",
    ".env.local",
    "private.pem",
    "tools/__pycache__/package_release.pyc",
):
    assert package.is_forbidden_source_path(PurePosixPath(forbidden)), forbidden

minimum = tuple(sorted(package.REQUIRED_SOURCE_FILES))
assert package.validate_source_entries(minimum) == minimum
for missing in minimum:
    incomplete = tuple(name for name in minimum if name != missing)
    try:
        package.validate_source_entries(incomplete)
    except package.ReleasePackageError as exc:
        assert "missing required source files" in str(exc)
        assert missing in str(exc)
    else:
        raise AssertionError(f"accepted source manifest without {missing}")

assert neo.is_allowed_nested_archive_set([neo.MIXINEXTRAS_PATH])
for invalid in (
    [],
    ["META-INF/jarjar/other.jar"],
    [neo.MIXINEXTRAS_PATH, "META-INF/jarjar/extra.jar"],
    ["META-INF/jars/mixinextras-forge-0.5.4.jar"],
):
    assert not neo.is_allowed_nested_archive_set(invalid), invalid

assert neo.parse_manifest_mixin_configs(
    b"Manifest-Version: 1.0\r\n"
    b"MixinConfigs: smart_resource_drops.mixins.json,smart_resource_drops.neof\r\n"
    b" orge.mixins.json\r\n\r\n"
) == neo.MIXIN_CONFIG_ORDER
for invalid_manifest in (
    b"Manifest-Version: 1.0\r\n\r\n",
    b"MixinConfigs: smart_resource_drops.mixins.json\r\n"
    b"MixinConfigs: smart_resource_drops.neoforge.mixins.json\r\n\r\n",
    b"MixinConfigs: smart_resource_drops.mixins.json,smart_resource_drops.mixins.json\r\n\r\n",
    b"MixinConfigs: smart_resource_drops.neoforge.mixins.json,smart_resource_drops.mixins.json\r\n\r\n",
    b"MixinConfigs: smart_resource_drops.mixins.json,smart_resource_drops.neoforge.mixins.json,extra.json\r\n\r\n",
):
    try:
        neo.parse_manifest_mixin_configs(invalid_manifest)
    except neo.ValidationError:
        pass
    else:
        raise AssertionError(f"accepted invalid MixinConfigs manifest: {invalid_manifest!r}")

for development in (
    "com/chedidandrew/smartresourcedrops/gametest/Fixture.class",
    "com/chedidandrew/smartresourcedrops/client/NeoForgeClientCategorySmokeTest.class",
    "com/chedidandrew/smartresourcedrops/optionaltest/OptionalChannelIds.class",
    "com/chedidandrew/smartresourcedrops/config/ConfigValidatorTest.class",
    "com/chedidandrew/smartresourcedrops/network/ConfigTransferCodecTests$Case.class",
    "data/smart_resource_drops_gametest/structures/wide.nbt",
):
    assert neo.is_development_test_entry(development), development
assert not neo.is_development_test_entry(
    "com/chedidandrew/smartresourcedrops/platform/neoforge/NeoForgeNetworking.class"
)

original_git_file_names = package.git_file_names
try:
    package.git_file_names = lambda *arguments: (
        ("untracked-release-sentinel.txt",)
        if arguments == ("--others", "--exclude-standard")
        else minimum
    )
    try:
        package.source_files()
    except package.ReleasePackageError as exc:
        assert "untracked non-ignored files" in str(exc)
        assert "untracked-release-sentinel.txt" in str(exc)
    else:
        raise AssertionError("accepted an untracked non-ignored source file")
finally:
    package.git_file_names = original_git_file_names

temp_root = ROOT / "out" / f"srm-package-policy-{os.getpid()}"
temp_root.mkdir(parents=True, exist_ok=False)
if True:
    first = temp_root / "first.zip"
    second = temp_root / "second.zip"
    top = "SmartResourceMultiplier-test"
    files = [ROOT / name for name in minimum]
    missing_files = [path for path in files if not path.is_file()]
    assert not missing_files, f"required package sources missing: {missing_files!r}"
    first_manifest = package.build_source_zip(first, top, files)
    second_manifest = package.build_source_zip(second, top, files)
    assert first_manifest == minimum
    assert first.read_bytes() == second.read_bytes(), "source ZIP is not deterministic"
    assert package.validate_source_zip(first, top) == minimum

    with zipfile.ZipFile(first) as archive:
        assert all(info.date_time == (1980, 1, 1, 0, 0, 0) for info in archive.infolist())
        assert all(info.filename.startswith(top + "/") for info in archive.infolist())

    bad = temp_root / "bad.zip"
    with zipfile.ZipFile(bad, "w") as archive:
        for name in minimum:
            archive.writestr(f"{top}/{name}", b"x")
        archive.writestr(f"{top}/build/Leak.class", b"x")
    try:
        package.validate_source_zip(bad, top)
    except package.ReleasePackageError as exc:
        assert "forbidden" in str(exc)
    else:
        raise AssertionError("accepted generated class in source ZIP")

    empty = temp_root / "dist"
    package.require_empty_output_directory(empty)
    (empty / "stale.jar").write_bytes(b"stale")
    try:
        package.require_empty_output_directory(empty)
    except package.ReleasePackageError as exc:
        assert "stale" in str(exc)
    else:
        raise AssertionError("accepted a non-empty output directory")

    def write_fabric_fixture(
        path: Path,
        *,
        entrypoints: dict[str, list[str]] | None = None,
        metadata_mixins: list[str] | None = None,
        refmap: bytes | None = None,
        extra_entries: dict[str, bytes] | None = None,
        omit_entries: set[str] | None = None,
    ) -> None:
        metadata = {
            "schemaVersion": 1,
            "id": fabric.MOD_ID,
            "version": fabric.VERSION,
            "name": "Smart Resource Multiplier",
            "description": (
                "Configurable block, entity death-loot, and safe supported shearing multipliers "
                "with persistent anti-dupe protection."
            ),
            "authors": ["Andrew Chedid"],
            "contact": fabric.EXPECTED_CONTACT,
            "license": "MIT",
            "icon": fabric.ICON,
            "environment": "*",
            "entrypoints": fabric.EXPECTED_ENTRYPOINTS if entrypoints is None else entrypoints,
            "mixins": [fabric.MIXIN] if metadata_mixins is None else metadata_mixins,
            "depends": {
                "fabricloader": ">=0.19.5",
                "minecraft": "1.20.1",
                "java": ">=17",
                "fabric-api": ">=0.92.12+1.20.1",
            },
            "suggests": {"modmenu": ">=7.2.2"},
            "custom": {"modmenu": {"links": fabric.EXPECTED_LINKS}},
        }
        class_header = bytes.fromhex("CAFEBABE0000003D")
        mixin_classes = {
            "com/chedidandrew/smartresourcedrops/mixin/" + name + ".class"
            for name in fabric.EXPECTED_MIXINS
        }
        with zipfile.ZipFile(path, "w") as archive:
            archive.writestr("fabric.mod.json", json.dumps(metadata))
            archive.writestr(
                fabric.MIXIN,
                (ROOT / "src/main/resources" / fabric.MIXIN).read_bytes(),
            )
            archive.writestr(
                fabric.REFMAP,
                refmap
                if refmap is not None
                else json.dumps({
                    "mappings": {"fixture": {"method": "mapped"}},
                    "data": {"named:intermediary": {"fixture": {"method": "mapped"}}},
                }).encode("utf-8"),
            )
            archive.writestr(
                fabric.LICENSE,
                (ROOT / "LICENSE").read_bytes(),
            )
            archive.writestr(
                fabric.PACK_METADATA,
                (ROOT / "src/main/resources/pack.mcmeta").read_bytes(),
            )
            for resource in fabric.PRODUCTION_RESOURCES:
                archive.writestr(
                    resource,
                    (ROOT / "src/main/resources" / resource).read_bytes(),
                )
            for class_name in (
                    fabric.REQUIRED_CLASSES | fabric.EXPECTED_PRODUCTION_CLASSES | mixin_classes):
                if omit_entries is None or class_name not in omit_entries:
                    archive.writestr(class_name, class_header)
            for name, body in (extra_entries or {}).items():
                archive.writestr(name, body)

    valid_fabric = temp_root / "valid-fabric.jar"
    write_fabric_fixture(valid_fabric)
    fabric.validate(valid_fabric)
    invalid_fabric_cases = {
        "wrong-entrypoint.jar": {
            "entrypoints": {**fabric.EXPECTED_ENTRYPOINTS, "modmenu": ["missing.Entrypoint"]},
        },
        "wrong-metadata-mixin.jar": {"metadata_mixins": ["wrong.mixins.json"]},
        "malformed-refmap.jar": {"refmap": b"{"},
        "empty-refmap.jar": {"refmap": b"{}"},
        "leaked-smoke-test.jar": {
            "extra_entries": {
                "com/chedidandrew/smartresourcedrops/platform/fabric/"
                "FabricPlacementPersistenceSmokeTest$Phase.class":
                    bytes.fromhex("CAFEBABE0000003D"),
            },
        },
        "leaked-unit-test.jar": {
            "extra_entries": {
                "com/chedidandrew/smartresourcedrops/config/ConfigValidatorTest$Case.class":
                    bytes.fromhex("CAFEBABE0000003D"),
            },
        },
        "missing-ordinary-production-class.jar": {
            "omit_entries": {
                "com/chedidandrew/smartresourcedrops/config/ConfigValidator.class",
            },
        },
    }
    for filename, arguments in invalid_fabric_cases.items():
        invalid_fabric = temp_root / filename
        write_fabric_fixture(invalid_fabric, **arguments)
        try:
            fabric.validate(invalid_fabric)
        except fabric.ValidationError:
            pass
        else:
            raise AssertionError(f"Fabric validator accepted {filename}")

print(
    "PASS: 1.20.1 source packaging is deterministic, wrapper-complete, generated/runtime-safe, "
    "and NeoForge permits only the pinned MixinExtras JarJar dependency"
)
