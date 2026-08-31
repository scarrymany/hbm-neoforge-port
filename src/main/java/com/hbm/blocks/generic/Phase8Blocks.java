package com.hbm.blocks.generic;

import com.hbm.blocks.BlockBase;
import com.hbm.blocks.BlockFallingBase;
import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Phase 8 block families required by CE {@code .nbt} palettes ({@code StructureManager.java} +
 * {@code assets/hbm/structures/*.nbt}) and the leftover brick/concrete/stairs/slab table in
 * {@code ModBlocks.java}:496-504 / 86-210. Stairs use {@link Blocks#STONE} as the {@link
 * net.minecraft.world.level.block.StairBlock} base state so we never call {@code DeferredBlock#get()}
 * from a static registrar (CE {@code BlockGenericStairs} took the live parent block).
 */
public final class Phase8Blocks {

    public static Supplier<BlockEntityType<BlockWandLoot.WandLootBlockEntity>> WAND_LOOT_ENTITY_TYPE;

    private Phase8Blocks() {
    }

    public static void registerAll() {
        registerDecoMetals();
        registerBrickConcrete();
        registerMeteorBricks();
        registerStairs();
        registerSlabs();
        registerWandLoot();
        registerBlock("block_electrical_scrap",
                () -> new BlockFallingBase(BlockBehaviour.Properties.of().strength(2.5F, 5.0F).sound(SoundType.METAL)),
                ModCreativeTabs.BLOCKS);
    }

    /** CE ModBlocks.java:497-504 — deco_* storage cubes used by vertibird / radio_house palettes. */
    private static void registerDecoMetals() {
        BlockBehaviour.Properties metal = BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL);
        registerBlock("deco_titanium", () -> new BlockBase(metal), ModCreativeTabs.BLOCKS);
        registerBlock("deco_red_copper", () -> new BlockBase(metal), ModCreativeTabs.BLOCKS);
        registerBlock("deco_tungsten", () -> new BlockBase(metal), ModCreativeTabs.BLOCKS);
        registerBlock("deco_aluminium", () -> new BlockBase(metal), ModCreativeTabs.BLOCKS);
        registerBlock("deco_rusty_steel", () -> new BlockBase(metal), ModCreativeTabs.BLOCKS);
        registerBlock("deco_lead", () -> new BlockBase(metal), ModCreativeTabs.BLOCKS);
        registerBlock("deco_beryllium", () -> new BlockBase(metal), ModCreativeTabs.BLOCKS);
        registerBlock("deco_asbestos", () -> new BlockBase(metal), ModCreativeTabs.BLOCKS);
    }

    /** CE ModBlocks.java:86-118 — missing brick/concrete full blocks (siblings of already-ported brick_concrete). */
    private static void registerBrickConcrete() {
        registerBlock("reinforced_stone", () -> new BlockBase(stone(15.0F, 100.0F)), ModCreativeTabs.BLOCKS);
        registerBlock("brick_concrete_cracked", () -> new BlockBase(stone(15.0F, 60.0F)), ModCreativeTabs.BLOCKS);
        registerBlock("brick_concrete_broken", () -> new BlockBase(stone(15.0F, 45.0F)), ModCreativeTabs.BLOCKS);
        registerBlock("brick_light", () -> new BlockBase(stone(15.0F, 20.0F)), ModCreativeTabs.BLOCKS);
        registerBlock("brick_asbestos", () -> new BlockBase(stone(15.0F, 40.0F)), ModCreativeTabs.BLOCKS);
        registerBlock("brick_obsidian", () -> new BlockBase(stone(15.0F, 120.0F)), ModCreativeTabs.BLOCKS);
        registerBlock("cmb_brick", () -> new BlockBase(BlockBehaviour.Properties.of().strength(25.0F, 5000.0F).sound(SoundType.METAL)), ModCreativeTabs.BLOCKS);
        registerBlock("concrete", () -> new BlockBase(stone(15.0F, 140.0F), true), ModCreativeTabs.BLOCKS);
        registerBlock("concrete_smooth", () -> new BlockBase(stone(15.0F, 140.0F), true), ModCreativeTabs.BLOCKS);
        registerBlock("concrete_asbestos", () -> new BlockBase(stone(15.0F, 1500.0F), true), ModCreativeTabs.BLOCKS);
        registerBlock("concrete_rebar", () -> new BlockBase(stone(50.0F, 240.0F), true), ModCreativeTabs.BLOCKS);
    }

    /** CE ModBlocks.java:398-405 — meteor dungeon brick family. {@code meteor_spawner} is a cube here
     *  (CE {@code BlockCybercrab} entity spawn is Phase 9). */
    private static void registerMeteorBricks() {
        BlockBehaviour.Properties meteor = stone(15.0F, 360.0F);
        registerBlock("meteor_polished", () -> new BlockBase(meteor), ModCreativeTabs.BLOCKS);
        registerBlock("meteor_brick", () -> new BlockBase(meteor), ModCreativeTabs.BLOCKS);
        registerBlock("meteor_brick_mossy", () -> new BlockBase(meteor), ModCreativeTabs.BLOCKS);
        registerBlock("meteor_brick_cracked", () -> new BlockBase(meteor), ModCreativeTabs.BLOCKS);
        registerBlock("meteor_brick_chiseled", () -> new BlockBase(meteor), ModCreativeTabs.BLOCKS);
        registerBlock("meteor_spawner", () -> new BlockBase(meteor), ModCreativeTabs.BLOCKS);
        registerBlock("meteor_battery", () -> new BlockBase(meteor), ModCreativeTabs.BLOCKS);
    }

    /** CE ModBlocks.java:134-191 — stairs. Base state is vanilla stone; hardness/resistance from CE. */
    private static void registerStairs() {
        stair("reinforced_stone_stairs", 15.0F, 76.0F);
        stair("brick_concrete_stairs", 15.0F, 95.0F);
        stair("brick_concrete_mossy_stairs", 15.0F, 94.0F);
        stair("brick_concrete_cracked_stairs", 15.0F, 40.0F);
        stair("brick_concrete_broken_stairs", 15.0F, 30.0F);
        stair("reinforced_brick_stairs", 15.0F, 240.0F);
        stair("brick_compound_stairs", 15.0F, 320.0F);
        stair("brick_asbestos_stairs", 15.0F, 28.0F);
        stair("brick_light_stairs", 15.0F, 20.0F);
        stair("lightstone_tile_stairs", 15.0F, 20.0F);
        stair("lightstone_bricks_stairs", 15.0F, 20.0F);
        stair("reinforced_sand_stairs", 15.0F, 32.0F);
        stair("brick_obsidian_stairs", 15.0F, 96.0F);
        stair("cmb_brick_reinforced_stairs", 25.0F, 45000.0F);
        stair("concrete_stairs", 15.0F, 94.0F);
        stair("concrete_smooth_stairs", 15.0F, 94.0F);
        stair("concrete_asbestos_stairs", 15.0F, 94.0F);
        stair("ducrete_smooth_stairs", 20.0F, 360.0F);
        stair("ducrete_stairs", 20.0F, 360.0F);
        stair("ducrete_brick_stairs", 15.0F, 500.0F);
        stair("ducrete_reinforced_stairs", 20.0F, 660.0F);
        stair("tile_lab_stairs", 1.0F, 15.0F);
        stair("tile_lab_cracked_stairs", 1.0F, 15.0F);
        stair("tile_lab_broken_stairs", 1.0F, 15.0F);
        stair("asphalt_stairs", 15.0F, 120.0F);
    }

    /** CE ModBlocks.java:194-210 — single slabs (1.21 {@link net.minecraft.world.level.block.SlabBlock}
     *  already owns DOUBLE; CE {@code *_double_slab} ids remap onto these). */
    private static void registerSlabs() {
        slab("reinforced_stone_slab", 15.0F, 60.0F);
        slab("brick_concrete_slab", 15.0F, 70.0F);
        slab("brick_concrete_mossy_slab", 15.0F, 70.0F);
        slab("brick_concrete_cracked_slab", 15.0F, 30.0F);
        slab("brick_concrete_broken_slab", 15.0F, 22.0F);
        slab("reinforced_brick_slab", 15.0F, 150.0F);
        slab("brick_light_slab", 15.0F, 20.0F);
        slab("brick_compound_slab", 15.0F, 200.0F);
        slab("brick_asbestos_slab", 15.0F, 20.0F);
        slab("lightstone_tile_slab", 15.0F, 20.0F);
        slab("lightstone_bricks_slab", 15.0F, 20.0F);
        slab("brick_fire_slab", 15.0F, 35.0F);
        slab("reinforced_sand_slab", 15.0F, 20.0F);
        slab("brick_obsidian_slab", 15.0F, 60.0F);
        slab("cmb_brick_reinforced_slab", 25.0F, 25000.0F);
        slab("concrete_slab", 15.0F, 70.0F);
        slab("concrete_smooth_slab", 15.0F, 70.0F);
        slab("concrete_asbestos_slab", 15.0F, 70.0F);
        slab("brick_slab", 15.0F, 70.0F);
    }

    /** CE {@code BlockWandLoot} / {@code wand_loot} — structure loot marker. */
    private static void registerWandLoot() {
        DeferredBlock<BlockWandLoot> wand = registerBlock("wand_loot",
                () -> new BlockWandLoot(BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL)),
                null);
        WAND_LOOT_ENTITY_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("wand_loot",
                () -> BlockEntityType.Builder.of(BlockWandLoot.WandLootBlockEntity::new, wand.get()).build(null));
    }

    private static void stair(String name, float hardness, float resistance) {
        registerBlock(name, () -> new BlockGenericStairs(Blocks.STONE.defaultBlockState(),
                BlockBehaviour.Properties.of().strength(hardness, resistance).sound(SoundType.STONE)), ModCreativeTabs.BLOCKS);
    }

    private static void slab(String name, float hardness, float resistance) {
        registerBlock(name, () -> new BlockGenericSlab(BlockBehaviour.Properties.of().strength(hardness, resistance).sound(SoundType.STONE)),
                ModCreativeTabs.BLOCKS);
    }

    private static BlockBehaviour.Properties stone(float hardness, float resistance) {
        return BlockBehaviour.Properties.of().strength(hardness, resistance).sound(SoundType.STONE);
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory, @Nullable ResourceKey<CreativeModeTab> tab) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        if (tab != null) {
            CreativeTabContents.add(tab, block);
        }
        return block;
    }
}
