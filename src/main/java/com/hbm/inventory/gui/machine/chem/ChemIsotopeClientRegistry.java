package com.hbm.inventory.gui.machine.chem;

import com.hbm.inventory.container.machine.chem.ChemIsotopeMenus;
import com.hbm.main.MainRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-side {@link net.minecraft.world.inventory.MenuType}-to-{@code Screen} binding for this machine family - see {@code PowerGenClientRegistry} for the precedent. */
// bus = Bus.MOD required: RegisterMenuScreensEvent implements IModBusEvent and only fires on the mod
// bus - @EventBusSubscriber's bus() defaults to Bus.GAME and does not auto-detect IModBusEvent
// (confirmed against real NeoForge 1.21.1 source and FancyModLoader's EventBusSubscriber javadoc).
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ChemIsotopeClientRegistry {

    private ChemIsotopeClientRegistry() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ChemIsotopeMenus.CENTRIFUGE.get(), CentrifugeScreen::new);
        event.register(ChemIsotopeMenus.GAS_CENTRIFUGE.get(), GasCentrifugeScreen::new);
        event.register(ChemIsotopeMenus.SILEX.get(), SilexScreen::new);
        event.register(ChemIsotopeMenus.CYCLOTRON.get(), CyclotronScreen::new);
        event.register(ChemIsotopeMenus.CHEM_PLANT.get(), ChemPlantScreen::new);
        event.register(ChemIsotopeMenus.ELECTROLYSER.get(), ElectrolyserScreen::new);
    }
}
