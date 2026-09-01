package com.hbm.capability;

import com.hbm.blockentity.machine.fusion.FusionBlockEntities;
import com.hbm.main.MainRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = MainRegistry.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class FusionCapabilities {

    private FusionCapabilities() {
    }

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FusionBlockEntities.FUSION_TORUS.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FusionBlockEntities.FUSION_TORUS.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, FusionBlockEntities.FUSION_TORUS.get(),
                (be, side) -> be.getEnergyStorageCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FusionBlockEntities.FUSION_KLYSTRON.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FusionBlockEntities.FUSION_KLYSTRON.get(),
                (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, FusionBlockEntities.FUSION_KLYSTRON.get(),
                (be, side) -> be.getEnergyStorageCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FusionBlockEntities.FUSION_BREEDER.get(),
                (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FusionBlockEntities.FUSION_BREEDER.get(),
                (be, side) -> be.getFluidHandlerCapability(side));

        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FusionBlockEntities.FUSION_BOILER.get(),
                (be, side) -> new NTMFluidHandlerWrapper(be, null));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FusionBlockEntities.FUSION_MHDT.get(),
                (be, side) -> new NTMFluidHandlerWrapper(be, null));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, FusionBlockEntities.FUSION_MHDT.get(),
                (be, side) -> new NTMEnergyCapabilityWrapper(be, null));
    }
}
