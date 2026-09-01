package com.hbm.blocks.network.energy;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.network.PylonMediumBlock;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/**
 * Block + {@code BlockItem} registration for Phase 2's energy cable/pylon network family - see
 * {@code docs/phase2/energy_cable_pylon_network.md}. Mirrors {@code PowerGenBlocks}' established
 * shape (table-driven {@link #registerAll()}/{@link #registerBlock} helper).
 * <p>
 * Block-entity-type registration lives in the sibling
 * {@link com.hbm.blockentity.network.energy.EnergyNetworkBlockEntities} class (its
 * {@code BlockEntityType.Builder.of} calls read the {@link DeferredBlock} fields below); wiring this
 * family into the game needs exactly one call from {@code ModBlocks.register()} (see this task's
 * wiring notes) - no other shared file needs a direct edit.
 * <p>
 * <b>Not ported</b> (see the research report's own "Phase-2-safe scope"/"Deferred scope" tables):
 * {@code WireCoatedRadResistant} (radiation-package marker), {@code BlockCableGauge} (OpenComputers integration explicitly recommended
 * for dropping, no NeoForge OpenComputers release exists to compile against anyway),
 * {@code BlockCablePaintable} (paint/facade mechanic explicitly flagged as needing a cross-team call
 * with whichever area owns {@code com.hbm.items.util} paint tools), {@code PowerDetector} (a
 * redstone-lamp-shaped energy receiver, orthogonal to the conductor/pylon network graph this pass
 * targets), and the {@code PylonMedium} transformer sub-variants (see
 * {@link com.hbm.blockentity.network.energy.PylonMediumBlockEntity}'s javadoc for why the
 * transformer-side connector branch itself is dropped, not just the item variants).
 */
public final class EnergyNetworkBlocks {

    private static final BlockBehaviour.Properties CABLE_PROPS =
            BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion();
    private static final BlockBehaviour.Properties PYLON_PROPS =
            BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion().noCollission();

    public static DeferredBlock<WireCoated> RED_WIRE_COATED;
    public static DeferredBlock<BlockCable> RED_CABLE;
    public static DeferredBlock<BlockCableClassic> RED_CABLE_CLASSIC;
    public static DeferredBlock<PowerCableBoxBlock> RED_CABLE_BOX;
    public static DeferredBlock<CableSwitchBlock> CABLE_SWITCH;
    public static DeferredBlock<CableDetectorBlock> CABLE_DETECTOR;
    public static DeferredBlock<CableDiodeBlock> CABLE_DIODE;
    public static DeferredBlock<PylonRedWireBlock> RED_PYLON;
    public static DeferredBlock<PylonRedWireBlock> RED_PYLON_STEEL_SMALL;
    public static DeferredBlock<PylonLargeBlock> RED_PYLON_LARGE;
    public static DeferredBlock<PylonMediumBlock> RED_PYLON_MEDIUM_WOOD;
    public static DeferredBlock<PylonMediumBlock> RED_PYLON_MEDIUM_STEEL;
    public static DeferredBlock<PylonMediumBlock> RED_PYLON_MEDIUM_TRANSFORMER;
    public static DeferredBlock<PylonMediumBlock> RED_PYLON_STEEL_TRANSFORMER;
    public static DeferredBlock<SubstationBlock> SUBSTATION;

    private EnergyNetworkBlocks() {
    }

    public static void registerAll() {
        // CE ModBlocks.java:765 — WireCoated, 5.0F/10.0F, machineTab. Full cube, not thin cable.
        RED_WIRE_COATED = registerBlock("red_wire_coated",
                () -> new WireCoated(BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL)));
        RED_CABLE = registerBlock("red_cable", () -> new BlockCable(CABLE_PROPS));
        RED_CABLE_CLASSIC = registerBlock("red_cable_classic", () -> new BlockCableClassic(CABLE_PROPS));
        RED_CABLE_BOX = registerBlock("red_cable_box", () -> new PowerCableBoxBlock(CABLE_PROPS));
        CABLE_SWITCH = registerBlock("cable_switch", () -> new CableSwitchBlock(CABLE_PROPS));
        CABLE_DETECTOR = registerBlock("cable_detector", () -> new CableDetectorBlock(CABLE_PROPS));
        CABLE_DIODE = registerBlock("cable_diode", () -> new CableDiodeBlock(CABLE_PROPS));
        RED_PYLON = registerBlock("red_pylon", () -> new PylonRedWireBlock(PYLON_PROPS));
        RED_PYLON_STEEL_SMALL = registerBlock("red_pylon_steel_small", () -> new PylonRedWireBlock(PYLON_PROPS));
        RED_PYLON_LARGE = registerBlock("red_pylon_large", () -> new PylonLargeBlock(PYLON_PROPS));
        RED_PYLON_MEDIUM_WOOD = registerBlock("red_pylon_medium_wood", () -> new PylonMediumBlock(PYLON_PROPS));
        RED_PYLON_MEDIUM_STEEL = registerBlock("red_pylon_medium_steel", () -> new PylonMediumBlock(PYLON_PROPS));
        // CE ModBlocks.java:778 / :780 — same PylonMedium class, CE registry ids (not field names).
        RED_PYLON_MEDIUM_TRANSFORMER = registerBlock("red_pylon_medium_transformer", () -> new PylonMediumBlock(PYLON_PROPS));
        RED_PYLON_STEEL_TRANSFORMER = registerBlock("red_pylon_steel_transformer", () -> new PylonMediumBlock(PYLON_PROPS));
        SUBSTATION = registerBlock("substation", () -> new SubstationBlock(CABLE_PROPS));

        com.hbm.blockentity.network.energy.EnergyNetworkBlockEntities.registerAll();
    }

    private static <T extends net.minecraft.world.level.block.Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, block);
        return block;
    }
}
