package com.hbm.capability;

import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.main.MainRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = MainRegistry.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class DummyableProcessCapabilities {

    private DummyableProcessCapabilities() {
    }

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.FURNACE_COMBINATION.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.FURNACE_COMBINATION.get(),
                (be, side) -> be.getFluidHandlerCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_BLAST_FURNACE.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_BLAST_FURNACE.get(),
                (be, side) -> be.getFluidHandlerCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_ROCK_MILL.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_ROCK_MILL.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DummyableProcessBlockEntities.MACHINE_ROCK_MILL.get(),
                (be, side) -> be.getEnergyStorageCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_ANNIHILATOR.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_ANNIHILATOR.get(),
                (be, side) -> be.getFluidHandlerCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_PRESS.get(),
                (be, side) -> be.getItemHandlerCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_ROTARY_FURNACE.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_ROTARY_FURNACE.get(),
                (be, side) -> be.getFluidHandlerCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_FRACTION_TOWER.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_FRACTION_TOWER.get(),
                (be, side) -> be.getFluidHandlerCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.WASTE_DRUM.get(),
                (be, side) -> be.getItemHandlerCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_COMPRESSOR.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_COMPRESSOR.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DummyableProcessBlockEntities.MACHINE_COMPRESSOR.get(),
                (be, side) -> be.getEnergyStorageCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_COKER.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_COKER.get(),
                (be, side) -> be.getFluidHandlerCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_CATALYTIC_CRACKER.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_CATALYTIC_CRACKER.get(),
                (be, side) -> be.getFluidHandlerCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_CATALYTIC_REFORMER.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_CATALYTIC_REFORMER.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DummyableProcessBlockEntities.MACHINE_CATALYTIC_REFORMER.get(),
                (be, side) -> be.getEnergyStorageCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_HYDROTREATER.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_HYDROTREATER.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DummyableProcessBlockEntities.MACHINE_HYDROTREATER.get(),
                (be, side) -> be.getEnergyStorageCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_VACUUM_DISTILL.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_VACUUM_DISTILL.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DummyableProcessBlockEntities.MACHINE_VACUUM_DISTILL.get(),
                (be, side) -> be.getEnergyStorageCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_RADIOLYSIS.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_RADIOLYSIS.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DummyableProcessBlockEntities.MACHINE_RADIOLYSIS.get(),
                (be, side) -> be.getEnergyStorageCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_FLARE.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_FLARE.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DummyableProcessBlockEntities.MACHINE_FLARE.get(),
                (be, side) -> be.getEnergyStorageCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_EPRESS.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DummyableProcessBlockEntities.MACHINE_EPRESS.get(),
                (be, side) -> be.getEnergyStorageCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_PYROOVEN.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_PYROOVEN.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DummyableProcessBlockEntities.MACHINE_PYROOVEN.get(),
                (be, side) -> be.getEnergyStorageCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_ARC_FURNACE.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DummyableProcessBlockEntities.MACHINE_ARC_FURNACE.get(),
                (be, side) -> be.getEnergyStorageCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_EXPOSURE_CHAMBER.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DummyableProcessBlockEntities.MACHINE_EXPOSURE_CHAMBER.get(),
                (be, side) -> be.getEnergyStorageCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_ORE_SLOPPER.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_ORE_SLOPPER.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DummyableProcessBlockEntities.MACHINE_ORE_SLOPPER.get(),
                (be, side) -> be.getEnergyStorageCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_TURBOFAN.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_TURBOFAN.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DummyableProcessBlockEntities.MACHINE_TURBOFAN.get(),
                (be, side) -> be.getEnergyStorageCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_RADGEN.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DummyableProcessBlockEntities.MACHINE_RADGEN.get(),
                (be, side) -> be.getEnergyStorageCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_HEPHAESTUS.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_HEPHAESTUS.get(),
                (be, side) -> be.getFluidHandlerCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_WOOD_BURNER.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_WOOD_BURNER.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DummyableProcessBlockEntities.MACHINE_WOOD_BURNER.get(),
                (be, side) -> be.getEnergyStorageCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.FURNACE_IRON.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.FURNACE_STEEL.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.HEATER_FIREBOX.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.HEATER_OVEN.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.HEATER_OILBURNER.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.HEATER_OILBURNER.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_SAWMILL.get(),
                (be, side) -> be.getItemHandlerCapability(side));

        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DummyableProcessBlockEntities.HEATER_ELECTRIC.get(),
                (be, side) -> be.getEnergyStorageCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.HEATER_HEATEX.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.HEATER_HEATEX.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DummyableProcessBlockEntities.MACHINE_STIRLING.get(),
                (be, side) -> be.getEnergyStorageCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_STORAGE_DRUM.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_STORAGE_DRUM.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_SUPERCOMPUTER.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_SUPERCOMPUTER.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DummyableProcessBlockEntities.MACHINE_SUPERCOMPUTER.get(),
                (be, side) -> be.getEnergyStorageCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_AUTOSAW.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_AUTOSAW.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_ASHPIT.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.HEAT_BOILER.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.HEAT_BOILER.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_INDUSTRIAL_BOILER.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_INDUSTRIAL_BOILER.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_TOWER_SMALL.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_TOWER_SMALL.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_TOWER_LARGE.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_TOWER_LARGE.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.RADIO_TELEX.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.RADAR_SCREEN.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_SIREN.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_CONDENSER.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_CONDENSER.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_CONDENSER_POWERED.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_CONDENSER_POWERED.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DummyableProcessBlockEntities.MACHINE_CONDENSER_POWERED.get(),
                (be, side) -> be.getEnergyStorageCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_INTAKE.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DummyableProcessBlockEntities.MACHINE_INTAKE.get(),
                (be, side) -> be.getEnergyStorageCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_DRAIN.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_DRAIN.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_BAT9000.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_BAT9000.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_DEUTERIUM_EXTRACTOR.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DummyableProcessBlockEntities.MACHINE_DEUTERIUM_EXTRACTOR.get(),
                (be, side) -> be.getEnergyStorageCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_DEUTERIUM_TOWER.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DummyableProcessBlockEntities.MACHINE_DEUTERIUM_TOWER.get(),
                (be, side) -> be.getEnergyStorageCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_UF6_TANK.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_PUF6_TANK.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_FUNNEL.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_MICROWAVE.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DummyableProcessBlockEntities.MACHINE_MICROWAVE.get(),
                (be, side) -> be.getEnergyStorageCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_ELECTRIC_FURNACE.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DummyableProcessBlockEntities.MACHINE_ELECTRIC_FURNACE.get(),
                (be, side) -> be.getEnergyStorageCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DummyableProcessBlockEntities.MACHINE_DETECTOR.get(),
                (be, side) -> be.getEnergyStorageCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_ORBUS.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_ORBUS.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_BRICK_FURNACE.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_RTG_FURNACE.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.FLUID_BARREL.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DummyableProcessBlockEntities.FLUID_BARREL.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_AUTOCRAFTER.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DummyableProcessBlockEntities.MACHINE_AUTOCRAFTER.get(),
                (be, side) -> be.getEnergyStorageCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_KEYFORGE.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_DIFURNACE.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_DIFURNACE_RTG.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_CONVEYOR_PRESS.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DummyableProcessBlockEntities.MACHINE_CONVEYOR_PRESS.get(),
                (be, side) -> be.getEnergyStorageCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MASS_STORAGE.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.MACHINE_TELELINKER.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.SOYUZ_CAPSULE.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DummyableProcessBlockEntities.FILING_CABINET.get(),
                (be, side) -> be.getItemHandlerCapability(side));
    }
}
