#!/usr/bin/env python3
"""Validate the final reobfuscated NeoForge/Forge 47 playable JAR."""

from __future__ import annotations

import hashlib
import json
from email.parser import BytesParser
from pathlib import Path, PurePosixPath
import struct
import sys
import tomllib
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def production_source_classes(*source_roots: Path) -> set[str]:
    """Return top-level classes for the NeoForge production source set."""
    entries: set[str] = set()
    for source_root in source_roots:
        for source in source_root.rglob("*.java"):
            relative = source.relative_to(source_root)
            if "platform/fabric/" in relative.as_posix():
                continue
            entries.add(relative.with_suffix(".class").as_posix())
    return entries


MOD_ID = "smart_resource_drops"
PUBLIC_NAME = "Smart Resource Multiplier"
METADATA = "META-INF/mods.toml"
ICON = "assets/smart_resource_drops/icon.png"
LICENSE = "LICENSE_smart-resource-multiplier-neoforge"
REFMAP = "smart_resource_drops.refmap.json"
PACK_METADATA = "pack.mcmeta"
PRODUCTION_RESOURCES = {
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
MIXINS = {
    "smart_resource_drops.mixins.json",
    "smart_resource_drops.neoforge.mixins.json",
}
MIXIN_CONFIG_ORDER = [
    "smart_resource_drops.mixins.json",
    "smart_resource_drops.neoforge.mixins.json",
]
EXPECTED_MIXIN_CLASSES = {
    "smart_resource_drops.mixins.json": [
        "BlockDropResourcesMixin",
        "BlockStateBaseDropsMixin",
        "ExplosionDropsMixin",
        "ExperienceOrbMixin",
        "LivingEntityDeathLootMixin",
        "PlayerShearingContextMixin",
        "ShearsDispenseItemBehaviorMixin",
        "EntityShearingDropMixin",
        "FallingBlockEntityMixin",
        "PistonMovingBlockEntityMixin",
        "BlockItemPlacementCaptureMixin",
        "LevelPlacementCaptureMixin",
    ],
    "smart_resource_drops.neoforge.mixins.json": [
        "CommonHooksPlacementMixin",
        "NeoForgeShearsDispenseItemBehaviorMixin",
        "ServerPlayerGameModeMixin",
    ],
}
EXPECTED_MIXIN_PACKAGES = {
    "smart_resource_drops.mixins.json": "com.chedidandrew.smartresourcedrops.mixin",
    "smart_resource_drops.neoforge.mixins.json":
        "com.chedidandrew.smartresourcedrops.platform.neoforge.mixin",
}
EXPECTED_OVERWRITE_POLICIES = {
    "smart_resource_drops.mixins.json": {"requireAnnotations": True},
    "smart_resource_drops.neoforge.mixins.json": None,
}
MIXINEXTRAS_PATH = "META-INF/jarjar/mixinextras-forge-0.5.4.jar"
MIXINEXTRAS_SHA256 = "7922899a121a27f63a69a9ffe57470d8719cc52d239dfa9408e15d32a7b4c264"
JARJAR_METADATA = "META-INF/jarjar/metadata.json"
REQUIRED_CLASSES = {
    "com/chedidandrew/smartresourcedrops/SmartResourceDrops.class",
    "com/chedidandrew/smartresourcedrops/network/ConfigPatchFragmentPayload.class",
    "com/chedidandrew/smartresourcedrops/network/ConfigSnapshotFragmentPayload.class",
    "com/chedidandrew/smartresourcedrops/network/ConfigTransferCodec.class",
    "com/chedidandrew/smartresourcedrops/platform/neoforge/NeoForgeClientEntrypoint.class",
    "com/chedidandrew/smartresourcedrops/platform/neoforge/NeoForgeEntrypoint.class",
    "com/chedidandrew/smartresourcedrops/platform/neoforge/NeoForgeNetworking.class",
    "com/chedidandrew/smartresourcedrops/platform/neoforge/NeoForgePlacementStorage.class",
    "com/chedidandrew/smartresourcedrops/platform/neoforge/mixin/CommonHooksPlacementMixin.class",
    "com/chedidandrew/smartresourcedrops/platform/neoforge/mixin/NeoForgeShearsDispenseItemBehaviorMixin.class",
    "com/chedidandrew/smartresourcedrops/platform/neoforge/mixin/ServerPlayerGameModeMixin.class",
}
EXPECTED_PRODUCTION_CLASSES = production_source_classes(
    ROOT / "src/main/java",
    ROOT / "src/client/java",
    ROOT / "neoforge/src/main/java",
)
FORBIDDEN_PARTS = {
    "clienttest",
    "fixture",
    "fixtures",
    "gametest",
    "optionaltest",
    "packagedprobe",
    "test",
    "tests",
}


class ValidationError(RuntimeError):
    pass


def is_development_test_entry(name: str) -> bool:
    path = PurePosixPath(name)
    lower = name.casefold()
    top_level_class = (
        path.name[:-len(".class")].split("$", 1)[0].casefold()
        if lower.endswith(".class")
        else ""
    )
    return (
        bool({part.casefold() for part in path.parts}.intersection(FORBIDDEN_PARTS))
        or lower.startswith("data/smart_resource_drops_gametest/")
        or "smoketest" in lower
        or "/optionaltest/" in f"/{lower}"
        or "/packagedprobe/" in f"/{lower}"
        or top_level_class.endswith(("test", "tests"))
    )


def is_allowed_nested_archive_set(names: list[str] | tuple[str, ...]) -> bool:
    return sorted(names) == [MIXINEXTRAS_PATH]


def parse_properties(path: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            result[key.strip()] = value.strip()
    return result


def candidate_path(version: str) -> Path:
    path = ROOT / "neoforge/build/libs" / f"smart-resource-multiplier-neoforge-{version}.jar"
    if not path.is_file():
        raise ValidationError(
            f"missing {path.relative_to(ROOT)}; run .\\gradlew.bat -p neoforge --no-daemon clean build"
        )
    return path


def parse_manifest_mixin_configs(manifest_bytes: bytes) -> list[str]:
    """Parse Java-manifest continuations and require one exact MixinConfigs header."""
    unfolded = manifest_bytes.replace(b"\r\n ", b"").replace(b"\n ", b"")
    manifest = BytesParser().parsebytes(unfolded)
    headers = manifest.get_all("MixinConfigs") or []
    if len(headers) != 1:
        raise ValidationError(
            f"manifest must contain exactly one MixinConfigs header, found {len(headers)}"
        )
    tokens = [part.strip() for part in headers[0].split(",") if part.strip()]
    if len(tokens) != len(set(tokens)):
        raise ValidationError(f"manifest MixinConfigs contains duplicate tokens: {tokens!r}")
    if tokens != MIXIN_CONFIG_ORDER:
        raise ValidationError(
            f"manifest MixinConfigs must be {MIXIN_CONFIG_ORDER!r}, found {tokens!r}"
        )
    return tokens


def inspect_mixin(archive: zipfile.ZipFile, name: str, names: set[str], errors: list[str]) -> None:
    try:
        value = json.loads(archive.read(name))
    except (KeyError, json.JSONDecodeError, UnicodeDecodeError) as exc:
        errors.append(f"invalid mixin config {name}: {exc}")
        return
    if value.get("required") is not True:
        errors.append(f"{name} must be required")
    if value.get("minVersion") != "0.8.5":
        errors.append(f"{name} must declare minVersion 0.8.5")
    if value.get("compatibilityLevel") != "JAVA_17":
        errors.append(f"{name} must use JAVA_17")
    if value.get("refmap") != REFMAP:
        errors.append(f"{name} must reference {REFMAP}")
    package = value.get("package")
    if package != EXPECTED_MIXIN_PACKAGES[name]:
        errors.append(
            f"{name} package must be {EXPECTED_MIXIN_PACKAGES[name]!r}, found {package!r}"
        )
    declared = value.get("mixins")
    if not isinstance(declared, list):
        errors.append(f"{name} has invalid mixins list")
        declared = []
    for side in ("client", "server"):
        entries = value.get(side, [])
        if not isinstance(entries, list) or entries:
            errors.append(f"{name} {side} list must be absent or empty, found {entries!r}")
    if declared != EXPECTED_MIXIN_CLASSES[name]:
        errors.append(f"{name} production mixin class list drifted: {declared!r}")
    if value.get("injectors") != {"defaultRequire": 1}:
        errors.append(f"{name} injector policy drifted")
    expected_overwrites = EXPECTED_OVERWRITE_POLICIES[name]
    if expected_overwrites is None:
        if "overwrites" in value:
            errors.append(f"{name} must not declare an overwrites policy")
    elif value.get("overwrites") != expected_overwrites:
        errors.append(f"{name} overwrites policy drifted")
    if not package or not declared:
        errors.append(f"{name} must declare a package and at least one mixin")
        return
    for entry in declared:
        if not isinstance(entry, str):
            errors.append(f"{name} contains a non-string mixin entry")
            continue
        class_name = f"{package}.{entry}".replace(".", "/") + ".class"
        if class_name not in names:
            errors.append(f"{name} declares missing class {class_name}")


def validate(path: Path, version: str) -> tuple[int, str]:
    errors: list[str] = []
    with zipfile.ZipFile(path) as archive:
        if archive.testzip() is not None:
            errors.append("JAR has a corrupt member")
        file_names = [info.filename for info in archive.infolist() if not info.is_dir()]
        names = set(file_names)
        folded_names: dict[str, list[str]] = {}
        for name in file_names:
            posix = PurePosixPath(name)
            if (
                not name
                or "\\" in name
                or name.startswith("/")
                or posix.as_posix() != name
                or any(part in {"", ".", ".."} for part in posix.parts)
            ):
                errors.append(f"unsafe archive member {name!r}")
            folded_names.setdefault(name.casefold(), []).append(name)
            parts = {part.casefold() for part in posix.parts}
            lower = name.casefold()
            if is_development_test_entry(name):
                errors.append(f"development/test entry leaked: {name}")
            if lower.endswith(".java"):
                errors.append(f"source entry leaked: {name}")
            if "/platform/fabric/" in f"/{lower}" or lower.startswith("net/fabricmc/"):
                errors.append(f"Fabric entry leaked: {name}")
            if lower.startswith("com/terraformersmc/"):
                errors.append(f"Mod Menu implementation was bundled: {name}")
            if lower.startswith("data/smart_resource_drops_gametest/"):
                errors.append(f"GameTest resource leaked: {name}")
        for collision in folded_names.values():
            if len(collision) != 1:
                errors.append(f"case-colliding members: {collision!r}")

        required = REQUIRED_CLASSES | EXPECTED_PRODUCTION_CLASSES | PRODUCTION_RESOURCES | MIXINS | {
            METADATA, PACK_METADATA, ICON, LICENSE, REFMAP, MIXINEXTRAS_PATH, JARJAR_METADATA
        }
        missing = sorted(required - names)
        if missing:
            errors.append("missing entries: " + ", ".join(missing))
        if "fabric.mod.json" in names:
            errors.append("Fabric metadata leaked")
        if "META-INF/neoforge.mods.toml" in names:
            errors.append("modern NeoForge metadata leaked")
        if any("LegacyFabricProvenanceMigration" in name or "SerializableChunkDataLegacyProvenanceMixin" in name for name in names):
            errors.append("unsupported cross-loader migration implementation leaked")

        try:
            pack_metadata = json.loads(archive.read(PACK_METADATA))
            if pack_metadata != {
                "pack": {
                    "pack_format": 15,
                    "description": "Smart Resource Multiplier resources for Minecraft 1.20.1",
                }
            }:
                errors.append(f"unexpected Minecraft 1.20.1 pack metadata: {pack_metadata!r}")
        except (KeyError, json.JSONDecodeError, UnicodeDecodeError) as exc:
            errors.append(f"invalid pack.mcmeta: {exc}")

        nested = sorted(name for name in names if name.casefold().endswith(".jar"))
        if not is_allowed_nested_archive_set(nested):
            errors.append(f"nested JAR set must be exactly {[MIXINEXTRAS_PATH]!r}, found {nested!r}")

        try:
            jarjar = json.loads(archive.read(JARJAR_METADATA))
            jars = jarjar.get("jars")
            expected = {
                "identifier": {"group": "io.github.llamalad7", "artifact": "mixinextras-forge"},
                "version": {"range": "[0.5.4,)", "artifactVersion": "0.5.4"},
                "path": MIXINEXTRAS_PATH,
                "isObfuscated": False,
            }
            if jars != [expected]:
                errors.append(f"unexpected JarJar metadata: {jars!r}")
            nested_hash = hashlib.sha256(archive.read(MIXINEXTRAS_PATH)).hexdigest()
            if nested_hash != MIXINEXTRAS_SHA256:
                errors.append(f"MixinExtras artifact hash drifted: {nested_hash}")
        except (KeyError, json.JSONDecodeError, UnicodeDecodeError) as exc:
            errors.append(f"invalid JarJar metadata/dependency: {exc}")

        try:
            metadata = tomllib.loads(archive.read(METADATA).decode("utf-8"))
        except (KeyError, UnicodeDecodeError, tomllib.TOMLDecodeError) as exc:
            errors.append(f"invalid legacy mods.toml: {exc}")
            metadata = {}
        if metadata.get("modLoader") != "javafml" or metadata.get("loaderVersion") != "[47,)":
            errors.append("mods.toml must declare javafml loader [47,)")
        if metadata.get("license") != "MIT":
            errors.append("mods.toml must declare MIT")
        mods = metadata.get("mods")
        if not isinstance(mods, list) or len(mods) != 1:
            errors.append("mods.toml must declare exactly one mod")
        else:
            mod = mods[0]
            expected_mod = {
                "modId": MOD_ID,
                "version": version,
                "displayName": PUBLIC_NAME,
                "logoFile": ICON,
            }
            for key, expected in expected_mod.items():
                if mod.get(key) != expected:
                    errors.append(f"mods.toml {key} must be {expected!r}, found {mod.get(key)!r}")
            if "iconFile" in mod:
                errors.append("legacy mods.toml must use logoFile, not iconFile")
        dependencies = metadata.get("dependencies", {}).get(MOD_ID, [])
        compact_dependencies = {
            (
                dep.get("modId"),
                dep.get("mandatory"),
                dep.get("versionRange"),
                dep.get("ordering"),
                dep.get("side"),
            )
            for dep in dependencies
            if isinstance(dep, dict)
        }
        expected_dependencies = {
            ("forge", True, "[47.1.106,)", "NONE", "BOTH"),
            ("minecraft", True, "[1.20.1]", "NONE", "BOTH"),
        }
        if compact_dependencies != expected_dependencies:
            errors.append(f"unexpected mods.toml dependencies: {compact_dependencies!r}")

        try:
            parse_manifest_mixin_configs(archive.read("META-INF/MANIFEST.MF"))
        except KeyError:
            errors.append("missing manifest")
        except ValidationError as exc:
            errors.append(str(exc))
        for mixin in MIXINS:
            inspect_mixin(archive, mixin, names, errors)

        try:
            refmap = json.loads(archive.read(REFMAP))
            mappings = refmap.get("mappings") if isinstance(refmap, dict) else None
            searge = refmap.get("data", {}).get("searge") if isinstance(refmap, dict) else None
            if not isinstance(mappings, dict) or not mappings:
                errors.append("refmap mappings must be a nonempty object")
            if not isinstance(searge, dict) or not searge:
                errors.append("refmap data.searge must be a nonempty object")
        except (KeyError, json.JSONDecodeError, UnicodeDecodeError) as exc:
            errors.append(f"invalid production refmap: {exc}")

        try:
            header = archive.read("com/chedidandrew/smartresourcedrops/SmartResourceDrops.class")[:8]
            magic, _, major = struct.unpack(">IHH", header)
            if magic != 0xCAFEBABE or major != 61:
                errors.append(f"main class must be Java 17 class-major 61, found {major}")
        except (KeyError, struct.error) as exc:
            errors.append(f"cannot inspect main class: {exc}")
        if any(
            struct.unpack(">H", archive.read(name)[6:8])[0] != 61
            for name in names
            if name.endswith(".class") and len(archive.read(name)) >= 8
        ):
            errors.append("one or more production classes are not class-major 61")
        if archive.read(ICON) != (ROOT / "src/main/resources/assets/smart_resource_drops/icon.png").read_bytes():
            errors.append("embedded icon differs from source")
        if archive.read(LICENSE) != (ROOT / "LICENSE").read_bytes():
            errors.append("embedded license differs from source")

    if errors:
        raise ValidationError("; ".join(errors))
    digest = hashlib.sha256(path.read_bytes()).hexdigest().upper()
    return len(file_names), digest


def main() -> int:
    try:
        version = parse_properties(ROOT / "neoforge/gradle.properties")["mod_version"]
        path = candidate_path(version)
        entries, digest = validate(path, version)
    except (KeyError, OSError, zipfile.BadZipFile, ValidationError) as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        return 1
    print(f"PASS: {path.name} is a Java 17 Forge-47 production JAR ({entries} entries, SHA-256 {digest})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
