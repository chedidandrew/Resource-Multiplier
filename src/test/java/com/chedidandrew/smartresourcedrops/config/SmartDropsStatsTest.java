package com.chedidandrew.smartresourcedrops.config;

import com.chedidandrew.smartresourcedrops.core.SmartDropsStats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SmartDropsStatsTest {
    @Test
    void fallbackStatisticsDescribeTheVanillaResultAndCountersSaturate(
            @TempDir final Path directory
    ) {
        final Path path = directory.resolve("smart_resource_drops.json");
        assertTrue(ConfigManager.load(path));
        assertTrue(ConfigManager.update(config -> config.statisticsEnabled = true, path));
        SmartDropsStats.reset();
        try {
            SmartDropsStats.recordBlockBudgetFallback(9L);
            SmartDropsStats.Snapshot fallback = SmartDropsStats.snapshot();
            assertEquals(1L, fallback.blocksEvaluated());
            assertEquals(0L, fallback.blocksMultiplied());
            assertEquals(9L, fallback.vanillaItems());
            assertEquals(0L, fallback.bonusItems());
            assertEquals(1L, fallback.blockBudgetFallbacks());

            SmartDropsStats.reset();
            SmartDropsStats.recordDrops(64, Long.MAX_VALUE);
            SmartDropsStats.recordDrops(64, Long.MAX_VALUE);
            SmartDropsStats.Snapshot saturated = SmartDropsStats.snapshot();
            assertEquals(Long.MAX_VALUE, saturated.vanillaItems());
            assertEquals(Long.MAX_VALUE, saturated.bonusItems());
            assertEquals(2L, saturated.blocksEvaluated());
            assertEquals(2L, saturated.blocksMultiplied());
        } finally {
            SmartDropsStats.reset();
            ConfigManager.update(config -> config.statisticsEnabled = false, path);
        }
    }
}
