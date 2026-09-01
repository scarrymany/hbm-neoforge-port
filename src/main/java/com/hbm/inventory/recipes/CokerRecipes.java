package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Combustible;
import com.hbm.inventory.fluid.trait.FT_Flammable;
import com.hbm.main.MainRegistry;
import com.hbm.util.Tuple.Triplet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CE {@code CokerRecipes.java}:30-68. Auto-coke from fluid heat + 8 explicit rows.
 * Census: {@code recipes.put} at each logical entry (CE used {@code registerAuto}/{@code registerRecipe}).
 */
public final class CokerRecipes {

    public static final Map<FluidType, Triplet<Integer, ItemStack, FluidStack>> recipes = new LinkedHashMap<>();

    private static boolean registered = false;

    private CokerRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // CE CokerRecipes.java:32-55
        recipes.put(Fluids.HEAVYOIL, auto(Fluids.HEAVYOIL, Fluids.OIL_COKER));
        recipes.put(Fluids.HEAVYOIL_VACUUM, auto(Fluids.HEAVYOIL_VACUUM, Fluids.REFORMATE));
        recipes.put(Fluids.COALCREOSOTE, auto(Fluids.COALCREOSOTE, Fluids.NAPHTHA_COKER));
        recipes.put(Fluids.SMEAR, auto(Fluids.SMEAR, Fluids.OIL_COKER));
        recipes.put(Fluids.HEATINGOIL, auto(Fluids.HEATINGOIL, Fluids.OIL_COKER));
        recipes.put(Fluids.HEATINGOIL_VACUUM, auto(Fluids.HEATINGOIL_VACUUM, Fluids.OIL_COKER));
        recipes.put(Fluids.RECLAIMED, auto(Fluids.RECLAIMED, Fluids.NAPHTHA_COKER));
        recipes.put(Fluids.NAPHTHA, auto(Fluids.NAPHTHA, Fluids.NAPHTHA_COKER));
        recipes.put(Fluids.NAPHTHA_DS, auto(Fluids.NAPHTHA_DS, Fluids.NAPHTHA_COKER));
        recipes.put(Fluids.NAPHTHA_CRACK, auto(Fluids.NAPHTHA_CRACK, Fluids.NAPHTHA_COKER));
        recipes.put(Fluids.DIESEL, auto(Fluids.DIESEL, Fluids.NAPHTHA_COKER));
        recipes.put(Fluids.DIESEL_REFORM, auto(Fluids.DIESEL_REFORM, Fluids.NAPHTHA_COKER));
        recipes.put(Fluids.DIESEL_CRACK, auto(Fluids.DIESEL_CRACK, Fluids.GAS_COKER));
        recipes.put(Fluids.DIESEL_CRACK_REFORM, auto(Fluids.DIESEL_CRACK_REFORM, Fluids.GAS_COKER));
        recipes.put(Fluids.LIGHTOIL, auto(Fluids.LIGHTOIL, Fluids.GAS_COKER));
        recipes.put(Fluids.LIGHTOIL_DS, auto(Fluids.LIGHTOIL_DS, Fluids.GAS_COKER));
        recipes.put(Fluids.LIGHTOIL_CRACK, auto(Fluids.LIGHTOIL_CRACK, Fluids.GAS_COKER));
        recipes.put(Fluids.LIGHTOIL_VACUUM, auto(Fluids.LIGHTOIL_VACUUM, Fluids.GAS_COKER));
        recipes.put(Fluids.BIOFUEL, auto(Fluids.BIOFUEL, Fluids.GAS_COKER));
        recipes.put(Fluids.AROMATICS, auto(Fluids.AROMATICS, Fluids.GAS_COKER));
        recipes.put(Fluids.REFORMATE, auto(Fluids.REFORMATE, Fluids.GAS_COKER));
        recipes.put(Fluids.XYLENE, auto(Fluids.XYLENE, Fluids.GAS_COKER));
        recipes.put(Fluids.FISHOIL, auto(Fluids.FISHOIL, Fluids.MERCURY));
        recipes.put(Fluids.SUNFLOWEROIL, auto(Fluids.SUNFLOWEROIL, Fluids.GAS_COKER));

        // CE CokerRecipes.java:57
        recipes.put(Fluids.WOODOIL, sfAuto(Fluids.WOODOIL, 340_000L, new ItemStack(Items.CHARCOAL), Fluids.GAS_COKER));

        // CE CokerRecipes.java:59-67
        recipes.put(Fluids.WATZ, rec(4_000, stack("ingot_mud", 4), null));
        recipes.put(Fluids.REDMUD, rec(450, new ItemStack(Items.IRON_INGOT), new FluidStack(Fluids.MERCURY, 50)));
        recipes.put(Fluids.BITUMEN, rec(16_000, stack("coke_petroleum", 1), new FluidStack(Fluids.OIL_COKER, 1_600)));
        recipes.put(Fluids.LUBRICANT, rec(12_000, stack("coke_petroleum", 1), new FluidStack(Fluids.OIL_COKER, 1_200)));
        recipes.put(Fluids.CALCIUM_SOLUTION, rec(125, stack("powder_calcium", 1), new FluidStack(Fluids.SPENTSTEAM, 100)));
        recipes.put(Fluids.SOURGAS, rec(1_000, stack("sulfur", 1), new FluidStack(Fluids.GAS_COKER, 150)));
        recipes.put(Fluids.SLOP, rec(1_000, stack("powder_limestone", 1), new FluidStack(Fluids.COLLOID, 250)));
        recipes.put(Fluids.VITRIOL, rec(4_000, stack("powder_iron", 1), new FluidStack(Fluids.SULFURIC_ACID, 500)));
    }

    public static Triplet<Integer, ItemStack, FluidStack> getOutput(FluidType type) {
        register();
        return recipes.get(type);
    }

    private static Triplet<Integer, ItemStack, FluidStack> auto(FluidType fluid, FluidType byproduct) {
        return sfAuto(fluid, 820_000L, stack("coke_petroleum", 1), byproduct);
    }

    private static Triplet<Integer, ItemStack, FluidStack> sfAuto(FluidType fluid, long tuPerSF, ItemStack fuel, FluidType type) {
        long tuFlammable = fluid.hasTrait(FT_Flammable.class) ? fluid.getTrait(FT_Flammable.class).getHeatEnergy() : 0;
        long tuCombustible = fluid.hasTrait(FT_Combustible.class) ? fluid.getTrait(FT_Combustible.class).getCombustionEnergy() : 0;
        long tuPerBucket = Math.max(tuFlammable, tuCombustible);
        if (tuPerBucket <= 0) tuPerBucket = 1;

        int mB = (int) (tuPerSF * 1000L / tuPerBucket);
        if (mB > 10_000) mB -= (mB % 1000);
        else if (mB > 1_000) mB -= (mB % 100);
        else if (mB > 100) mB -= (mB % 10);

        FluidStack byproduct = type == null ? null : new FluidStack(type, Math.max(10, mB / 10));
        return rec(mB, fuel, byproduct);
    }

    private static Triplet<Integer, ItemStack, FluidStack> rec(int quantity, ItemStack output, FluidStack byproduct) {
        return new Triplet<>(quantity, output == null ? ItemStack.EMPTY : output, byproduct);
    }

    private static ItemStack stack(String id, int n) {
        Item i = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
        return i == Items.AIR ? ItemStack.EMPTY : new ItemStack(i, n);
    }
}
