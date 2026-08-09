package com.skelly.excamate;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum ExcaMateMode {
    VEIN("Vein"),
    BRANCH_1X2("Branch"),
    EXCAVATE_3X3("Excavate");

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
            String normalized = value.trim().toUpperCase(Locale.ROOT);
            if ("EXCAMATE_3X3".equals(normalized) || "EXCAVATE_3X3X3".equals(normalized)) {
                return EXCAVATE_3X3;
            }

            try {
                return valueOf(normalized);
            } catch (IllegalArgumentException ignored) {
                // Fall through to the safe default for old or manually edited configs.
            }
        }

        return VEIN;
    }
}
