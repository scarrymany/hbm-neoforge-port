package com.hbm.inventory.gui.machine.workshop;

import com.hbm.inventory.container.machine.workshop.WorkshopMenus;
import com.hbm.main.MainRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class WorkshopClientRegistry {

    private WorkshopClientRegistry() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(WorkshopMenus.MACHINE_AMMO_PRESS.get(), AmmoPressScreen::new);
        event.register(WorkshopMenus.MACHINE_ARC_WELDER.get(), ArcWelderScreen::new);
        event.register(WorkshopMenus.MACHINE_SOLDERING_STATION.get(), SolderingScreen::new);
    }
}
