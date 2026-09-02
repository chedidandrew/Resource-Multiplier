#!/usr/bin/env python3
"""Static regression checks for Mod Menu and the hierarchical config editor."""

from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CLIENT = ROOT / "src/client/java/com/chedidandrew/smartresourcedrops/client"
FABRIC_CLIENT = ROOT / "src/client/java/com/chedidandrew/smartresourcedrops/platform/fabric/client"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def read(path: Path) -> str:
    require(path.is_file(), f"Missing required source file: {path.relative_to(ROOT)}")
    return path.read_text(encoding="utf-8")


def method_body(source: str, method_name: str) -> str:
    """Return a Java method body for small source-contract assertions."""
    match = re.search(rf"\b{re.escape(method_name)}\s*\([^)]*\)\s*\{{", source)
    require(match is not None, f"Missing method {method_name}(...) in checked source")
    start = match.end() - 1
    depth = 0
    for index in range(start, len(source)):
        character = source[index]
        if character == "{":
            depth += 1
        elif character == "}":
            depth -= 1
            if depth == 0:
                return source[start + 1:index]
    raise AssertionError(f"Unclosed method body for {method_name}(...)")


def require_before(source: str, first: str, second: str, message: str) -> None:
    first_index = source.find(first)
    second_index = source.find(second)
    require(first_index >= 0 and second_index >= 0 and first_index < second_index, message)


fabric = json.loads(read(ROOT / "src/main/resources/fabric.mod.json"))
build = read(ROOT / "build.gradle")
props = read(ROOT / "gradle.properties")
lang = json.loads(read(ROOT / "src/main/resources/assets/smart_resource_drops/lang/en_us.json"))
public_name = "Smart Resource Multiplier"
legacy_public_name = "Smart Resource" + " Drops"

integration = read(FABRIC_CLIENT / "FabricModMenuIntegration.java")
routes = read(CLIENT / "SmartDropsConfigScreens.java")
client = read(FABRIC_CLIENT / "FabricClientEntrypoint.java")
client_bridge = read(CLIENT / "ClientNetworkBridge.java")
queue = read(CLIENT / "../core/client/util/ClientCommandQueue.java")
loading = read(CLIENT / "SmartDropsConfigLoadingScreen.java")
state = read(CLIENT / "ClientConfigState.java")
root_screen = read(CLIENT / "SmartDropsConfigScreen.java")
reset_confirmation = read(CLIENT / "ResetAllSettingsConfirmScreen.java")
session = read(CLIENT / "ConfigEditorSession.java")
category_tag_index = read(CLIENT / "ClientCategoryTagIndex.java")
entity_category_tag_index = read(CLIENT / "ClientEntityCategoryTagIndex.java")
entity_overrides = read(CLIENT / "EntityOverridesScreen.java")
entity_rule_edit = read(CLIENT / "EntityRuleEditScreen.java")
entity_filter = read(CLIENT / "EntityFilterScreen.java")
sub_screen = read(CLIENT / "SmartDropsSubScreen.java")
structured_list = read(CLIENT / "StructuredConfigList.java")
networking = read(ROOT / "src/main/java/com/chedidandrew/smartresourcedrops/network/SmartDropsNetworking.java")
fabric_networking = read(
    ROOT / "src/main/java/com/chedidandrew/smartresourcedrops/platform/fabric/FabricNetworking.java"
)
reset_payload = read(ROOT / "src/main/java/com/chedidandrew/smartresourcedrops/network/ConfigResetPayload.java")
invalidation_payload = read(
    ROOT / "src/main/java/com/chedidandrew/smartresourcedrops/network/ConfigInvalidationPayload.java"
)
mutation_result_payload = read(
    ROOT / "src/main/java/com/chedidandrew/smartresourcedrops/network/ConfigMutationResultPayload.java"
)
manager = read(ROOT / "src/main/java/com/chedidandrew/smartresourcedrops/config/ConfigManager.java")
commands = read(ROOT / "src/main/java/com/chedidandrew/smartresourcedrops/command/SmartDropsCommands.java")
atomic_writer = read(ROOT / "src/main/java/com/chedidandrew/smartresourcedrops/core/util/AtomicConfigWriter.java")

required_child_names = (
    "RuleListScreen.java",
    "RuleEditScreen.java",
    "BlockOverridesScreen.java",
    "FilterConfigScreen.java",
    "AdvancedConfigScreen.java",
    "PresetPreviewScreen.java",
)
child_paths = sorted(
    path
    for path in CLIENT.glob("*Screen.java")
    if path.name not in {
        "SmartDropsConfigScreen.java",
        "SmartDropsConfigLoadingScreen.java",
        "SmartDropsSubScreen.java",
        "ResetAllSettingsConfirmScreen.java",
    }
)
children = {path.name: read(path) for path in child_paths}
for name in required_child_names:
    require(name in children, f"Missing hierarchical editor screen: {name}")

# The display brand is public copy. Established namespaces and command literals are
# compatibility contracts and must not follow the display-name rename.
require(fabric.get("id") == "smart_resource_drops", "The stable mod id must remain smart_resource_drops")
require(fabric.get("name") == public_name, "fabric.mod.json must expose the Smart Resource Multiplier name")
require(
    fabric.get("contact")
    == {
        "homepage": "https://www.curseforge.com/minecraft/mc-mods/resource-multiplier",
        "issues": "https://github.com/chedidandrew/Resource-Multiplier/issues",
        "sources": "https://github.com/chedidandrew/Resource-Multiplier",
    },
    "Mod Menu metadata must expose the canonical website, issue tracker, and source repository",
)
require(
    fabric.get("custom") == {"modmenu": {"links": {'smart_resource_drops.modmenu.link.kofi': 'https://ko-fi.com/andrewchedid', 'smart_resource_drops.modmenu.link.paypal': 'https://www.paypal.com/paypalme/chedidandrew', 'smart_resource_drops.modmenu.link.cash_app': 'https://cash.app/%24AndrewChedid'}}},
    "Mod Menu metadata must expose the exact optional support links",
)
for key, label in {
    "smart_resource_drops.modmenu.link.kofi": "Support on Ko-fi",
    "smart_resource_drops.modmenu.link.paypal": "Support on PayPal",
    "smart_resource_drops.modmenu.link.cash_app": "Support on Cash App",
}.items():
    require(lang.get(key) == label, f"Missing localized Mod Menu link label: {key}")
require(fabric.get("license") == "MIT", "Mod Menu metadata must expose the MIT license")
require(
    fabric.get("icon") == "assets/smart_resource_drops/icon.png",
    "Mod Menu metadata must reference the packaged production icon",
)
require(
    lang.get("modmenu.nameTranslation.smart_resource_drops") == public_name,
    "Mod Menu must display Smart Resource Multiplier",
)
require(
    lang.get("smart_resource_drops.title") == public_name,
    "The config GUI title must display Smart Resource Multiplier",
)
require(
    'Component.translatable("smart_resource_drops.title")' in root_screen,
    "The root config screen must keep using the stable localized title key",
)
require(
    lang.get("smart_resource_drops.gui.reset_confirm_title") == "Reset Smart Resource Multiplier?",
    "The reset confirmation title must use the public display name",
)
reset_body_copy = lang.get("smart_resource_drops.gui.reset_confirm_body", "")
require(
    "every Smart Resource Multiplier setting" in reset_body_copy,
    "The reset confirmation body must use the public display name",
)
require(
    'Component.translatable("smart_resource_drops.gui.reset_confirm_title")' in reset_confirmation
    and 'Component.translatable("smart_resource_drops.gui.reset_confirm_body")' in reset_confirmation,
    "The reset screen must render the stable localized confirmation copy",
)
require(
    not any(legacy_public_name in value for value in lang.values() if isinstance(value, str)),
    "A localized value still exposes the retired display name",
)
require('Commands.literal("smartdrops")' in commands, "The stable /smartdrops command literal changed")
require(
    'ClientCommands.literal("smartdropsgui")' in client,
    "The stable /smartdropsgui command literal changed",
)

# Mod Menu remains optional and both entry points use one routing policy.
entrypoints = fabric.get("entrypoints", {})
require(
    "com.chedidandrew.smartresourcedrops.platform.fabric.client.FabricModMenuIntegration"
    in entrypoints.get("modmenu", []),
    "fabric.mod.json must register the Mod Menu entrypoint",
)
require(
    fabric.get("suggests", {}).get("modmenu") == ">=${modmenu_version}",
    "Mod Menu must remain an optional suggested dependency",
)
require("compileOnly \"com.terraformersmc:modmenu:${modMenuVersionValue}\"" in build, "Mod Menu must be compile-only")
require("https://maven.terraformersmc.com/" in build, "Terraformers release repository is missing")
require(re.search(r"^modmenu_version=20\.0\.0$", props, re.MULTILINE) is not None, "Expected Mod Menu 20.0.0 pin")
require("implements ModMenuApi" in integration, "Integration must implement ModMenuApi")
require("SmartDropsConfigScreens::create" in integration, "Mod Menu must use the shared config-screen route")
require("SmartDropsConfigScreens.create" in client, "/smartdropsgui must use the same config-screen route")
require("OPEN_CONFIG_QUEUE_KEY" in client and "runCoalesced" in client, "/smartdropsgui must open after chat closes")
require("canOpenDelayedCommand" in client, "Delayed /smartdropsgui opens must guard connection and screen state")
require(
    "QueuedWorkDrain.removeDue" in queue and queue.index("dueActions =") < queue.index("action.run()"),
    "Queued actions must be removed before execution so cancellation can safely re-enter the queue",
)
require("LOCAL_DEFAULTS" in routes and "ConfigManager.snapshotForClient()" in routes, "Title screen must open local defaults")
require("hasSingleplayerServer()" in routes, "Integrated-server startup must not fall back to local defaults")
require("cachedSnapshot" in routes, "A connection-scoped cached snapshot should open READY")

# Loading/request lifecycle, response validation, and the explicit client-thread handoff.
require("initialRequestStarted" in loading, "Loading screen must suppress duplicate init requests")
require("enum State" in loading and "LOADING" in loading and "ERROR" in loading, "Loading/error phases must be explicit")
require("REQUEST_TIMEOUT_TICKS" in loading, "Loading requests must have a bounded timeout")
require('Component.literal("Retry")' not in loading, "Loading screen should use localized retry UI")
require("void removed()" in loading and "cancelRequest(this.requestId)" in loading, "Removing a loading screen must cancel its request")
require("this.retryButton.active = this.state == State.ERROR" in loading, "Resize must preserve Retry availability")
require("REQUESTS.isCurrent" in state and "loading.acceptsRequest" in state, "Late or stale snapshots must be ignored")
require("minecraft.gui.screen()" in state, "Responses must verify the requesting screen is still current")
require("tryParseSnapshotJson" in state and "decoded.isEmpty()" in state, "Invalid authoritative snapshots must enter ERROR")
require("ConfigPatchPayload" in state, "Client config state must submit the bounded patch payload")
require("ClientNetworkBridge.send(new ConfigPatchPayload" in state, "Apply must send its patch immediately")
require("ConfigResetPayload" in state, "Client config state must submit the dedicated reset payload")
require(
    "ClientNetworkBridge.send(new ConfigResetPayload" in state,
    "Reset must send one dedicated request instead of expanding defaults into patches",
)
require("ClientNetworkBridge.install" in client, "Fabric must install the loader-neutral client transport")
require("ClientPlayNetworking.send(payload)" in client, "Fabric transport must send through Fabric networking")
require("interface Transport" in client_bridge, "The shared client transport contract is missing")
require("context.client().execute" in client, "Snapshot handling must cross an explicit client-thread boundary")
require(
    "ClientPlayConnectionEvents.DISCONNECT.register" in client and "ClientCommandQueue.clear();" in client,
    "Disconnect must immediately discard queued client-side work",
)

# The root screen is General only. Registry enumeration, search, patch construction,
# and staged state belong to the shared session rather than the screen hierarchy.
for key in (
    "smart_resource_drops.gui.global_multiplier",
    "smart_resource_drops.gui.placement_protection_label",
    "smart_resource_drops.gui.multiplier_source",
    "smart_resource_drops.gui.multiply_xp",
    "smart_resource_drops.gui.root_block_categories",
    "smart_resource_drops.gui.block_overrides",
    "smart_resource_drops.gui.dimensions",
    "smart_resource_drops.gui.root_block_filters",
    "smart_resource_drops.gui.advanced",
    "smart_resource_drops.gui.entity_drops",
):
    require(key in root_screen, f"General screen is missing its control/navigation key: {key}")

# This clarity pass deliberately changes only two root navigation labels. The
# shared Categories/Filters keys remain the child-screen titles.
root_navigation = method_body(root_screen, "addNavigation")
root_label_contract = {
    "smart_resource_drops.gui.root_block_categories": "Block Categories",
    "smart_resource_drops.gui.root_block_filters": "Block Filters",
}
for key, expected in root_label_contract.items():
    require(lang.get(key) == expected, f"Root navigation label changed unexpectedly: {key}")
    require(
        f'Component.translatable("{key}")' in root_navigation,
        f"Root navigation must use its root-specific label key: {key}",
    )
for shared_key in (
    "smart_resource_drops.gui.categories",
    "smart_resource_drops.gui.filters",
):
    require(
        f'Component.translatable("{shared_key}")' not in root_navigation,
        f"Root navigation must not reuse the child-screen title key: {shared_key}",
    )

rule_list_screen = children["RuleListScreen.java"]
filter_config_screen = children["FilterConfigScreen.java"]
require(
    re.search(
        r'super\s*\(\s*Component\.translatable\(\s*kind\s*==\s*Kind\.CATEGORY\s*'
        r'\?\s*"smart_resource_drops\.gui\.categories"\s*'
        r':\s*"smart_resource_drops\.gui\.dimensions"\s*\)',
        rule_list_screen,
    )
    is not None,
    "The Categories child-screen title must keep using the shared Categories key",
)
require(
    re.search(
        r'super\s*\(\s*Component\.translatable\(\s*'
        r'"smart_resource_drops\.gui\.filters"\s*\)',
        filter_config_screen,
    )
    is not None,
    "The Filters child-screen title must keep using the shared Filters key",
)
require(
    lang.get("smart_resource_drops.gui.categories") == "Categories"
    and lang.get("smart_resource_drops.gui.filters") == "Filters",
    "Shared child-screen titles must remain Categories and Filters",
)

root_navigation_tooltips = {
    "smart_resource_drops.gui.root_dimensions_tooltip": (
        "Configure block-drop multipliers for specific dimensions."
    ),
    "smart_resource_drops.gui.root_advanced_tooltip": (
        "Configure presets, block sources, block-entity safety, piston handling, "
        "personal overrides, and runtime statistics."
    ),
    "smart_resource_drops.gui.root_entity_drops_tooltip": (
        "Configure entity death loot, mob XP, and supported entity shearing. "
        "These settings are separate from block drops."
    ),
}
for key, expected in root_navigation_tooltips.items():
    require(lang.get(key) == expected, f"Root navigation tooltip changed unexpectedly: {key}")
    require(
        re.search(
            rf'Component\.translatable\(\s*"{re.escape(key)}"\s*\)',
            root_navigation,
        )
        is not None,
        f"Root navigation button is missing its tooltip key: {key}",
    )
require(
    root_navigation.count(".tooltip(Tooltip.create(Component.translatable(") == 3,
    "Only Dimensions, Advanced, and Entity Drops should gain root navigation tooltips",
)

block_xp_tooltips = {
    "smart_resource_drops.gui.multiply_xp_tooltip": (
        "Multiply XP produced by eligible block breaks."
    ),
    "smart_resource_drops.gui.xp_multiplier_tooltip": (
        "Sets the multiplier for XP produced by eligible block breaks when block XP "
        "multiplication is enabled."
    ),
}
for key, expected in block_xp_tooltips.items():
    require(lang.get(key) == expected, f"Block-XP tooltip changed unexpectedly: {key}")
    require(key in root_screen, f"The root block-XP control must keep using its tooltip key: {key}")

advanced_screen = children["AdvancedConfigScreen.java"]
advanced_rows = method_body(advanced_screen, "refreshRows")
advanced_boolean_body = method_body(advanced_screen, "addBooleanRow")
advanced_tooltip_contract = (
    (
        "smart_resource_drops.gui.enabled",
        "smart_resource_drops.gui.enabled_tooltip",
        "Master switch for Smart Resource Multiplier. Saved settings remain unchanged while multiplication is disabled.",
    ),
    (
        "smart_resource_drops.gui.player_mining",
        "smart_resource_drops.gui.player_mining_tooltip",
        "Allow eligible block drops caused by player mining to use block multipliers.",
    ),
    (
        "smart_resource_drops.gui.explosions",
        "smart_resource_drops.gui.explosions_tooltip",
        "Allow eligible block drops caused by explosions to use block multipliers.",
    ),
    (
        "smart_resource_drops.gui.automated_mining",
        "smart_resource_drops.gui.automated_mining_tooltip",
        "Allow supported non-player Block.dropResources paths to use block multipliers. "
        "Systems that create or insert items directly remain unchanged.",
    ),
    (
        "smart_resource_drops.gui.protect_block_entities",
        "smart_resource_drops.gui.protect_block_entities_tooltip",
        "Keep blocks with block entities at vanilla 1x unless explicitly allowlisted, "
        "protecting inventories and special data.",
    ),
    (
        "smart_resource_drops.gui.piston_safety",
        "smart_resource_drops.gui.piston_safety_tooltip",
        "Treat piston-moved destination blocks as protected so placement provenance "
        "cannot be lost or exploited.",
    ),
    (
        "smart_resource_drops.gui.player_overrides",
        "smart_resource_drops.gui.player_overrides_tooltip",
        "Allow players to use personal block multipliers within the limits configured by the server.",
    ),
    (
        "smart_resource_drops.gui.statistics",
        "smart_resource_drops.gui.statistics_tooltip",
        "Track block-multiplication activity in memory for the current server session. "
        "This does not change drop behavior.",
    ),
)
for label_key, tooltip_key, expected in advanced_tooltip_contract:
    require(
        lang.get(tooltip_key) == expected,
        f"Advanced explanatory tooltip changed unexpectedly: {tooltip_key}",
    )
    require(
        re.search(
            rf'Component\.translatable\("{re.escape(label_key)}"\)\s*,\s*'
            rf'Component\.translatable\("{re.escape(tooltip_key)}"\)',
            advanced_rows,
        )
        is not None,
        f"Advanced row must pair {label_key} with {tooltip_key}",
    )
require(
    re.search(
        r"\baddBooleanRow\s*\([^)]*\bfinal\s+Component\s+tooltip\s*,",
        advanced_screen,
    )
    is not None
    and re.search(r"rightDetail\s*,\s*tooltip\s*,", advanced_boolean_body) is not None,
    "Advanced boolean rows must pass their setting-specific tooltip into StructuredConfigList",
)
require(
    'Component.literal(": ")' not in advanced_boolean_body
    and re.search(r"\.append\s*\(\s*label\s*\)", advanced_boolean_body) is None,
    "Advanced tooltips must not regress to the generic label-plus-ON/OFF construction",
)

require(
    lang.get("smart_resource_drops.gui.source_all_tooltip")
    == "All eligible blocks can receive multiplied drops regardless of placement history.",
    "The already-clear All Blocks tooltip must remain unchanged",
)
preserved_tooltips = {
    "smart_resource_drops.gui.global_multiplier_tooltip": (
        "Default multiplier used when no more specific override exists."
    ),
    "smart_resource_drops.gui.placement_protection_tooltip": (
        "In Natural Blocks Only mode, prevents multiplied drops from blocks previously placed by a player."
    ),
    "smart_resource_drops.gui.block_multiplier_tooltip": (
        "Overrides this block only. Inherit uses its category, dimension, then Global."
    ),
    "smart_resource_drops.gui.filter_blacklist_tooltip": (
        "All eligible blocks are multiplied except blocks or tags added to the blacklist."
    ),
    "smart_resource_drops.gui.entity_default_multiplier_tooltip": (
        "Used when no exact entity or category override exists. Inherit uses Global."
    ),
    "smart_resource_drops.gui.entity_kill_requirement_tooltip": (
        "Controls which authoritative death attributions qualify for entity items and mob XP."
    ),
    "smart_resource_drops.gui.multiply_mob_xp_tooltip": (
        "Separately multiplies XP from qualifying living-entity deaths."
    ),
    "smart_resource_drops.gui.boss_drops_tooltip": (
        "Allows configured multipliers for normal boss loot-table output; special rewards remain excluded."
    ),
    "smart_resource_drops.gui.manual_shearing_tooltip": (
        "Multiply certified standard shearing loot produced by a player's entity interaction."
    ),
    "smart_resource_drops.gui.shearing_safety_tooltip": (
        "Unknown and special shearables cannot receive overrides. Shearing output safety limits cannot be disabled."
    ),
    "smart_resource_drops.gui.preset_warning": (
        "This preset replaces all block, category, and dimension multiplier overrides."
    ),
}
for key, expected in preserved_tooltips.items():
    require(lang.get(key) == expected, f"Existing detailed tooltip must remain unchanged: {key}")

for obsolete in (
    "BuiltInRegistries",
    "ViewMode",
    "previousButton",
    "nextButton",
    "rowsPerPage",
    "mouseScrolled(",
    "filteredKeys(",
    '"smart_resource_drops.gui.previous"',
    '"smart_resource_drops.gui.next"',
    '"smart_resource_drops.gui.page"',
):
    require(obsolete not in root_screen, f"General screen still contains flat-editor state/control: {obsolete}")
require(
    re.search(r"\b(?:int|Integer)\s+page\b", root_screen) is None,
    "General screen must not retain the old paged-view cursor",
)
require("ConfigEditorSession" in root_screen, "General screen must be backed by one shared editor session")
require("ResetAllSettingsConfirmScreen" in root_screen, "Reset must open a dedicated confirmation screen")
require(
    re.search(r"\bresetButton\s*\(\s*\)", root_screen) is not None,
    "The root must expose its Reset All Settings button for runtime regression checks",
)
require(
    re.search(r"\bConfigPatch\s+buildPatch\s*\(", root_screen) is None,
    "Patch construction must not drift back into the root screen",
)

require(
    re.search(r"\bBuiltInRegistries\s*\.\s*BLOCK\b", session) is not None,
    "ConfigEditorSession must own the cached block registry catalogue",
)
require(re.search(r"\bsearchBlocks\s*\(", session) is not None, "ConfigEditorSession must own block search")
require(re.search(r"\bbuildPatch\s*\(\s*\)", session) is not None, "ConfigEditorSession must build the single staged patch")
require(re.search(r"\bisDirty\s*\(\s*\)", session) is not None, "ConfigEditorSession must own dirty-state calculation")

search_cache_limit = re.search(r"\bSEARCH_CACHE_LIMIT\s*=\s*(\d+)\s*;", session)
require(
    search_cache_limit is not None and 1 <= int(search_cache_limit.group(1)) <= 256,
    "ConfigEditorSession search memoization must have a small positive hard limit",
)
for method_name in ("searchBlocks", "searchFilterBlocks", "searchFilterTags"):
    require(
        re.search(r"\bcachedSearch\s*\(", method_body(session, method_name)) is not None,
        f"ConfigEditorSession.{method_name} must use the shared bounded search cache",
    )
cached_search_body = method_body(session, "cachedSearch")
for contract in (
    r"\bcache\s*\.\s*get\s*\(",
    r"\bcache\s*\.\s*size\s*\(\s*\)\s*>=\s*SEARCH_CACHE_LIMIT",
    r"\biterator\s*\.\s*remove\s*\(\s*\)",
    r"\bcache\s*\.\s*put\s*\(",
):
    require(
        re.search(contract, cached_search_body) is not None,
        f"ConfigEditorSession.cachedSearch is missing bounded-cache behavior: {contract}",
    )
require_before(
    cached_search_body,
    "iterator.remove()",
    "cache.put(query, loaded)",
    "The bounded search cache must evict before inserting a new result",
)
require(
    re.search(r"\b(?:List|Map)<[^;\n]+>\s+\w*(?:block|Block)\w*\s*(?:=|;)", session) is not None,
    "ConfigEditorSession must retain block metadata/search data instead of rescanning every frame",
)

# Title-screen category membership has no live world tag binding. Resolve the installed
# data-pack tag graph as a lazy fallback, then merge it with live registry categories when
# the session's block catalogue is first requested.
require("class ClientCategoryTagIndex" in category_tag_index, "ClientCategoryTagIndex must remain a client-only resolver")
require(
    "ClientModResources.findAll(relative)" in category_tag_index,
    "ClientCategoryTagIndex must use the loader-neutral installed-resource locator",
)
require(
    '"data/%s/tags/block/%s.json"' in category_tag_index,
    "ClientCategoryTagIndex must resolve the installed block-tag JSON layout",
)
require(
    re.search(r"\bFabricLoader\s*\.\s*getInstance\s*\(\s*\)\s*\.\s*getAllMods\s*\(\s*\)", client)
    is not None
    and re.search(r"\bmod\s*\.\s*findPath\s*\(", method_body(client, "findResources")) is not None,
    "Fabric must expose installed-mod resources through the loader-neutral locator",
)
tag_read_body = method_body(category_tag_index, "readValues")
for contract in (
    "JsonParser.parseReader",
    'object.has("replace")',
    "blocks.clear()",
    'object.getAsJsonArray("values")',
    "id.charAt(0) == '#'",
    "this.resolve(id.substring(1))",
):
    require(contract in tag_read_body, f"ClientCategoryTagIndex is missing tag-JSON resolution behavior: {contract}")
require(
    "this.resolving.add" in method_body(category_tag_index, "resolve")
    and "this.cache.put" in method_body(category_tag_index, "resolve"),
    "ClientCategoryTagIndex must cache resolved tags and break recursive tag cycles",
)

catalog_body = method_body(session, "catalog")
require(
    re.search(r"\bthis\s*\.\s*catalog\s*==\s*null", catalog_body) is not None
    and re.search(r"\bthis\s*\.\s*catalog\s*=\s*buildCatalog\s*\(\s*\)", catalog_body) is not None,
    "ConfigEditorSession must build the registry catalogue lazily on first use",
)
require(
    re.search(r"\bCatalog\s+catalog\s*;", session) is not None
    and re.search(r"\bCatalog\s+catalog\s*=\s*buildCatalog\s*\(", session) is None,
    "The block catalogue must not be initialized eagerly as a field",
)
constructor_start = session.find("public ConfigEditorSession(")
constructor_end = session.find("public Screen originalParent()", constructor_start)
require(constructor_start >= 0 and constructor_end > constructor_start, "Could not isolate ConfigEditorSession constructors")
constructor_region = session[constructor_start:constructor_end]
require(
    re.search(r"\b(?:catalog|buildCatalog)\s*\(", constructor_region) is None,
    "Creating the root General screen must not scan the block registry or category tag graph",
)
for eager_root_call in (
    "blockCatalog(",
    "blockInfo(",
    "categoryBlocks(",
    "categoryBlockCount(",
    "searchBlocks(",
    "searchFilterBlocks(",
    "searchFilterTags(",
    "runtimeTagIds(",
):
    require(eager_root_call not in root_screen, f"General screen must leave catalogue creation lazy: {eager_root_call}")

build_catalog_body = method_body(session, "buildCatalog")
require(
    re.search(r"\bClientCategoryTagIndex\s*\.\s*load\s*\(\s*\)", build_catalog_body) is not None,
    "ConfigEditorSession must use installed tag JSON as the title-screen category fallback",
)
require(
    "SmartDropTags.categoriesFor" in build_catalog_body
    and "declaredCategoryBlocks" in build_catalog_body
    and "resolvedCategories.add(category)" in build_catalog_body,
    "ConfigEditorSession must merge live category bindings with the installed-resource fallback",
)
require(
    len(re.findall(r"\bClientCategoryTagIndex\s*\.\s*load\s*\(\s*\)", session)) == 1,
    "Installed category tag JSON must be resolved only inside the one lazy catalogue build",
)

# Every child stays inside the same editor flow. The shared base owns root/back/session
# navigation; no child may recreate the route, request another snapshot, or invalidate
# the connection cache merely because the user navigated between editor screens.
require(re.search(r"\bSmartDropsConfigScreen\s+root\b", sub_screen) is not None, "Child screens must retain their root screen")
require(re.search(r"\bScreen\s+backScreen\b", sub_screen) is not None, "Child screens must retain their immediate back target")
require(re.search(r"\bConfigEditorSession\s+session\b", sub_screen) is not None, "Child screens must share the root editor session")
require(
    re.search(r"\bunsavedChangesIndicatorVisible\s*\(\s*\)", sub_screen) is not None
    and re.search(
        r"\breturn\s+this\s*\.\s*session\s*\.\s*isDirty\s*\(\s*\)\s*;",
        method_body(sub_screen, "unsavedChangesIndicatorVisible"),
    ) is not None,
    "Every child screen must derive its unsaved-changes indicator from the shared session",
)
require(
    'Component.translatable("smart_resource_drops.gui.unsaved_changes")' in sub_screen,
    "The shared child screen must render the localized unsaved-changes indicator",
)
require(
    "UnsavedChangesIndicatorLayout" in sub_screen
    and "usesStackedUnsavedChangesLayout" in sub_screen
    and "contentBottom()" in sub_screen,
    "Child screens must expose one collision-aware inline/compact indicator layout",
)
require(
    "smart_resource_drops.gui.unsaved_changes" in lang,
    "The shared unsaved-changes indicator must be localized",
)
require(
    re.search(r"\bsetScreen\s*\(\s*this\s*\.\s*backScreen\s*\)", method_body(sub_screen, "onClose")) is not None,
    "Child Back/Escape must return within the editor flow",
)
require("invalidateCachedSnapshot" not in sub_screen, "Child navigation must not invalidate the authoritative snapshot cache")
for name, source in children.items():
    require(
        re.search(r"\bextends\s+SmartDropsSubScreen\b", source) is not None,
        f"{name} must participate in the shared editor navigation flow",
    )
    require("ConfigEditorSession" in source, f"{name} must receive the shared editor session")
    for forbidden in (
        "SmartDropsConfigScreens.create",
        "ClientConfigState.request(",
        "ClientConfigState.invalidateCachedSnapshot",
        "BuiltInRegistries",
    ):
        require(forbidden not in source, f"{name} must not leave/reload the shared editor session: {forbidden}")
    require(
        "smart_resource_drops.gui.apply" not in source
        and re.search(r"\bbuildPatch\s*\(", source) is None,
        f"{name} must not expose a child-level Apply action",
    )
for name in ("RuleListScreen.java", "BlockOverridesScreen.java", "FilterConfigScreen.java", "AdvancedConfigScreen.java"):
    require("StructuredConfigList" in children[name], f"{name} must use the virtualized structured list")
require(
    re.search(r"\bsession\s*\.\s*searchBlocks\s*\(", children["BlockOverridesScreen.java"]) is not None,
    "BlockOverridesScreen must consume the session's cached block search",
)

# Large registries use Minecraft's virtualized selection list. Rows render and handle
# selection directly; they must never allocate a Button per block/category/dimension.
require(
    re.search(r"extends\s+ObjectSelectionList\s*<", structured_list) is not None,
    "StructuredConfigList must extend ObjectSelectionList",
)
require(re.search(r"\bextractContent\s*\(", structured_list) is not None, "Structured list rows must render through list-entry content extraction")
require(re.search(r"\bmouseClicked\s*\(", structured_list) is not None, "Structured list rows must handle selection without child Buttons")
for forbidden in ("import net.minecraft.client.gui.components.Button", "Button.builder(", "new Button("):
    require(forbidden not in structured_list, f"StructuredConfigList must not allocate a Button per row: {forbidden}")
require(
    "Tooltip.splitTooltip(StructuredConfigList.this.minecraft, tooltip)" in structured_list,
    "Structured list tooltips must use Minecraft's standard wrapped multi-line tooltip path",
)
require(
    "setTooltipForNextFrame(tooltip, mouseX, mouseY)" not in structured_list,
    "Structured list tooltips must not send newline-bearing Components to the single-line overload",
)

# Apply is staged, dirty-gated, and emits exactly one patch. Local/default authority
# persists that patch atomically; connected authority waits for the loading/ack bridge.
require(
    re.search(r"\bsession\s*\.\s*isDirty\s*\(\s*\)", root_screen) is not None,
    "Apply must consult the shared dirty state",
)
require(
    re.search(
        r"\b\w*apply\w*\s*\.\s*active\s*=\s*[^;]*session\s*\.\s*isDirty\s*\(\s*\)",
        root_screen,
        re.IGNORECASE,
    ) is not None,
    "Apply must be disabled while the shared session is clean",
)
require(
    len(re.findall(r"\bsession\s*\.\s*buildPatch\s*\(\s*\)", root_screen)) == 1,
    "The root must build exactly one bounded patch per Apply",
)
require(
    re.search(r"\bConfigManager\s*\.\s*applyLocalPatch\s*\(", root_screen) is not None,
    "Local/default Apply must use atomic patch persistence",
)
require("AtomicConfigWriter.write" in manager, "ConfigManager must persist accepted patches through the atomic writer")
require("StandardCopyOption.ATOMIC_MOVE" in atomic_writer, "Local/default saves must retain atomic replacement semantics")
require(
    re.search(r"\bnew\s+SmartDropsConfigLoadingScreen\s*\(", root_screen) is not None,
    "Connected Apply must wait for the loading/ack bridge",
)
require("ClientCommandQueue.flush();" not in root_screen, "Apply must not perform one command/write per edit")
require("ClientConfigState.submit" not in root_screen, "The root must submit through the loading lifecycle, not bypass it")

# A full reset is a separately confirmed, revision-guarded transaction. It must
# replace configuration only, never provenance attachments or accumulated statistics.
require(
    re.search(r"\brecord\s+ConfigResetPayload\s*\(\s*int\s+requestId\s*,\s*long\s+expectedRevision", reset_payload)
    is not None,
    "Reset must use a small dedicated request carrying the authoritative base revision",
)
require("writeVarLong(payload.expectedRevision())" in reset_payload, "Reset revision must be encoded on the wire")
require(
    "registerGlobalReceiver(ConfigResetPayload.TYPE" in fabric_networking
    and "SmartDropsNetworking.handleReset" in fabric_networking,
    "The server must register the dedicated reset receiver",
)
handle_reset_body = method_body(networking, "handleReset")
apply_reset_body = method_body(networking, "applyReset")
after_reset_body = method_body(networking, "afterAuthoritativeReset")
require(
    "canEditConfiguration(player)" in handle_reset_body,
    "The reset receiver must revalidate permission server-side at mutation time",
)
require(
    "ConfigManager.reset(payload.expectedRevision())" in apply_reset_body,
    "The server reset must reject requests based on stale authoritative snapshots",
)
require(
    "clearPendingPatches()" in apply_reset_body
    and "clearPendingPatches()" in after_reset_body,
    "A successful reset must invalidate every queued pre-reset patch",
)
require(
    "ConfigManager.setPublicationListener" in networking
    and "broadcastInvalidation" in networking
    and "PublicationKind.RESET" in networking,
    "A successful reset must invalidate other operators' open editor revisions",
)
require(
    "RESET_COOLDOWN_TICKS" in networking and "acceptReset(player)" in networking,
    "Destructive reset writes must have a short server-side rate limit",
)
require(
    "PENDING" not in method_body(networking, "acceptReset"),
    "Destructive resets must be rejected during cooldown, never queued",
)
require_before(
    apply_reset_body,
    "ConfigManager.reset(payload.expectedRevision())",
    "clearPendingPatches()",
    "Pending patches may be cleared only after the reset transaction succeeds",
)
require(
    handle_reset_body.count("sendMutationResult(") == 3,
    "Every unauthorized, stale, or cooldown reset rejection must use the compact response path",
)
require(
    "sendSnapshot(" not in handle_reset_body,
    "A rejected tiny reset packet must not bypass the bounded full-snapshot response path",
)
require(
    "sendSnapshot(" in apply_reset_body,
    "Every accepted reset must still return its authoritative acknowledgement",
)
require_before(
    commands,
    "if (!ConfigManager.reset())",
    "SmartDropsNetworking.afterAuthoritativeReset(context.getSource().getServer())",
    "A successful command/console reset must clear queued patches and invalidate open editors",
)
require(
    "ConfigManager.ClientSnapshot" in networking and "snapshot.revision()" in networking,
    "Authoritative snapshots must carry the configuration revision",
)
require(
    "payload.expectedRevision()" in method_body(networking, "applyPatch"),
    "Ordinary patches must participate in the same stale-revision guard as reset",
)
require(
    "record ConfigInvalidationPayload(long revision, ChangeKind changeKind)" in invalidation_payload
    and "ConfigInvalidationPayload.TYPE" in client,
    "Other connected clients must receive typed lightweight revision invalidation",
)
require(
    "ConfigMutationResultPayload.TYPE" in client
    and "record ConfigMutationResultPayload(" in mutation_result_payload,
    "Rejected mutations must receive a compact explicit response",
)
require(
    "REQUEST_COOLDOWN_TICKS = 40L" in networking
    and "sendMutationResult" in networking,
    "Config snapshot request amplification must remain rate-limited and mutation failures compact",
)
require(
    "visibleSession.isDirty()" in state
    and "server_changed_draft_kept" in state
    and "new SmartDropsConfigLoadingScreen(resultParent)" in state,
    "Revision invalidation must preserve dirty drafts and refresh clean editors",
)
require(
    "buildEntityCatalog(" in session
    and "ConfigScreenOpenPolicy.Authority.LOCAL_DEFAULTS" in session
    and "useLocalResourceFallback" in session
    and ": Map.of()" in session,
    "Connected entity catalogs must not merge local resource-tag fallbacks into live server tags",
)
require(
    "categoryEstimated" in session
    and "entity_category_estimated_tooltip" in entity_overrides
    and "entity_category_estimated_warning" in entity_rule_edit
    and "entity_category_estimated_tooltip" in entity_filter,
    "Metadata-only entity classifications must be presented as estimates with runtime-inspection guidance",
)
require(
    "ConfigManager.applyLocalPatch(patch, this.session.revision())" in root_screen,
    "Local/default Apply must use the revision from which its draft was staged",
)
require(
    "SCHEMA_TWO_ENTITY_FIELDS.forEach(decodedObject::remove)" in manager,
    "Schema-1 migration must discard coincidental schema-2 entity fields before Gson decoding",
)
require(
    "final SmartDropsConfig defaults = SmartDropsConfig.defaults()" in manager,
    "Reset must use the one authoritative default configuration factory",
)
reset_path_match = re.search(
    r"\breset\s*\(\s*final\s+Path\s+path\s*\)\s*\{(?P<body>.*?)\n\s*}\n\s*\n\s*public\s+static\s+synchronized\s+boolean\s+save",
    manager,
    re.DOTALL,
)
require(reset_path_match is not None, "Could not isolate the atomic ConfigManager.reset(Path) transaction")
reset_path_body = reset_path_match.group("body")
require(
    reset_path_body.count("AtomicConfigWriter.write") == 1,
    "A full reset must persist exactly once through the atomic writer",
)
require_before(
    reset_path_body,
    "AtomicConfigWriter.write",
    "publishConfig(defaults, PublicationKind.RESET)",
    "Reset must persist successfully before publishing defaults in memory",
)
require("void replaceWithDefaults" not in root_screen, "The GUI must not duplicate config defaults")
require(
    "this.resetButton.setTooltip" in root_screen
    and "smart_resource_drops.gui.reset_no_permission" in root_screen,
    "Read-only Reset All Settings must remain disabled with an explicit permission tooltip",
)
require(
    "ClientCommandQueue.clear()" in method_body(state, "invalidatePendingMutations"),
    "Confirmed reset must clear every delayed Smart Resource Multiplier client mutation/action",
)
require(
    "belongsToCurrentConnection" in session
    and root_screen.count("belongsToCurrentConnection") >= 2,
    "Apply and Reset must reject a screen retained from a different server connection",
)
require(
    "this.operation != Operation.RESET || this.state == State.ERROR"
    in method_body(loading, "shouldCloseOnEsc")
    and "new SmartDropsConfigLoadingScreen(this.resultParent)" in method_body(loading, "onClose"),
    "An in-flight reset must never navigate back to the stale pre-reset draft",
)
reset_related_source = "\n".join((manager, networking, state, root_screen, reset_confirmation))
require("PlacementTracker" not in reset_related_source, "Reset must never mutate player-placed provenance")
require("SmartDropsStats.reset" not in reset_related_source, "Reset must never erase accumulated statistics/history")
for copy_fragment in (
    "discard all current changes",
    "Player-placed block tracking will NOT be affected",
    "This action cannot be undone",
):
    require(
        any(copy_fragment in value for value in lang.values() if isinstance(value, str)),
        f"Reset confirmation is missing required safety copy: {copy_fragment}",
    )

exit_body = method_body(root_screen, "exitFlow")
invalidate_pattern = r"\bClientConfigState\s*\.\s*invalidateCachedSnapshot\s*\(\s*\)"
require(re.search(invalidate_pattern, exit_body) is not None, "Final root exit must invalidate connected cached permissions/values")
require(re.search(invalidate_pattern, root_screen) is not None, "The root must own cache invalidation")
require(
    len(re.findall(invalidate_pattern, root_screen)) == 1,
    "Cache invalidation must be confined to the root's final-exit path",
)
require(
    re.search(r"\bexitFlow\s*\(\s*\)", method_body(root_screen, "onClose")) is not None,
    "Root Escape must use the final-exit path",
)
if re.search(r"\bremoved\s*\(", root_screen) is not None:
    removed_body = method_body(root_screen, "removed")
    require(
        re.search(r"\b(?:exitFlow|invalidateCachedSnapshot)\s*\(", removed_body) is None,
        "Opening a child removes the root temporarily, so removed() must not exit/invalidate the editor flow",
    )

# Server-side authorization, validation, coalescing, and anti-amplification must not
# regress while the client UI is reorganized.
handle_patch_body = method_body(networking, "handlePatch")
require_before(
    handle_patch_body,
    "if (acceptOrQueuePatch(player, payload, editableAtReceipt))",
    "applyPatch(player, payload)",
    "Rate limiting must apply to authorized as well as unauthorized patch requests",
)
require(
    "PENDING_PATCHES.put" in networking and "flushPendingPatches" in networking,
    "Rate-limited valid patches must be queued instead of silently discarded",
)
require(
    "if (editableAtReceipt)" in networking and "PENDING_PATCHES.remove(player)" in networking,
    "Unauthorized packets must never be retained for later privilege changes",
)
require(
    networking.count("player.hasDisconnected() || player.isRemoved()") >= 2,
    "Disconnected players must lose pending snapshot requests and config patches",
)
require("isSingleplayerOwner" in networking, "The integrated-server owner must be editable without cheats")
require("hasValuesWithinBounds" in manager, "Server patches must reject out-of-range multipliers")
cooldown_start = handle_patch_body.find("if (acceptOrQueuePatch(player, payload, editableAtReceipt))")
apply_start = handle_patch_body.find("applyPatch(player, payload)", cooldown_start)
require(cooldown_start >= 0 and apply_start > cooldown_start, "Could not locate the patch cooldown branch")
require(
    "sendSnapshot" not in handle_patch_body[cooldown_start:apply_start],
    "A rate-limited tiny patch must not amplify into a full config snapshot response",
)

# Minecraft 26.2 interprets six-digit RGB literals as alpha=0. Check every editor
# rendering source so newly added child screens cannot silently reintroduce invisible text.
render_sources = {
    "loading screen": loading,
    "general screen": root_screen,
    "reset confirmation": reset_confirmation,
    "sub-screen base": sub_screen,
    "structured list": structured_list,
    **{name: source for name, source in children.items()},
}
for source_name, source in render_sources.items():
    colors = re.findall(r"0x[0-9A-Fa-f_]+", source)
    for color in colors:
        digits = color[2:].replace("_", "")
        require(
            len(digits) == 8,
            f"Every custom UI color in {source_name} must include an ARGB alpha byte: {color}",
        )
require("this.minecraft.level != null" in loading, "Title loading errors must use the menu background")
require("this.minecraft.level != null" in root_screen, "Title General config must use the menu background")
require("this.minecraft != null && this.minecraft.level != null" in sub_screen, "Child screens must preserve title/in-world backgrounds")

required_language_keys = (
    # Loading, authority, and status lifecycle.
    "modmenu.nameTranslation.smart_resource_drops",
    "modmenu.summaryTranslation.smart_resource_drops",
    "modmenu.descriptionTranslation.smart_resource_drops",
    "smart_resource_drops.gui.loading",
    "smart_resource_drops.gui.join_world",
    "smart_resource_drops.gui.sync_unavailable",
    "smart_resource_drops.gui.request_timeout",
    "smart_resource_drops.gui.patch_applied",
    "smart_resource_drops.gui.patch_rejected",
    "smart_resource_drops.gui.patch_unauthorized",
    "smart_resource_drops.gui.load_failed",
    "smart_resource_drops.gui.reason_no_connection",
    "smart_resource_drops.gui.reason_invalid_response",
    "smart_resource_drops.gui.local_authoritative",
    "smart_resource_drops.gui.local_applied",
    "smart_resource_drops.gui.apply_local",
    "smart_resource_drops.gui.local_rejected",
    "smart_resource_drops.gui.session_changed",
    "smart_resource_drops.gui.not_connected",
    "smart_resource_drops.gui.no_changes",
    "smart_resource_drops.gui.read_only",
    # Confirmed full-configuration reset lifecycle.
    "smart_resource_drops.gui.reset_all",
    "smart_resource_drops.gui.reset_all_tooltip",
    "smart_resource_drops.gui.reset_no_permission",
    "smart_resource_drops.gui.reset_confirm_title",
    "smart_resource_drops.gui.reset_confirm_body",
    "smart_resource_drops.gui.reset_everything",
    "smart_resource_drops.gui.reset_cancel",
    "smart_resource_drops.gui.reset_loading",
    "smart_resource_drops.gui.reset_loading_detail",
    "smart_resource_drops.gui.reset_server_applied",
    "smart_resource_drops.gui.reset_server_rejected",
    "smart_resource_drops.gui.reset_server_unauthorized",
    "smart_resource_drops.gui.reset_local_applied",
    "smart_resource_drops.gui.reset_local_rejected",
    # General controls and hierarchy.
    "smart_resource_drops.gui.back",
    "smart_resource_drops.gui.general",
    "smart_resource_drops.gui.configuration",
    "smart_resource_drops.gui.server_heading",
    "smart_resource_drops.gui.server_detail",
    "smart_resource_drops.gui.server_read_only_detail",
    "smart_resource_drops.gui.local_heading",
    "smart_resource_drops.gui.local_detail",
    "smart_resource_drops.gui.global_multiplier_tooltip",
    "smart_resource_drops.gui.placement_protection_label",
    "smart_resource_drops.gui.placement_protection_tooltip",
    "smart_resource_drops.gui.multiplier_source",
    "smart_resource_drops.gui.source_natural",
    "smart_resource_drops.gui.source_all",
    "smart_resource_drops.gui.source_placed",
    "smart_resource_drops.gui.source_natural_tooltip",
    "smart_resource_drops.gui.source_all_tooltip",
    "smart_resource_drops.gui.source_placed_tooltip",
    "smart_resource_drops.gui.multiply_xp",
    "smart_resource_drops.gui.multiply_xp_tooltip",
    "smart_resource_drops.gui.xp_multiplier",
    "smart_resource_drops.gui.xp_multiplier_tooltip",
    "smart_resource_drops.gui.on",
    "smart_resource_drops.gui.off",
    "smart_resource_drops.gui.inherit",
    "smart_resource_drops.gui.configured",
    "smart_resource_drops.gui.effective",
    "smart_resource_drops.gui.inherited_from",
    "smart_resource_drops.gui.configure",
    # Dedicated category/block/dimension/filter/advanced flows.
    "smart_resource_drops.gui.categories",
    "smart_resource_drops.gui.root_block_categories",
    "smart_resource_drops.gui.categories_search",
    "smart_resource_drops.gui.category_blocks",
    "smart_resource_drops.gui.view_category_blocks",
    "smart_resource_drops.gui.block_overrides",
    "smart_resource_drops.gui.blocks_search",
    "smart_resource_drops.gui.blocks_empty",
    "smart_resource_drops.gui.blocks_empty_help",
    "smart_resource_drops.gui.blocks_override_hint",
    "smart_resource_drops.gui.blocks_result_limit",
    "smart_resource_drops.gui.block_category",
    "smart_resource_drops.gui.dimensions",
    "smart_resource_drops.gui.root_dimensions_tooltip",
    "smart_resource_drops.gui.dimensions_search",
    "smart_resource_drops.gui.filters",
    "smart_resource_drops.gui.root_block_filters",
    "smart_resource_drops.gui.filters_search",
    "smart_resource_drops.gui.filter_mode_blacklist",
    "smart_resource_drops.gui.filter_mode_whitelist",
    "smart_resource_drops.gui.filter_blacklist_explanation",
    "smart_resource_drops.gui.filter_whitelist_explanation",
    "smart_resource_drops.gui.filter_blacklist_tooltip",
    "smart_resource_drops.gui.filter_whitelist_tooltip",
    "smart_resource_drops.gui.filter_add",
    "smart_resource_drops.gui.filter_remove",
    "smart_resource_drops.gui.filter_none",
    "smart_resource_drops.gui.filter_tag_read_only",
    "smart_resource_drops.gui.advanced",
    "smart_resource_drops.gui.root_advanced_tooltip",
    "smart_resource_drops.gui.presets",
    "smart_resource_drops.gui.preset_preview",
    "smart_resource_drops.gui.preset_warning",
    "smart_resource_drops.gui.stage_preset",
    "smart_resource_drops.gui.preset_vanilla_plus",
    "smart_resource_drops.gui.preset_vanilla_plus_summary",
    "smart_resource_drops.gui.preset_faster_survival",
    "smart_resource_drops.gui.preset_faster_survival_summary",
    "smart_resource_drops.gui.preset_fast_progression",
    "smart_resource_drops.gui.preset_fast_progression_summary",
    "smart_resource_drops.gui.reset_override",
    "smart_resource_drops.gui.enabled",
    "smart_resource_drops.gui.enabled_tooltip",
    "smart_resource_drops.gui.player_mining",
    "smart_resource_drops.gui.player_mining_tooltip",
    "smart_resource_drops.gui.explosions",
    "smart_resource_drops.gui.explosions_tooltip",
    "smart_resource_drops.gui.automated_mining",
    "smart_resource_drops.gui.automated_mining_tooltip",
    "smart_resource_drops.gui.protect_block_entities",
    "smart_resource_drops.gui.protect_block_entities_tooltip",
    "smart_resource_drops.gui.piston_safety",
    "smart_resource_drops.gui.piston_safety_tooltip",
    "smart_resource_drops.gui.player_overrides",
    "smart_resource_drops.gui.player_overrides_tooltip",
    "smart_resource_drops.gui.statistics",
    "smart_resource_drops.gui.statistics_tooltip",
    "smart_resource_drops.gui.root_entity_drops_tooltip",
    "smart_resource_drops.gui.read_only_value",
)
for key in required_language_keys:
    require(key in lang, f"Missing language key: {key}")
require(
    lang["smart_resource_drops.gui.reset_all_tooltip"]
    == "Restore all Smart Resource Multiplier settings to their defaults.",
    "Reset tooltip must describe configuration defaults without implying world-data deletion",
)
require(
    lang["smart_resource_drops.gui.reset_no_permission"]
    == "You do not have permission to modify server settings.",
    "Read-only Reset tooltip must explain the server permission requirement",
)
require(
    lang["smart_resource_drops.gui.entity_category_estimated_tooltip"]
    == "Estimated from registry metadata. Verify in-world with /smartdrops inspect entity.",
    "Estimated entity tooltips must stay concise and retain authoritative inspection guidance",
)
require(
    lang["smart_resource_drops.gui.entity_category_estimated_count"]
    == "%s estimated entries; verify in-world with /smartdrops inspect entity.",
    "Estimated category-count tooltips must stay concise",
)

all_client_ui = "\n".join((
    loading,
    state,
    root_screen,
    reset_confirmation,
    session,
    sub_screen,
    structured_list,
    *children.values(),
))
for key in set(re.findall(r'Component\s*\.\s*translatable\s*\(\s*"([^"]+)"', all_client_ui)):
    if key.startswith(("smart_resource_drops.", "modmenu.")):
        require(key in lang, f"Java UI source references a missing language key: {key}")

# Keep optional integration and every client GUI class out of the dedicated-server source set.
common_java = "\n".join(path.read_text(encoding="utf-8") for path in (ROOT / "src/main/java").rglob("*.java"))
require("com.terraformersmc.modmenu" not in common_java, "Mod Menu API leaked into common/server source")
require("net.minecraft.client" not in common_java, "Client GUI classes leaked into common/server source")

all_resources = "\n".join(
    path.read_text(encoding="utf-8")
    for path in (ROOT / "src/main/resources").rglob("*")
    if path.is_file() and path.suffix in {".json", ".mcmeta", ".txt"}
)
require("#minecraft:tall_flowers" not in all_resources, "Invalid Minecraft 26.2 tall_flower tag was reintroduced")

print("Hierarchical Mod Menu integration checks: PASS")
