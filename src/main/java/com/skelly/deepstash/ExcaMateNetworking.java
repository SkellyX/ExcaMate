package com.skelly.deepstash;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ExcaMateNetworking {
    private static final Set<@NotNull UUID> HELD_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Map<@NotNull UUID, @NotNull ExcaMateMode> PLAYER_MODES = new ConcurrentHashMap<>();

    private ExcaMateNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(VeinKeyStatePayload.TYPE, VeinKeyStatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CycleModePayload.TYPE, CycleModePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(VeinKeyStatePayload.TYPE, (payload, context) -> {
            UUID playerId = context.player().getUUID();
            if (payload.held()) {
                HELD_PLAYERS.add(playerId);
            } else {
                HELD_PLAYERS.remove(playerId);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(CycleModePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            ExcaMateMode nextMode = getMode(player).next();
            PLAYER_MODES.put(player.getUUID(), nextMode);
            player.sendOverlayMessage(Component.literal("ExcaMate Mode: " + nextMode.displayName()));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID playerId = handler.player.getUUID();
            HELD_PLAYERS.remove(playerId);
            PLAYER_MODES.remove(playerId);
        });
    }

    public static boolean isVeinKeyHeld(@NotNull ServerPlayer player) {
        return HELD_PLAYERS.contains(player.getUUID());
    }

    public static @NotNull ExcaMateMode getMode(@NotNull ServerPlayer player) {
        return PLAYER_MODES.computeIfAbsent(player.getUUID(), ignored -> getDefaultMode());
    }

    private static @NotNull ExcaMateMode getDefaultMode() {
        return ExcaMate.config == null ? ExcaMateMode.VEIN : ExcaMateMode.fromConfigName(ExcaMate.config.defaultMode);
    }

    public record VeinKeyStatePayload(boolean held) implements CustomPacketPayload {
        public static final Type<@NotNull VeinKeyStatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ExcaMate.MOD_ID, "vein_key_state")
        );

        public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull VeinKeyStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            VeinKeyStatePayload::held,
            VeinKeyStatePayload::new
        );

        @Override
        public Type<? extends @NotNull CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record CycleModePayload() implements CustomPacketPayload {
        public static final Type<@NotNull CycleModePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ExcaMate.MOD_ID, "cycle_mode")
        );

        public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull CycleModePayload> CODEC = StreamCodec.unit(
            new CycleModePayload()
        );

        @Override
        public Type<? extends @NotNull CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
