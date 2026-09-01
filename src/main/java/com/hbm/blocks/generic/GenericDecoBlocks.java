package com.hbm.blocks.generic;

import com.hbm.blocks.BlockEnums;
import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Table-driven registration for CE's decorative-prop family (upstream hbm-ce {@code ModBlocks.java}
 * {@code DecoBlock}/{@code DecoBlockAlt}/{@code DecoPoleSatelliteReceiver}/{@code DecoPoleTop}/
 * {@code DecoSteelPoles}/{@code DecoTapeRecorder}/{@code BlockDecoModel}/{@code BlockDecoCRT}/
 * {@code BlockDecoToaster}/{@code BlockBakedLayered}/{@code BlockBarrier}/{@code BlockBakeOld}/
 * {@code BlockFallingBaked}/{@code BlockBeaconable}/{@code BlockWriting}/{@code HEVBattery}/
 * {@code BlockWand} instances) - see {@code docs/phase1/blocks_generic.md}'s "Deco (visual props)"
 * section. Mirrors {@link com.hbm.blocks.OreBlocks}'s table-driven-{@code registerAll()} shape.
 * <p>
 * <b>{@link BlockBakeOld}.</b> CE's only concrete instance ({@code ModBlocks.absorber} and its
 * red/green/pink siblings) is itself {@code @Deprecated} in CE and depends on the Phase-2
 * {@code BlockAbsorber}/radiation-block family for the state it ticks into; registering a stand-in
 * instance here would mean inventing content this area has no way to verify. Per
 * {@link BlockBakeOld}'s own class javadoc ("no instance is registered from this area - this class
 * is left as reusable infrastructure"), no {@code BlockBakeOld} instance is registered below; the
 * class itself is still compiled and available for whichever phase ports the absorber family.
 */
public final class GenericDecoBlocks {

    private static final float DECO_HARDNESS = 5.0F;
    private static final float DECO_RESISTANCE = 15.0F;
    private static final float UNBREAKABLE_RESISTANCE = 3_600_000.0F;

    public static Supplier<BlockEntityType<DecoBlockAlt.StatuePulseBlockEntity>> STATUE_PULSE_ENTITY_TYPE;
    public static Supplier<BlockEntityType<LanternBlockEntity>> LANTERN_ENTITY_TYPE;
    public static Supplier<BlockEntityType<LanternBehemothBlockEntity>> LANTERN_BEHEMOTH_ENTITY_TYPE;

    private GenericDecoBlocks() {
    }

    public static void registerAll() {
        registerDecoBlocks();
        registerDecoBlockAlt();
        registerPoles();
        registerDecoCRT();
        registerDecoToaster();
        registerDecoModel();
        registerBakedLayered();
        registerBarrier();
        registerFallingBaked();
        registerBeaconable();
        registerWriting();
        registerHevBattery();
        registerWand();
        registerLantern();
    }

    private static void registerDecoBlocks() {
        registerBlock("steel_wall", () -> new DecoBlock(deco15Props(), DecoBlock.Shape.WALL), ModCreativeTabs.BLOCKS);
        registerBlock("steel_corner", () -> new DecoBlock(BlockBehaviour.Properties.of().strength(15.0F, DECO_RESISTANCE).sound(SoundType.METAL), DecoBlock.Shape.CORNER),
                ModCreativeTabs.BLOCKS);
        registerBlock("steel_roof", () -> new DecoBlock(deco15Props(), DecoBlock.Shape.ROOF), ModCreativeTabs.BLOCKS);
        registerBlock("steel_beam", () -> new DecoBlock(deco15Props(), DecoBlock.Shape.BEAM), ModCreativeTabs.BLOCKS);

        // CE's deco_sat_* dish props also reuse plain DecoBlock (Shape.PLAIN), hardness 5.0F/10.0F.
        registerBlock("deco_sat_mapper", () -> new DecoBlock(satProps(), DecoBlock.Shape.PLAIN), ModCreativeTabs.BLOCKS);
        registerBlock("deco_sat_radar", () -> new DecoBlock(satProps(), DecoBlock.Shape.PLAIN), ModCreativeTabs.BLOCKS);
        registerBlock("deco_sat_scanner", () -> new DecoBlock(satProps(), DecoBlock.Shape.PLAIN), ModCreativeTabs.BLOCKS);
        registerBlock("deco_sat_laser", () -> new DecoBlock(satProps(), DecoBlock.Shape.PLAIN), ModCreativeTabs.BLOCKS);
        registerBlock("deco_sat_foeq", () -> new DecoBlock(satProps(), DecoBlock.Shape.PLAIN), ModCreativeTabs.BLOCKS);
        registerBlock("deco_sat_resonator", () -> new DecoBlock(satProps(), DecoBlock.Shape.PLAIN), ModCreativeTabs.BLOCKS);
        // CE ModBlocks.java:1515 / :1517 — unused 1.7 leftovers, still registered cubes.
        registerBlock("boxcar", () -> new DecoBlock(deco15Props(), DecoBlock.Shape.PLAIN), ModCreativeTabs.BLOCKS);
        registerBlock("boat", () -> new DecoBlock(deco15Props(), DecoBlock.Shape.PLAIN), ModCreativeTabs.BLOCKS);
    }

    private static void registerDecoBlockAlt() {
        // CE: getItemDropped always returns statue_elb regardless of which stage was broken. The
        // base-statue supplier is wired up from inside each block's own construction lambda (rather
        // than via DeferredBlock.get() right after registerBlock() returns) because .get() throws
        // until the BLOCKS registry's RegisterEvent has actually fired, which has not happened yet
        // at registerAll() time - baseStatueRef is instead populated whenever statue_elb's own
        // factory lambda eventually runs, and every stage's getDrops() only reads it later, at
        // actual gameplay time, well after all four blocks exist.
        java.util.concurrent.atomic.AtomicReference<DecoBlockAlt> baseStatueRef = new java.util.concurrent.atomic.AtomicReference<>();
        Supplier<DecoBlockAlt> baseStatueSupplier = baseStatueRef::get;

        registerBlock("statue_elb", () -> {
            DecoBlockAlt block = new DecoBlockAlt(unbreakableProps(), false);
            block.setBaseStatue(baseStatueSupplier);
            baseStatueRef.set(block);
            return block;
        }, null);
        registerBlock("statue_elb_g", () -> {
            DecoBlockAlt block = new DecoBlockAlt(unbreakableProps(), false);
            block.setBaseStatue(baseStatueSupplier);
            return block;
        }, null);
        registerBlock("statue_elb_w", () -> {
            DecoBlockAlt block = new DecoBlockAlt(unbreakableProps(), false);
            block.setBaseStatue(baseStatueSupplier);
            return block;
        }, null);
        DeferredBlock<DecoBlockAlt> statueElbF = registerBlock("statue_elb_f", () -> {
            DecoBlockAlt block = new DecoBlockAlt(unbreakableProps().lightLevel(state -> 15), true);
            block.setBaseStatue(baseStatueSupplier);
            return block;
        }, null);

        STATUE_PULSE_ENTITY_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("statue_elb_f",
                () -> BlockEntityType.Builder.of(DecoBlockAlt.StatuePulseBlockEntity::new, statueElbF.get()).build(null));
    }

    private static void registerPoles() {
        registerBlock("tape_recorder", () -> new DecoTapeRecorder(deco15Props()), ModCreativeTabs.BLOCKS);
        registerBlock("steel_poles", () -> new DecoSteelPoles(deco15Props()), ModCreativeTabs.BLOCKS);
        registerBlock("pole_top", () -> new DecoPoleTop(deco15Props()), ModCreativeTabs.BLOCKS);
        registerBlock("pole_satellite_receiver", () -> new DecoPoleSatelliteReceiver(deco15Props()), ModCreativeTabs.BLOCKS);
        // CE ModBlocks.deco_steel / steel_scaffold — missing from the original deco table; Antenna
        // (CE Antenna.java:48) and ItemPoolsLegacy.POOL_ANTENNA need both.
        registerBlock("deco_steel", () -> new DecoBlock(deco15Props(), DecoBlock.Shape.PLAIN), ModCreativeTabs.BLOCKS);
        registerBlock("steel_scaffold", () -> new DecoBlock(deco15Props(), DecoBlock.Shape.BEAM), ModCreativeTabs.BLOCKS);
    }

    private static void registerDecoCRT() {
        for (BlockEnums.DecoCRTEnum value : BlockEnums.DecoCRTEnum.VALUES) {
            BlockDecoCRT.Variant variant = BlockDecoCRT.Variant.valueOf(value.name());
            registerBlock("deco_crt_" + value.name().toLowerCase(java.util.Locale.ROOT), () -> new BlockDecoCRT(deco10Props(), variant), ModCreativeTabs.BLOCKS);
        }
    }

    private static void registerDecoToaster() {
        for (BlockEnums.DecoToasterEnum value : BlockEnums.DecoToasterEnum.VALUES) {
            BlockDecoToaster.Variant variant = BlockDecoToaster.Variant.valueOf(value.name());
            registerBlock("deco_toaster_" + value.name().toLowerCase(java.util.Locale.ROOT), () -> new BlockDecoToaster(deco10Props(), variant), ModCreativeTabs.BLOCKS);
        }
    }

    private static void registerDecoModel() {
        for (BlockEnums.DecoComputerEnum value : BlockEnums.DecoComputerEnum.VALUES) {
            registerBlock("deco_computer_" + value.name().toLowerCase(java.util.Locale.ROOT), () -> new BlockDecoModel(deco10Props(), value), ModCreativeTabs.BLOCKS);
        }
    }

    private static void registerBakedLayered() {
        // CE's own only concrete BlockBakedLayered instance (concrete_super_broken) is actually a
        // BlockFallingBaked (see registerFallingBaked()); no plain non-falling CE instance of this
        // class was found to carry over, so this method registers a generic named entry using the
        // same base hardness/resistance CE gives that sibling block, matching the class's own
        // "no TE, real content is the layer mechanic" javadoc.
        registerBlock("deco_layered", () -> new BlockBakedLayered(BlockBehaviour.Properties.of().strength(10.0F, 20.0F).sound(SoundType.STONE)),
                ModCreativeTabs.BLOCKS);
    }

    private static void registerBarrier() {
        registerBlock("wood_barrier", () -> new BlockBarrier(BlockBehaviour.Properties.of().strength(DECO_HARDNESS, DECO_RESISTANCE).sound(SoundType.WOOD)),
                ModCreativeTabs.BLOCKS);
    }

    private static void registerFallingBaked() {
        registerBlock("concrete_super_broken", () -> new BlockFallingBaked(BlockBehaviour.Properties.of().strength(10.0F, 20.0F).sound(SoundType.STONE)),
                ModCreativeTabs.BLOCKS);
    }

    /**
     * {@code block_bismuth}/{@code block_tantalium}/{@code block_niobium}/{@code block_lanthanium}/
     * {@code block_zirconium} are deliberately NOT registered here even though CE has them as
     * {@code BlockBeaconable} storage blocks: {@code Mats.java} tags all 5 materials with
     * {@code MaterialShapes.BLOCK} autogen, so {@link com.hbm.blocks.MaterialBlockGenerator}
     * already registers them (as suffix-first {@code bismuth_block}/etc., per the project's
     * established Mats-driven naming convention) - registering them again here would be duplicate
     * in-game content under two different ids for the same material. {@code block_cadmium}/
     * {@code block_coltan}/{@code block_actinium} stay here under CE's legacy prefix-first ids
     * because none of those three materials carry {@code MaterialShapes.BLOCK} in {@code Mats.java}
     * yet, so this class remains their only source.
     */
    private static void registerBeaconable() {
        registerBlock("block_cadmium", () -> new BlockBeaconable(metalProps(30.0F)), ModCreativeTabs.BLOCKS);
        registerBlock("block_coltan", () -> new BlockBeaconable(metalProps(30.0F)), ModCreativeTabs.BLOCKS);

        registerBlock("block_actinium", () -> new BlockBeaconable(metalProps(10.0F)), ModCreativeTabs.BLOCKS);

        registerBlock("block_polymer", () -> new BlockBeaconable(BlockBehaviour.Properties.of().strength(3.0F, 10.0F).sound(SoundType.METAL)), ModCreativeTabs.BLOCKS);
        registerBlock("block_bakelite", () -> new BlockBeaconable(BlockBehaviour.Properties.of().strength(3.0F, 10.0F).sound(SoundType.STONE)), ModCreativeTabs.BLOCKS);
        registerBlock("block_rubber", () -> new BlockBeaconable(BlockBehaviour.Properties.of().strength(3.0F, 10.0F).sound(SoundType.STONE)), ModCreativeTabs.BLOCKS);
    }

    private static void registerWriting() {
        registerBlock("brick_concrete_marked", () -> new BlockWriting(BlockBehaviour.Properties.of().strength(15.0F, 160.0F).sound(SoundType.STONE)),
                ModCreativeTabs.BLOCKS);
    }

    private static void registerHevBattery() {
        registerBlock("hev_battery_block", () -> new HEVBattery(BlockBehaviour.Properties.of().strength(15.0F, 0.25F).sound(SoundType.METAL)),
                ModCreativeTabs.MACHINE);
    }

    private static void registerLantern() {
        // CE ModBlocks.java:281 — BlockLantern Dummyable {4,0,0,0,0,0}, blinds glyphids.
        DeferredBlock<BlockLantern> lantern = registerBlock("lantern",
                () -> new BlockLantern(deco15Props().lightLevel(state -> 15).noOcclusion()),
                ModCreativeTabs.BLOCKS);
        LANTERN_ENTITY_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("lantern",
                () -> BlockEntityType.Builder.of(LanternBlockEntity::new, lantern.get()).build(null));
        DeferredBlock<BlockLanternBehemoth> behemoth = registerBlock("lantern_behemoth",
                () -> new BlockLanternBehemoth(deco15Props().lightLevel(state -> 15).noOcclusion()),
                ModCreativeTabs.BLOCKS);
        LANTERN_BEHEMOTH_ENTITY_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("lantern_behemoth",
                () -> BlockEntityType.Builder.of(LanternBehemothBlockEntity::new, behemoth.get()).build(null));
    }

    private static void registerWand() {
        // CE: setCreativeTab(null) is not set on wand_air in CE (no explicit tab call at all, which
        // in 1.12 means it silently has no tab); kept off the creative menu here for the same effect.
        registerBlock("wand_air", () -> new BlockWand(BlockBehaviour.Properties.of().strength(0.0F, 0.0F).noCollission().noOcclusion()), null);
    }

    private static BlockBehaviour.Properties deco15Props() {
        return BlockBehaviour.Properties.of().strength(DECO_HARDNESS, DECO_RESISTANCE).sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties deco10Props() {
        return BlockBehaviour.Properties.of().strength(DECO_HARDNESS, 10.0F).sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties satProps() {
        return BlockBehaviour.Properties.of().strength(DECO_HARDNESS, 10.0F).sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties metalProps(float resistance) {
        return BlockBehaviour.Properties.of().strength(DECO_HARDNESS, resistance).sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties unbreakableProps() {
        return BlockBehaviour.Properties.of().strength(-1.0F, UNBREAKABLE_RESISTANCE).sound(SoundType.METAL).noOcclusion();
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
