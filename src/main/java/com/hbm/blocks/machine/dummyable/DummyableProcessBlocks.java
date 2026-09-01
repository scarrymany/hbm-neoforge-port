package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.FluidBarrelBlock;
import com.hbm.blocks.machine.MachineAutocrafterBlock;
import com.hbm.blocks.machine.MachineBrickFurnaceBlock;
import com.hbm.blocks.machine.MachineDetectorBlock;
import com.hbm.blocks.machine.MachineDiFurnaceBlock;
import com.hbm.blocks.machine.MachineDiFurnaceRtgBlock;
import com.hbm.blocks.machine.MachineElectricFurnaceBlock;
import com.hbm.blocks.machine.MachineFunnelBlock;
import com.hbm.blocks.machine.MachineKeyForgeBlock;
import com.hbm.blocks.machine.MachineMicrowaveBlock;
import com.hbm.blocks.machine.MachineRtgFurnaceBlock;
import com.hbm.blocks.machine.WasteDrumBlock;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.inventory.container.machine.dummyable.DummyableProcessMenus;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/**
 * Dummyable process machines + waste drum.
 * CE furnace_combination / blast / rock mill / annihilator / press / rotary furnace / fraction tower /
 * waste_drum / compressor / coker / catalytic cracker / catalytic reformer / hydrotreater /
 * vacuum distill / radiolysis / flare / epress / pyrooven / arc furnace / exposure /
 * ore slopper / turbofan / radgen / hephaestus / wood burner /
 * furnace iron / furnace steel / firebox / oven / oilburner / sawmill /
 * ashpit / heat boilers / cooling towers / telex / radar screen / siren / condenser /
 * condenser powered / intake / drain / BAT9000 / deuterium / fan /
 * UF6/PuF6 tanks / funnel / microwave / electric furnace / detector / orbus /
 * autocrafter / keyforge / di-furnace / RTG di-furnace /
 * conveyor press / mass storage.
 */
public final class DummyableProcessBlocks {

    private static final BlockBehaviour.Properties MACHINE_PROPS =
            BlockBehaviour.Properties.of().strength(5.0F, 15.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();

    public static DeferredBlock<FurnaceCombinationBlock> FURNACE_COMBINATION;
    public static DeferredBlock<MachineBlastFurnaceBlock> MACHINE_BLAST_FURNACE;
    public static DeferredBlock<MachineRockMillBlock> MACHINE_ROCK_MILL;
    public static DeferredBlock<MachineAnnihilatorBlock> MACHINE_ANNIHILATOR;
    public static DeferredBlock<MachinePressBlock> MACHINE_PRESS;
    public static DeferredBlock<MachineRotaryFurnaceBlock> MACHINE_ROTARY_FURNACE;
    public static DeferredBlock<MachineFractionTowerBlock> MACHINE_FRACTION_TOWER;
    public static DeferredBlock<WasteDrumBlock> WASTE_DRUM;
    public static DeferredBlock<MachineCompressorBlock> MACHINE_COMPRESSOR;
    public static DeferredBlock<MachineCokerBlock> MACHINE_COKER;
    public static DeferredBlock<MachineCatalyticCrackerBlock> MACHINE_CATALYTIC_CRACKER;
    public static DeferredBlock<MachineCatalyticReformerBlock> MACHINE_CATALYTIC_REFORMER;
    public static DeferredBlock<MachineHydrotreaterBlock> MACHINE_HYDROTREATER;
    public static DeferredBlock<MachineVacuumDistillBlock> MACHINE_VACUUM_DISTILL;
    public static DeferredBlock<MachineRadiolysisBlock> MACHINE_RADIOLYSIS;
    public static DeferredBlock<MachineGasFlareBlock> MACHINE_FLARE;
    public static DeferredBlock<MachineEPressBlock> MACHINE_EPRESS;
    public static DeferredBlock<MachinePyroOvenBlock> MACHINE_PYROOVEN;
    public static DeferredBlock<MachineArcFurnaceBlock> MACHINE_ARC_FURNACE;
    public static DeferredBlock<MachineExposureChamberBlock> MACHINE_EXPOSURE_CHAMBER;
    public static DeferredBlock<MachineOreSlopperBlock> MACHINE_ORE_SLOPPER;
    public static DeferredBlock<MachineTurbofanBlock> MACHINE_TURBOFAN;
    public static DeferredBlock<MachineRadGenBlock> MACHINE_RADGEN;
    public static DeferredBlock<MachineHephaestusBlock> MACHINE_HEPHAESTUS;
    public static DeferredBlock<MachineWoodBurnerBlock> MACHINE_WOOD_BURNER;
    public static DeferredBlock<FurnaceIronBlock> FURNACE_IRON;
    public static DeferredBlock<FurnaceSteelBlock> FURNACE_STEEL;
    public static DeferredBlock<HeaterFireboxBlock> HEATER_FIREBOX;
    public static DeferredBlock<HeaterOvenBlock> HEATER_OVEN;
    public static DeferredBlock<HeaterOilburnerBlock> HEATER_OILBURNER;
    public static DeferredBlock<MachineSawmillBlock> MACHINE_SAWMILL;
    public static DeferredBlock<HeaterElectricBlock> HEATER_ELECTRIC;
    public static DeferredBlock<HeaterHeatexBlock> HEATER_HEATEX;
    public static DeferredBlock<MachineStirlingBlock> MACHINE_STIRLING;
    public static DeferredBlock<MachineStirlingBlock> MACHINE_STIRLING_STEEL;
    public static DeferredBlock<MachineStirlingBlock> MACHINE_STIRLING_CREATIVE;
    public static DeferredBlock<com.hbm.blocks.machine.StorageDrumBlock> MACHINE_STORAGE_DRUM;
    public static DeferredBlock<MachineSuperComputerBlock> MACHINE_SUPERCOMPUTER;
    public static DeferredBlock<com.hbm.blocks.machine.MachineAutosawBlock> MACHINE_AUTOSAW;
    public static DeferredBlock<FractionSpacerBlock> FRACTION_SPACER;
    public static DeferredBlock<WatzPumpBlock> WATZ_PUMP;
    public static DeferredBlock<VendingMachineBlock> VENDING_MACHINE;
    public static DeferredBlock<MachineAshpitBlock> MACHINE_ASHPIT;
    public static DeferredBlock<HeatBoilerBlock> HEAT_BOILER;
    public static DeferredBlock<HeatBoilerIndustrialBlock> MACHINE_INDUSTRIAL_BOILER;
    public static DeferredBlock<MachineTowerSmallBlock> MACHINE_TOWER_SMALL;
    public static DeferredBlock<MachineTowerLargeBlock> MACHINE_TOWER_LARGE;
    public static DeferredBlock<RadioTelexBlock> RADIO_TELEX;
    public static DeferredBlock<RadarScreenBlock> RADAR_SCREEN;
    public static DeferredBlock<com.hbm.blocks.machine.MachineSirenBlock> MACHINE_SIREN;
    public static DeferredBlock<com.hbm.blocks.machine.MachineCondenserBlock> MACHINE_CONDENSER;
    public static DeferredBlock<MachineCondenserPoweredBlock> MACHINE_CONDENSER_POWERED;
    public static DeferredBlock<MachineIntakeBlock> MACHINE_INTAKE;
    public static DeferredBlock<MachineDrainBlock> MACHINE_DRAIN;
    public static DeferredBlock<MachineBAT9000Block> MACHINE_BAT9000;
    public static DeferredBlock<com.hbm.blocks.machine.MachineDeuteriumExtractorBlock> MACHINE_DEUTERIUM_EXTRACTOR;
    public static DeferredBlock<MachineDeuteriumTowerBlock> MACHINE_DEUTERIUM_TOWER;
    public static DeferredBlock<com.hbm.blocks.machine.MachineFanBlock> FAN;
    public static DeferredBlock<MachineHexTankBlock> MACHINE_UF6_TANK;
    public static DeferredBlock<MachineHexTankBlock> MACHINE_PUF6_TANK;
    public static DeferredBlock<MachineFunnelBlock> MACHINE_FUNNEL;
    public static DeferredBlock<MachineMicrowaveBlock> MACHINE_MICROWAVE;
    public static DeferredBlock<MachineElectricFurnaceBlock> MACHINE_ELECTRIC_FURNACE_OFF;
    public static DeferredBlock<MachineElectricFurnaceBlock> MACHINE_ELECTRIC_FURNACE_ON;
    public static DeferredBlock<MachineDetectorBlock> MACHINE_DETECTOR;
    public static DeferredBlock<MachineOrbusBlock> MACHINE_ORBUS;
    public static DeferredBlock<MachineBrickFurnaceBlock> MACHINE_FURNACE_BRICK_OFF;
    public static DeferredBlock<MachineBrickFurnaceBlock> MACHINE_FURNACE_BRICK_ON;
    public static DeferredBlock<MachineRtgFurnaceBlock> MACHINE_RTG_FURNACE_OFF;
    public static DeferredBlock<MachineRtgFurnaceBlock> MACHINE_RTG_FURNACE_ON;
    public static DeferredBlock<FluidBarrelBlock> BARREL_PLASTIC;
    public static DeferredBlock<FluidBarrelBlock> BARREL_CORRODED;
    public static DeferredBlock<FluidBarrelBlock> BARREL_IRON;
    public static DeferredBlock<FluidBarrelBlock> BARREL_STEEL;
    public static DeferredBlock<FluidBarrelBlock> BARREL_TCALLOY;
    public static DeferredBlock<FluidBarrelBlock> BARREL_ANTIMATTER;
    public static DeferredBlock<MachineAutocrafterBlock> MACHINE_AUTOCRAFTER;
    public static DeferredBlock<MachineKeyForgeBlock> MACHINE_KEYFORGE;
    public static DeferredBlock<MachineDiFurnaceBlock> MACHINE_DIFURNACE_OFF;
    public static DeferredBlock<MachineDiFurnaceBlock> MACHINE_DIFURNACE_ON;
    public static DeferredBlock<MachineDiFurnaceRtgBlock> MACHINE_DIFURNACE_RTG_OFF;
    public static DeferredBlock<MachineDiFurnaceRtgBlock> MACHINE_DIFURNACE_RTG_ON;
    public static DeferredBlock<MachineConveyorPressBlock> MACHINE_CONVEYOR_PRESS;
    public static DeferredBlock<com.hbm.blocks.machine.MassStorageBlock> MASS_STORAGE_WOOD;
    public static DeferredBlock<com.hbm.blocks.machine.MassStorageBlock> MASS_STORAGE_IRON;
    public static DeferredBlock<com.hbm.blocks.machine.MassStorageBlock> MASS_STORAGE_DESH;
    public static DeferredBlock<com.hbm.blocks.machine.MassStorageBlock> MASS_STORAGE;

    private DummyableProcessBlocks() {
    }

    public static void registerAll() {
        FURNACE_COMBINATION = registerBlock("furnace_combination", () -> new FurnaceCombinationBlock(MACHINE_PROPS));
        MACHINE_BLAST_FURNACE = registerBlock("machine_blast_furnace", () -> new MachineBlastFurnaceBlock(MACHINE_PROPS));
        MACHINE_ROCK_MILL = registerBlock("machine_rock_mill", () -> new MachineRockMillBlock(MACHINE_PROPS));
        MACHINE_ANNIHILATOR = registerBlock("machine_annihilator", () -> new MachineAnnihilatorBlock(MACHINE_PROPS));
        MACHINE_PRESS = registerBlock("machine_press", () -> new MachinePressBlock(MACHINE_PROPS));
        MACHINE_ROTARY_FURNACE = registerBlock("machine_rotary_furnace", () -> new MachineRotaryFurnaceBlock(MACHINE_PROPS));
        MACHINE_FRACTION_TOWER = registerBlock("machine_fraction_tower", () -> new MachineFractionTowerBlock(MACHINE_PROPS));
        WASTE_DRUM = registerBlock("machine_waste_drum", () -> new WasteDrumBlock(MACHINE_PROPS));
        MACHINE_COMPRESSOR = registerBlock("machine_compressor", () -> new MachineCompressorBlock(MACHINE_PROPS));
        MACHINE_COKER = registerBlock("machine_coker", () -> new MachineCokerBlock(MACHINE_PROPS));
        MACHINE_CATALYTIC_CRACKER = registerBlock("machine_catalytic_cracker", () -> new MachineCatalyticCrackerBlock(MACHINE_PROPS));
        MACHINE_CATALYTIC_REFORMER = registerBlock("machine_catalytic_reformer", () -> new MachineCatalyticReformerBlock(MACHINE_PROPS));
        MACHINE_HYDROTREATER = registerBlock("machine_hydrotreater", () -> new MachineHydrotreaterBlock(MACHINE_PROPS));
        MACHINE_VACUUM_DISTILL = registerBlock("machine_vacuum_distill", () -> new MachineVacuumDistillBlock(MACHINE_PROPS));
        MACHINE_RADIOLYSIS = registerBlock("machine_radiolysis", () -> new MachineRadiolysisBlock(MACHINE_PROPS));
        MACHINE_FLARE = registerBlock("machine_flare", () -> new MachineGasFlareBlock(MACHINE_PROPS));
        MACHINE_EPRESS = registerBlock("machine_epress", () -> new MachineEPressBlock(MACHINE_PROPS));
        MACHINE_PYROOVEN = registerBlock("machine_pyrooven", () -> new MachinePyroOvenBlock(MACHINE_PROPS));
        MACHINE_ARC_FURNACE = registerBlock("machine_arc_furnace", () -> new MachineArcFurnaceBlock(MACHINE_PROPS));
        MACHINE_EXPOSURE_CHAMBER = registerBlock("machine_exposure_chamber", () -> new MachineExposureChamberBlock(MACHINE_PROPS));
        MACHINE_ORE_SLOPPER = registerBlock("machine_ore_slopper", () -> new MachineOreSlopperBlock(MACHINE_PROPS));
        MACHINE_TURBOFAN = registerBlock("machine_turbofan", () -> new MachineTurbofanBlock(MACHINE_PROPS));
        MACHINE_RADGEN = registerBlock("machine_radgen", () -> new MachineRadGenBlock(MACHINE_PROPS));
        MACHINE_HEPHAESTUS = registerBlock("machine_hephaestus", () -> new MachineHephaestusBlock(MACHINE_PROPS));
        MACHINE_WOOD_BURNER = registerBlock("machine_wood_burner", () -> new MachineWoodBurnerBlock(MACHINE_PROPS));
        FURNACE_IRON = registerBlock("furnace_iron", () -> new FurnaceIronBlock(MACHINE_PROPS));
        FURNACE_STEEL = registerBlock("furnace_steel", () -> new FurnaceSteelBlock(MACHINE_PROPS));
        HEATER_FIREBOX = registerBlock("heater_firebox", () -> new HeaterFireboxBlock(MACHINE_PROPS));
        HEATER_OVEN = registerBlock("heater_oven", () -> new HeaterOvenBlock(MACHINE_PROPS));
        HEATER_OILBURNER = registerBlock("heater_oilburner", () -> new HeaterOilburnerBlock(MACHINE_PROPS));
        MACHINE_SAWMILL = registerBlock("machine_sawmill", () -> new MachineSawmillBlock(MACHINE_PROPS));
        HEATER_ELECTRIC = registerBlock("heater_electric", () -> new HeaterElectricBlock(MACHINE_PROPS));
        HEATER_HEATEX = registerBlock("heater_heatex", () -> new HeaterHeatexBlock(MACHINE_PROPS));
        MACHINE_STIRLING = registerBlock("machine_stirling", () -> new MachineStirlingBlock(MACHINE_PROPS));
        MACHINE_STIRLING_STEEL = registerBlock("machine_stirling_steel", () -> new MachineStirlingBlock(MACHINE_PROPS));
        MACHINE_STIRLING_CREATIVE = registerBlock("machine_stirling_creative", () -> new MachineStirlingBlock(MACHINE_PROPS));
        MACHINE_STORAGE_DRUM = registerBlock("machine_storage_drum", () -> new com.hbm.blocks.machine.StorageDrumBlock(MACHINE_PROPS));
        MACHINE_SUPERCOMPUTER = registerBlock("machine_supercomputer", () -> new MachineSuperComputerBlock(MACHINE_PROPS));
        MACHINE_AUTOSAW = registerBlock("machine_autosaw", () -> new com.hbm.blocks.machine.MachineAutosawBlock(MACHINE_PROPS));
        FRACTION_SPACER = registerBlock("fraction_spacer", () -> new FractionSpacerBlock(MACHINE_PROPS));
        WATZ_PUMP = registerBlock("watz_pump", () -> new WatzPumpBlock(MACHINE_PROPS));
        VENDING_MACHINE = registerBlock("vending_machine", () -> new VendingMachineBlock(MACHINE_PROPS));
        MACHINE_ASHPIT = registerBlock("machine_ashpit", () -> new MachineAshpitBlock(MACHINE_PROPS));
        HEAT_BOILER = registerBlock("heat_boiler", () -> new HeatBoilerBlock(MACHINE_PROPS));
        MACHINE_INDUSTRIAL_BOILER = registerBlock("machine_industrial_boiler", () -> new HeatBoilerIndustrialBlock(MACHINE_PROPS));
        MACHINE_TOWER_SMALL = registerBlock("machine_tower_small", () -> new MachineTowerSmallBlock(MACHINE_PROPS));
        MACHINE_TOWER_LARGE = registerBlock("machine_tower_large", () -> new MachineTowerLargeBlock(MACHINE_PROPS));
        RADIO_TELEX = registerBlock("radio_telex", () -> new RadioTelexBlock(MACHINE_PROPS));
        RADAR_SCREEN = registerBlock("radar_screen", () -> new RadarScreenBlock(MACHINE_PROPS));
        MACHINE_SIREN = registerBlock("machine_siren", () -> new com.hbm.blocks.machine.MachineSirenBlock(MACHINE_PROPS));
        MACHINE_CONDENSER = registerBlock("machine_condenser", () -> new com.hbm.blocks.machine.MachineCondenserBlock(MACHINE_PROPS));
        MACHINE_CONDENSER_POWERED = registerBlock("machine_condenser_powered", () -> new MachineCondenserPoweredBlock(MACHINE_PROPS));
        MACHINE_INTAKE = registerBlock("machine_intake", () -> new MachineIntakeBlock(MACHINE_PROPS));
        MACHINE_DRAIN = registerBlock("machine_drain", () -> new MachineDrainBlock(MACHINE_PROPS));
        MACHINE_BAT9000 = registerBlock("machine_bat9000", () -> new MachineBAT9000Block(MACHINE_PROPS));
        MACHINE_DEUTERIUM_EXTRACTOR = registerBlock("machine_deuterium_extractor", () -> new com.hbm.blocks.machine.MachineDeuteriumExtractorBlock(MACHINE_PROPS));
        MACHINE_DEUTERIUM_TOWER = registerBlock("machine_deuterium_tower", () -> new MachineDeuteriumTowerBlock(MACHINE_PROPS));
        FAN = registerBlock("fan", () -> new com.hbm.blocks.machine.MachineFanBlock(MACHINE_PROPS));
        MACHINE_UF6_TANK = registerBlock("machine_uf6_tank", () -> new MachineHexTankBlock(MACHINE_PROPS, false));
        MACHINE_PUF6_TANK = registerBlock("machine_puf6_tank", () -> new MachineHexTankBlock(MACHINE_PROPS, true));
        MACHINE_FUNNEL = registerBlock("machine_funnel", () -> new MachineFunnelBlock(MACHINE_PROPS));
        MACHINE_MICROWAVE = registerBlock("machine_microwave", () -> new MachineMicrowaveBlock(MACHINE_PROPS));
        MACHINE_ELECTRIC_FURNACE_OFF = registerBlock("machine_electric_furnace_off", () -> new MachineElectricFurnaceBlock(MACHINE_PROPS));
        MACHINE_ELECTRIC_FURNACE_ON = registerBlockNoTab("machine_electric_furnace_on", () -> new MachineElectricFurnaceBlock(MACHINE_PROPS));
        MACHINE_DETECTOR = registerBlock("machine_detector", () -> new MachineDetectorBlock(MACHINE_PROPS));
        MACHINE_ORBUS = registerBlock("machine_orbus", () -> new MachineOrbusBlock(MACHINE_PROPS));
        MACHINE_FURNACE_BRICK_OFF = registerBlock("machine_furnace_brick_off", () -> new MachineBrickFurnaceBlock(MACHINE_PROPS));
        MACHINE_FURNACE_BRICK_ON = registerBlockNoTab("machine_furnace_brick_on", () -> new MachineBrickFurnaceBlock(MACHINE_PROPS));
        MACHINE_RTG_FURNACE_OFF = registerBlock("machine_rtg_furnace_off", () -> new MachineRtgFurnaceBlock(MACHINE_PROPS));
        MACHINE_RTG_FURNACE_ON = registerBlockNoTab("machine_rtg_furnace_on", () -> new MachineRtgFurnaceBlock(MACHINE_PROPS));
        BARREL_PLASTIC = registerBlock("barrel_plastic", () -> new FluidBarrelBlock(MACHINE_PROPS, 12_000, FluidBarrelBlock.Kind.PLASTIC));
        BARREL_CORRODED = registerBlockNoTab("barrel_corroded", () -> new FluidBarrelBlock(MACHINE_PROPS, 6_000, FluidBarrelBlock.Kind.CORRODED));
        BARREL_IRON = registerBlockNoTab("barrel_iron", () -> new FluidBarrelBlock(MACHINE_PROPS, 8_000, FluidBarrelBlock.Kind.IRON));
        BARREL_STEEL = registerBlock("barrel_steel", () -> new FluidBarrelBlock(MACHINE_PROPS, 16_000, FluidBarrelBlock.Kind.STEEL));
        BARREL_TCALLOY = registerBlock("barrel_tcalloy", () -> new FluidBarrelBlock(MACHINE_PROPS, 24_000, FluidBarrelBlock.Kind.TCALLOY));
        BARREL_ANTIMATTER = registerBlock("barrel_antimatter", () -> new FluidBarrelBlock(MACHINE_PROPS, 16_000, FluidBarrelBlock.Kind.ANTIMATTER));
        MACHINE_AUTOCRAFTER = registerBlock("machine_autocrafter", () -> new MachineAutocrafterBlock(MACHINE_PROPS));
        MACHINE_KEYFORGE = registerBlock("machine_keyforge", () -> new MachineKeyForgeBlock(MACHINE_PROPS));
        MACHINE_DIFURNACE_OFF = registerBlock("machine_difurnace_off", () -> new MachineDiFurnaceBlock(MACHINE_PROPS));
        MACHINE_DIFURNACE_ON = registerBlockNoTab("machine_difurnace_on", () -> new MachineDiFurnaceBlock(MACHINE_PROPS));
        MACHINE_DIFURNACE_RTG_OFF = registerBlock("machine_difurnace_rtg_off", () -> new MachineDiFurnaceRtgBlock(MACHINE_PROPS));
        MACHINE_DIFURNACE_RTG_ON = registerBlockNoTab("machine_difurnace_rtg_on", () -> new MachineDiFurnaceRtgBlock(MACHINE_PROPS));
        MACHINE_CONVEYOR_PRESS = registerBlock("machine_conveyor_press", () -> new MachineConveyorPressBlock(MACHINE_PROPS));
        MASS_STORAGE_WOOD = registerBlock("mass_storage_wood", () -> new com.hbm.blocks.machine.MassStorageBlock(MACHINE_PROPS, 1_000));
        MASS_STORAGE_IRON = registerBlock("mass_storage_iron", () -> new com.hbm.blocks.machine.MassStorageBlock(MACHINE_PROPS, 10_000));
        MASS_STORAGE_DESH = registerBlock("mass_storage_desh", () -> new com.hbm.blocks.machine.MassStorageBlock(MACHINE_PROPS, 100_000));
        MASS_STORAGE = registerBlock("mass_storage", () -> new com.hbm.blocks.machine.MassStorageBlock(MACHINE_PROPS, 1_000_000));
        DummyableProcessBlockEntities.registerAll();
        DummyableProcessMenus.registerAll();
    }

    private static <T extends net.minecraft.world.level.block.Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory) {
        DeferredBlock<T> block = registerBlockNoTab(name, factory);
        CreativeTabContents.add(ModCreativeTabs.MACHINE, block);
        return block;
    }

    private static <T extends net.minecraft.world.level.block.Block> DeferredBlock<T> registerBlockNoTab(String name, Supplier<T> factory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }
}
