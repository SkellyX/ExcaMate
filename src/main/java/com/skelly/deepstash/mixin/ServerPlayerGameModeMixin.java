package com.skelly.deepstash.mixin;

import com.skelly.deepstash.ExcaMate;
import com.skelly.deepstash.ExcaMateMode;
import com.skelly.deepstash.ExcaMateMiningRules;
import com.skelly.deepstash.ExcaMateNetworking;
import com.skelly.deepstash.ExcaMateXpCapture;
import com.skelly.deepstash.config.ExcaMateConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    private static final int ORE_STRAGGLER_SEARCH_RADIUS = 4;
    private static final float EXHAUSTION_PER_EXTRA_BLOCK = 0.005F;

    @Shadow protected @NotNull ServerLevel level;

    @Shadow @Final protected @NotNull ServerPlayer player;

    @Shadow public abstract boolean isCreative();

    @Shadow public abstract boolean destroyBlock(@NotNull BlockPos pos);

    private boolean excamate$breakingBlock;
    private boolean excamate$capturingXp;
    private boolean excamate$processingExcaMateBreak;
    @Nullable
    private BlockPos excamate$pendingPos;
    @Nullable
    private BlockState excamate$pendingState;
    @Nullable
    private BlockPos excamate$lastActionPos;
    @Nullable
    private Direction excamate$lastHitDirection;

    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"))
    private void excamate$captureHitDirection(
        @NotNull BlockPos pos,
        Action action,
        @NotNull Direction direction,
        int worldHeight,
        int sequence,
        CallbackInfo ci
    ) {
        excamate$lastActionPos = pos;
        excamate$lastHitDirection = direction;
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void excamate$captureState(@NotNull BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ExcaMateConfig config = ExcaMate.config;
        if (excamate$breakingBlock || config == null) return;
        if (!excamate$isVeinMiningActive()) return;
        if (isCreative()) return;

        @NotNull BlockState state = level.getBlockState(pos);
        if (state.isAir() || !excamate$canMineWithExcaMate(pos, state, config)) return;

        excamate$pendingPos = pos;
        excamate$pendingState = state;
        excamate$processingExcaMateBreak = true;

        if (config.autoCollectXp) {
            // Start before vanilla breaks the initial block, so ExcaMate-active ore mining
            // captures the first block's vanilla XP too instead of leaving one orb behind.
            ExcaMateXpCapture.start();
            excamate$capturingXp = true;
        }
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void excamate$destroyAndStash(@NotNull BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            BlockPos pendingPos = excamate$pendingPos;
            if (pendingPos != null && pendingPos.equals(pos)) {
                excamate$pendingPos = null;
                excamate$pendingState = null;
                excamate$clearHitDirection(pos);
                excamate$processingExcaMateBreak = false;
                excamate$stopXpCapture();
            }
            return;
        }
        ExcaMateConfig config = ExcaMate.config;
        if (excamate$breakingBlock || config == null) return;
        if (isCreative()) return;
        BlockState brokenState = excamate$pendingState;
        BlockPos pendingPos = excamate$pendingPos;
        if (pendingPos == null || !pendingPos.equals(pos) || brokenState == null) {
            excamate$processingExcaMateBreak = false;
            excamate$stopXpCapture();
            return;
        }

        excamate$pendingPos = null;
        excamate$pendingState = null;

        int capturedXp;
        try {
            if (excamate$shouldAutoPickupExcaMateDrops(brokenState)) {
                excamate$stashDropsNear(pos);
            }

            if (excamate$isVeinMiningActive()) {
                excamate$mineExcaMateBlocks(pos, brokenState);
            }
        } finally {
            excamate$clearHitDirection(pos);
            excamate$processingExcaMateBreak = false;
            capturedXp = excamate$stopXpCapture();
        }

        // Award the initial block + all extra vein block XP in one server-side batch.
        // This avoids XP orb spam and prevents a separate initial-block orb during ExcaMate.
        if (capturedXp > 0) {
            player.giveExperiencePoints(capturedXp);
        }
    }

    private void excamate$mineExcaMateBlocks(@NotNull BlockPos startPos, @NotNull BlockState startState) {
        int maxBlocks = Math.min(excamate$getConfiguredExtraBlockLimit(ExcaMateNetworking.getMode(player)), excamate$getSafeExtraToolUses());
        if (maxBlocks <= 0) return;

        if (ExcaMateNetworking.getMode(player) == ExcaMateMode.BRANCH_1X2) {
            excamate$mineBranchTunnel(startState, maxBlocks);
            return;
        }

        List<@NotNull BlockPos> blocksToMine = excamate$collectBlocksForMode(startPos, startState, maxBlocks);
        excamate$breakCollectedBlocks(blocksToMine);
    }

    private List<@NotNull BlockPos> excamate$collectBlocksForMode(@NotNull BlockPos startPos, @NotNull BlockState startState, int maxBlocks) {
        // The selected mode is universal; strict tool checks decide which blocks each held tool may include.
        return switch (ExcaMateNetworking.getMode(player)) {
            case VEIN -> excamate$collectVeinModeBlocks(startPos, startState, maxBlocks);
            case BRANCH_1X2 -> excamate$collectBranchTunnel(startPos, startState, maxBlocks);
            case EXCAVATE_3X3 -> excamate$collectExcavationFace(startPos, startState, maxBlocks);
        };
    }

    private List<@NotNull BlockPos> excamate$collectVeinModeBlocks(@NotNull BlockPos startPos, @NotNull BlockState startState, int maxBlocks) {
        List<BlockPos> blocksToMine = excamate$collectConnectedVein(startPos, startState, maxBlocks);
        if (ExcaMateMiningRules.isOreBlock(startState) && blocksToMine.size() < maxBlocks) {
            excamate$collectOreStragglers(startPos, startState, blocksToMine, maxBlocks);
        }

        return blocksToMine;
    }

    private List<@NotNull BlockPos> excamate$collectBranchTunnel(@NotNull BlockPos startPos, @NotNull BlockState startState, int maxBlocks) {
        List<@NotNull BlockPos> blocksToMine = new ArrayList<>();
        Direction facing = player.getDirection();
        BlockPos playerFeet = player.blockPosition();

        for (int step = 0; step <= maxBlocks && blocksToMine.size() < maxBlocks; step++) {
            BlockPos lower = playerFeet.relative(facing, step);
            if (excamate$isValidPatternBlock(lower, startState)) {
                blocksToMine.add(lower);
                if (blocksToMine.size() >= maxBlocks) break;
            }

            BlockPos upper = lower.above();
            if (excamate$isValidPatternBlock(upper, startState)) {
                blocksToMine.add(upper);
            }
        }

        return blocksToMine;
    }

    private void excamate$mineBranchTunnel(@NotNull BlockState startState, int maxBlocks) {
        Direction facing = player.getDirection();
        BlockPos playerFeet = player.blockPosition();
        int extraBlocksBroken = 0;

        for (int step = 0; step <= maxBlocks && extraBlocksBroken < maxBlocks; step++) {
            boolean brokeBlockThisStep = false;
            BlockPos lower = playerFeet.relative(facing, step);

            if (extraBlocksBroken < maxBlocks
                && excamate$getSafeExtraToolUses() > 0
                && excamate$isValidPatternBlock(lower, startState)
                && excamate$breakExcaMateBlock(lower)) {
                extraBlocksBroken++;
                brokeBlockThisStep = true;
            }

            BlockPos upper = lower.above();
            if (extraBlocksBroken < maxBlocks
                && excamate$getSafeExtraToolUses() > 0
                && excamate$isValidPatternBlock(upper, startState)
                && excamate$breakExcaMateBlock(upper)) {
                extraBlocksBroken++;
                brokeBlockThisStep = true;
            }

            if (brokeBlockThisStep) {
                excamate$tryPlaceBranchTorch(facing);
            }

            if (!excamate$isVeinMiningActive() || excamate$getSafeExtraToolUses() <= 0) break;
        }

        if (extraBlocksBroken > 0) {
            player.causeFoodExhaustion(EXHAUSTION_PER_EXTRA_BLOCK * extraBlocksBroken);
        }
    }

    private List<@NotNull BlockPos> excamate$collectExcavationFace(@NotNull BlockPos startPos, @NotNull BlockState startState, int maxBlocks) {
        List<@NotNull BlockPos> blocksToMine = new ArrayList<>();
        Direction hitDirection = excamate$getHitDirectionFor(startPos);

        for (int firstAxis = -1; firstAxis <= 1 && blocksToMine.size() < maxBlocks; firstAxis++) {
            for (int secondAxis = -1; secondAxis <= 1 && blocksToMine.size() < maxBlocks; secondAxis++) {
                BlockPos candidate = excamate$getExcavationFacePos(startPos, hitDirection, firstAxis, secondAxis);
                if (candidate.equals(startPos)) continue;
                if (excamate$isValidPatternBlock(candidate, startState)) {
                    blocksToMine.add(candidate);
                }
            }
        }

        return blocksToMine;
    }

    private Direction excamate$getHitDirectionFor(@NotNull BlockPos startPos) {
        BlockPos lastActionPos = excamate$lastActionPos;
        if (lastActionPos != null && lastActionPos.equals(startPos) && excamate$lastHitDirection != null) {
            return excamate$lastHitDirection;
        }

        return player.getDirection();
    }

    private void excamate$clearHitDirection(@NotNull BlockPos pos) {
        BlockPos lastActionPos = excamate$lastActionPos;
        if (lastActionPos != null && lastActionPos.equals(pos)) {
            excamate$lastActionPos = null;
            excamate$lastHitDirection = null;
        }
    }

    private BlockPos excamate$getExcavationFacePos(@NotNull BlockPos center, @NotNull Direction hitDirection, int firstAxis, int secondAxis) {
        return switch (hitDirection) {
            case UP, DOWN -> center.offset(firstAxis, 0, secondAxis);
            case NORTH, SOUTH -> center.offset(firstAxis, secondAxis, 0);
            case EAST, WEST -> center.offset(0, secondAxis, firstAxis);
        };
    }

    private void excamate$breakCollectedBlocks(List<@NotNull BlockPos> blocksToMine) {
        int extraBlocksBroken = 0;

        // Extra blocks still go through vanilla destroyBlock/playerDestroy so drops, Fortune,
        // Silk Touch, sounds, and durability stay vanilla. The Block XP mixin only converts
        // vanilla-calculated XP orb amounts into direct XP while the outer capture is active.
        for (BlockPos blockPos : blocksToMine) {
            if (!excamate$isVeinMiningActive() || excamate$getSafeExtraToolUses() <= 0) break;

            if (excamate$breakExcaMateBlock(blockPos)) {
                extraBlocksBroken++;
            }
        }

        // ServerPlayerGameMode#destroyBlock does not charge food exhaustion in this version,
        // so apply the vanilla-sized block break cost once for the extra vein blocks only.
        if (extraBlocksBroken > 0) {
            player.causeFoodExhaustion(EXHAUSTION_PER_EXTRA_BLOCK * extraBlocksBroken);
        }
    }

    private boolean excamate$breakExcaMateBlock(@NotNull BlockPos blockPos) {
        boolean wasProcessingExcaMateBreak = excamate$processingExcaMateBreak;
        BlockState state = level.getBlockState(blockPos);
        excamate$breakingBlock = true;
        excamate$processingExcaMateBreak = true;
        try {
            boolean destroyed = destroyBlock(blockPos);
            if (destroyed && excamate$shouldAutoPickupExcaMateDrops(state)) {
                excamate$stashDropsNear(blockPos);
            }

            return destroyed;
        } finally {
            excamate$breakingBlock = false;
            excamate$processingExcaMateBreak = wasProcessingExcaMateBreak;
        }
    }

    private List<@NotNull BlockPos> excamate$collectConnectedVein(@NotNull BlockPos startPos, @NotNull BlockState startState, int maxBlocks) {
        Set<@NotNull BlockPos> visited = new HashSet<>();
        Queue<@NotNull BlockPos> queue = new ArrayDeque<>();
        List<@NotNull BlockPos> blocksToMine = new ArrayList<>();

        visited.add(startPos);
        queue.add(startPos);

        while (!queue.isEmpty() && blocksToMine.size() < maxBlocks) {
            @NotNull BlockPos current = queue.remove();

            for (Direction direction : Direction.values()) {
                @NotNull BlockPos neighbor = current.relative(direction);
                if (!visited.add(neighbor) || !excamate$isMatchingMineableNeighbor(neighbor, startState)) continue;

                queue.add(neighbor);
                blocksToMine.add(neighbor);
                if (blocksToMine.size() >= maxBlocks) break;
            }
        }

        return blocksToMine;
    }

    private void excamate$collectOreStragglers(
        @NotNull BlockPos startPos,
        @NotNull BlockState startState,
        List<@NotNull BlockPos> blocksToMine,
        int maxBlocks
    ) {
        Set<@NotNull BlockPos> knownBlocks = new HashSet<>(blocksToMine);
        knownBlocks.add(startPos);

        List<@NotNull BlockPos> stragglers = new ArrayList<>();
        int radius = ORE_STRAGGLER_SEARCH_RADIUS;
        for (int x = startPos.getX() - radius; x <= startPos.getX() + radius; x++) {
            for (int y = startPos.getY() - radius; y <= startPos.getY() + radius; y++) {
                for (int z = startPos.getZ() - radius; z <= startPos.getZ() + radius; z++) {
                    @NotNull BlockPos candidate = new BlockPos(x, y, z);
                    if (knownBlocks.contains(candidate) || !excamate$isNearbyOreStraggler(startPos, candidate, startState)) continue;

                    knownBlocks.add(candidate);
                    stragglers.add(candidate);
                }
            }
        }

        stragglers.sort((first, second) -> Double.compare(first.distSqr(startPos), second.distSqr(startPos)));
        for (BlockPos straggler : stragglers) {
            if (blocksToMine.size() >= maxBlocks) return;
            blocksToMine.add(straggler);
        }
    }

    private boolean excamate$isNearbyOreStraggler(@NotNull BlockPos startPos, @NotNull BlockPos candidate, @NotNull BlockState startState) {
        if (!level.isInWorldBounds(candidate)) return false;
        if (candidate.distSqr(startPos) > ORE_STRAGGLER_SEARCH_RADIUS * ORE_STRAGGLER_SEARCH_RADIUS) return false;

        @NotNull BlockState candidateState = level.getBlockState(candidate);
        return !candidateState.isAir()
            && ExcaMateMiningRules.isSameOreType(startState, candidateState)
            && excamate$canMineWithExcaMate(candidate, candidateState);
    }

    private boolean excamate$isMatchingMineableNeighbor(@NotNull BlockPos pos, @NotNull BlockState startState) {
        if (!level.isInWorldBounds(pos)) return false;

        @NotNull BlockState state = level.getBlockState(pos);
        return excamate$isSameVeinType(startState, state)
            && !state.isAir()
            && excamate$canMineWithExcaMate(pos, state)
            && (ExcaMateMiningRules.isWoodBlock(startState) || excamate$isInMiningRange(pos));
    }

    private boolean excamate$isValidPatternBlock(@NotNull BlockPos pos, @NotNull BlockState startState) {
        if (!level.isInWorldBounds(pos)) return false;

        @NotNull BlockState state = level.getBlockState(pos);
        return !state.isAir()
            && excamate$canMineWithExcaMate(pos, state);
    }

    private boolean excamate$canMineWithExcaMate(@NotNull BlockPos pos, @NotNull BlockState state) {
        ExcaMateConfig config = ExcaMate.config;
        return config != null && excamate$canMineWithExcaMate(pos, state, config);
    }

    private boolean excamate$canMineWithExcaMate(
        @NotNull BlockPos pos,
        @NotNull BlockState state,
        @NotNull ExcaMateConfig config
    ) {
        return excamate$isSafeExcaMateBlock(pos, state)
            && ExcaMateMiningRules.canMineWithExcaMate(player, state, config);
    }

    private boolean excamate$isSafeExcaMateBlock(@NotNull BlockPos pos, @NotNull BlockState state) {
        if (level.getBlockEntity(pos) != null) return false;

        return !state.is(Blocks.CHEST)
            && !state.is(Blocks.TRAPPED_CHEST)
            && !state.is(Blocks.BARREL)
            && !state.is(BlockTags.SHULKER_BOXES)
            && !state.is(Blocks.ENDER_CHEST)
            && !state.is(Blocks.BEDROCK)
            && !state.is(Blocks.END_PORTAL_FRAME)
            && !state.is(Blocks.COMMAND_BLOCK)
            && !state.is(Blocks.CHAIN_COMMAND_BLOCK)
            && !state.is(Blocks.REPEATING_COMMAND_BLOCK)
            && !state.is(Blocks.STRUCTURE_BLOCK)
            && !state.is(Blocks.STRUCTURE_VOID)
            && !state.is(Blocks.JIGSAW)
            && !state.is(Blocks.BARRIER)
            && !state.is(Blocks.SPAWNER);
    }

    private boolean excamate$isSameVeinType(@NotNull BlockState startState, @NotNull BlockState state) {
        if (ExcaMateMiningRules.isWoodBlock(startState)) {
            return ExcaMateMiningRules.isWoodBlock(state);
        }

        if (ExcaMateMiningRules.isOreBlock(startState)) {
            return ExcaMateMiningRules.isSameOreType(startState, state);
        }

        return state.getBlock() == startState.getBlock();
    }

    private boolean excamate$isInMiningRange(@NotNull BlockPos pos) {
        double dx = player.getX() - (pos.getX() + 0.5);
        double dy = player.getY() - (pos.getY() + 0.5);
        double dz = player.getZ() - (pos.getZ() + 0.5);
        return Math.sqrt(dx * dx + dy * dy + dz * dz) <= 6.0;
    }

    private int excamate$getSafeExtraToolUses() {
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) return 0;
        if (!heldItem.isDamageableItem()) return Integer.MAX_VALUE;

        return Math.max(0, heldItem.getMaxDamage() - heldItem.getDamageValue() - 1);
    }

    private int excamate$getConfiguredExtraBlockLimit(@NotNull ExcaMateMode mode) {
        // The player breaks the first block normally; this cap applies to the extra blocks
        // ExcaMate may add during the hold-to-activate mining action. Caps are per-mode
        // so vein, branch, and excavation mining can be tuned independently.
        ExcaMateConfig config = ExcaMate.config;
        if (config == null) return 0;

        return switch (mode) {
            case VEIN -> config.veinMaxBlocks;
            case BRANCH_1X2 -> config.branchMaxBlocks;
            case EXCAVATE_3X3 -> config.excaMateMaxBlocks;
        };
    }

    private boolean excamate$isVeinMiningActive() {
        return ExcaMateNetworking.isVeinKeyHeld(player);
    }

    private boolean excamate$shouldAutoPickupExcaMateDrops(@NotNull BlockState sourceState) {
        ExcaMateConfig config = ExcaMate.config;
        return config != null
            && config.autoPickup
            && excamate$processingExcaMateBreak
            && excamate$isVeinMiningActive()
            && !config.isAutoPickupBlockBlocked(sourceState.getBlock())
            && (
                ExcaMateMiningRules.isDefaultSupportedBlock(sourceState)
                    || config.isExtraAutoPickupBlockAllowed(sourceState.getBlock())
            );
    }

    private void excamate$tryPlaceBranchTorch(@NotNull Direction facing) {
        ExcaMateConfig config = ExcaMate.config;
        if (config == null || !config.autoTorchInBranchMode) return;
        if (!excamate$isVeinMiningActive() || ExcaMateNetworking.getMode(player) != ExcaMateMode.BRANCH_1X2) return;

        ItemStack offhandStack = player.getOffhandItem();
        Block torchBlock = excamate$getTorchBlock(offhandStack);
        if (torchBlock == null) return;

        BlockPos playerFeet = player.blockPosition();
        if (excamate$tryPlaceTorchAt(playerFeet, torchBlock, offhandStack)) return;

        excamate$tryPlaceTorchAt(playerFeet.relative(facing.getOpposite()), torchBlock, offhandStack);
    }

    @Nullable
    private Block excamate$getTorchBlock(@NotNull ItemStack stack) {
        if (stack.is(Items.TORCH)) return Blocks.TORCH;
        if (stack.is(Items.SOUL_TORCH)) return Blocks.SOUL_TORCH;
        return null;
    }

    private boolean excamate$tryPlaceTorchAt(@NotNull BlockPos pos, @NotNull Block torchBlock, @NotNull ItemStack torchStack) {
        BlockState currentState = level.getBlockState(pos);
        if (currentState.is(torchBlock)) return true;
        if (!currentState.canBeReplaced() || !currentState.getFluidState().isEmpty()) return false;

        BlockState torchState = torchBlock.defaultBlockState();
        if (!torchState.canSurvive(level, pos)) return false;
        if (!level.setBlock(pos, torchState, Block.UPDATE_ALL)) return false;

        SoundType soundType = torchState.getSoundType();
        level.playSound(
            null,
            pos,
            soundType.getPlaceSound(),
            SoundSource.BLOCKS,
            (soundType.getVolume() + 1.0F) / 2.0F,
            soundType.getPitch() * 0.8F
        );

        if (!isCreative()) {
            torchStack.shrink(1);
        }

        return true;
    }

    private int excamate$stopXpCapture() {
        if (!excamate$capturingXp) return 0;

        excamate$capturingXp = false;
        return ExcaMateXpCapture.stop();
    }

    private void excamate$stashDropsNear(@NotNull BlockPos pos) {
        AABB pickupArea = new AABB(pos).inflate(1.0);
        List<@NotNull ItemEntity> itemEntities = level.getEntitiesOfClass(ItemEntity.class, pickupArea);

        for (ItemEntity itemEntity : itemEntities) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) continue;

            ItemStack remaining = stack.copy();
            if (player.getInventory().add(remaining)) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(remaining);
            }
        }

        player.getInventory().setChanged();
    }
}
