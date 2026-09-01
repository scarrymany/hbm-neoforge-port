package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.inventory.container.machine.dummyable.DummyableProcessMenus;
import com.hbm.inventory.gui.SafeMenuScreens;
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
        SafeMenuScreens.bind(event, DummyableProcessMenus.FURNACE_COMBINATION, FurnaceCombinationScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_BLAST_FURNACE, BlastFurnaceScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_ROCK_MILL, RockMillScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_ANNIHILATOR, AnnihilatorScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_PRESS, PressScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_ROTARY_FURNACE, RotaryFurnaceScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_FRACTION_TOWER, FractionTowerScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.WASTE_DRUM, WasteDrumScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_COMPRESSOR, CompressorScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_COKER, CokerScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_CATALYTIC_CRACKER, CatalyticCrackerScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_CATALYTIC_REFORMER, CatalyticReformerScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_HYDROTREATER, HydrotreaterScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_VACUUM_DISTILL, VacuumDistillScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_RADIOLYSIS, RadiolysisScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_FLARE, GasFlareScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_EPRESS, EPressScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_PYROOVEN, PyroOvenScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_ARC_FURNACE, ArcFurnaceScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_EXPOSURE_CHAMBER, ExposureChamberScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_ORE_SLOPPER, OreSlopperScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_TURBOFAN, TurbofanScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_RADGEN, RadGenScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_HEPHAESTUS, HephaestusScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_WOOD_BURNER, WoodBurnerScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.FURNACE_IRON, FurnaceIronScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.FURNACE_STEEL, FurnaceSteelScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.HEATER_FIREBOX, FireboxScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.HEATER_OVEN, HeaterOvenScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.HEATER_OILBURNER, OilburnerScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_SAWMILL, SawmillScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.HEATER_ELECTRIC, HeaterElectricScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.HEATER_HEATEX, HeaterHeatexScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_STIRLING, StirlingScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_STORAGE_DRUM, StorageDrumScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_SUPERCOMPUTER, SuperComputerScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_AUTOSAW, AutosawScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_ASHPIT, AshpitScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.HEAT_BOILER, HeatBoilerScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_CONDENSER, CondenserScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_SIREN, SirenScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.RADIO_TELEX, RadioTelexScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.RADAR_SCREEN, RadarScreenScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_INTAKE, IntakeScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_DRAIN, DrainScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_BAT9000, BAT9000Screen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_DEUTERIUM, DeuteriumScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.HEX_TANK, HexTankScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_ORBUS, OrbusScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_FUNNEL, FunnelScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_MICROWAVE, MicrowaveScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_ELECTRIC_FURNACE, ElectricFurnaceScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_BRICK_FURNACE, BrickFurnaceScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_RTG_FURNACE, RtgFurnaceScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.FLUID_BARREL, FluidBarrelScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_AUTOCRAFTER, AutocrafterScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_KEYFORGE, KeyForgeScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_DIFURNACE, DiFurnaceScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_DIFURNACE_RTG, DiFurnaceRtgScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_CONVEYOR_PRESS, ConveyorPressScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MASS_STORAGE, MassStorageScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_TELELINKER, TeleLinkerScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.SOYUZ_CAPSULE, SoyuzCapsuleScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.FILING_CABINET, FileCabinetScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_MINING_LASER, MiningLaserScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_STRAND_CASTER, StrandCasterScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_FORCEFIELD, ForceFieldScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_FLUIDTANK, MachineFluidTankScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_BIGASSTANK, BigAssTankScreen::new);
        SafeMenuScreens.bind(event, DummyableProcessMenus.MACHINE_SATLINKER, SatLinkerScreen::new);
    }
}
