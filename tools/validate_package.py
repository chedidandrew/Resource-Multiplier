#!/usr/bin/env python3
"""Static release-policy validation for the Minecraft 1.20.1 backport."""

from __future__ import annotations

import hashlib
import json
import pathlib
import struct
import sys
from collections.abc import Iterable

ROOT = pathlib.Path(__file__).resolve().parents[1]
ERRORS: list[str] = []


def fail(message: str) -> None:
    ERRORS.append(message)


def read(path: str) -> str:
    candidate = ROOT / path
    if not candidate.is_file():
        fail(f"Missing required file: {path}")
        return ""
    try:
        return candidate.read_text(encoding="utf-8")
    except (OSError, UnicodeError) as exc:
        fail(f"Cannot read {path}: {exc}")
        return ""


def require(text: str, needles: Iterable[str], label: str) -> None:
    for needle in needles:
        if needle not in text:
            fail(f"{label} is missing required text: {needle!r}")


def forbid(text: str, needles: Iterable[str], label: str) -> None:
    for needle in needles:
        if needle in text:
            fail(f"{label} contains forbidden stale text: {needle!r}")


def gradle_named_block(text: str, name: str) -> str:
    """Return a named Gradle closure, including nested braces."""
    marker = f"        {name} {{"
    start = text.find(marker)
    if start < 0:
        fail(f"NeoForge build is missing run block: {name}")
        return ""
    brace = text.find("{", start)
    depth = 0
    for index in range(brace, len(text)):
        if text[index] == "{":
            depth += 1
        elif text[index] == "}":
            depth -= 1
            if depth == 0:
                return text[start:index + 1]
    fail(f"NeoForge build has an unterminated run block: {name}")
    return text[start:]


def parse_properties(path: str) -> dict[str, str]:
    result: dict[str, str] = {}
    for raw in read(path).splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        result[key.strip()] = value.strip()
    return result


def validate_exact(mapping: dict[str, str], expected: dict[str, str], label: str) -> None:
    for key, value in expected.items():
        if mapping.get(key) != value:
            fail(f"{label} {key} must be {value!r}, found {mapping.get(key)!r}")


REQUIRED_FILES = [
    "LICENSE",
    "README.md",
    "BUILD_STATUS.md",
    "CHANGELOG.md",
    "gradle.properties",
    "build.gradle",
    "gradlew",
    "gradlew.bat",
    "src/main/resources/fabric.mod.json",
    "src/main/resources/pack.mcmeta",
    "src/main/resources/smart_resource_drops.mixins.json",
    "src/main/resources/assets/smart_resource_drops/icon.png",
    "src/main/java/com/chedidandrew/smartresourcedrops/network/ConfigTransferCodec.java",
    "src/main/java/com/chedidandrew/smartresourcedrops/network/ConfigTransferAssembler.java",
    "src/main/java/com/chedidandrew/smartresourcedrops/network/ConfigPatchFragmentPayload.java",
    "src/main/java/com/chedidandrew/smartresourcedrops/network/ConfigSnapshotFragmentPayload.java",
    "src/clienttest/java/com/chedidandrew/smartresourcedrops/client/FabricClientSmokeTest.java",
    "src/clienttest/java/com/chedidandrew/smartresourcedrops/client/FabricMultiplayerClientSmokeTest.java",
    "src/clienttest/java/com/chedidandrew/smartresourcedrops/platform/fabric/FabricMultiplayerServerSmokeTest.java",
    "src/clienttest/java/com/chedidandrew/smartresourcedrops/platform/fabric/FabricPlacementPersistenceSmokeTest.java",
    "neoforge/build.gradle",
    "neoforge/gradle.properties",
    "neoforge/src/main/templates/META-INF/mods.toml",
    "neoforge/src/main/resources/smart_resource_drops.neoforge.mixins.json",
    "neoforge/src/gametest/resources/data/smart_resource_drops_gametest/structures/wide.nbt",
    "neoforge/src/optionalchanneltest/resources/META-INF/mods.toml",
    "neoforge/src/optionalchanneltest/resources/pack.mcmeta",
    "neoforge/src/clienttest/java/com/chedidandrew/smartresourcedrops/platform/neoforge/NeoForgePlacementPersistenceSmokeTest.java",
    "tools/validate_neoforge_jar.py",
    "tools/validate_fabric_jar.py",
    "tools/run_core_tests.sh",
    "tools/run_core_tests.ps1",
    "tools/run_fabric_multiplayer_smoke.sh",
    "tools/run_neoforge_multiplayer_smoke.sh",
    "tools/run_neoforge_optional_channel_smoke.sh",
    "tools/run_neoforge_oversized_wire_smoke.sh",
    "tools/run_neoforge_production_server_smoke.sh",
    ".github/workflows/build.yml",
    ".github/workflows/release.yml",
    "docs/releases/1.3.0+mc1.20.1.md",
]
for required in REQUIRED_FILES:
    if not (ROOT / required).is_file():
        fail(f"Missing required file: {required}")

try:
    pack_metadata = json.loads(read("src/main/resources/pack.mcmeta"))
    if pack_metadata != {
        "pack": {
            "pack_format": 15,
            "description": "Smart Resource Multiplier resources for Minecraft 1.20.1",
        }
    }:
        fail(f"pack.mcmeta must be the exact Minecraft 1.20.1 pack contract, found {pack_metadata!r}")
except json.JSONDecodeError as exc:
    fail(f"Invalid JSON in src/main/resources/pack.mcmeta: {exc}")

try:
    optional_channel_pack = json.loads(read("neoforge/src/optionalchanneltest/resources/pack.mcmeta"))
    if optional_channel_pack != {
        "pack": {
            "pack_format": 15,
            "description": "Smart Resource Multiplier optional-channel test resources",
        }
    }:
        fail(f"optional-channel pack.mcmeta must be the exact test-resource contract, found {optional_channel_pack!r}")
except json.JSONDecodeError as exc:
    fail(f"Invalid JSON in neoforge/src/optionalchanneltest/resources/pack.mcmeta: {exc}")

root_props = parse_properties("gradle.properties")
validate_exact(
    root_props,
    {
        "minecraft_version": "1.20.1",
        "java_version": "17",
        "loader_version": "0.19.5",
        "loom_version": "1.17.20",
        "fabric_version": "0.92.12+1.20.1",
        "modmenu_version": "7.2.2",
        "mod_version": "1.3.0+mc1.20.1",
        "archives_base_name": "smart-resource-multiplier",
    },
    "gradle.properties",
)
if root_props.get("release_ready") not in {"false", "true"}:
    fail("gradle.properties release_ready must be exactly false or true")

neo_props = parse_properties("neoforge/gradle.properties")
validate_exact(
    neo_props,
    {
        "minecraft_version": "1.20.1",
        "minecraft_version_range": "[1.20.1]",
        "java_version": "17",
        "neo_version": "1.20.1-47.1.106",
        "moddev_version": "2.0.146",
        "mod_version": "1.3.0+mc1.20.1",
        "archives_base_name": "smart-resource-multiplier-neoforge",
    },
    "neoforge/gradle.properties",
)
if neo_props.get("mod_version") != root_props.get("mod_version"):
    fail("Fabric and NeoForge mod_version values differ")

fabric_metadata = read("src/main/resources/fabric.mod.json")
require(
    fabric_metadata,
    [
        '"id": "smart_resource_drops"',
        '"name": "Smart Resource Multiplier"',
        '"minecraft": "${minecraft_version}"',
        '"java": ">=${java_version}"',
        '"fabricloader": ">=${loader_version}"',
        '"fabric-api": ">=${fabric_version}"',
        '"modmenu": ">=${modmenu_version}"',
    ],
    "Fabric metadata",
)
forbid(fabric_metadata, ["1.21.11", "26.2", '">=21"'], "Fabric metadata")

neo_metadata = read("neoforge/src/main/templates/META-INF/mods.toml")
require(
    neo_metadata,
    [
        'modLoader="javafml"',
        'loaderVersion="[47,)"',
        'modId="${mod_id}"',
        'displayTest="NONE"',
        'logoFile="assets/smart_resource_drops/icon.png"',
        'modId="forge"',
        'versionRange="[47.1.106,)"',
        'modId="minecraft"',
        'versionRange="[1.20.1]"',
    ],
    "NeoForge legacy metadata",
)
forbid(neo_metadata, ["iconFile", "neoforge.mods.toml", 'modId="neoforge"', "21.11"], "NeoForge legacy metadata")

neo_build = read("neoforge/build.gradle")
require(
    neo_build,
    [
        "net.neoforged.moddev.legacyforge",
        "neoForgeVersion = neo_version",
        "type = 'gameTestServer'",
        "forge.enabledGameTestNamespaces",
        "smart_resource_drops.refmap.json",
        "MixinConfigs",
        "mixinextras-common:0.5.4",
        "mixinextras-forge:0.5.4",
        "jarJar(implementation",
        "gametestRuntimeOnly 'io.github.llamalad7:mixinextras-forge:0.5.4'",
    ],
    "NeoForge build",
)
forbid(neo_build, ["runMigrationRestart", "--tests smart_resource_drops_gametest", "version = neo_version"], "NeoForge build")

require(
    neo_build,
    [
        "def addProductionMixinArguments = { run ->",
        "run.programArgument '--mixin.config'",
        "run.programArgument 'smart_resource_drops.mixins.json'",
        "run.programArgument 'smart_resource_drops.neoforge.mixins.json'",
    ],
    "NeoForge per-run mixin configuration",
)
if neo_build.count("def addProductionMixinArguments = { run ->") != 1:
    fail("NeoForge build must define exactly one production-mixin argument helper")
if neo_build.count("addProductionMixinArguments(delegate)") != 13:
    fail("NeoForge build must attach production mixins to exactly 13 production-mod runs")
for run_name in (
    "client",
    "server",
    "gameTestServer",
    "clientCategoryTest",
    "multiplayerServerTest",
    "multiplayerClientTest",
    "optionalClientOnlyClientTest",
    "optionalServerOnlyServerTest",
    "oversizedWireServerTest",
    "oversizedWireClientTest",
    "persistenceMarkServerTest",
    "persistenceRemoveServerTest",
    "persistenceVerifyAbsentServerTest",
):
    if "addProductionMixinArguments(delegate)" not in gradle_named_block(neo_build, run_name):
        fail(f"NeoForge production-mod run must receive production mixins: {run_name}")
for run_name in ("optionalClientOnlyServerTest", "optionalServerOnlyClientTest"):
    if "addProductionMixinArguments(delegate)" in gradle_named_block(neo_build, run_name):
        fail(f"NeoForge probe-only run must not receive production mixins: {run_name}")
forbid(
    neo_build,
    [
        "config 'smart_resource_drops.mixins.json'",
        "config 'smart_resource_drops.neoforge.mixins.json'",
    ],
    "NeoForge global mixin configuration",
)

for path in (
    "src/main/resources/smart_resource_drops.mixins.json",
    "neoforge/src/main/resources/smart_resource_drops.neoforge.mixins.json",
):
    try:
        mixin = json.loads(read(path))
    except json.JSONDecodeError as exc:
        fail(f"Invalid JSON in {path}: {exc}")
        continue
    if mixin.get("compatibilityLevel") != "JAVA_17":
        fail(f"{path} must use JAVA_17")
    if mixin.get("minVersion") != "0.8.5":
        fail(f"{path} must declare minVersion 0.8.5")

required_tags = [
    *(f"src/main/resources/data/smart_resource_drops/tags/blocks/categories/{name}.json" for name in (
        "building_blocks", "crops", "end", "leaves", "logs", "nether", "ores",
        "plants", "raw_resource_blocks", "soil", "stone",
    )),
    *(f"src/main/resources/data/smart_resource_drops/tags/entity_types/categories/{name}.json" for name in (
        "ambient", "aquatic", "bosses", "golems", "hostile", "miscellaneous",
        "neutral", "passive", "villagers_npcs",
    )),
    "src/main/resources/data/smart_resource_drops/tags/entity_types/shearing/standard_resources.json",
    "src/main/resources/data/smart_resource_drops/tags/entity_types/shearing/special.json",
    "src/main/resources/data/smart_resource_drops/tags/items/protected_entity_loot.json",
]
for path in required_tags:
    if not (ROOT / path).is_file():
        fail(f"Missing Minecraft 1.20.1 plural tag resource: {path}")
for stale_dir in (
    "src/main/resources/data/smart_resource_drops/tags/block",
    "src/main/resources/data/smart_resource_drops/tags/entity_type",
    "src/main/resources/data/smart_resource_drops/tags/item",
):
    if (ROOT / stale_dir).exists():
        fail(f"Stale singular tag directory remains: {stale_dir}")

special_path = ROOT / "src/main/resources/data/smart_resource_drops/tags/entity_types/shearing/special.json"
if special_path.is_file():
    try:
        special_values = json.loads(special_path.read_text(encoding="utf-8")).get("values")
        if not isinstance(special_values, list):
            fail("special shearing tag values must be a JSON list")
        elif set(special_values) != {"minecraft:mooshroom", "minecraft:snow_golem"}:
            fail(f"1.20.1 special shearing tag must contain only mooshroom and snow_golem, found {special_values!r}")
    except (OSError, json.JSONDecodeError) as exc:
        fail(f"Cannot validate special shearing tag: {exc}")

production_text = "\n".join(
    path.read_text(encoding="utf-8", errors="replace")
    for base in (
        ROOT / "src/main/java",
        ROOT / "src/main/resources",
        ROOT / "neoforge/src/main/java",
        ROOT / "neoforge/src/main/resources",
        ROOT / "config-examples",
    )
    if base.exists()
    for path in base.rglob("*")
    if path.is_file() and path.suffix in {".java", ".json", ".toml"}
)
for stale_id in ("minecraft:trial_spawner", "minecraft:vault", "minecraft:bogged", "minecraft:breeze", "minecraft:armadillo", "minecraft:copper_golem"):
    if stale_id in production_text:
        fail(f"Post-1.20.1 identifier remains in production sources/resources: {stale_id}")
neo_production_text = "\n".join(
    path.read_text(encoding="utf-8", errors="replace")
    for path in (ROOT / "neoforge/src/main").rglob("*")
    if path.is_file() and path.suffix in {".java", ".json", ".toml"}
)
for stale_api in ("net.neoforged.neoforge", "CustomPacketPayload", "StreamCodec", "neoforge.mods.toml"):
    if stale_api in neo_production_text:
        fail(f"Modern NeoForge API/metadata leaked into the 1.20.1 production target: {stale_api}")

codec = read("src/main/java/com/chedidandrew/smartresourcedrops/network/ConfigTransferCodec.java")
require(codec, ["RAW_UTF8_MAX = 1_048_576", "CHUNK_BYTES = 30_720", "MAX_CHUNKS = 35", "CodingErrorAction.REPORT", "Inflater"], "fragment codec")
networking = read("src/main/java/com/chedidandrew/smartresourcedrops/network/SmartDropsNetworking.java")
require(networking, ["MAX_ACTIVE_PATCH_TRANSFERS = 32", "MAX_BUFFERED_PATCH_BYTES = 16 * 1024 * 1024", "canEditConfiguration", "clearPendingPatches"], "server transfer admission")
fabric_networking = read("src/main/java/com/chedidandrew/smartresourcedrops/platform/fabric/FabricNetworking.java")
require(fabric_networking, ["ConfigPatchFragmentPayload", "ServerPlayNetworking.registerGlobalReceiver"], "Fabric networking")
forbid(fabric_networking, ["ConfigPatchPayload.ID,", "ConfigSnapshotPayload.ID,"], "Fabric registered wire channels")
neo_networking = read("neoforge/src/main/java/com/chedidandrew/smartresourcedrops/platform/neoforge/NeoForgeNetworking.java")
require(neo_networking, ["PROTOCOL_VERSION = \"2\"", "ConfigPatchFragmentPayload", "ConfigSnapshotFragmentPayload", "PLAY_TO_SERVER", "PLAY_TO_CLIENT", "acceptMissingOr"], "NeoForge SimpleChannel")
neo_entrypoint = read("neoforge/src/main/java/com/chedidandrew/smartresourcedrops/platform/neoforge/NeoForgeEntrypoint.java")
require(
    neo_entrypoint,
    [
        "IExtensionPoint.DisplayTest.class",
        "NetworkConstants.IGNORESERVERONLY",
        "(remoteVersion, isServer) -> true",
    ],
    "NeoForge missing-client display test",
)
require(read("src/client/java/com/chedidandrew/smartresourcedrops/client/ClientCategoryTagIndex.java"), ["tags/blocks"], "1.20.1 client block-tag fallback")
require(read("src/client/java/com/chedidandrew/smartresourcedrops/client/ClientEntityCategoryTagIndex.java"), ["tags/entity_types"], "1.20.1 client entity-tag fallback")
require(read("src/client/java/com/chedidandrew/smartresourcedrops/client/ClientShearingTagIndex.java"), ["tags/entity_types"], "1.20.1 client shearing-tag fallback")
for client_index in (
    "src/client/java/com/chedidandrew/smartresourcedrops/client/ClientCategoryTagIndex.java",
    "src/client/java/com/chedidandrew/smartresourcedrops/client/ClientEntityCategoryTagIndex.java",
    "src/client/java/com/chedidandrew/smartresourcedrops/client/ClientShearingTagIndex.java",
):
    forbid(read(client_index), ["tags/block/", "tags/entity_type/"], client_index)

core_shell = read("tools/run_core_tests.sh")
require(core_shell, ["javac --release 17"], "POSIX core-test launcher")
forbid(core_shell, ["--release 21"], "POSIX core-test launcher")
core_powershell = read("tools/run_core_tests.ps1")
require(core_powershell, ["Test-Java17Jdk", "JAVA_17_HOME", "--release 17"], "PowerShell core-test launcher")
forbid(core_powershell, ["Java21", "JAVA_21_HOME", "--release 21"], "PowerShell core-test launcher")

root_build = read("build.gradle")
require(root_build, ["useLegacyMixinAp = true", "defaultRefmapName = 'smart_resource_drops.refmap.json'", "runPersistenceMarkServerTest", "runPersistenceRemoveServerTest", "runPersistenceVerifyAbsentServerTest", "persistence-absent.success"], "Fabric build gates")
neo_build = read("neoforge/build.gradle")
require(neo_build, ["runPersistenceMarkServerTest", "runPersistenceRemoveServerTest", "runPersistenceVerifyAbsentServerTest"], "NeoForge build gates")
forbid(
    neo_build,
    ["packagedProbeSourceSet", "runPackagedServerTest", "runPackagedClientTest", "forgeserveruserdev"],
    "NeoForge build gates",
)
require(
    neo_build,
    ["NeoForge category client produced no", "NeoForge category client log contains unexpected ERROR lines", "NeoForge category client log contains forbidden failure", "Error while loading the narrator", "Error starting SoundSystem. Turning off sounds & music"],
    "NeoForge physical-client log gate",
)

bug_report = read(".github/ISSUE_TEMPLATE/bug_report.yml")
require(
    bug_report,
    ["1.3.0+mc1.20.1", 'placeholder: "1.20.1"', "Fabric Loader 0.19.5 or NeoForge 1.20.1-47.1.106", "Fabric API 0.92.12+1.20.1", 'placeholder: "17"'],
    "1.20.1 bug-report template",
)
forbid(
    bug_report,
    ["26.2", "0.19.3", "26.2.0.72", "0.158.0+26.2", 'placeholder: "25"'],
    "1.20.1 bug-report template",
)

workflow_build = read(".github/workflows/build.yml")
require(workflow_build, ["Build and verify Minecraft 1.20.1", "Set up Java 21 Gradle runtime", "Switch to Java 17 for production server", "runClientSmoke", "run_fabric_multiplayer_smoke.sh", "runPersistenceVerifyAbsentServerTest", "runGameTestServer", "run_neoforge_optional_channel_smoke.sh", "run_neoforge_oversized_wire_smoke.sh", "run_neoforge_production_server_smoke.sh", "timeout-minutes: 150", "timeout-minutes: 210"], "build workflow")
require(
    root_build,
    ["Fabric client smoke produced no", "Fabric client smoke log contains unexpected ERROR lines", "Failed to verify authentication", "Error while loading the narrator", "Error starting SoundSystem. Turning off sounds & music", "Fabric client smoke log contains forbidden failure", "'Game crashed!'"],
    "Fabric physical-client log gate",
)
workflow_release = read(".github/workflows/release.yml")
require(workflow_release, ["v1.3.0+mc1.20.1", "origin/backport/1.20.1", "tag_commit\" = \"$branch_commit", "release_ready", "make_latest: false", "docs/releases/1.3.0+mc1.20.1.md", "timeout-minutes: 300", "Set up Java 21 Gradle runtime", "Switch to Java 17 for production server", "runPersistenceVerifyAbsentServerTest", "run_neoforge_production_server_smoke.sh", "tools/package_release.py --output dist", "dist/smart-resource-multiplier-1.3.0+mc1.20.1.jar", "dist/smart-resource-multiplier-neoforge-1.3.0+mc1.20.1.jar"], "release workflow")
for workflow_name, workflow in (("build workflow", workflow_build), ("release workflow", workflow_release)):
    forbid(workflow, ["runClientGameTest", "runMigrationRestart", "runPackagedServerTest", "runPackagedClientTest", "1.21.11", "21.11.45", "forgeserveruserdev"], workflow_name)

if workflow_build.count("java-version: '21'") != 2 or workflow_build.count("java-version: '17'") != 1:
    fail("build workflow must use Java 21 for both Gradle jobs and Java 17 only for the production server")
if workflow_release.count("java-version: '21'") != 1 or workflow_release.count("java-version: '17'") != 1:
    fail("release workflow must use Java 21 for Gradle and Java 17 only for the production server")

production_server_smoke = read("tools/run_neoforge_production_server_smoke.sh")
require(
    production_server_smoke,
    [
        "c1ea1c3be532c4444004efd7f9cc71e1590d010ff44845b93effba61e3bd1526",
        "84194a2f286ef7c14ed7ce0090dba59902951553",
        "--installServer",
        "version\\ \\\"17[.]",
        "Launching target 'forgeserver'",
        "forgeserveruserdev",
        "smartdrops status",
        "smartdrops validate",
        "No configuration or world data was changed.",
        "validate_neoforge_jar.py",
    ],
    "official production Forge 47 server gate",
)

wide_nbt = ROOT / "neoforge/src/gametest/resources/data/smart_resource_drops_gametest/structures/wide.nbt"
if wide_nbt.is_file():
    raw = wide_nbt.read_bytes()
    if len(raw) < 1000 or raw[:2] != b"\x1f\x8b":
        fail("NeoForge wide.nbt must be a nontrivial gzip-compressed legacy structure")
wide_snbt = read("src/gametest/resources/data/smart_resource_drops_gametest/gametest/structures/wide.snbt")
require(wide_snbt, ["DataVersion: 3465", "size: [32, 8, 32]"], "Fabric 1.20.1 wide structure")

for removed in (
    "neoforge/src/main/java/com/chedidandrew/smartresourcedrops/platform/neoforge/LegacyFabricProvenanceMigration.java",
    "neoforge/src/main/java/com/chedidandrew/smartresourcedrops/platform/neoforge/mixin/SerializableChunkDataLegacyProvenanceMixin.java",
    "neoforge/src/clienttest/java/com/chedidandrew/smartresourcedrops/platform/neoforge/NeoForgeMigrationRestartSmokeTest.java",
    "neoforge/src/test/resources/fixtures/fabric-placement-provenance-chunk--554625--233041.nbt.b64",
    "src/gametest/java/com/chedidandrew/smartresourcedrops/gametest/mixin/client/FabricClientGameTestTimeoutMixin.java",
    "src/gametest/resources/smart_resource_drops_gametest.mixins.json",
    "neoforge/src/packagedprobetest",
):
    if (ROOT / removed).exists():
        fail(f"Unsupported 1.21-era migration artifact remains: {removed}")

for path in ROOT.rglob("*.json"):
    if any(part in {"build", ".gradle", ".git", "run"} for part in path.parts):
        continue
    try:
        json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError) as exc:
        fail(f"Invalid JSON {path.relative_to(ROOT)}: {exc}")

icon = ROOT / "src/main/resources/assets/smart_resource_drops/icon.png"
if icon.is_file():
    data = icon.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n" or len(data) < 24:
        fail("Production icon is not a valid PNG header")
    else:
        width, height = struct.unpack(">II", data[16:24])
        if (width, height) != (512, 512):
            fail(f"Production icon must be 512x512, found {width}x{height}")
        expected_hash = "db216ccd6058404de18f797ebb5be87a313899a27c3f1971fdf086b8637dc190"
        actual_hash = hashlib.sha256(data).hexdigest()
        if actual_hash != expected_hash:
            fail(f"Production icon SHA-256 drifted: {actual_hash}")

if ERRORS:
    print(f"Package validation failed with {len(ERRORS)} error(s):", file=sys.stderr)
    for error in ERRORS:
        print(f"- {error}", file=sys.stderr)
    raise SystemExit(1)

print("Package validation passed for Smart Resource Multiplier 1.3.0+mc1.20.1.")
print("Targets: Fabric Loader 0.19.5/Fabric API 0.92.12+1.20.1 and NeoForge 1.20.1-47.1.106; Java 17.")
print(f"Publication latch: {root_props.get('release_ready')} (release workflow independently requires true).")
