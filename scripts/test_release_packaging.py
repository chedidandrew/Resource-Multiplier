#!/usr/bin/env python3
from __future__ import annotations

import importlib.util
import json
import tempfile
import zipfile
from pathlib import Path, PurePosixPath

ROOT = Path(__file__).resolve().parent.parent


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


spec = importlib.util.spec_from_file_location(
    "smart_resource_drops_package_release",
    ROOT / "tools/package_release.py",
)
require(spec is not None and spec.loader is not None, "Could not load package_release.py")
package_release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(package_release)

neoforge_validator_spec = importlib.util.spec_from_file_location(
    "smart_resource_drops_validate_neoforge_jar",
    ROOT / "tools/validate_neoforge_jar.py",
)
require(
    neoforge_validator_spec is not None and neoforge_validator_spec.loader is not None,
    "Could not load validate_neoforge_jar.py",
)
validate_neoforge_jar = importlib.util.module_from_spec(neoforge_validator_spec)
neoforge_validator_spec.loader.exec_module(validate_neoforge_jar)

for development_entry in (
    "com/chedidandrew/smartresourcedrops/client/NeoForgeClientCategorySmokeTest.class",
    "com/chedidandrew/smartresourcedrops/client/NeoForgeMultiplayerClientSmokeTest.class",
    "com/chedidandrew/smartresourcedrops/client/NeoForgeMultiplayerClientSmokeTest$Phase.class",
    "com/chedidandrew/smartresourcedrops/client/NeoForgeOptionalClientOnlySmokeTest.class",
    "com/chedidandrew/smartresourcedrops/client/NeoForgeOversizedWireClientSmokeTest.class",
    "com/chedidandrew/smartresourcedrops/optionaltest/NeoForgeOptionalChannelServerProbe.class",
    "com/chedidandrew/smartresourcedrops/optionaltest/NeoForgeOptionalServerOnlyClientSmokeTest.class",
    "com/chedidandrew/smartresourcedrops/optionaltest/OptionalChannelIds.class",
    "com/chedidandrew/smartresourcedrops/platform/neoforge/NeoForgeMultiplayerServerSmokeTest.class",
    "com/chedidandrew/smartresourcedrops/platform/neoforge/NeoForgePlacementPersistenceSmokeTest.class",
    "com/chedidandrew/smartresourcedrops/platform/neoforge/NeoForgeOversizedWireServerSmokeTest.class",
    "com/chedidandrew/smartresourcedrops/packagedprobe/PackagedClientProbe.class",
    "com/chedidandrew/smartresourcedrops/packagedprobe/PackagedProbeSupport.class",
    "com/chedidandrew/smartresourcedrops/packagedprobe/PackagedServerProbe.class",
):
    require(
        validate_neoforge_jar.is_development_test_entry(development_entry),
        f"NeoForge JAR validator accepted development-only entry {development_entry}",
    )
require(
    not validate_neoforge_jar.is_development_test_entry(
        "com/chedidandrew/smartresourcedrops/platform/neoforge/NeoForgeNetworking.class"
    ),
    "NeoForge JAR validator rejected a production adapter as test-only",
)
require(package_release.PUBLIC_MOD_NAME == "Smart Resource Multiplier", "Public mod name contract changed")
require(package_release.PUBLIC_ARCHIVE_BASE == "SmartResourceMultiplier", "Release archive base changed")
require(package_release.PLAYABLE_JAR_BASE == "smart-resource-multiplier", "Playable JAR base changed")
require(
    package_release.EXPECTED_MODMENU_LINKS
    == {'smart_resource_drops.modmenu.link.kofi': 'https://ko-fi.com/andrewchedid', 'smart_resource_drops.modmenu.link.paypal': 'https://www.paypal.com/paypalme/chedidandrew', 'smart_resource_drops.modmenu.link.cash_app': 'https://cash.app/%24AndrewChedid'},
    "Mod Menu support-link contract changed",
)
require(
    package_release.EXPECTED_CONTACT
    == {
        "homepage": "https://www.curseforge.com/minecraft/mc-mods/resource-multiplier",
        "issues": "https://github.com/chedidandrew/Resource-Multiplier/issues",
        "sources": "https://github.com/chedidandrew/Resource-Multiplier",
    },
    "Public project contact metadata changed",
)


def write_minimum_release_entries(
    archive: zipfile.ZipFile,
    *,
    omitted: str | None = None,
    mixin_classes: list[str] | None = None,
    mixin_config_overrides: dict[str, object] | None = None,
    metadata_overrides: dict[str, object] | None = None,
    license_payload: bytes | None = None,
    icon_payload: bytes | None = None,
    class_major: int = package_release.EXPECTED_FABRIC_CLASS_MAJOR,
) -> None:
    metadata = {
        "schemaVersion": 1,
        "id": "smart_resource_drops",
        "name": "Smart Resource Multiplier",
        "version": "test",
        "contact": dict(package_release.EXPECTED_CONTACT),
        "license": "MIT",
        "custom": {"modmenu": {"links": dict(package_release.EXPECTED_MODMENU_LINKS)}},
        "icon": "assets/smart_resource_drops/icon.png",
        "entrypoints": {
            "main": ["com.chedidandrew.smartresourcedrops.platform.fabric.FabricEntrypoint"],
            "client": [
                "com.chedidandrew.smartresourcedrops.platform.fabric.client.FabricClientEntrypoint"
            ],
            "modmenu": [
                "com.chedidandrew.smartresourcedrops.platform.fabric.client.FabricModMenuIntegration"
            ],
        },
        "mixins": ["smart_resource_drops.mixins.json"],
        "depends": {
            "fabricloader": ">=0.19.5",
            "minecraft": ">=1.21.9 <1.21.11",
            "java": ">=21",
            "fabric-api": ">=0.134.1+1.21.9",
        },
        "suggests": {"modmenu": ">=16.0.1"},
    }
    if metadata_overrides is not None:
        metadata.update(metadata_overrides)
    if omitted != "fabric.mod.json":
        archive.writestr("fabric.mod.json", json.dumps(metadata).encode())
    if omitted != "smart_resource_drops.mixins.json":
        mixin_config = {
            "package": "com.chedidandrew.smartresourcedrops.mixin",
            "mixins": (
                list(package_release.EXPECTED_PRODUCTION_MIXINS)
                if mixin_classes is None
                else mixin_classes
            ),
        }
        if mixin_config_overrides is not None:
            mixin_config.update(mixin_config_overrides)
        archive.writestr(
            "smart_resource_drops.mixins.json",
            json.dumps(mixin_config).encode(),
        )
    for required in package_release.REQUIRED_RELEASE_JAR_ENTRIES:
        if required == omitted or required in {"fabric.mod.json", "smart_resource_drops.mixins.json"}:
            continue
        if required == "LICENSE_smart-resource-multiplier":
            payload = (ROOT / "LICENSE").read_bytes() if license_payload is None else license_payload
        elif required == "assets/smart_resource_drops/icon.png":
            payload = (
                ROOT / "src/main/resources/assets/smart_resource_drops/icon.png"
            ).read_bytes() if icon_payload is None else icon_payload
        elif required == "data/smart_resource_drops/tags/item/protected_entity_loot.json":
            payload = json.dumps({
                "replace": False,
                "values": ["minecraft:saddle", "minecraft:totem_of_undying"],
            }).encode()
        elif required == "data/smart_resource_drops/tags/entity_type/shearing/standard_resources.json":
            payload = json.dumps({
                "replace": False,
                "values": ["minecraft:sheep"],
            }).encode()
        elif required == "data/smart_resource_drops/tags/entity_type/shearing/special.json":
            payload = json.dumps({
                "replace": False,
                "values": [
                    "minecraft:bogged",
                    "minecraft:copper_golem",
                    "minecraft:mooshroom",
                    "minecraft:snow_golem",
                ],
            }).encode()
        elif required.endswith(".class"):
            payload = b"\xca\xfe\xba\xbe\x00\x00" + class_major.to_bytes(2, "big")
        else:
            payload = b'{"replace":false,"values":[]}' if required.endswith(".json") else b"placeholder"
        archive.writestr(required, payload)

required_ignores = {
    ".gradle/",
    ".gradle-wrapper/",
    "build/",
    ".build/",
    "run/",
    "logs/",
    "dist/",
    "out/",
    ".idea/",
    ".vs/",
    "*.class",
    "*.log",
    "__pycache__/",
    "*.pyc",
}
gitignore_lines = {
    line.strip()
    for line in (ROOT / ".gitignore").read_text(encoding="utf-8").splitlines()
    if line.strip() and not line.lstrip().startswith("#")
}
missing_ignores = sorted(required_ignores - gitignore_lines)
require(not missing_ignores, f".gitignore is missing: {', '.join(missing_ignores)}")

for allowed in (
    "gradlew",
    "gradlew.bat",
    "gradle/wrapper/gradle-wrapper.jar",
    "gradle/wrapper/gradle-wrapper.properties",
):
    require(
        not package_release.is_forbidden_source_path(PurePosixPath(allowed)),
        f"Required wrapper file is excluded: {allowed}",
    )

for forbidden in (
    ".gradle/cache.bin",
    ".gradle-wrapper/gradle-9.5.1/bin/gradle",
    "build/classes/Example.class",
    ".build/core-tests/Example.class",
    "run/config/smart_resource_drops.json",
    "logs/latest.log",
    "dist/SmartResourceMultiplier-1.3.1+mc1.21.1-source.zip",
    "out/production/Example.class",
    ".idea/workspace.xml",
    ".vs/SmartResourceDrops/v17/.suo",
    "tools/__pycache__/package_release.cpython-313.pyc",
    "config/smart_resource_drops_stats.json",
    "config/other_mod.json",
    "credentials.json",
    ".env.local",
    "private.pem",
    "mods/private-test.jar",
    "saves/MyWorld/playerdata/player.dat",
    "screenshots/private.png",
    "world/level.dat",
    "server.properties",
    ".vscode/settings.json",
):
    require(
        package_release.is_forbidden_source_path(PurePosixPath(forbidden)),
        f"Generated/runtime path was not excluded: {forbidden}",
    )

source_files = package_release.source_files()
source_relatives = tuple(path.relative_to(ROOT).as_posix() for path in source_files)
require(
    source_relatives == tuple(sorted(package_release.git_file_names("--cached"))),
    "Source package must contain exactly the Git-tracked manifest",
)

for missing_source in (
    ".github/FUNDING.yml",
    ".github/ISSUE_TEMPLATE/bug_report.yml",
    ".github/ISSUE_TEMPLATE/config.yml",
    ".github/ISSUE_TEMPLATE/mod_compatibility.yml",
    ".github/PULL_REQUEST_TEMPLATE.md",
    "CONTRIBUTING.md",
    "BUILD_STATUS.md",
    "CHANGELOG.md",
    "README.md",
    "SECURITY.md",
    "docs/ROADMAP.md",
    "docs/COMPATIBILITY.md",
    "docs/COMMANDS.md",
    "docs/CONFIGURATION.md",
    "docs/IMPLEMENTATION_LOG.md",
    "docs/PERFORMANCE.md",
    "docs/PUBLIC_RELEASE_CHECKLIST.md",
    "docs/releases/1.3.0.md",
    "docs/releases/1.3.1+mc1.21.1.md",
    "docs/TESTING.md",
    "docs/images/general-config.webp",
    "docs/images/block-overrides.webp",
    "docs/images/shearing-config.webp",
    "gradle/wrapper/gradle-wrapper.jar",
    "src/main/java/com/chedidandrew/smartresourcedrops/config/ConfigValidator.java",
    "src/main/java/com/chedidandrew/smartresourcedrops/config/ConfigValidationReport.java",
    "src/main/java/com/chedidandrew/smartresourcedrops/core/BlockLootBudgetWarnings.java",
    "src/main/java/com/chedidandrew/smartresourcedrops/core/util/BlockLootOutputBudget.java",
    "src/main/java/com/chedidandrew/smartresourcedrops/core/util/AtomicConfigWriter.java",
    "src/main/java/com/chedidandrew/smartresourcedrops/core/util/LootOutputBudget.java",
    "src/test/java/com/chedidandrew/smartresourcedrops/config/ConfigValidatorTest.java",
    "src/test/java/com/chedidandrew/smartresourcedrops/core/util/BlockLootOutputBudgetTest.java",
    "src/test/java/com/chedidandrew/smartresourcedrops/core/util/AtomicConfigWriterTest.java",
    "src/gametest/java/com/chedidandrew/smartresourcedrops/gametest/SmartResourceDropsBlockBudgetGameTests.java",
    "src/gametest/java/com/chedidandrew/smartresourcedrops/gametest/fixture/GameTestBlockLootFixtures.java",
    "src/gametest/java/com/chedidandrew/smartresourcedrops/gametest/fixture/FabricGameTestEntityFixtures.java",
    "neoforge/src/gametest/java/com/chedidandrew/smartresourcedrops/gametest/fixture/NeoForgeGameTestEntityFixtures.java",
    "neoforge/src/gametest/java/com/chedidandrew/smartresourcedrops/gametest/NeoForgeMixinAuditGameTests.java",
    "neoforge/src/gametest/resources/data/smart_resource_drops_gametest/loot_modifiers/entity_final_loot.json",
    "neoforge/src/clienttest/java/com/chedidandrew/smartresourcedrops/client/NeoForgeMultiplayerClientSmokeTest.java",
    "neoforge/src/clienttest/java/com/chedidandrew/smartresourcedrops/client/NeoForgeOptionalClientOnlySmokeTest.java",
    "neoforge/src/clienttest/java/com/chedidandrew/smartresourcedrops/client/NeoForgeOversizedWireClientSmokeTest.java",
    "neoforge/src/clienttest/java/com/chedidandrew/smartresourcedrops/platform/neoforge/NeoForgeMultiplayerServerSmokeTest.java",
    "neoforge/src/clienttest/java/com/chedidandrew/smartresourcedrops/platform/neoforge/NeoForgePlacementPersistenceSmokeTest.java",
    "neoforge/src/clienttest/java/com/chedidandrew/smartresourcedrops/platform/neoforge/NeoForgeOversizedWireServerSmokeTest.java",
    "neoforge/src/optionalchanneltest/java/com/chedidandrew/smartresourcedrops/optionaltest/NeoForgeOptionalChannelServerProbe.java",
    "neoforge/src/optionalchanneltest/java/com/chedidandrew/smartresourcedrops/optionaltest/NeoForgeOptionalServerOnlyClientSmokeTest.java",
    "neoforge/src/optionalchanneltest/java/com/chedidandrew/smartresourcedrops/optionaltest/OptionalChannelIds.java",
    "neoforge/src/optionalchanneltest/resources/META-INF/neoforge.mods.toml",
    "neoforge/src/packagedprobetest/java/com/chedidandrew/smartresourcedrops/packagedprobe/PackagedClientProbe.java",
    "neoforge/src/packagedprobetest/java/com/chedidandrew/smartresourcedrops/packagedprobe/PackagedProbeSupport.java",
    "neoforge/src/packagedprobetest/java/com/chedidandrew/smartresourcedrops/packagedprobe/PackagedServerProbe.java",
    "neoforge/src/packagedprobetest/resources/META-INF/neoforge.mods.toml",
    "src/clienttest/java/com/chedidandrew/smartresourcedrops/client/FabricClientSmokeTest.java",
    "src/clienttest/java/com/chedidandrew/smartresourcedrops/client/FabricMultiplayerClientSmokeTest.java",
    "src/clienttest/java/com/chedidandrew/smartresourcedrops/platform/fabric/FabricMultiplayerServerSmokeTest.java",
    "src/clienttest/java/com/chedidandrew/smartresourcedrops/platform/fabric/FabricPlacementPersistenceSmokeTest.java",
    "src/gametest/java/com/chedidandrew/smartresourcedrops/gametest/FabricAutomationAuthorityGameTests.java",
    "src/gametest/java/com/chedidandrew/smartresourcedrops/gametest/FabricMixinAuditGameTests.java",
    "src/gametest/java/com/chedidandrew/smartresourcedrops/gametest/GameTestAssertions.java",
    "neoforge/src/gametest/java/com/chedidandrew/smartresourcedrops/gametest/NeoForgeAutomationAuthorityGameTests.java",
    "neoforge/src/gametest/java/net/fabricmc/fabric/api/gametest/v1/GameTest.java",
    "neoforge/src/main/java/com/chedidandrew/smartresourcedrops/platform/neoforge/LegacyFabricProvenanceMigration.java",
    "neoforge/src/main/java/com/chedidandrew/smartresourcedrops/platform/neoforge/mixin/SerializableChunkDataLegacyProvenanceMixin.java",
    "neoforge/src/test/java/com/chedidandrew/smartresourcedrops/platform/neoforge/LegacyFabricProvenanceMigrationTest.java",
    "neoforge/src/test/resources/fixtures/fabric-placement-provenance-chunk--554625--233041.nbt.b64",
    "neoforge/src/clienttest/java/com/chedidandrew/smartresourcedrops/platform/neoforge/NeoForgeMigrationRestartSmokeTest.java",
    "neoforge/src/gametest/resources/data/smart_resource_drops_gametest/structure/wide.nbt",
    "tools/run_fabric_multiplayer_smoke.sh",
    "tools/run_neoforge_multiplayer_smoke.sh",
    "tools/run_neoforge_optional_channel_smoke.sh",
    "tools/run_neoforge_oversized_wire_smoke.sh",
    "tools/prepare_fabric_ci_artifact.py",
    "tools/validate_neoforge_jar.py",
    "src/main/java/com/chedidandrew/smartresourcedrops/core/entity/EntityClassifier.java",
    "src/main/java/com/chedidandrew/smartresourcedrops/core/entity/EntityDropTags.java",
    "src/main/java/com/chedidandrew/smartresourcedrops/core/entity/EntityLootTags.java",
    "src/main/resources/data/smart_resource_drops/tags/block/categories/ores.json",
    "src/main/resources/data/smart_resource_drops/tags/item/protected_entity_loot.json",
):
    incomplete_entries = tuple(
        relative for relative in source_relatives if relative != missing_source
    )
    try:
        package_release.validate_source_entries(incomplete_entries)
    except package_release.ReleasePackageError as exc:
        require(
            "missing required source files" in str(exc) and missing_source in str(exc),
            f"Missing required source failure was not explicit for {missing_source}",
        )
    else:
        raise AssertionError(f"Source manifest accepted missing required file {missing_source}")

jar_build_relatives = tuple(
    path.relative_to(ROOT).as_posix() for path in package_release.jar_build_inputs()
)
require("BUILD_STATUS.md" not in jar_build_relatives, "Release notes must not make the JAR stale")
for required_build_input in (
    "build.gradle",
    "gradle.properties",
    "src/main/resources/fabric.mod.json",
    "src/client/java/com/chedidandrew/smartresourcedrops/client/SmartDropsConfigScreen.java",
    "src/test/java/com/chedidandrew/smartresourcedrops/config/SmartDropsConfigTest.java",
    "src/gametest/java/com/chedidandrew/smartresourcedrops/gametest/SmartResourceDropsGameTests.java",
    "src/gametest/java/com/chedidandrew/smartresourcedrops/gametest/GameTestPlayers.java",
    "src/gametest/java/com/chedidandrew/smartresourcedrops/gametest/fixture/GameTestEntityFixtures.java",
    "src/gametest/java/com/chedidandrew/smartresourcedrops/gametest/fixture/GameTestBlockLootFixtures.java",
):
    require(
        required_build_input in jar_build_relatives,
        f"JAR freshness inputs omitted {required_build_input}",
    )

neoforge_jar_build_relatives = tuple(
    path.relative_to(ROOT).as_posix() for path in package_release.neoforge_jar_build_inputs()
)
for required_build_input in (
    "gradle.properties",
    "src/main/resources/assets/smart_resource_drops/lang/en_us.json",
    "neoforge/build.gradle",
    "neoforge/gradle.properties",
    "neoforge/settings.gradle",
    "neoforge/src/main/templates/META-INF/neoforge.mods.toml",
    "neoforge/src/main/java/com/chedidandrew/smartresourcedrops/platform/neoforge/NeoForgeEntrypoint.java",
):
    require(
        required_build_input in neoforge_jar_build_relatives,
        f"NeoForge JAR freshness inputs omitted {required_build_input}",
    )

root_properties = package_release.parse_properties(ROOT / "gradle.properties")
neoforge_properties = package_release.parse_properties(ROOT / "neoforge/gradle.properties")
require(
    root_properties["mod_version"] == "1.3.2+mc1.21.9-1.21.10",
    "Fabric release version drifted from the audited 1.21.9-1.21.10 lane",
)
require(
    neoforge_properties["mod_version"] == "1.3.2+mc1.21.9",
    "NeoForge release version drifted from the audited exact 1.21.9 lane",
)
require(
    root_properties["mod_version"].split("+", 1)[0]
    == neoforge_properties["mod_version"].split("+", 1)[0],
    "Fabric and NeoForge public release versions drifted",
)

with tempfile.TemporaryDirectory(prefix="smart-resource-multiplier-source-test-") as temp_dir:
    temp_root = Path(temp_dir)
    first = temp_root / "first.zip"
    second = temp_root / "second.zip"
    top_level = "SmartResourceMultiplier-test"

    first_entries = package_release.build_source_zip(first, top_level, source_files)
    second_entries = package_release.build_source_zip(second, top_level, source_files)
    require(first_entries == source_relatives, "Source ZIP entries differ from the validated manifest")
    require(first.read_bytes() == second.read_bytes(), "Source ZIP output is not deterministic")

    with zipfile.ZipFile(first) as archive:
        archived = tuple(info.filename for info in archive.infolist())
    require(
        all(name.startswith(f"{top_level}/") for name in archived),
        "Source ZIP contains an entry outside its top-level directory",
    )
    require(
        not any(
            package_release.is_forbidden_source_path(
                PurePosixPath(name.removeprefix(f"{top_level}/"))
            )
            for name in archived
        ),
        "Source ZIP contains generated/runtime data",
    )

    forbidden_zip = temp_root / "forbidden.zip"
    with zipfile.ZipFile(forbidden_zip, "w") as archive:
        for required in sorted(package_release.REQUIRED_SOURCE_FILES):
            archive.writestr(f"{top_level}/{required}", b"placeholder")
        archive.writestr(f"{top_level}/build/Leak.class", b"forbidden")
    try:
        package_release.validate_source_zip(forbidden_zip, top_level)
    except package_release.ReleasePackageError as exc:
        require("forbidden" in str(exc), "Forbidden-entry failure was not explicit")
    else:
        raise AssertionError("Source ZIP validator accepted a generated class file")

    incomplete_zip = temp_root / "incomplete.zip"
    with zipfile.ZipFile(incomplete_zip, "w") as archive:
        archive.writestr(f"{top_level}/gradlew", b"placeholder")
    try:
        package_release.validate_source_zip(incomplete_zip, top_level)
    except package_release.ReleasePackageError as exc:
        require("wrapper" in str(exc).lower(), "Missing-wrapper failure was not explicit")
    else:
        raise AssertionError("Source ZIP validator accepted missing wrapper files")

    clean_jar = temp_root / "clean.jar"
    with zipfile.ZipFile(clean_jar, "w") as archive:
        write_minimum_release_entries(archive)
    clean_entries = package_release.validate_release_jar(clean_jar, "test")
    require("fabric.mod.json" in clean_entries, "Clean playable JAR was not validated")

    for field, replacement, expected_error in (
        ("name", "Smart Resource" + " Drops", "display name"),
        ("id", "resource_multiplier", "mod id"),
        ("contact", {"homepage": "https://example.invalid"}, "contact"),
        ("license", "All-Rights-Reserved", "license"),
        ("icon", "assets/resource_multiplier/icon.png", "icon"),
        ("entrypoints", {}, "entrypoint"),
        ("mixins", [], "mixin"),
        ("depends", {}, "dependencies"),
        ("suggests", {}, "mod menu"),
    ):
        invalid_metadata_jar = temp_root / f"invalid_{field}.jar"
        with zipfile.ZipFile(invalid_metadata_jar, "w") as archive:
            write_minimum_release_entries(
                archive,
                metadata_overrides={field: replacement},
            )
        try:
            package_release.validate_release_jar(invalid_metadata_jar, "test")
        except package_release.ReleasePackageError as exc:
            require(
                expected_error in str(exc).lower(),
                f"Invalid release metadata failure was not explicit for {field}",
            )
        else:
            raise AssertionError(f"Playable JAR accepted invalid metadata field {field}")

    for label, overrides in (
        ("empty", {"mixins": []}),
        ("wrong_type", {"mixins": "SheepShearingLootMixin"}),
        (
            "missing_core_hook",
            {"mixins": package_release.EXPECTED_PRODUCTION_MIXINS[1:]},
        ),
    ):
        invalid_mixin_config_jar = temp_root / f"invalid_mixin_config_{label}.jar"
        with zipfile.ZipFile(invalid_mixin_config_jar, "w") as archive:
            write_minimum_release_entries(archive, mixin_config_overrides=overrides)
        try:
            package_release.validate_release_jar(invalid_mixin_config_jar, "test")
        except package_release.ReleasePackageError as exc:
            require("mixin" in str(exc).lower(), f"Invalid mixin config failure was not explicit for {label}")
        else:
            raise AssertionError(f"Playable JAR accepted {label} production mixin configuration")

    wrong_class_major_jar = temp_root / "wrong_class_major.jar"
    with zipfile.ZipFile(wrong_class_major_jar, "w") as archive:
        write_minimum_release_entries(archive, class_major=61)
    try:
        package_release.validate_release_jar(wrong_class_major_jar, "test")
    except package_release.ReleasePackageError as exc:
        require(
            "class-file major" in str(exc).lower() and "java 21" in str(exc).lower(),
            "Wrong Fabric class-file-major failure was not explicit",
        )
    else:
        raise AssertionError("Playable JAR accepted non-Java-21 class files")

    wrong_license_jar = temp_root / "wrong_embedded_license.jar"
    with zipfile.ZipFile(wrong_license_jar, "w") as archive:
        write_minimum_release_entries(archive, license_payload=b"All Rights Reserved\n")
    try:
        package_release.validate_release_jar(wrong_license_jar, "test")
    except package_release.ReleasePackageError as exc:
        require("license" in str(exc).lower(), "Embedded-license mismatch failure was not explicit")
    else:
        raise AssertionError("Playable JAR accepted a mismatched embedded license")

    wrong_icon_jar = temp_root / "wrong_embedded_icon.jar"
    with zipfile.ZipFile(wrong_icon_jar, "w") as archive:
        write_minimum_release_entries(archive, icon_payload=b"not-the-approved-icon")
    try:
        package_release.validate_release_jar(wrong_icon_jar, "test")
    except package_release.ReleasePackageError as exc:
        require("icon" in str(exc).lower(), "Embedded-icon mismatch failure was not explicit")
    else:
        raise AssertionError("Playable JAR accepted a mismatched embedded icon")

    forbidden_jar_entries = (
        "com/chedidandrew/smartresourcedrops/gametest/EntityFixture.class",
        "data/smart_resource_drops_gametest/loot_table/entities/fixture.json",
        "net/fabricmc/fabric/api/FabricApi.class",
        "org/junit/jupiter/api/Test.class",
        "META-INF/jars/shaded-helper.jar",
        "src/main/java/LeakedSource.java",
        "build.gradle",
        "run/config/smart_resource_drops.json",
        "com/chedidandrew/smartresourcedrops/client/FabricClientSmokeTest.class",
        "com/chedidandrew/smartresourcedrops/client/FabricClientSmokeTest$Phase.class",
        "com/chedidandrew/smartresourcedrops/client/FabricMultiplayerClientSmokeTest.class",
        "com/chedidandrew/smartresourcedrops/client/FabricMultiplayerClientSmokeTest$Phase.class",
        "com/chedidandrew/smartresourcedrops/platform/fabric/FabricMultiplayerServerSmokeTest.class",
        "com/chedidandrew/smartresourcedrops/platform/fabric/FabricMultiplayerServerSmokeTest$State.class",
        "com/chedidandrew/smartresourcedrops/platform/fabric/FabricPlacementPersistenceSmokeTest.class",
        "com/chedidandrew/smartresourcedrops/platform/fabric/FabricPlacementPersistenceSmokeTest$Phase.class",
        "com/chedidandrew/smartresourcedrops/client/NeoForgeClientCategorySmokeTest.class",
        "com/chedidandrew/smartresourcedrops/client/NeoForgeMultiplayerClientSmokeTest.class",
        "com/chedidandrew/smartresourcedrops/client/NeoForgeMultiplayerClientSmokeTest$Phase.class",
        "com/chedidandrew/smartresourcedrops/client/NeoForgeOptionalClientOnlySmokeTest.class",
        "com/chedidandrew/smartresourcedrops/client/NeoForgeOversizedWireClientSmokeTest.class",
        "com/chedidandrew/smartresourcedrops/optionaltest/NeoForgeOptionalChannelServerProbe.class",
        "com/chedidandrew/smartresourcedrops/optionaltest/NeoForgeOptionalServerOnlyClientSmokeTest.class",
        "com/chedidandrew/smartresourcedrops/optionaltest/OptionalChannelIds.class",
        "com/chedidandrew/smartresourcedrops/platform/neoforge/NeoForgeMultiplayerServerSmokeTest.class",
        "com/chedidandrew/smartresourcedrops/platform/neoforge/NeoForgePlacementPersistenceSmokeTest.class",
        "com/chedidandrew/smartresourcedrops/platform/neoforge/NeoForgeOversizedWireServerSmokeTest.class",
        "com/chedidandrew/smartresourcedrops/packagedprobe/PackagedClientProbe.class",
        "com/chedidandrew/smartresourcedrops/packagedprobe/PackagedProbeSupport.class",
        "com/chedidandrew/smartresourcedrops/packagedprobe/PackagedServerProbe.class",
    )
    for forbidden_entry in forbidden_jar_entries:
        forbidden_jar = temp_root / (forbidden_entry.replace("/", "_") + ".jar")
        with zipfile.ZipFile(forbidden_jar, "w") as archive:
            write_minimum_release_entries(archive)
            archive.writestr(forbidden_entry, b"leak")
        try:
            package_release.validate_release_jar(forbidden_jar, "test")
        except package_release.ReleasePackageError as exc:
            require(
                "fixture" in str(exc).lower()
                or "bundled" in str(exc).lower()
                or "runtime" in str(exc).lower()
                or "nested" in str(exc).lower()
                or "source-package" in str(exc).lower()
                or "smoke-test" in str(exc).lower(),
                f"Playable-JAR rejection was not explicit for {forbidden_entry}",
            )
        else:
            raise AssertionError(f"Playable JAR accepted forbidden entry {forbidden_entry}")

    for missing_entry in (
        "LICENSE_smart-resource-multiplier",
        "com/chedidandrew/smartresourcedrops/platform/fabric/client/FabricClientEntrypoint.class",
        "smart_resource_drops.mixins.json",
        "assets/smart_resource_drops/icon.png",
        "assets/smart_resource_drops/lang/en_us.json",
        "com/chedidandrew/smartresourcedrops/core/entity/EntityClassifier.class",
        "com/chedidandrew/smartresourcedrops/core/entity/EntityDropTags.class",
        "com/chedidandrew/smartresourcedrops/core/entity/EntityLootTags.class",
        "com/chedidandrew/smartresourcedrops/config/ConfigValidator.class",
        "com/chedidandrew/smartresourcedrops/config/ConfigValidationReport.class",
        "com/chedidandrew/smartresourcedrops/core/BlockLootBudgetWarnings.class",
        "com/chedidandrew/smartresourcedrops/core/util/BlockLootOutputBudget.class",
        "com/chedidandrew/smartresourcedrops/core/util/LootOutputBudget.class",
        "data/smart_resource_drops/tags/block/categories/ores.json",
        "data/smart_resource_drops/tags/entity_type/categories/passive.json",
        "data/smart_resource_drops/tags/item/protected_entity_loot.json",
        "data/smart_resource_drops/tags/entity_type/shearing/standard_resources.json",
        "data/smart_resource_drops/tags/entity_type/shearing/special.json",
        "com/chedidandrew/smartresourcedrops/core/shearing/ShearingRuleResolver.class",
        "com/chedidandrew/smartresourcedrops/mixin/LivingEntityShearingLootMixin.class",
    ):
        incomplete_jar = temp_root / ("missing_" + missing_entry.replace("/", "_") + ".jar")
        with zipfile.ZipFile(incomplete_jar, "w") as archive:
            write_minimum_release_entries(archive, omitted=missing_entry)
        try:
            package_release.validate_release_jar(incomplete_jar, "test")
        except package_release.ReleasePackageError as exc:
            require("missing" in str(exc).lower() or "invalid" in str(exc).lower(),
                    f"Missing required JAR asset was not explicit for {missing_entry}")
        else:
            raise AssertionError(f"Playable JAR accepted missing required asset {missing_entry}")

    missing_mixin_jar = temp_root / "missing_declared_mixin.jar"
    with zipfile.ZipFile(missing_mixin_jar, "w") as archive:
        write_minimum_release_entries(archive, mixin_classes=["MissingFixtureMixin"])
    try:
        package_release.validate_release_jar(missing_mixin_jar, "test")
    except package_release.ReleasePackageError as exc:
        require("missing declared mixin class" in str(exc).lower(),
                "Missing declared mixin class failure was not explicit")
    else:
        raise AssertionError("Playable JAR accepted a missing declared mixin class")

    empty_output = temp_root / "empty-output"
    empty_output.mkdir()
    package_release.require_empty_output_directory(empty_output)
    (empty_output / "stale.jar").write_bytes(b"legacy")
    try:
        package_release.require_empty_output_directory(empty_output)
    except package_release.ReleasePackageError as exc:
        require("stale" in str(exc).lower(), "Stale-output failure was not explicit")
    else:
        raise AssertionError("Release packager accepted a non-empty output directory")

for built_jar in sorted((ROOT / "build/libs").glob("smart-resource-multiplier-*.jar")):
    if not built_jar.name.endswith("-sources.jar"):
        package_release.validate_release_jar(built_jar)

print(
    "PASS: Git-tracked deterministic source ZIP excludes secrets/generated/runtime data, playable JARs reject "
    "test fixtures/nested or shaded dependencies/source/runtime leaks, require publication "
    "templates/scope-security docs and declared entrypoint/mixin/protected-tag/key assets, "
    "and preserve all required wrapper files "
    f"({len(source_relatives)} source entries)"
)
