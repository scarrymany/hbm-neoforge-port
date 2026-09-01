package com.hbm.inventory.gui.machine.accel;

import com.hbm.inventory.container.machine.accel.AccelMenus;
import com.hbm.inventory.gui.SafeMenuScreens;
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
        SafeMenuScreens.bind(event, AccelMenus.MACHINE_FEL, FelScreen::new);
        SafeMenuScreens.bind(event, AccelMenus.MACHINE_EXCAVATOR, ExcavatorScreen::new);
        SafeMenuScreens.bind(event, AccelMenus.PA_PART, PaPartScreen::new);
        SafeMenuScreens.bind(event, AccelMenus.PA_DETECTOR, PaDetectorScreen::new);
    }
}
