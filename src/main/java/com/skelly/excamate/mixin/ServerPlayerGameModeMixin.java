package com.skelly.excamate.mixin;

import com.skelly.excamate.ExcaMate;
import com.skelly.excamate.ExcaMateDropSweeper;
import com.skelly.excamate.ExcaMateMode;
import com.skelly.excamate.ExcaMateMiningRules;
import com.skelly.excamate.ExcaMateNetworking;
import com.skelly.excamate.ExcaMateXpCapture;
import com.skelly.excamate.config.ExcaMateConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.sounds.SoundSource;
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
    // Radius (in blocks) within which disconnected ore nodes of the same type are swept into the vein.
    private static final int ORE_STRAGGLER_SEARCH_RADIUS = 4;
    // Matches the vanilla hunger cost for breaking one block (ServerPlayerGameMode#destroyBlock
    // does not charge exhaustion itself in this version, so ExcaMate applies it manually).
    private static final float EXHAUSTION_PER_EXTRA_BLOCK = 0.005F;
    // Max distance from the player at which vein-connected blocks may be mined.
    // Prevents a vein from reaching blocks the player could not otherwise interact with.
    private static final double VEIN_MAX_REACH = 6.0;
    // Fixed pickup-sweep size for cascading plants (chorus, bamboo, sugar cane, etc.), anchored on
    // the block the player actually broke. Generous enough to cover any vanilla structure of these
    // types, since the real extent can't be measured after vanilla's own collapse has already run.
    private static final double CASCADE_SWEEP_HORIZONTAL_RADIUS = 6.0;
    private static final double CASCADE_SWEEP_VERTICAL_RADIUS = 32.0;
    // Vanilla cascades these structures one segment per tick, so the sweep needs to keep
    // running for a couple of seconds after the break, not just once.
    private static final int CASCADE_SWEEP_DURATION_TICKS = 40;

    // Pairs the pos and state of the block that triggered ExcaMate so the RETURN inject can find them.
    private record PendingBreak(@NotNull BlockPos pos, @NotNull BlockState state) {}
    // Pairs the pos and face direction from handleBlockBreakAction for use by excavation mode.
    private record LastHit(@NotNull BlockPos pos, @NotNull Direction direction) {}

    @Shadow protected @NotNull ServerLevel level;

    @Shadow @Final protected @NotNull ServerPlayer player;

    @Shadow public abstract boolean isCreative();

    @Shadow public abstract boolean destroyBlock(@NotNull BlockPos pos);

    private boolean excamate$breakingBlock;
    private boolean excamate$capturingXp;
    private boolean excamate$processingExcaMateBreak;
    @Nullable private PendingBreak excamate$pendingBreak;
    @Nullable private LastHit excamate$lastHit;

    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"))
    private void excamate$captureHitDirection(
        @NotNull BlockPos pos,
        Action action,
        @NotNull Direction direction,
        int worldHeight,
        int sequence,
        CallbackInfo ci
    ) {
        excamate$lastHit = new LastHit(pos, direction);
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void excamate$captureState(@NotNull BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ExcaMateConfig config = ExcaMate.config;
        if (excamate$breakingBlock || config == null) return;
        if (!excamate$isVeinMiningActive()) return;
        if (isCreative()) return;

        @NotNull BlockState state = level.getBlockState(pos);
        if (state.isAir() || !excamate$canMineWithExcaMate(pos, state, config)) return;

        excamate$pendingBreak = new PendingBreak(pos, state);
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
            PendingBreak pending = excamate$pendingBreak;
            if (pending != null && pending.pos().equals(pos)) {
                excamate$pendingBreak = null;
                excamate$clearHitDirection(pos);
                excamate$processingExcaMateBreak = false;
                excamate$stopXpCapture();
            }
            return;
        }
        ExcaMateConfig config = ExcaMate.config;
        if (excamate$breakingBlock || config == null) return;
        if (isCreative()) return;
        PendingBreak pending = excamate$pendingBreak;
        if (pending == null || !pending.pos().equals(pos)) {
            excamate$processingExcaMateBreak = false;
            excamate$stopXpCapture();
            return;
        }

        excamate$pendingBreak = null;
        BlockState brokenState = pending.state();

        int capturedXp;
        try {
            excamate$tryReplantAfterBreak(pos, brokenState);

            // excamate$mineExcaMateBlocks does its own post-batch pickup sweep (covering
            // this block plus any extras), gated the same way shouldAutoPickupExcaMateDrops is.
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
        // Tracks every position broken this swing (the initial block plus any extras) so a single
        // pickup sweep can cover the whole worked area afterward. Some plants (chorus, bamboo, sugar
        // cane) cascade-break and drop items away from where they were actually targeted, so sweeping
        // only around each individually-broken position misses those scattered drops.
        List<@NotNull BlockPos> minedPositions = new ArrayList<>();
        minedPositions.add(startPos);

        ExcaMateMode mode = ExcaMateNetworking.getMode(player);
        int maxBlocks = Math.min(excamate$getConfiguredExtraBlockLimit(mode), excamate$getSafeExtraToolUses());

        if (maxBlocks > 0) {
            // Branch mode breaks blocks step-by-step so it can place torches after each pair;
            // Vein and Excavate collect all positions first then break them in one pass.
            if (mode == ExcaMateMode.BRANCH_1X2) {
                excamate$mineBranchTunnel(startState, maxBlocks, minedPositions);
            } else {
                // The selected mode is universal; strict tool checks decide which blocks each held tool may include.
                List<@NotNull BlockPos> blocksToMine = switch (mode) {
                    case VEIN -> excamate$collectVeinModeBlocks(startPos, startState, maxBlocks);
                    case EXCAVATE_3X3 -> excamate$collectExcavationVolume(startPos, startState, maxBlocks);
                    default -> List.of();
                };
                excamate$breakCollectedBlocks(blocksToMine, minedPositions);
            }
        }

        if (excamate$shouldAutoPickupExcaMateDrops(startState)) {
            if (ExcaMateMiningRules.isCascadingPlantBlock(startState)) {
                // Vanilla schedules each cascading segment's destruction a tick after the one
                // below/beside it breaks, so a tall structure collapses one link per tick rather
                // than all at once. A single sweep right now only catches whatever has already
                // dropped; ExcaMateDropSweeper re-sweeps the same area for the next couple of
                // seconds so later waves of the cascade get picked up too.
                AABB sweepArea = excamate$cascadeSweepArea(startPos);
                excamate$stashDropsIn(sweepArea);
                ExcaMateDropSweeper.schedule(player, sweepArea, CASCADE_SWEEP_DURATION_TICKS);
            } else {
                excamate$stashDropsAround(minedPositions);
            }
        }
    }

    private static @NotNull AABB excamate$cascadeSweepArea(@NotNull BlockPos startPos) {
        return new AABB(startPos).inflate(CASCADE_SWEEP_HORIZONTAL_RADIUS, CASCADE_SWEEP_VERTICAL_RADIUS, CASCADE_SWEEP_HORIZONTAL_RADIUS);
    }

    private List<@NotNull BlockPos> excamate$collectVeinModeBlocks(@NotNull BlockPos startPos, @NotNull BlockState startState, int maxBlocks) {
        List<BlockPos> blocksToMine = excamate$collectConnectedVein(startPos, startState, maxBlocks);
        if (ExcaMateMiningRules.isOreBlock(startState) && blocksToMine.size() < maxBlocks) {
            excamate$collectOreStragglers(startPos, startState, blocksToMine, maxBlocks);
        }

        return blocksToMine;
    }

    private void excamate$mineBranchTunnel(@NotNull BlockState startState, int maxBlocks, @NotNull List<@NotNull BlockPos> minedPositions) {
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
                minedPositions.add(lower);
                extraBlocksBroken++;
                brokeBlockThisStep = true;
            }

            BlockPos upper = lower.above();
            if (extraBlocksBroken < maxBlocks
                && excamate$getSafeExtraToolUses() > 0
                && excamate$isValidPatternBlock(upper, startState)
                && excamate$breakExcaMateBlock(upper)) {
                minedPositions.add(upper);
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

    private List<@NotNull BlockPos> excamate$collectExcavationVolume(@NotNull BlockPos startPos, @NotNull BlockState startState, int maxBlocks) {
        List<@NotNull BlockPos> blocksToMine = new ArrayList<>();
        Direction hitDirection = excamate$getHitDirectionFor(startPos);
        Direction depthDirection = hitDirection.getOpposite();

        for (int depth = 0; depth < 3 && blocksToMine.size() < maxBlocks; depth++) {
            BlockPos layerCenter = startPos.relative(depthDirection, depth);
            for (int firstAxis = -1; firstAxis <= 1 && blocksToMine.size() < maxBlocks; firstAxis++) {
                for (int secondAxis = -1; secondAxis <= 1 && blocksToMine.size() < maxBlocks; secondAxis++) {
                    BlockPos candidate = excamate$getExcavationVolumePos(layerCenter, depthDirection, firstAxis, secondAxis);
                    if (candidate.equals(startPos)) continue;
                    if (excamate$isValidPatternBlock(candidate, startState)) {
                        blocksToMine.add(candidate);
                    }
                }
            }
        }

        return blocksToMine;
    }

    private Direction excamate$getHitDirectionFor(@NotNull BlockPos startPos) {
        LastHit lastHit = excamate$lastHit;
        if (lastHit != null && lastHit.pos().equals(startPos)) {
            return lastHit.direction();
        }

        return player.getDirection();
    }

    private void excamate$clearHitDirection(@NotNull BlockPos pos) {
        LastHit lastHit = excamate$lastHit;
        if (lastHit != null && lastHit.pos().equals(pos)) {
            excamate$lastHit = null;
        }
    }

    private BlockPos excamate$getExcavationVolumePos(@NotNull BlockPos center, @NotNull Direction depthDirection, int firstAxis, int secondAxis) {
        return switch (depthDirection.getAxis()) {
            case Y -> center.offset(firstAxis, 0, secondAxis);
            case Z -> center.offset(firstAxis, secondAxis, 0);
            case X -> center.offset(0, secondAxis, firstAxis);
        };
    }

    private void excamate$breakCollectedBlocks(List<@NotNull BlockPos> blocksToMine, @NotNull List<@NotNull BlockPos> minedPositions) {
        int extraBlocksBroken = 0;

        // Extra blocks still go through vanilla destroyBlock/playerDestroy so drops, Fortune,
        // Silk Touch, sounds, and durability stay vanilla. The Block XP mixin only converts
        // vanilla-calculated XP orb amounts into direct XP while the outer capture is active.
        for (BlockPos blockPos : blocksToMine) {
            if (!excamate$isVeinMiningActive() || excamate$getSafeExtraToolUses() <= 0) break;

            if (excamate$breakExcaMateBlock(blockPos)) {
                minedPositions.add(blockPos);
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
            if (destroyed) {
                excamate$tryReplantAfterBreak(blockPos, state);
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
            && (ExcaMateMiningRules.isWoodBlock(startState)
                || ExcaMateMiningRules.isTallPlantBlock(startState)
                || excamate$isInMiningRange(pos));
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
        return excamate$isNeverMineableBlock(state)
            && ExcaMateMiningRules.canMineWithExcaMate(player, state, config);
    }

    // Blocks that can never be vein-mined regardless of the allow list.
    // Functional/container blocks (chests, barrels, etc.) are not listed here;
    // they are simply absent from the default block lists and require explicit allow-listing.
    private boolean excamate$isNeverMineableBlock(@NotNull BlockState state) {
        return !state.is(Blocks.BEDROCK)
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
        return Math.sqrt(dx * dx + dy * dy + dz * dz) <= VEIN_MAX_REACH;
    }

    private int excamate$getSafeExtraToolUses() {
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) return 0;
        if (!heldItem.isDamageableItem()) return Integer.MAX_VALUE;

        return Math.max(0, heldItem.getMaxDamage() - heldItem.getDamageValue() - 1);
    }

    private int excamate$getConfiguredExtraBlockLimit(@NotNull ExcaMateMode mode) {
        // Configured caps represent the total action size, including the first block
        // broken by vanilla before ExcaMate adds any extra blocks.
        ExcaMateConfig config = ExcaMate.config;
        if (config == null) return 0;

        int totalBlockLimit = switch (mode) {
            case VEIN -> config.veinMaxBlocks;
            case BRANCH_1X2 -> config.branchMaxBlocks;
            case EXCAVATE_3X3 -> config.excaMateMaxBlocks;
        };

        return Math.max(0, totalBlockLimit - 1);
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
            && !config.isAutoPickupBlockBlocked(sourceState.getBlock());
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
    private Block excamate$getCropBlockForSeed(@NotNull ItemStack stack) {
        if (stack.is(Items.WHEAT_SEEDS)) return Blocks.WHEAT;
        if (stack.is(Items.CARROT)) return Blocks.CARROTS;
        if (stack.is(Items.POTATO)) return Blocks.POTATOES;
        if (stack.is(Items.BEETROOT_SEEDS)) return Blocks.BEETROOTS;
        if (stack.is(Items.NETHER_WART)) return Blocks.NETHER_WART;
        if (stack.is(Items.TORCHFLOWER_SEEDS)) return Blocks.TORCHFLOWER_CROP;
        if (stack.is(Items.PITCHER_POD)) return Blocks.PITCHER_CROP;
        return null;
    }

    private void excamate$tryReplantAfterBreak(@NotNull BlockPos pos, @NotNull BlockState brokenState) {
        ExcaMateConfig config = ExcaMate.config;
        if (config == null || !config.autoReplantCrops) return;
        if (!ExcaMateMiningRules.isPlantBlock(brokenState)) return;

        ItemStack offhandStack = player.getOffhandItem();
        Block cropBlock = excamate$getCropBlockForSeed(offhandStack);
        if (cropBlock == null) return;

        BlockState cropState = cropBlock.defaultBlockState();
        if (!level.getBlockState(pos).isAir()) return;
        if (!cropState.canSurvive(level, pos)) return;
        if (!level.setBlock(pos, cropState, Block.UPDATE_ALL)) return;

        SoundType soundType = cropState.getSoundType();
        level.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS,
            (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);

        if (!isCreative()) {
            offhandStack.shrink(1);
        }
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

    // Sweeps one bounding box covering every position broken this swing, rather than a separate
    // 1-block box per position. Plants like chorus, bamboo, and sugar cane cascade-break when their
    // support is removed, dropping items at the cascaded block's own position — which may be well
    // away from any position ExcaMate actually targeted — so a wide single sweep is needed to catch them.
    private void excamate$stashDropsAround(@NotNull List<@NotNull BlockPos> positions) {
        if (positions.isEmpty()) return;

        AABB pickupArea = new AABB(positions.get(0));
        for (int i = 1; i < positions.size(); i++) {
            pickupArea = pickupArea.minmax(new AABB(positions.get(i)));
        }

        excamate$stashDropsIn(pickupArea.inflate(1.0));
    }

    private void excamate$stashDropsIn(@NotNull AABB area) {
        List<@NotNull ItemEntity> itemEntities = level.getEntitiesOfClass(ItemEntity.class, area);

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
