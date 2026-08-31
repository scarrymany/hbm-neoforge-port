package com.hbm.inventory.recipes.chem;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.items.bomb.NukeCasingItems;
import com.hbm.items.machine.ItemArcElectrode;
import com.hbm.items.machine.MachineItems;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipe data for the Chemical Plant, ported from CE {@code ChemicalPlantRecipes.java}.
 * Numbers cited per call site. Blocked recipes (unregistered items) are listed in the header, not invented.
 * <p>
 * Skipped vs CE: biomes/explosives/additives that need unregistered items —
 * {@code chem.biogas} (:85 biomass), {@code chem.tarsand} (:102 ore_oil_sand+ANY_TAR),
 * {@code chem.tel}/ {@code chem.deicer} (:107/:112 fuel_additive), {@code chem.meatprocessing}
 * (:235 glyphid meat), {@code chem.biosolidfuel}/ {@code chem.biooilsolidfuel} (:246/:250 biomass_compressed),
 * {@code chem.schrabidic} (:282 pellet_charged), {@code chem.coltancleaning}/ {@code chem.coltancrystal}
 * (:293/:304 generic {@code dust}), {@code chem.cordite}/ {@code chem.rocketfuel}/ {@code chem.dynamite}/
 * {@code chem.tnt}/ {@code chem.tatb}/ {@code chem.napalm} (unregistered explosives / canister_napalm),
 * {@code chem.batteryquantum} (:176 pellet_charged), {@code chem.uf6} (:361 sulfur item).
 */
public final class ChemPlantRecipes {

    public static final List<ChemPlantRecipe> RECIPES = new ArrayList<>();

    private static boolean registered = false;

    private ChemPlantRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // ChemicalPlantRecipes.java:41
        RECIPES.add(new ChemPlantRecipe("chem.hydrogen", 20, 400,
                new AStack[]{OreDictStack.ofCommonTag("coals")},
                new FluidStack[]{new FluidStack(Fluids.WATER, 8_000)},
                new ItemStack[0],
                new FluidStack(Fluids.HYDROGEN, 500)));

        // :51
        RECIPES.add(new ChemPlantRecipe("chem.oxygen", 20, 400,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.AIR, 8_000)},
                new ItemStack[0],
                new FluidStack(Fluids.OXYGEN, 500)));

        // :55
        RECIPES.add(new ChemPlantRecipe("chem.xenon", 300, 1_000,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.AIR, 16_000)},
                new ItemStack[0],
                new FluidStack(Fluids.XENON, 50)));

        // :59
        RECIPES.add(new ChemPlantRecipe("chem.xenonoxy", 20, 1_000,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.AIR, 8_000), new FluidStack(Fluids.OXYGEN, 250)},
                new ItemStack[0],
                new FluidStack(Fluids.XENON, 50)));

        // :63 moon_turf — Phase8Blocks
        RECIPES.add(new ChemPlantRecipe("chem.helium3", 200, 2_000,
                new AStack[]{new ComparableStack(block("moon_turf"), 8)},
                new FluidStack[0],
                new ItemStack[0],
                new FluidStack(Fluids.HELIUM3, 1_000)));

        // :67
        RECIPES.add(new ChemPlantRecipe("chem.co2", 60, 100,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.GAS, 1_000)},
                new ItemStack[0],
                new FluidStack(Fluids.CARBONDIOXIDE, 1_000)));

        // :71 F.dust → crystal_fluorite (same sub as SILEX/GasCent)
        RECIPES.add(new ChemPlantRecipe("chem.perfluoromethyl", 20, 100,
                new AStack[]{new ComparableStack(PlateCrystalWasteItems.CRYSTAL_FLUORITE.get())},
                new FluidStack[]{new FluidStack(Fluids.PETROLEUM, 1_000), new FluidStack(Fluids.UNSATURATEDS, 500)},
                new ItemStack[0],
                new FluidStack(Fluids.PERFLUOROMETHYL, 1_000)));

        // :76 two fluid outs
        RECIPES.add(new ChemPlantRecipe("chem.cccentrifuge", 200, 100,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.CHLOROCALCITE_CLEANED, 500), new FluidStack(Fluids.SULFURIC_ACID, 8_000)},
                new ItemStack[0],
                new FluidStack[]{new FluidStack(Fluids.POTASSIUM_CHLORIDE, 250), new FluidStack(Fluids.CALCIUM_CHLORIDE, 250)}));

        // :81
        RECIPES.add(new ChemPlantRecipe("chem.ethanol", 50, 100,
                new AStack[]{new ComparableStack(Items.SUGAR, 10)},
                new FluidStack[0],
                new ItemStack[0],
                new FluidStack(Fluids.ETHANOL, 1_000)));

        // :90
        RECIPES.add(new ChemPlantRecipe("chem.biofuel", 60, 100,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.BIOGAS, 1_500), new FluidStack(Fluids.ETHANOL, 250)},
                new ItemStack[0],
                new FluidStack(Fluids.BIOFUEL, 1_000)));

        // :94
        RECIPES.add(new ChemPlantRecipe("chem.reoil", 40, 100,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.SMEAR, 1_000)},
                new ItemStack[0],
                new FluidStack(Fluids.RECLAIMED, 800)));

        // :98
        RECIPES.add(new ChemPlantRecipe("chem.gasoline", 40, 100,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.NAPHTHA, 1_000)},
                new ItemStack[0],
                new FluidStack(Fluids.GASOLINE, 800)));

        // :117
        RECIPES.add(new ChemPlantRecipe("chem.cobble", 20, 100,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.WATER, 1_000), new FluidStack(Fluids.LAVA, 25)},
                new ItemStack[]{new ItemStack(Blocks.COBBLESTONE)},
                (FluidStack) null));

        // :120
        RECIPES.add(new ChemPlantRecipe("chem.stone", 60, 500,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.WATER, 1_000), new FluidStack(Fluids.LAVA, 25), new FluidStack(Fluids.AIR, 4_000)},
                new ItemStack[]{new ItemStack(Blocks.STONE)},
                (FluidStack) null));

        // :123
        RECIPES.add(new ChemPlantRecipe("chem.obsidian", 60, 500,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.WATER, 1_000), new FluidStack(Fluids.LAVA, 500), new FluidStack(Fluids.AIR, 4_000)},
                new ItemStack[]{new ItemStack(Blocks.OBSIDIAN)},
                (FluidStack) null));

        // :126
        RECIPES.add(new ChemPlantRecipe("chem.aggregate", 320, 500,
                new AStack[]{new ComparableStack(Blocks.COBBLESTONE, 16)},
                new FluidStack[0],
                new ItemStack[]{new ItemStack(Blocks.GRAVEL, 8), new ItemStack(Blocks.SAND, 8)},
                (FluidStack) null));

        // :129
        RECIPES.add(new ChemPlantRecipe("chem.concrete", 100, 100,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_CEMENT.get()), new ComparableStack(Blocks.GRAVEL, 8), new ComparableStack(Blocks.SAND, 8)},
                new FluidStack[]{new FluidStack(Fluids.WATER, 2_000)},
                new ItemStack[]{new ItemStack(block("concrete_smooth"), 16)},
                (FluidStack) null));

        // :134 ASBESTOS.ingot ×4 (non-LBSM)
        RECIPES.add(new ChemPlantRecipe("chem.concreteasbestos", 100, 100,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_CEMENT.get(), 4), new ComparableStack(IngotNuggetItems.INGOT_ASBESTOS.get(), 4), new ComparableStack(Blocks.SAND, 8)},
                new FluidStack[]{new FluidStack(Fluids.WATER, 2_000)},
                new ItemStack[]{new ItemStack(block("concrete_asbestos"), 16)},
                (FluidStack) null));

        // :139 FERRO.ingot
        RECIPES.add(new ChemPlantRecipe("chem.ducrete", 150, 100,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_CEMENT.get(), 4), new ComparableStack(IngotNuggetItems.INGOT_FERROURANIUM.get()), new ComparableStack(Blocks.SAND, 8)},
                new FluidStack[]{new FluidStack(Fluids.WATER, 2_000)},
                new ItemStack[]{new ItemStack(block("ducrete_smooth"), 8)},
                (FluidStack) null));

        // :144
        RECIPES.add(new ChemPlantRecipe("chem.liquidconk", 100, 100,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_CEMENT.get()), new ComparableStack(Blocks.GRAVEL, 8), new ComparableStack(Blocks.SAND, 8)},
                new FluidStack[]{new FluidStack(Fluids.WATER, 2_000)},
                new ItemStack[0],
                new FluidStack(Fluids.CONCRETE, 16_000)));

        // :149
        RECIPES.add(new ChemPlantRecipe("chem.asphalt", 100, 100,
                new AStack[]{new ComparableStack(Blocks.GRAVEL, 2), new ComparableStack(Blocks.SAND, 6)},
                new FluidStack[]{new FluidStack(Fluids.BITUMEN, 1_000)},
                new ItemStack[]{new ItemStack(block("asphalt"), 16)},
                (FluidStack) null));

        // :155 battery_pack LEAD → battery_lead_pack
        RECIPES.add(new ChemPlantRecipe("chem.batterylead", 100, 100,
                new AStack[]{new ComparableStack(PlateCrystalWasteItems.PLATE_STEEL.get(), 4), new ComparableStack(IngotNuggetItems.INGOT_LEAD.get(), 4)},
                new FluidStack[]{new FluidStack(Fluids.SULFURIC_ACID, 8_000)},
                new ItemStack[]{new ItemStack(item("battery_lead_pack"))},
                (FluidStack) null));

        // :160
        RECIPES.add(new ChemPlantRecipe("chem.batterylithium", 100, 1_000,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_LITHIUM.get(), 12), new ComparableStack(BilletPowderItems.POWDER_COBALT.get(), 8), new ComparableStack(IngotNuggetItems.INGOT_POLYMER.get(), 4)},
                new FluidStack[]{new FluidStack(Fluids.OXYGEN, 2_000)},
                new ItemStack[]{new ItemStack(item("battery_lithium_pack"))},
                (FluidStack) null));

        // :166
        RECIPES.add(new ChemPlantRecipe("chem.batterysodium", 100, 10_000,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_SODIUM.get(), 24), new ComparableStack(BilletPowderItems.POWDER_IRON.get(), 24), new ComparableStack(IngotNuggetItems.INGOT_PC.get(), 12)},
                new FluidStack[0],
                new ItemStack[]{new ItemStack(item("battery_sodium_pack"))},
                (FluidStack) null));

        // :171
        RECIPES.add(new ChemPlantRecipe("chem.batteryschrabidium", 100, 25_000,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_SCHRABIDIUM.get(), 24), new ComparableStack(PlateCrystalWasteItems.PLATE_BISMUTH.get(), 8)},
                new FluidStack[]{new FluidStack(Fluids.HELIUM4, 8_000)},
                new ItemStack[]{new ItemStack(item("battery_schrabidium_pack"))},
                (FluidStack) null));

        // :185 desh (non-LBSM: LIGHTOIL + MERCURY)
        RECIPES.add(new ChemPlantRecipe("chem.desh", 100, 100,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_DESH_MIX.get())},
                new FluidStack[]{new FluidStack(Fluids.LIGHTOIL, 200), new FluidStack(Fluids.MERCURY, 200)},
                new ItemStack[]{new ItemStack(IngotNuggetItems.INGOT_DESH.get())},
                (FluidStack) null));

        // :191
        RECIPES.add(new ChemPlantRecipe("chem.deshcracked", 100, 100,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_DESH_MIX.get())},
                new FluidStack[]{new FluidStack(Fluids.LIGHTOIL_CRACK, 500), new FluidStack(Fluids.MERCURY, 100)},
                new ItemStack[]{new ItemStack(IngotNuggetItems.INGOT_DESH.get())},
                (FluidStack) null));

        // :198
        RECIPES.add(new ChemPlantRecipe("chem.polymer", 100, 100,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_COAL.get(), 2), new ComparableStack(PlateCrystalWasteItems.CRYSTAL_FLUORITE.get())},
                new FluidStack[]{new FluidStack(Fluids.PETROLEUM, 1_000)},
                new ItemStack[]{new ItemStack(IngotNuggetItems.INGOT_POLYMER.get(), 4)},
                (FluidStack) null));

        // :203
        RECIPES.add(new ChemPlantRecipe("chem.bakelite", 100, 100,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.AROMATICS, 500), new FluidStack(Fluids.PETROLEUM, 500)},
                new ItemStack[]{new ItemStack(IngotNuggetItems.INGOT_BAKELITE.get())},
                (FluidStack) null));

        // :207 S.dust → crystal_sulfur
        RECIPES.add(new ChemPlantRecipe("chem.rubber", 100, 200,
                new AStack[]{new ComparableStack(PlateCrystalWasteItems.CRYSTAL_SULFUR.get())},
                new FluidStack[]{new FluidStack(Fluids.UNSATURATEDS, 500)},
                new ItemStack[]{new ItemStack(IngotNuggetItems.INGOT_RUBBER.get(), 2)},
                (FluidStack) null));

        // :212
        RECIPES.add(new ChemPlantRecipe("chem.hardplastic", 100, 1_000,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.XYLENE, 500), new FluidStack(Fluids.PHOSGENE, 500)},
                new ItemStack[]{new ItemStack(IngotNuggetItems.INGOT_PC.get())},
                (FluidStack) null));

        // :216
        RECIPES.add(new ChemPlantRecipe("chem.pvc", 100, 1_000,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_CADMIUM.get())},
                new FluidStack[]{new FluidStack(Fluids.UNSATURATEDS, 250), new FluidStack(Fluids.CHLORINE, 250)},
                new ItemStack[]{new ItemStack(IngotNuggetItems.INGOT_PVC.get(), 2)},
                (FluidStack) null));

        // :221
        RECIPES.add(new ChemPlantRecipe("chem.kevlar", 60, 300,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.AROMATICS, 200), new FluidStack(Fluids.NITRIC_ACID, 100), new FluidStack(Fluids.CHLORINE, 100)},
                new ItemStack[]{new ItemStack(PlateCrystalWasteItems.PLATE_KEVLAR.get(), 4)},
                (FluidStack) null));

        // :225 chocolate via registry id
        RECIPES.add(new ChemPlantRecipe("chem.meth", 60, 300,
                new AStack[]{new ComparableStack(Items.WHEAT), new ComparableStack(Items.COCOA_BEANS, 2)},
                new FluidStack[]{new FluidStack(Fluids.LUBRICANT, 400), new FluidStack(Fluids.PEROXIDE, 500)},
                new ItemStack[]{new ItemStack(item("chocolate"), 4)},
                (FluidStack) null));

        // :230
        RECIPES.add(new ChemPlantRecipe("chem.epearl", 100, 300,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_DIAMOND.get())},
                new FluidStack[]{new FluidStack(Fluids.XPJUICE, 500)},
                new ItemStack[0],
                new FluidStack(Fluids.ENDERJUICE, 100)));

        // :241 deco_steel / deco_rusty_steel
        RECIPES.add(new ChemPlantRecipe("chem.rustysteel", 40, 100,
                new AStack[]{new ComparableStack(block("deco_steel"), 8)},
                new FluidStack[]{new FluidStack(Fluids.WATER, 1_000)},
                new ItemStack[]{new ItemStack(block("deco_rusty_steel"), 8)},
                (FluidStack) null));

        // :255 / :259 arc_electrode GRAPHITE
        RECIPES.add(new ChemPlantRecipe("chem.oilelectrodes", 600, 100,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.HEATINGOIL, 4_000)},
                new ItemStack[]{new ItemStack(MachineItems.ARC_ELECTRODES.get(ItemArcElectrode.EnumElectrodeType.GRAPHITE).get())},
                (FluidStack) null));
        RECIPES.add(new ChemPlantRecipe("chem.lubeelectrodes", 600, 100,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.LUBRICANT, 8_000)},
                new ItemStack[]{new ItemStack(MachineItems.ARC_ELECTRODES.get(ItemArcElectrode.EnumElectrodeType.GRAPHITE).get())},
                (FluidStack) null));

        // :264
        RECIPES.add(new ChemPlantRecipe("chem.peroxide", 50, 100,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.WATER, 1_000)},
                new ItemStack[0],
                new FluidStack(Fluids.PEROXIDE, 1_000)));

        // :268
        RECIPES.add(new ChemPlantRecipe("chem.sulfuricacid", 50, 100,
                new AStack[]{new ComparableStack(PlateCrystalWasteItems.CRYSTAL_SULFUR.get())},
                new FluidStack[]{new FluidStack(Fluids.PEROXIDE, 1_000), new FluidStack(Fluids.WATER, 1_000)},
                new ItemStack[0],
                new FluidStack(Fluids.SULFURIC_ACID, 2_000)));

        // :273 KNO.dust → crystal_niter
        RECIPES.add(new ChemPlantRecipe("chem.nitricacid", 50, 100,
                new AStack[]{new ComparableStack(PlateCrystalWasteItems.CRYSTAL_NITER.get())},
                new FluidStack[]{new FluidStack(Fluids.SULFURIC_ACID, 500)},
                new ItemStack[0],
                new FluidStack(Fluids.NITRIC_ACID, 1_000)));

        // :278
        RECIPES.add(new ChemPlantRecipe("chem.birkeland", 200, 5_000,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.AIR, 8_000), new FluidStack(Fluids.WATER, 2_000)},
                new ItemStack[0],
                new FluidStack(Fluids.NITRIC_ACID, 1_000)));

        // :287
        RECIPES.add(new ChemPlantRecipe("chem.schrabidate", 150, 5_000,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_IRON.get())},
                new FluidStack[]{new FluidStack(Fluids.SCHRABIDIC, 250)},
                new ItemStack[]{new ItemStack(BilletPowderItems.POWDER_SCHRABIDATE.get())},
                (FluidStack) null));

        // :299
        RECIPES.add(new ChemPlantRecipe("chem.coltanpain", 120, 100,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_COLTAN.get()), new ComparableStack(PlateCrystalWasteItems.CRYSTAL_FLUORITE.get())},
                new FluidStack[]{new FluidStack(Fluids.GAS, 1_000), new FluidStack(Fluids.OXYGEN, 500)},
                new ItemStack[0],
                new FluidStack(Fluids.PAIN, 1_000)));

        // :334
        RECIPES.add(new ChemPlantRecipe("chem.c4", 100, 1_000,
                new AStack[]{new ComparableStack(PlateCrystalWasteItems.CRYSTAL_NITER.get())},
                new FluidStack[]{new FluidStack(Fluids.UNSATURATEDS, 500)},
                new ItemStack[]{new ItemStack(IngotNuggetItems.INGOT_C4.get(), 4)},
                (FluidStack) null));

        // :345 STEEL.bolt → steel_bolt (Mats autogen)
        RECIPES.add(new ChemPlantRecipe("chem.laminate", 20, 100,
                new AStack[]{new ComparableStack(Blocks.GLASS), new ComparableStack(item("steel_bolt"), 4)},
                new FluidStack[]{new FluidStack(Fluids.XYLENE, 50), new FluidStack(Fluids.PHOSGENE, 50)},
                new ItemStack[]{new ItemStack(block("reinforced_laminate"))},
                (FluidStack) null));

        // :350 part_generic GLASS_POLARIZED
        RECIPES.add(new ChemPlantRecipe("chem.polarized", 100, 500,
                new AStack[]{new ComparableStack(Blocks.GLASS_PANE)},
                new FluidStack[]{new FluidStack(Fluids.PETROLEUM, 1_000)},
                new ItemStack[]{new ItemStack(item("part_generic_glass_polarized"), 16)},
                (FluidStack) null));

        // :356 U.billet
        RECIPES.add(new ChemPlantRecipe("chem.yellowcake", 250, 500,
                new AStack[]{new ComparableStack(BilletPowderItems.BILLET_URANIUM.get(), 2), new ComparableStack(PlateCrystalWasteItems.CRYSTAL_SULFUR.get(), 2)},
                new FluidStack[]{new FluidStack(Fluids.PEROXIDE, 500)},
                new ItemStack[]{new ItemStack(BilletPowderItems.POWDER_YELLOWCAKE.get())},
                (FluidStack) null));

        // :367
        RECIPES.add(new ChemPlantRecipe("chem.puf6", 200, 500,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_PLUTONIUM.get()), new ComparableStack(PlateCrystalWasteItems.CRYSTAL_FLUORITE.get(), 3)},
                new FluidStack[]{new FluidStack(Fluids.WATER, 1_000)},
                new ItemStack[0],
                new FluidStack(Fluids.PUF6, 900)));

        // :372
        RECIPES.add(new ChemPlantRecipe("chem.sas3", 200, 5_000,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_SCHRABIDIUM.get()), new ComparableStack(PlateCrystalWasteItems.CRYSTAL_SULFUR.get(), 2)},
                new FluidStack[]{new FluidStack(Fluids.PEROXIDE, 2_000)},
                new ItemStack[0],
                new FluidStack(Fluids.SAS3, 1_000)));

        // :377
        RECIPES.add(new ChemPlantRecipe("chem.balefire", 100, 10_000,
                new AStack[]{new ComparableStack(NukeCasingItems.EGG_BALEFIRE_SHARD.get())},
                new FluidStack[]{new FluidStack(Fluids.KEROSENE, 6_000)},
                new ItemStack[]{new ItemStack(BilletPowderItems.POWDER_BALEFIRE.get())},
                new FluidStack(Fluids.BALEFIRE, 8_000)));

        // :383
        RECIPES.add(new ChemPlantRecipe("chem.dhc", 400, 500,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.DEUTERIUM, 500), new FluidStack(Fluids.REFORMGAS, 250), new FluidStack(Fluids.SYNGAS, 250)},
                new ItemStack[0],
                new FluidStack(Fluids.DHC, 500)));

        // :388
        RECIPES.add(new ChemPlantRecipe("chem.osmiridiumdeath", 240, 1_000,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_PALEOGENITE.get()), new ComparableStack(PlateCrystalWasteItems.CRYSTAL_FLUORITE.get(), 8), new ComparableStack(IngotNuggetItems.NUGGET_BISMUTH.get(), 4)},
                new FluidStack[]{new FluidStack(Fluids.PEROXIDE, 1_000)},
                new ItemStack[0],
                new FluidStack(Fluids.DEATH, 1_000)));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    /**
     * Up to 3 item in, 3 fluid in, 3 item out, 3 fluid out — CE {@code inputItemLimit}/{@code outputFluidLimit}.
     */
    public static final class ChemPlantRecipe {
        public final String name;
        public final int duration;
        public final long power;
        public final AStack[] inputItems;
        public final FluidStack[] inputFluids;
        public final ItemStack[] outputItems;
        public final FluidStack outputFluid;
        public final FluidStack[] outputFluids;

        public ChemPlantRecipe(String name, int duration, long power, AStack[] inputItems,
                                FluidStack[] inputFluids, ItemStack[] outputItems, FluidStack outputFluid) {
            this(name, duration, power, inputItems, inputFluids, outputItems,
                    outputFluid == null ? new FluidStack[0] : new FluidStack[]{outputFluid});
        }

        public ChemPlantRecipe(String name, int duration, long power, AStack[] inputItems,
                                FluidStack[] inputFluids, ItemStack[] outputItems, FluidStack[] outputFluids) {
            this.name = name;
            this.duration = duration;
            this.power = power;
            this.inputItems = inputItems == null ? new AStack[0] : inputItems;
            this.inputFluids = inputFluids == null ? new FluidStack[0] : inputFluids;
            this.outputItems = outputItems == null ? new ItemStack[0] : outputItems;
            this.outputFluids = outputFluids == null ? new FluidStack[0] : outputFluids;
            this.outputFluid = this.outputFluids.length > 0 ? this.outputFluids[0] : null;
        }
    }
}
