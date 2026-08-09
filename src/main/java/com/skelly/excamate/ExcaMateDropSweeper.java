package com.skelly.excamate;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Cascading plants (chorus plant, bamboo, sugar cane, etc.) don't collapse instantly in vanilla —
 * each disconnected segment schedules its own destruction a tick after the one below/beside it, so
 * a tall structure unravels one link per tick rather than all at once. A single pickup sweep taken
 * right after the player's break can only catch whatever has already dropped by that instant; this
 * class re-sweeps the same area for several subsequent ticks so later waves of the cascade get
 * picked up too, instead of being left on the ground.
 */
public final class ExcaMateDropSweeper {
    private record PendingSweep(@NotNull ServerPlayer player, @NotNull AABB area, int ticksRemaining) {
    }

    private static final List<@NotNull PendingSweep> PENDING = new ArrayList<>();

    private ExcaMateDropSweeper() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> onEndServerTick());
    }

    public static void schedule(@NotNull ServerPlayer player, @NotNull AABB area, int durationTicks) {
        if (durationTicks > 0) {
            PENDING.add(new PendingSweep(player, area, durationTicks));
        }
    }

    private static void onEndServerTick() {
        if (PENDING.isEmpty()) return;

        List<@NotNull PendingSweep> remaining = new ArrayList<>();
        for (PendingSweep sweep : PENDING) {
            ServerPlayer player = sweep.player();
            if (!player.isRemoved()) {
                stashDropsIn(player, sweep.area());
                if (sweep.ticksRemaining() > 1) {
                    remaining.add(new PendingSweep(player, sweep.area(), sweep.ticksRemaining() - 1));
                }
            }
        }

        PENDING.clear();
        PENDING.addAll(remaining);
    }

    private static void stashDropsIn(@NotNull ServerPlayer player, @NotNull AABB area) {
        ServerLevel level = player.level();
        List<@NotNull ItemEntity> itemEntities = level.getEntitiesOfClass(ItemEntity.class, area);

        for (ItemEntity itemEntity : itemEntities) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) continue;

            ItemStack remainingStack = stack.copy();
            if (player.getInventory().add(remainingStack)) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(remainingStack);
            }
        }

        player.getInventory().setChanged();
    }
}
