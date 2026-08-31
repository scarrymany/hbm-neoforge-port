package com.hbm.blockentity.machine;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.SensorBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public final class SensorBlockEntities {

    public static Supplier<BlockEntityType<MachineRadarBlockEntity>> MACHINE_RADAR;
    public static Supplier<BlockEntityType<MachineRadarBlockEntity>> MACHINE_RADAR_LARGE;

    private SensorBlockEntities() {
    }

    public static void registerAll() {
        MACHINE_RADAR = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_radar", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineRadarBlockEntity(MACHINE_RADAR.get(), pos, state, false),
                SensorBlocks.MACHINE_RADAR.get()
        ).build(null));
        MACHINE_RADAR_LARGE = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_radar_large", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineRadarBlockEntity(MACHINE_RADAR_LARGE.get(), pos, state, true),
                SensorBlocks.MACHINE_RADAR_LARGE.get()
        ).build(null));
    }
}
