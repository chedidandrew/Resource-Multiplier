#!/usr/bin/env python3
"""Regression checks for structured-row tooltip composition.

The runtime client suite verifies Minecraft's actual 170-pixel tooltip wrapping.
This focused source audit prevents a truncated row's complete text from being
hidden whenever the row also has supplemental hover details.
"""
from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "src/client/java/com/chedidandrew/smartresourcedrops/client/StructuredConfigList.java"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


source = SOURCE.read_text(encoding="utf-8")

require(
    "row.tooltip().getString().isEmpty() ? fullRowText() : row.tooltip()" not in source,
    "Supplemental tooltips must not replace complete clipped row text",
)
require(
    "composeHoverText(row, truncated)" in source,
    "Structured rows must compose one hover payload from truncation and supplemental details",
)
require(
    "static Component composeHoverText(final Row row, final boolean truncated)" in source,
    "StructuredConfigList must retain an explicit hover-text composition helper",
)
for field in ("primary", "secondary", "leftDetail", "rightDetail"):
    require(
        f"appendTooltipPart(text, row.{field}())" in source,
        f"Truncated hover text must include the complete {field} field",
    )
require(
    "appendTooltipPart(text, uniqueSupplementalText(row))" in source,
    "Supplemental hover details must be de-duplicated after complete clipped fields",
)
require(
    ".filter(representedLines::add)" in source,
    "Supplemental tooltip lines must be compared against represented row lines",
)
require(
    "appendNarrationPart(text, uniqueSupplementalText(row))" in source,
    "Narration must use the same de-duplicated supplemental text",
)
require(
    'if (!tooltip.getString().isEmpty())' in source,
    "Rows must not schedule an empty tooltip after duplicate lines are removed",
)
require(
    'target.append(Component.literal("\\n"));' in source,
    "Tooltip sections must use explicit lines before vanilla wrapping",
)
require(
    "Tooltip.splitTooltip(" in source,
    "Composed hover text must still use Minecraft's standard tooltip splitter",
)

print("Structured tooltip composition checks: PASS")
