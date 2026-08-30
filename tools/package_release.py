#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import os
import shutil
import stat
import zipfile
from collections.abc import Iterable, Sequence
from pathlib import Path, PurePosixPath

ROOT = Path(__file__).resolve().parent.parent
FIXED_TIME = (2026, 8, 29, 0, 0, 0)
PUBLIC_MOD_NAME = "Resource Multiplier"
PUBLIC_ARCHIVE_BASE = "ResourceMultiplier"
PLAYABLE_JAR_BASE = "resource-multiplier"
REQUIRED_SOURCE_FILES = frozenset(
    {
        ".gitignore",
        ".github/ISSUE_TEMPLATE/bug_report.yml",
        ".github/ISSUE_TEMPLATE/config.yml",
        ".github/ISSUE_TEMPLATE/mod_compatibility.yml",
        ".github/PULL_REQUEST_TEMPLATE.md",
        ".github/workflows/build.yml",
        ".github/workflows/release.yml",
        "BUILD_STATUS.md",
        "CHANGELOG.md",
        "CONTRIBUTING.md",
        "LICENSE",
        "README.md",
        "SECURITY.md",
        "build.gradle",
        "docs/COMPATIBILITY.md",
        "docs/CONFIGURATION.md",
        "docs/IMPLEMENTATION_LOG.md",
        "docs/PERFORMANCE.md",
        "docs/PUBLIC_RELEASE_CHECKLIST.md",
        "docs/ROADMAP.md",
        "docs/TESTING.md",
        "gradle.properties",
        "gradlew",
        "gradlew.bat",
        "gradle/wrapper/gradle-wrapper.jar",
        "gradle/wrapper/gradle-wrapper.properties",
        "settings.gradle",
        "src/main/java/com/chedidandrew/smartresourcedrops/SmartResourceDrops.java",
        "src/main/java/com/chedidandrew/smartresourcedrops/config/ConfigLoadDiagnostics.java",
        "src/main/java/com/chedidandrew/smartresourcedrops/command/ConfigValidationFormatter.java",
        "src/main/java/com/chedidandrew/smartresourcedrops/config/ConfigValidationReport.java",
        "src/main/java/com/chedidandrew/smartresourcedrops/config/ConfigValidator.java",
        "src/main/java/com/chedidandrew/smartresourcedrops/config/LiveConfigRegistryView.java",
        "src/main/java/com/chedidandrew/smartresourcedrops/config/ConfigRegistryView.java",
        "src/main/java/com/chedidandrew/smartresourcedrops/core/BlockLootBudgetWarnings.java",
        "src/main/java/com/chedidandrew/smartresourcedrops/core/util/BlockLootOutputBudget.java",
        "src/main/java/com/chedidandrew/smartresourcedrops/core/util/BoundedRateLimiter.java",
        "src/main/java/com/chedidandrew/smartresourcedrops/core/util/LootOutputBudget.java",
        "src/main/java/com/chedidandrew/smartresourcedrops/core/entity/EntityClassifier.java",
        "src/main/java/com/chedidandrew/smartresourcedrops/core/entity/EntityDropTags.java",
        "src/main/java/com/chedidandrew/smartresourcedrops/core/entity/EntityLootTags.java",
        "src/main/java/com/chedidandrew/smartresourcedrops/core/shearing/ShearingActionContext.java",
        "src/main/java/com/chedidandrew/smartresourcedrops/core/shearing/ShearingOutputBudget.java",
        "src/main/java/com/chedidandrew/smartresourcedrops/core/shearing/ShearingRuleResolver.java",
        "src/main/java/com/chedidandrew/smartresourcedrops/core/shearing/ShearingRuleTrace.java",
        "src/main/java/com/chedidandrew/smartresourcedrops/core/shearing/ShearingTags.java",
        "src/main/java/com/chedidandrew/smartresourcedrops/mixin/LivingEntityShearingLootMixin.java",
        "src/main/java/com/chedidandrew/smartresourcedrops/mixin/PlayerShearingContextMixin.java",
        "src/main/java/com/chedidandrew/smartresourcedrops/mixin/ShearsDispenseItemBehaviorMixin.java",
        "src/client/java/com/chedidandrew/smartresourcedrops/client/ShearingDropsScreen.java",
        "src/client/java/com/chedidandrew/smartresourcedrops/client/ShearingOverridesScreen.java",
        "src/client/java/com/chedidandrew/smartresourcedrops/client/ShearingRuleEditScreen.java",
        "src/main/resources/assets/smart_resource_drops/icon.png",
        "src/main/resources/assets/smart_resource_drops/lang/en_us.json",
        "src/main/resources/data/smart_resource_drops/tags/block/categories/building_blocks.json",
        "src/main/resources/data/smart_resource_drops/tags/block/categories/crops.json",
        "src/main/resources/data/smart_resource_drops/tags/block/categories/end.json",
        "src/main/resources/data/smart_resource_drops/tags/block/categories/leaves.json",
        "src/main/resources/data/smart_resource_drops/tags/block/categories/logs.json",
        "src/main/resources/data/smart_resource_drops/tags/block/categories/nether.json",
        "src/main/resources/data/smart_resource_drops/tags/block/categories/ores.json",
        "src/main/resources/data/smart_resource_drops/tags/block/categories/plants.json",
        "src/main/resources/data/smart_resource_drops/tags/block/categories/raw_resource_blocks.json",
        "src/main/resources/data/smart_resource_drops/tags/block/categories/soil.json",
        "src/main/resources/data/smart_resource_drops/tags/block/categories/stone.json",
        "src/main/resources/data/smart_resource_drops/tags/item/protected_entity_loot.json",
        "src/main/resources/data/smart_resource_drops/tags/entity_type/shearing/standard_resources.json",
        "src/main/resources/data/smart_resource_drops/tags/entity_type/shearing/special.json",
        "src/main/resources/fabric.mod.json",
        "src/main/resources/smart_resource_drops.mixins.json",
        "src/test/java/com/chedidandrew/smartresourcedrops/config/ConfigValidatorTest.java",
        "src/test/resources/config/migration/schema-1.json",
        "src/test/resources/config/migration/schema-2.json",
        "src/test/java/com/chedidandrew/smartresourcedrops/core/util/BlockLootOutputBudgetTest.java",
        "src/test/java/com/chedidandrew/smartresourcedrops/core/shearing/ShearingOutputBudgetTest.java",
        "src/test/java/com/chedidandrew/smartresourcedrops/core/shearing/ShearingOutputBufferTest.java",
        "src/test/java/com/chedidandrew/smartresourcedrops/core/shearing/ShearingRuleResolverTest.java",
        "src/gametest/java/com/chedidandrew/smartresourcedrops/gametest/SmartResourceDropsBlockBudgetGameTests.java",
        "src/gametest/java/com/chedidandrew/smartresourcedrops/gametest/SmartResourceDropsShearingGameTests.java",
        "src/gametest/java/com/chedidandrew/smartresourcedrops/core/shearing/ShearingGameTestAccess.java",
        "src/gametest/java/com/chedidandrew/smartresourcedrops/gametest/fixture/GameTestBlockLootFixtures.java",
        "tools/package_release.py",
        "tools/validate_package.py",
    }
)
EXCLUDED_PARTS = frozenset(
    {
    ".build",
    ".git",
    ".gradle",
    ".gradle-wrapper",
    ".idea",
    ".fleet",
    ".vs",
    ".vscode",
    "__pycache__",
    "build",
    "dist",
    "out",
    "run",
    "logs",
    "world",
    }
)
EXCLUDED_FILE_NAMES = frozenset(
    {
        ".classpath",
        ".project",
        ".DS_Store",
        "Thumbs.db",
        "command_history.txt",
        "eula.txt",
        "ops.json",
        "options.txt",
        "server.properties",
        "servers.dat",
        "usercache.json",
        "whitelist.json",
    }
)
EXCLUDED_SUFFIXES = frozenset({".class", ".iml", ".ipr", ".iws", ".log", ".pyc"})
EXCLUDED_ENDINGS = (".log.gz", ".tmp")
JAR_BUILD_INPUT_FILES = (
    "LICENSE",
    "build.gradle",
    "settings.gradle",
    "gradle.properties",
    "gradlew",
    "gradlew.bat",
    "gradle/wrapper/gradle-wrapper.jar",
    "gradle/wrapper/gradle-wrapper.properties",
)
REQUIRED_RELEASE_JAR_ENTRIES = frozenset(
    {
        "fabric.mod.json",
        "smart_resource_drops.mixins.json",
        "LICENSE_resource-multiplier",
        "com/chedidandrew/smartresourcedrops/SmartResourceDrops.class",
        "com/chedidandrew/smartresourcedrops/client/SmartResourceDropsClient.class",
        "com/chedidandrew/smartresourcedrops/client/SmartResourceDropsModMenuIntegration.class",
        "com/chedidandrew/smartresourcedrops/config/ConfigLoadDiagnostics.class",
        "com/chedidandrew/smartresourcedrops/config/ConfigValidationReport.class",
        "com/chedidandrew/smartresourcedrops/config/ConfigValidator.class",
        "com/chedidandrew/smartresourcedrops/core/BlockLootBudgetWarnings.class",
        "com/chedidandrew/smartresourcedrops/core/util/BlockLootOutputBudget.class",
        "com/chedidandrew/smartresourcedrops/core/util/BoundedRateLimiter.class",
        "com/chedidandrew/smartresourcedrops/core/util/LootOutputBudget.class",
        "com/chedidandrew/smartresourcedrops/core/entity/EntityClassifier.class",
        "com/chedidandrew/smartresourcedrops/core/entity/EntityDropTags.class",
        "com/chedidandrew/smartresourcedrops/core/entity/EntityLootTags.class",
        "com/chedidandrew/smartresourcedrops/core/shearing/ShearingActionContext.class",
        "com/chedidandrew/smartresourcedrops/core/shearing/ShearingOutputBudget.class",
        "com/chedidandrew/smartresourcedrops/core/shearing/ShearingRuleResolver.class",
        "com/chedidandrew/smartresourcedrops/core/shearing/ShearingRuleTrace.class",
        "com/chedidandrew/smartresourcedrops/core/shearing/ShearingTags.class",
        "com/chedidandrew/smartresourcedrops/mixin/LivingEntityShearingLootMixin.class",
        "com/chedidandrew/smartresourcedrops/mixin/PlayerShearingContextMixin.class",
        "com/chedidandrew/smartresourcedrops/mixin/ShearsDispenseItemBehaviorMixin.class",
        "assets/smart_resource_drops/icon.png",
        "assets/smart_resource_drops/lang/en_us.json",
        "data/smart_resource_drops/tags/block/categories/building_blocks.json",
        "data/smart_resource_drops/tags/block/categories/crops.json",
        "data/smart_resource_drops/tags/block/categories/end.json",
        "data/smart_resource_drops/tags/block/categories/leaves.json",
        "data/smart_resource_drops/tags/block/categories/logs.json",
        "data/smart_resource_drops/tags/block/categories/nether.json",
        "data/smart_resource_drops/tags/block/categories/ores.json",
        "data/smart_resource_drops/tags/block/categories/plants.json",
        "data/smart_resource_drops/tags/block/categories/raw_resource_blocks.json",
        "data/smart_resource_drops/tags/block/categories/soil.json",
        "data/smart_resource_drops/tags/block/categories/stone.json",
        "data/smart_resource_drops/tags/entity_type/categories/ambient.json",
        "data/smart_resource_drops/tags/entity_type/categories/aquatic.json",
        "data/smart_resource_drops/tags/entity_type/categories/bosses.json",
        "data/smart_resource_drops/tags/entity_type/categories/golems.json",
        "data/smart_resource_drops/tags/entity_type/categories/hostile.json",
        "data/smart_resource_drops/tags/entity_type/categories/miscellaneous.json",
        "data/smart_resource_drops/tags/entity_type/categories/neutral.json",
        "data/smart_resource_drops/tags/entity_type/categories/passive.json",
        "data/smart_resource_drops/tags/entity_type/categories/villagers_npcs.json",
        "data/smart_resource_drops/tags/item/protected_entity_loot.json",
        "data/smart_resource_drops/tags/entity_type/shearing/standard_resources.json",
        "data/smart_resource_drops/tags/entity_type/shearing/special.json",
    }
)
FORBIDDEN_RELEASE_JAR_PREFIXES = (
    "com/terraformersmc/",
    "com/google/",
    "com/mojang/",
    "data/smart_resource_drops_gametest/",
    "net/fabricmc/",
    "net/minecraft/",
    "org/apiguardian/",
    "org/assertj/",
    "org/hamcrest/",
    "org/junit/",
    "org/opentest4j/",
    "org/slf4j/",
    "org/spongepowered/",
    "it/unimi/",
    "io/netty/",
    "src/",
    "docs/",
    "scripts/",
    "tools/",
    "config/",
    "gradle/",
    "run/",
    "logs/",
    "world/",
)
FORBIDDEN_RELEASE_JAR_SUFFIXES = (
    ".jar",
    ".java",
    ".kt",
    ".py",
    ".ps1",
    ".sh",
    ".bat",
    ".gradle",
)
FORBIDDEN_RELEASE_JAR_PARTS = frozenset(
    {
        "fixture",
        "fixtures",
        "gametest",
        "testmod",
    }
)
FORBIDDEN_RELEASE_JAR_NAMES = frozenset(
    {
        "command_history.txt",
        "eula.txt",
        "ops.json",
        "server.properties",
        "usercache.json",
        "whitelist.json",
        "build.gradle",
        "settings.gradle",
        "gradle.properties",
        "gradlew",
        "gradlew.bat",
        "readme.md",
    }
)


class ReleasePackageError(RuntimeError):
    """Raised when a source package would be incomplete or contain generated data."""


def parse_properties(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()
    return values


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def zip_info(name: str, mode: int = 0o644) -> zipfile.ZipInfo:
    info = zipfile.ZipInfo(name, FIXED_TIME)
    info.create_system = 3
    info.external_attr = (stat.S_IFREG | mode) << 16
    info.compress_type = zipfile.ZIP_DEFLATED
    return info


def write_file(
    archive: zipfile.ZipFile,
    source: Path,
    archive_name: str,
    mode: int | None = None,
) -> None:
    if mode is None:
        mode = source.stat().st_mode & 0o777
    archive.writestr(zip_info(archive_name, mode), source.read_bytes())


def is_forbidden_source_path(relative: Path | PurePosixPath) -> bool:
    parts = tuple(part.casefold() for part in relative.parts)
    if not parts:
        return True
    if any(part in EXCLUDED_PARTS for part in parts[:-1]):
        return True

    name = parts[-1]
    if name in {item.casefold() for item in EXCLUDED_FILE_NAMES}:
        return True
    if any(name.endswith(suffix) for suffix in EXCLUDED_SUFFIXES):
        return True
    if name.endswith(EXCLUDED_ENDINGS):
        return True
    if name.startswith("smart_resource_drops.broken-"):
        return True
    if name.startswith("smart_resource_drops.oversized-"):
        return True

    # Runtime configuration belongs to a Minecraft instance, never a source release.
    if parts[0] == "config" and name.startswith("smart_resource_drops"):
        return True

    # Do not recursively package artifacts when an output directory is placed in the project.
    if name.startswith(("resourcemultiplier-", "smartresourcedrops-")) and (
        name.endswith("-source.zip")
        or name.endswith("-release-bundle.zip")
        or name.endswith("-release-bundle.zip.sha256")
        or name.endswith("-sha256sums.txt")
        or name.endswith("-package_readme.txt")
        or name.endswith("-build_status.md")
    ):
        return True
    if name.startswith(("resource-multiplier-", "smart-resource-drops-")) and name.endswith(".jar"):
        return True
    return False


def validate_source_entries(relative_names: Iterable[str]) -> tuple[str, ...]:
    names = tuple(relative_names)
    errors: list[str] = []
    normalized: list[str] = []

    for name in names:
        path = PurePosixPath(name)
        if (
            not name
            or "\\" in name
            or name.startswith("/")
            or path.is_absolute()
            or path.as_posix() != name
            or any(part in {"", ".", ".."} for part in path.parts)
        ):
            errors.append(f"unsafe source entry {name!r}")
            continue
        normalized.append(name)
        if is_forbidden_source_path(path):
            errors.append(f"forbidden generated/runtime entry {name!r}")

    collisions: dict[str, list[str]] = {}
    for name in normalized:
        collisions.setdefault(name.casefold(), []).append(name)
    for values in collisions.values():
        if len(values) > 1:
            errors.append(f"duplicate or case-colliding entries: {', '.join(values)}")

    missing = sorted(REQUIRED_SOURCE_FILES.difference(normalized))
    if missing:
        errors.append(f"missing required source files: {', '.join(missing)}")

    if errors:
        raise ReleasePackageError("; ".join(errors))
    return tuple(sorted(normalized))


def source_files() -> list[Path]:
    files: list[Path] = []
    for path in ROOT.rglob("*"):
        if path.is_symlink():
            raise ReleasePackageError(
                f"Symbolic links are not supported in the source package: {path.relative_to(ROOT)}"
            )
        if not path.is_file():
            continue
        relative = path.relative_to(ROOT)
        if is_forbidden_source_path(relative):
            continue
        files.append(path)
    files.sort(key=lambda item: item.relative_to(ROOT).as_posix())
    validate_source_entries(path.relative_to(ROOT).as_posix() for path in files)
    return files


def jar_build_inputs() -> list[Path]:
    """Return files whose modification requires rebuilding or retesting the release JAR.

    Documentation is intentionally absent: BUILD_STATUS records the already-built JAR's
    identity and must be allowed to change before the final source archive is packaged.
    """
    files = [ROOT / relative for relative in JAR_BUILD_INPUT_FILES]
    files.extend(path for path in (ROOT / "src").rglob("*") if path.is_file())
    missing = [str(path.relative_to(ROOT)) for path in files if not path.is_file()]
    if missing:
        raise ReleasePackageError(f"missing JAR build inputs: {', '.join(sorted(missing))}")
    return sorted(set(files), key=lambda path: path.relative_to(ROOT).as_posix())


def validate_release_jar(
    jar_path: Path,
    expected_version: str | None = None,
) -> tuple[str, ...]:
    """Reject test fixtures, bundled dependencies, and runtime data from the playable JAR."""
    errors: list[str] = []
    names: list[str] = []
    try:
        with zipfile.ZipFile(jar_path) as archive:
            bad_member = archive.testzip()
            if bad_member is not None:
                errors.append(f"corrupt JAR member {bad_member!r}")
            for info in archive.infolist():
                if info.is_dir():
                    continue
                name = info.filename
                path = PurePosixPath(name)
                names.append(name)
                if (
                    not name
                    or "\\" in name
                    or name.startswith("/")
                    or path.is_absolute()
                    or path.as_posix() != name
                    or any(part in {"", ".", ".."} for part in path.parts)
                ):
                    errors.append(f"unsafe release JAR entry {name!r}")
                    continue

                folded = name.casefold()
                parts = tuple(part.casefold() for part in path.parts)
                if any(folded.startswith(prefix) for prefix in FORBIDDEN_RELEASE_JAR_PREFIXES):
                    errors.append(f"bundled dependency, test fixture, or runtime entry {name!r}")
                if any(part in FORBIDDEN_RELEASE_JAR_PARTS for part in parts):
                    errors.append(f"development-only test fixture entry {name!r}")
                if parts[-1] in FORBIDDEN_RELEASE_JAR_NAMES:
                    errors.append(f"runtime/server entry {name!r}")
                if folded.endswith(FORBIDDEN_RELEASE_JAR_SUFFIXES):
                    errors.append(f"nested archive or source-package entry {name!r}")
                if folded.endswith(".gametest.mixins.json"):
                    errors.append(f"development-only GameTest mixin entry {name!r}")

            collisions: dict[str, list[str]] = {}
            for name in names:
                collisions.setdefault(name.casefold(), []).append(name)
            for values in collisions.values():
                if len(values) > 1:
                    errors.append(f"duplicate or case-colliding JAR entries: {', '.join(values)}")

            missing = sorted(REQUIRED_RELEASE_JAR_ENTRIES.difference(names))
            if missing:
                errors.append(f"missing required production JAR entries: {', '.join(missing)}")

            try:
                metadata = json.loads(archive.read("fabric.mod.json"))
            except (KeyError, json.JSONDecodeError) as exc:
                errors.append(f"invalid release JAR metadata: {exc}")
            else:
                if metadata.get("id") != "smart_resource_drops":
                    errors.append(
                        "release JAR metadata uses mod id "
                        f"{metadata.get('id')!r} instead of 'smart_resource_drops'"
                    )
                if metadata.get("name") != PUBLIC_MOD_NAME:
                    errors.append(
                        "release JAR metadata uses display name "
                        f"{metadata.get('name')!r} instead of {PUBLIC_MOD_NAME!r}"
                    )
                if expected_version is not None and metadata.get("version") != expected_version:
                    errors.append(
                        f"release JAR version {metadata.get('version')!r} does not match "
                        f"gradle.properties {expected_version!r}"
                    )

                entrypoints = metadata.get("entrypoints", {})
                if isinstance(entrypoints, dict):
                    for group, declarations in entrypoints.items():
                        if not isinstance(declarations, list):
                            errors.append(f"invalid {group!r} entrypoint declarations")
                            continue
                        for declaration in declarations:
                            value = declaration if isinstance(declaration, str) else (
                                declaration.get("value") if isinstance(declaration, dict) else None
                            )
                            if not isinstance(value, str) or not value:
                                errors.append(f"invalid {group!r} entrypoint {declaration!r}")
                                continue
                            class_name = value.split("::", 1)[0]
                            class_entry = class_name.replace(".", "/") + ".class"
                            if class_entry not in names:
                                errors.append(
                                    f"missing declared {group!r} entrypoint class {class_entry!r}"
                                )

                mixin_declarations = metadata.get("mixins", [])
                if isinstance(mixin_declarations, list):
                    for declaration in mixin_declarations:
                        config_name = declaration if isinstance(declaration, str) else (
                            declaration.get("config") if isinstance(declaration, dict) else None
                        )
                        if not isinstance(config_name, str) or not config_name:
                            errors.append(f"invalid mixin declaration {declaration!r}")
                            continue
                        try:
                            mixin_config = json.loads(archive.read(config_name))
                        except (KeyError, json.JSONDecodeError) as exc:
                            errors.append(f"invalid declared mixin config {config_name!r}: {exc}")
                            continue
                        mixin_package = mixin_config.get("package")
                        if not isinstance(mixin_package, str) or not mixin_package:
                            errors.append(f"declared mixin config {config_name!r} has no package")
                            continue
                        for side in ("mixins", "client", "server"):
                            classes = mixin_config.get(side, [])
                            if not isinstance(classes, list):
                                errors.append(f"invalid {side!r} list in {config_name!r}")
                                continue
                            for class_name in classes:
                                if not isinstance(class_name, str) or not class_name:
                                    errors.append(f"invalid mixin class in {config_name!r}")
                                    continue
                                class_entry = (
                                    f"{mixin_package}.{class_name}".replace(".", "/") + ".class"
                                )
                                if class_entry not in names:
                                    errors.append(
                                        f"missing declared mixin class {class_entry!r}"
                                    )

                for tag_entry in sorted(
                    entry for entry in REQUIRED_RELEASE_JAR_ENTRIES
                    if entry.startswith("data/smart_resource_drops/tags/")
                ):
                    try:
                        tag_data = json.loads(archive.read(tag_entry))
                    except (KeyError, json.JSONDecodeError) as exc:
                        errors.append(f"invalid production tag {tag_entry!r}: {exc}")
                        continue
                    if not isinstance(tag_data, dict):
                        errors.append(f"production tag {tag_entry!r} must be a JSON object")
                        continue
                    if tag_data.get("replace") is not False:
                        errors.append(f"production tag {tag_entry!r} must use replace=false")
                    values = tag_data.get("values")
                    if not isinstance(values, list):
                        errors.append(f"production tag {tag_entry!r} has no values list")
                    elif tag_entry.endswith("/item/protected_entity_loot.json"):
                        protected = {"minecraft:saddle", "minecraft:totem_of_undying"}
                        if not protected.issubset(set(values)):
                            errors.append(
                                f"protected entity-loot tag {tag_entry!r} is missing saddle or totem"
                            )
                    elif tag_entry.endswith("/entity_type/shearing/standard_resources.json"):
                        if values != ["minecraft:sheep"]:
                            errors.append(
                                f"production standard shearing tag {tag_entry!r} must contain only minecraft:sheep"
                            )
                    elif tag_entry.endswith("/entity_type/shearing/special.json"):
                        expected_special = {
                            "minecraft:bogged",
                            "minecraft:copper_golem",
                            "minecraft:mooshroom",
                            "minecraft:snow_golem",
                            "minecraft:sulfur_cube",
                        }
                        if set(values) != expected_special or len(values) != len(expected_special):
                            errors.append(
                                f"production special shearing tag {tag_entry!r} differs from the audited vanilla safety set"
                            )
    except (OSError, zipfile.BadZipFile) as exc:
        raise ReleasePackageError(f"cannot read release JAR {jar_path}: {exc}") from exc

    if errors:
        raise ReleasePackageError("; ".join(errors))
    return tuple(sorted(names))


def validate_source_zip(
    output: Path,
    top_level: str,
    expected_relatives: Sequence[str] | None = None,
) -> tuple[str, ...]:
    prefix = f"{top_level}/"
    errors: list[str] = []
    relative_names: list[str] = []
    info_by_relative: dict[str, zipfile.ZipInfo] = {}

    try:
        with zipfile.ZipFile(output) as archive:
            bad_member = archive.testzip()
            if bad_member is not None:
                errors.append(f"corrupt ZIP member {bad_member!r}")
            for info in archive.infolist():
                name = info.filename
                if info.is_dir():
                    errors.append(f"unexpected directory entry {name!r}")
                    continue
                if not name.startswith(prefix):
                    errors.append(f"entry outside required top-level directory: {name!r}")
                    continue
                relative = name[len(prefix) :]
                relative_names.append(relative)
                info_by_relative[relative] = info
    except (OSError, zipfile.BadZipFile) as exc:
        raise ReleasePackageError(f"cannot read source ZIP {output}: {exc}") from exc

    if errors:
        raise ReleasePackageError("; ".join(errors))
    validated = validate_source_entries(relative_names)

    if expected_relatives is not None:
        expected = set(expected_relatives)
        actual = set(validated)
        missing = sorted(expected - actual)
        unexpected = sorted(actual - expected)
        if missing or unexpected:
            details = []
            if missing:
                details.append(f"missing manifest entries: {', '.join(missing)}")
            if unexpected:
                details.append(f"unexpected manifest entries: {', '.join(unexpected)}")
            raise ReleasePackageError("; ".join(details))

    gradlew_mode = (info_by_relative["gradlew"].external_attr >> 16) & 0o777
    if gradlew_mode & 0o111 == 0:
        raise ReleasePackageError("gradlew is not executable in the source ZIP")
    return validated


def build_source_zip(
    output: Path,
    top_level: str,
    files: Sequence[Path] | None = None,
) -> tuple[str, ...]:
    selected = list(files) if files is not None else source_files()
    relative_names = validate_source_entries(
        path.relative_to(ROOT).as_posix() for path in selected
    )
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for path in selected:
            relative = path.relative_to(ROOT).as_posix()
            mode = 0o755 if relative == "gradlew" else None
            write_file(archive, path, f"{top_level}/{relative}", mode)
    return validate_source_zip(output, top_level, relative_names)


def build_bundle(output: Path, files: list[Path]) -> None:
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for path in sorted(files, key=lambda item: item.name):
            write_file(archive, path, path.name)


def main() -> None:
    parser = argparse.ArgumentParser(description="Create deterministic Resource Multiplier release packages.")
    parser.add_argument("--output-dir", type=Path, default=ROOT.parent)
    parser.add_argument(
        "--source-only",
        action="store_true",
        help="Build and validate only the deterministic public source ZIP.",
    )
    args = parser.parse_args()

    properties = parse_properties(ROOT / "gradle.properties")
    version = properties["mod_version"]
    minecraft_version = properties["minecraft_version"]
    prefix = f"{PUBLIC_ARCHIVE_BASE}-{version}"
    jar_name = f"{PLAYABLE_JAR_BASE}-{version}.jar"
    source_name = f"{prefix}-source.zip"
    checksum_name = f"{prefix}-SHA256SUMS.txt"
    package_readme_name = f"{prefix}-PACKAGE_README.txt"
    bundle_name = f"{prefix}-release-bundle.zip"

    output_dir = args.output_dir.resolve()
    output_dir.mkdir(parents=True, exist_ok=True)

    source_output = output_dir / source_name
    try:
        source_inputs = source_files()
        if args.source_only:
            source_entries = build_source_zip(source_output, prefix, source_inputs)
            print(f"Created and validated {source_output} ({len(source_entries)} source files)")
            return
    except ReleasePackageError as exc:
        raise SystemExit(f"Source package validation failed: {exc}") from exc

    jar_source = ROOT / "build" / "libs" / jar_name
    if not jar_source.is_file():
        raise SystemExit(f"Missing Loom-built JAR: {jar_source}. Run ./gradlew clean build first.")
    try:
        build_inputs = jar_build_inputs()
    except ReleasePackageError as exc:
        raise SystemExit(f"Release JAR input validation failed: {exc}") from exc
    newest_input = max(path.stat().st_mtime for path in build_inputs)
    if jar_source.stat().st_mtime + 1 < newest_input:
        raise SystemExit(
            f"Stale Loom-built JAR: {jar_source}. Java/resource/test or Gradle build inputs are newer; "
            "run ./gradlew clean test runGameTest build first."
        )
    try:
        validate_release_jar(jar_source, version)
    except ReleasePackageError as exc:
        raise SystemExit(f"Release JAR content validation failed: {exc}") from exc

    jar_output = output_dir / jar_name
    checksum_output = output_dir / checksum_name
    package_readme_output = output_dir / package_readme_name
    bundle_output = output_dir / bundle_name
    bundle_checksum_output = output_dir / f"{bundle_name}.sha256"

    shutil.copyfile(jar_source, jar_output)
    os.chmod(jar_output, 0o644)
    try:
        build_source_zip(source_output, prefix, source_inputs)
    except ReleasePackageError as exc:
        raise SystemExit(f"Source package validation failed: {exc}") from exc

    package_readme = f"""Resource Multiplier {version} package\n\nTarget: Minecraft Java {minecraft_version}, Fabric Loader {properties['loader_version']}, Fabric API {properties['fabric_version']}, Java 25.\n\nFiles:\n- {jar_name}: Fabric Loom-built release JAR produced by `gradlew clean build`.\n- {source_name}: Complete GitHub-ready source, tests, documentation, Gradle build, and GitHub Actions workflows.\n- {checksum_name}: SHA-256 hashes for the release JAR and source archive.\n- {prefix}-BUILD_STATUS.md: Validation status and remaining manual in-game release checks.\n\nRelease gate:\nOnly package and publish a JAR produced by the real Java 25 Fabric Loom build. Complete the manual in-game checks in docs/PUBLIC_RELEASE_CHECKLIST.md before making a public release.\n"""
    package_readme_output.write_text(package_readme, encoding="utf-8", newline="\n")

    checksums = [jar_output, source_output]
    checksum_output.write_text(
        "".join(f"{sha256(path)}  {path.name}\n" for path in checksums),
        encoding="utf-8",
        newline="\n",
    )

    status_output = output_dir / f"{prefix}-BUILD_STATUS.md"
    shutil.copyfile(ROOT / "BUILD_STATUS.md", status_output)

    build_bundle(
        bundle_output,
        [jar_output, source_output, checksum_output, package_readme_output, status_output],
    )
    bundle_checksum_output.write_text(
        f"{sha256(bundle_output)}  {bundle_output.name}\n",
        encoding="utf-8",
        newline="\n",
    )

    print(f"Created {source_output}")
    print(f"Created {jar_output}")
    print(f"Created {checksum_output}")
    print(f"Created {bundle_output}")
    print(f"Created {bundle_checksum_output}")


if __name__ == "__main__":
    main()
