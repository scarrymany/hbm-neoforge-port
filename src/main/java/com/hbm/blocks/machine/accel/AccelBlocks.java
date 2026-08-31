package com.hbm.blocks.machine.accel;

import com.hbm.blockentity.machine.accel.AccelBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.inventory.container.machine.accel.AccelMenus;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/**
 * FEL + excavator + Albion PA parts. CE {@code machine_fel}, {@code machine_excavator},
 * {@code pa_beamline}/{@code pa_rfc}/{@code pa_quadrupole}/{@code pa_dipole}/{@code pa_source}/{@code pa_detector}.
 * Real TE + menu where CE has a GUI; beamline is a connector (no GUI in CE).
 */
public final class AccelBlocks {

    private static final BlockBehaviour.Properties MACHINE_PROPS =
            BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();

    public static DeferredBlock<FelBlock> MACHINE_FEL;
    public static DeferredBlock<ExcavatorBlock> MACHINE_EXCAVATOR;
    public static DeferredBlock<PaPartBlock> PA_BEAMLINE;
    public static DeferredBlock<PaPartBlock> PA_RFC;
    public static DeferredBlock<PaPartBlock> PA_QUADRUPOLE;
    public static DeferredBlock<PaPartBlock> PA_DIPOLE;
    public static DeferredBlock<PaPartBlock> PA_SOURCE;
    public static DeferredBlock<PaDetectorBlock> PA_DETECTOR;

    private AccelBlocks() {
    }

    public static void registerAll() {
        MACHINE_FEL = registerBlock("machine_fel", () -> new FelBlock(MACHINE_PROPS));
        MACHINE_EXCAVATOR = registerBlock("machine_excavator", () -> new ExcavatorBlock(MACHINE_PROPS));
        PA_BEAMLINE = registerBlock("pa_beamline", () -> new PaPartBlock(MACHINE_PROPS, PaPartBlock.Kind.BEAMLINE));
        PA_RFC = registerBlock("pa_rfc", () -> new PaPartBlock(MACHINE_PROPS, PaPartBlock.Kind.RFC));
        PA_QUADRUPOLE = registerBlock("pa_quadrupole", () -> new PaPartBlock(MACHINE_PROPS, PaPartBlock.Kind.QUADRUPOLE));
        PA_DIPOLE = registerBlock("pa_dipole", () -> new PaPartBlock(MACHINE_PROPS, PaPartBlock.Kind.DIPOLE));
        PA_SOURCE = registerBlock("pa_source", () -> new PaPartBlock(MACHINE_PROPS, PaPartBlock.Kind.SOURCE));
        PA_DETECTOR = registerBlock("pa_detector", () -> new PaDetectorBlock(MACHINE_PROPS));
        AccelBlockEntities.registerAll();
        AccelMenus.registerAll();
    }

    private static <T extends net.minecraft.world.level.block.Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, block);
        return block;
    }
}
