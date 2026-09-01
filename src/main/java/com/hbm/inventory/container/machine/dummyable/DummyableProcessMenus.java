package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.FurnaceCombinationBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineAnnihilatorBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineBlastFurnaceBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineFractionTowerBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachinePressBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineRockMillBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineRotaryFurnaceBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineCatalyticCrackerBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineCatalyticReformerBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineCokerBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineCompressorBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineEPressBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineGasFlareBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineHydrotreaterBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineRadiolysisBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineArcFurnaceBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineExposureChamberBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachinePyroOvenBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineHephaestusBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineOreSlopperBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineRadGenBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineTurbofanBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineVacuumDistillBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineWoodBurnerBlockEntity;
import com.hbm.blockentity.machine.dummyable.FurnaceIronBlockEntity;
import com.hbm.blockentity.machine.dummyable.FurnaceSteelBlockEntity;
import com.hbm.blockentity.machine.dummyable.HeaterFireboxBlockEntity;
import com.hbm.blockentity.machine.dummyable.HeaterOilburnerBlockEntity;
import com.hbm.blockentity.machine.dummyable.HeaterOvenBlockEntity;
import com.hbm.blockentity.machine.dummyable.HeaterElectricBlockEntity;
import com.hbm.blockentity.machine.dummyable.HeaterHeatexBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineAutosawBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineSawmillBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineStirlingBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineSuperComputerBlockEntity;
import com.hbm.blockentity.machine.dummyable.StorageDrumBlockEntity;
import com.hbm.blockentity.machine.dummyable.CondenserBlockEntity;
import com.hbm.blockentity.machine.dummyable.DeuteriumExtractorBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineBAT9000BlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineDrainBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineIntakeBlockEntity;
import com.hbm.blockentity.machine.dummyable.HeatBoilerBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineAshpitBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineSirenBlockEntity;
import com.hbm.blockentity.machine.dummyable.RadarScreenBlockEntity;
import com.hbm.blockentity.machine.dummyable.RadioTelexBlockEntity;
import com.hbm.blockentity.machine.dummyable.WasteDrumBlockEntity;
import com.hbm.blockentity.machine.dummyable.HexTankBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineElectricFurnaceBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineFunnelBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineMicrowaveBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineOrbusBlockEntity;
import com.hbm.blockentity.machine.dummyable.FluidBarrelBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineBrickFurnaceBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineRtgFurnaceBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineAutocrafterBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineKeyForgeBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineDiFurnaceBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineDiFurnaceRtgBlockEntity;
import com.hbm.inventory.container.ModMenuTypes;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class DummyableProcessMenus {

    public static DeferredHolder<MenuType<?>, MenuType<FurnaceCombinationMenu>> FURNACE_COMBINATION;
    public static DeferredHolder<MenuType<?>, MenuType<BlastFurnaceMenu>> MACHINE_BLAST_FURNACE;
    public static DeferredHolder<MenuType<?>, MenuType<RockMillMenu>> MACHINE_ROCK_MILL;
    public static DeferredHolder<MenuType<?>, MenuType<AnnihilatorMenu>> MACHINE_ANNIHILATOR;
    public static DeferredHolder<MenuType<?>, MenuType<PressMenu>> MACHINE_PRESS;
    public static DeferredHolder<MenuType<?>, MenuType<RotaryFurnaceMenu>> MACHINE_ROTARY_FURNACE;
    public static DeferredHolder<MenuType<?>, MenuType<FractionTowerMenu>> MACHINE_FRACTION_TOWER;
    public static DeferredHolder<MenuType<?>, MenuType<WasteDrumMenu>> WASTE_DRUM;
    public static DeferredHolder<MenuType<?>, MenuType<CompressorMenu>> MACHINE_COMPRESSOR;
    public static DeferredHolder<MenuType<?>, MenuType<CokerMenu>> MACHINE_COKER;
    public static DeferredHolder<MenuType<?>, MenuType<CatalyticCrackerMenu>> MACHINE_CATALYTIC_CRACKER;
    public static DeferredHolder<MenuType<?>, MenuType<CatalyticReformerMenu>> MACHINE_CATALYTIC_REFORMER;
    public static DeferredHolder<MenuType<?>, MenuType<HydrotreaterMenu>> MACHINE_HYDROTREATER;
    public static DeferredHolder<MenuType<?>, MenuType<VacuumDistillMenu>> MACHINE_VACUUM_DISTILL;
    public static DeferredHolder<MenuType<?>, MenuType<RadiolysisMenu>> MACHINE_RADIOLYSIS;
    public static DeferredHolder<MenuType<?>, MenuType<GasFlareMenu>> MACHINE_FLARE;
    public static DeferredHolder<MenuType<?>, MenuType<EPressMenu>> MACHINE_EPRESS;
    public static DeferredHolder<MenuType<?>, MenuType<PyroOvenMenu>> MACHINE_PYROOVEN;
    public static DeferredHolder<MenuType<?>, MenuType<ArcFurnaceMenu>> MACHINE_ARC_FURNACE;
    public static DeferredHolder<MenuType<?>, MenuType<ExposureChamberMenu>> MACHINE_EXPOSURE_CHAMBER;
    public static DeferredHolder<MenuType<?>, MenuType<OreSlopperMenu>> MACHINE_ORE_SLOPPER;
    public static DeferredHolder<MenuType<?>, MenuType<TurbofanMenu>> MACHINE_TURBOFAN;
    public static DeferredHolder<MenuType<?>, MenuType<RadGenMenu>> MACHINE_RADGEN;
    public static DeferredHolder<MenuType<?>, MenuType<HephaestusMenu>> MACHINE_HEPHAESTUS;
    public static DeferredHolder<MenuType<?>, MenuType<WoodBurnerMenu>> MACHINE_WOOD_BURNER;
    public static DeferredHolder<MenuType<?>, MenuType<FurnaceIronMenu>> FURNACE_IRON;
    public static DeferredHolder<MenuType<?>, MenuType<FurnaceSteelMenu>> FURNACE_STEEL;
    public static DeferredHolder<MenuType<?>, MenuType<FireboxMenu>> HEATER_FIREBOX;
    public static DeferredHolder<MenuType<?>, MenuType<HeaterOvenMenu>> HEATER_OVEN;
    public static DeferredHolder<MenuType<?>, MenuType<OilburnerMenu>> HEATER_OILBURNER;
    public static DeferredHolder<MenuType<?>, MenuType<SawmillMenu>> MACHINE_SAWMILL;
    public static DeferredHolder<MenuType<?>, MenuType<HeaterElectricMenu>> HEATER_ELECTRIC;
    public static DeferredHolder<MenuType<?>, MenuType<HeaterHeatexMenu>> HEATER_HEATEX;
    public static DeferredHolder<MenuType<?>, MenuType<StirlingMenu>> MACHINE_STIRLING;
    public static DeferredHolder<MenuType<?>, MenuType<StorageDrumMenu>> MACHINE_STORAGE_DRUM;
    public static DeferredHolder<MenuType<?>, MenuType<SuperComputerMenu>> MACHINE_SUPERCOMPUTER;
    public static DeferredHolder<MenuType<?>, MenuType<AutosawMenu>> MACHINE_AUTOSAW;
    public static DeferredHolder<MenuType<?>, MenuType<AshpitMenu>> MACHINE_ASHPIT;
    public static DeferredHolder<MenuType<?>, MenuType<HeatBoilerMenu>> HEAT_BOILER;
    public static DeferredHolder<MenuType<?>, MenuType<CondenserMenu>> MACHINE_CONDENSER;
    public static DeferredHolder<MenuType<?>, MenuType<SirenMenu>> MACHINE_SIREN;
    public static DeferredHolder<MenuType<?>, MenuType<RadioTelexMenu>> RADIO_TELEX;
    public static DeferredHolder<MenuType<?>, MenuType<RadarScreenMenu>> RADAR_SCREEN;
    public static DeferredHolder<MenuType<?>, MenuType<IntakeMenu>> MACHINE_INTAKE;
    public static DeferredHolder<MenuType<?>, MenuType<DrainMenu>> MACHINE_DRAIN;
    public static DeferredHolder<MenuType<?>, MenuType<BAT9000Menu>> MACHINE_BAT9000;
    public static DeferredHolder<MenuType<?>, MenuType<DeuteriumMenu>> MACHINE_DEUTERIUM;
    public static DeferredHolder<MenuType<?>, MenuType<HexTankMenu>> HEX_TANK;
    public static DeferredHolder<MenuType<?>, MenuType<OrbusMenu>> MACHINE_ORBUS;
    public static DeferredHolder<MenuType<?>, MenuType<FunnelMenu>> MACHINE_FUNNEL;
    public static DeferredHolder<MenuType<?>, MenuType<MicrowaveMenu>> MACHINE_MICROWAVE;
    public static DeferredHolder<MenuType<?>, MenuType<ElectricFurnaceMenu>> MACHINE_ELECTRIC_FURNACE;
    public static DeferredHolder<MenuType<?>, MenuType<BrickFurnaceMenu>> MACHINE_BRICK_FURNACE;
    public static DeferredHolder<MenuType<?>, MenuType<RtgFurnaceMenu>> MACHINE_RTG_FURNACE;
    public static DeferredHolder<MenuType<?>, MenuType<FluidBarrelMenu>> FLUID_BARREL;
    public static DeferredHolder<MenuType<?>, MenuType<AutocrafterMenu>> MACHINE_AUTOCRAFTER;
    public static DeferredHolder<MenuType<?>, MenuType<KeyForgeMenu>> MACHINE_KEYFORGE;
    public static DeferredHolder<MenuType<?>, MenuType<DiFurnaceMenu>> MACHINE_DIFURNACE;
    public static DeferredHolder<MenuType<?>, MenuType<DiFurnaceRtgMenu>> MACHINE_DIFURNACE_RTG;

    private DummyableProcessMenus() {
    }

    public static void registerAll() {
        FURNACE_COMBINATION = reg("furnace_combination", (id, inv, buf) ->
                new FurnaceCombinationMenu(id, inv, (FurnaceCombinationBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_BLAST_FURNACE = reg("machine_blast_furnace", (id, inv, buf) ->
                new BlastFurnaceMenu(id, inv, (MachineBlastFurnaceBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_ROCK_MILL = reg("machine_rock_mill", (id, inv, buf) ->
                new RockMillMenu(id, inv, (MachineRockMillBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_ANNIHILATOR = reg("machine_annihilator", (id, inv, buf) ->
                new AnnihilatorMenu(id, inv, (MachineAnnihilatorBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_PRESS = reg("machine_press", (id, inv, buf) ->
                new PressMenu(id, inv, (MachinePressBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_ROTARY_FURNACE = reg("machine_rotary_furnace", (id, inv, buf) ->
                new RotaryFurnaceMenu(id, inv, (MachineRotaryFurnaceBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_FRACTION_TOWER = reg("machine_fraction_tower", (id, inv, buf) ->
                new FractionTowerMenu(id, inv, (MachineFractionTowerBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        WASTE_DRUM = reg("machine_waste_drum", (id, inv, buf) ->
                new WasteDrumMenu(id, inv, (WasteDrumBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_COMPRESSOR = reg("machine_compressor", (id, inv, buf) ->
                new CompressorMenu(id, inv, (MachineCompressorBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_COKER = reg("machine_coker", (id, inv, buf) ->
                new CokerMenu(id, inv, (MachineCokerBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_CATALYTIC_CRACKER = reg("machine_catalytic_cracker", (id, inv, buf) ->
                new CatalyticCrackerMenu(id, inv, (MachineCatalyticCrackerBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_CATALYTIC_REFORMER = reg("machine_catalytic_reformer", (id, inv, buf) ->
                new CatalyticReformerMenu(id, inv, (MachineCatalyticReformerBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_HYDROTREATER = reg("machine_hydrotreater", (id, inv, buf) ->
                new HydrotreaterMenu(id, inv, (MachineHydrotreaterBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_VACUUM_DISTILL = reg("machine_vacuum_distill", (id, inv, buf) ->
                new VacuumDistillMenu(id, inv, (MachineVacuumDistillBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_RADIOLYSIS = reg("machine_radiolysis", (id, inv, buf) ->
                new RadiolysisMenu(id, inv, (MachineRadiolysisBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_FLARE = reg("machine_flare", (id, inv, buf) ->
                new GasFlareMenu(id, inv, (MachineGasFlareBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_EPRESS = reg("machine_epress", (id, inv, buf) ->
                new EPressMenu(id, inv, (MachineEPressBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_PYROOVEN = reg("machine_pyrooven", (id, inv, buf) ->
                new PyroOvenMenu(id, inv, (MachinePyroOvenBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_ARC_FURNACE = reg("machine_arc_furnace", (id, inv, buf) ->
                new ArcFurnaceMenu(id, inv, (MachineArcFurnaceBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_EXPOSURE_CHAMBER = reg("machine_exposure_chamber", (id, inv, buf) ->
                new ExposureChamberMenu(id, inv, (MachineExposureChamberBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_ORE_SLOPPER = reg("machine_ore_slopper", (id, inv, buf) ->
                new OreSlopperMenu(id, inv, (MachineOreSlopperBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_TURBOFAN = reg("machine_turbofan", (id, inv, buf) ->
                new TurbofanMenu(id, inv, (MachineTurbofanBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_RADGEN = reg("machine_radgen", (id, inv, buf) ->
                new RadGenMenu(id, inv, (MachineRadGenBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_HEPHAESTUS = reg("machine_hephaestus", (id, inv, buf) ->
                new HephaestusMenu(id, inv, (MachineHephaestusBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_WOOD_BURNER = reg("machine_wood_burner", (id, inv, buf) ->
                new WoodBurnerMenu(id, inv, (MachineWoodBurnerBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        FURNACE_IRON = reg("furnace_iron", (id, inv, buf) ->
                new FurnaceIronMenu(id, inv, (FurnaceIronBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        FURNACE_STEEL = reg("furnace_steel", (id, inv, buf) ->
                new FurnaceSteelMenu(id, inv, (FurnaceSteelBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        HEATER_FIREBOX = reg("heater_firebox", (id, inv, buf) ->
                new FireboxMenu(id, inv, (HeaterFireboxBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        HEATER_OVEN = reg("heater_oven", (id, inv, buf) ->
                new HeaterOvenMenu(id, inv, (HeaterOvenBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        HEATER_OILBURNER = reg("heater_oilburner", (id, inv, buf) ->
                new OilburnerMenu(id, inv, (HeaterOilburnerBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_SAWMILL = reg("machine_sawmill", (id, inv, buf) ->
                new SawmillMenu(id, inv, (MachineSawmillBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        HEATER_ELECTRIC = reg("heater_electric", (id, inv, buf) ->
                new HeaterElectricMenu(id, inv, (HeaterElectricBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        HEATER_HEATEX = reg("heater_heatex", (id, inv, buf) ->
                new HeaterHeatexMenu(id, inv, (HeaterHeatexBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_STIRLING = reg("machine_stirling", (id, inv, buf) ->
                new StirlingMenu(id, inv, (MachineStirlingBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_STORAGE_DRUM = reg("machine_storage_drum", (id, inv, buf) ->
                new StorageDrumMenu(id, inv, (StorageDrumBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_SUPERCOMPUTER = reg("machine_supercomputer", (id, inv, buf) ->
                new SuperComputerMenu(id, inv, (MachineSuperComputerBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_AUTOSAW = reg("machine_autosaw", (id, inv, buf) ->
                new AutosawMenu(id, inv, (MachineAutosawBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_ASHPIT = reg("machine_ashpit", (id, inv, buf) ->
                new AshpitMenu(id, inv, (MachineAshpitBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        HEAT_BOILER = reg("heat_boiler", (id, inv, buf) ->
                new HeatBoilerMenu(id, inv, (HeatBoilerBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_CONDENSER = reg("machine_condenser", (id, inv, buf) ->
                new CondenserMenu(id, inv, (CondenserBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_SIREN = reg("machine_siren", (id, inv, buf) ->
                new SirenMenu(id, inv, (MachineSirenBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        RADIO_TELEX = reg("radio_telex", (id, inv, buf) ->
                new RadioTelexMenu(id, inv, (RadioTelexBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        RADAR_SCREEN = reg("radar_screen", (id, inv, buf) ->
                new RadarScreenMenu(id, inv, (RadarScreenBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_INTAKE = reg("machine_intake", (id, inv, buf) ->
                new IntakeMenu(id, inv, (MachineIntakeBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_DRAIN = reg("machine_drain", (id, inv, buf) ->
                new DrainMenu(id, inv, (MachineDrainBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_BAT9000 = reg("machine_bat9000", (id, inv, buf) ->
                new BAT9000Menu(id, inv, (MachineBAT9000BlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_DEUTERIUM = reg("machine_deuterium", (id, inv, buf) ->
                new DeuteriumMenu(id, inv, (DeuteriumExtractorBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        HEX_TANK = reg("hex_tank", (id, inv, buf) ->
                new HexTankMenu(id, inv, (HexTankBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_ORBUS = reg("machine_orbus", (id, inv, buf) ->
                new OrbusMenu(id, inv, (MachineOrbusBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_FUNNEL = reg("machine_funnel", (id, inv, buf) ->
                new FunnelMenu(id, inv, (MachineFunnelBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_MICROWAVE = reg("machine_microwave", (id, inv, buf) ->
                new MicrowaveMenu(id, inv, (MachineMicrowaveBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_ELECTRIC_FURNACE = reg("machine_electric_furnace", (id, inv, buf) ->
                new ElectricFurnaceMenu(id, inv, (MachineElectricFurnaceBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_BRICK_FURNACE = reg("machine_furnace_brick", (id, inv, buf) ->
                new BrickFurnaceMenu(id, inv, (MachineBrickFurnaceBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_RTG_FURNACE = reg("machine_rtg_furnace", (id, inv, buf) ->
                new RtgFurnaceMenu(id, inv, (MachineRtgFurnaceBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        FLUID_BARREL = reg("fluid_barrel", (id, inv, buf) ->
                new FluidBarrelMenu(id, inv, (FluidBarrelBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_AUTOCRAFTER = reg("machine_autocrafter", (id, inv, buf) ->
                new AutocrafterMenu(id, inv, (MachineAutocrafterBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_KEYFORGE = reg("machine_keyforge", (id, inv, buf) ->
                new KeyForgeMenu(id, inv, (MachineKeyForgeBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_DIFURNACE = reg("machine_difurnace", (id, inv, buf) ->
                new DiFurnaceMenu(id, inv, (MachineDiFurnaceBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_DIFURNACE_RTG = reg("machine_difurnace_rtg", (id, inv, buf) ->
                new DiFurnaceRtgMenu(id, inv, (MachineDiFurnaceRtgBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
    }

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> reg(String name, IContainerFactory<T> factory) {
        return ModMenuTypes.MENU_TYPES.register(name, () -> IMenuTypeExtension.create(factory));
    }
}
