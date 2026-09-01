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
        event.register(DummyableProcessMenus.MACHINE_PYROOVEN.get(), PyroOvenScreen::new);
        event.register(DummyableProcessMenus.MACHINE_ARC_FURNACE.get(), ArcFurnaceScreen::new);
        event.register(DummyableProcessMenus.MACHINE_EXPOSURE_CHAMBER.get(), ExposureChamberScreen::new);
        event.register(DummyableProcessMenus.MACHINE_ORE_SLOPPER.get(), OreSlopperScreen::new);
        event.register(DummyableProcessMenus.MACHINE_TURBOFAN.get(), TurbofanScreen::new);
        event.register(DummyableProcessMenus.MACHINE_RADGEN.get(), RadGenScreen::new);
        event.register(DummyableProcessMenus.MACHINE_HEPHAESTUS.get(), HephaestusScreen::new);
        event.register(DummyableProcessMenus.MACHINE_WOOD_BURNER.get(), WoodBurnerScreen::new);
        event.register(DummyableProcessMenus.FURNACE_IRON.get(), FurnaceIronScreen::new);
        event.register(DummyableProcessMenus.FURNACE_STEEL.get(), FurnaceSteelScreen::new);
        event.register(DummyableProcessMenus.HEATER_FIREBOX.get(), FireboxScreen::new);
        event.register(DummyableProcessMenus.HEATER_OVEN.get(), HeaterOvenScreen::new);
        event.register(DummyableProcessMenus.HEATER_OILBURNER.get(), OilburnerScreen::new);
        event.register(DummyableProcessMenus.MACHINE_SAWMILL.get(), SawmillScreen::new);
    }
}
