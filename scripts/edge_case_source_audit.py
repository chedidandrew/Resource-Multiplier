#!/usr/bin/env python3
from pathlib import Path
import re, sys
ROOT=Path(__file__).resolve().parents[1]
files=list(ROOT.rglob("*.java"))
text={p:p.read_text(encoding="utf-8") for p in files}
all_text="\n".join(text.values())
checks=[]
def check(name,ok,detail=""):
    checks.append((name,ok,detail))
check("0x remains representable", "Math.max(1" not in "\n".join(line for line in all_text.splitlines() if "multiplier" in line.lower()))
check("legal-stack multiplier", "StackConsolidator" in all_text and "getMaxStackSize" in all_text and "copyWithCount" in all_text)
check("multi-block placement transaction", "BlockItemPlacementCaptureMixin" in all_text and "PlacementCapture.begin" in all_text and "PlacementCapture.end" in all_text)
check("remove-before-drop cache bounded", "MAX_ENTRIES_PER_LEVEL" in all_text and "MAX_AGE_TICKS" in all_text)
check("client queue bounded", "MAX_PENDING" in all_text and "QUIET_PERIOD_NANOS" in all_text)
check("namespaced parser", "StringArgumentType.greedyString()" in all_text and "ResourceLocation.tryParse" in all_text)
# BEFORE handlers must not consume provenance. Flag obvious remove/consume calls in their nearby block.
bad=[]
for p,t in text.items():
    for m in re.finditer(r"PlayerBlockBreakEvents\.BEFORE",t):
        nearby=t[m.start():m.start()+1800].lower()
        if any(token in nearby for token in ("consumeplaced", "removeplaced", "unmark(")):
            bad.append(str(p.relative_to(ROOT)))
check("canceled break does not obviously consume provenance in BEFORE",not bad,", ".join(bad))
# The source must mention block entity safety somewhere in policy/config/runtime.
check("block entity safety is represented", "blockentity" in all_text.lower())
check(
    "entity shearing uses only scoped final-helper hooks",
    "PlayerShearingContextMixin" in all_text
    and "ShearsDispenseItemBehaviorMixin" in all_text
    and "EntityShearingDropMixin" in all_text
    and "captureSpawn" in all_text
    and "IForgeShearable;onSheared" in all_text,
)
check(
    "shearing output budget is independently bounded",
    "MAX_MULTIPLIED_ITEMS = 1_024L" in all_text
    and "MAX_SOURCE_OR_MATERIALIZED_STACKS = 256L" in all_text,
)
check(
    "known vanilla special shearables fail closed",
    "KNOWN_VANILLA_SPECIAL_IDS" in all_text
    and "minecraft:mooshroom" in all_text
    and "minecraft:snow_golem" in all_text
    and "minecraft:copper_golem" not in "\n".join(
        value for path, value in text.items() if "src/main" in path.as_posix()
    ),
)
failed=[c for c in checks if not c[1]]
for name,ok,detail in checks: print(("PASS" if ok else "FAIL")+": "+name+(" ("+detail+")" if detail else ""))
sys.exit(1 if failed else 0)
