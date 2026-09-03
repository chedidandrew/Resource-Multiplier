#!/usr/bin/env python3
"""Deterministic policy and package regressions for Smart Resource Multiplier."""
from __future__ import annotations

import json
import random
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


@dataclass
class Cell:
    placed: bool = False
    block: str | None = None


def eligible(mode: str, placed: bool, protection: bool) -> bool:
    if mode == "ALL":
        return True
    if mode == "PLAYER_PLACED_ONLY":
        return placed
    if mode == "NATURAL_ONLY":
        return not (protection and placed)
    raise AssertionError(mode)


def policy_tests() -> None:
    assert eligible("NATURAL_ONLY", False, True)
    assert not eligible("NATURAL_ONLY", True, True)
    assert eligible("NATURAL_ONLY", True, False)
    assert eligible("ALL", False, True) and eligible("ALL", True, True)
    assert not eligible("PLAYER_PLACED_ONLY", False, True)
    assert eligible("PLAYER_PLACED_ONLY", True, True)

    # Stateful randomized provenance model covering placement, transforms,
    # pistons, falling blocks, canceled breaks, successful breaks and reuse.
    rng = random.Random(0x5A17D0)
    cells = {i: Cell(False, "stone") for i in range(64)}
    for _ in range(20_000):
        op = rng.randrange(7)
        a = rng.randrange(64)
        b = rng.randrange(64)
        if op == 0:
            cells[a] = Cell(True, "stone")
        elif op == 1 and cells[a].block:
            before = cells[a].placed
            cells[a].block = "transformed_stone"
            assert cells[a].placed == before
        elif op == 2 and cells[a].block and not cells[b].block:
            cells[b] = Cell(cells[a].placed, cells[a].block)
            cells[a] = Cell(False, None)
        elif op == 3 and cells[a].block and not cells[b].block:
            cells[b] = Cell(cells[a].placed, cells[a].block)
            cells[a] = Cell(False, None)
        elif op == 4 and cells[a].block:
            before = Cell(cells[a].placed, cells[a].block)
            assert cells[a] == before
        elif op == 5 and cells[a].block:
            was_placed = cells[a].placed
            assert eligible("NATURAL_ONLY", was_placed, True) == (not was_placed)
            cells[a] = Cell(False, None)
        elif op == 6 and not cells[a].block:
            cells[a] = Cell(False, "generated")


def source_tests() -> None:
    java = list(ROOT.rglob("*.java"))
    assert java, "No Java sources found"
    all_text = "\n".join(p.read_text(encoding="utf-8") for p in java)
    assert "StackConsolidator" in all_text
    assert "ClientCommandQueue" in all_text
    assert "RecentRemovalCache" in all_text
    assert "BlockItemPlacementCaptureMixin" in all_text
    assert "LevelPlacementCaptureMixin" in all_text
    assert "ShearingActionContext" in all_text
    assert "PlayerShearingContextMixin" in all_text
    assert "ShearsDispenseItemBehaviorMixin" in all_text
    assert "SheepShearingLootMixin" in all_text

    identifier_lines = "\n".join(
        line
        for line in all_text.splitlines()
        if any(name in line for name in ('"block"', '"dimension"', '"tag"', '"identifier"'))
    )
    assert "StringArgumentType.word()" not in identifier_lines

    for cfg in ROOT.rglob("*mixins*.json"):
        data = json.loads(cfg.read_text(encoding="utf-8"))
        if cfg.name == "smart_resource_drops.mixins.json":
            joined = " ".join(data.get("mixins", []))
            if "BlockItemPlacementCaptureMixin" in all_text:
                assert "BlockItemPlacementCaptureMixin" in joined
                assert "LevelPlacementCaptureMixin" in joined
            if "ShearingActionContext" in all_text:
                assert "PlayerShearingContextMixin" in joined
                assert "ShearsDispenseItemBehaviorMixin" in joined
                assert "SheepShearingLootMixin" in joined

    neoforge_mixins = json.loads((
        ROOT / "neoforge/src/main/resources/smart_resource_drops.neoforge.mixins.json"
    ).read_text(encoding="utf-8"))
    neoforge_joined = " ".join(neoforge_mixins.get("mixins", []))
    assert {
        "CommonHooksPlacementMixin",
        "NeoForgeShearsDispenseItemBehaviorMixin",
        "ServerPlayerGameModeMixin",
    }.issubset(neoforge_mixins.get("mixins", []))

    standard_shearing = json.loads((
        ROOT / "src/main/resources/data/smart_resource_drops/tags/entity_type/shearing/standard_resources.json"
    ).read_text(encoding="utf-8"))
    special_shearing = json.loads((
        ROOT / "src/main/resources/data/smart_resource_drops/tags/entity_type/shearing/special.json"
    ).read_text(encoding="utf-8"))
    assert standard_shearing == {"replace": False, "values": ["minecraft:sheep"]}
    assert set(special_shearing.get("values", [])) == {
        "minecraft:bogged",
        "minecraft:mooshroom",
        "minecraft:snow_golem",
    }
    assert special_shearing.get("replace") is False

    direct: list[str] = []
    client_root = ROOT / "src/client/java"
    if client_root.exists():
        for path in client_root.rglob("*.java"):
            if path.name == "ClientCommandQueue.java":
                continue
            for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
                if ".sendCommand(" in line or ".sendUnattendedCommand(" in line:
                    direct.append(f"{path}:{line_number}")
    assert not direct, "Uncoalesced GUI command sends: " + ", ".join(direct)


if __name__ == "__main__":
    policy_tests()
    source_tests()
    print("Smart Resource Multiplier polish regressions: PASS")
