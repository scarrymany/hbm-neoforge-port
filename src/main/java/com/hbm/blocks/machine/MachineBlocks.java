package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.CargoElevatorBlockEntity;
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
 * Machine blocks and entities registration (cargo_elevator etc.).
 * Ports CE's cargo_elevator (CE ModBlocks :1440).
 */
public final class MachineBlocks {

    private static final BlockBehaviour.Properties MACHINE_PROPS =
            BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL);

    private MachineBlocks() {
    }

    public static void registerAll() {
        // CE ModBlocks :1440: cargo_elevator
        ModBlocks.CARGO_ELEVATOR = ModBlocks.BLOCKS.register("cargo_elevator",
                () -> new BlockCargoElevator(MACHINE_PROPS));
        ModItems.ITEMS.register("cargo_elevator", () -> new BlockItem(ModBlocks.CARGO_ELEVATOR.get(),
                new Item.Properties().stacksTo(64)));

        ModBlocks.CARGO_ELEVATOR_ENTITY = ModBlocks.BLOCK_ENTITY_TYPES.register("cargo_elevator", () ->
                BlockEntityType.Builder.of(CargoElevatorBlockEntity::new, ModBlocks.CARGO_ELEVATOR.get()).build(null));

        CreativeTabContents.add(ModCreativeTabs.MACHINE, () -> ModBlocks.CARGO_ELEVATOR.get());
    }
}
