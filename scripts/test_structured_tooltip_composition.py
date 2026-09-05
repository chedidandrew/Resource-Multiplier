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
    "hoverText(truncated)" in source,
    "Structured rows must compose one hover payload from truncation and supplemental details",
)
require(
    "private Component hoverText(final boolean truncated)" in source,
    "StructuredConfigList must retain an explicit hover-text composition helper",
)
for field in ("primary", "secondary", "leftDetail", "rightDetail"):
    require(
        f"appendUniquePart(text, seen, row.{field}(), \"\\n\")" in source,
        f"Truncated hover text must include the complete unique {field} field",
    )
    require(
        f"rememberPart(seen, row.{field}())" in source,
        f"Visible {field} text must suppress matching supplemental tooltip lines",
    )
require(
    'appendUniquePart(text, seen, row.tooltip(), "\\n")' in source,
    "Only unique supplemental hover details must follow complete clipped fields",
)
require(
    source.index("if (truncated)") < source.index('appendUniquePart(text, seen, row.tooltip(), "\\n")'),
    "Complete clipped fields must precede supplemental hover details",
)
require(
    "if (!seen.add(line))" in source,
    "Tooltip composition must discard repeated normalized lines",
)
require(
    'appendUniquePart(text, seen, row.tooltip(), ", ")' in source,
    "Narration must use the same unique-line composition policy",
)
require(
    "if (!tooltip.getString().isEmpty())" in source,
    "Empty supplemental tooltips must not render an empty tooltip box",
)
require(
    'target.append(Component.literal(separator));' in source,
    "Tooltip sections must use explicit lines before vanilla wrapping",
)
require(
    "Tooltip.splitTooltip(" in source,
    "Composed hover text must still use Minecraft's standard tooltip splitter",
)

print("Structured tooltip composition checks: PASS")
