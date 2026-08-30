package com.hbm.inventory.gui.machine.chem;

import com.hbm.inventory.container.machine.chem.ChemIsotopeMenus;
import com.hbm.main.MainRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-side {@link net.minecraft.world.inventory.MenuType}-to-{@code Screen} binding for this machine family - see {@code PowerGenClientRegistry} for the precedent. */
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT)
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
