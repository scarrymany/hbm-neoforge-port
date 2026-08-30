package com.hbm.blockentity.machine;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.MachineMiniRTGBlock;
import com.hbm.blocks.machine.PowerGenBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * {@link BlockEntityType} registration for this power-generation family, sibling to
 * {@link PowerGenBlocks} (that class registers the {@code Block}s this one's
 * {@code BlockEntityType.Builder.of} calls reference) - see that class's own javadoc for why the two
 * are split across files, matching the concurrent storage-machines pass's
 * {@code StorageMachineBlocks}/{@code StorageBlockEntities} split.
 * <p>
 * Called from {@link PowerGenBlocks#registerAll()}, not registered independently - see this task's
 * wiring notes for the single call site ({@code ModBlocks.register()}) this whole family needs.
 */
public final class PowerGenBlockEntities {

    public static Supplier<BlockEntityType<MachineRTGBlockEntity>> MACHINE_RTG;
    public static Supplier<BlockEntityType<MachineMiniRTGBlockEntity>> MACHINE_MINI_RTG;
    public static Supplier<BlockEntityType<MachineSteamEngineBlockEntity>> STEAM_ENGINE;
    public static Supplier<BlockEntityType<MachineDieselBlockEntity>> MACHINE_DIESEL;
    public static Supplier<BlockEntityType<MachineCombustionEngineBlockEntity>> COMBUSTION_ENGINE;
    public static Supplier<BlockEntityType<MachineTurbineBlockEntity>> MACHINE_TURBINE;
    public static Supplier<BlockEntityType<MachineLargeTurbineBlockEntity>> LARGE_TURBINE;
    public static Supplier<BlockEntityType<MachineIndustrialTurbineBlockEntity>> INDUSTRIAL_TURBINE;
    public static Supplier<BlockEntityType<MachineTurbineGasBlockEntity>> TURBINE_GAS;
    public static Supplier<BlockEntityType<SolarBoilerBlockEntity>> SOLAR_BOILER;
    public static Supplier<BlockEntityType<SolarMirrorBlockEntity>> SOLAR_MIRROR;

    private PowerGenBlockEntities() {
    }

    public static void registerAll() {
        MACHINE_RTG = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_rtg", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineRTGBlockEntity(MACHINE_RTG.get(), pos, state),
                PowerGenBlocks.MACHINE_RTG.get()
        ).build(null));

        MACHINE_MINI_RTG = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_minirtg", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineMiniRTGBlockEntity(MACHINE_MINI_RTG.get(), pos, state, ((MachineMiniRTGBlock) state.getBlock()).isPolonium()),
                PowerGenBlocks.MACHINE_MINI_RTG.get(), PowerGenBlocks.MACHINE_POWER_RTG.get()
        ).build(null));

        STEAM_ENGINE = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_steam_engine", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineSteamEngineBlockEntity(STEAM_ENGINE.get(), pos, state),
                PowerGenBlocks.MACHINE_STEAM_ENGINE.get()
        ).build(null));

        MACHINE_DIESEL = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_diesel", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineDieselBlockEntity(MACHINE_DIESEL.get(), pos, state),
                PowerGenBlocks.MACHINE_DIESEL.get()
        ).build(null));

        COMBUSTION_ENGINE = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_combustion_engine", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineCombustionEngineBlockEntity(COMBUSTION_ENGINE.get(), pos, state),
                PowerGenBlocks.MACHINE_COMBUSTION_ENGINE.get()
        ).build(null));

        MACHINE_TURBINE = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_turbine", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineTurbineBlockEntity(MACHINE_TURBINE.get(), pos, state),
                PowerGenBlocks.MACHINE_TURBINE.get()
        ).build(null));

        LARGE_TURBINE = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_large_turbine", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineLargeTurbineBlockEntity(LARGE_TURBINE.get(), pos, state),
                PowerGenBlocks.MACHINE_LARGE_TURBINE.get()
        ).build(null));

        INDUSTRIAL_TURBINE = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_industrial_turbine", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineIndustrialTurbineBlockEntity(INDUSTRIAL_TURBINE.get(), pos, state),
                PowerGenBlocks.MACHINE_INDUSTRIAL_TURBINE.get()
        ).build(null));

        TURBINE_GAS = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_turbine_gas", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineTurbineGasBlockEntity(TURBINE_GAS.get(), pos, state),
                PowerGenBlocks.MACHINE_TURBINE_GAS.get()
        ).build(null));

        SOLAR_BOILER = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_solar_boiler", () -> BlockEntityType.Builder.of(
                (pos, state) -> new SolarBoilerBlockEntity(SOLAR_BOILER.get(), pos, state),
                PowerGenBlocks.MACHINE_SOLAR_BOILER.get()
        ).build(null));

        SOLAR_MIRROR = ModBlocks.BLOCK_ENTITY_TYPES.register("solar_mirror", () -> BlockEntityType.Builder.of(
                (pos, state) -> new SolarMirrorBlockEntity(SOLAR_MIRROR.get(), pos, state),
                PowerGenBlocks.SOLAR_MIRROR.get()
        ).build(null));
    }
}
