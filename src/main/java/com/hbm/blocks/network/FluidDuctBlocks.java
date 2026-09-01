package com.hbm.blocks.network;

import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Function;

/**
 * Table-driven registration for the {@code FluidDuctBase} family (10 concrete blocks - see
 * {@code docs/phase2/network_fluid_ducts.md}'s registry table), mirroring
 * {@code com.hbm.blocks.machine.StorageMachineBlocks}'s own {@code registerAll()}/{@code registerBlock}
 * shape. Block-entity-type registration lives in the sibling
 * {@link com.hbm.blockentity.network.FluidDuctBlockEntities}.
 *
 * <p>All ten sit on {@link ModCreativeTabs#CONTROL}, matching CE's own {@code MainRegistry.controlTab}
 * placement for the subset of these blocks whose constructor explicitly set a creative tab
 * ({@code FluidDuctStandard}/{@code FluidDuctBox}/{@code FluidPipeAnchor}) - the remaining CE classes
 * read for this pass ({@code FluidCounterValve}/{@code FluidValve}/{@code FluidSwitch}/
 * {@code FluidDuctPaintable}/{@code FluidDuctPaintableBlockExhaust}/{@code FluidDuctGauge}) did not
 * call {@code setCreativeTab} in their own constructors; placing the whole family on one shared tab
 * for discoverability is a reasonable, non-gameplay-affecting choice rather than leaving the rest
 * unreachable in creative.
 *
 * <p>Hardness/resistance (5.0F/10.0F) is a reasonable placeholder for a thin metal pipe - CE's own
 * values for this specific family were not visible in the constructors read for this pass (no
 * {@code setHardness}/{@code setResistance} call appears in any of the ten), so this port picks one
 * uniform value rather than guessing per-block, matching the sibling energy-network family's own
 * choice for the same reason ({@code EnergyNetworkBlocks}, same wave, same
 * {@code com.hbm.blocks.network} package) rather than inventing a second convention.
 */
public final class FluidDuctBlocks {

    private static final float HARDNESS = 5.0F;
    private static final float RESISTANCE = 10.0F;

    public static DeferredBlock<FluidDuctStandardBlock> DUCT_STANDARD;
    public static DeferredBlock<FluidDuctBoxBlock> DUCT_BOX;
    public static DeferredBlock<FluidDuctBoxExhaustBlock> DUCT_BOX_EXHAUST;
    public static DeferredBlock<FluidDuctBoxExhaustBlock> DUCT_EXHAUST;
    public static DeferredBlock<FluidCounterValveBlock> COUNTER_VALVE;
    public static DeferredBlock<FluidValveBlock> VALVE;
    public static DeferredBlock<FluidSwitchBlock> SWITCH;
    public static DeferredBlock<FluidDuctPaintableBlock> DUCT_PAINTABLE;
    public static DeferredBlock<FluidDuctPaintableExhaustBlock> DUCT_PAINTABLE_EXHAUST;
    public static DeferredBlock<FluidDuctGaugeBlock> DUCT_GAUGE;
    public static DeferredBlock<FluidPipeAnchorBlock> PIPE_ANCHOR;

    private FluidDuctBlocks() {
    }

    public static void registerAll() {
        DUCT_STANDARD = registerBlock("fluid_duct_standard", FluidDuctStandardBlock::new);
        DUCT_BOX = registerBlock("fluid_duct_box", FluidDuctBoxBlock::new);
        DUCT_BOX_EXHAUST = registerBlock("fluid_duct_box_exhaust", FluidDuctBoxExhaustBlock::new);
        // CE id fluid_duct_exhaust — same live exhaust duct as fluid_duct_box_exhaust (prior drift).
        DUCT_EXHAUST = registerBlock("fluid_duct_exhaust", FluidDuctBoxExhaustBlock::new);
        COUNTER_VALVE = registerBlock("fluid_counter_valve", FluidCounterValveBlock::new);
        VALVE = registerBlock("fluid_valve", FluidValveBlock::new);
        SWITCH = registerBlock("fluid_switch", FluidSwitchBlock::new);
        DUCT_PAINTABLE = registerBlock("fluid_duct_paintable", FluidDuctPaintableBlock::new);
        DUCT_PAINTABLE_EXHAUST = registerBlock("fluid_duct_paintable_exhaust", FluidDuctPaintableExhaustBlock::new);
        DUCT_GAUGE = registerBlock("fluid_duct_gauge", FluidDuctGaugeBlock::new);
        PIPE_ANCHOR = registerBlock("fluid_pipe_anchor", FluidPipeAnchorBlock::new);
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Function<BlockBehaviour.Properties, T> factory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name,
                () -> factory.apply(BlockBehaviour.Properties.of().strength(HARDNESS, RESISTANCE).sound(SoundType.METAL).noOcclusion()));
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.CONTROL, block);
        return block;
    }
}
