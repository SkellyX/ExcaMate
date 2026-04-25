package com.skelly.deepstash.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import com.skelly.deepstash.ExcaMate;
import com.skelly.deepstash.ExcaMateMode;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExcaMateConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("excamate.json");
    private static final Path LEGACY_CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("deepstash.json");
    private static final int DEFAULT_VEIN_MAX_BLOCKS = 64;
    private static final int DEFAULT_BRANCH_MAX_BLOCKS = 32;
    private static final int DEFAULT_EXCAMATE_MAX_BLOCKS = 27;
    private static final int SAFE_MAX_BLOCK_LIMIT = 128;

    public boolean autoPickup = true;
    public boolean autoCollectXp = true;
    public boolean autoTorchInBranchMode = true;
    public int veinMaxBlocks = DEFAULT_VEIN_MAX_BLOCKS;
    public int branchMaxBlocks = DEFAULT_BRANCH_MAX_BLOCKS;
    public int excaMateMaxBlocks = DEFAULT_EXCAMATE_MAX_BLOCKS;
    public String defaultMode = ExcaMateMode.VEIN.configName();

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
        return config;
    }

    private static void migrateLegacyMaxBlockLimit(@NotNull String json, @NotNull ExcaMateConfig config) {
        JsonElement parsed = JsonParser.parseString(json);
        if (!parsed.isJsonObject()) return;

        JsonObject object = parsed.getAsJsonObject();
        JsonElement legacyLimit = object.get("veinMinerMaxBlocks");
        if (legacyLimit == null || !legacyLimit.isJsonPrimitive() || !legacyLimit.getAsJsonPrimitive().isNumber()) return;

        int fallback = legacyLimit.getAsInt();
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

    public void validate() {
        // ExcaMate block caps are per-mode now. The old veinMinerMaxBlocks value is
        // only read as a migration fallback, then omitted when this config is saved.
        veinMaxBlocks = validateBlockLimit(veinMaxBlocks, DEFAULT_VEIN_MAX_BLOCKS);
        branchMaxBlocks = validateBlockLimit(branchMaxBlocks, DEFAULT_BRANCH_MAX_BLOCKS);
        excaMateMaxBlocks = validateBlockLimit(excaMateMaxBlocks, DEFAULT_EXCAMATE_MAX_BLOCKS);

        defaultMode = ExcaMateMode.fromConfigName(defaultMode).configName();
    }

    private static int validateBlockLimit(int value, int defaultValue) {
        if (value <= 0) return defaultValue;
        return Math.min(value, SAFE_MAX_BLOCK_LIMIT);
    }

    public void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            ExcaMate.LOGGER.error("Failed to save config", e);
        }
    }
}
