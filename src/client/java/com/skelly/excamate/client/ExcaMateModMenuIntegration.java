package com.skelly.excamate.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.InvocationTargetException;

public class ExcaMateModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (!FabricLoader.getInstance().isModLoaded("cloth-config")) {
            return ModMenuApi.super.getModConfigScreenFactory();
        }

        return ExcaMateModMenuIntegration::createClothConfigScreen;
    }

    private static Screen createClothConfigScreen(Screen parent) {
        try {
            Class<?> screenClass = Class.forName("com.skelly.excamate.client.ExcaMateConfigScreen");
            return (Screen) screenClass.getMethod("create", Screen.class).invoke(null, parent);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to create the ExcaMate Cloth Config screen", e);
        }
    }
}
