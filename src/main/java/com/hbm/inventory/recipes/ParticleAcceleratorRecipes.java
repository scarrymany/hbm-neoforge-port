package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.machine.Phase11ProcessItems;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * CE {@code ParticleAcceleratorRecipes.java:65-88}. Symmetric match, momentum is a minimum gate.
 * Skipped {@code :84-86} SBD.ingot() (no schrabidate INGOT autogen).
 */
public final class ParticleAcceleratorRecipes {

    public static final List<ParticleAcceleratorRecipe> RECIPES = new ArrayList<>();

    private static boolean registered = false;

    private ParticleAcceleratorRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // :65-66
        RECIPES.add(new ParticleAcceleratorRecipe(cs("particle_hydrogen"), cs("particle_copper"), 300, stk("particle_amat"), ItemStack.EMPTY));
        // :67-68
        RECIPES.add(new ParticleAcceleratorRecipe(cs("particle_amat"), cs("particle_amat"), 400, stk("particle_aschrab"), ItemStack.EMPTY));
        // :69-70
        RECIPES.add(new ParticleAcceleratorRecipe(cs("particle_aschrab"), cs("particle_aschrab"), 10_000, stk("particle_dark"), ItemStack.EMPTY));
        // :71-72
        RECIPES.add(new ParticleAcceleratorRecipe(cs("particle_hydrogen"), cs("particle_amat"), 2_500, stk("particle_muon"), ItemStack.EMPTY));
        // :73-74
        RECIPES.add(new ParticleAcceleratorRecipe(cs("particle_hydrogen"), cs("particle_lead"), 6_500, stk("particle_higgs"), ItemStack.EMPTY));
        // :75-76
        RECIPES.add(new ParticleAcceleratorRecipe(cs("particle_muon"), cs("particle_higgs"), 5_000, stk("particle_tachyon"), ItemStack.EMPTY));
        // :77-78
        RECIPES.add(new ParticleAcceleratorRecipe(cs("particle_muon"), cs("particle_dark"), 12_500, stk("particle_strange"), ItemStack.EMPTY));
        // :79-81
        RECIPES.add(new ParticleAcceleratorRecipe(cs("particle_strange"),
                new ComparableStack(BilletPowderItems.POWDER_MAGIC.get()), 12_500,
                stk("particle_sparkticle"), new ItemStack(Phase11ProcessItems.DUST.get())));
        // :82-83
        RECIPES.add(new ParticleAcceleratorRecipe(cs("particle_sparkticle"), cs("particle_higgs"), 70_000, stk("particle_digamma"), ItemStack.EMPTY));
        // :87-88 chicken → food nugget (already registered)
        RECIPES.add(new ParticleAcceleratorRecipe(
                new ComparableStack(Items.CHICKEN), new ComparableStack(Items.CHICKEN),
                100, stk("nugget"), stk("nugget")));
    }

    public static ParticleAcceleratorRecipe getOutput(ItemStack in1, ItemStack in2) {
        register();
        if (in1 == null || in1.isEmpty() || in2 == null || in2.isEmpty()) return null;
        for (ParticleAcceleratorRecipe recipe : RECIPES) {
            if (recipe.matchesRecipe(in1, in2)) return recipe;
        }
        return null;
    }

    private static ComparableStack cs(String id) {
        return new ComparableStack(item(id));
    }

    private static ItemStack stk(String id) {
        return new ItemStack(item(id));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    public static final class ParticleAcceleratorRecipe {
        public final AStack input1;
        public final AStack input2;
        public final int momentum;
        public final ItemStack output1;
        public final ItemStack output2;

        public ParticleAcceleratorRecipe(AStack input1, AStack input2, int momentum, ItemStack output1, ItemStack output2) {
            this.input1 = input1;
            this.input2 = input2;
            this.momentum = momentum;
            this.output1 = output1;
            this.output2 = output2 == null ? ItemStack.EMPTY : output2;
        }

        public boolean matchesRecipe(ItemStack in1, ItemStack in2) {
            return (input1.matchesRecipe(in1, true) && input2.matchesRecipe(in2, true))
                    || (input1.matchesRecipe(in2, true) && input2.matchesRecipe(in1, true));
        }
    }
}
