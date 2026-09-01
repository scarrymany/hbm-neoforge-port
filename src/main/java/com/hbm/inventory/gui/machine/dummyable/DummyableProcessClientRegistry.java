package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.inventory.container.machine.dummyable.DummyableProcessMenus;
import com.hbm.main.MainRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class DummyableProcessClientRegistry {

    private DummyableProcessClientRegistry() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(DummyableProcessMenus.FURNACE_COMBINATION.get(), FurnaceCombinationScreen::new);
        event.register(DummyableProcessMenus.MACHINE_BLAST_FURNACE.get(), BlastFurnaceScreen::new);
        event.register(DummyableProcessMenus.MACHINE_ROCK_MILL.get(), RockMillScreen::new);
        event.register(DummyableProcessMenus.MACHINE_ANNIHILATOR.get(), AnnihilatorScreen::new);
        event.register(DummyableProcessMenus.MACHINE_PRESS.get(), PressScreen::new);
        event.register(DummyableProcessMenus.MACHINE_ROTARY_FURNACE.get(), RotaryFurnaceScreen::new);
        event.register(DummyableProcessMenus.MACHINE_FRACTION_TOWER.get(), FractionTowerScreen::new);
        event.register(DummyableProcessMenus.WASTE_DRUM.get(), WasteDrumScreen::new);
        event.register(DummyableProcessMenus.MACHINE_COMPRESSOR.get(), CompressorScreen::new);
        event.register(DummyableProcessMenus.MACHINE_COKER.get(), CokerScreen::new);
        event.register(DummyableProcessMenus.MACHINE_CATALYTIC_CRACKER.get(), CatalyticCrackerScreen::new);
        event.register(DummyableProcessMenus.MACHINE_CATALYTIC_REFORMER.get(), CatalyticReformerScreen::new);
        event.register(DummyableProcessMenus.MACHINE_HYDROTREATER.get(), HydrotreaterScreen::new);
        event.register(DummyableProcessMenus.MACHINE_VACUUM_DISTILL.get(), VacuumDistillScreen::new);
        event.register(DummyableProcessMenus.MACHINE_RADIOLYSIS.get(), RadiolysisScreen::new);
        event.register(DummyableProcessMenus.MACHINE_FLARE.get(), GasFlareScreen::new);
        event.register(DummyableProcessMenus.MACHINE_EPRESS.get(), EPressScreen::new);
    }
}
