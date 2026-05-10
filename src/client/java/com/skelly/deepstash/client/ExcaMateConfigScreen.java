package com.skelly.deepstash.client;

import com.skelly.deepstash.ExcaMate;
import com.skelly.deepstash.ExcaMateMode;
import com.skelly.deepstash.config.ExcaMateConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry;
import me.shedaniel.clothconfig2.gui.entries.StringListListEntry;
import me.shedaniel.clothconfig2.gui.widget.SearchFieldEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

public final class ExcaMateConfigScreen {
    private static final int MIN_BLOCK_LIMIT = 1;
    private static final int MAX_BLOCK_LIMIT = 128;

    private ExcaMateConfigScreen() {
    }

    public static Screen create(Screen parent) {
        ExcaMateConfig config = ExcaMate.config == null ? ExcaMateConfig.load() : ExcaMate.config;
        ExcaMate.config = config;

        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(translatable("title"))
            .setSavingRunnable(() -> save(config))
            .setAfterInitConsumer(ExcaMateConfigScreen::removeScreenSearchBar);

        ConfigEntryBuilder entries = builder.entryBuilder();
        addGeneralCategory(builder, entries, config);
        addBlockLimitsCategory(builder, entries, config);
        addBlockListsCategory(builder, entries, config);

        return builder.build();
    }

    private static void addGeneralCategory(
        @NotNull ConfigBuilder builder,
        @NotNull ConfigEntryBuilder entries,
        @NotNull ExcaMateConfig config
    ) {
        ConfigCategory general = builder.getOrCreateCategory(translatable("category.general"));

        general.addEntry(entries.startBooleanToggle(translatable("option.auto_pickup"), config.autoPickup)
            .setDefaultValue(true)
            .setTooltip(translatable("tooltip.auto_pickup"))
            .setSaveConsumer(value -> config.autoPickup = value)
            .build());

        general.addEntry(entries.startBooleanToggle(translatable("option.direct_xp"), config.autoCollectXp)
            .setDefaultValue(true)
            .setTooltip(translatable("tooltip.direct_xp"))
            .setSaveConsumer(value -> config.autoCollectXp = value)
            .build());

        general.addEntry(entries.startBooleanToggle(translatable("option.auto_torch"), config.autoTorchInBranchMode)
            .setDefaultValue(true)
            .setTooltip(translatable("tooltip.auto_torch"))
            .setSaveConsumer(value -> config.autoTorchInBranchMode = value)
            .build());

        general.addEntry(entries.startEnumSelector(
                translatable("option.default_mode"),
                ExcaMateMode.class,
                ExcaMateMode.fromConfigName(config.defaultMode)
            )
            .setDefaultValue(ExcaMateMode.VEIN)
            .setEnumNameProvider(mode -> translatable("mode." + mode.name().toLowerCase(Locale.ROOT)))
            .setSaveConsumer(value -> config.defaultMode = value.configName())
            .build());
    }

    private static void addBlockLimitsCategory(
        @NotNull ConfigBuilder builder,
        @NotNull ConfigEntryBuilder entries,
        @NotNull ExcaMateConfig config
    ) {
        ConfigCategory limits = builder.getOrCreateCategory(translatable("category.block_limits"));

        limits.addEntry(entries.startIntField(translatable("option.vein_max_blocks"), config.veinMaxBlocks)
            .setDefaultValue(12)
            .setMin(MIN_BLOCK_LIMIT)
            .setMax(MAX_BLOCK_LIMIT)
            .setTooltip(translatable("tooltip.vein_max_blocks"))
            .setSaveConsumer(value -> config.veinMaxBlocks = value)
            .build());

        limits.addEntry(entries.startIntField(translatable("option.branch_max_blocks"), config.branchMaxBlocks)
            .setDefaultValue(16)
            .setMin(MIN_BLOCK_LIMIT)
            .setMax(MAX_BLOCK_LIMIT)
            .setTooltip(translatable("tooltip.branch_max_blocks"))
            .setSaveConsumer(value -> config.branchMaxBlocks = value)
            .build());

        limits.addEntry(entries.startIntField(translatable("option.excamate_max_blocks"), config.excaMateMaxBlocks)
            .setDefaultValue(27)
            .setMin(MIN_BLOCK_LIMIT)
            .setMax(MAX_BLOCK_LIMIT)
            .setTooltip(translatable("tooltip.excamate_max_blocks"))
            .setSaveConsumer(value -> config.excaMateMaxBlocks = value)
            .build());
    }

    private static void addBlockListsCategory(
        @NotNull ConfigBuilder builder,
        @NotNull ConfigEntryBuilder entries,
        @NotNull ExcaMateConfig config
    ) {
        ConfigCategory blockLists = builder.getOrCreateCategory(translatable("category.block_lists"));

        List<@NotNull String> extraVeinMineAllowList = new ArrayList<>(config.extraVeinMineAllowList);
        addBlockListEntries(
            blockLists,
            entries,
            "extra_vein_mine_allowlist",
            extraVeinMineAllowList,
            values -> config.extraVeinMineAllowList = values,
            true
        );

        List<@NotNull String> veinMineBlockList = new ArrayList<>(config.veinMineBlockList);
        addBlockListEntries(
            blockLists,
            entries,
            "vein_mine_blocklist",
            veinMineBlockList,
            values -> config.veinMineBlockList = values,
            false
        );

        List<@NotNull String> autoPickupBlockList = new ArrayList<>(config.autoPickupBlockList);
        addBlockListEntries(
            blockLists,
            entries,
            "auto_pickup_blocklist",
            autoPickupBlockList,
            values -> config.autoPickupBlockList = values,
            false
        );
    }

    private static void addBlockListEntries(
        @NotNull ConfigCategory category,
        @NotNull ConfigEntryBuilder entries,
        @NotNull String key,
        @NotNull List<@NotNull String> stagedValues,
        @NotNull Consumer<List<@NotNull String>> saveConsumer,
        boolean showInstructions
    ) {
        category.addEntry(entries.startTextDescription(translatable("subtitle." + key)).build());
        if (showInstructions) {
            category.addEntry(entries.startTextDescription(translatable("note.block_lists")).build());
        }

        DropdownBoxEntry<String> blockSelector = entries.startStringDropdownMenu(
                translatable("option." + key + ".add_id"),
                "",
                ExcaMateConfigScreen::blockSelectionLabel
            )
            .setDefaultValue("")
            .setSelections(blockIdSelections())
            .setSuggestionMode(true)
            .setErrorSupplier(value -> {
                String blockId = extractBlockId(value);
                if (blockId.isEmpty()) return Optional.empty();
                if (!isKnownBlockId(blockId)) return Optional.of(translatable("error.invalid_block_id"));

                return Optional.empty();
            })
            .build();
        category.addEntry(blockSelector);
        category.addEntry(blockListEntry(entries, key, stagedValues, blockSelector, saveConsumer));
    }

    private static @NotNull Component blockSelectionLabel(@NotNull String blockId) {
        if (blockId.isBlank()) {
            return Component.empty();
        }

        Identifier id = Identifier.tryParse(blockId);
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
            return translatable("option.block_selector.placeholder");
        }

        Block block = BuiltInRegistries.BLOCK.getValue(id);
        return Component.literal(block.getName().getString() + " (" + blockId + ")");
    }

    private static @NotNull List<String> blockIdSelections() {
        return BuiltInRegistries.BLOCK.keySet().stream()
            .map(Identifier::toString)
            .sorted()
            .toList();
    }

    private static boolean isKnownBlockId(@NotNull String value) {
        Identifier id = Identifier.tryParse(value.trim());
        return id != null && BuiltInRegistries.BLOCK.containsKey(id);
    }

    private static StringListListEntry blockListEntry(
        @NotNull ConfigEntryBuilder entries,
        @NotNull String key,
        @NotNull List<@NotNull String> currentValues,
        @NotNull DropdownBoxEntry<String> blockSelector,
        @NotNull Consumer<List<@NotNull String>> saveConsumer
    ) {
        return entries.startStrList(translatable("option." + key), displayBlockIdList(currentValues))
            .setDefaultValue(List.of())
            .setCellErrorSupplier(ExcaMateConfigScreen::validateBlockListValue)
            .setCreateNewInstance(entry -> {
                String selectedBlockId = extractBlockId(blockSelector.getValue());
                if (!isKnownBlockId(selectedBlockId) || containsBlockId(entry.getValue(), selectedBlockId)) {
                    return new ReadOnlyBlockListCell("", entry);
                }

                blockSelector.getSelectionElement().getTopRenderer().setValue("");
                return new ReadOnlyBlockListCell(displayBlockId(selectedBlockId), entry);
            })
            .setSaveConsumer(values -> saveConsumer.accept(cleanBlockIdList(values)))
            .build();
    }

    private static @NotNull List<String> displayBlockIdList(@NotNull List<@NotNull String> values) {
        return values.stream().map(ExcaMateConfigScreen::displayBlockId).toList();
    }

    private static @NotNull String displayBlockId(@NotNull String blockId) {
        Identifier id = Identifier.tryParse(blockId.trim());
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) return blockId.trim();

        return BuiltInRegistries.BLOCK.getValue(id).getName().getString() + " (" + id + ")";
    }

    private static @NotNull Optional<Component> validateBlockListValue(@NotNull String value) {
        String blockId = extractBlockId(value);
        if (blockId.isEmpty()) return Optional.empty();
        if (!isKnownBlockId(blockId)) return Optional.of(translatable("error.invalid_block_id"));

        return Optional.empty();
    }

    private static @NotNull List<@NotNull String> cleanBlockIdList(@NotNull List<String> values) {
        LinkedHashSet<@NotNull String> cleaned = new LinkedHashSet<>();
        for (String value : values) {
            String blockId = extractBlockId(value);
            if (!blockId.isEmpty()) {
                cleaned.add(blockId);
            }
        }

        return new ArrayList<>(cleaned);
    }

    private static boolean containsBlockId(@NotNull List<String> values, @NotNull String blockId) {
        return values.stream().map(ExcaMateConfigScreen::extractBlockId).anyMatch(blockId::equals);
    }

    private static @NotNull String extractBlockId(@NotNull String value) {
        String trimmed = value.trim();
        int openParen = trimmed.lastIndexOf('(');
        int closeParen = trimmed.endsWith(")") ? trimmed.length() - 1 : -1;
        if (openParen >= 0 && closeParen > openParen) {
            return trimmed.substring(openParen + 1, closeParen).trim();
        }

        return trimmed;
    }

    private static void save(@NotNull ExcaMateConfig config) {
        config.validate();
        config.save();
        ExcaMate.config = config;
    }

    private static void removeScreenSearchBar(@NotNull Screen screen) {
        if (screen instanceof ClothConfigScreen clothScreen && clothScreen.listWidget != null) {
            clothScreen.listWidget.children().removeIf(SearchFieldEntry.class::isInstance);
        }
    }

    private static @NotNull Component translatable(@NotNull String key) {
        return Component.translatable("config.excamate." + key);
    }

    private static final class ReadOnlyBlockListCell extends StringListListEntry.StringListCell {
        private ReadOnlyBlockListCell(@NotNull String value, @NotNull StringListListEntry entry) {
            super(value, entry);
            widget.setEditable(false);
            widget.active = false;
        }

        @Override
        public void extractRenderState(
            @NotNull GuiGraphicsExtractor graphics,
            int index,
            int y,
            int x,
            int width,
            int height,
            int mouseX,
            int mouseY,
            boolean selected,
            float delta
        ) {
            boolean hovered = isMouseOver(mouseX, mouseY);
            if (selected) {
                graphics.fill(x, y + 1, x + width - 12, y + height - 1, 0x66FFFFFF);
            } else if (hovered) {
                graphics.fill(x, y + 1, x + width - 12, y + height - 1, 0x33FFFFFF);
            }

            graphics.text(
                Minecraft.getInstance().font,
                Component.literal(getValue()).getVisualOrderText(),
                x + 2,
                y + 6,
                getPreferredTextColor()
            );
        }

        @Override
        public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean focused) {
            return event.button() == 0 && isMouseOver(event.x(), event.y());
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }
    }
}
