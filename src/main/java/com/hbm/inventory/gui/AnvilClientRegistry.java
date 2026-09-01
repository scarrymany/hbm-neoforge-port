package com.hbm.inventory.gui;

import com.hbm.inventory.container.AnvilMenus;
import com.hbm.main.MainRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class AnvilClientRegistry {

    private AnvilClientRegistry() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        SafeMenuScreens.bind(event, AnvilMenus.ANVIL, AnvilScreen::new);
    }
}
