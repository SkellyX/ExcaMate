package com.skelly.excamate;

import com.skelly.excamate.config.ExcaMateConfig;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopperCollection;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ExcaMateMiningRules {

    private static Stream<Block> copperStates(WeatheringCopperCollection<Block> collection) {
        WeatheringCopperCollection.ByState<Block> states = collection.weathering();
        return Stream.of(states.unaffected(), states.exposed(), states.weathered(), states.oxidized());
    }

    private static final Set<@NotNull Block> STONE_BLOCKS = Stream.concat(
        Stream.of(
        Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.DEEPSLATE,
        Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS,
        Blocks.CRACKED_STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS, Blocks.COAL_ORE,
        Blocks.COPPER_ORE, Blocks.IRON_ORE, Blocks.GOLD_ORE, Blocks.LAPIS_ORE, Blocks.DIAMOND_ORE,
        Blocks.REDSTONE_ORE, Blocks.EMERALD_ORE, Blocks.NETHER_QUARTZ_ORE, Blocks.OBSIDIAN,
        Blocks.BASALT, Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS,
        Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS, Blocks.DEEPSLATE_COAL_ORE, Blocks.DEEPSLATE_COPPER_ORE,
        Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
        Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
        Blocks.RAW_IRON_BLOCK, Blocks.RAW_COPPER_BLOCK, Blocks.RAW_GOLD_BLOCK,
        Blocks.IRON_BLOCK, Blocks.GOLD_BLOCK, Blocks.LAPIS_BLOCK, Blocks.REDSTONE_BLOCK,
        Blocks.DIAMOND_BLOCK, Blocks.EMERALD_BLOCK, Blocks.COAL_BLOCK, Blocks.AMETHYST_BLOCK,
        // Nether
        Blocks.NETHERRACK, Blocks.MAGMA_BLOCK,
        Blocks.NETHER_BRICKS, Blocks.CRACKED_NETHER_BRICKS, Blocks.CHISELED_NETHER_BRICKS, Blocks.RED_NETHER_BRICKS,
        Blocks.NETHER_BRICK_SLAB, Blocks.NETHER_BRICK_STAIRS, Blocks.NETHER_BRICK_WALL, Blocks.NETHER_BRICK_FENCE,
        Blocks.RED_NETHER_BRICK_SLAB, Blocks.RED_NETHER_BRICK_STAIRS, Blocks.RED_NETHER_BRICK_WALL,
        // The End
        Blocks.END_STONE, Blocks.END_STONE_BRICKS,
        Blocks.END_STONE_BRICK_SLAB, Blocks.END_STONE_BRICK_STAIRS, Blocks.END_STONE_BRICK_WALL,
        Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR, Blocks.PURPUR_SLAB, Blocks.PURPUR_STAIRS,
        // Cave
        Blocks.TUFF, Blocks.CHISELED_TUFF, Blocks.POLISHED_TUFF, Blocks.TUFF_BRICKS,
        Blocks.TUFF_SLAB, Blocks.TUFF_STAIRS, Blocks.TUFF_WALL,
        Blocks.TUFF_BRICK_SLAB, Blocks.TUFF_BRICK_STAIRS, Blocks.TUFF_BRICK_WALL,
        Blocks.POLISHED_TUFF_SLAB, Blocks.POLISHED_TUFF_STAIRS, Blocks.POLISHED_TUFF_WALL,
        Blocks.CALCITE, Blocks.DRIPSTONE_BLOCK,
        // Sandstone
        Blocks.SANDSTONE, Blocks.CHISELED_SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.CUT_SANDSTONE,
        Blocks.RED_SANDSTONE, Blocks.CHISELED_RED_SANDSTONE, Blocks.SMOOTH_RED_SANDSTONE, Blocks.CUT_RED_SANDSTONE,
        Blocks.SANDSTONE_SLAB, Blocks.SANDSTONE_STAIRS, Blocks.SANDSTONE_WALL,
        Blocks.RED_SANDSTONE_SLAB, Blocks.RED_SANDSTONE_STAIRS, Blocks.RED_SANDSTONE_WALL,
        Blocks.SMOOTH_SANDSTONE_SLAB, Blocks.SMOOTH_SANDSTONE_STAIRS,
        Blocks.CUT_SANDSTONE_SLAB, Blocks.CUT_RED_SANDSTONE_SLAB,
        Blocks.SMOOTH_RED_SANDSTONE_SLAB, Blocks.SMOOTH_RED_SANDSTONE_STAIRS,
        // Blackstone variants
        Blocks.CHISELED_POLISHED_BLACKSTONE, Blocks.GILDED_BLACKSTONE,
        Blocks.BLACKSTONE_SLAB, Blocks.BLACKSTONE_STAIRS, Blocks.BLACKSTONE_WALL,
        Blocks.POLISHED_BLACKSTONE_SLAB, Blocks.POLISHED_BLACKSTONE_STAIRS, Blocks.POLISHED_BLACKSTONE_WALL,
        Blocks.POLISHED_BLACKSTONE_BRICK_SLAB, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Blocks.POLISHED_BLACKSTONE_BRICK_WALL,
        // Nether terrain
        Blocks.CRIMSON_NYLIUM, Blocks.WARPED_NYLIUM,
        Blocks.POLISHED_BASALT, Blocks.SMOOTH_BASALT, Blocks.BONE_BLOCK,
        // The End
        Blocks.CRYING_OBSIDIAN
        ),
        Stream.concat(copperStates(Blocks.COPPER_BLOCK), copperStates(Blocks.CUT_COPPER))
    ).collect(Collectors.toUnmodifiableSet());

    private static final Set<@NotNull Block> ORE_BLOCKS = Set.of(
        Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
        Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
        Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
        Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE,
        Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
        Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
        Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
        Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
        Blocks.NETHER_QUARTZ_ORE,
        Blocks.ANCIENT_DEBRIS
    );

    private static final Set<@NotNull Block> SOIL_BLOCKS = Set.of(
        Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.PODZOL, Blocks.MYCELIUM, Blocks.GRAVEL,
        Blocks.SAND, Blocks.RED_SAND, Blocks.CLAY,
        Blocks.SOUL_SAND, Blocks.SOUL_SOIL, Blocks.MUD, Blocks.SNOW_BLOCK
    );

    // Tall, single-column plants (like trees) where a vein should reach the whole stalk regardless of player distance.
    private static final Set<@NotNull Block> TALL_PLANT_BLOCKS = Set.of(
        Blocks.SUGAR_CANE, Blocks.CACTUS, Blocks.BAMBOO,
        Blocks.KELP, Blocks.KELP_PLANT,
        Blocks.TWISTING_VINES, Blocks.TWISTING_VINES_PLANT,
        Blocks.WEEPING_VINES, Blocks.WEEPING_VINES_PLANT
    );


    private static final Set<@NotNull Block> PLANT_BLOCKS = Set.of(
        // Crops
        Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS,
        Blocks.NETHER_WART, Blocks.COCOA, Blocks.TORCHFLOWER_CROP, Blocks.PITCHER_CROP,
        // Tall plants
        Blocks.SUGAR_CANE, Blocks.CACTUS, Blocks.BAMBOO,
        Blocks.KELP, Blocks.KELP_PLANT,
        Blocks.TWISTING_VINES, Blocks.TWISTING_VINES_PLANT,
        Blocks.WEEPING_VINES, Blocks.WEEPING_VINES_PLANT,
        // Sculk (deep dark — hoe is the standard clearing tool in vanilla)
        Blocks.SCULK, Blocks.SCULK_CATALYST, Blocks.SCULK_SENSOR, Blocks.SCULK_SHRIEKER
    );

    private static final Set<@NotNull Block> WOOD_BLOCKS = Set.of(
        Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.JUNGLE_LOG, Blocks.ACACIA_LOG,
        Blocks.DARK_OAK_LOG, Blocks.MANGROVE_LOG, Blocks.CHERRY_LOG, Blocks.BAMBOO_BLOCK,
        Blocks.OAK_WOOD, Blocks.SPRUCE_WOOD, Blocks.BIRCH_WOOD, Blocks.JUNGLE_WOOD, Blocks.ACACIA_WOOD,
        Blocks.DARK_OAK_WOOD, Blocks.MANGROVE_WOOD, Blocks.CHERRY_WOOD, Blocks.STRIPPED_OAK_LOG,
        Blocks.STRIPPED_SPRUCE_LOG, Blocks.STRIPPED_BIRCH_LOG, Blocks.STRIPPED_JUNGLE_LOG,
        Blocks.STRIPPED_ACACIA_LOG, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.STRIPPED_MANGROVE_LOG,
        Blocks.STRIPPED_CHERRY_LOG, Blocks.STRIPPED_OAK_WOOD, Blocks.STRIPPED_SPRUCE_WOOD,
        Blocks.STRIPPED_BIRCH_WOOD, Blocks.STRIPPED_JUNGLE_WOOD, Blocks.STRIPPED_ACACIA_WOOD,
        Blocks.STRIPPED_DARK_OAK_WOOD, Blocks.STRIPPED_MANGROVE_WOOD, Blocks.STRIPPED_CHERRY_WOOD,
        Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS, Blocks.JUNGLE_PLANKS,
        Blocks.ACACIA_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.MANGROVE_PLANKS, Blocks.CHERRY_PLANKS,
        Blocks.BAMBOO_PLANKS, Blocks.OAK_SLAB, Blocks.SPRUCE_SLAB, Blocks.BIRCH_SLAB, Blocks.JUNGLE_SLAB,
        Blocks.ACACIA_SLAB, Blocks.DARK_OAK_SLAB, Blocks.MANGROVE_SLAB, Blocks.CHERRY_SLAB, Blocks.BAMBOO_SLAB,
        Blocks.OAK_STAIRS, Blocks.SPRUCE_STAIRS, Blocks.BIRCH_STAIRS, Blocks.JUNGLE_STAIRS,
        Blocks.ACACIA_STAIRS, Blocks.DARK_OAK_STAIRS, Blocks.MANGROVE_STAIRS, Blocks.CHERRY_STAIRS,
        Blocks.BAMBOO_STAIRS, Blocks.OAK_FENCE, Blocks.SPRUCE_FENCE, Blocks.BIRCH_FENCE, Blocks.JUNGLE_FENCE,
        Blocks.ACACIA_FENCE, Blocks.DARK_OAK_FENCE, Blocks.MANGROVE_FENCE, Blocks.CHERRY_FENCE,
        Blocks.OAK_FENCE_GATE, Blocks.SPRUCE_FENCE_GATE, Blocks.BIRCH_FENCE_GATE, Blocks.JUNGLE_FENCE_GATE,
        Blocks.ACACIA_FENCE_GATE, Blocks.DARK_OAK_FENCE_GATE, Blocks.MANGROVE_FENCE_GATE, Blocks.CHERRY_FENCE_GATE,
        Blocks.BOOKSHELF, Blocks.CRAFTING_TABLE, Blocks.COMPOSTER, Blocks.LECTERN,
        Blocks.JUKEBOX, Blocks.NOTE_BLOCK, Blocks.BEEHIVE, Blocks.BEE_NEST,
        Blocks.CHORUS_PLANT,
        // Nether wood
        Blocks.CRIMSON_STEM, Blocks.WARPED_STEM, Blocks.STRIPPED_CRIMSON_STEM, Blocks.STRIPPED_WARPED_STEM,
        Blocks.CRIMSON_HYPHAE, Blocks.WARPED_HYPHAE, Blocks.STRIPPED_CRIMSON_HYPHAE, Blocks.STRIPPED_WARPED_HYPHAE,
        Blocks.CRIMSON_PLANKS, Blocks.WARPED_PLANKS,
        Blocks.CRIMSON_SLAB, Blocks.WARPED_SLAB, Blocks.CRIMSON_STAIRS, Blocks.WARPED_STAIRS,
        Blocks.CRIMSON_FENCE, Blocks.WARPED_FENCE, Blocks.CRIMSON_FENCE_GATE, Blocks.WARPED_FENCE_GATE,
        // Mushroom
        Blocks.MUSHROOM_STEM, Blocks.RED_MUSHROOM_BLOCK, Blocks.BROWN_MUSHROOM_BLOCK
    );

    private ExcaMateMiningRules() {
    }

    public static boolean isVeinMineableBlock(@NotNull BlockState state) {
        return isPickaxeMineableBlock(state) || isOreBlock(state) || isSoilBlock(state) || isWoodBlock(state) || isPlantBlock(state);
    }

    public static boolean isDefaultSupportedBlock(@NotNull BlockState state) {
        return isVeinMineableBlock(state) && state.getBlock() != Blocks.ANCIENT_DEBRIS;
    }

    public static boolean canMineWithExcaMate(
        @NotNull Player player,
        @NotNull BlockState state,
        @NotNull ExcaMateConfig config
    ) {
        Block block = state.getBlock();
        if (config.isVeinMineBlockBlocked(block)) return false;
        boolean defaultSupported = isDefaultSupportedBlock(state);
        boolean extraSupported = config.isExtraVeinMineBlockAllowed(block);
        if (!defaultSupported && !extraSupported) return false;

        return canUseCorrectToolForBlock(player, state);
    }

    public static boolean isPlantBlock(@NotNull BlockState state) {
        return PLANT_BLOCKS.contains(state.getBlock());
    }

    public static boolean isTallPlantBlock(@NotNull BlockState state) {
        return TALL_PLANT_BLOCKS.contains(state.getBlock());
    }

    // Plants that vanilla connects via a support chain: removing one link collapses every
    // disconnected piece above/around it, each dropping items at its own position rather than
    // wherever the player actually broke a block. Auto-pickup needs to sweep the whole connected
    // structure for these, not just the positions ExcaMate itself broke.
    public static boolean isCascadingPlantBlock(@NotNull BlockState state) {
        return isTallPlantBlock(state) || state.getBlock() == Blocks.CHORUS_PLANT;
    }

    public static boolean isWoodBlock(@NotNull BlockState state) {
        return WOOD_BLOCKS.contains(state.getBlock());
    }

    public static boolean isOreBlock(@NotNull BlockState state) {
        return oreType(state) != OreType.NONE;
    }

    public static boolean isSameOreType(@NotNull BlockState first, @NotNull BlockState second) {
        OreType firstType = oreType(first);
        OreType secondType = oreType(second);
        if (firstType != OreType.NONE || secondType != OreType.NONE) {
            return firstType == secondType && firstType != OreType.NONE;
        }

        return isOreBlock(first) && first.getBlock() == second.getBlock();
    }

    public static boolean canUseCorrectToolForBlock(@NotNull Player player, @NotNull BlockState state) {
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) return false;

        if (isPickaxeMineableBlock(state) || isOreBlock(state)) {
            if (heldItem.is(ItemTags.PICKAXES) && heldItem.isCorrectToolForDrops(state)) return true;
        }

        if (isWoodBlock(state)) {
            if (heldItem.is(ItemTags.AXES) && heldItem.isCorrectToolForDrops(state)) return true;
        }

        if (isSoilBlock(state)) {
            if (heldItem.is(ItemTags.SHOVELS) && heldItem.isCorrectToolForDrops(state)) return true;
        }

        if (isPlantBlock(state)) {
            // Crops/tall plants aren't in vanilla's mineable/hoe tag (only leaves, hay, sculk, etc. are),
            // so isCorrectToolForDrops would reject them — the hoe is ExcaMate's own plant-category tool, not vanilla's.
            if (heldItem.is(ItemTags.HOES)) return true;
            // Bamboo is instantly broken by swords in vanilla — sword-specific carve-out
            if (state.getBlock() == Blocks.BAMBOO && heldItem.is(ItemTags.SWORDS)) return true;
        }

        return false;
    }

    private static boolean isPickaxeMineableBlock(@NotNull BlockState state) {
        return STONE_BLOCKS.contains(state.getBlock());
    }

    private static boolean isSoilBlock(@NotNull BlockState state) {
        return SOIL_BLOCKS.contains(state.getBlock());
    }

    private static OreType oreType(@NotNull BlockState state) {
        return oreType(state.getBlock());
    }

    private static OreType oreType(@NotNull Block block) {
        if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) return OreType.COAL;
        if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) return OreType.COPPER;
        if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) return OreType.IRON;
        if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE) return OreType.GOLD;
        if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) return OreType.LAPIS;
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) return OreType.DIAMOND;
        if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) return OreType.REDSTONE;
        if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) return OreType.EMERALD;
        if (block == Blocks.NETHER_QUARTZ_ORE) return OreType.QUARTZ;
        if (block == Blocks.ANCIENT_DEBRIS) return OreType.ANCIENT_DEBRIS;
        return OreType.NONE;
    }

    private enum OreType {
        NONE,
        COAL,
        COPPER,
        IRON,
        GOLD,
        LAPIS,
        DIAMOND,
        REDSTONE,
        EMERALD,
        QUARTZ,
        ANCIENT_DEBRIS
    }
}
