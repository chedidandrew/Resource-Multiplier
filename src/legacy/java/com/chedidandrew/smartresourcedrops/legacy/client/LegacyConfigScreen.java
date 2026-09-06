package com.chedidandrew.smartresourcedrops.legacy.client;

import com.chedidandrew.smartresourcedrops.legacy.LegacyConfig;
import com.chedidandrew.smartresourcedrops.legacy.LegacyNetwork;
import com.chedidandrew.smartresourcedrops.legacy.SmartResourceMultiplier;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.TextFormatting;

/** Compact server-authoritative menu designed for the smaller legacy GUI scale. */
public final class LegacyConfigScreen extends GuiScreen {
    private static final int PAGE_MAIN = 0;
    private static final int PAGE_ENTITY = 1;
    private static final int PAGE_ADVANCED = 2;
    private static final int PAGE_CATEGORIES = 3;
    private static final int PAGE_ABOUT = 4;
    private static final String[] CATEGORIES = {
            "ores", "raw_resource_blocks", "logs", "stone", "soil", "nether",
            "end", "crops", "plants", "leaves", "building_blocks", "miscellaneous"
    };

    private final GuiScreen parent;
    private final boolean localDefaults;
    private final Map<Integer, List<String>> tooltips = new HashMap<Integer, List<String>>();
    private LegacyConfig draft = LegacyConfig.defaults();
    private boolean loading = true;
    private boolean editable;
    private int page = PAGE_MAIN;
    private int categoryPage;
    private String status = "Loading server settings...";
    private String pendingUrl;
    private int pendingUrlId;

    LegacyConfigScreen(GuiScreen parent) {
        this(parent, false);
    }

    private LegacyConfigScreen(GuiScreen parent, boolean localDefaults) {
        this.parent = parent;
        this.localDefaults = localDefaults;
        if (localDefaults) {
            draft = SmartResourceMultiplier.config().copy();
            loading = false;
            editable = true;
            status = TextFormatting.GREEN + "Local defaults loaded.";
        }
    }

    static LegacyConfigScreen localDefaults(GuiScreen parent) {
        return new LegacyConfigScreen(parent, true);
    }

    boolean isReadyForLocalEditing() {
        return localDefaults && !loading && editable;
    }

    void acceptServer(LegacyNetwork.ConfigMessage message) {
        try {
            draft = LegacyConfig.fromJson(message.json);
            editable = message.editable;
            loading = false;
            status = message.action == LegacyNetwork.REJECTED
                    ? TextFormatting.RED + "Changes were rejected; server values restored."
                    : editable ? TextFormatting.GREEN + "Server settings loaded."
                               : TextFormatting.YELLOW + "Read-only: operator permission required.";
        } catch (RuntimeException exception) {
            loading = false;
            editable = false;
            status = TextFormatting.RED + "The server returned an invalid configuration.";
        }
        initGui();
    }

    @Override
    public void initGui() {
        buttonList.clear();
        tooltips.clear();
        int left = Math.max(12, width / 2 - 155);
        int contentWidth = Math.min(310, width - 24);
        // ScaledResolution can be as short as 240 px. Keep the action row at
        // the conventional bottom margin and compact vertical gaps so no
        // configuration row is covered at that supported minimum.
        int bottom = actionRowY(height);

        if (page == PAGE_MAIN) initMain(left, contentWidth);
        else if (page == PAGE_ENTITY) initEntity(left, contentWidth);
        else if (page == PAGE_ADVANCED) initAdvanced(left, contentWidth);
        else if (page == PAGE_CATEGORIES) initCategories(left, contentWidth);
        else initAbout(left, contentWidth);

        if (page != PAGE_MAIN) add(900, left, bottom, contentWidth, 20, "Back", null);
        else {
            GuiButton apply = add(901, left, bottom, contentWidth / 2 - 2, 20, "Apply Changes",
                    "Validate and save these settings on the server.");
            apply.enabled = !loading && editable;
            add(902, left + contentWidth / 2 + 2, bottom, contentWidth / 2 - 2, 20, "Done", null);
        }
        for (GuiButton button : buttonList) {
            if (button.id < 900) button.enabled = !loading && (editable || isNavigation(button.id));
        }
    }

    private void initMain(int left, int widthValue) {
        int y = 50;
        addStepper(10, left, y, widthValue, "Global Multiplier", draft.globalMultiplier,
                "Default multiplier when no block, category, or dimension override exists.");
        y += gap(25);
        add(20, left, y, widthValue, 20, "Smart Placement Protection: " + onOff(draft.smartPlacementProtection),
                "Natural Blocks Only excludes blocks that were previously placed by a player.");
        y += gap(23);
        add(21, left, y, widthValue, 20, "Multiplier Source: " + sourceName(draft.sourceMode),
                "Choose natural blocks, every block, or only known player-placed blocks.");
        y += gap(23);
        add(22, left, y, widthValue, 20, "Multiply Block XP: " + onOff(draft.multiplyExperience),
                "Only XP from eligible block breaks. Mob XP is configured under Entity Drops.");
        y += gap(25);
        addStepper(30, left, y, widthValue, "Block XP Multiplier", draft.experienceMultiplier,
                "Multiplier used for eligible block-break experience only.");
        y += gap(27);
        int half = widthValue / 2 - 2;
        add(100, left, y, half, 20, "Block Categories", "Configure the built-in block-category multipliers.");
        add(101, left + half + 4, y, half, 20, "Entity Drops", "Entity loot, mob XP, bosses, and shearing.");
        y += gap(23);
        add(102, left, y, half, 20, "Advanced", "Mining sources, block entities, and safety controls.");
        add(103, left + half + 4, y, half, 20, "About & Support", "Version information and optional support links.");
    }

    private void initEntity(int left, int widthValue) {
        int y = 48;
        add(200, left, y, widthValue, 20, "Entity Drops: " + onOff(draft.entityDropsEnabled),
                "Multiply final standard death-loot items from qualifying living entities.");
        y += gap(23);
        addStepper(210, left, y, widthValue, "Default Entity Multiplier", draft.defaultEntityMultiplier,
                "Used when no exact entity or entity-category override exists.");
        y += gap(25);
        add(220, left, y, widthValue, 20, "Kill Requirement: " + killName(draft.entityKillRequirement),
                "Controls which deaths qualify for entity items and mob XP.");
        y += gap(23);
        add(221, left, y, widthValue / 2 - 2, 20, "Boss Drops: " + onOff(draft.bossDropsEnabled),
                "Explicitly allow ordinary boss death-loot multiplication.");
        add(222, left + widthValue / 2 + 2, y, widthValue / 2 - 2, 20,
                "Boss XP: " + onOff(draft.multiplyBossExperience),
                "Explicitly allow qualifying boss experience multiplication.");
        y += gap(23);
        add(223, left, y, widthValue, 20, "Multiply Mob XP: " + onOff(draft.multiplyMobExperience),
                "Mob XP is separate from the Block XP option on the main page.");
        y += gap(25);
        addStepper(230, left, y, widthValue, "Mob XP Multiplier", draft.mobExperienceMultiplier,
                "Multiplier for qualifying living-entity death XP.");
        y += gap(25);
        add(240, left, y, widthValue, 20, "Manual Shearing: " + onOff(draft.manualShearingDropsEnabled),
                "Multiply supported player shearing results independently of death loot.");
        y += gap(25);
        addStepper(250, left, y, widthValue, "Shearing Multiplier", draft.defaultShearingMultiplier,
                "Default supported shearing multiplier. Exact JSON overrides still take priority.");
    }

    private void initAdvanced(int left, int widthValue) {
        int y = 48;
        add(300, left, y, widthValue, 20, "Mod Enabled: " + onOff(draft.enabled),
                "Master switch for every multiplier.");
        y += gap(23);
        add(301, left, y, widthValue, 20, "Player Mining: " + onOff(draft.playerMining),
                "Allow eligible player-mined blocks to be multiplied.");
        y += gap(23);
        add(302, left, y, widthValue, 20, "Explosion Drops: " + onOff(draft.explosions),
                "Allow eligible explosion-produced block drops to be multiplied.");
        y += gap(23);
        add(303, left, y, widthValue, 20, "Automated Mining: " + onOff(draft.automatedMining),
                "Allow eligible non-player block harvesting from compatible automation.");
        y += gap(23);
        add(304, left, y, widthValue, 20, "Protect Block Entities: " + onOff(draft.protectBlockEntities),
                "Containers and other block entities stay at vanilla output unless allowlisted.");
        y += gap(23);
        add(305, left, y, widthValue, 20, "Conservative Piston Safety: "
                + onOff(draft.conservativePistonProtection),
                "Preserve placed-block protection conservatively around piston movement.");
        y += gap(26);
        int third = (widthValue - 8) / 3;
        add(310, left, y, third, 20, "Vanilla Plus", "Global 1x with ores and logs at 2x.");
        add(311, left + third + 4, y, third, 20, "Faster", "Balanced 2x survival preset.");
        add(312, left + (third + 4) * 2, y, third, 20, "Fast", "Global 4x progression preset.");
        y += gap(25);
        add(313, left, y, widthValue, 20, TextFormatting.RED + "Reset All Settings",
                "Restore safe defaults. Persistent placed-block history is not erased.");
    }

    private void initCategories(int left, int widthValue) {
        int start = categoryPage * 6;
        int y = 48;
        for (int index = start; index < Math.min(start + 6, CATEGORIES.length); index++) {
            String category = CATEGORIES[index];
            Integer value = draft.categoryMultipliers.get(category);
            add(400 + index, left, y, widthValue, 20,
                    title(category) + ": " + (value == null ? "Inherit" : value + "x"),
                    "Click to cycle Inherit, 0x, 1x, 2x, 4x, 8x, 16x, 32x, and 64x.");
            y += gap(23);
        }
        add(450, left, y + 2, widthValue, 20, categoryPage == 0 ? "Next Categories >" : "< Previous Categories",
                null);
    }

    private void initAbout(int left, int widthValue) {
        int y = 72;
        add(500, left, y, widthValue, 20, "CurseForge Project", "Open the Smart Resource Multiplier project page.");
        y += gap(25);
        add(501, left, y, widthValue, 20, "Support on Ko-fi", "Optional donation link.");
        y += gap(23);
        add(502, left, y, widthValue, 20, "Support with PayPal", "Optional donation link.");
        y += gap(23);
        add(503, left, y, widthValue, 20, "Support with Cash App", "Optional donation link.");
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) return;
        int id = button.id;
        if (id == 900) { page = PAGE_MAIN; initGui(); return; }
        if (id == 901) {
            if (localDefaults) {
                if (SmartResourceMultiplier.replaceConfig(draft.copy())) {
                    draft = SmartResourceMultiplier.config().copy();
                    status = TextFormatting.GREEN + "Local defaults saved for future worlds.";
                } else {
                    status = TextFormatting.RED + "Could not save local defaults.";
                }
                initGui();
                return;
            }
            status = TextFormatting.YELLOW + "Saving on server...";
            LegacyNetwork.CHANNEL.sendToServer(new LegacyNetwork.ConfigMessage(
                    LegacyNetwork.APPLY, true, draft.toJson()));
            return;
        }
        if (id == 902) { mc.displayGuiScreen(parent); return; }
        if (id == 100) { page = PAGE_CATEGORIES; initGui(); return; }
        if (id == 101) { page = PAGE_ENTITY; initGui(); return; }
        if (id == 102) { page = PAGE_ADVANCED; initGui(); return; }
        if (id == 103) { page = PAGE_ABOUT; initGui(); return; }

        if (id == 10) draft.globalMultiplier = down(draft.globalMultiplier, 0);
        else if (id == 11) draft.globalMultiplier = up(draft.globalMultiplier);
        else if (id == 20) draft.smartPlacementProtection = !draft.smartPlacementProtection;
        else if (id == 21) draft.sourceMode = next(draft.sourceMode);
        else if (id == 22) draft.multiplyExperience = !draft.multiplyExperience;
        else if (id == 30) draft.experienceMultiplier = down(draft.experienceMultiplier, 1);
        else if (id == 31) draft.experienceMultiplier = up(draft.experienceMultiplier);
        else if (id == 200) draft.entityDropsEnabled = !draft.entityDropsEnabled;
        else if (id == 210) draft.defaultEntityMultiplier = down(draft.defaultEntityMultiplier, 0);
        else if (id == 211) draft.defaultEntityMultiplier = up(draft.defaultEntityMultiplier);
        else if (id == 220) draft.entityKillRequirement = next(draft.entityKillRequirement);
        else if (id == 221) draft.bossDropsEnabled = !draft.bossDropsEnabled;
        else if (id == 222) draft.multiplyBossExperience = !draft.multiplyBossExperience;
        else if (id == 223) draft.multiplyMobExperience = !draft.multiplyMobExperience;
        else if (id == 230) draft.mobExperienceMultiplier = down(draft.mobExperienceMultiplier, 1);
        else if (id == 231) draft.mobExperienceMultiplier = up(draft.mobExperienceMultiplier);
        else if (id == 240) draft.manualShearingDropsEnabled = !draft.manualShearingDropsEnabled;
        else if (id == 250) draft.defaultShearingMultiplier = down(draft.defaultShearingMultiplier, 0);
        else if (id == 251) draft.defaultShearingMultiplier = up(draft.defaultShearingMultiplier);
        else if (id == 300) draft.enabled = !draft.enabled;
        else if (id == 301) draft.playerMining = !draft.playerMining;
        else if (id == 302) draft.explosions = !draft.explosions;
        else if (id == 303) draft.automatedMining = !draft.automatedMining;
        else if (id == 304) draft.protectBlockEntities = !draft.protectBlockEntities;
        else if (id == 305) draft.conservativePistonProtection = !draft.conservativePistonProtection;
        else if (id == 310) draft.applyPreset(LegacyConfig.Preset.VANILLA_PLUS);
        else if (id == 311) draft.applyPreset(LegacyConfig.Preset.FASTER_SURVIVAL);
        else if (id == 312) draft.applyPreset(LegacyConfig.Preset.FAST_PROGRESSION);
        else if (id == 313) draft = LegacyConfig.defaults();
        else if (id >= 400 && id < 412) cycleCategory(CATEGORIES[id - 400]);
        else if (id == 450) { categoryPage = categoryPage == 0 ? 1 : 0; }
        else if (id >= 500 && id <= 503) openLink(id);
        initGui();
    }

    private void openLink(int id) {
        if (id == 500) pendingUrl = "https://www.curseforge.com/minecraft/mc-mods/resource-multiplier";
        else if (id == 501) pendingUrl = "https://ko-fi.com/andrewchedid";
        else if (id == 502) pendingUrl = "https://www.paypal.com/paypalme/chedidandrew";
        else pendingUrl = "https://cash.app/%24AndrewChedid";
        pendingUrlId = id;
        mc.displayGuiScreen(new GuiConfirmOpenLink(this, pendingUrl, id, true));
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if (result && pendingUrl != null && id == pendingUrlId) {
            try {
                Class<?> desktop = Class.forName("java.awt.Desktop");
                Object instance = desktop.getMethod("getDesktop").invoke(null);
                desktop.getMethod("browse", URI.class).invoke(instance, new URI(pendingUrl));
            } catch (Exception ignored) {
                status = TextFormatting.RED + "Could not open the browser.";
            }
        }
        pendingUrl = null;
        mc.displayGuiScreen(this);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, "Smart Resource Multiplier", width / 2, 12, 0xFFFFFF);
        drawCenteredString(fontRenderer, pageTitle(), width / 2, 26, 0xDDDDDD);
        drawCenteredString(fontRenderer, status, width / 2, 38, 0xAAAAAA);
        if (page == PAGE_ABOUT) {
            drawCenteredString(fontRenderer, "Forge 1.12.2 • " + SmartResourceMultiplier.VERSION,
                    width / 2, 52, 0xAAAAAA);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        for (GuiButton button : buttonList) {
            if (button.visible && button.isMouseOver() && tooltips.containsKey(Integer.valueOf(button.id))) {
                drawHoveringText(tooltips.get(Integer.valueOf(button.id)), mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private GuiButton add(int id, int x, int y, int w, int h, String label, String tooltip) {
        GuiButton button = new GuiButton(id, x, y, w, h, label);
        buttonList.add(button);
        if (tooltip != null) tooltips.put(Integer.valueOf(id), fontRenderer.listFormattedStringToWidth(tooltip, 250));
        return button;
    }

    private void addStepper(int id, int left, int y, int widthValue, String label, int value, String tooltip) {
        int buttonWidth = 24;
        int valueArea = 58;
        int labelWidth = widthValue - valueArea - buttonWidth * 2;
        add(id, left + labelWidth, y, buttonWidth, 20, "-", tooltip);
        add(id + 1, left + widthValue - buttonWidth, y, buttonWidth, 20, "+", tooltip);
        // The value is part of the center label, so it remains centered between minus and plus at every GUI scale.
        add(id + 1000, left, y, labelWidth, 20, label, tooltip).enabled = false;
        add(id + 2000, left + labelWidth + buttonWidth, y, valueArea, 20, value + "x", tooltip).enabled = false;
    }

    private static boolean isNavigation(int id) {
        return id == 100 || id == 101 || id == 102 || id == 103 || id == 450 || id >= 500;
    }

    static int actionRowY(int screenHeight) {
        return screenHeight - 24;
    }

    static int verticalGap(int screenHeight, int normal) {
        return screenHeight < 270 ? 20 : normal;
    }

    private int gap(int normal) {
        return verticalGap(height, normal);
    }

    private int up(int value) { return Math.min(draft.maximumMultiplier, value + 1); }
    private static int down(int value, int minimum) { return Math.max(minimum, value - 1); }
    private static String onOff(boolean value) { return value ? "ON" : "OFF"; }

    private static String sourceName(LegacyConfig.SourceMode mode) {
        if (mode == LegacyConfig.SourceMode.ALL) return "All Blocks";
        if (mode == LegacyConfig.SourceMode.PLAYER_PLACED_ONLY) return "Player-Placed Blocks Only";
        return "Natural Blocks Only";
    }

    private static LegacyConfig.SourceMode next(LegacyConfig.SourceMode mode) {
        if (mode == LegacyConfig.SourceMode.NATURAL_ONLY) return LegacyConfig.SourceMode.ALL;
        if (mode == LegacyConfig.SourceMode.ALL) return LegacyConfig.SourceMode.PLAYER_PLACED_ONLY;
        return LegacyConfig.SourceMode.NATURAL_ONLY;
    }

    private static String killName(LegacyConfig.EntityKillRequirement mode) {
        if (mode == LegacyConfig.EntityKillRequirement.ALL_STANDARD_DEATH_LOOT) return "All Standard Death Loot";
        if (mode == LegacyConfig.EntityKillRequirement.PLAYER_OR_TAMED_ENTITY) return "Player or Tamed Entity";
        return "Player Kills Only";
    }

    private static LegacyConfig.EntityKillRequirement next(LegacyConfig.EntityKillRequirement mode) {
        if (mode == LegacyConfig.EntityKillRequirement.PLAYER_KILLS_ONLY)
            return LegacyConfig.EntityKillRequirement.PLAYER_OR_TAMED_ENTITY;
        if (mode == LegacyConfig.EntityKillRequirement.PLAYER_OR_TAMED_ENTITY)
            return LegacyConfig.EntityKillRequirement.ALL_STANDARD_DEATH_LOOT;
        return LegacyConfig.EntityKillRequirement.PLAYER_KILLS_ONLY;
    }

    private void cycleCategory(String category) {
        int[] values = {-1, 0, 1, 2, 4, 8, 16, 32, 64};
        Integer current = draft.categoryMultipliers.get(category);
        int found = 0;
        for (int index = 0; index < values.length; index++) {
            if ((current == null && values[index] == -1) || (current != null && current.intValue() == values[index])) {
                found = index;
                break;
            }
        }
        int next = values[(found + 1) % values.length];
        if (next < 0) draft.categoryMultipliers.remove(category);
        else draft.categoryMultipliers.put(category, Integer.valueOf(next));
    }

    private String pageTitle() {
        if (page == PAGE_ENTITY) return "Entity and Mob Drops";
        if (page == PAGE_ADVANCED) return "Advanced";
        if (page == PAGE_CATEGORIES) return "Block Categories";
        if (page == PAGE_ABOUT) return "About & Support";
        return "General";
    }

    private static String title(String value) {
        String[] words = value.replace('_', ' ').split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.length() == 0) continue;
            if (result.length() > 0) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }
}
