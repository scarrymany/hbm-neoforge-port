package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.machine.ItemDrive.EnumDriveType;
import com.hbm.items.machine.MachineItems;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * CE {@code SuperComputerRecipes.java}. Numbers verbatim from CE {@code registerDefaults}.
 * {@link #INSTANCE} is the GenericRecipes view for {@code GUIScreenRecipeSelector}.
 */
public final class SuperComputerRecipes {

    public static final GenericRecipes INSTANCE = new GenericRecipes();
    public static final List<SuperComputerRecipe> RECIPES = new ArrayList<>();

    private static boolean registered = false;

    public record ChanceOut(ItemStack stack, int weight) {
    }

    public static final class SuperComputerRecipe {
        public final String name;
        public final int duration;
        public final long power;
        public final AStack[] inputItems;
        public final FluidStack inputFluid;
        public final ChanceOut[] outputChoices;
        public final FluidStack outputFluid;

        public SuperComputerRecipe(String name, int duration, long power, AStack[] inputItems,
                                   FluidStack inputFluid, ChanceOut[] outputChoices, FluidStack outputFluid) {
            this.name = name;
            this.duration = duration;
            this.power = power;
            this.inputItems = inputItems == null ? new AStack[0] : inputItems;
            this.inputFluid = inputFluid;
            this.outputChoices = outputChoices == null ? new ChanceOut[0] : outputChoices;
            this.outputFluid = outputFluid;
        }
    }

    private SuperComputerRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        int min = 60 * 20;

        registerSimulation(EnumDriveType.FLASH_FLIGHTSIM, "com.flightcalc");
        registerSimulation(EnumDriveType.FLASH_PARTICLESIM, "com.particlecalc");

        registerTriplet("com.processflight", 30 * min, 15 * min, 5 * min,
                EnumDriveType.DISK_FLIGHTDATA, EnumDriveType.DISK_FLIGHTDATA_PROCESSED, EnumDriveType.DISK_BROKEN,
                99, 95, 90);
        registerTriplet("com.processorbit", 60 * min, 30 * min, 15 * min,
                EnumDriveType.DISK_ORBITDATA, EnumDriveType.DISK_ORBITDATA_PROCESSED, EnumDriveType.DISK_BROKEN,
                75, 65, 50);

        registerCopy("com.copyflightcalc", 15 * min, EnumDriveType.FLASH_FLIGHTSIM, EnumDriveType.FLASH_EMPTY, EnumDriveType.FLASH_BROKEN, 95);
        registerCopy("com.copyparticlecalc", 15 * min, EnumDriveType.FLASH_PARTICLESIM, EnumDriveType.FLASH_EMPTY, EnumDriveType.FLASH_BROKEN, 95);
        registerCopy("com.copyfligthdata", 15 * min, EnumDriveType.DISK_FLIGHTDATA_PROCESSED, EnumDriveType.DISK_EMPTY, EnumDriveType.DISK_BROKEN, 75);

        // CE SuperComputerRecipes.java:50-56 KEY_BLUE = dyeBlue
        RECIPES.add(new SuperComputerRecipe("com.blueprints", 15 * min, 50_000L,
                new AStack[]{new ComparableStack(Items.PAPER, 16), new ComparableStack(Items.BLUE_DYE, 16)},
                null,
                new ChanceOut[]{
                        new ChanceOut(new ItemStack(item("blueprint_folder_base")), 20),
                        new ChanceOut(new ItemStack(Items.PAPER, 16), 80)
                },
                null));
        // CE :57-63 CINNABAR.gem() → ModItems.cinnabar
        RECIPES.add(new SuperComputerRecipe("com.beigeprints", 15 * min, 50_000L,
                new AStack[]{new ComparableStack(Items.PAPER, 24), new ComparableStack(BilletPowderItems.CINNABAR.get(), 24)},
                null,
                new ChanceOut[]{
                        new ChanceOut(new ItemStack(item("blueprint_folder_discover")), 10),
                        new ChanceOut(new ItemStack(Items.PAPER, 24), 90)
                },
                null));
        // CE :64-71
        RECIPES.add(new SuperComputerRecipe("com.klaus", 60 * min, 5_000_000L,
                new AStack[]{
                        new ComparableStack(drive(EnumDriveType.DISK_EMPTY), 64),
                        new ComparableStack(drive(EnumDriveType.DISK_EMPTY), 64),
                        new ComparableStack(drive(EnumDriveType.DISK_EMPTY), 64)
                },
                new FluidStack(Fluids.WATER, 1_000_000),
                new ChanceOut[]{new ChanceOut(new ItemStack(drive(EnumDriveType.KLAUS)), 100)},
                new FluidStack(Fluids.SLOP, 1_000)));

        rebuildGeneric();
    }

    public static void rebuildGeneric() {
        INSTANCE.recipeNameMap.clear();
        INSTANCE.recipeOrderedList.clear();
        for (SuperComputerRecipe recipe : RECIPES) {
            GenericRecipe generic = new GenericRecipe(recipe.name)
                    .setDuration(recipe.duration)
                    .setPower(recipe.power);
            if (recipe.inputItems.length > 0) generic.inputItem = recipe.inputItems;
            if (recipe.inputFluid != null) generic.inputFluid = new FluidStack[]{recipe.inputFluid};
            if (recipe.outputChoices.length > 0) {
                ItemStack[] outs = new ItemStack[recipe.outputChoices.length];
                for (int i = 0; i < outs.length; i++) outs[i] = recipe.outputChoices[i].stack();
                generic.outputItems(outs);
            }
            if (recipe.outputFluid != null) generic.outputFluid = new FluidStack[]{recipe.outputFluid};
            INSTANCE.recipeNameMap.put(recipe.name, generic);
            INSTANCE.recipeOrderedList.add(generic);
        }
    }

    @Nullable
    public static SuperComputerRecipe byName(String name) {
        if (name == null || name.isEmpty() || "null".equals(name)) return null;
        for (SuperComputerRecipe recipe : RECIPES) {
            if (recipe.name.equals(name)) return recipe;
        }
        return null;
    }

    private static void registerSimulation(EnumDriveType type, String name) {
        int min = 60 * 20;
        registerTriplet(name, 15 * min, 5 * min / 2, 1 * min,
                EnumDriveType.FLASH_EMPTY, type, EnumDriveType.FLASH_BROKEN, 95, 50, 25);
    }

    private static void registerTriplet(String name, int time0, int time1, int time2,
                                        EnumDriveType input, EnumDriveType output, EnumDriveType broken,
                                        int chance0, int chance1, int chance2) {
        RECIPES.add(fluidRow(name + "_water", time0, 10_000L, input, output, broken, chance0,
                new FluidStack(Fluids.WATER, 16_000), new FluidStack(Fluids.SPENTSTEAM, 16_000)));
        RECIPES.add(fluidRow(name + "_pfm", time1, 10_000L, input, output, broken, chance1,
                new FluidStack(Fluids.PERFLUOROMETHYL_COLD, 16_000), new FluidStack(Fluids.PERFLUOROMETHYL, 16_000)));
        RECIPES.add(fluidRow(name + "_helium", time2, 10_000L, input, output, broken, chance2,
                new FluidStack(Fluids.HELIUM4, 16_000), null));
    }

    private static SuperComputerRecipe fluidRow(String name, int time, long power,
                                                EnumDriveType input, EnumDriveType output, EnumDriveType broken,
                                                int chance, FluidStack in, FluidStack out) {
        return new SuperComputerRecipe(name, time, power,
                new AStack[]{new ComparableStack(drive(input))},
                in,
                new ChanceOut[]{
                        new ChanceOut(new ItemStack(drive(output)), chance),
                        new ChanceOut(new ItemStack(drive(broken)), 100 - chance)
                },
                out);
    }

    private static void registerCopy(String name, int time, EnumDriveType full, EnumDriveType empty,
                                     EnumDriveType broken, int chance) {
        RECIPES.add(new SuperComputerRecipe(name, time, 10_000L,
                new AStack[]{new ComparableStack(drive(full)), new ComparableStack(drive(empty))},
                null,
                new ChanceOut[]{
                        new ChanceOut(new ItemStack(drive(full), 2), chance),
                        new ChanceOut(new ItemStack(drive(broken), 2), 100 - chance)
                },
                null));
    }

    private static Item drive(EnumDriveType type) {
        return MachineItems.DRIVES.get(type).get();
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }
}
