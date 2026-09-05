package com.hbm.inventory.gui.machine.fusion;

import com.hbm.inventory.container.machine.fusion.FusionMenus;
import com.hbm.inventory.gui.SafeMenuScreens;
import com.hbm.main.MainRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-side {@link net.minecraft.world.inventory.MenuType}-to-{@code Screen} binding for this machine family - see {@code ChemIsotopeClientRegistry} for the precedent. */
// bus = Bus.MOD required: RegisterMenuScreensEvent implements IModBusEvent and only fires on the mod
// bus - @EventBusSubscriber's bus() defaults to Bus.GAME and does not auto-detect IModBusEvent
// (confirmed against real NeoForge 1.21.1 source and FancyModLoader's EventBusSubscriber javadoc).
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class FusionClientRegistry {

    private FusionClientRegistry() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        SafeMenuScreens.bind(event, FusionMenus.ICF_REACTOR, IcfReactorScreen::new);
        SafeMenuScreens.bind(event, FusionMenus.ICF_PRESS, IcfPressScreen::new);
        SafeMenuScreens.bind(event, FusionMenus.WATZ_REACTOR, WatzReactorScreen::new);
        SafeMenuScreens.bind(event, FusionMenus.FUSION_PLASMA_FORGE, PlasmaForgeScreen::new);
        SafeMenuScreens.bind(event, FusionMenus.FUSION_TORUS, FusionTorusScreen::new);
        SafeMenuScreens.bind(event, FusionMenus.FUSION_KLYSTRON, FusionKlystronScreen::new);
        SafeMenuScreens.bind(event, FusionMenus.FUSION_BREEDER, FusionBreederScreen::new);
    }
}
