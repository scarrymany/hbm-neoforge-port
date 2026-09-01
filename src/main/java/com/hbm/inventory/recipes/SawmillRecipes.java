package com.hbm.inventory.recipes;

import com.hbm.items.BilletPowderItems;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CE {@code TileEntitySawmill.java}:279-324 / {@code getRecipes()} :327-337.
 * Census: {@code RECIPES.add} per CE JEI row. {@code powder_sawdust} byproduct is registered
 * (unlike the vanilla-craft ban on that id as a crafting-table result).
 */
public final class SawmillRecipes {

    public static final List<SawmillRecipe> RECIPES = new ArrayList<>();

    private static boolean registered = false;

    private SawmillRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        // CE TileEntitySawmill.java:327-334
        RECIPES.add(new SawmillRecipe(ItemTags.LOGS, new ItemStack(Items.OAK_PLANKS, 6), 0.5F));
        RECIPES.add(new SawmillRecipe(ItemTags.PLANKS, new ItemStack(Items.STICK, 6), 0.1F));
        RECIPES.add(new SawmillRecipe(null, new ItemStack(BilletPowderItems.POWDER_SAWDUST.get()), 0F)); // stickWood
        RECIPES.add(new SawmillRecipe(ItemTags.SAPLINGS, new ItemStack(Items.STICK, 1), 0.1F));
    }

    public static List<SawmillRecipe> getAll() {
        register();
        return RECIPES;
    }

    public static ItemStack getOutput(ItemStack input, Level level) {
        register();
        if (input == null || input.isEmpty()) return ItemStack.EMPTY;
        if (input.is(ItemTags.LOGS) && level != null) {
            ItemStack plank = plankFromLog(input, level);
            if (!plank.isEmpty()) return plank;
        }
        if (input.is(Items.STICK)) {
            return new ItemStack(BilletPowderItems.POWDER_SAWDUST.get());
        }
        for (SawmillRecipe rec : RECIPES) {
            if (rec.tag != null && input.is(rec.tag)) return rec.output.copy();
        }
        return ItemStack.EMPTY;
    }

    public static float sawdustChance(ItemStack input) {
        register();
        if (input == null || input.isEmpty()) return 0F;
        if (input.is(Items.STICK)) return 0F;
        for (SawmillRecipe rec : RECIPES) {
            if (rec.tag != null && input.is(rec.tag)) return rec.sawdustChance;
        }
        return 0F;
    }

    public static boolean isInput(ItemStack stack) {
        return !getOutput(stack, null).isEmpty() || (!stack.isEmpty() && (stack.is(ItemTags.LOGS) || stack.is(Items.STICK)));
    }

    private static ItemStack plankFromLog(ItemStack log, Level level) {
        CraftingInput input = CraftingInput.of(1, 1, List.of(log.copyWithCount(1)));
        Optional<RecipeHolder<CraftingRecipe>> found = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        if (found.isEmpty()) return new ItemStack(Items.OAK_PLANKS, 6);
        ItemStack out = found.get().value().assemble(input, level.registryAccess());
        if (out.isEmpty() || !out.is(ItemTags.PLANKS)) return new ItemStack(Items.OAK_PLANKS, 6);
        out = out.copy();
        out.setCount(Math.max(1, out.getCount() * 6 / 4));
        return out;
    }

    public static final class SawmillRecipe {
        public final TagKey<Item> tag;
        public final ItemStack output;
        public final float sawdustChance;

        public SawmillRecipe(TagKey<Item> tag, ItemStack output, float sawdustChance) {
            this.tag = tag;
            this.output = output;
            this.sawdustChance = sawdustChance;
        }
    }
}
