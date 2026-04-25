package com.skelly.deepstash.client;

import com.skelly.deepstash.ExcaMate;
import com.skelly.deepstash.ExcaMateNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class ExcaMateClient implements ClientModInitializer {
    private static final String VEIN_MINE_KEY_TRANSLATION = "key.deepstash.vein_mine";
    private static final String CYCLE_MODE_KEY_TRANSLATION = "key.deepstash.cycle_mode";
    private static final int DEFAULT_VEIN_MINE_KEY = GLFW.GLFW_KEY_V;
    private static final int DEFAULT_CYCLE_MODE_KEY = GLFW.GLFW_KEY_B;

    public static KeyMapping veinMineKey;
    public static KeyMapping cycleModeKey;
    private static boolean lastSentVeinMineKeyDown;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(ExcaMate.MOD_ID, "controls")
        );

        veinMineKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            VEIN_MINE_KEY_TRANSLATION,
            DEFAULT_VEIN_MINE_KEY,
            category
        ));
        cycleModeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            CYCLE_MODE_KEY_TRANSLATION,
            DEFAULT_CYCLE_MODE_KEY,
            category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.getConnection() == null) {
                lastSentVeinMineKeyDown = false;
                return;
            }

            boolean isKeyDown = veinMineKey.isDown();
            if (isKeyDown != lastSentVeinMineKeyDown) {
                ClientPlayNetworking.send(new ExcaMateNetworking.VeinKeyStatePayload(isKeyDown));
                lastSentVeinMineKeyDown = isKeyDown;
            }

            while (cycleModeKey.consumeClick()) {
                ClientPlayNetworking.send(new ExcaMateNetworking.CycleModePayload());
            }
        });
    }
}
