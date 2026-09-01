package com.hbm.inventory.gui.machine.reprocess;

import com.hbm.inventory.container.machine.reprocess.ReprocessMenus;
import com.hbm.inventory.gui.SafeMenuScreens;
import com.hbm.main.MainRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ReprocessClientRegistry {

    private ReprocessClientRegistry() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        SafeMenuScreens.bind(event, ReprocessMenus.MACHINE_PUREX, PurexScreen::new);
        SafeMenuScreens.bind(event, ReprocessMenus.MACHINE_LIQUEFACTOR, LiquefactorScreen::new);
        SafeMenuScreens.bind(event, ReprocessMenus.MACHINE_SOLIDIFIER, SolidifierScreen::new);
    }
}
