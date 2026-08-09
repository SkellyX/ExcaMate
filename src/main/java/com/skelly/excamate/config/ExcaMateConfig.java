package com.skelly.excamate.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import com.skelly.excamate.ExcaMate;
import com.skelly.excamate.ExcaMateMode;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExcaMateConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("excamate.json");
    private static final Path LEGACY_CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("deepstash.json");
    private static final int DEFAULT_VEIN_MAX_BLOCKS = 12;
    private static final int DEFAULT_BRANCH_MAX_BLOCKS = 16;
    private static final int DEFAULT_EXCAMATE_MAX_BLOCKS = 27;
    private static final int LEGACY_DEFAULT_MAX_BLOCKS = 16;
    private static final int SAFE_MAX_BLOCK_LIMIT = 128;
    private static final int CURRENT_CONFIG_VERSION = 1;

    public int configVersion = CURRENT_CONFIG_VERSION;
    public boolean autoPickup = true;
    public boolean autoCollectXp = true;
    public boolean autoTorchInBranchMode = true;
    public boolean autoReplantCrops = true;
    public int veinMaxBlocks = DEFAULT_VEIN_MAX_BLOCKS;
    public int branchMaxBlocks = DEFAULT_BRANCH_MAX_BLOCKS;
    public int excaMateMaxBlocks = DEFAULT_EXCAMATE_MAX_BLOCKS;
    public String defaultMode = ExcaMateMode.VEIN.configName();
    public List<@NotNull String> extraVeinMineAllowList = new ArrayList<>();
    public List<@NotNull String> veinMineBlockList = new ArrayList<>();
    public List<@NotNull String> autoPickupBlockList = new ArrayList<>();

    private transient Set<@NotNull Block> extraVeinMineAllowBlocks = Set.of();
    private transient Set<@NotNull Block> veinMineBlockedBlocks = Set.of();
    private transient Set<@NotNull Block> autoPickupBlockedBlocks = Set.of();

    public static @NotNull ExcaMateConfig load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                return loadFrom(CONFIG_PATH);
            }

            if (Files.exists(LEGACY_CONFIG_PATH)) {
                ExcaMateConfig config = loadFrom(LEGACY_CONFIG_PATH);
                config.save();
                return config;
            }
        } catch (IOException | JsonParseException e) {
            ExcaMate.LOGGER.error("Failed to load config", e);
        }
        
        ExcaMateConfig config = new ExcaMateConfig();
        config.validate();
        config.save();
        return config;
    }

    private static @NotNull ExcaMateConfig loadFrom(@NotNull Path path) throws IOException {
        String json = Files.readString(path);
        ExcaMateConfig config = loadedOrDefault(json);
        config.validate();
        config.save();
        return config;
    }

    private static @NotNull ExcaMateConfig loadedOrDefault(@NotNull String json) {
        @Nullable ExcaMateConfig config = GSON.fromJson(json, ExcaMateConfig.class);
        config = config == null ? new ExcaMateConfig() : config;
        migrateLegacyMaxBlockLimit(json, config);
        migrateLegacyVeinMineAllowList(json, config);
        return config;
    }

    private static void migrateLegacyMaxBlockLimit(@NotNull String json, @NotNull ExcaMateConfig config) {
        JsonElement parsed = JsonParser.parseString(json);
        if (!parsed.isJsonObject()) return;

        JsonObject object = parsed.getAsJsonObject();
        JsonElement legacyLimit = object.get("veinMinerMaxBlocks");
        if (legacyLimit == null || !legacyLimit.isJsonPrimitive() || !legacyLimit.getAsJsonPrimitive().isNumber()) return;

        int fallback = legacyLimit.getAsInt();
        if (fallback == LEGACY_DEFAULT_MAX_BLOCKS) return;

        if (!object.has("veinMaxBlocks")) {
            config.veinMaxBlocks = fallback;
        }
        if (!object.has("branchMaxBlocks")) {
            config.branchMaxBlocks = fallback;
        }
        if (!object.has("excaMateMaxBlocks")) {
            config.excaMateMaxBlocks = fallback;
        }
    }

    private static void migrateLegacyVeinMineAllowList(@NotNull String json, @NotNull ExcaMateConfig config) {
        JsonElement parsed = JsonParser.parseString(json);
        if (!parsed.isJsonObject()) return;

        JsonObject object = parsed.getAsJsonObject();
        if (object.has("extraVeinMineAllowList") || !object.has("veinMineAllowList")) return;

        JsonElement legacyAllowList = object.get("veinMineAllowList");
        if (!legacyAllowList.isJsonArray()) return;

        JsonArray legacyValues = legacyAllowList.getAsJsonArray();
        config.extraVeinMineAllowList = new ArrayList<>();
        for (JsonElement value : legacyValues) {
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                config.extraVeinMineAllowList.add(value.getAsString());
            }
        }
    }

    public void validate() {
        // ExcaMate block caps are per-mode now. The old veinMinerMaxBlocks value is
        // only read as a migration fallback, then omitted when this config is saved.
        veinMaxBlocks = validateBlockLimit(veinMaxBlocks, DEFAULT_VEIN_MAX_BLOCKS);
        branchMaxBlocks = validateBlockLimit(branchMaxBlocks, DEFAULT_BRANCH_MAX_BLOCKS);
        excaMateMaxBlocks = validateBlockLimit(excaMateMaxBlocks, DEFAULT_EXCAMATE_MAX_BLOCKS);

        defaultMode = ExcaMateMode.fromConfigName(defaultMode).configName();

        extraVeinMineAllowList = nonNullList(extraVeinMineAllowList);
        veinMineBlockList = nonNullList(veinMineBlockList);
        autoPickupBlockList = nonNullList(autoPickupBlockList);

        extraVeinMineAllowBlocks = parseBlockList("extraVeinMineAllowList", extraVeinMineAllowList);
        veinMineBlockedBlocks = parseBlockList("veinMineBlockList", veinMineBlockList);
        autoPickupBlockedBlocks = parseBlockList("autoPickupBlockList", autoPickupBlockList);
    }

    private static int validateBlockLimit(int value, int defaultValue) {
        if (value <= 0) return defaultValue;
        return Math.min(value, SAFE_MAX_BLOCK_LIMIT);
    }

    private static @NotNull List<@NotNull String> nonNullList(@Nullable List<@NotNull String> list) {
        if (list == null) return new ArrayList<>();
        list.removeIf(value -> value == null || value.isBlank());
        return list;
    }

    private static @NotNull Set<@NotNull Block> parseBlockList(@NotNull String fieldName, @NotNull List<@NotNull String> blockIds) {
        if (blockIds.isEmpty()) return Set.of();

        Set<@NotNull Block> blocks = new HashSet<>();
        for (String blockId : blockIds) {
            Identifier id = Identifier.tryParse(blockId);
            if (id == null) {
                ExcaMate.LOGGER.warn("Ignoring invalid block ID in {}: {}", fieldName, blockId);
                continue;
            }

            if (!BuiltInRegistries.BLOCK.containsKey(id)) {
                ExcaMate.LOGGER.warn("Ignoring unknown block ID in {}: {}", fieldName, blockId);
                continue;
            }

            blocks.add(BuiltInRegistries.BLOCK.getValue(id));
        }

        return Set.copyOf(blocks);
    }

    public boolean isExtraVeinMineBlockAllowed(@NotNull Block block) {
        return extraVeinMineAllowBlocks.contains(block);
    }

    public boolean isVeinMineBlockBlocked(@NotNull Block block) {
        return veinMineBlockedBlocks.contains(block);
    }

    public boolean isAutoPickupBlockBlocked(@NotNull Block block) {
        return autoPickupBlockedBlocks.contains(block);
    }

    public void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            ExcaMate.LOGGER.error("Failed to save config", e);
        }
    }
}
