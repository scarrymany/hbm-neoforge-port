package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * CE {@code PUREXRecipes.java:66-517}. {@code RECIPES.add} so the census hits.
 * Skips a recipe if any I/O item is unregistered (BuiltInRegistries → AIR).
 * Skipped vs CE: chance-output {@code purex.thoriumsalt} (:362), ICF (:467, no
 * {@code icf_pellet_depleted}), vitrification (:477-486, no {@code sand_lead}),
 * naquadria-guarded watz (:443-465).
 */
public final class PUREXRecipes {

    public static final List<PUREXRecipe> RECIPES = new ArrayList<>();

    private static boolean registered = false;

    private PUREXRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        final int pilePower = 100;
        final int zirnoxPower = 1_000;
        final int platePower = 1_500;
        final int pwrPower = 2_500;
        final int watzPower = 10_000;

        // :66
        add("purex.uzh", 600, 1_000,
                stacks(in("billet_uranium_fuel", 1), in("billet_zirconium", 3)),
                fluids(new FluidStack(Fluids.NITRIC_ACID, 1_000), new FluidStack(Fluids.HYDROGEN, 4_000)),
                stacks(out("billet_uzh", 4)),
                null);
        // :72
        add("purex.flashgold", 600, 1_000,
                stacks(in("billet_au198", 1), in("pellet_charged", 1)),
                fluids(new FluidStack(Fluids.AMAT, 1_000)),
                stacks(out("billet_balefire_gold", 2)),
                null);
        // :78
        add("purex.flashlead", 600, 1_000,
                stacks(in("billet_pb209", 1), in("billet_balefire_gold", 1)),
                fluids(new FluidStack(Fluids.AMAT, 1_000)),
                stacks(out("billet_flashlead", 1)),
                null);

        // CP-1 :87
        add("purex.pilepu", 40, pilePower,
                stacks(in("pile_rod_plutonium", 1)),
                fluids(new FluidStack(Fluids.SULFURIC_ACID, 100)),
                stacks(out("billet_pu_mix", 2), out("billet_uranium", 1), out("plate_iron", 2)),
                null);
        add("purex.pilethorium", 40, pilePower,
                stacks(in("pile_rod_mk2_thorium_fuel", 1)),
                fluids(new FluidStack(Fluids.SULFURIC_ACID, 100)),
                stacks(out("billet_thorium_fuel", 2), out("billet_nuclear_waste", 1)),
                null);
        add("purex.pilepu239", 40, pilePower,
                stacks(in("pile_rod_pu239", 1)),
                fluids(new FluidStack(Fluids.SULFURIC_ACID, 100)),
                stacks(out("billet_pu239", 1), out("billet_pu_mix", 1), out("billet_uranium", 1), out("plate_iron", 2)),
                null);

        FluidStack[] kn = fluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250));

        // ZIRNOX :112
        add("purex.zirnoxnu", 100, zirnoxPower, stacks(in("waste_natural_uranium", 1)), kn,
                stacks(out("nugget_u238", 1), out("nugget_pu_mix", 2), out("nugget_pu239", 1), out("nuclear_waste_tiny", 2)), null);
        add("purex.zirnoxmeu", 100, zirnoxPower, stacks(in("waste_uranium", 1)), kn,
                stacks(out("nugget_pu_mix", 1), out("nugget_plutonium", 2), out("nugget_technetium", 1), out("nuclear_waste_tiny", 2)), null);
        add("purex.zirnoxthmeu", 100, zirnoxPower, stacks(in("waste_thorium", 1)), kn,
                stacks(out("nugget_u238", 1), out("nugget_th232", 1), out("nugget_u233", 2), out("nuclear_waste_tiny", 2)), null);
        add("purex.zirnoxmox", 100, zirnoxPower, stacks(in("waste_mox", 1)), kn,
                stacks(out("nugget_pu_mix", 1), out("nugget_technetium", 1), out("nugget_u238", 1), out("nuclear_waste_tiny", 3)), null);
        add("purex.zirnoxmep", 100, zirnoxPower, stacks(in("waste_plutonium", 1)), kn,
                stacks(out("nugget_pu_mix", 2), out("nugget_technetium", 1), out("nuclear_waste_tiny", 3)), null);
        add("purex.zirnoxheu233", 100, zirnoxPower, stacks(in("waste_u233", 1)), kn,
                stacks(out("nugget_u235", 1), out("nugget_neptunium", 1), out("nugget_technetium", 1), out("nuclear_waste_tiny", 3)), null);
        add("purex.zirnoxheu235", 100, zirnoxPower, stacks(in("waste_u235", 1)), kn,
                stacks(out("nugget_pu238", 1), out("nugget_neptunium", 1), out("nugget_technetium", 1), out("nuclear_waste_tiny", 3)), null);
        add("purex.zirnoxles", 100, zirnoxPower, stacks(in("waste_schrabidium", 1)), kn,
                stacks(out("nugget_beryllium", 2), out("nugget_pu239", 1), out("nuclear_waste_tiny", 3)), null);
        add("purex.zirnoxzfbmox", 100, zirnoxPower, stacks(in("waste_zfb_mox", 1)), kn,
                stacks(out("nugget_zirconium", 3), out("nugget_technetium", 1), out("nugget_pu_mix", 1), out("nuclear_waste_tiny", 1)), null);

        // Plate :187
        add("purex.platemox", 100, platePower, stacks(in("waste_plate_mox", 1)), kn,
                stacks(out("powder_sr90_tiny", 1), out("nugget_pu_mix", 3), out("powder_cs137_tiny", 1), out("nuclear_waste_tiny", 4)), null);
        add("purex.platepu238be", 100, platePower, stacks(in("waste_plate_pu238be", 1)), kn,
                stacks(out("nugget_beryllium", 1), out("nugget_pu238", 1), out("powder_coal_tiny", 2), out("nugget_lead", 2)), null);
        add("purex.platepu239", 100, platePower, stacks(in("waste_plate_pu239", 1)), kn,
                stacks(out("nugget_pu240", 2), out("nugget_technetium", 1), out("powder_cs137_tiny", 1), out("nuclear_waste_tiny", 5)), null);
        add("purex.platera226be", 100, platePower, stacks(in("waste_plate_ra226be", 1)), kn,
                stacks(out("nugget_beryllium", 2), out("nugget_polonium", 2), out("powder_coal_tiny", 1), out("nugget_lead", 1)), null);
        add("purex.platesa326", 100, platePower, stacks(in("waste_plate_sa326", 1)), kn,
                stacks(out("nugget_solinium", 1), out("powder_neodymium_tiny", 1), out("nugget_tantalium", 1), out("nuclear_waste_tiny", 6)), null);
        add("purex.plateu233", 100, platePower, stacks(in("waste_plate_u233", 1)), kn,
                stacks(out("nugget_u235", 1), out("powder_i131_tiny", 1), out("powder_sr90_tiny", 1), out("nuclear_waste_tiny", 6)), null);
        add("purex.plateu235", 100, platePower, stacks(in("waste_plate_u235", 1)), kn,
                stacks(out("nugget_neptunium", 1), out("nugget_pu238", 1), out("nugget_technetium", 1), out("nuclear_waste_tiny", 6)), null);

        // PWR :247
        add("purex.pwrmeu", 100, pwrPower, stacks(in("pwr_fuel_depleted_meu", 1)), kn,
                stacks(out("nugget_u238", 3), out("nugget_plutonium", 4), out("nugget_technetium", 2), out("nuclear_waste_tiny", 3)), null);
        add("purex.pwrheu233", 100, pwrPower, stacks(in("pwr_fuel_depleted_heu233", 1)), kn,
                stacks(out("nugget_u235", 3), out("nugget_pu238", 3), out("nugget_technetium", 1), out("nuclear_waste_tiny", 5)), null);
        add("purex.pwrheu235", 100, pwrPower, stacks(in("pwr_fuel_depleted_heu235", 1)), kn,
                stacks(out("nugget_neptunium", 3), out("nugget_pu238", 3), out("nugget_technetium", 1), out("nuclear_waste_tiny", 5)), null);
        add("purex.pwrmen", 100, pwrPower, stacks(in("pwr_fuel_depleted_men", 1)), kn,
                stacks(out("nugget_u238", 3), out("nugget_pu239", 4), out("nugget_technetium", 2), out("nuclear_waste_tiny", 3)), null);
        add("purex.pwrhen237", 100, pwrPower, stacks(in("pwr_fuel_depleted_hen237", 1)), kn,
                stacks(out("nugget_pu238", 2), out("nugget_pu239", 4), out("nugget_technetium", 1), out("nuclear_waste_tiny", 5)), null);
        add("purex.pwrmox", 100, pwrPower, stacks(in("pwr_fuel_depleted_mox", 1)), kn,
                stacks(out("nugget_u238", 3), out("nugget_pu240", 4), out("nugget_technetium", 2), out("nuclear_waste_tiny", 3)), null);
        add("purex.pwrmep", 100, pwrPower, stacks(in("pwr_fuel_depleted_mep", 1)), kn,
                stacks(out("nugget_lead", 2), out("nugget_pu_mix", 4), out("nugget_technetium", 2), out("nuclear_waste_tiny", 3)), null);
        add("purex.pwrhep239", 100, pwrPower, stacks(in("pwr_fuel_depleted_hep239", 1)), kn,
                stacks(out("nugget_pu_mix", 2), out("nugget_pu240", 4), out("nugget_technetium", 1), out("nuclear_waste_tiny", 5)), null);
        add("purex.pwrhep241", 100, pwrPower, stacks(in("pwr_fuel_depleted_hep241", 1)), kn,
                stacks(out("nugget_lead", 3), out("nugget_zirconium", 2), out("nugget_technetium", 1), out("nuclear_waste_tiny", 6)), null);
        add("purex.pwrmea", 100, pwrPower, stacks(in("pwr_fuel_depleted_mea", 1)), kn,
                stacks(out("nugget_lead", 3), out("nugget_zirconium", 2), out("nugget_technetium", 1), out("nuclear_waste_tiny", 6)), null);
        add("purex.pwrhea242", 100, pwrPower, stacks(in("pwr_fuel_depleted_hea242", 1)), kn,
                stacks(out("nugget_lead", 3), out("nugget_zirconium", 2), out("nugget_technetium", 1), out("nuclear_waste_tiny", 6)), null);
        add("purex.pwrhes326", 100, pwrPower, stacks(in("pwr_fuel_depleted_hes326", 1)), kn,
                stacks(out("nugget_solinium", 3), out("nugget_lead", 2), out("nugget_euphemium", 1), out("nuclear_waste_tiny", 6)), null);
        add("purex.pwrhes327", 100, pwrPower, stacks(in("pwr_fuel_depleted_hes327", 1)), kn,
                stacks(out("nugget_australium", 4), out("nugget_lead", 1), out("nugget_euphemium", 1), out("nuclear_waste_tiny", 6)), null);
        add("purex.pwrbfbam", 100, pwrPower, stacks(in("pwr_fuel_depleted_bfb_am_mix", 1)), kn,
                stacks(out("nugget_am_mix", 9), out("nugget_pu_mix", 2), out("nugget_bismuth", 6), out("nuclear_waste_tiny", 1)), null);
        add("purex.pwrbfpu241", 100, pwrPower, stacks(in("pwr_fuel_depleted_bfb_pu241", 1)), kn,
                stacks(out("nugget_pu241", 9), out("nugget_pu_mix", 2), out("nugget_bismuth", 6), out("nuclear_waste_tiny", 1)), null);

        // Watz :371
        FluidStack watzOut = new FluidStack(Fluids.WATZ, 1_000);
        add("purex.watzschrab", 60, watzPower, stacks(in("watz_pellet_depleted_schrabidium", 1)), kn,
                stacks(out("nugget_solinium", 15), out("nugget_euphemium", 3), out("nuclear_waste", 2)), watzOut);
        add("purex.watzhes", 60, watzPower, stacks(in("watz_pellet_depleted_hes", 1)), kn,
                stacks(out("nugget_solinium", 17), out("nugget_euphemium", 1), out("nuclear_waste", 2)), watzOut);
        add("purex.watzmes", 60, watzPower, stacks(in("watz_pellet_depleted_mes", 1)), kn,
                stacks(out("nugget_solinium", 12), out("nugget_tantalium", 6), out("nuclear_waste", 2)), watzOut);
        add("purex.watzles", 60, watzPower, stacks(in("watz_pellet_depleted_les", 1)), kn,
                stacks(out("nugget_solinium", 9), out("nugget_tantalium", 9), out("nuclear_waste", 2)), watzOut);
        add("purex.watzhen", 60, watzPower, stacks(in("watz_pellet_depleted_hen", 1)), kn,
                stacks(out("nugget_pu239", 12), out("nugget_technetium", 6), out("nuclear_waste", 2)), watzOut);
        add("purex.watzmeu", 60, watzPower, stacks(in("watz_pellet_depleted_meu", 1)), kn,
                stacks(out("nugget_pu239", 12), out("nugget_bismuth", 6), out("nuclear_waste", 2)), watzOut);
        add("purex.watzmep", 60, watzPower, stacks(in("watz_pellet_depleted_mep", 1)), kn,
                stacks(out("nugget_pu241", 12), out("nugget_bismuth", 6), out("nuclear_waste", 2)), watzOut);
        add("purex.watzlead", 60, watzPower, stacks(in("watz_pellet_depleted_lead", 1)), kn,
                stacks(out("nugget_lead", 6), out("nugget_bismuth", 12), out("nuclear_waste", 2)), watzOut);
        add("purex.watzboron", 60, watzPower, stacks(in("watz_pellet_depleted_boron", 1)), kn,
                stacks(out("powder_coal_tiny", 12), out("nugget_co60", 6), out("nuclear_waste", 2)), watzOut);
        add("purex.watzdu", 60, watzPower, stacks(in("watz_pellet_depleted_du", 1)), kn,
                stacks(out("nugget_polonium", 12), out("nugget_pu238", 6), out("nuclear_waste", 2)), watzOut);

        // Schrab :489
        add("purex.schraranium", 200, 1_000, stacks(in("ingot_schraranium", 1)),
                fluids(new FluidStack(Fluids.KEROSENE, 2_000), new FluidStack(Fluids.NITRIC_ACID, 1_000)),
                stacks(out("nugget_schrabidium", 3), out("nugget_uranium", 3), out("nugget_neptunium", 2)), null);
        FluidStack[] schrabIn = fluids(new FluidStack(Fluids.SOLVENT, 4_000), new FluidStack(Fluids.SCHRABIDIC, 500));
        add("purex.schrabzirnox", 200, 50_000, stacks(in("waste_plutonium", 1)), schrabIn,
                stacks(out("powder_schrabidium", 1), out("nugget_technetium", 3), out("nuclear_waste_tiny", 4)), null);
        add("purex.schrabpwr", 200, 50_000, stacks(in("pwr_fuel_depleted_mep", 1)), schrabIn,
                stacks(out("powder_schrabidium", 1), out("nugget_technetium", 3), out("nuclear_waste_tiny", 4)), null);
        add("purex.schrabmen", 200, 50_000, stacks(in("pwr_fuel_depleted_men", 1)), schrabIn,
                stacks(out("powder_schrabidium", 1), out("nugget_technetium", 3), out("nuclear_waste_tiny", 4)), null);
    }

    private static void add(String name, int duration, long power, AStack[] inItems, FluidStack[] inFluids,
                            ItemStack[] outItems, FluidStack outFluid) {
        if (inItems == null || outItems == null) return;
        for (AStack a : inItems) {
            if (a == null) return;
            if (a instanceof ComparableStack cs && (cs.toStack().isEmpty() || cs.toStack().is(Items.AIR))) return;
        }
        for (ItemStack s : outItems) {
            if (s == null || s.isEmpty() || s.is(Items.AIR)) return;
        }
        RECIPES.add(new PUREXRecipe(name, duration, power, inItems, inFluids, outItems, outFluid));
    }

    private static AStack in(String id, int n) {
        Item it = item(id);
        return it == Items.AIR ? null : new ComparableStack(it, n);
    }

    private static ItemStack out(String id, int n) {
        Item it = item(id);
        return it == Items.AIR ? ItemStack.EMPTY : new ItemStack(it, n);
    }

    private static AStack[] stacks(AStack... a) {
        return a;
    }

    private static ItemStack[] stacks(ItemStack... a) {
        return a;
    }

    private static FluidStack[] fluids(FluidStack... a) {
        return a;
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    @SuppressWarnings("unused")
    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    public static final class PUREXRecipe {
        public final String name;
        public final int duration;
        public final long power;
        public final AStack[] inputItems;
        public final FluidStack[] inputFluids;
        public final ItemStack[] outputItems;
        public final FluidStack outputFluid;

        public PUREXRecipe(String name, int duration, long power, AStack[] inputItems,
                           FluidStack[] inputFluids, ItemStack[] outputItems, FluidStack outputFluid) {
            this.name = name;
            this.duration = duration;
            this.power = power;
            this.inputItems = inputItems == null ? new AStack[0] : inputItems;
            this.inputFluids = inputFluids == null ? new FluidStack[0] : inputFluids;
            this.outputItems = outputItems == null ? new ItemStack[0] : outputItems;
            this.outputFluid = outputFluid;
        }
    }
}
