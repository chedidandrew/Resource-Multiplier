#!/usr/bin/env python3
"""Validate the standalone NeoForge playable JAR before it is shared or uploaded."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path, PurePosixPath
import struct
import sys
import tomllib
import zipfile


ROOT = Path(__file__).resolve().parents[1]
PUBLIC_NAME = "Smart Resource Multiplier"
MOD_ID = "smart_resource_drops"
ICON_ENTRY = "assets/smart_resource_drops/icon.png"
METADATA_ENTRY = "META-INF/neoforge.mods.toml"
LICENSE_ENTRY = "LICENSE_smart-resource-multiplier-neoforge"
MIXIN_CONFIGS = {
    "smart_resource_drops.mixins.json",
    "smart_resource_drops.neoforge.mixins.json",
}
REQUIRED_CLASSES = {
    "com/chedidandrew/smartresourcedrops/SmartResourceDrops.class",
    "com/chedidandrew/smartresourcedrops/platform/PlatformPlayerSupport.class",
    "com/chedidandrew/smartresourcedrops/platform/neoforge/NeoForgeClientEntrypoint.class",
    "com/chedidandrew/smartresourcedrops/platform/neoforge/NeoForgeEntrypoint.class",
    "com/chedidandrew/smartresourcedrops/platform/neoforge/LegacyFabricProvenanceMigration.class",
    "com/chedidandrew/smartresourcedrops/platform/neoforge/NeoForgeNetworking.class",
    "com/chedidandrew/smartresourcedrops/platform/neoforge/NeoForgePlacementStorage.class",
    "com/chedidandrew/smartresourcedrops/platform/neoforge/mixin/CommonHooksPlacementMixin.class",
    "com/chedidandrew/smartresourcedrops/platform/neoforge/mixin/NeoForgeShearsDispenseItemBehaviorMixin.class",
    "com/chedidandrew/smartresourcedrops/platform/neoforge/mixin/ServerPlayerGameModeMixin.class",
    "com/chedidandrew/smartresourcedrops/platform/neoforge/mixin/SerializableChunkDataLegacyProvenanceMixin.class",
}
FORBIDDEN_PARTS = {
    "clienttest",
    "fixture",
    "fixtures",
    "gametest",
    "optionaltest",
    "test",
    "tests",
}
FORBIDDEN_TEST_CLASS_PREFIXES = {
    "com/chedidandrew/smartresourcedrops/client/neoforgeclientcategorysmoketest",
    "com/chedidandrew/smartresourcedrops/client/neoforgemultiplayerclientsmoketest",
    "com/chedidandrew/smartresourcedrops/client/neoforgeoptionalclientonlysmoketest",
    "com/chedidandrew/smartresourcedrops/client/neoforgeoversizedwireclientsmoketest",
    "com/chedidandrew/smartresourcedrops/platform/neoforge/neoforgemigrationrestartsmoketest",
    "com/chedidandrew/smartresourcedrops/platform/neoforge/neoforgemultiplayerserversmoketest",
    "com/chedidandrew/smartresourcedrops/platform/neoforge/neoforgeplacementpersistencesmoketest",
    "com/chedidandrew/smartresourcedrops/platform/neoforge/neoforgeoversizedwireserversmoketest",
    "com/chedidandrew/smartresourcedrops/packagedprobe/",
}


class ValidationError(RuntimeError):
    """Raised when the candidate is not a safe standalone NeoForge artifact."""


def is_development_test_entry(name: str) -> bool:
    """Return whether a JAR member belongs only to a NeoForge test source set."""
    path = PurePosixPath(name)
    folded = name.casefold()
    parts = {part.casefold() for part in path.parts}
    if parts.intersection(FORBIDDEN_PARTS):
        return True
    return any(
        (prefix.endswith("/") and folded.startswith(prefix))
        or folded == prefix + ".class"
        or folded.startswith(prefix + "$")
        for prefix in FORBIDDEN_TEST_CLASS_PREFIXES
    )


def properties(path: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        result[key.strip()] = value.strip()
    return result


def candidate_path(expected_version: str) -> Path:
    expected = (
        ROOT
        / "neoforge"
        / "build"
        / "libs"
        / f"smart-resource-multiplier-neoforge-{expected_version}.jar"
    )
    if not expected.is_file():
        raise ValidationError(
            f"Missing NeoForge playable JAR {expected.relative_to(ROOT)}; "
            "run .\\gradlew.bat -p neoforge clean build first"
        )
    return expected


def validate_mixin_config(
    archive: zipfile.ZipFile,
    config_name: str,
    names: set[str],
    errors: list[str],
    expected_java_version: int,
) -> None:
    try:
        data = json.loads(archive.read(config_name))
    except (KeyError, json.JSONDecodeError) as exc:
        errors.append(f"invalid mixin config {config_name!r}: {exc}")
        return
    if not isinstance(data, dict):
        errors.append(f"mixin config {config_name!r} is not an object")
        return
    expected_compatibility = f"JAVA_{expected_java_version}"
    if data.get("required") is not True or data.get("compatibilityLevel") != expected_compatibility:
        errors.append(
            f"mixin config {config_name!r} is not required Java {expected_java_version} metadata"
        )
    package = data.get("package")
    if not isinstance(package, str) or not package:
        errors.append(f"mixin config {config_name!r} has no package")
        return
    declared = 0
    for side in ("mixins", "client", "server"):
        classes = data.get(side, [])
        if not isinstance(classes, list):
            errors.append(f"mixin config {config_name!r} has an invalid {side!r} list")
            continue
        for class_name in classes:
            declared += 1
            if not isinstance(class_name, str) or not class_name:
                errors.append(f"mixin config {config_name!r} has an invalid class name")
                continue
            class_entry = f"{package}.{class_name}".replace(".", "/") + ".class"
            if class_entry not in names:
                errors.append(f"missing declared mixin class {class_entry!r}")
    if declared == 0:
        errors.append(f"mixin config {config_name!r} declares no mixins")


def validate(jar_path: Path, expected_version: str) -> tuple[int, str]:
    errors: list[str] = []
    expected_java_version = int(properties(ROOT / "neoforge/gradle.properties")["java_version"])
    expected_class_major = expected_java_version + 44
    try:
        with zipfile.ZipFile(jar_path) as archive:
            bad_member = archive.testzip()
            if bad_member is not None:
                errors.append(f"corrupt JAR member {bad_member!r}")

            file_names: list[str] = []
            for info in archive.infolist():
                if info.is_dir():
                    continue
                name = info.filename
                file_names.append(name)
                path = PurePosixPath(name)
                if (
                    not name
                    or "\\" in name
                    or name.startswith("/")
                    or path.is_absolute()
                    or path.as_posix() != name
                    or any(part in {"", ".", ".."} for part in path.parts)
                ):
                    errors.append(f"unsafe JAR entry {name!r}")
                    continue
                folded = name.casefold()
                if is_development_test_entry(name):
                    errors.append(f"development-only test entry {name!r}")
                if folded.endswith((".jar", ".java")):
                    errors.append(f"nested archive or source entry {name!r}")
                if "/platform/fabric/" in f"/{folded}" or folded.startswith("net/fabricmc/"):
                    errors.append(f"Fabric-only runtime entry {name!r}")
                if folded.startswith("com/terraformersmc/"):
                    errors.append(f"optional Mod Menu dependency was bundled as {name!r}")
                if folded.startswith("data/smart_resource_drops_gametest/"):
                    errors.append(f"GameTest resource leaked into the playable JAR as {name!r}")

            names = set(file_names)
            collisions: dict[str, list[str]] = {}
            for name in file_names:
                collisions.setdefault(name.casefold(), []).append(name)
            for values in collisions.values():
                if len(values) > 1:
                    errors.append(f"duplicate or case-colliding entries: {', '.join(values)}")

            required = REQUIRED_CLASSES | MIXIN_CONFIGS | {
                METADATA_ENTRY,
                ICON_ENTRY,
                LICENSE_ENTRY,
            }
            missing = sorted(required.difference(names))
            if missing:
                errors.append(f"missing production entries: {', '.join(missing)}")
            if "fabric.mod.json" in names:
                errors.append("Fabric metadata leaked into the NeoForge JAR")

            try:
                metadata = tomllib.loads(archive.read(METADATA_ENTRY).decode("utf-8"))
            except (KeyError, UnicodeDecodeError, tomllib.TOMLDecodeError) as exc:
                errors.append(f"invalid NeoForge metadata: {exc}")
                metadata = {}
            mods = metadata.get("mods") if isinstance(metadata, dict) else None
            if not isinstance(mods, list) or len(mods) != 1 or not isinstance(mods[0], dict):
                errors.append("NeoForge metadata must declare exactly one mod")
            else:
                mod = mods[0]
                if mod.get("modId") != MOD_ID:
                    errors.append(f"NeoForge metadata uses mod ID {mod.get('modId')!r}")
                if mod.get("displayName") != PUBLIC_NAME:
                    errors.append(f"NeoForge metadata uses display name {mod.get('displayName')!r}")
                if mod.get("version") != expected_version:
                    errors.append(f"NeoForge metadata uses version {mod.get('version')!r}")
                if mod.get("iconFile") != ICON_ENTRY:
                    errors.append("NeoForge metadata does not reference the production icon")
            if metadata.get("license") != "MIT":
                errors.append("NeoForge metadata does not declare the MIT license")
            if metadata.get("modLoader") != "javafml":
                errors.append("NeoForge metadata must use the FML 4 javafml loader")
            if metadata.get("loaderVersion") != "[4,)":
                errors.append("NeoForge metadata must declare loaderVersion='[4,)'")

            expected_properties = properties(ROOT / "neoforge/gradle.properties")
            dependencies = metadata.get("dependencies", {}).get(MOD_ID, []) \
                    if isinstance(metadata.get("dependencies"), dict) else []
            expected_dependencies = {
                "neoforge": expected_properties["neo_version_range"],
                "minecraft": expected_properties["minecraft_version_range"],
            }
            actual_dependencies = {
                item.get("modId"): item.get("versionRange")
                for item in dependencies
                if isinstance(item, dict)
            } if isinstance(dependencies, list) else {}
            if actual_dependencies != expected_dependencies:
                errors.append(
                    "NeoForge metadata dependencies differ from the configured exact target: "
                    f"{actual_dependencies!r}"
                )
            declared_mixins = metadata.get("mixins")
            declared_configs = {
                item.get("config")
                for item in declared_mixins
                if isinstance(item, dict)
            } if isinstance(declared_mixins, list) else set()
            if declared_configs != MIXIN_CONFIGS:
                errors.append("NeoForge metadata does not declare both production mixin configs")

            for config_name in MIXIN_CONFIGS:
                validate_mixin_config(
                    archive,
                    config_name,
                    names,
                    errors,
                    expected_java_version,
                )

            try:
                embedded_license = archive.read(LICENSE_ENTRY)
            except KeyError:
                pass
            else:
                if embedded_license != (ROOT / "LICENSE").read_bytes():
                    errors.append("embedded license differs from the repository license")
            try:
                embedded_icon = archive.read(ICON_ENTRY)
            except KeyError:
                embedded_icon = b""
            if embedded_icon != (
                ROOT / "src/main/resources/assets/smart_resource_drops/icon.png"
            ).read_bytes():
                errors.append("embedded icon differs from the approved production icon")

            for class_name in sorted(name for name in names if name.endswith(".class")):
                try:
                    magic, _, major = struct.unpack(">IHH", archive.read(class_name)[:8])
                except struct.error as exc:
                    errors.append(f"cannot inspect {class_name!r} bytecode: {exc}")
                    continue
                if magic != 0xCAFEBABE or major != expected_class_major:
                    errors.append(
                        f"{class_name!r} is not Java {expected_java_version} bytecode "
                        f"(major={major})"
                    )
    except (OSError, zipfile.BadZipFile) as exc:
        raise ValidationError(f"cannot read {jar_path}: {exc}") from exc

    if errors:
        raise ValidationError("; ".join(errors))
    return len(file_names), hashlib.sha256(jar_path.read_bytes()).hexdigest().upper()


def main() -> int:
    try:
        config = properties(ROOT / "neoforge/gradle.properties")
        expected_version = config["mod_version"]
        jar_path = candidate_path(expected_version)
        entries, digest = validate(jar_path, expected_version)
    except (KeyError, ValidationError) as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        return 1
    print(
        f"PASS: NeoForge playable JAR is loader-isolated, test-free, Java {config['java_version']}, "
        f"and metadata-complete ({entries} entries, SHA-256 {digest})"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
