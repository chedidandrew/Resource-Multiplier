#!/usr/bin/env python3
from __future__ import annotations

import json
import re
import struct
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ERRORS: list[str] = []
PROJECT_HOMEPAGE = "https://www.curseforge.com/minecraft/mc-mods/resource-multiplier"
PROJECT_SOURCES = "https://github.com/chedidandrew/Resource-Multiplier"
PROJECT_ISSUES = f"{PROJECT_SOURCES}/issues"
EXPECTED_CONTACT = {
    "homepage": PROJECT_HOMEPAGE,
    "issues": PROJECT_ISSUES,
    "sources": PROJECT_SOURCES,
}
EXPECTED_MODMENU_LINKS = {
    "smart_resource_drops.modmenu.link.kofi": "https://ko-fi.com/andrewchedid",
    "smart_resource_drops.modmenu.link.paypal": "https://www.paypal.com/paypalme/chedidandrew",
    "smart_resource_drops.modmenu.link.cash_app": "https://cash.app/%24AndrewChedid",
}
EXPECTED_ENTRYPOINTS = {
    "main": ["com.chedidandrew.smartresourcedrops.SmartResourceDrops"],
    "client": ["com.chedidandrew.smartresourcedrops.client.SmartResourceDropsClient"],
    "modmenu": [
        "com.chedidandrew.smartresourcedrops.client.SmartResourceDropsModMenuIntegration"
    ],
}
EXPECTED_SOURCE_DEPENDS = {
    "fabricloader": ">=${loader_version}",
    "minecraft": "~${minecraft_version}",
    "java": ">=25",
    "fabric-api": ">=${fabric_version}",
}


def fail(message: str) -> None:
    ERRORS.append(message)


def read_json(path: Path) -> object:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        fail(f"Invalid JSON: {path.relative_to(ROOT)}: {exc}")
        return {}


required = [
    ".gitignore",
    ".github/FUNDING.yml",
    ".github/ISSUE_TEMPLATE/bug_report.yml",
    ".github/ISSUE_TEMPLATE/config.yml",
    ".github/ISSUE_TEMPLATE/mod_compatibility.yml",
    ".github/PULL_REQUEST_TEMPLATE.md",
    "README.md",
    "CONTRIBUTING.md",
    "SECURITY.md",
    "LICENSE",
    "CHANGELOG.md",
    "BUILD_STATUS.md",
    "build.gradle",
    "settings.gradle",
    "gradle.properties",
    "gradlew",
    "gradlew.bat",
    "gradle/wrapper/gradle-wrapper.jar",
    "gradle/wrapper/gradle-wrapper.properties",
    ".github/workflows/build.yml",
    ".github/workflows/release.yml",
    "src/main/resources/fabric.mod.json",
    "src/main/resources/smart_resource_drops.mixins.json",
    "src/main/resources/assets/smart_resource_drops/icon.png",
    "src/main/resources/assets/smart_resource_drops/lang/en_us.json",
    "src/main/resources/data/smart_resource_drops/tags/item/protected_entity_loot.json",
    "src/main/resources/data/smart_resource_drops/tags/entity_type/shearing/standard_resources.json",
    "src/main/resources/data/smart_resource_drops/tags/entity_type/shearing/special.json",
    "src/main/java/com/chedidandrew/smartresourcedrops/SmartResourceDrops.java",
    "src/main/java/com/chedidandrew/smartresourcedrops/config/ConfigLoadDiagnostics.java",
    "src/main/java/com/chedidandrew/smartresourcedrops/config/ConfigValidationReport.java",
    "src/main/java/com/chedidandrew/smartresourcedrops/config/ConfigValidator.java",
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
    "src/main/java/com/chedidandrew/smartresourcedrops/core/shearing/ShearingTags.java",
    "src/main/java/com/chedidandrew/smartresourcedrops/mixin/LivingEntityShearingLootMixin.java",
    "src/main/java/com/chedidandrew/smartresourcedrops/mixin/PlayerShearingContextMixin.java",
    "src/main/java/com/chedidandrew/smartresourcedrops/mixin/ShearsDispenseItemBehaviorMixin.java",
    "src/client/java/com/chedidandrew/smartresourcedrops/client/SmartResourceDropsClient.java",
    "src/client/java/com/chedidandrew/smartresourcedrops/client/SmartResourceDropsModMenuIntegration.java",
    "src/client/java/com/chedidandrew/smartresourcedrops/client/SmartDropsConfigLoadingScreen.java",
    "src/client/java/com/chedidandrew/smartresourcedrops/client/ShearingDropsScreen.java",
    "src/client/java/com/chedidandrew/smartresourcedrops/client/ShearingOverridesScreen.java",
    "src/client/java/com/chedidandrew/smartresourcedrops/client/ShearingRuleEditScreen.java",
    "src/test/java/com/chedidandrew/smartresourcedrops/core/util/StackConsolidatorTest.java",
    "src/test/java/com/chedidandrew/smartresourcedrops/config/ConfigValidatorTest.java",
    "src/test/java/com/chedidandrew/smartresourcedrops/core/util/BlockLootOutputBudgetTest.java",
    "src/test/java/com/chedidandrew/smartresourcedrops/core/shearing/ShearingOutputBudgetTest.java",
    "src/test/java/com/chedidandrew/smartresourcedrops/core/shearing/ShearingOutputBufferTest.java",
    "src/test/java/com/chedidandrew/smartresourcedrops/core/shearing/ShearingRuleResolverTest.java",
    "src/test/java/com/chedidandrew/smartresourcedrops/core/provenance/ProvenanceTransitionPolicyTest.java",
    "src/test/resources/config/migration/schema-1.json",
    "src/test/resources/config/migration/schema-2.json",
    "src/gametest/java/com/chedidandrew/smartresourcedrops/gametest/SmartResourceDropsGameTests.java",
    "src/gametest/java/com/chedidandrew/smartresourcedrops/gametest/SmartResourceDropsEntityGameTests.java",
    "src/gametest/java/com/chedidandrew/smartresourcedrops/gametest/SmartResourceDropsBlockBudgetGameTests.java",
    "src/gametest/java/com/chedidandrew/smartresourcedrops/gametest/SmartResourceDropsShearingGameTests.java",
    "src/gametest/java/com/chedidandrew/smartresourcedrops/core/shearing/ShearingGameTestAccess.java",
    "src/gametest/java/com/chedidandrew/smartresourcedrops/gametest/GameTestPlayers.java",
    "src/gametest/java/com/chedidandrew/smartresourcedrops/gametest/fixture/GameTestEntityFixtures.java",
    "src/gametest/java/com/chedidandrew/smartresourcedrops/gametest/fixture/GameTestBlockLootFixtures.java",
    "src/gametest/java/com/chedidandrew/smartresourcedrops/gametest/SmartDropsClientGameTest.java",
    "src/gametest/resources/fabric.mod.json",
    "config-examples/default.json",
    "docs/ARCHITECTURE.md",
    "docs/ANTI_DUPE.md",
    "docs/COMMANDS.md",
    "docs/CONFIGURATION.md",
    "docs/COMPATIBILITY.md",
    "docs/PERFORMANCE.md",
    "docs/PUBLIC_RELEASE_CHECKLIST.md",
    "docs/TESTING.md",
    "docs/GITHUB_UPLOAD.md",
    "docs/IMPLEMENTATION_LOG.md",
    "docs/ROADMAP.md",
    "docs/images/general-config.webp",
    "docs/images/block-overrides.webp",
    "docs/images/shearing-config.webp",
    "tools/package_release.py",
    "scripts/test_release_packaging.py",
]
for relative in required:
    if not (ROOT / relative).is_file():
        fail(f"Missing required file: {relative}")

wrapper_jar = ROOT / "gradle/wrapper/gradle-wrapper.jar"
if wrapper_jar.is_file():
    try:
        with zipfile.ZipFile(wrapper_jar) as archive:
            if archive.testzip() is not None or "org/gradle/wrapper/GradleWrapperMain.class" not in archive.namelist():
                fail("gradle-wrapper.jar is corrupt or missing GradleWrapperMain")
    except zipfile.BadZipFile:
        fail("gradle-wrapper.jar is not a valid JAR")

scope_markers = {
    "README.md": ("project charter", "server-authoritative multiplication"),
    "CONTRIBUTING.md": ("scope charter", "concrete reproducible case"),
    "docs/ROADMAP.md": (
        "no supported public java api",
        "concrete, reproducible third-party integration case",
    ),
}
for relative, markers in scope_markers.items():
    document = (ROOT / relative).read_text(encoding="utf-8").lower()
    for marker in markers:
        if marker not in document:
            fail(f"{relative} is missing required scope/API statement: {marker}")

security_text = (ROOT / "SECURITY.md").read_text(encoding="utf-8").lower()
for marker in (
    "does not currently identify a verified project-owned private reporting channel",
    "do not post duplication-exploit steps",
    "redact player uuids",
):
    if marker not in security_text:
        fail(f"SECURITY.md is missing honest privacy/reporting guidance: {marker}")

license_text = (ROOT / "LICENSE").read_text(encoding="utf-8")
for marker in (
    "MIT License",
    "Copyright (c) 2026 Andrew Chedid",
    "Permission is hereby granted, free of charge, to any person obtaining a copy",
    "The above copyright notice and this permission notice shall be included",
    'THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND',
):
    if marker not in license_text:
        fail(f"LICENSE is missing required MIT text: {marker}")

issue_config = (ROOT / ".github/ISSUE_TEMPLATE/config.yml").read_text(encoding="utf-8")
if not re.search(r"(?m)^blank_issues_enabled:\s*true\s*$", issue_config):
    fail("Issue-template config must allow blank issues")
if not re.search(r"(?m)^contact_links:\s*\[\]\s*$", issue_config):
    fail("Issue-template config must keep external contact links empty unless a verified route is added")
if "http://" in issue_config or "https://" in issue_config:
    fail("Issue-template config must not publish an unreviewed external contact link")

workflow_paths = (
    ROOT / ".github/workflows/build.yml",
    ROOT / ".github/workflows/release.yml",
)
for workflow_path in workflow_paths:
    workflow_text = workflow_path.read_text(encoding="utf-8")
    action_refs = re.findall(r"(?m)^\s*uses:\s*[^@\s]+@([^\s#]+)", workflow_text)
    if not action_refs:
        fail(f"{workflow_path.relative_to(ROOT)} declares no GitHub Actions")
    for action_ref in action_refs:
        if re.fullmatch(r"[0-9a-f]{40}", action_ref) is None:
            fail(
                f"{workflow_path.relative_to(ROOT)} uses mutable action ref {action_ref!r}; "
                "pin third-party actions to full commit SHAs"
            )
    if "actions/checkout@" in workflow_text and "persist-credentials: false" not in workflow_text:
        fail(f"{workflow_path.relative_to(ROOT)} must disable persisted checkout credentials")

public_text_paths = (
    ROOT / "README.md",
    ROOT / "BUILD_STATUS.md",
    ROOT / "CHANGELOG.md",
    ROOT / "CONTRIBUTING.md",
    ROOT / "SECURITY.md",
    *sorted((ROOT / "docs").glob("*.md")),
)
personal_path_pattern = re.compile(r"(?:[A-Za-z]:\\Users\\|/home/[^/\s]+/|/Users/[^/\s]+/)")
for public_text_path in public_text_paths:
    if personal_path_pattern.search(public_text_path.read_text(encoding="utf-8")):
        fail(f"Public documentation contains a personal absolute path: {public_text_path.relative_to(ROOT)}")

form_markers = {
    ".github/ISSUE_TEMPLATE/bug_report.yml": (
        "name: Bug report",
        "id: reproduction",
        "Fabric Loader version",
        "Fabric API version",
        "Java version",
        "never upload an entire development world",
        "SECURITY.md",
        "Redact account IDs",
    ),
    ".github/ISSUE_TEMPLATE/mod_compatibility.yml": (
        "name: Mod compatibility report",
        "id: other-mod-link",
        "id: registry-id",
        "id: inspection-category",
        "id: validation",
        "Custom block placer",
        "Direct ItemEntity output",
        "never upload an entire development world",
        "id: minimal-set",
        "exact compatibility case",
        "Redact UUIDs",
    ),
    ".github/PULL_REQUEST_TEMPLATE.md": (
        "Gameplay impact:",
        "schema-migration impact:",
        "Server-authority",
        "## Scope and safety",
        "No new public Java compatibility API",
        "## Verification",
        "dedicated-server and client GameTests",
        "manual client/server checks",
        "No generated files",
        "SECURITY.md",
    ),
}
for relative, markers in form_markers.items():
    text = (ROOT / relative).read_text(encoding="utf-8")
    for marker in markers:
        if marker not in text:
            fail(f"{relative} is missing required publication guidance: {marker}")

funding_text = (ROOT / ".github/FUNDING.yml").read_text(encoding="utf-8")
for marker in (
    "ko_fi: andrewchedid",
    "https://www.paypal.com/paypalme/chedidandrew",
    "https://cash.app/%24AndrewChedid",
):
    if marker not in funding_text:
        fail(f".github/FUNDING.yml is missing required funding destination: {marker}")

generated_json_roots = {".build", ".gradle", ".gradle-wrapper", "build", "run"}
for path in sorted(ROOT.rglob("*.json")):
    relative_parts = path.relative_to(ROOT).parts
    if not any(part in generated_json_roots for part in relative_parts):
        read_json(path)

properties: dict[str, str] = {}
for raw_line in (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines():
    line = raw_line.strip()
    if line and not line.startswith("#") and "=" in line:
        key, value = line.split("=", 1)
        properties[key.strip()] = value.strip()

expected_properties = {
    "mod_version": "1.2.1",
    "minecraft_version": "26.2",
    "loader_version": "0.19.3",
    "loom_version": "1.17.20",
    "fabric_version": "0.158.0+26.2",
    "maven_group": "com.chedidandrew",
    "archives_base_name": "resource-multiplier",
}
for key, expected in expected_properties.items():
    if properties.get(key) != expected:
        fail(f"gradle.properties {key} must be {expected!r}, found {properties.get(key)!r}")
if properties.get("release_ready") != "true":
    fail("The stable 1.2.1 release commit must set release_ready=true")

readme_text = (ROOT / "README.md").read_text(encoding="utf-8")
readme_word_count = len(re.findall(r"\S+", readme_text))
if not 1_000 <= readme_word_count <= 1_500:
    fail(f"README.md public landing page must stay near 1,000-1,500 words, found {readme_word_count}")
for marker in (
    '<h1 align="center">Resource Multiplier</h1>',
    'src="src/main/resources/assets/smart_resource_drops/icon.png"',
    'alt="Resource Multiplier icon"',
    "actions/workflows/build.yml/badge.svg?branch=main",
    "Minecraft-26.2",
    "Loader-Fabric",
    "Java-25",
    "License-MIT",
    "Status-1.2.1-Release",
    "> [!IMPORTANT]",
    "Current stable release:",
    "www.curseforge.com/minecraft/mc-mods/resource-multiplier",
    "Download the current release from",
    "docs/COMMANDS.md",
    "docs/images/general-config.webp",
    "docs/images/block-overrides.webp",
    "docs/images/shearing-config.webp",
):
    if marker not in readme_text:
        fail(f"README.md is missing required public-presentation marker: {marker}")
if readme_text.count("```") % 2 != 0:
    fail("README.md contains an unclosed fenced code block")
if "Smart Resource Drops" in readme_text:
    fail("README.md still contains the former public name")

readme_targets = re.findall(r"!?\[[^\]]*\]\(([^)]+)\)", readme_text)
readme_targets.extend(re.findall(r'(?:href|src)="([^"]+)"', readme_text))
for raw_target in readme_targets:
    target = raw_target.strip().strip("<>")
    if target.startswith(("https://", "http://", "mailto:", "#")):
        continue
    path_text = target.split("#", 1)[0].split("?", 1)[0]
    if path_text and not (ROOT / path_text).exists():
        fail(f"README.md contains a broken relative link or image path: {target}")

for image_attributes in re.findall(r"<img\b([^>]*)>", readme_text, re.IGNORECASE):
    alt_match = re.search(r'alt="([^"]+)"', image_attributes)
    src_match = re.search(r'src="([^"]+)"', image_attributes)
    if alt_match is None or not alt_match.group(1).strip():
        fail("README.md contains an HTML image without descriptive alt text")
    if src_match is None:
        fail("README.md contains an HTML image without a source")
    width_match = re.search(r'width="(\d+)"', image_attributes)
    if width_match is not None and int(width_match.group(1)) > 760:
        fail("README.md contains an image wider than the 760-pixel landing-page limit")

readme_headings = [
    len(match.group(1))
    for match in re.finditer(r"(?m)^(#{1,6})\s+", readme_text)
]
if any(level != 2 for level in readme_headings):
    fail("README.md must use the centered HTML H1 followed by a flat H2 section hierarchy")

fabric = read_json(ROOT / "src/main/resources/fabric.mod.json")
if isinstance(fabric, dict):
    checks = {
        "schemaVersion": 1,
        "id": "smart_resource_drops",
        "name": "Resource Multiplier",
        "environment": "*",
    }
    for key, expected in checks.items():
        if fabric.get(key) != expected:
            fail(f"fabric.mod.json {key} mismatch")
    if fabric.get("contact") != EXPECTED_CONTACT:
        fail("fabric.mod.json must expose the canonical homepage, issues, and sources URLs")
    if fabric.get("custom") != {"modmenu": {"links": EXPECTED_MODMENU_LINKS}}:
        fail("fabric.mod.json must expose the exact Mod Menu support links")
    if fabric.get("license") != "MIT":
        fail("fabric.mod.json must declare the SPDX MIT license identifier")
    if fabric.get("icon") != "assets/smart_resource_drops/icon.png":
        fail("fabric.mod.json must reference the packaged production icon")
    depends = fabric.get("depends", {})
    if depends != EXPECTED_SOURCE_DEPENDS:
        fail("fabric.mod.json must declare the exact loader, Minecraft, Java, and Fabric API constraints")
    entrypoints = fabric.get("entrypoints")
    if entrypoints != EXPECTED_ENTRYPOINTS:
        fail("fabric.mod.json must declare the exact main, client, and Mod Menu entrypoints")
    if fabric.get("mixins") != ["smart_resource_drops.mixins.json"]:
        fail("fabric.mod.json must declare the production mixin configuration")
    suggests = fabric.get("suggests", {})
    if not isinstance(suggests, dict) or suggests.get("modmenu") != ">=${modmenu_version}":
        fail("fabric.mod.json must keep Mod Menu optional through suggests")

public_copy_contracts = {
    "src/main/java/com/chedidandrew/smartresourcedrops/SmartResourceDrops.java": (
        'public static final String MOD_NAME = "Resource Multiplier";',
    ),
    "src/main/java/com/chedidandrew/smartresourcedrops/command/BlockInspectionFormatter.java": (
        'Component.literal("Resource Multiplier Inspection")',
    ),
    "src/main/java/com/chedidandrew/smartresourcedrops/command/EntityInspectionFormatter.java": (
        'Component.literal("Resource Multiplier Entity Inspection")',
    ),
    "src/main/java/com/chedidandrew/smartresourcedrops/command/ConfigValidationFormatter.java": (
        'Component.literal("Resource Multiplier Validation")',
    ),
    "src/main/java/com/chedidandrew/smartresourcedrops/command/SmartDropsCommands.java": (
        '"Resource Multiplier: "',
        '"Resource Multiplier shearing: master="',
    ),
}

compatibility_contracts = {
    "src/main/java/com/chedidandrew/smartresourcedrops/SmartResourceDrops.java": (
        'public static final String MOD_ID = "smart_resource_drops";',
    ),
    "src/main/java/com/chedidandrew/smartresourcedrops/command/SmartDropsCommands.java": (
        'Commands.literal("smartdrops")',
    ),
    "src/client/java/com/chedidandrew/smartresourcedrops/client/SmartResourceDropsClient.java": (
        'ClientCommands.literal("smartdropsgui")',
        '"smart_resource_drops:open_config_gui"',
    ),
    "src/main/java/com/chedidandrew/smartresourcedrops/config/ConfigManager.java": (
        'resolve("smart_resource_drops.json")',
        '"smart_resource_drops.broken-"',
        '"smart_resource_drops.oversized-"',
        '"smart_resource_drops.schema-"',
    ),
    "src/main/java/com/chedidandrew/smartresourcedrops/config/SmartDropsConfig.java": (
        "public static final int CURRENT_SCHEMA = 3;",
    ),
    "src/main/java/com/chedidandrew/smartresourcedrops/provenance/PlacementTracker.java": (
        'SmartResourceDrops.id("placed_blocks")',
    ),
    "src/main/java/com/chedidandrew/smartresourcedrops/mixin/LivingEntityDeathLootMixin.java": (
        '"smart_resource_drops.kill_origin"',
    ),
    "src/main/java/com/chedidandrew/smartresourcedrops/mixin/FallingBlockEntityMixin.java": (
        '"SmartResourceDropsProtected"',
    ),
    "src/main/java/com/chedidandrew/smartresourcedrops/mixin/PistonMovingBlockEntityMixin.java": (
        '"SmartResourceDropsCaptured"',
        '"SmartResourceDropsProtectDestination"',
    ),
    "src/main/java/com/chedidandrew/smartresourcedrops/network/ConfigPatchPayload.java": (
        'SmartResourceDrops.id("config_patch")',
    ),
    "src/main/java/com/chedidandrew/smartresourcedrops/network/ConfigRequestPayload.java": (
        'SmartResourceDrops.id("config_request")',
    ),
    "src/main/java/com/chedidandrew/smartresourcedrops/network/ConfigResetPayload.java": (
        'SmartResourceDrops.id("config_reset")',
    ),
    "src/main/java/com/chedidandrew/smartresourcedrops/network/ConfigMutationResultPayload.java": (
        'SmartResourceDrops.id("config_mutation_result")',
    ),
    "src/main/java/com/chedidandrew/smartresourcedrops/network/ConfigSnapshotPayload.java": (
        'SmartResourceDrops.id("config_snapshot")',
    ),
    "src/main/java/com/chedidandrew/smartresourcedrops/network/ConfigInvalidationPayload.java": (
        'SmartResourceDrops.id("config_invalidation")',
    ),
}

for contract_name, contracts in (
    ("public wording", public_copy_contracts),
    ("compatibility identifier", compatibility_contracts),
):
    for relative, tokens in contracts.items():
        path = ROOT / relative
        if not path.is_file():
            fail(f"Missing {contract_name} source: {relative}")
            continue
        source_text = path.read_text(encoding="utf-8")
        for token in tokens:
            if token not in source_text:
                fail(f"{relative} is missing required {contract_name}: {token}")

retired_public_name = ("Smart Resource" + " Drops").casefold()
for source_root in (
    ROOT / "src/main",
    ROOT / "src/client",
    ROOT / "src/test",
    ROOT / "src/gametest",
):
    for path in source_root.rglob("*"):
        if path.is_file() and path.suffix in {".java", ".json", ".mcmeta", ".txt"}:
            if retired_public_name in path.read_text(encoding="utf-8").casefold():
                fail(f"Retired public display name remains in {path.relative_to(ROOT)}")

datapack_namespace = ROOT / "src/main/resources/data/smart_resource_drops"
if not datapack_namespace.is_dir():
    fail("Production datapack namespace must remain data/smart_resource_drops")
if (ROOT / "src/main/resources/data/resource_multiplier").exists():
    fail("Display-name namespace data/resource_multiplier must not replace the stable datapack namespace")

for schema_version in (1, 2):
    relative = f"src/test/resources/config/migration/schema-{schema_version}.json"
    fixture = read_json(ROOT / relative)
    if not isinstance(fixture, dict) or fixture.get("schemaVersion") != schema_version:
        fail(f"{relative} must preserve the schema-{schema_version} migration fixture")

java_roots = (
    ROOT / "src/main/java",
    ROOT / "src/client/java",
    ROOT / "src/test/java",
    ROOT / "src/gametest/java",
    ROOT / "tools/core-tests",
)
for java_root in java_roots:
    for path in java_root.rglob("*.java"):
        source_text = path.read_text(encoding="utf-8")
        package_match = re.search(r"(?m)^package\s+([^;]+);", source_text)
        if package_match is None or not package_match.group(1).startswith(
            "com.chedidandrew.smartresourcedrops"
        ):
            fail(f"Java package namespace changed in {path.relative_to(ROOT)}")
        if "com.chedidandrew.resourcemultiplier" in source_text:
            fail(f"Display-name-derived Java namespace leaked into {path.relative_to(ROOT)}")

mixin = read_json(ROOT / "src/main/resources/smart_resource_drops.mixins.json")
if isinstance(mixin, dict):
    if mixin.get("required") is not True:
        fail("Production mixin configuration must be required")
    if mixin.get("compatibilityLevel") != "JAVA_25":
        fail("Mixin compatibility level must be JAVA_25")
    package_name = mixin.get("package", "")
    listed = mixin.get("mixins")
    if isinstance(listed, list):
        if not listed:
            fail("Production mixin configuration must not be empty")
        for obsolete in ("BlockItemPlacementMixin", "LevelSetBlockMixin"):
            if obsolete in listed:
                fail(f"Legacy provenance mixin must not be enabled: {obsolete}")
        for required_mixin in (
            "PlayerShearingContextMixin",
            "ShearsDispenseItemBehaviorMixin",
            "LivingEntityShearingLootMixin",
        ):
            if required_mixin not in listed:
                fail(f"Required shearing mixin is not enabled: {required_mixin}")
        for name in listed:
            source = ROOT / "src/main/java" / Path(*package_name.split(".")) / f"{name}.java"
            if not source.is_file():
                fail(f"Mixin listed without source: {name}")
    else:
        fail("Production mixin configuration must contain a mixins list")
else:
    fail("Production mixin configuration must be a JSON object")

config = read_json(ROOT / "config-examples/default.json")
required_config_keys = {
    "enabled",
    "globalMultiplier",
    "maximumMultiplier",
    "smartPlacementProtection",
    "sourceMode",
    "filterMode",
    "blockMultipliers",
    "categoryMultipliers",
    "dimensionMultipliers",
    "blacklist",
    "whitelist",
    "tagBlacklist",
    "tagWhitelist",
    "multiplyExperience",
    "experienceMultiplier",
    "explosions",
    "automatedMining",
    "protectBlockEntities",
    "playerMining",
    "blockEntityAllowlist",
    "conservativePistonProtection",
    "allowPlayerOverrides",
    "maxPlayerMultiplier",
    "playerMultipliers",
    "statisticsEnabled",
    "entityDropsEnabled",
    "inheritDefaultEntityMultiplier",
    "defaultEntityMultiplier",
    "entityKillRequirement",
    "entityFilterMode",
    "bossDropsEnabled",
    "multiplyMobExperience",
    "mobExperienceMultiplier",
    "multiplyBossExperience",
    "entityCategoryMultipliers",
    "entityMultipliers",
    "entityBlacklist",
    "entityWhitelist",
    "entityTagBlacklist",
    "entityTagWhitelist",
    "manualShearingDropsEnabled",
    "automatedShearingDropsEnabled",
    "inheritDefaultShearingMultiplier",
    "defaultShearingMultiplier",
    "shearingEntityMultipliers",
}
if isinstance(config, dict):
    missing = required_config_keys - config.keys()
    if missing:
        fail(f"Default config missing keys: {sorted(missing)}")
    if config.get("globalMultiplier") != 2:
        fail("Default global multiplier must be 2")
    if config.get("maximumMultiplier") != 64:
        fail("Default maximum multiplier must be 64")
    if config.get("sourceMode") != "NATURAL_ONLY":
        fail("Default source mode must be NATURAL_ONLY")
    if config.get("smartPlacementProtection") is not True:
        fail("Placement protection must be enabled by default")
    if config.get("protectBlockEntities") is not True:
        fail("Block entities must be protected by default")
    if config.get("schemaVersion") != 3:
        fail("The shearing configuration must use schemaVersion 3")
    if config.get("entityDropsEnabled") is not False:
        fail("Entity death-loot multiplication must remain disabled by default")
    if config.get("entityKillRequirement") != "PLAYER_KILLS_ONLY":
        fail("The default entity kill requirement must be PLAYER_KILLS_ONLY")
    if config.get("bossDropsEnabled") is not False:
        fail("Boss death-loot multiplication must remain disabled by default")
    if config.get("multiplyMobExperience") is not False:
        fail("Mob experience multiplication must remain disabled by default")
    if config.get("multiplyBossExperience") is not False:
        fail("Boss experience multiplication must remain disabled by default")
    if config.get("manualShearingDropsEnabled") is not True:
        fail("Fresh/default configuration must enable manual shearing drops")
    if config.get("automatedShearingDropsEnabled") is not False:
        fail("Fresh/default configuration must disable automated shearing drops")
    if config.get("inheritDefaultShearingMultiplier") is not True:
        fail("Default shearing multiplier must inherit the global rule")
    if config.get("shearingEntityMultipliers") != {}:
        fail("Fresh/default configuration must not contain exact shearing overrides")
    if config.get("entityCategoryMultipliers") != {
        "golems": 1,
        "villagers_npcs": 1,
        "bosses": 1,
        "miscellaneous": 1,
    }:
        fail("Safe entity category defaults are missing or reordered")

    config_source = (
        ROOT
        / "src/main/java/com/chedidandrew/smartresourcedrops/config/SmartDropsConfig.java"
    ).read_text(encoding="utf-8")
    safety_method = re.search(
        r"public void installSafetyBlacklist\(\) \{(?P<body>.*?)\n    \}",
        config_source,
        re.DOTALL,
    )
    if safety_method is None:
        fail("Could not locate the authoritative default safety blacklist")
    else:
        authoritative_blacklist = re.findall(
            r'blacklist\.add\("([^"]+)"\);', safety_method.group("body")
        )
        if config.get("blacklist") != authoritative_blacklist:
            fail(
                "config-examples/default.json blacklist must exactly match "
                "SmartDropsConfig.installSafetyBlacklist() ordering"
            )

block_category_root = ROOT / "src/main/resources/data/smart_resource_drops/tags/block/categories"
expected_block_category_tags = {
    "building_blocks.json",
    "crops.json",
    "end.json",
    "leaves.json",
    "logs.json",
    "nether.json",
    "ores.json",
    "plants.json",
    "raw_resource_blocks.json",
    "soil.json",
    "stone.json",
}
actual_block_category_tags = {path.name for path in block_category_root.glob("*.json")}
if actual_block_category_tags != expected_block_category_tags:
    fail(
        "Production block-category datapack tags changed: "
        f"missing={sorted(expected_block_category_tags - actual_block_category_tags)}, "
        f"unexpected={sorted(actual_block_category_tags - expected_block_category_tags)}"
    )

plants_tag = read_json(block_category_root / "plants.json")
if isinstance(plants_tag, dict):
    plant_values = plants_tag.get("values", [])
    if not isinstance(plant_values, list):
        fail("Plants category tag values must be a list")
    else:
        if "#minecraft:tall_flowers" in plant_values:
            fail("Plants category uses removed Minecraft 26.2 tag #minecraft:tall_flowers")
        if "#minecraft:flowers" not in plant_values:
            fail("Plants category must include the Minecraft 26.2 #minecraft:flowers tag")

entity_categories = (
    "bosses",
    "villagers_npcs",
    "golems",
    "neutral",
    "passive",
    "hostile",
    "aquatic",
    "ambient",
    "miscellaneous",
)
entity_tag_root = ROOT / "src/main/resources/data/smart_resource_drops/tags/entity_type/categories"
for category in entity_categories:
    entity_tag = read_json(entity_tag_root / f"{category}.json")
    if isinstance(entity_tag, dict):
        if entity_tag.get("replace") is not False:
            fail(f"Entity category {category} must remain datapack-extensible with replace=false")
        values = entity_tag.get("values")
        if not isinstance(values, list):
            fail(f"Entity category {category} values must be a list")
        elif not all(isinstance(value, (str, dict)) for value in values):
            fail(f"Entity category {category} contains an invalid tag value")

boss_tag = read_json(entity_tag_root / "bosses.json")
if isinstance(boss_tag, dict):
    expected_bosses = {
        "minecraft:ender_dragon",
        "minecraft:wither",
        "minecraft:warden",
        "minecraft:elder_guardian",
        "minecraft:ravager",
        "minecraft:evoker",
    }
    if not expected_bosses.issubset(set(boss_tag.get("values", []))):
        fail("Boss safety tag is missing a required vanilla boss/special-progression entity")

protected_entity_loot = read_json(
    ROOT / "src/main/resources/data/smart_resource_drops/tags/item/protected_entity_loot.json"
)
if isinstance(protected_entity_loot, dict):
    if protected_entity_loot.get("replace") is not False:
        fail("Protected entity-loot item tag must remain datapack-extensible with replace=false")
    protected_values = protected_entity_loot.get("values")
    if not isinstance(protected_values, list):
        fail("Protected entity-loot item tag values must be a list")
    elif not {"minecraft:saddle", "minecraft:totem_of_undying"}.issubset(set(protected_values)):
        fail("Protected entity-loot item tag must include saddle and Totem of Undying")

shearing_tag_root = ROOT / "src/main/resources/data/smart_resource_drops/tags/entity_type/shearing"
standard_shearing = read_json(shearing_tag_root / "standard_resources.json")
if isinstance(standard_shearing, dict):
    if standard_shearing.get("replace") is not False:
        fail("Standard shearing tag must remain datapack-extensible with replace=false")
    if standard_shearing.get("values") != ["minecraft:sheep"]:
        fail("Production standard shearing tag must contain only minecraft:sheep")

special_shearing = read_json(shearing_tag_root / "special.json")
expected_special_shearing = {
    "minecraft:bogged",
    "minecraft:copper_golem",
    "minecraft:mooshroom",
    "minecraft:snow_golem",
    "minecraft:sulfur_cube",
}
if isinstance(special_shearing, dict):
    if special_shearing.get("replace") is not False:
        fail("Special shearing tag must remain datapack-extensible with replace=false")
    special_values = special_shearing.get("values")
    if not isinstance(special_values, list) or set(special_values) != expected_special_shearing:
        fail("Production special shearing tag differs from the audited Minecraft 26.2 safety set")
    if any("gametest" in str(value) or "fixture" in str(value) for value in special_values):
        fail("Development-only shearing fixture leaked into the production special tag")

entity_loot_source = (
    ROOT / "src/main/java/com/chedidandrew/smartresourcedrops/core/entity/EntityLootMultiplier.java"
).read_text(encoding="utf-8")
if "Items.SADDLE" not in entity_loot_source or "Items.TOTEM_OF_UNDYING" not in entity_loot_source:
    fail("Entity final-loot policy must protect ravager saddles and evoker totems")

icon = ROOT / "src/main/resources/assets/smart_resource_drops/icon.png"
if icon.is_file():
    data = icon.read_bytes()
    if len(data) < 24 or data[:8] != b"\x89PNG\r\n\x1a\n":
        fail("Mod icon is not a valid PNG")
    else:
        width, height = struct.unpack(">II", data[16:24])
        if (width, height) != (128, 128):
            fail(f"Mod icon must be 128x128, found {width}x{height}")

build_gradle = (ROOT / "build.gradle").read_text(encoding="utf-8")
for expected in [
    "splitEnvironmentSourceSets",
    "withSourcesJar",
    "options.release = 25",
    "useJUnitPlatform",
    "configureTests",
    "compileOnly \"com.terraformersmc:modmenu",
    "runClientGameTest",
    "-Xlint:deprecation",
]:
    if expected not in build_gradle:
        fail(f"build.gradle is missing {expected}")
if "-Werror" in build_gradle:
    fail("Deprecation reporting must not make third-party dependency warnings fatal")

for relative in (
    "src/main/java/com/chedidandrew/smartresourcedrops/core/entity/EntityClassifier.java",
    "src/main/java/com/chedidandrew/smartresourcedrops/core/entity/EntityDropTags.java",
):
    source_text = (ROOT / relative).read_text(encoding="utf-8")
    if "builtInRegistryHolder()" in source_text:
        fail(f"{relative} still uses deprecated EntityType.builtInRegistryHolder()")
    if "BuiltInRegistries.ENTITY_TYPE.wrapAsHolder" not in source_text:
        fail(f"{relative} does not use the supported registry holder access")

game_test_sources = list((ROOT / "src/gametest/java").rglob("*.java"))
for source in game_test_sources:
    if "makeMockServerPlayerInLevel" in source.read_text(encoding="utf-8"):
        fail(f"Deprecated mock-player helper remains in {source.relative_to(ROOT)}")

game_test_player_helper = (
    ROOT / "src/gametest/java/com/chedidandrew/smartresourcedrops/gametest/GameTestPlayers.java"
).read_text(encoding="utf-8")
if (
    "makeMockServerPlayer(GameType.SURVIVAL)" not in game_test_player_helper
    or "static ServerPlayer survival" not in game_test_player_helper
):
    fail("GameTests must centralize supported survival mock-player construction")

entity_game_tests = (
    ROOT
    / "src/gametest/java/com/chedidandrew/smartresourcedrops/gametest/SmartResourceDropsEntityGameTests.java"
).read_text(encoding="utf-8")
if (
    "assertExperienceTotalAndClear" not in entity_game_tests
    or "killAllEntitiesOfClass(ExperienceOrb.class)" not in entity_game_tests
):
    fail("Entity XP GameTests must isolate repeated scenarios by clearing prior XP orbs")

game_test_metadata = read_json(ROOT / "src/gametest/resources/fabric.mod.json")
if isinstance(game_test_metadata, dict):
    if game_test_metadata.get("id") != "smart_resource_drops_gametest":
        fail("GameTest metadata must preserve the smart_resource_drops_gametest id")
    if game_test_metadata.get("name") != "Resource Multiplier GameTests":
        fail("GameTest metadata must expose the Resource Multiplier GameTests name")
    game_test_entrypoints = game_test_metadata.get("entrypoints", {})
    if not isinstance(game_test_entrypoints, dict) or not game_test_entrypoints.get("fabric-client-gametest"):
        fail("GameTest metadata is missing the Fabric client GameTest entrypoint")
    elif (
        "com.chedidandrew.smartresourcedrops.gametest.fixture.GameTestEntityFixtures"
        not in game_test_entrypoints.get("main", [])
    ):
        fail("GameTest metadata is missing the development-only entity fixture initializer")
    elif (
        "com.chedidandrew.smartresourcedrops.gametest.fixture.GameTestBlockLootFixtures"
        not in game_test_entrypoints.get("main", [])
    ):
        fail("GameTest metadata is missing the development-only block-loot fixture initializer")
    elif (
        "com.chedidandrew.smartresourcedrops.gametest.SmartResourceDropsEntityGameTests"
        not in game_test_entrypoints.get("fabric-gametest", [])
    ):
        fail("GameTest metadata is missing the entity death-loot server suite")
    elif (
        "com.chedidandrew.smartresourcedrops.gametest.SmartResourceDropsBlockBudgetGameTests"
        not in game_test_entrypoints.get("fabric-gametest", [])
    ):
        fail("GameTest metadata is missing the block-loot budget server suite")

fixture_root = ROOT / "src/gametest/resources/data/smart_resource_drops_gametest/loot_table/entities"
required_fixture_tables = {
    "aquatic.json",
    "boss.json",
    "carrying.json",
    "category_only.json",
    "component_rich.json",
    "cooked_final.json",
    "direct_output.json",
    "duplicate_hook.json",
    "empty.json",
    "equipment.json",
    "exception.json",
    "hostile.json",
    "inventory.json",
    "looting_final.json",
    "nested_outer.json",
    "neutral.json",
    "passive.json",
    "unclassified.json",
    "unstackable.json",
}
actual_fixture_tables = {path.name for path in fixture_root.glob("*.json")}
if actual_fixture_tables != required_fixture_tables:
    fail(
        "Development entity loot fixtures differ from the required deterministic matrix: "
        f"missing={sorted(required_fixture_tables - actual_fixture_tables)}, "
        f"unexpected={sorted(actual_fixture_tables - required_fixture_tables)}"
    )

for production_root in (ROOT / "src/main", ROOT / "src/client"):
    leaked = [
        path.relative_to(ROOT).as_posix()
        for path in production_root.rglob("*")
        if path.is_file()
        and (
            "gametest" in path.as_posix().casefold()
            or "testmod" in path.as_posix().casefold()
            or "fixture" in path.parts
        )
    ]
    if leaked:
        fail(f"Development-only fixture leaked into production sources: {', '.join(leaked)}")

for workflow in (".github/workflows/build.yml", ".github/workflows/release.yml"):
    workflow_text = (ROOT / workflow).read_text(encoding="utf-8")
    if "runClientGameTest" not in workflow_text:
        fail(f"{workflow} must run the client GUI GameTest release gate")
    if "scripts/test_release_packaging.py" not in workflow_text:
        fail(f"{workflow} must run the deterministic source-packaging regression")

release_workflow = (ROOT / ".github/workflows/release.yml").read_text(encoding="utf-8")
build_workflow = (ROOT / ".github/workflows/build.yml").read_text(encoding="utf-8")
if re.search(r"(?m)^\s+tags:\s*", build_workflow):
    fail("The regular build workflow must not duplicate the authoritative release workflow on tags")
if "branches: ['**']" not in build_workflow or "pull_request:" not in build_workflow:
    fail("The regular build workflow must retain branch-push and pull-request validation")
if "tags: ['v*']" not in release_workflow:
    fail("The release workflow must remain the sole v* tag workflow")
if "tools/package_release.py --output-dir dist" not in release_workflow or "dist/*" not in release_workflow:
    fail("The release workflow must create and publish the validated deterministic release bundle")
for required_release_gate in (
    'test "$release_ready" = "true"',
    "git merge-base --is-ancestor",
    "refs/remotes/origin/main",
    "fetch-depth: 0",
):
    if required_release_gate not in release_workflow:
        fail(f"The release workflow is missing its publication safety gate: {required_release_gate}")

java_files = list((ROOT / "src/main/java").rglob("*.java")) + list((ROOT / "src/client/java").rglob("*.java"))
if len(java_files) < 20:
    fail(f"Expected complete implementation, found only {len(java_files)} Java files")

for source in java_files:
    text = source.read_text(encoding="utf-8")
    if "TODO" in text or "FIXME" in text:
        fail(f"Unresolved TODO/FIXME in {source.relative_to(ROOT)}")

piston_mixin = (ROOT / "src/main/java/com/chedidandrew/smartresourcedrops/mixin/PistonMovingBlockEntityMixin.java").read_text(encoding="utf-8")
if "(PistonMovingBlockEntityMixin) (Object) entity" in piston_mixin:
    fail("Piston mixin must access target state through its injected interface, not a mixin-class cast")
if "implements ProtectedPistonMovement" not in piston_mixin:
    fail("Piston mixin is missing its runtime-safe provenance carrier interface")

screen_source = (ROOT / "src/client/java/com/chedidandrew/smartresourcedrops/client/SmartDropsConfigScreen.java").read_text(encoding="utf-8")
if "left + 520" in screen_source or "Math.max(4" in screen_source:
    fail("Client configuration screen still contains a known narrow-layout overflow")

for forbidden in ["vein mining", "auto-smelt", "magnet pickup"]:
    for source in java_files:
        if forbidden in source.read_text(encoding="utf-8").lower():
            fail(f"Out-of-scope behavior reference in source: {source.relative_to(ROOT)}")

if ERRORS:
    print("PACKAGE VALIDATION FAILED", file=sys.stderr)
    for error in ERRORS:
        print(f"- {error}", file=sys.stderr)
    sys.exit(1)

print(f"PASS: package metadata, JSON, icon, and {len(java_files)} Java sources validated")
