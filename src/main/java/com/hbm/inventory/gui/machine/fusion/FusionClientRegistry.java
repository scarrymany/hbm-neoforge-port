package com.hbm.inventory.gui.machine.fusion;

import com.hbm.inventory.container.machine.fusion.FusionMenus;
import com.hbm.main.MainRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-side {@link net.minecraft.world.inventory.MenuType}-to-{@code Screen} binding for this machine family - see {@code ChemIsotopeClientRegistry} for the precedent. */
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT)
public final class FusionClientRegistry {

    private FusionClientRegistry() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(FusionMenus.ICF_REACTOR.get(), IcfReactorScreen::new);
        event.register(FusionMenus.ICF_PRESS.get(), IcfPressScreen::new);
        event.register(FusionMenus.WATZ_REACTOR.get(), WatzReactorScreen::new);
    }
}
