package com.hbm.blocks.machine;

import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.inventory.container.machine.SensorMenus;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/**
 * Radar family. CE {@code ModBlocks.java:1193-1194} {@code machine_radar}/{@code machine_radar_large}.
 * TE/logic: {@code TileEntityMachineRadarNT.java} (range/power/consumption/ping) +
 * {@code TileEntityMachineRadarLarge.java:16} (range 3000).
 */
public final class SensorBlocks {

    private static final BlockBehaviour.Properties MACHINE_PROPS =
            BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();

    public static DeferredBlock<MachineRadarBlock> MACHINE_RADAR;
    public static DeferredBlock<MachineRadarBlock> MACHINE_RADAR_LARGE;

    private SensorBlocks() {
    }

    public static void registerAll() {
        MACHINE_RADAR = registerBlock("machine_radar", () -> new MachineRadarBlock(MACHINE_PROPS, false));
        MACHINE_RADAR_LARGE = registerBlock("machine_radar_large", () -> new MachineRadarBlock(MACHINE_PROPS, true));
        com.hbm.blockentity.machine.SensorBlockEntities.registerAll();
        SensorMenus.registerAll();
    }

    private static <T extends net.minecraft.world.level.block.Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, block);
        return block;
    }
}
