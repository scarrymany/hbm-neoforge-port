package com.hbm.inventory.recipes;

import com.hbm.blockentity.machine.fusion.FusionBreederBlockEntity;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.machine.Phase11ProcessItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * CE {@code FusionRecipes.registerDefaults} — 10 torus rows, numbers 1:1.
 */
public final class FusionRecipes {

    public static final GenericRecipes SET = new GenericRecipes();
    public static final FusionRecipes INSTANCE = new FusionRecipes();
    public final List<FusionRecipe> recipeOrderedList = new ArrayList<>();
    public long maxInput;

    private boolean registered;

    private FusionRecipes() {
    }

    public static void register() {
        INSTANCE.registerDefaults();
    }

    public FusionRecipe byName(String name) {
        return (FusionRecipe) SET.recipeNameMap.get(name);
    }

    private void registerDefaults() {
        if (registered) return;
        registered = true;

        long solenoid = 25_000;
        double breederCapacity = FusionBreederBlockEntity.CAPACITY;

        registerRow((FusionRecipe) new FusionRecipe("fus.dd").setInputEnergy(750_000).setOutputEnergy(1_000_000)
                .setOutputFlux(breederCapacity / 200).setRGB(1F, 0.2F, 0.2F).setNamed()
                .setIcon(new ItemStack(item("gas_full")))
                .setPower(solenoid).setDuration(100)
                .inputFluids(new FluidStack(Fluids.DEUTERIUM, 20))
                .outputFluids(new FluidStack(Fluids.HELIUM4, 1_000)));

        registerRow((FusionRecipe) new FusionRecipe("fus.do").setInputEnergy(250_000).setOutputEnergy(1_250_000)
                .setOutputFlux(breederCapacity / 200).setNamed()
                .setIcon(new ItemStack(item("gas_full")))
                .setPower(solenoid).setDuration(100)
                .inputFluids(new FluidStack(Fluids.DEUTERIUM, 10), new FluidStack(Fluids.OXYGEN, 10))
                .outputItems(new ItemStack(Phase11ProcessItems.PELLET_CHARGED.get())));

        registerRow((FusionRecipe) new FusionRecipe("fus.dt").setInputEnergy(750_000).setOutputEnergy(3_750_000)
                .setOutputFlux(breederCapacity / 100).setNamed()
                .setIcon(new ItemStack(item("gas_full")))
                .setPower(solenoid).setDuration(100)
                .inputFluids(new FluidStack(Fluids.DEUTERIUM, 10), new FluidStack(Fluids.TRITIUM, 10))
                .outputFluids(new FluidStack(Fluids.HELIUM4, 1_000)));

        registerRow((FusionRecipe) new FusionRecipe("fus.tcl").setInputEnergy(2_500_000).setOutputEnergy(6_250_000)
                .setOutputFlux(breederCapacity / 20).setRGB(0.8F, 0.6F, 0.4F).setNamed()
                .setIcon(new ItemStack(BilletPowderItems.POWDER_CHLOROPHYTE.get()))
                .setPower(solenoid).setDuration(100)
                .inputFluids(new FluidStack(Fluids.TRITIUM, 10), new FluidStack(Fluids.CHLORINE, 10))
                .outputItems(new ItemStack(BilletPowderItems.POWDER_CHLOROPHYTE.get())));

        registerRow((FusionRecipe) new FusionRecipe("fus.h3").setInputEnergy(500_000).setOutputEnergy(3_750_000)
                .setOutputFlux(0).setRGB(0.2F, 0.2F, 1F).setNamed()
                .setIcon(new ItemStack(item("gas_full")))
                .setPower(solenoid).setDuration(100)
                .inputFluids(new FluidStack(Fluids.HELIUM3, 20))
                .outputFluids(new FluidStack(Fluids.HELIUM4, 1_000)));

        registerRow((FusionRecipe) new FusionRecipe("fus.th4").setInputEnergy(875_000).setOutputEnergy(4_000_000)
                .setOutputFlux(breederCapacity / 20).setRGB(0.2F, 0.2F, 1F).setNamed()
                .setIcon(new ItemStack(item("gas_full")))
                .setPower(solenoid).setDuration(100)
                .inputFluids(new FluidStack(Fluids.TRITIUM, 10), new FluidStack(Fluids.HELIUM4, 10))
                .outputItems(new ItemStack(Phase11ProcessItems.PELLET_CHARGED.get())));

        registerRow((FusionRecipe) new FusionRecipe("fus.cl").setInputEnergy(3_750_000).setOutputEnergy(10_000_000)
                .setOutputFlux(breederCapacity / 10).setRGB(1F, 0.6F, 0.2F).setNamed()
                .setIcon(new ItemStack(BilletPowderItems.POWDER_CHLOROPHYTE.get()))
                .setPower(solenoid).setDuration(100)
                .inputFluids(new FluidStack(Fluids.CHLORINE, 20))
                .outputItems(new ItemStack(BilletPowderItems.POWDER_CHLOROPHYTE.get())));

        registerRow((FusionRecipe) new FusionRecipe("fus.dhc").setInputEnergy(10_000_000).setOutputEnergy(25_000_000)
                .setOutputFlux(breederCapacity / 5).setRGB(0.2F, 0.8F, 0.8F).setNamed()
                .setIcon(new ItemStack(item("fluid_icon")))
                .setPower(solenoid).setDuration(100)
                .inputFluids(new FluidStack(Fluids.DHC, 20))
                .outputItems(new ItemStack(BilletPowderItems.POWDER_CHLOROPHYTE.get())));

        registerRow((FusionRecipe) new FusionRecipe("fus.bf").setInputEnergy(1_000_000).setOutputEnergy(12_500_000)
                .setOutputFlux(breederCapacity / 5).setRGB(0.2F, 1F, 0.2F).setNamed()
                .setIcon(new ItemStack(item("fluid_icon")))
                .setPower(solenoid).setDuration(100)
                .inputFluids(new FluidStack(Fluids.BALEFIRE, 15), new FluidStack(Fluids.AMAT, 5))
                .outputItems(new ItemStack(item("powder_balefire"))));

        registerRow((FusionRecipe) new FusionRecipe("fus.stellar").setInputEnergy(10_000_000).setOutputEnergy(50_000_000)
                .setOutputFlux(breederCapacity).setRGB(1F, 0.4F, 0.1F).setNamed()
                .setIcon(new ItemStack(item("fluid_icon")))
                .setPower(solenoid).setDuration(100)
                .inputFluids(new FluidStack(Fluids.STELLAR_FLUX, 10))
                .outputItems(new ItemStack(BilletPowderItems.POWDER_GOLD.get())));

        maxInput = 0;
        for (FusionRecipe recipe : recipeOrderedList) {
            if (recipe.ignitionTemp > maxInput) maxInput = recipe.ignitionTemp;
        }
    }

    private void registerRow(FusionRecipe recipe) {
        SET.recipeNameMap.put(recipe.getInternalName(), recipe);
        recipeOrderedList.add(recipe);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse("hbm:" + id));
    }
}
