package com.hbm.blocks.generic;

import com.hbm.blocks.BlockFallingBase;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.PlantEnums.EnumDeadPlantType;
import com.hbm.blocks.PlantEnums.EnumFlowerPlantType;
import com.hbm.blocks.PlantEnums.EnumTallPlantType;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Table-driven registration for CE's plant/vegetation family (dead plants, flowers, tall plants,
 * hanging vine, mushrooms, reeds, the two "special ground" dirt reskins, the dungeon glyph/glyphid
 * decoration set, the guide block) plus the fallout/wasteland vanilla-block reskins - see
 * {@code docs/phase1/blocks_generic.md}'s "Plants / vegetation / terrain" and "Fallout / wasteland
 * terrain" tables.
 * <p>
 * This class also implements the ground-check/cross-reference helpers that {@link BlockDeadPlant},
 * {@link BlockNTMFlower} and {@link BlockTallPlant} already call ({@link #isDeadPlantGround},
 * {@link #isFlowerPlantGround}, {@link #isTallPlantGround}, {@link #isWatered}, {@link #isOiled},
 * {@link #isOiledOrDeadDirt}, {@link #deadPlant}, {@link #flowerPlant}, {@link #tallPlant}) -
 * CE's {@code BlockPlantEnumMeta.PLANTABLE_BLOCKS}/{@code isOiled}/{@code isWatered} logic,
 * translated from "one metadata-multi block, many values" to "many distinct registered blocks,
 * looked up by enum".
 * <p>
 * {@code dirt_dead}/{@code dirt_oily} are CE's plain {@code BlockFallingBase} instances (no
 * dedicated class of their own in CE either) - registered here rather than invented as new files,
 * since they only exist in this port as the ground/oil-check targets these plant blocks need.
 * <p>
 * <b>Not ported</b> (see the individual block classes' own javadoc for the full rationale in each
 * case): CE's {@code ContaminationUtil.isRadImmune}/{@code HbmPotion.radiation} radiation-system
 * hooks on the waste blocks, CE's {@code HugeMush} bonemeal-grow world-gen feature on
 * {@link BlockMush}, and the cosmetic 0-6 "texture variant" metadata property CE gave every waste
 * block (confirmed unused against CE's own model JSONs).
 */
public final class PlantBlocks {

    public static DeferredBlock<BlockFallingBase> DIRT_DEAD;
    public static DeferredBlock<BlockFallingBase> DIRT_OILY;

    public static DeferredBlock<BlockHangingVine> VINE_PHOSPHOR;
    public static DeferredBlock<BlockMush> MUSH;
    public static DeferredBlock<BlockMushHuge> MUSH_BLOCK;
    public static DeferredBlock<BlockMushHuge> MUSH_BLOCK_STEM;
    public static DeferredBlock<BlockReeds> REEDS;
    public static DeferredBlock<BlockNTMDirt> NTM_DIRT;
    public static DeferredBlock<BlockDirt> IMPACT_DIRT;
    public static DeferredBlock<Guide> BOOK_GUIDE;

    public static DeferredBlock<WasteEarth> WASTE_EARTH;
    public static DeferredBlock<WasteMycelium> WASTE_MYCELIUM;
    public static DeferredBlock<WasteEarth> BURNING_EARTH;
    public static DeferredBlock<WasteEarth> FROZEN_GRASS;
    public static DeferredBlock<WasteSand> WASTE_TRINITITE;
    public static DeferredBlock<WasteSand> WASTE_TRINITITE_RED;
    public static DeferredBlock<WasteIce> WASTE_ICE;
    public static DeferredBlock<WasteLeaves> WASTE_LEAVES;
    public static DeferredBlock<WasteGrassTall> WASTE_GRASS_TALL;
    public static DeferredBlock<WasteLog> WASTE_LOG;
    public static DeferredBlock<WasteLog> FROZEN_LOG;

    private static final Map<EnumDeadPlantType, DeferredBlock<BlockDeadPlant>> DEAD_PLANTS = new EnumMap<>(EnumDeadPlantType.class);
    private static final Map<EnumFlowerPlantType, DeferredBlock<BlockNTMFlower>> FLOWER_PLANTS = new EnumMap<>(EnumFlowerPlantType.class);
    private static final Map<EnumTallPlantType, DeferredBlock<BlockTallPlant>> TALL_PLANTS = new EnumMap<>(EnumTallPlantType.class);
    private static final Map<BlockGlyphid.Type, DeferredBlock<BlockGlyphid>> GLYPHID = new EnumMap<>(BlockGlyphid.Type.class);
    private static final Map<BlockGlyph.Type, DeferredBlock<BlockGlyph>> GLYPH = new EnumMap<>(BlockGlyph.Type.class);
    public static DeferredBlock<BlockGlyphidSpawner> GLYPHID_SPAWNER;
    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<GlyphidSpawnerBlockEntity>> GLYPHID_SPAWNER_ENTITY_TYPE;

    private PlantBlocks() {
    }

    public static void registerAll() {
        registerGroundBlocks();
        registerDeadPlants();
        registerFlowerPlants();
        registerTallPlants();
        registerHangingVine();
        registerMush();
        registerReeds();
        registerDirt();
        registerGlyph();
        registerGlyphid();
        registerGuide();
        registerWaste();
    }

    // ==================== ground / oil / water helpers ====================

    /** Mirrors CE's {@code BlockDeadPlant.initPlacables()} {@code PLANTABLE_BLOCKS} set. */
    public static boolean isDeadPlantGround(Block block) {
        return block == WASTE_EARTH.get() || block == DIRT_OILY.get() || block == DIRT_DEAD.get();
    }

    /** Mirrors CE's {@code BlockNTMFlower}/{@code BlockTallPlant} {@code initPlacables()}. */
    public static boolean isFlowerPlantGround(Block block) {
        return block == DIRT_DEAD.get() || block == DIRT_OILY.get() || block == Blocks.GRASS_BLOCK || block == Blocks.DIRT;
    }

    public static boolean isTallPlantGround(Block block) {
        return isFlowerPlantGround(block);
    }

    /** Ground check shared by {@link BlockMush} and {@link WasteGrassTall}. */
    public static boolean isWasteGround(Block block) {
        return block == WASTE_EARTH.get() || block == WASTE_MYCELIUM.get();
    }

    /** Mirrors CE's {@code BlockPlantEnumMeta.isWatered}: water adjacent one level below. */
    public static boolean isWatered(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.north().below()).is(Blocks.WATER)
                || level.getBlockState(pos.south().below()).is(Blocks.WATER)
                || level.getBlockState(pos.east().below()).is(Blocks.WATER)
                || level.getBlockState(pos.west().below()).is(Blocks.WATER);
    }

    /** Mirrors CE's {@code BlockPlantEnumMeta.isOiled}: directly below is oily/dead dirt. */
    public static boolean isOiled(LevelReader level, BlockPos pos) {
        return isOiledOrDeadDirt(level.getBlockState(pos.below()).getBlock());
    }

    public static boolean isOiledOrDeadDirt(Block block) {
        return block == DIRT_OILY.get() || block == DIRT_DEAD.get();
    }

    public static Block deadPlant(EnumDeadPlantType type) {
        return DEAD_PLANTS.get(type).get();
    }

    public static Block flowerPlant(EnumFlowerPlantType type) {
        return FLOWER_PLANTS.get(type).get();
    }

    public static Block tallPlant(EnumTallPlantType type) {
        return TALL_PLANTS.get(type).get();
    }

    public static DeferredBlock<BlockGlyphid> glyphid(BlockGlyphid.Type type) {
        return GLYPHID.get(type);
    }

    // ==================== registration ====================

    private static void registerGroundBlocks() {
        BlockBehaviour.Properties dirtProps = BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.GRAVEL);
        DIRT_DEAD = registerBlock("dirt_dead", () -> new BlockFallingBase(dirtProps), ModCreativeTabs.RESOURCE);
        DIRT_OILY = registerBlock("dirt_oily", () -> new BlockFallingBase(dirtProps), ModCreativeTabs.RESOURCE);
    }

    private static BlockBehaviour.Properties plantProps() {
        return BlockBehaviour.Properties.of().noCollission().instabreak().sound(SoundType.GRASS);
    }

    private static void registerDeadPlants() {
        BlockBehaviour.Properties props = plantProps().noLootTable();
        for (EnumDeadPlantType type : EnumDeadPlantType.VALUES) {
            String name = "plant_dead_" + type.name().toLowerCase(java.util.Locale.ROOT);
            DEAD_PLANTS.put(type, registerBlock(name, () -> new BlockDeadPlant(props, type), ModCreativeTabs.RESOURCE));
        }
    }

    private static void registerFlowerPlants() {
        for (EnumFlowerPlantType type : EnumFlowerPlantType.VALUES) {
            String name = "plant_flower_" + type.name().toLowerCase(java.util.Locale.ROOT);
            FLOWER_PLANTS.put(type, registerBlock(name, () -> new BlockNTMFlower(plantProps(), type), ModCreativeTabs.RESOURCE));
        }
    }

    private static void registerTallPlants() {
        for (EnumTallPlantType type : EnumTallPlantType.VALUES) {
            String name = "plant_tall_" + type.name().toLowerCase(java.util.Locale.ROOT);
            // Only the lower half of each pair is creative-tab-obtainable, matching CE's
            // getSubBlocks() override (see BlockTallPlant's class javadoc for why the upper
            // half still gets its own BlockItem regardless).
            ResourceKey<CreativeModeTab> tab = type.name().endsWith("_LOWER") ? ModCreativeTabs.RESOURCE : null;
            TALL_PLANTS.put(type, registerBlock(name, () -> new BlockTallPlant(plantProps(), type), tab));
        }
    }

    private static void registerHangingVine() {
        VINE_PHOSPHOR = registerBlock("vine_phosphor",
                () -> new BlockHangingVine(BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.VINE).noOcclusion()),
                ModCreativeTabs.BLOCKS);
    }

    private static void registerMush() {
        MUSH = registerBlock("mush",
                () -> new BlockMush(plantProps().lightLevel(state -> 8)),
                ModCreativeTabs.RESOURCE);
        MUSH_BLOCK = registerBlock("mush_block",
                () -> new BlockMushHuge(BlockBehaviour.Properties.of().strength(0.2F).sound(SoundType.WOOD).lightLevel(state -> 15)),
                ModCreativeTabs.RESOURCE);
        MUSH_BLOCK_STEM = registerBlock("mush_block_stem",
                () -> new BlockMushHuge(BlockBehaviour.Properties.of().strength(0.3F).sound(SoundType.WOOD).lightLevel(state -> 4)),
                ModCreativeTabs.RESOURCE);
    }

    private static void registerReeds() {
        REEDS = registerBlock("plant_reeds",
                () -> new BlockReeds(BlockBehaviour.Properties.of().noCollission().instabreak().sound(SoundType.GRASS).noOcclusion()),
                ModCreativeTabs.BLOCKS);
    }

    private static void registerDirt() {
        NTM_DIRT = registerBlock("ntm_dirt",
                () -> new BlockNTMDirt(BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.GRAVEL)),
                null);
        IMPACT_DIRT = registerBlock("impact_dirt",
                () -> new BlockDirt(BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.GRAVEL)),
                ModCreativeTabs.RESOURCE);
    }

    private static void registerGlyph() {
        for (BlockGlyph.Type type : BlockGlyph.Type.VALUES) {
            String name = "brick_jungle_glyph_" + type.ordinal();
            BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(15.0F, 360.0F).sound(SoundType.STONE).requiresCorrectToolForDrops();
            GLYPH.put(type, registerBlock(name, () -> new BlockGlyph(props, type), ModCreativeTabs.BLOCKS));
        }
    }

    private static void registerGlyphid() {
        for (BlockGlyphid.Type type : BlockGlyphid.Type.VALUES) {
            String name = type == BlockGlyphid.Type.BASE ? "glyphid_base" : "glyphid_base_" + type.name().toLowerCase(java.util.Locale.ROOT);
            BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.WOOL).noLootTable();
            GLYPHID.put(type, registerBlock(name, () -> new BlockGlyphid(props, type), ModCreativeTabs.BLOCKS));
        }
        // CE ModBlocks.java:564 BlockGlyphidSpawner Material.CORAL, SoundType.CLOTH, hardness 0.5F
        GLYPHID_SPAWNER = registerBlock("glyphid_spawner",
                () -> new BlockGlyphidSpawner(BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.WOOL)),
                ModCreativeTabs.BLOCKS);
        GLYPHID_SPAWNER_ENTITY_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("glyphid_spawner",
                () -> BlockEntityType.Builder.of(GlyphidSpawnerBlockEntity::new, GLYPHID_SPAWNER.get()).build(null));
    }

    private static void registerGuide() {
        BOOK_GUIDE = registerBlock("book_guide",
                () -> new Guide(BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL)),
                ModCreativeTabs.NUKE);
    }

    private static void registerWaste() {
        WASTE_EARTH = registerBlock("waste_earth",
                () -> new WasteEarth(BlockBehaviour.Properties.of().strength(0.5F, 1.0F).sound(SoundType.GRAVEL), WasteEarth.Kind.WASTE),
                ModCreativeTabs.RESOURCE);
        WASTE_MYCELIUM = registerBlock("waste_mycelium",
                () -> new WasteMycelium(BlockBehaviour.Properties.of().strength(0.5F, 1.0F).sound(SoundType.GRAVEL).lightLevel(state -> 4)),
                ModCreativeTabs.RESOURCE);
        BURNING_EARTH = registerBlock("burning_earth",
                () -> new WasteEarth(BlockBehaviour.Properties.of().strength(0.6F).sound(SoundType.STONE), WasteEarth.Kind.BURNING),
                ModCreativeTabs.RESOURCE);
        FROZEN_GRASS = registerBlock("frozen_grass",
                () -> new WasteEarth(BlockBehaviour.Properties.of().strength(0.5F, 2.5F).sound(SoundType.GLASS), WasteEarth.Kind.FROZEN),
                ModCreativeTabs.RESOURCE);

        WASTE_TRINITITE = registerBlock("waste_trinitite",
                () -> new WasteSand(BlockBehaviour.Properties.of().strength(0.5F, 2.5F).sound(SoundType.SAND)),
                ModCreativeTabs.RESOURCE);
        WASTE_TRINITITE_RED = registerBlock("waste_trinitite_red",
                () -> new WasteSand(BlockBehaviour.Properties.of().strength(0.5F, 2.5F).sound(SoundType.SAND)),
                ModCreativeTabs.RESOURCE);

        WASTE_ICE = registerBlock("waste_ice",
                () -> new WasteIce(BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.GLASS).friction(0.98F).noOcclusion()),
                ModCreativeTabs.RESOURCE);

        WASTE_LEAVES = registerBlock("waste_leaves",
                () -> new WasteLeaves(BlockBehaviour.Properties.of().strength(0.3F).sound(SoundType.GRASS).noOcclusion()),
                ModCreativeTabs.RESOURCE);

        WASTE_GRASS_TALL = registerBlock("waste_grass_tall",
                () -> new WasteGrassTall(BlockBehaviour.Properties.of().noCollission().instabreak().sound(SoundType.GRASS).noLootTable()),
                ModCreativeTabs.RESOURCE);

        WASTE_LOG = registerBlock("waste_log",
                () -> new WasteLog(BlockBehaviour.Properties.of().strength(5.0F, 2.5F).sound(SoundType.WOOD), WasteLog.Kind.WASTE),
                ModCreativeTabs.RESOURCE);
        FROZEN_LOG = registerBlock("frozen_log",
                () -> new WasteLog(BlockBehaviour.Properties.of().strength(0.5F, 2.5F).sound(SoundType.GLASS), WasteLog.Kind.FROZEN),
                ModCreativeTabs.RESOURCE);
    }

    // ==================== construction helper ====================

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory, @Nullable ResourceKey<CreativeModeTab> tab) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        if (tab != null) {
            CreativeTabContents.add(tab, block);
        }
        return block;
    }
}
