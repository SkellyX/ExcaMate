package com.skelly.deepstash;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum ExcaMateMode {
    VEIN("Vein"),
    BRANCH_1X2("Branch Tunnel"),
    EXCAVATE_3X3("3x3 Excavation");

    private final String displayName;

    ExcaMateMode(String displayName) {
        this.displayName = displayName;
    }

    public @NotNull ExcaMateMode next() {
        ExcaMateMode[] modes = values();
        return modes[(ordinal() + 1) % modes.length];
    }

    public @NotNull String displayName() {
        return displayName;
    }

    public @NotNull String configName() {
        return name();
    }

    public static @NotNull ExcaMateMode fromConfigName(String value) {
        if (value != null) {
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // Fall through to the safe default for old or manually edited configs.
            }
        }

        return VEIN;
    }
}
