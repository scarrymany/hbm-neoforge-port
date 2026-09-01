package com.hbm.blocks.generic;

import com.hbm.blocks.BlockBase;
import com.hbm.blocks.BlockFallingBase;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.NTMAnvil;
import com.hbm.blocks.machine.RailBooster;
import com.hbm.blocks.machine.RailGeneric;
import com.hbm.blocks.machine.RailHighspeed;
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
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Phase 8 block families required by CE {@code .nbt} palettes ({@code StructureManager.java} +
 * {@code assets/hbm/structures/*.nbt}) and the leftover brick/concrete/stairs/slab table in
 * {@code ModBlocks.java}:86-227 / 398-434 / 497-504 / 643-644 / 857. Stairs use {@link Blocks#STONE}
 * as the {@link net.minecraft.world.level.block.StairBlock} base state so we never call
 * {@code DeferredBlock#get()} from a static registrar (CE {@code BlockGenericStairs} took the live
 * parent block).
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
        registerColoredConcreteStairsSlabs();
        registerJungleDungeonBricks();
        registerGeiger();
        registerWandLoot();
        registerBlock("block_electrical_scrap",
                () -> new BlockFallingBase(BlockBehaviour.Properties.of().strength(2.5F, 5.0F).sound(SoundType.METAL)),
                ModCreativeTabs.BLOCKS);
        registerStorageLeftovers();
        registerRails();
        registerAnvils();
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
        // CE ModBlocks.java:360-362 / :128 / :1547 / :1283 / :1314-1316 — leftover cubes.
        registerBlock("gneiss_brick", () -> new BlockBase(stone(1.5F, 10.0F)), ModCreativeTabs.BLOCKS);
        registerBlock("gneiss_tile", () -> new BlockBase(stone(1.5F, 10.0F)), ModCreativeTabs.BLOCKS);
        registerBlock("gneiss_chiseled", () -> new BlockBase(stone(1.5F, 10.0F)), ModCreativeTabs.BLOCKS);
        registerBlock("vinyl_tile_large", () -> new BlockBase(stone(10.0F, 60.0F)), ModCreativeTabs.BLOCKS);
        registerBlock("vinyl_tile_small", () -> new BlockBase(stone(10.0F, 60.0F)), ModCreativeTabs.BLOCKS);
        registerBlock("pink_planks", () -> new BlockBase(BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.WOOD)), null);
        registerBlock("struct_launcher", () -> new BlockBase(BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL)), ModCreativeTabs.MISSILE);
        registerBlock("struct_scaffold", () -> new BlockBase(BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL)), ModCreativeTabs.MISSILE);
        registerBlock("fusion_heater", () -> new BlockBase(BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL)), ModCreativeTabs.MACHINE);
        registerBlock("fusion_hatch", () -> new BlockBase(BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL)), ModCreativeTabs.MACHINE);
        registerBlock("fusion_core_block", () -> new BlockBase(BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL)), ModCreativeTabs.MACHINE);
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
        slab("ducrete_smooth_slab", 20.0F, 250.0F);
        slab("ducrete_slab", 20.0F, 250.0F);
        slab("ducrete_brick_slab", 15.0F, 375.0F);
        slab("ducrete_reinforced_slab", 20.0F, 500.0F);
        slab("tile_lab_slab", 1.0F, 10.0F);
        slab("tile_lab_cracked_slab", 1.0F, 10.0F);
        slab("tile_lab_broken_slab", 1.0F, 10.0F);
    }

    /**
     * CE {@code ModBlocks.java}:150-181 / 212-227 — 16 dye-color stairs+slabs (1.12 {@code silver}
     * kept as the registry path) plus 8 {@code concrete_colored_ext} stairs.
     */
    private static void registerColoredConcreteStairsSlabs() {
        String[] colors = {
                "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
                "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"
        };
        for (String color : colors) {
            stair("concrete_colored_stairs_" + color, 15.0F, 94.0F);
            slab("concrete_" + color + "_slab", 15.0F, 70.0F);
        }
        for (BlockConcreteColoredExt.Type type : BlockConcreteColoredExt.Type.VALUES) {
            stair("concrete_colored_ext_stairs_" + type.name().toLowerCase(Locale.US), 15.0F, 94.0F);
        }
    }

    /** CE {@code ModBlocks.java}:407-434 / 643-644 — jungle/dungeon cubes (plain BlockBase only). */
    private static void registerJungleDungeonBricks() {
        registerBlock("brick_jungle", () -> new BlockBase(stone(15.0F, 360.0F)), ModCreativeTabs.BLOCKS);
        registerBlock("brick_jungle_cracked", () -> new BlockBase(stone(15.0F, 360.0F)), ModCreativeTabs.BLOCKS);
        registerBlock("brick_dungeon", () -> new BlockBase(stone(15.0F, 360.0F)), ModCreativeTabs.BLOCKS);
        registerBlock("brick_dungeon_flat", () -> new BlockBase(stone(15.0F, 360.0F)), ModCreativeTabs.BLOCKS);
        registerBlock("brick_dungeon_tile", () -> new BlockBase(stone(15.0F, 360.0F)), ModCreativeTabs.BLOCKS);
        registerBlock("brick_dungeon_circle", () -> new BlockBase(stone(15.0F, 360.0F)), ModCreativeTabs.BLOCKS);
        registerBlock("brick_fire", () -> new BlockBase(stone(10.0F, 10.0F)), ModCreativeTabs.BLOCKS);
        stair("brick_fire_stairs", 15.0F, 35.0F);
    }

    /**
     * CE storage/deco cubes that are <b>not</b> Mats {@code *_block} autogen
     * ({@code ModBlocks.java}:299, 458-479, 573-574, 609, 623, 636, 650-652, 864, 1421).
     * Skip prefix-first aliases of already-registered suffix-first Mats blocks.
     */
    private static void registerStorageLeftovers() {
        registerBlock("block_scrap",
                () -> new BlockFallingBase(BlockBehaviour.Properties.of().strength(2.5F, 5.0F).sound(SoundType.GRAVEL)),
                ModCreativeTabs.BLOCKS);
        BlockBehaviour.Properties waste = BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL);
        registerBlock("block_waste_painted", () -> new BlockNuclearWaste(waste), ModCreativeTabs.BLOCKS);
        registerBlock("block_waste_vitrified", () -> new BlockNuclearWaste(waste), ModCreativeTabs.BLOCKS);
        registerBlock("block_fallout",
                () -> new BlockHazardFalling(BlockBehaviour.Properties.of().strength(0.2F).sound(SoundType.GRAVEL)),
                ModCreativeTabs.RESOURCE);
        registerBlock("block_foam",
                () -> new BlockBase(BlockBehaviour.Properties.of().strength(0.5F, 0.0F).sound(SoundType.SNOW)),
                ModCreativeTabs.BLOCKS);
        registerBlock("block_yellowcake",
                () -> new BlockHazardFalling(BlockBehaviour.Properties.of().strength(5.0F, 300.0F).sound(SoundType.SAND)),
                ModCreativeTabs.BLOCKS);
        registerBlock("block_white_phosphorus",
                () -> new BlockHazard(BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.STONE)),
                ModCreativeTabs.BLOCKS);
        registerBlock("block_au198",
                () -> new BlockHazard(BlockBehaviour.Properties.of().strength(5.0F, 300.0F).sound(SoundType.METAL)),
                ModCreativeTabs.BLOCKS);
        registerBlock("ash_digamma",
                () -> new BlockHazardFalling(BlockBehaviour.Properties.of().strength(0.5F, 150.0F).sound(SoundType.SAND)),
                ModCreativeTabs.RESOURCE);
        registerBlock("ancient_scrap",
                () -> new BlockOutgas(BlockBehaviour.Properties.of().strength(100.0F, 6000.0F).sound(SoundType.METAL)),
                ModCreativeTabs.RESOURCE);

        BlockBehaviour.Properties fuel = BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL);
        registerBlock("block_mox_fuel", () -> new BlockHazard(fuel), ModCreativeTabs.BLOCKS);
        registerBlock("block_thorium_fuel", () -> new BlockHazard(fuel), ModCreativeTabs.BLOCKS);
        registerBlock("block_plutonium_fuel", () -> new BlockHazard(fuel), ModCreativeTabs.BLOCKS);
        registerBlock("block_uranium_fuel", () -> new BlockHazard(fuel), ModCreativeTabs.BLOCKS);
        registerBlock("block_schrabidium_fuel",
                () -> new BlockHazard(BlockBehaviour.Properties.of().strength(5.0F, 300.0F).sound(SoundType.METAL)),
                ModCreativeTabs.BLOCKS);

        BlockBehaviour.Properties metal = BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL);
        registerBlock("block_red_copper", () -> new BlockBase(metal), ModCreativeTabs.BLOCKS);
        registerBlock("block_tcalloy",
                () -> new BlockBase(BlockBehaviour.Properties.of().strength(5.0F, 70.0F).sound(SoundType.METAL)),
                ModCreativeTabs.BLOCKS);
        registerBlock("block_cdalloy",
                () -> new BlockBase(BlockBehaviour.Properties.of().strength(5.0F, 70.0F).sound(SoundType.METAL)),
                ModCreativeTabs.BLOCKS);
        registerBlock("block_euphemium",
                () -> new BlockBase(BlockBehaviour.Properties.of().strength(5.0F, 30000.0F).sound(SoundType.METAL)),
                ModCreativeTabs.BLOCKS);
        registerBlock("block_smore",
                () -> new BlockBase(stone(15.0F, 450.0F)), ModCreativeTabs.BLOCKS);
        registerBlock("block_graphite",
                () -> new BlockBase(BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL)),
                ModCreativeTabs.BLOCKS);

        registerBlock("tektite", () -> new BlockBase(stone(1.5F, 10.0F)), ModCreativeTabs.RESOURCE);
        registerBlock("gravel_obsidian",
                () -> new BlockFallingBase(BlockBehaviour.Properties.of().strength(5.0F, 300.0F).sound(SoundType.GRAVEL)),
                ModCreativeTabs.RESOURCE);
        registerBlock("gravel_diamond",
                () -> new BlockFallingBase(BlockBehaviour.Properties.of().strength(0.6F).sound(SoundType.GRAVEL)),
                ModCreativeTabs.RESOURCE);
        registerBlock("moon_turf",
                () -> new BlockFallingBase(BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.SAND)),
                ModCreativeTabs.RESOURCE);
        registerBlock("stone_porous", () -> new BlockBase(stone(5.0F, 10.0F)), ModCreativeTabs.RESOURCE);

        registerBlock("basalt", () -> new BlockBase(stone(5.0F, 10.0F)), ModCreativeTabs.RESOURCE);
        registerBlock("basalt_smooth", () -> new BlockBase(stone(5.0F, 10.0F)), ModCreativeTabs.BLOCKS);
        registerBlock("basalt_brick", () -> new BlockBase(stone(5.0F, 10.0F)), ModCreativeTabs.BLOCKS);
        registerBlock("basalt_polished", () -> new BlockBase(stone(5.0F, 10.0F)), ModCreativeTabs.BLOCKS);
        registerBlock("basalt_tiles", () -> new BlockBase(stone(5.0F, 10.0F)), ModCreativeTabs.BLOCKS);
    }

    /**
     * Vanilla-compatible minecart rails. CE {@code ModBlocks.java}:836-839.
     * {@code rail_large_*}/{@code rail_narrow_straight/curve} need {@code IRailNTM} implementors
     * (none exist — same skip as meteor jigsaw walker).
     */
    private static void registerRails() {
        BlockBehaviour.Properties rail = BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL).noCollission();
        registerBlock("rail_highspeed", () -> new RailHighspeed(rail), ModCreativeTabs.BLOCKS);
        registerBlock("rail_booster", () -> new RailBooster(rail), ModCreativeTabs.BLOCKS);
        registerBlock("rail_wood", () -> new RailGeneric(rail, 0.2F), ModCreativeTabs.BLOCKS);
        registerBlock("rail_narrow", () -> new RailGeneric(rail, 0.4F), ModCreativeTabs.BLOCKS);
    }

    /** CE {@code ModBlocks.java}:1094-1105 — facing falling casings, no GUI this pass. */
    private static void registerAnvils() {
        BlockBehaviour.Properties anvil = BlockBehaviour.Properties.of()
                .strength(5.0F, 100.0F).sound(SoundType.ANVIL).noOcclusion();
        registerBlock("anvil_iron", () -> new NTMAnvil(anvil, NTMAnvil.TIER_IRON), ModCreativeTabs.MACHINE);
        registerBlock("anvil_lead", () -> new NTMAnvil(anvil, NTMAnvil.TIER_IRON), ModCreativeTabs.MACHINE);
        registerBlock("anvil_steel", () -> new NTMAnvil(anvil, NTMAnvil.TIER_STEEL), ModCreativeTabs.MACHINE);
        registerBlock("anvil_desh", () -> new NTMAnvil(anvil, NTMAnvil.TIER_OIL), ModCreativeTabs.MACHINE);
        registerBlock("anvil_ferrouranium", () -> new NTMAnvil(anvil, NTMAnvil.TIER_NUCLEAR), ModCreativeTabs.MACHINE);
        registerBlock("anvil_saturnite", () -> new NTMAnvil(anvil, NTMAnvil.TIER_RBMK), ModCreativeTabs.MACHINE);
        registerBlock("anvil_bismuth_bronze", () -> new NTMAnvil(anvil, NTMAnvil.TIER_RBMK), ModCreativeTabs.MACHINE);
        registerBlock("anvil_arsenic_bronze", () -> new NTMAnvil(anvil, NTMAnvil.TIER_RBMK), ModCreativeTabs.MACHINE);
        registerBlock("anvil_schrabidate", () -> new NTMAnvil(anvil, NTMAnvil.TIER_FUSION), ModCreativeTabs.MACHINE);
        registerBlock("anvil_dnt", () -> new NTMAnvil(anvil, NTMAnvil.TIER_PARTICLE), ModCreativeTabs.MACHINE);
        registerBlock("anvil_osmiridium", () -> new NTMAnvil(anvil, NTMAnvil.TIER_GERALD), ModCreativeTabs.MACHINE);
        registerBlock("anvil_murky", () -> new NTMAnvil(anvil, 1916169), ModCreativeTabs.MACHINE);
    }

    /** CE {@code ModBlocks.java}:857 / {@code GeigerCounter.java}:28 — facing casing for Bunker. */
    private static void registerGeiger() {
        registerBlock("geiger",
                () -> new BlockGeiger(BlockBehaviour.Properties.of().strength(15.0F, 0.25F).sound(SoundType.STONE).noOcclusion()),
                ModCreativeTabs.MACHINE);
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
