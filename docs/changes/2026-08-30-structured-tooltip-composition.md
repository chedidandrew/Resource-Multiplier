# Structured tooltip composition follow-up

## Problem

Structured configuration rows already used Minecraft's standard 170-pixel tooltip splitter, but a row with both clipped visible text and a supplemental tooltip displayed only the supplemental text. On narrow windows, high GUI scales, long translations, or long modded registry IDs, the clipped row value therefore remained unavailable even after hovering.

## Cause

`StructuredConfigList.Entry` selected either the complete row text or the supplemental tooltip. A nonempty supplemental tooltip always won, even when one or more visible row fields had been shortened with an ellipsis.

## Change

The hover payload now composes both sources:

1. When any visible row field is clipped, its complete unabridged primary, secondary, left-detail, and right-detail values are added first.
2. Supplemental hover details are appended afterward on separate lines.
3. The combined component still passes through Minecraft's standard `Tooltip.splitTooltip` path, preserving the 170-pixel wrapping and control-character safeguards already covered by the real client GameTest.
4. Narration remains unchanged and continues to include the row fields plus supplemental details.

A focused source regression is run by both the ordinary build workflow and the guarded release workflow. It prevents the old either-or selection from returning and requires every clipped field plus supplemental text to participate in the composed hover payload.

## Scope

This is a client-only presentation correction. It does not change gameplay rules, configuration values, server authority, networking, persistence, compatibility identifiers, or release readiness.
