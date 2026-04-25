package com.skelly.deepstash;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class ExcaMateMiningRules {
    // Common tags let correctly-tagged modded ores participate even when Minecraft has no vanilla ore-family tag for them.
    private static final TagKey<@NotNull Block> COMMON_ORES = TagKey.create(
        Registries.BLOCK,
        Identifier.fromNamespaceAndPath("c", "ores")
    );

    private static final Set<@NotNull Block> STONE_BLOCKS = Set.of(
        Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.DEEPSLATE,
        Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS,
        Blocks.CRACKED_STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS, Blocks.COAL_ORE,
        Blocks.COPPER_ORE, Blocks.IRON_ORE, Blocks.GOLD_ORE, Blocks.LAPIS_ORE, Blocks.DIAMOND_ORE,
        Blocks.REDSTONE_ORE, Blocks.EMERALD_ORE, Blocks.NETHER_QUARTZ_ORE, Blocks.OBSIDIAN,
        Blocks.BASALT, Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS,
        Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS, Blocks.DEEPSLATE_COAL_ORE, Blocks.DEEPSLATE_COPPER_ORE,
        Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
        Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
        Blocks.RAW_IRON_BLOCK, Blocks.RAW_COPPER_BLOCK, Blocks.RAW_GOLD_BLOCK, Blocks.COPPER_BLOCK,
        Blocks.EXPOSED_COPPER, Blocks.WEATHERED_COPPER, Blocks.OXIDIZED_COPPER, Blocks.CUT_COPPER,
        Blocks.EXPOSED_CUT_COPPER, Blocks.WEATHERED_CUT_COPPER, Blocks.OXIDIZED_CUT_COPPER,
        Blocks.IRON_BLOCK, Blocks.GOLD_BLOCK, Blocks.LAPIS_BLOCK, Blocks.REDSTONE_BLOCK,
        Blocks.DIAMOND_BLOCK, Blocks.EMERALD_BLOCK, Blocks.COAL_BLOCK, Blocks.AMETHYST_BLOCK
    );

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
        Blocks.SAND, Blocks.RED_SAND, Blocks.CLAY
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
        Blocks.BOOKSHELF, Blocks.CRAFTING_TABLE, Blocks.CHEST, Blocks.TRAPPED_CHEST,
        Blocks.BARREL, Blocks.COMPOSTER, Blocks.LECTERN, Blocks.JUKEBOX, Blocks.NOTE_BLOCK,
        Blocks.BEEHIVE, Blocks.BEE_NEST
    );

    private ExcaMateMiningRules() {
    }

    public static boolean isVeinMineableBlock(@NotNull BlockState state) {
        return isPickaxeMineableBlock(state) || isOreBlock(state) || isSoilBlock(state) || isWoodBlock(state);
    }

    public static boolean isWoodBlock(@NotNull BlockState state) {
        return isTagged(state, BlockTags.LOGS) || WOOD_BLOCKS.contains(state.getBlock());
    }

    public static boolean isOreBlock(@NotNull BlockState state) {
        return oreType(state) != OreType.NONE || isTagged(state, COMMON_ORES) || ORE_BLOCKS.contains(state.getBlock());
    }

    public static boolean isSameOreType(@NotNull BlockState first, @NotNull BlockState second) {
        OreType firstType = oreType(first);
        OreType secondType = oreType(second);
        if (firstType != OreType.NONE || secondType != OreType.NONE) {
            return firstType == secondType && firstType != OreType.NONE;
        }

        return isOreBlock(first) && first.getBlock() == second.getBlock();
    }

    public static boolean canUseCorrectTool(@NotNull Player player, @NotNull BlockState state) {
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) return false;

        if (isWoodBlock(state)) return heldItem.is(ItemTags.AXES);
        if (isSoilBlock(state)) return heldItem.is(ItemTags.SHOVELS);
        if (isOreBlock(state) || isPickaxeMineableBlock(state)) {
            return heldItem.is(ItemTags.PICKAXES) && heldItem.isCorrectToolForDrops(state);
        }

        return false;
    }

    private static boolean isPickaxeMineableBlock(@NotNull BlockState state) {
        return isTagged(state, BlockTags.MINEABLE_WITH_PICKAXE) || STONE_BLOCKS.contains(state.getBlock());
    }

    private static boolean isSoilBlock(@NotNull BlockState state) {
        return isTagged(state, BlockTags.MINEABLE_WITH_SHOVEL) || SOIL_BLOCKS.contains(state.getBlock());
    }

    private static boolean isTagged(@NotNull BlockState state, TagKey<@NotNull Block> tag) {
        return state.typeHolder().is(tag);
    }

    private static OreType oreType(@NotNull BlockState state) {
        if (isTagged(state, BlockTags.COAL_ORES)) return OreType.COAL;
        if (isTagged(state, BlockTags.COPPER_ORES)) return OreType.COPPER;
        if (isTagged(state, BlockTags.IRON_ORES)) return OreType.IRON;
        if (isTagged(state, BlockTags.GOLD_ORES)) return OreType.GOLD;
        if (isTagged(state, BlockTags.LAPIS_ORES)) return OreType.LAPIS;
        if (isTagged(state, BlockTags.DIAMOND_ORES)) return OreType.DIAMOND;
        if (isTagged(state, BlockTags.REDSTONE_ORES)) return OreType.REDSTONE;
        if (isTagged(state, BlockTags.EMERALD_ORES)) return OreType.EMERALD;

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
