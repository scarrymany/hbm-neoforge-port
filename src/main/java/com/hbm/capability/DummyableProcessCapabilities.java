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
    }
}
