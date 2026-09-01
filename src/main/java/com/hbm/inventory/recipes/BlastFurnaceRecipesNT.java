package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * CE {@code BlastFurnaceRecipesNT.java}:33-82 — NT blast furnace (not the deprecated DiFurnace table).
 * Census: {@code .register(new } sites.
 */
public final class BlastFurnaceRecipesNT {

    public static final BlastFurnaceRecipesNT INSTANCE = new BlastFurnaceRecipesNT();
    public static final List<BlastFurnaceRecipe> RECIPES = new ArrayList<>();

    private static boolean registered = false;

    private BlastFurnaceRecipesNT() {
    }

    public void register(BlastFurnaceRecipe recipe) {
        RECIPES.add(recipe);
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        INSTANCE.registerDefaults();
    }

    public void registerDefaults() {
        // CE BlastFurnaceRecipesNT.java:33-82
        this.register(new BlastFurnaceRecipe("blast.steelFromIngot", 800,
                new AStack[]{tag("ingots/iron", 2), new OreDictStack(ItemTags.SAND)},
                stacks(stack("ingot_steel", 2), stack("slag_ingot", 1))));
        this.register(new BlastFurnaceRecipe("blast.steelFromDust", 800,
                new AStack[]{tag("dusts/iron", 2), new OreDictStack(ItemTags.SAND)},
                stacks(stack("ingot_steel", 2), stack("slag_ingot", 1))));
        this.register(new BlastFurnaceRecipe("blast.steelFromOre", 800,
                new AStack[]{tag("ores/iron", 1), new OreDictStack(ItemTags.SAND)},
                stacks(stack("ingot_steel", 2), stack("slag_ingot", 2))));
        this.register(new BlastFurnaceRecipe("blast.steelWithFlux", 1_200,
                new AStack[]{tag("ores/iron", 1), new ComparableStack(item("powder_flux"))},
                stacks(stack("ingot_steel", 3), stack("slag_ingot", 2))));

        this.register(new BlastFurnaceRecipe("blast.mingrade", 400,
                new AStack[]{tag("ingots/copper", 1), new ComparableStack(Items.REDSTONE)},
                stacks(stack("ingot_red_copper", 2))));
        this.register(new BlastFurnaceRecipe("blast.mingradeDust", 400,
                new AStack[]{tag("dusts/copper", 1), new ComparableStack(Items.REDSTONE)},
                stacks(stack("ingot_red_copper", 2))));
        this.register(new BlastFurnaceRecipe("blast.mingradeIngot", 400,
                new AStack[]{tag("ingots/copper", 1), new ComparableStack(item("ingot_redstone"))},
                stacks(stack("ingot_red_copper", 2))));
        this.register(new BlastFurnaceRecipe("blast.mingradeCursed", 400,
                new AStack[]{tag("dusts/copper", 1), new ComparableStack(item("ingot_redstone"))},
                stacks(stack("ingot_red_copper", 2))));
        this.register(new BlastFurnaceRecipe("blast.mingradeOre", 1_200,
                new AStack[]{tag("ores/copper", 1), new ComparableStack(Items.REDSTONE, 6)},
                stacks(stack("ingot_red_copper", 6), stack("slag_ingot", 1))));

        this.register(new BlastFurnaceRecipe("blast.meteorSword", 1_200,
                new AStack[]{new ComparableStack(item("ingot_cobalt")), new ComparableStack(item("meteorite_sword_hardened"))},
                stacks(stack("meteorite_sword_alloyed", 1))));

        this.register(new BlastFurnaceRecipe("blast.meteor", 600,
                new AStack[]{new ComparableStack(item("ingot_cobalt")), new ComparableStack(item("powder_meteorite"))},
                stacks(stack("ingot_meteorite", 1))));
        this.register(new BlastFurnaceRecipe("blast.starmetal", 600,
                new AStack[]{new ComparableStack(item("ingot_saturnite")), new ComparableStack(item("ingot_meteorite"))},
                stacks(stack("ingot_starmetal", 1))));

        this.register(new BlastFurnaceRecipe("blast.paa", 600,
                new AStack[]{new ComparableStack(Items.GOLD_INGOT), new ComparableStack(item("plate_mixed"))},
                stacks(stack("plate_paa", 1))));

        this.register(new BlastFurnaceRecipe("blast.firebrick", 800,
                new AStack[]{new ComparableStack(item("powder_aluminium")), new ComparableStack(Items.CLAY_BALL, 7)},
                stacks(stack("ingot_firebrick", 8))));
        this.register(new BlastFurnaceRecipe("blast.firebrickLimestone", 800,
                new AStack[]{new ComparableStack(item("powder_limestone")), new ComparableStack(Items.CLAY_BALL, 6)},
                stacks(stack("ingot_firebrick", 8))));

        for (int i = 0; i < RECIPES.size(); i++) {
            BlastFurnaceRecipe r = RECIPES.get(i);
            ItemStack[] kept = java.util.Arrays.stream(r.outputs)
                    .filter(s -> s != null && !s.isEmpty() && s.getItem() != Items.AIR)
                    .toArray(ItemStack[]::new);
            if (kept.length == 0) {
                RECIPES.remove(i--);
            } else if (kept.length != r.outputs.length) {
                RECIPES.set(i, new BlastFurnaceRecipe(r.name, r.duration, r.inputs, kept));
            }
        }
    }

    public BlastFurnaceRecipe getRecipe(ItemStack s0, ItemStack s1) {
        register();
        for (BlastFurnaceRecipe recipe : RECIPES) {
            if (recipe.inputs.length == 1) {
                if (!s0.isEmpty() && s1.isEmpty() && recipe.inputs[0].matchesRecipe(s0, false)) return recipe;
                if (s0.isEmpty() && !s1.isEmpty() && recipe.inputs[0].matchesRecipe(s1, false)) return recipe;
            }
            if (recipe.inputs.length == 2 && !s0.isEmpty() && !s1.isEmpty()) {
                if (recipe.inputs[0].matchesRecipe(s0, true) && recipe.inputs[1].matchesRecipe(s1, false)) return recipe;
                if (recipe.inputs[1].matchesRecipe(s0, true) && recipe.inputs[0].matchesRecipe(s1, false)) return recipe;
            }
        }
        return null;
    }

    public boolean isIngredient(ItemStack stack) {
        register();
        for (BlastFurnaceRecipe recipe : RECIPES) {
            for (AStack in : recipe.inputs) {
                if (in.matchesRecipe(stack, true)) return true;
            }
        }
        return false;
    }

    private static OreDictStack tag(String path, int n) {
        return new OreDictStack(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path)), n);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    private static ItemStack stack(String id, int n) {
        Item i = item(id);
        return i == Items.AIR ? ItemStack.EMPTY : new ItemStack(i, n);
    }

    private static ItemStack[] stacks(ItemStack... s) {
        return s;
    }

    public static final class BlastFurnaceRecipe {
        public final String name;
        public final int duration;
        public final AStack[] inputs;
        public final ItemStack[] outputs;

        public BlastFurnaceRecipe(String name, int duration, AStack[] inputs, ItemStack[] outputs) {
            this.name = name;
            this.duration = duration;
            this.inputs = inputs;
            this.outputs = outputs;
        }
    }
}
