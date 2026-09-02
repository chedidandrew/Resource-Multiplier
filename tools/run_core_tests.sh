#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/.build/core-tests"
rm -rf "$OUT"
mkdir -p "$OUT"

javac --release 21 -Xlint:deprecation -d "$OUT" \
  "$ROOT/src/main/java/com/chedidandrew/smartresourcedrops/config/ConfigLoadDiagnostics.java" \
  "$ROOT/src/main/java/com/chedidandrew/smartresourcedrops/config/SmartDropsConfig.java" \
  "$ROOT/src/main/java/com/chedidandrew/smartresourcedrops/core/entity/EntityCategory.java" \
  "$ROOT/src/main/java/com/chedidandrew/smartresourcedrops/core/Category.java" \
  "$ROOT/src/main/java/com/chedidandrew/smartresourcedrops/core/DropSource.java" \
  "$ROOT/src/main/java/com/chedidandrew/smartresourcedrops/core/PackedBlockPosition.java" \
  "$ROOT/src/main/java/com/chedidandrew/smartresourcedrops/core/RuleResolutionTrace.java" \
  "$ROOT/src/main/java/com/chedidandrew/smartresourcedrops/core/RuleEngine.java" \
  "$ROOT/tools/core-tests/com/chedidandrew/smartresourcedrops/core/RuleEngineTest.java"

java -cp "$OUT" com.chedidandrew.smartresourcedrops.core.RuleEngineTest
