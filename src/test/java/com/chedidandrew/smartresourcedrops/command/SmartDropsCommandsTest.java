package com.chedidandrew.smartresourcedrops.command;

import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import com.chedidandrew.smartresourcedrops.core.Category;
import com.chedidandrew.smartresourcedrops.core.DropSource;
import com.chedidandrew.smartresourcedrops.core.RuleEngine;
import com.chedidandrew.smartresourcedrops.core.RuleResolutionTrace;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SmartDropsCommandsTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void terminalListArgumentsParseNamespacedIdentifiers() {
        assertParses("blacklist add minecraft:diamond_ore", SmartDropsCommands.ListKind.BLACKLIST);
        assertParses("blacklist remove mod_name:block_name", SmartDropsCommands.ListKind.BLACKLIST);
    }

    @Test
    void terminalTagArgumentsParseOptionalHashMarker() {
        assertParses("tag-blacklist add #c:ores", SmartDropsCommands.ListKind.TAG_BLACKLIST);
        assertParses("tag-blacklist remove minecraft:logs", SmartDropsCommands.ListKind.TAG_BLACKLIST);
    }

    @Test
    void listNormalizationValidatesAndCanonicalizesIdentifiers() {
        assertEquals(
                "c:ores",
                SmartDropsCommands.normalizeListValue(" #C:ORES ", SmartDropsCommands.ListKind.TAG_BLACKLIST));
        assertEquals(
                "minecraft:stone",
                SmartDropsCommands.normalizeListValue(" Minecraft:Stone ", SmartDropsCommands.ListKind.BLACKLIST));
        assertNull(SmartDropsCommands.normalizeListValue("not valid", SmartDropsCommands.ListKind.BLACKLIST));
    }

    @Test
    void inspectCommandsParseForNormalCommandSources() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.register(SmartDropsCommands.buildRoot());

        assertParses(dispatcher, "smartdrops inspect");
        assertParses(dispatcher, "smartdrops inspect verbose");
        assertParses(dispatcher, "smartdrops inspect entity");
        assertParses(dispatcher, "smartdrops inspect entity verbose");
    }

    @Test
    void validationCommandsExistAtTheRootWithCompactAndVerboseExecutors() {
        var root = SmartDropsCommands.buildRoot().build();
        var validate = root.getChild("validate");

        assertNotNull(validate);
        assertNotNull(validate.getCommand());
        assertNotNull(validate.getChild("verbose"));
        assertNotNull(validate.getChild("verbose").getCommand());
    }

    @Test
    void shearingCommandsUseOneNamespacedEntityArgumentAndExpectedHierarchy() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.register(SmartDropsCommands.buildRoot());

        assertParses(dispatcher, "smartdrops shearing status");

        var root = SmartDropsCommands.buildRoot().build();
        var shearing = root.getChild("shearing");
        var adminShearing = root.getChild("admin").getChild("shearing");
        assertNotNull(shearing);
        assertNotNull(shearing.getChild("status").getCommand());
        assertNotNull(adminShearing);
        var entityId = adminShearing.getChild("entity").getChild("entity_id");
        assertNotNull(entityId);
        assertNotNull(entityId.getChild("inherit").getCommand());
        assertNotNull(entityId.getChild("multiplier").getCommand());
    }

    @Test
    void verboseFormatterRendersMappedBlockState() {
        BlockState state = Blocks.OAK_LOG.defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.X);
        RuleResolutionTrace trace = trace("minecraft:oak_log", false, Category.LOGS);

        assertEquals("axis=x", BlockInspectionFormatter.describeState(state));

        String rendered = rendered(BlockInspectionFormatter.format(
                state,
                new BlockPos(12, 70, -4),
                trace,
                true));
        assertTrue(rendered.contains("State: axis=x"), rendered);
        assertTrue(rendered.contains("Position: 12, 70, -4"), rendered);
        assertTrue(rendered.contains("Matched categories: Logs"), rendered);
        assertTrue(rendered.contains(
                "Output safety budget: Enforced at drop time (262144 items / 4096 stacks)"), rendered);
    }

    @Test
    void formatterBoundsLongIdsAndNeverRendersBlockEntityNbt() {
        String longId = "example:" + "very_long_modded_block_path_".repeat(8);
        RuleResolutionTrace longIdTrace = trace(longId, false, Category.MISCELLANEOUS);
        List<Component> longIdLines = BlockInspectionFormatter.format(
                Blocks.STONE.defaultBlockState(),
                BlockPos.ZERO,
                longIdTrace,
                true);

        Component idLine = longIdLines.stream()
                .filter(line -> line.getString().startsWith("  ID: "))
                .findFirst()
                .orElseThrow();
        Component idValue = idLine.getSiblings().getLast();
        assertTrue(idValue.getString().length() <= 96, idValue.getString());
        assertTrue(idValue.getString().endsWith("…"), idValue.getString());
        assertFalse(idValue.getString().contains(longId));
        ClickEvent copyEvent = idValue.getStyle().getClickEvent();
        assertNotNull(copyEvent);
        assertEquals(ClickEvent.Action.COPY_TO_CLIPBOARD, copyEvent.action());
        assertInstanceOf(ClickEvent.CopyToClipboard.class, copyEvent);
        assertEquals(longId, ((ClickEvent.CopyToClipboard) copyEvent).value());

        assertEquals("abcd", BlockInspectionFormatter.truncate("abcd", 4));
        assertEquals("abc…", BlockInspectionFormatter.truncate("abcdef", 4));
        String boundedState = BlockInspectionFormatter.truncate("s".repeat(200), 160);
        assertEquals(160, boundedState.length());
        assertTrue(boundedState.endsWith("…"));

        RuleResolutionTrace chestTrace = trace("minecraft:chest", true, Category.MISCELLANEOUS);
        String chestOutput = rendered(BlockInspectionFormatter.format(
                Blocks.CHEST.defaultBlockState(),
                BlockPos.ZERO,
                chestTrace,
                true));
        assertTrue(chestOutput.contains("Block entity: Yes"), chestOutput);
        assertFalse(chestOutput.contains("Items"), chestOutput);
        assertFalse(chestOutput.contains("LootTable"), chestOutput);
        assertFalse(chestOutput.contains("CustomName"), chestOutput);
    }

    private static void assertParses(String command, SmartDropsCommands.ListKind kind) {
        String literal = command.substring(0, command.indexOf(' '));
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.register(SmartDropsCommands.buildListCommand(literal, kind));

        ParseResults<CommandSourceStack> result = dispatcher.parse(command, (CommandSourceStack) null);

        assertTrue(result.getExceptions().isEmpty(), () -> "Parse failures: " + result.getExceptions());
        assertEquals("", result.getReader().getRemaining());
    }

    private static void assertParses(
            CommandDispatcher<CommandSourceStack> dispatcher,
            String command
    ) {
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, (CommandSourceStack) null);

        assertTrue(result.getExceptions().isEmpty(), () -> "Parse failures: " + result.getExceptions());
        assertEquals("", result.getReader().getRemaining());
        assertNotNull(result.getContext().getCommand(), "Parsed command was not executable");
    }

    private static RuleResolutionTrace trace(
            String blockId,
            boolean hasBlockEntity,
            Category category
    ) {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.blacklist.clear();
        return RuleEngine.trace(config, new RuleEngine.RuleInput(
                blockId,
                "minecraft:overworld",
                new LinkedHashSet<>(List.of(category)),
                false,
                hasBlockEntity,
                Set.of(),
                DropSource.PLAYER,
                null));
    }

    private static String rendered(List<Component> lines) {
        return String.join("\n", lines.stream().map(Component::getString).toList());
    }
}
