package com.hbm.blocks.machine;

import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.inventory.container.machine.PWRMenus;
import com.hbm.inventory.recipes.machine.BreederRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.PWRHotFuelItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/**
 * Block + {@code BlockItem} registration for Phase 2's PWR (pressurized water reactor) and breeding-
 * reactor family - see {@code docs/phase2/reactors_breeding_pwr.md}. Mirrors {@code PowerGenBlocks}'
 * shape (table-driven {@code registerAll()}/{@code registerBlock} helper, block-entity registration
 * in the sibling {@link com.hbm.blockentity.machine.PWRBlockEntities}, {@code MenuType}s triggered
 * from here too) - see that class's own javadoc for the full rationale, identical here: wiring this
 * family into the game needs exactly one call from {@code ModBlocks.register()} (see this task's
 * wiring notes), no other shared file needs a direct edit.
 *
 * <h2>Coordination note on {@code BlockGenericPWR} - read before touching this class</h2>
 * This package's controller/proxy multiblock needs to recognize six Phase-1-ported
 * {@code com.hbm.blocks.generic.BlockGenericPWR} instances ({@code pwr_heatex}/{@code pwr_heatsink}/
 * {@code pwr_neutron_source}/{@code pwr_reflector}/{@code pwr_casing}/{@code pwr_port}) by identity.
 * Per this task's own instruction, that already-committed plain-block file is <b>not</b> edited or
 * given a tile entity here - a TE-backed variant would conflict with Phase 1's shipped content for no
 * reason, since {@code MachinePWRControllerBlock}'s flood-fill and
 * {@link com.hbm.blockentity.machine.PWRControllerBlockEntity#setup} only ever need {@code Block}
 * identity, never TE state, exactly like CE's own {@code MachinePWRController.isValidCasing}/
 * {@code isValidCore}. {@link PWRPhase1Blocks} resolves those six by registry id instead (see that
 * class's own javadoc) - this package's own new content is only the seven registry entries below
 * ({@code pwr_fuelrod}/{@code pwr_control}/{@code pwr_channel}/{@code pwr_controller}/
 * {@code pwr_block}/{@code corium_block}/{@code machine_reactor_breeding}).
 *
 * <h2>{@code corium_block} is a plain solid block, not CE's finite-spreading fluid</h2>
 * CE's {@code ModBlocks.corium_block} is actually {@code CoriumFinite extends BlockFluidClassic} - a
 * spreading fluid block backed by {@code ModFluids.corium_fluid}. This port has no world-fluid-block
 * system at all yet (Phase 1's own finding, restated in {@code docs/phase2/blockentity_base.md}), so
 * {@link #CORIUM_BLOCK} is a plain, non-spreading solid block instead - {@code meltDown()} still
 * replaces every fuel-rod position and detonates (see
 * {@link com.hbm.blockentity.machine.PWRControllerBlockEntity#meltDown}), it just doesn't ooze
 * afterward. A documented scope-cut, not a silent behavior change; CE's own exact hardness for this
 * block was not specified in the read source (only {@code .setResistance(500F)} was) - a high but
 * otherwise unremarkable strength value is used here instead of guessing CE's real number.
 */
public final class PWRBlocks {

    private static final BlockBehaviour.Properties MACHINE_PROPS =
            BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    private static final BlockBehaviour.Properties PROXY_PROPS =
            BlockBehaviour.Properties.of().strength(15.0F, 10.0F).sound(SoundType.METAL).noLootTable();
    private static final BlockBehaviour.Properties CORIUM_PROPS =
            BlockBehaviour.Properties.of().strength(50.0F, 500.0F).sound(SoundType.STONE).noLootTable();

    public static DeferredBlock<BlockPillarPWR> PWR_FUELROD;
    public static DeferredBlock<BlockPillarPWR> PWR_CONTROL;
    public static DeferredBlock<BlockPillarPWR> PWR_CHANNEL;
    public static DeferredBlock<MachinePWRControllerBlock> PWR_CONTROLLER;
    public static DeferredBlock<PWRProxyBlock> PWR_PROXY;
    public static DeferredBlock<Block> CORIUM_BLOCK;
    public static DeferredBlock<MachineReactorBreedingBlock> REACTOR_BREEDING;

    private PWRBlocks() {
    }

    public static void registerAll() {
        PWR_FUELROD = registerBlock("pwr_fuelrod", () -> new BlockPillarPWR(MACHINE_PROPS));
        PWR_CONTROL = registerBlock("pwr_control", () -> new BlockPillarPWR(MACHINE_PROPS));
        PWR_CHANNEL = registerBlock("pwr_channel", () -> new BlockPillarPWR(MACHINE_PROPS));
        PWR_CONTROLLER = registerBlock("pwr_controller", () -> new MachinePWRControllerBlock(MACHINE_PROPS));
        REACTOR_BREEDING = registerBlock("machine_reactor_breeding", () -> new MachineReactorBreedingBlock(MACHINE_PROPS));

        // Never obtainable/placeable directly - no BlockItem registered, see each field's own javadoc
        // reference above ("Never obtainable" on PWRProxyBlock; corium_block is meltdown-only debris).
        PWR_PROXY = ModBlocks.BLOCKS.register("pwr_block", () -> new PWRProxyBlock(PROXY_PROPS));
        CORIUM_BLOCK = ModBlocks.BLOCKS.register("corium_block", () -> new Block(CORIUM_PROPS));

        com.hbm.blockentity.machine.PWRBlockEntities.registerAll();
        PWRMenus.registerAll();
        PWRHotFuelItems.registerAll();
        com.hbm.items.machine.PWRDepletedFuelItems.registerAll();
        BreederRecipes.bootstrap();
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, block);
        return block;
    }
}
