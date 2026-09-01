package com.hbm.capability;

import com.hbm.blockentity.machine.pile.PileBlockEntities;
import com.hbm.blockentity.machine.pile.PileLoaderBlockEntity;
import com.hbm.blockentity.machine.pile.PileVentBlockEntity;
import com.hbm.main.MainRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = MainRegistry.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class PileDeviceCapabilities {

    private PileDeviceCapabilities() {
    }

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PileBlockEntities.PILE_LOADER.get(),
                (be, side) -> be instanceof PileLoaderBlockEntity loader ? loader.itemHandler : null);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, PileBlockEntities.PILE_VENT.get(),
                (be, side) -> be instanceof PileVentBlockEntity vent ? new NTMFluidHandlerWrapper(vent, null) : null);
    }
}
