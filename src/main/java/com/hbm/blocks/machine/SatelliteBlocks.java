package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.TapeDriveBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Satellite system blocks and entities registration.
 * Ports CE's machine_tape_drive (satellite data recorder).
 */
public final class SatelliteBlocks {

    private static final BlockBehaviour.Properties MACHINE_PROPS =
            BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL);

    private SatelliteBlocks() {
    }

    public static void registerAll() {
        // CE ModBlocks.java:1239: machine_tape_drive
        ModBlocks.MACHINE_TAPE_DRIVE = ModBlocks.BLOCKS.register("machine_tape_drive",
                () -> new MachineTapeDrive(MACHINE_PROPS));
        ModItems.ITEMS.register("machine_tape_drive", () -> new BlockItem(ModBlocks.MACHINE_TAPE_DRIVE.get(),
                new Item.Properties().stacksTo(64)));

        ModBlocks.TAPE_DRIVE_ENTITY = ModBlocks.BLOCK_ENTITY_TYPES.register("tape_drive", () ->
                BlockEntityType.Builder.of(TapeDriveBlockEntity::new, ModBlocks.MACHINE_TAPE_DRIVE.get()).build(null));

        CreativeTabContents.add(ModCreativeTabs.MACHINE, () -> ModBlocks.MACHINE_TAPE_DRIVE.get());
    }
}
