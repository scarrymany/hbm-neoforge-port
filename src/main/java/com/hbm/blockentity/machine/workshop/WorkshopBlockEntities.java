package com.hbm.blockentity.machine.workshop;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.workshop.WorkshopBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public final class WorkshopBlockEntities {

    public static Supplier<BlockEntityType<AmmoPressBlockEntity>> MACHINE_AMMO_PRESS;
    public static Supplier<BlockEntityType<ArcWelderBlockEntity>> MACHINE_ARC_WELDER;
    public static Supplier<BlockEntityType<SolderingBlockEntity>> MACHINE_SOLDERING_STATION;

    private WorkshopBlockEntities() {
    }

    public static void registerAll() {
        MACHINE_AMMO_PRESS = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_ammo_press", () -> BlockEntityType.Builder.of(
                (pos, state) -> new AmmoPressBlockEntity(MACHINE_AMMO_PRESS.get(), pos, state),
                WorkshopBlocks.MACHINE_AMMO_PRESS.get()
        ).build(null));
        MACHINE_ARC_WELDER = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_arc_welder", () -> BlockEntityType.Builder.of(
                (pos, state) -> new ArcWelderBlockEntity(MACHINE_ARC_WELDER.get(), pos, state),
                WorkshopBlocks.MACHINE_ARC_WELDER.get()
        ).build(null));
        MACHINE_SOLDERING_STATION = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_soldering_station", () -> BlockEntityType.Builder.of(
                (pos, state) -> new SolderingBlockEntity(MACHINE_SOLDERING_STATION.get(), pos, state),
                WorkshopBlocks.MACHINE_SOLDERING_STATION.get()
        ).build(null));
    }
}
