package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Flammable;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.machine.Phase11ProcessItems;
import com.hbm.items.special.BedrockOreGrade;
import com.hbm.items.special.BedrockOreItems;
import com.hbm.items.special.BedrockOreType;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CE {@code PyroOvenRecipes.java}:36-124. Census: {@code RECIPES.add} at each logical row.
 * Solid-fuel family now unblocked ({@code solid_fuel} / {@code solid_fuel_bf} in Phase11ProcessItems).
 * Tar→soot skipped: {@code powder_ash} still unregistered.
 */
public final class PyroOvenRecipes {

    public static final List<PyroOvenRecipe> RECIPES = new ArrayList<>();

    private static boolean registered = false;

    private PyroOvenRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // CE PyroOvenRecipes.java:37-62 solid-fuel autogen. Explicit RECIPES.add so census sees each fluid.
        RECIPES.add(sf(Fluids.SMEAR));
        RECIPES.add(sf(Fluids.HEATINGOIL));
        RECIPES.add(sf(Fluids.HEATINGOIL_VACUUM));
        RECIPES.add(sf(Fluids.RECLAIMED));
        RECIPES.add(sf(Fluids.PETROIL));
        RECIPES.add(sf(Fluids.NAPHTHA));
        RECIPES.add(sf(Fluids.NAPHTHA_CRACK));
        RECIPES.add(sf(Fluids.DIESEL));
        RECIPES.add(sf(Fluids.DIESEL_REFORM));
        RECIPES.add(sf(Fluids.DIESEL_CRACK));
        RECIPES.add(sf(Fluids.DIESEL_CRACK_REFORM));
        RECIPES.add(sf(Fluids.LIGHTOIL));
        RECIPES.add(sf(Fluids.LIGHTOIL_CRACK));
        RECIPES.add(sf(Fluids.LIGHTOIL_VACUUM));
        RECIPES.add(sf(Fluids.KEROSENE));
        RECIPES.add(sf(Fluids.KEROSENE_REFORM));
        RECIPES.add(sf(Fluids.SOURGAS));
        RECIPES.add(sf(Fluids.REFORMGAS));
        RECIPES.add(sf(Fluids.SYNGAS));
        RECIPES.add(sf(Fluids.PETROLEUM));
        RECIPES.add(sf(Fluids.LPG));
        RECIPES.add(sf(Fluids.BIOFUEL));
        RECIPES.add(sf(Fluids.AROMATICS));
        RECIPES.add(sf(Fluids.UNSATURATEDS));
        RECIPES.add(sf(Fluids.REFORMATE));
        RECIPES.add(sf(Fluids.XYLENE));
        // CE PyroOvenRecipes.java:63 BALEFIRE → solid_fuel_bf
        RECIPES.add(sf(Fluids.BALEFIRE, 24_000_000L, item("solid_fuel_bf")));

        // CE PyroOvenRecipes.java:67-73 bedrock roast — 5 add sites, same as CE loop
        for (BedrockOreType type : BedrockOreType.VALUES) {
            RECIPES.add(roast(type, BedrockOreGrade.BASE, BedrockOreGrade.BASE_ROASTED));
            RECIPES.add(roast(type, BedrockOreGrade.PRIMARY, BedrockOreGrade.PRIMARY_ROASTED));
            RECIPES.add(roast(type, BedrockOreGrade.SULFURIC_BYPRODUCT, BedrockOreGrade.SULFURIC_ROASTED));
            RECIPES.add(roast(type, BedrockOreGrade.SOLVENT_BYPRODUCT, BedrockOreGrade.SOLVENT_ROASTED));
            RECIPES.add(roast(type, BedrockOreGrade.RAD_BYPRODUCT, BedrockOreGrade.RAD_ROASTED));
        }

        // CE PyroOvenRecipes.java:76-78 syngas from coal gem/dust
        RECIPES.add(new PyroOvenRecipe(100)
                .in(new FluidStack(Fluids.STEAM, 500)).in(new ComparableStack(Items.COAL))
                .out(new FluidStack(Fluids.SYNGAS, 1_000)));
        RECIPES.add(new PyroOvenRecipe(100)
                .in(new FluidStack(Fluids.STEAM, 500)).in(new ComparableStack(BilletPowderItems.POWDER_COAL.get()))
                .out(new FluidStack(Fluids.SYNGAS, 1_000)));
        // CE PyroOvenRecipes.java:83-85 tungsten carbide
        RECIPES.add(new PyroOvenRecipe(300)
                .in(new FluidStack(Fluids.SYNGAS, 2_000)).in(new ComparableStack(BilletPowderItems.POWDER_TUNGSTEN.get()))
                .out(new FluidStack(Fluids.SPENTSTEAM, 1_000)).out(new ItemStack(IngotNuggetItems.INGOT_TUNGSTEN_CARBIDE.get())));
        // CE PyroOvenRecipes.java:86-88 coke → syngas (ANY_COKE.gem)
        RECIPES.add(new PyroOvenRecipe(100)
                .in(new FluidStack(Fluids.STEAM, 250)).in(coke())
                .out(new FluidStack(Fluids.SYNGAS, 1_000)));
        // CE PyroOvenRecipes.java:90-92 biomass → syngas + charcoal
        RECIPES.add(new PyroOvenRecipe(100)
                .in(new ComparableStack(Phase11ProcessItems.BIOMASS.get(), 4))
                .out(new FluidStack(Fluids.SYNGAS, 1_000)).out(new ItemStack(Items.CHARCOAL)));
        // CE PyroOvenRecipes.java:98-100 / :101-103 heavyoil from coal
        RECIPES.add(new PyroOvenRecipe(100)
                .in(new FluidStack(Fluids.HYDROGEN, 500)).in(new ComparableStack(Items.COAL))
                .out(new FluidStack(Fluids.HEAVYOIL, 1_000)));
        RECIPES.add(new PyroOvenRecipe(100)
                .in(new FluidStack(Fluids.HYDROGEN, 500)).in(new ComparableStack(BilletPowderItems.POWDER_COAL.get()))
                .out(new FluidStack(Fluids.HEAVYOIL, 1_000)));
        // CE PyroOvenRecipes.java:104-106 coke → heavyoil
        RECIPES.add(new PyroOvenRecipe(100)
                .in(new FluidStack(Fluids.HYDROGEN, 250)).in(coke())
                .out(new FluidStack(Fluids.HEAVYOIL, 1_000)));
        // CE PyroOvenRecipes.java:108-110 / :111-113 coalgas from coal
        RECIPES.add(new PyroOvenRecipe(50)
                .in(new FluidStack(Fluids.HEAVYOIL, 500)).in(new ComparableStack(Items.COAL))
                .out(new FluidStack(Fluids.COALGAS, 1_000)));
        RECIPES.add(new PyroOvenRecipe(50)
                .in(new FluidStack(Fluids.HEAVYOIL, 500)).in(new ComparableStack(BilletPowderItems.POWDER_COAL.get()))
                .out(new FluidStack(Fluids.COALGAS, 1_000)));
        // CE PyroOvenRecipes.java:114-116 coke → coalgas
        RECIPES.add(new PyroOvenRecipe(50)
                .in(new FluidStack(Fluids.HEAVYOIL, 500)).in(coke())
                .out(new FluidStack(Fluids.COALGAS, 1_000)));
        // CE PyroOvenRecipes.java:118-120 refgas from coker gas
        RECIPES.add(new PyroOvenRecipe(60)
                .in(new FluidStack(Fluids.GAS_COKER, 4_000))
                .out(new FluidStack(Fluids.REFORMGAS, 100)));
        // CE PyroOvenRecipes.java:122-124 hydrogen + graphite from natgas
        RECIPES.add(new PyroOvenRecipe(60)
                .in(new FluidStack(Fluids.GAS, 12_000))
                .out(new FluidStack(Fluids.HYDROGEN, 8_000)).out(new ItemStack(IngotNuggetItems.INGOT_GRAPHITE.get())));
    }

    /** CE {@code registerSFAuto(fluid)} — 1_440_000 TU/SF, {@code solid_fuel}. */
    private static PyroOvenRecipe sf(FluidType fluid) {
        return sf(fluid, 1_440_000L, item("solid_fuel"));
    }

    /** CE {@code registerSFAuto(fluid, tuPerSF, fuel)} :130-142. */
    private static PyroOvenRecipe sf(FluidType fluid, long tuPerSF, Item fuel) {
        FT_Flammable trait = fluid.getTrait(FT_Flammable.class);
        long tuPerBucket = trait == null ? 1L : Math.max(1L, trait.getHeatEnergy());
        int mB = (int) (tuPerSF * 1000L * 0.5D / tuPerBucket);
        if (mB > 10_000) mB -= (mB % 1000);
        else if (mB > 1_000) mB -= (mB % 100);
        else if (mB > 100) mB -= (mB % 10);
        mB = Math.max(mB, 1);
        return new PyroOvenRecipe(60).in(new FluidStack(fluid, mB)).out(new ItemStack(fuel));
    }

    private static PyroOvenRecipe roast(BedrockOreType type, BedrockOreGrade raw, BedrockOreGrade roasted) {
        return new PyroOvenRecipe(10)
                .in(new ComparableStack(BedrockOreItems.get(type, raw).get()))
                .out(new FluidStack(Fluids.VITRIOL, 50))
                .out(new ItemStack(BedrockOreItems.get(type, roasted).get()));
    }

    private static OreDictStack coke() {
        return new OreDictStack(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "any_coke")), 1);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    public static List<PyroOvenRecipe> getAllRecipes() {
        register();
        return Collections.unmodifiableList(RECIPES);
    }

    public static class PyroOvenRecipe {
        public FluidStack inputFluid;
        public AStack inputItem;
        public FluidStack outputFluid;
        public ItemStack outputItem;
        public final int duration;

        public PyroOvenRecipe(int duration) {
            this.duration = duration;
        }

        public PyroOvenRecipe in(FluidStack stack) {
            this.inputFluid = stack;
            return this;
        }

        public PyroOvenRecipe in(AStack stack) {
            this.inputItem = stack;
            return this;
        }

        public PyroOvenRecipe out(FluidStack stack) {
            this.outputFluid = stack;
            return this;
        }

        public PyroOvenRecipe out(ItemStack stack) {
            this.outputItem = stack;
            return this;
        }
    }
}
