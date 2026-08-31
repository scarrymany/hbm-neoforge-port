package com.hbm.inventory.gui.machine.accel;

import com.hbm.inventory.container.machine.accel.AccelMenus;
import com.hbm.main.MainRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class AccelClientRegistry {

    private AccelClientRegistry() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(AccelMenus.MACHINE_FEL.get(), FelScreen::new);
        event.register(AccelMenus.MACHINE_EXCAVATOR.get(), ExcavatorScreen::new);
        event.register(AccelMenus.PA_PART.get(), PaPartScreen::new);
        event.register(AccelMenus.PA_DETECTOR.get(), PaDetectorScreen::new);
    }
}
