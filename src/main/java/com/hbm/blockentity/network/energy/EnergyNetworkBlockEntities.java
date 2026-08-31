package com.hbm.blockentity.network.energy;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.network.energy.EnergyNetworkBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * {@link BlockEntityType} registration for the Phase 2 energy cable/pylon network family
 * ({@code docs/phase2/energy_cable_pylon_network.md}), sibling to
 * {@link EnergyNetworkBlocks} exactly like {@code PowerGenBlockEntities}/{@code PowerGenBlocks} -
 * see that pair's own javadoc for why the split exists. Called from
 * {@link EnergyNetworkBlocks#registerAll()}, not registered independently.
 */
public final class EnergyNetworkBlockEntities {

    public static Supplier<BlockEntityType<CableBaseBlockEntity>> CABLE;
    public static Supplier<BlockEntityType<CableSwitchBlockEntity>> CABLE_SWITCH;
    public static Supplier<BlockEntityType<CableDiodeBlockEntity>> CABLE_DIODE;
    public static Supplier<BlockEntityType<PylonBlockEntity>> PYLON;
    public static Supplier<BlockEntityType<PylonLargeBlockEntity>> PYLON_LARGE;
    public static Supplier<BlockEntityType<PylonMediumBlockEntity>> PYLON_MEDIUM;
    public static Supplier<BlockEntityType<SubstationBlockEntity>> SUBSTATION;
    public static Supplier<BlockEntityType<ProxyConductorBlockEntity>> PROXY_CONDUCTOR;

    private EnergyNetworkBlockEntities() {
    }

    public static void registerAll() {
        CABLE = ModBlocks.BLOCK_ENTITY_TYPES.register("red_cable", () -> BlockEntityType.Builder.of(
                (pos, state) -> new CableBaseBlockEntity(CABLE.get(), pos, state),
                EnergyNetworkBlocks.RED_CABLE.get(), EnergyNetworkBlocks.RED_CABLE_CLASSIC.get(),
                EnergyNetworkBlocks.RED_CABLE_BOX.get(), EnergyNetworkBlocks.RED_WIRE_COATED.get()
        ).build(null));

        CABLE_SWITCH = ModBlocks.BLOCK_ENTITY_TYPES.register("cable_switch", () -> BlockEntityType.Builder.of(
                (pos, state) -> new CableSwitchBlockEntity(CABLE_SWITCH.get(), pos, state),
                EnergyNetworkBlocks.CABLE_SWITCH.get(), EnergyNetworkBlocks.CABLE_DETECTOR.get()
        ).build(null));

        CABLE_DIODE = ModBlocks.BLOCK_ENTITY_TYPES.register("cable_diode", () -> BlockEntityType.Builder.of(
                (pos, state) -> new CableDiodeBlockEntity(CABLE_DIODE.get(), pos, state),
                EnergyNetworkBlocks.CABLE_DIODE.get()
        ).build(null));

        PYLON = ModBlocks.BLOCK_ENTITY_TYPES.register("red_pylon", () -> BlockEntityType.Builder.of(
                (pos, state) -> new PylonBlockEntity(PYLON.get(), pos, state),
                EnergyNetworkBlocks.RED_PYLON.get(), EnergyNetworkBlocks.RED_PYLON_STEEL_SMALL.get()
        ).build(null));

        PYLON_LARGE = ModBlocks.BLOCK_ENTITY_TYPES.register("red_pylon_large", () -> BlockEntityType.Builder.of(
                (pos, state) -> new PylonLargeBlockEntity(PYLON_LARGE.get(), pos, state),
                EnergyNetworkBlocks.RED_PYLON_LARGE.get()
        ).build(null));

        PYLON_MEDIUM = ModBlocks.BLOCK_ENTITY_TYPES.register("red_pylon_medium", () -> BlockEntityType.Builder.of(
                (pos, state) -> new PylonMediumBlockEntity(PYLON_MEDIUM.get(), pos, state),
                EnergyNetworkBlocks.RED_PYLON_MEDIUM_WOOD.get(), EnergyNetworkBlocks.RED_PYLON_MEDIUM_STEEL.get()
        ).build(null));

        SUBSTATION = ModBlocks.BLOCK_ENTITY_TYPES.register("substation", () -> BlockEntityType.Builder.of(
                (pos, state) -> new SubstationBlockEntity(SUBSTATION.get(), pos, state),
                EnergyNetworkBlocks.SUBSTATION.get()
        ).build(null));

        PROXY_CONDUCTOR = ModBlocks.BLOCK_ENTITY_TYPES.register("substation_proxy_conductor", () -> BlockEntityType.Builder.of(
                (pos, state) -> new ProxyConductorBlockEntity(PROXY_CONDUCTOR.get(), pos, state),
                EnergyNetworkBlocks.SUBSTATION.get()
        ).build(null));
    }
}
