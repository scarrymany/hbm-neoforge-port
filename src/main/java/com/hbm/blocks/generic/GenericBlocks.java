package com.hbm.blocks.generic;

import com.hbm.blockentity.machine.DoorGenericBlockEntities;
import com.hbm.blocks.BlockBase;
import com.hbm.blocks.BlockEnums;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.BlastDoor;
import com.hbm.blocks.machine.BlockSlidingBlastDoor;
import com.hbm.blocks.test.KeypadTestBlock;
import com.hbm.blocks.machine.DummyBlockBlast;
import com.hbm.tileentity.DoorDecl;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ItemEnums.EnumCokeType;
import com.hbm.items.ModItems;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.items.tool.ItemModDoor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Table-driven registration hub for everything else in {@code com.hbm.blocks.generic} - the
 * package's structural/building, door/trapdoor/ladder, glass, non-hazardous ore/mineral,
 * self-contained hazard-adjacent, speed/tool-interaction and metadata-only-decoration families (see
 * {@code docs/phase1/blocks_generic.md}). Delegates the plant/vegetation/waste family to
 * {@link PlantBlocks}, the crate/barrel/loot family to {@link GenericCrateBlocks} and the
 * decorative-prop family to {@link GenericDecoBlocks}, then registers every remaining
 * already-ported class in this package plus the handful of classes this pass ports fresh from CE
 * ({@link BlockPlatemetal}, {@link BlockConcreteColored}, {@link BlockConcreteColoredExt},
 * {@link BlockOreMeta}, {@link BlockPinkLog}, {@link BlockPipe}, {@link BlockCap}, {@link BlockCoke},
 * {@link BlockLightstone}, {@link BlockFlammable}). Mirrors {@link com.hbm.blocks.OreBlocks}'s
 * table-driven-{@code registerAll()} shape and its {@code registerBlock()} helper.
 * <p>
 * <b>Not registered here</b> (see each class's own javadoc for the full rationale):
 * {@link BlockGenericSlab}/{@link BlockGenericStairs}/{@link BlockRBMKSlab}'s stairs sibling/
 * {@link BlockSpeedyStairs} - infrastructure classes with no base material block in this survey's
 * scope to pair with yet (the one CE stairs instance, {@code asphalt_stairs}, is skipped rather than
 * risking a cross-{@code DeferredRegister} {@code .get()} call during block construction - see this
 * class's port report entry); {@link BlockOreMeta}/{@link BlockFlammable} - CE itself never
 * constructs either class anywhere in its own {@code ModBlocks}, confirmed by searching the upstream
 * source tree, so there is no concrete instance to carry over; {@link BlockOreBasalt}/
 * {@code BlockBiomeStone} - already substituted by {@link com.hbm.blocks.OreBlocks}'s own
 * {@code registerBasalt()}/{@code registerBiomeStone()} with equivalent plain blocks, per this
 * area's audit; {@link BlockCluster}/{@link BlockDepthOre}/{@link BlockNTMOre}/{@link BlockOutgas} -
 * already registered by {@link com.hbm.blocks.OreBlocks} directly; {@code BlockHazard}/
 * {@code BlockHazardFalling}/{@code BlockNuclearWaste} - radiation-system-coupled, deferred scope
 * per the port report, out of this pass entirely.
 */
public final class GenericBlocks {

    public static DeferredBlock<BlockDoorGeneric> VAULT_DOOR;
    public static DeferredBlock<BlastDoor> BLAST_DOOR;
    public static DeferredBlock<BlockDoorGeneric> FIRE_DOOR;
    public static DeferredBlock<BlockDoorGeneric> SLIDING_BLAST_DOOR;
    public static DeferredBlock<BlockDoorGeneric> LARGE_VEHICLE_DOOR;
    public static DeferredBlock<BlockDoorGeneric> WATER_DOOR;
    public static DeferredBlock<BlockDoorGeneric> QE_CONTAINMENT;
    public static DeferredBlock<BlockDoorGeneric> QE_SLIDING_DOOR;
    public static DeferredBlock<BlockDoorGeneric> ROUND_AIRLOCK_DOOR;
    public static DeferredBlock<BlockDoorGeneric> SECURE_ACCESS_DOOR;
    public static DeferredBlock<BlockDoorGeneric> SLIDING_SEAL_DOOR;
    public static DeferredBlock<BlockDoorGeneric> CARGO_DOOR;
    public static DeferredBlock<BlockDoorGeneric> SILO_HATCH;
    public static DeferredBlock<BlockDoorGeneric> SILO_HATCH_LARGE;
    public static DeferredBlock<BlockDoorGeneric> TRANSITION_SEAL;
    public static DeferredBlock<DummyBlockBlast> DUMMY_BLOCK_BLAST;
    public static DeferredBlock<BlockSlidingBlastDoor> SLIDING_BLAST_DOOR_LEGACY;
    public static DeferredBlock<BlockSlidingBlastDoor> SLIDING_BLAST_DOOR_2;
    public static DeferredBlock<BlockSlidingBlastDoor> SLIDING_BLAST_DOOR_KEYPAD;
    public static DeferredBlock<KeypadTestBlock> KEYPAD_TEST;

    private GenericBlocks() {
    }

    public static void registerAll() {
        PlantBlocks.registerAll();
        GenericCrateBlocks.registerAll();
        GenericDecoBlocks.registerAll();
        BedrockOreBlocks.registerAll();
        WastelandVirusBlocks.registerAll();
        FalloutBlocks.registerAll();

        registerStructural();
        registerDoorsLaddersGlass();
        registerOreMineral();
        registerHazardAdjacent();
        registerSpeedAndTool();
        registerMetadataDecoration();
    }

    // ==================== structural / building materials ====================

    private static void registerStructural() {
        registerBlock("brick_red", () -> new BlockRedBrick(BlockBehaviour.Properties.of().strength(0.0F, 10_000.0F).sound(SoundType.STONE)), null);

        registerForgottenBrick();
        registerForgottenLock();

        registerBlock("brick_jungle_fragile", () -> new FragileBrick(BlockBehaviour.Properties.of().strength(15.0F, 360.0F).sound(SoundType.STONE)),
                ModCreativeTabs.BLOCKS);

        registerRailings();

        registerBlock("dungeon_chain", () -> new BlockChain(BlockBehaviour.Properties.of().strength(0.25F, 2.0F).sound(SoundType.METAL).noOcclusion()),
                ModCreativeTabs.BLOCKS);

        registerBlock("steel_grate",
                () -> new BlockGrate(BlockBehaviour.Properties.of().strength(2.0F, 5.0F).sound(SoundType.METAL).noOcclusion()), ModCreativeTabs.BLOCKS);
        ModBlocks.STEEL_GRATE_WIDE = registerBlock("steel_grate_wide",
                () -> new BlockGrate(BlockBehaviour.Properties.of().strength(2.0F, 5.0F).sound(SoundType.METAL).noOcclusion()), ModCreativeTabs.BLOCKS);

        registerBlock("fence_metal", () -> new BlockMetalFence(BlockBehaviour.Properties.of().strength(15.0F, 0.25F).sound(SoundType.METAL).noOcclusion()),
                ModCreativeTabs.MACHINE);

        registerRotatablePillars();

        registerBlock("concrete_pillar", () -> new BlockRadResistantPillar(BlockBehaviour.Properties.of().strength(15.0F, 180.0F).sound(SoundType.STONE)),
                ModCreativeTabs.BLOCKS);

        registerBlock("deco_rbmk_panel_slab", () -> new BlockRBMKSlab(BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL)),
                ModCreativeTabs.BLOCKS);
        registerBlock("deco_rbmk_smooth_panel_slab", () -> new BlockRBMKSlab(BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL)),
                ModCreativeTabs.BLOCKS);

        registerGenericPwr();
        registerWoodStructure();
        registerUberConcrete();
        registerConcreteColored();
        registerConcreteColoredExt();
        registerLayering();

        registerBlock("sandbags", () -> new BlockSandbags(BlockBehaviour.Properties.of().strength(5.0F, 30.0F).sound(SoundType.GRAVEL)),
                ModCreativeTabs.BLOCKS);

        registerScaffold();
        registerPlatemetal();
        registerCmBlock();
    }

    /**
     * CE's seven metadata variants (default/bw/nullstone/hole/hole_empty/nullroom_wood/
     * nullroom_stone), flattened to one registry entry each per {@link BlockForgottenBrick}'s own
     * javadoc. Only the "hole" variant carries the empty-handed-click-for-coal_eternal interaction;
     * {@code ModBlocks.BRICK_FORGOTTEN} (the default variant) is the wall material
     * {@link BlockForgottenLock#generate} builds its vault room out of.
     */
    private static void registerForgottenBrick() {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(-1.0F, 666_666.0F).sound(SoundType.STONE);

        ModBlocks.BRICK_FORGOTTEN = registerBlock("brick_forgotten", () -> new BlockForgottenBrick(props), null);
        registerBlock("brick_forgotten_bw", () -> new BlockForgottenBrick(props), null);
        registerBlock("brick_forgotten_nullstone", () -> new BlockForgottenBrick(props), null);

        DeferredBlock<BlockForgottenBrick> holeEmpty = registerBlock("brick_forgotten_hole_empty", () -> new BlockForgottenBrick(props), null);
        registerBlock("brick_forgotten_hole", () -> new BlockForgottenBrick(props, holeEmpty::get), null);

        registerBlock("brick_forgotten_nullroom_wood", () -> new BlockForgottenBrick(props), null);
        registerBlock("brick_forgotten_nullroom_stone", () -> new BlockForgottenBrick(props), null);
    }

    /** CE's four metadata variants (default/bw/nullstone/killsyou), one registry entry each. */
    private static void registerForgottenLock() {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(-1.0F, 666_666.0F).sound(SoundType.STONE);
        registerBlock("brick_forgotten_lock", () -> new BlockForgottenLock(props), null);
        registerBlock("brick_forgotten_lock_bw", () -> new BlockForgottenLock(props), null);
        registerBlock("brick_forgotten_lock_nullstone", () -> new BlockForgottenLock(props), null);
        registerBlock("brick_forgotten_lock_killsyou", () -> new BlockForgottenLock(props), null);
    }

    /** CE's six {@code type}-selected registrations (see {@link BlockRailing}'s own javadoc). */
    private static void registerRailings() {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(0.25F, 2.0F).sound(SoundType.METAL).noOcclusion();
        registerBlock("railing_end_floor", () -> new BlockRailing(BlockRailing.Kind.PANEL, props), ModCreativeTabs.BLOCKS);
        registerBlock("railing_end_self", () -> new BlockRailing(BlockRailing.Kind.PANEL, props), ModCreativeTabs.BLOCKS);
        registerBlock("railing_end_flipped_floor", () -> new BlockRailing(BlockRailing.Kind.PANEL, props), ModCreativeTabs.BLOCKS);
        registerBlock("railing_end_flipped_self", () -> new BlockRailing(BlockRailing.Kind.PANEL, props), ModCreativeTabs.BLOCKS);
        registerBlock("railing_normal", () -> new BlockRailing(BlockRailing.Kind.PANEL, props), ModCreativeTabs.BLOCKS);
        registerBlock("railing_bend", () -> new BlockRailing(BlockRailing.Kind.DOUBLE_PANEL, props), ModCreativeTabs.BLOCKS);
    }

    private static void registerRotatablePillars() {
        registerBlock("meteor_pillar", () -> new BlockRotatablePillar(BlockBehaviour.Properties.of().strength(15.0F, 360.0F).sound(SoundType.STONE)),
                ModCreativeTabs.BLOCKS);
        ModBlocks.BLOCK_SCHRABIDIUM_CLUSTER = registerBlock("block_schrabidium_cluster",
                () -> new BlockRotatablePillar(BlockBehaviour.Properties.of().strength(5.0F, 30_000.0F).sound(SoundType.STONE)), ModCreativeTabs.BLOCKS);
        ModBlocks.BLOCK_EUPHEMIUM_CLUSTER = registerBlock("block_euphemium_cluster",
                () -> new BlockRotatablePillar(BlockBehaviour.Properties.of().strength(5.0F, 30_000.0F).sound(SoundType.STONE)), ModCreativeTabs.BLOCKS);
        registerBlock("block_tritium", () -> new BlockRotatablePillar(BlockBehaviour.Properties.of().strength(3.0F, 2.0F).sound(SoundType.GLASS).noOcclusion()),
                ModCreativeTabs.BLOCKS);
        registerBlock("block_insulator", () -> new BlockRotatablePillar(BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.WOOL)),
                ModCreativeTabs.BLOCKS);
        registerBlock("block_fiberglass", () -> new BlockRotatablePillar(BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.WOOL)),
                ModCreativeTabs.BLOCKS);
    }

    private static void registerGenericPwr() {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL);
        registerBlock("pwr_heatex", () -> new BlockGenericPWR(props), ModCreativeTabs.MACHINE);
        registerBlock("pwr_heatsink", () -> new BlockGenericPWR(props), ModCreativeTabs.MACHINE);
        registerBlock("pwr_neutron_source", () -> new BlockGenericPWR(props), ModCreativeTabs.MACHINE);
        registerBlock("pwr_reflector", () -> new BlockGenericPWR(props), ModCreativeTabs.MACHINE);
        registerBlock("pwr_casing", () -> new BlockGenericPWR(props), ModCreativeTabs.MACHINE);
        registerBlock("pwr_port", () -> new BlockGenericPWR(props), ModCreativeTabs.MACHINE);
    }

    private static void registerWoodStructure() {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(5.0F, 15.0F).sound(SoundType.WOOD);
        registerBlock("wood_structure_roof", () -> new BlockWoodStructure(BlockWoodStructure.Kind.ROOF, props), ModCreativeTabs.BLOCKS);
        registerBlock("wood_structure_scaffold", () -> new BlockWoodStructure(BlockWoodStructure.Kind.SCAFFOLD, props), ModCreativeTabs.BLOCKS);
        registerBlock("wood_structure_ceiling", () -> new BlockWoodStructure(BlockWoodStructure.Kind.CEILING, props), ModCreativeTabs.BLOCKS);
    }

    /**
     * CE's sixteen curing-stage metadata values (0 = fresh, 15 = about to collapse), chained via a
     * lazily-resolved {@code nextStage} supplier per {@link BlockUberConcrete}'s own javadoc. Stage
     * 15 gets a {@code null} next-stage: the terminal "collapse into rubble" block
     * ({@code concrete_super_broken}) is not registered by this area, matching that class's own
     * documented scope cut.
     */
    private static void registerUberConcrete() {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(150.0F, 1000.0F).sound(SoundType.STONE);
        int stageCount = 16;
        @SuppressWarnings("unchecked")
        DeferredBlock<BlockUberConcrete>[] stages = new DeferredBlock[stageCount];

        for (int i = 0; i < stageCount; i++) {
            int stage = i;
            Supplier<? extends Block> next;
            if (stage == stageCount - 1) {
                next = null;
            } else {
                next = () -> stages[stage + 1].get();
            }
            stages[stage] = registerBlock("concrete_super_" + stage, () -> new BlockUberConcrete(props, stage, next), ModCreativeTabs.BLOCKS);
        }
    }

    /** CE's sixteen {@link DyeColor} metadata values, one registry entry each. */
    private static void registerConcreteColored() {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(15.0F, 140.0F).sound(SoundType.STONE);
        for (DyeColor color : DyeColor.values()) {
            registerBlock("concrete_" + color.name().toLowerCase(Locale.ROOT), () -> new BlockConcreteColored(props, color), ModCreativeTabs.BLOCKS);
        }
    }

    /** CE's eight {@link BlockConcreteColoredExt.Type} metadata values, one registry entry each. */
    private static void registerConcreteColoredExt() {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(15.0F, 140.0F).sound(SoundType.STONE);
        for (BlockConcreteColoredExt.Type type : BlockConcreteColoredExt.Type.VALUES) {
            registerBlock("concrete_ext_" + type.name().toLowerCase(Locale.ROOT), () -> new BlockConcreteColoredExt(props, type), ModCreativeTabs.BLOCKS);
        }
    }

    private static void registerLayering() {
        registerBlock("foam_layer", () -> new BlockLayering(BlockBehaviour.Properties.of().strength(0.1F).sound(SoundType.SNOW).noOcclusion().replaceable()), null);
        registerBlock("sand_boron_layer", () -> new BlockLayering(BlockBehaviour.Properties.of().strength(0.1F).sound(SoundType.SAND).noOcclusion().replaceable()), null);
        registerBlock("leaves_layer", () -> new BlockLayering(BlockBehaviour.Properties.of().strength(0.1F).sound(SoundType.GRASS).noOcclusion()), null);
        registerBlock("oil_spill", () -> new BlockLayering(BlockBehaviour.Properties.of().strength(0.1F).sound(SoundType.SNOW).noOcclusion()), null);
    }

    /** CE's four {@link BlockScaffold.Variant} item-metadata picks, one registry entry each. */
    private static void registerScaffold() {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(5.0F, 15.0F).sound(SoundType.METAL).noOcclusion();
        registerBlock("scaffold_steel", () -> new BlockScaffold(props, BlockScaffold.Variant.STEEL), ModCreativeTabs.BLOCKS);
        registerBlock("scaffold_red", () -> new BlockScaffold(props, BlockScaffold.Variant.RED), ModCreativeTabs.BLOCKS);
        registerBlock("scaffold_white", () -> new BlockScaffold(props, BlockScaffold.Variant.WHITE), ModCreativeTabs.BLOCKS);
        registerBlock("scaffold_yellow", () -> new BlockScaffold(props, BlockScaffold.Variant.YELLOW), ModCreativeTabs.BLOCKS);
    }

    /** CE's fifteen {@link BlockEnums.PlatemetalType} metadata values, one registry entry each. */
    // ==================== cm_block construction blocks ====================
    
    private static void registerCmBlock() {
        // CE ModBlocks.java cm_block metadata variants (steel/bismoid/desh/resistant).
        // Port registers separate blocks per material (NeoForge 1.21.1 has no metadata).
        // CE :1046-1049 = cm_block x4 variants + derivatives (cm_sheet/cm_tank/cm_port)
        
        // cm_block base variants
        registerBlock("cm_block_steel",
                () -> new BlockBase(BlockBehaviour.Properties.of().strength(5.0F, 600.0F).sound(SoundType.METAL)),
                ModCreativeTabs.BLOCKS);
        registerBlock("cm_block_bismoid_bronze",
                () -> new BlockBase(BlockBehaviour.Properties.of().strength(5.0F, 600.0F).sound(SoundType.METAL)),
                ModCreativeTabs.BLOCKS);
        registerBlock("cm_block_desh",
                () -> new BlockBase(BlockBehaviour.Properties.of().strength(5.0F, 600.0F).sound(SoundType.METAL)),
                ModCreativeTabs.BLOCKS);
        registerBlock("cm_block_resistant",
                () -> new BlockBase(BlockBehaviour.Properties.of().strength(5.0F, 600.0F).sound(SoundType.METAL)),
                ModCreativeTabs.BLOCKS);
        
        // cm_sheet derivatives (thin panels)
        registerBlock("cm_sheet_steel",
                () -> new BlockBase(BlockBehaviour.Properties.of().strength(3.0F, 400.0F).sound(SoundType.METAL)),
                ModCreativeTabs.BLOCKS);
        registerBlock("cm_sheet_bismoid_bronze",
                () -> new BlockBase(BlockBehaviour.Properties.of().strength(3.0F, 400.0F).sound(SoundType.METAL)),
                ModCreativeTabs.BLOCKS);
        registerBlock("cm_sheet_desh",
                () -> new BlockBase(BlockBehaviour.Properties.of().strength(3.0F, 400.0F).sound(SoundType.METAL)),
                ModCreativeTabs.BLOCKS);
        registerBlock("cm_sheet_resistant",
                () -> new BlockBase(BlockBehaviour.Properties.of().strength(3.0F, 400.0F).sound(SoundType.METAL)),
                ModCreativeTabs.BLOCKS);
        
        // cm_tank derivatives (glass-windowed)
        registerBlock("cm_tank_steel",
                () -> new BlockBase(BlockBehaviour.Properties.of().strength(4.0F, 500.0F).sound(SoundType.METAL).noOcclusion()),
                ModCreativeTabs.BLOCKS);
        registerBlock("cm_tank_bismoid_bronze",
                () -> new BlockBase(BlockBehaviour.Properties.of().strength(4.0F, 500.0F).sound(SoundType.METAL).noOcclusion()),
                ModCreativeTabs.BLOCKS);
        registerBlock("cm_tank_desh",
                () -> new BlockBase(BlockBehaviour.Properties.of().strength(4.0F, 500.0F).sound(SoundType.METAL).noOcclusion()),
                ModCreativeTabs.BLOCKS);
        registerBlock("cm_tank_resistant",
                () -> new BlockBase(BlockBehaviour.Properties.of().strength(4.0F, 500.0F).sound(SoundType.METAL).noOcclusion()),
                ModCreativeTabs.BLOCKS);
        
        // cm_port derivatives (I/O connectors)
        registerBlock("cm_port_steel",
                () -> new BlockBase(BlockBehaviour.Properties.of().strength(4.0F, 500.0F).sound(SoundType.METAL)),
                ModCreativeTabs.BLOCKS);
        registerBlock("cm_port_bismoid_bronze",
                () -> new BlockBase(BlockBehaviour.Properties.of().strength(4.0F, 500.0F).sound(SoundType.METAL)),
                ModCreativeTabs.BLOCKS);
        registerBlock("cm_port_desh",
                () -> new BlockBase(BlockBehaviour.Properties.of().strength(4.0F, 500.0F).sound(SoundType.METAL)),
                ModCreativeTabs.BLOCKS);
        registerBlock("cm_port_resistant",
                () -> new BlockBase(BlockBehaviour.Properties.of().strength(4.0F, 500.0F).sound(SoundType.METAL)),
                ModCreativeTabs.BLOCKS);
    }

    private static void registerPlatemetal() {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL);
        for (BlockEnums.PlatemetalType type : BlockEnums.PlatemetalType.VALUES) {
            registerBlock("platemetal_" + type.name().toLowerCase(Locale.ROOT), () -> new BlockPlatemetal(props, type), ModCreativeTabs.BLOCKS);
        }
    }

    // ==================== doors / trapdoors / ladders / glass / pipes ====================

    private static void registerDoorsLaddersGlass() {
        registerDoors();

        registerBlock("trapdoor_steel", () -> new BlockNTMTrapdoor(BlockBehaviour.Properties.of().strength(3.0F, 8.0F).sound(SoundType.METAL).noOcclusion()),
                ModCreativeTabs.BLOCKS);

        registerLadders();
        registerGlass();
        registerPipes();
        registerBlock("pink_log", () -> new BlockPinkLog(BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.WOOD)), null);
    }

    private static void registerDoors() {
        ModBlocks.DOOR_RED = registerModDoor("door_red", () -> new BlockModDoor(BlockBehaviour.Properties.of().strength(10.0F, 100.0F).sound(SoundType.METAL).noOcclusion()));
        registerBlock("stone_keyhole", () -> new BlockKeyhole(BlockBehaviour.Properties.of().strength(1.5F, 6.0F).sound(SoundType.STONE)), null);
        registerModDoor("door_metal", () -> new BlockModDoor(BlockBehaviour.Properties.of().strength(5.0F, 5.0F).sound(SoundType.METAL).noOcclusion()));
        registerModDoor("door_office", () -> new BlockModDoor(BlockBehaviour.Properties.of().strength(10.0F, 10.0F).sound(SoundType.METAL).noOcclusion()));
        registerModDoor("door_bunker", () -> new BlockModDoor(BlockBehaviour.Properties.of().strength(10.0F, 100.0F).sound(SoundType.METAL).noOcclusion()));

        VAULT_DOOR = registerBlock("vault_door", () -> new BlockDoorGeneric(doorProps(500.0F, 1000.0F), DoorDecl.VAULT_DOOR, true), ModCreativeTabs.MACHINE);
        BLAST_DOOR = registerBlock("blast_door", () -> new BlastDoor(doorProps(250.0F, 1000.0F)), ModCreativeTabs.MACHINE);
        FIRE_DOOR = registerBlock("fire_door", () -> new BlockDoorGeneric(doorProps(100.0F, 1000.0F), DoorDecl.FIRE_DOOR, true), ModCreativeTabs.MACHINE);
        SLIDING_BLAST_DOOR = registerBlock("sliding_blast_door", () -> new BlockDoorGeneric(doorProps(150.0F, 750.0F), DoorDecl.SLIDE_DOOR, false), ModCreativeTabs.MACHINE);
        SLIDING_BLAST_DOOR_LEGACY = registerBlock("sliding_blast_door_legacy", () -> new BlockSlidingBlastDoor(doorProps(150.0F, 750.0F)), ModCreativeTabs.MACHINE);
        SLIDING_BLAST_DOOR_2 = registerBlock("sliding_blast_door_2", () -> new BlockSlidingBlastDoor(doorProps(150.0F, 750.0F)), ModCreativeTabs.MACHINE);
        SLIDING_BLAST_DOOR_KEYPAD = registerBlock("sliding_blast_door_keypad", () -> new BlockSlidingBlastDoor(doorProps(150.0F, 750.0F)), null);
        KEYPAD_TEST = registerBlock("keypad_test", () -> new KeypadTestBlock(
                BlockBehaviour.Properties.of().strength(15.0F, 7500.0F).sound(SoundType.METAL)), null);
        LARGE_VEHICLE_DOOR = registerBlock("large_vehicle_door", () -> new BlockDoorGeneric(doorProps(100.0F, 1000.0F), DoorDecl.LARGE_VEHICLE_DOOR, true), ModCreativeTabs.MACHINE);
        WATER_DOOR = registerBlock("water_door", () -> new BlockDoorGeneric(doorProps(50.0F, 500.0F), DoorDecl.WATER_DOOR, false), ModCreativeTabs.MACHINE);
        QE_CONTAINMENT = registerBlock("qe_containment", () -> new BlockDoorGeneric(doorProps(100.0F, 1000.0F), DoorDecl.QE_CONTAINMENT, true), ModCreativeTabs.MACHINE);
        QE_SLIDING_DOOR = registerBlock("qe_sliding_door", () -> new BlockDoorGeneric(doorProps(100.0F, 500.0F), DoorDecl.QE_SLIDING, false), ModCreativeTabs.MACHINE);
        ROUND_AIRLOCK_DOOR = registerBlock("round_airlock_door", () -> new BlockDoorGeneric(doorProps(100.0F, 1000.0F), DoorDecl.ROUND_AIRLOCK_DOOR, true), ModCreativeTabs.MACHINE);
        SECURE_ACCESS_DOOR = registerBlock("secure_access_door", () -> new BlockDoorGeneric(doorProps(200.0F, 2000.0F), DoorDecl.SECURE_ACCESS_DOOR, true), ModCreativeTabs.MACHINE);
        SLIDING_SEAL_DOOR = registerBlock("sliding_seal_door", () -> new BlockDoorGeneric(doorProps(10.0F, 1000.0F), DoorDecl.SLIDING_SEAL_DOOR, false), ModCreativeTabs.MACHINE);
        CARGO_DOOR = registerBlock("cargo_door", () -> new BlockDoorGeneric(doorProps(5.0F, 50.0F), DoorDecl.CARGO_DOOR, false), ModCreativeTabs.MACHINE);
        SILO_HATCH = registerBlock("silo_hatch", () -> new BlockDoorGeneric(doorProps(10.0F, 100.0F), DoorDecl.SILO_HATCH, false), ModCreativeTabs.MACHINE);
        SILO_HATCH_LARGE = registerBlock("silo_hatch_large", () -> new BlockDoorGeneric(doorProps(10.0F, 100.0F), DoorDecl.SILO_HATCH_LARGE, false), ModCreativeTabs.MACHINE);
        TRANSITION_SEAL = registerBlock("transition_seal", () -> new BlockDoorGeneric(doorProps(1000.0F, 1_000_000.0F), DoorDecl.TRANSITION_SEAL, true), ModCreativeTabs.MACHINE);
        DUMMY_BLOCK_BLAST = ModBlocks.BLOCKS.register("dummy_block_blast",
                () -> new DummyBlockBlast(BlockBehaviour.Properties.of().strength(500.0F, 10_000.0F).sound(SoundType.METAL).noOcclusion().noLootTable()));

        DoorGenericBlockEntities.registerAll();
    }

    private static BlockBehaviour.Properties doorProps(float hardness, float resistance) {
        return BlockBehaviour.Properties.of().strength(hardness, resistance).sound(SoundType.METAL).noOcclusion();
    }

    /** CE's twelve registrations; only {@code ladder_red_top} sets {@code capTop}. */
    private static void registerLadders() {
        // SoundType.LADDER is a real vanilla constant, but every one of CE's twelve ladder
        // registrations is a metal variant (iron/gold/aluminium/.../steel/red) - SoundType.METAL
        // (already confirmed elsewhere in this codebase) is the more faithful choice regardless.
        // Deliberately no noCollission(): a ladder is climbable via its own thin getShape(), not by
        // having zero collision outright (vanilla's own Blocks.LADDER properties omit it too).
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(0.25F, 2.0F).sound(SoundType.METAL).noOcclusion();
        String[] names = {
                "ladder_sturdy", "ladder_iron", "ladder_gold", "ladder_aluminium", "ladder_copper",
                "ladder_titanium", "ladder_lead", "ladder_cobalt", "ladder_steel", "ladder_tungsten", "ladder_red"
        };
        for (String name : names) {
            registerBlock(name, () -> new BlockNTMLadder(props), ModCreativeTabs.BLOCKS);
        }
        registerBlock("ladder_red_top", () -> new BlockNTMLadder(props, true), ModCreativeTabs.BLOCKS);
    }

    private static void registerGlass() {
        registerBlock("reinforced_glass", () -> new BlockNTMGlass(glassProps(0.3F, 25.0F), false, true), ModCreativeTabs.BLOCKS);
        registerBlock("reinforced_laminate", () -> new BlockNTMGlass(glassProps(15.0F, 300.0F), true, true), ModCreativeTabs.BLOCKS);
        registerBlock("glass_uranium", () -> new BlockNTMGlass(glassProps(0.3F, 10.0F).lightLevel(state -> 5)), ModCreativeTabs.BLOCKS);
        registerBlock("glass_trinitite", () -> new BlockNTMGlass(glassProps(0.3F, 10.0F).lightLevel(state -> 5)), ModCreativeTabs.BLOCKS);
        registerBlock("glass_polonium", () -> new BlockNTMGlass(glassProps(0.3F, 10.0F).lightLevel(state -> 5)), ModCreativeTabs.BLOCKS);
        registerBlock("glass_boron", () -> new BlockNTMGlass(glassProps(0.3F, 10.0F), true, true), ModCreativeTabs.BLOCKS);
        registerBlock("glass_lead", () -> new BlockNTMGlass(glassProps(0.3F, 10.0F), true, true), ModCreativeTabs.BLOCKS);
        registerBlock("glass_ash", () -> new BlockNTMGlass(glassProps(3.0F, 10.0F)), ModCreativeTabs.BLOCKS);
        registerBlock("glass_quartz", () -> new BlockNTMGlass(glassProps(1.0F, 40.0F), true), ModCreativeTabs.BLOCKS);
        registerBlock("glass_polarized", () -> new BlockNTMGlass(glassProps(0.3F, 10.0F)), ModCreativeTabs.BLOCKS);

        registerBlock("reinforced_glass_pane", () -> new BlockNTMGlassPane(glassProps(2.0F, 25.0F), false, true), ModCreativeTabs.BLOCKS);
        registerBlock("reinforced_laminate_pane", () -> new BlockNTMGlassPane(glassProps(15.0F, 300.0F), true, true), ModCreativeTabs.BLOCKS);
    }

    private static BlockBehaviour.Properties glassProps(float hardness, float resistance) {
        return BlockBehaviour.Properties.of().strength(hardness, resistance).sound(SoundType.GLASS).noOcclusion();
    }

    /** CE's twenty-four framed/rimmed/quad pipe-segment textures, all sharing one behavior class. */
    private static void registerPipes() {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(2.0F, 5.0F).sound(SoundType.METAL).noOcclusion();
        String[] names = {
                "deco_pipe", "deco_pipe_rusted", "deco_pipe_green", "deco_pipe_green_rusted",
                "deco_pipe_red", "deco_pipe_marked",
                "deco_pipe_rim", "deco_pipe_rim_rusted", "deco_pipe_rim_green", "deco_pipe_rim_green_rusted",
                "deco_pipe_rim_red", "deco_pipe_rim_marked",
                "deco_pipe_framed", "deco_pipe_framed_rusted", "deco_pipe_framed_green", "deco_pipe_framed_green_rusted",
                "deco_pipe_framed_red", "deco_pipe_framed_marked",
                "deco_pipe_quad", "deco_pipe_quad_rusted", "deco_pipe_quad_green", "deco_pipe_quad_green_rusted",
                "deco_pipe_quad_red", "deco_pipe_quad_marked"
        };
        for (String name : names) {
            registerBlock(name, () -> new BlockPipe(props), ModCreativeTabs.BLOCKS);
        }
    }

    // ==================== ore / mineral (non-hazardous) ====================

    private static void registerOreMineral() {
        registerBlock("ore_bedrock_coltan",
                () -> new BlockBedrockOre(BlockBehaviour.Properties.of().strength(-1.0F, 3_600_000.0F).sound(SoundType.STONE),
                        () -> PlateCrystalWasteItems.FRAGMENT_COLTAN.get()),
                ModCreativeTabs.RESOURCE);

        registerDepth();
        registerResourceStone();
        registerStalagmites();
        registerMeteorOre();
    }

    private static void registerDepth() {
        BlockBehaviour.Properties standard = BlockBehaviour.Properties.of().strength(-1.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops();
        registerBlock("stone_depth", () -> new BlockDepth(standard), ModCreativeTabs.RESOURCE);
        registerBlock("stone_depth_nether", () -> new BlockDepth(standard), ModCreativeTabs.RESOURCE);
        registerBlock("depth_brick", () -> new BlockDepth(standard), ModCreativeTabs.BLOCKS);
        registerBlock("depth_tiles", () -> new BlockDepth(standard), ModCreativeTabs.BLOCKS);
        registerBlock("depth_nether_brick", () -> new BlockDepth(standard), ModCreativeTabs.BLOCKS);
        registerBlock("depth_nether_tiles", () -> new BlockDepth(standard), ModCreativeTabs.BLOCKS);

        BlockBehaviour.Properties dnt = BlockBehaviour.Properties.of().strength(-1.0F, 60_000.0F).sound(SoundType.STONE).requiresCorrectToolForDrops();
        registerBlock("depth_dnt", () -> new BlockDepth(dnt), ModCreativeTabs.BLOCKS);
    }

    /** CE's six {@link BlockEnums.EnumStoneType} metadata values, one registry entry each. */
    private static void registerResourceStone() {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops();
        for (BlockEnums.EnumStoneType type : BlockEnums.EnumStoneType.VALUES) {
            registerBlock("stone_resource_" + type.name().toLowerCase(Locale.ROOT), () -> new BlockResourceStone(props, type), ModCreativeTabs.RESOURCE);
        }
    }

    /**
     * CE's two registry names ({@code stalagmite} floor-grown, {@code stalactite} ceiling-hung),
     * each carrying both {@link BlockEnums.EnumStalagmiteType} values via item metadata; flattened
     * to four registry entries total per {@link BlockStalagmite}'s own javadoc.
     */
    private static void registerStalagmites() {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(0.5F, 2.0F).sound(SoundType.STONE).noOcclusion();
        for (BlockEnums.EnumStalagmiteType type : BlockEnums.EnumStalagmiteType.VALUES) {
            String suffix = type.name().toLowerCase(Locale.ROOT);
            registerBlock("stalagmite_" + suffix, () -> new BlockStalagmite(props, type, false), ModCreativeTabs.BLOCKS);
            registerBlock("stalactite_" + suffix, () -> new BlockStalagmite(props, type, true), ModCreativeTabs.BLOCKS);
        }
    }

    /** CE's {@code ore_meteor}, five {@link BlockEnums.EnumMeteorType} metadata values. */
    private static void registerMeteorOre() {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops();
        for (BlockEnums.EnumMeteorType type : BlockEnums.EnumMeteorType.VALUES) {
            registerBlock("block_meteor_ore_" + type.name().toLowerCase(Locale.ROOT), () -> new BlockMeteorOre(props, type), ModCreativeTabs.BLOCKS);
        }
    }

    // ==================== hazard-adjacent but self-contained ====================

    private static void registerHazardAdjacent() {
        registerBlock("chlorine_gas", () -> new BlockClorine(BlockBehaviour.Properties.of().strength(0.0F, 0.0F).noCollission().noOcclusion().randomTicks()),
                ModCreativeTabs.MACHINE);

        // CE's block_lithium is not registered here: Mats.java tags MAT_LITHIUM with
        // MaterialShapes.BLOCK autogen, so MaterialBlockGenerator.registerAll() already registers
        // it as lithium_block via this same BlockHydroreactive class - registering it again here
        // would be duplicate in-game content under two different ids for the same material (the
        // identical bismuth/tantalium/niobium/lanthanium/zirconium fix already applied to
        // GenericDecoBlocks.registerBeaconable()).

        registerBlock("ore_nether_smoldering",
                () -> new BlockSmolder(BlockBehaviour.Properties.of().strength(0.4F, 10.0F).sound(SoundType.STONE).lightLevel(state -> 1)),
                ModCreativeTabs.RESOURCE);

        registerReinforcedLamps();
        registerRadResistant();
        registerClean();

        registerBlock("barricade", () -> new BlockNoDrop(BlockBehaviour.Properties.of().strength(1.0F, 2.5F).sound(SoundType.SAND)), null);
        registerBlock("oil_pipe", () -> new BlockNoDrop(BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL)), null);
        registerBlock("drill_pipe", () -> new BlockNoDrop(BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL)), null);

        registerBarbedWire();

        registerBlock("spikes", () -> new Spikes(BlockBehaviour.Properties.of().strength(2.5F, 5.0F).sound(SoundType.METAL).noOcclusion()),
                ModCreativeTabs.BLOCKS);
    }

    /** CE's on/off pair, cross-wired via a lazy companion supplier per {@link ReinforcedLamp}'s own javadoc. */
    private static void registerReinforcedLamps() {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(15.0F, 80.0F).sound(SoundType.STONE).lightLevel(state -> 15);
        AtomicReference<ReinforcedLamp> offRef = new AtomicReference<>();
        AtomicReference<ReinforcedLamp> onRef = new AtomicReference<>();

        registerBlock("reinforced_lamp_off", () -> {
            ReinforcedLamp block = new ReinforcedLamp(props, false);
            block.setCompanion(onRef::get);
            offRef.set(block);
            return block;
        }, ModCreativeTabs.BLOCKS);

        registerBlock("reinforced_lamp_on", () -> {
            ReinforcedLamp block = new ReinforcedLamp(props, true);
            block.setCompanion(offRef::get);
            onRef.set(block);
            return block;
        }, null);
    }

    /**
     * CE's {@code block_boron} and {@code block_lead} are deliberately NOT registered here even
     * though CE has them as {@link BlockRadResistant} shielding-marker blocks: {@code Mats.java}
     * tags {@code MAT_BORON}/{@code MAT_LEAD} with {@code MaterialShapes.BLOCK} autogen, so
     * {@link com.hbm.blocks.MaterialBlockGenerator} already registers them (as suffix-first
     * {@code boron_block}/{@code lead_block}, via this same {@link BlockRadResistant} class) -
     * registering them again here would be duplicate in-game content under two different ids for
     * the same material, the identical issue already fixed for bismuth/tantalium/niobium/
     * lanthanium/zirconium in {@code GenericDecoBlocks.registerBeaconable()}.
     */
    private static void registerRadResistant() {
        registerBlock("reinforced_light", () -> new BlockRadResistant(BlockBehaviour.Properties.of().strength(15.0F, 80.0F).sound(SoundType.STONE).lightLevel(state -> 15)),
                ModCreativeTabs.BLOCKS);
        registerBlock("brick_concrete", () -> new BlockRadResistant(BlockBehaviour.Properties.of().strength(15.0F, 160.0F).sound(SoundType.STONE), true),
                ModCreativeTabs.BLOCKS);
        registerBlock("brick_concrete_mossy", () -> new BlockRadResistant(BlockBehaviour.Properties.of().strength(15.0F, 160.0F).sound(SoundType.STONE)),
                ModCreativeTabs.BLOCKS);
        registerBlock("reinforced_brick", () -> new BlockRadResistant(BlockBehaviour.Properties.of().strength(15.0F, 300.0F).sound(SoundType.STONE)),
                ModCreativeTabs.BLOCKS);
        // CE: new BlockBase(Material.ROCK, "reinforced_sand").setHardness(15.0F).setResistance(40.0F)
        // - the fifth member of ExplosionChaos.explode's indestructible-block list (see
        // docs/phase4/entities_vortex_gravity_wells.md Table B); its four siblings
        // (reinforced_glass/reinforced_lamp_on/reinforced_lamp_off above, reinforced_brick just above)
        // were already registered here.
        registerBlock("reinforced_sand", () -> new BlockBase(BlockBehaviour.Properties.of().strength(15.0F, 40.0F).sound(SoundType.STONE)),
                ModCreativeTabs.BLOCKS);
        registerBlock("brick_compound", () -> new BlockRadResistant(BlockBehaviour.Properties.of().strength(15.0F, 400.0F).sound(SoundType.STONE)),
                ModCreativeTabs.BLOCKS);
        registerBlock("cmb_brick_reinforced", () -> new BlockRadResistant(BlockBehaviour.Properties.of().strength(25.0F, 50_000.0F).sound(SoundType.METAL)),
                ModCreativeTabs.BLOCKS);
        registerBlock("ducrete_smooth", () -> new BlockRadResistant(BlockBehaviour.Properties.of().strength(20.0F, 500.0F).sound(SoundType.STONE), true),
                ModCreativeTabs.BLOCKS);
        registerBlock("ducrete", () -> new BlockRadResistant(BlockBehaviour.Properties.of().strength(20.0F, 500.0F).sound(SoundType.STONE), true),
                ModCreativeTabs.BLOCKS);
        registerBlock("ducrete_brick", () -> new BlockRadResistant(BlockBehaviour.Properties.of().strength(15.0F, 750.0F).sound(SoundType.STONE), true),
                ModCreativeTabs.BLOCKS);
        registerBlock("ducrete_reinforced", () -> new BlockRadResistant(BlockBehaviour.Properties.of().strength(20.0F, 1000.0F).sound(SoundType.STONE), true),
                ModCreativeTabs.BLOCKS);
        registerBlock("block_niter_reinforced", () -> new BlockRadResistant(BlockBehaviour.Properties.of().strength(15.0F, 6000.0F).sound(SoundType.METAL)),
                ModCreativeTabs.BLOCKS);
        registerBlock("block_australium", () -> new BlockRadResistant(BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL)),
                ModCreativeTabs.BLOCKS);
        registerBlock("hazmat", () -> new BlockRadResistant(BlockBehaviour.Properties.of().strength(15.0F, 50.0F).sound(SoundType.WOOL)),
                ModCreativeTabs.BLOCKS);
    }

    private static void registerClean() {
        ModBlocks.TILE_LAB = registerBlock("tile_lab", () -> new BlockClean(BlockBehaviour.Properties.of().strength(1.0F, 20.0F).sound(SoundType.GLASS)), ModCreativeTabs.BLOCKS);
        ModBlocks.TILE_LAB_CRACKED = registerBlock("tile_lab_cracked", () -> new BlockClean(BlockBehaviour.Properties.of().strength(1.0F, 20.0F).sound(SoundType.GLASS)), ModCreativeTabs.BLOCKS);
        ModBlocks.TILE_LAB_BROKEN = registerBlock("tile_lab_broken", () -> new BlockClean(BlockBehaviour.Properties.of().strength(1.0F, 20.0F).sound(SoundType.GLASS)), ModCreativeTabs.BLOCKS);
        registerBlock("deco_rbmk", () -> new BlockClean(BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL)), ModCreativeTabs.BLOCKS);
        registerBlock("deco_rbmk_smooth", () -> new BlockClean(BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL)), ModCreativeTabs.BLOCKS);
        registerBlock("deco_rbmk_panel", () -> new BlockClean(BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL)), ModCreativeTabs.BLOCKS);
        registerBlock("deco_rbmk_smooth_panel", () -> new BlockClean(BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL)), ModCreativeTabs.BLOCKS);
    }

    private static void registerBarbedWire() {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL).noCollission().noOcclusion();
        registerBlock("barbed_wire", () -> new BarbedWire(props, BarbedWire.Type.STANDARD), ModCreativeTabs.BLOCKS);
        registerBlock("barbed_wire_fire", () -> new BarbedWire(props, BarbedWire.Type.FIRE), ModCreativeTabs.BLOCKS);
        registerBlock("barbed_wire_poison", () -> new BarbedWire(props, BarbedWire.Type.POISON), ModCreativeTabs.BLOCKS);
        registerBlock("barbed_wire_acid", () -> new BarbedWire(props, BarbedWire.Type.ACID), ModCreativeTabs.BLOCKS);
        registerBlock("barbed_wire_wither", () -> new BarbedWire(props, BarbedWire.Type.WITHER), ModCreativeTabs.BLOCKS);
        registerBlock("barbed_wire_ultradeath", () -> new BarbedWire(props, BarbedWire.Type.ULTRADEATH), ModCreativeTabs.BLOCKS);
    }

    // ==================== speed / tool-interaction ====================

    private static void registerSpeedAndTool() {
        registerBlock("asphalt", () -> new BlockSpeedy(BlockBehaviour.Properties.of().strength(15.0F, 120.0F).sound(SoundType.STONE), 1.5), ModCreativeTabs.BLOCKS);
        registerBlock("asphalt_light",
                () -> new BlockSpeedy(BlockBehaviour.Properties.of().strength(15.0F, 120.0F).sound(SoundType.STONE).lightLevel(state -> 1), 1.5),
                ModCreativeTabs.BLOCKS);
        // CE's asphalt_stairs (a BlockSpeedyStairs paired with the "asphalt" block above) is not
        // registered here: BlockSpeedyStairs's constructor takes a resolved Block, not a lazy
        // supplier, and calling asphalt's DeferredBlock#get() from inside another block's own
        // registration-time factory is not a safe cross-DeferredRegister ordering guarantee (see
        // this class's own javadoc and the port report). BlockSpeedyStairs itself still compiles
        // and is available once a future pass wants to wire that link up properly.

        registerBlock("watz_casing", () -> new BlockToolConversion(BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL)),
                ModCreativeTabs.MACHINE);
    }

    // ==================== metadata-only decoration ====================

    private static void registerMetadataDecoration() {
        BlockBehaviour.Properties capProps = BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL);
        for (BlockEnums.EnumBlockCapType type : BlockEnums.EnumBlockCapType.VALUES) {
            registerBlock("block_cap_" + type.name().toLowerCase(Locale.ROOT), () -> new BlockCap(capProps, type), ModCreativeTabs.BLOCKS);
        }

        BlockBehaviour.Properties cokeProps = BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL);
        for (EnumCokeType type : EnumCokeType.VALUES) {
            registerBlock("block_coke_" + type.name().toLowerCase(Locale.ROOT), () -> new BlockCoke(cokeProps, type), ModCreativeTabs.BLOCKS);
        }

        BlockBehaviour.Properties lightstoneProps = BlockBehaviour.Properties.of().strength(20.0F, 20.0F).sound(SoundType.STONE);
        for (BlockEnums.LightstoneType type : BlockEnums.LightstoneType.VALUES) {
            registerBlock("lightstone_" + type.name().toLowerCase(Locale.ROOT), () -> new BlockLightstone(lightstoneProps, type), ModCreativeTabs.BLOCKS);
        }

        // BlockFlammable carries no CE instance to register - see this class's own javadoc.
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

    /** Exact CE {@code ItemModDoor} — {@code DoorItem} two-tall place, stack 1. Tab stays null. */
    private static DeferredBlock<BlockModDoor> registerModDoor(String name, Supplier<BlockModDoor> factory) {
        DeferredBlock<BlockModDoor> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new ItemModDoor(block.get(), new Item.Properties()));
        return block;
    }
}
