#!/usr/bin/env python3
"""Validate the final remapped Fabric 1.20.1 playable JAR."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path, PurePosixPath
import struct
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def production_source_classes(*source_roots: Path) -> set[str]:
    """Return the top-level class entry every production Java source must contribute."""
    entries: set[str] = set()
    for source_root in source_roots:
        for source in source_root.rglob("*.java"):
            entries.add(source.relative_to(source_root).with_suffix(".class").as_posix())
    return entries


MOD_ID = "smart_resource_drops"
VERSION = "1.3.0+mc1.20.1"
ICON = "assets/smart_resource_drops/icon.png"
LICENSE = "LICENSE_smart-resource-multiplier"
MIXIN = "smart_resource_drops.mixins.json"
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
REQUIRED_CLASSES = {
    "com/chedidandrew/smartresourcedrops/SmartResourceDrops.class",
    "com/chedidandrew/smartresourcedrops/network/ConfigPatchFragmentPayload.class",
    "com/chedidandrew/smartresourcedrops/network/ConfigSnapshotFragmentPayload.class",
    "com/chedidandrew/smartresourcedrops/network/ConfigTransferCodec.class",
    "com/chedidandrew/smartresourcedrops/platform/fabric/FabricEntrypoint.class",
    "com/chedidandrew/smartresourcedrops/platform/fabric/FabricNetworking.class",
    "com/chedidandrew/smartresourcedrops/platform/fabric/FabricPlacementStorage.class",
    "com/chedidandrew/smartresourcedrops/platform/fabric/client/FabricClientEntrypoint.class",
    "com/chedidandrew/smartresourcedrops/platform/fabric/client/FabricModMenuIntegration.class",
}
EXPECTED_PRODUCTION_CLASSES = production_source_classes(
    ROOT / "src/main/java",
    ROOT / "src/client/java",
)
EXPECTED_CONTACT = {
    "homepage": "https://www.curseforge.com/minecraft/mc-mods/resource-multiplier",
    "issues": "https://github.com/chedidandrew/Resource-Multiplier/issues",
    "sources": "https://github.com/chedidandrew/Resource-Multiplier",
}
EXPECTED_ENTRYPOINTS = {
    "main": ["com.chedidandrew.smartresourcedrops.platform.fabric.FabricEntrypoint"],
    "client": ["com.chedidandrew.smartresourcedrops.platform.fabric.client.FabricClientEntrypoint"],
    "modmenu": ["com.chedidandrew.smartresourcedrops.platform.fabric.client.FabricModMenuIntegration"],
}
EXPECTED_MIXINS = [
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
]
EXPECTED_LINKS = {
    "smart_resource_drops.modmenu.link.kofi": "https://ko-fi.com/andrewchedid",
    "smart_resource_drops.modmenu.link.paypal": "https://www.paypal.com/paypalme/chedidandrew",
    "smart_resource_drops.modmenu.link.cash_app": "https://cash.app/%24AndrewChedid",
}
FORBIDDEN_TEST_CLASS_PREFIXES = (
    "com/chedidandrew/smartresourcedrops/client/FabricClientSmokeTest",
    "com/chedidandrew/smartresourcedrops/client/FabricMultiplayerClientSmokeTest",
    "com/chedidandrew/smartresourcedrops/platform/fabric/FabricMultiplayerServerSmokeTest",
    "com/chedidandrew/smartresourcedrops/platform/fabric/FabricPlacementPersistenceSmokeTest",
)


class ValidationError(RuntimeError):
    pass


def candidate() -> Path:
    path = ROOT / "build/libs" / f"smart-resource-multiplier-{VERSION}.jar"
    if not path.is_file():
        raise ValidationError(f"missing {path.relative_to(ROOT)}; run .\\gradlew.bat --no-daemon clean build")
    return path


def validate(path: Path) -> tuple[int, str]:
    errors: list[str] = []
    with zipfile.ZipFile(path) as archive:
        if archive.testzip() is not None:
            errors.append("JAR has a corrupt member")
        file_names = [info.filename for info in archive.infolist() if not info.is_dir()]
        names = set(file_names)
        folded: dict[str, list[str]] = {}
        for name in file_names:
            posix = PurePosixPath(name)
            lower = name.casefold()
            if not name or "\\" in name or name.startswith("/") or posix.as_posix() != name or any(part in {"", ".", ".."} for part in posix.parts):
                errors.append(f"unsafe archive member {name!r}")
            folded.setdefault(lower, []).append(name)
            if lower.endswith((".jar", ".java")):
                errors.append(f"nested archive/source entry leaked: {name}")
            if "/platform/neoforge/" in f"/{lower}" or lower.startswith("net/minecraftforge/") or lower.startswith("net/neoforged/"):
                errors.append(f"NeoForge entry leaked: {name}")
            if any(part.casefold() in {"clienttest", "gametest", "optionaltest", "test", "tests", "fixtures"} for part in posix.parts):
                errors.append(f"development/test entry leaked: {name}")
            if lower.startswith("data/smart_resource_drops_gametest/"):
                errors.append(f"GameTest resource leaked: {name}")
            if lower.endswith(".class"):
                top_level_class = posix.name[:-len(".class")].split("$", 1)[0].casefold()
                if top_level_class.endswith(("test", "tests")):
                    errors.append(f"compiled unit-test class leaked: {name}")
            if name.endswith(".class") and any(
                    name == prefix + ".class" or name.startswith(prefix + "$")
                    for prefix in FORBIDDEN_TEST_CLASS_PREFIXES):
                errors.append(f"compiled smoke-test class leaked: {name}")
        if any(len(values) != 1 for values in folded.values()):
            errors.append("archive has duplicate or case-colliding members")
        required = REQUIRED_CLASSES | EXPECTED_PRODUCTION_CLASSES | PRODUCTION_RESOURCES | {
            "fabric.mod.json", PACK_METADATA, ICON, LICENSE, MIXIN, REFMAP
        }
        missing = sorted(required - names)
        if missing:
            errors.append("missing entries: " + ", ".join(missing))
        if "META-INF/mods.toml" in names or "META-INF/neoforge.mods.toml" in names:
            errors.append("NeoForge metadata leaked")

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

        try:
            metadata = json.loads(archive.read("fabric.mod.json"))
        except (KeyError, json.JSONDecodeError, UnicodeDecodeError) as exc:
            errors.append(f"invalid fabric.mod.json: {exc}")
            metadata = {}
        expected = {
            "schemaVersion": 1,
            "id": MOD_ID,
            "version": VERSION,
            "name": "Smart Resource Multiplier",
            "environment": "*",
            "license": "MIT",
            "icon": ICON,
        }
        for key, value in expected.items():
            if metadata.get(key) != value:
                errors.append(f"fabric.mod.json {key} must be {value!r}, found {metadata.get(key)!r}")
        if metadata.get("description") != (
                "Configurable block, entity death-loot, and safe supported shearing multipliers "
                "with persistent anti-dupe protection."):
            errors.append("fabric.mod.json description drifted")
        if metadata.get("authors") != ["Andrew Chedid"]:
            errors.append("fabric.mod.json authors drifted")
        if metadata.get("contact") != EXPECTED_CONTACT:
            errors.append("fabric.mod.json contact metadata drifted")
        if metadata.get("entrypoints") != EXPECTED_ENTRYPOINTS:
            errors.append("fabric.mod.json entrypoints drifted")
        if metadata.get("mixins") != [MIXIN]:
            errors.append("fabric.mod.json mixin registration drifted")
        if metadata.get("custom") != {"modmenu": {"links": EXPECTED_LINKS}}:
            errors.append("fabric.mod.json Mod Menu support links drifted")
        depends = metadata.get("depends", {})
        expected_depends = {
            "fabricloader": ">=0.19.5",
            "minecraft": "1.20.1",
            "java": ">=17",
            "fabric-api": ">=0.92.12+1.20.1",
        }
        if depends != expected_depends:
            errors.append(f"unexpected Fabric dependencies: {depends!r}")
        if metadata.get("suggests", {}).get("modmenu") != ">=7.2.2":
            errors.append("Mod Menu must remain optional at >=7.2.2")

        try:
            mixin = json.loads(archive.read(MIXIN))
            if mixin.get("required") is not True or mixin.get("compatibilityLevel") != "JAVA_17":
                errors.append("production mixin config must be required and JAVA_17")
            if mixin.get("minVersion") != "0.8.5" or mixin.get("refmap") != REFMAP:
                errors.append("production mixin config must declare Mixin 0.8.5 and the generated refmap")
            package = mixin.get("package")
            declared = mixin.get("mixins", [])
            if package != "com.chedidandrew.smartresourcedrops.mixin":
                errors.append("production mixin package drifted")
            if declared != EXPECTED_MIXINS:
                errors.append(f"production mixin class list drifted: {declared!r}")
            if mixin.get("injectors") != {"defaultRequire": 1}:
                errors.append("production mixin injector policy drifted")
            if mixin.get("overwrites") != {"requireAnnotations": True}:
                errors.append("production mixin overwrite policy drifted")
            for entry in declared:
                class_name = f"{package}.{entry}".replace(".", "/") + ".class"
                if class_name not in names:
                    errors.append(f"mixin config declares missing class {class_name}")
        except (KeyError, json.JSONDecodeError, UnicodeDecodeError, TypeError) as exc:
            errors.append(f"invalid mixin config: {exc}")

        try:
            refmap = json.loads(archive.read(REFMAP))
            mappings = refmap.get("mappings") if isinstance(refmap, dict) else None
            data = refmap.get("data") if isinstance(refmap, dict) else None
            if not isinstance(mappings, dict) or not mappings:
                errors.append("refmap mappings must be a nonempty object")
            if not isinstance(data, dict) or not any(
                    isinstance(value, dict) and value for value in data.values()):
                errors.append("refmap data must contain at least one nonempty namespace map")
        except (KeyError, json.JSONDecodeError, UnicodeDecodeError) as exc:
            errors.append(f"invalid production refmap: {exc}")

        for name in names:
            if not name.endswith(".class"):
                continue
            body = archive.read(name)
            if len(body) < 8:
                errors.append(f"truncated class {name}")
                continue
            magic, _, major = struct.unpack(">IHH", body[:8])
            if magic != 0xCAFEBABE or major != 61:
                errors.append(f"{name} is not Java 17 class-major 61")
        if ICON in names and archive.read(ICON) != (ROOT / "src/main/resources/assets/smart_resource_drops/icon.png").read_bytes():
            errors.append("embedded icon differs from source")
        if LICENSE in names and archive.read(LICENSE) != (ROOT / "LICENSE").read_bytes():
            errors.append("embedded license differs from source")

    if errors:
        raise ValidationError("; ".join(errors))
    return len(file_names), hashlib.sha256(path.read_bytes()).hexdigest().upper()


def main() -> int:
    try:
        path = candidate()
        entries, digest = validate(path)
    except (OSError, zipfile.BadZipFile, ValidationError) as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        return 1
    print(f"PASS: {path.name} is a Java 17 Fabric production JAR ({entries} entries, SHA-256 {digest})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
