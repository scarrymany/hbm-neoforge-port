package com.hbm.capability;

import com.hbm.blockentity.machine.accel.AccelBlockEntities;
import com.hbm.main.MainRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = MainRegistry.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class AccelCapabilities {

    private AccelCapabilities() {
    }

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, AccelBlockEntities.MACHINE_EXCAVATOR.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, AccelBlockEntities.MACHINE_EXCAVATOR.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, AccelBlockEntities.MACHINE_EXCAVATOR.get(),
                (be, side) -> be.getEnergyStorageCapability(side));
    }
}
