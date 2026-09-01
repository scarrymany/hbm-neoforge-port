package com.hbm.inventory.recipes;

import com.hbm.items.special.BedrockOreGrade;
import com.hbm.items.special.BedrockOreItems;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.ItemBedrockOreBase;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * CE has no standalone slopper recipe class — {@code TileEntityMachineOreSlopper.java:149-197}
 * + JEI {@code OreSlopperHandler}. Census: {@code RECIPES.add} per {@link BedrockOreType}.
 */
public final class OreSlopperRecipes {

    public static final List<OreSlopperRecipe> RECIPES = new ArrayList<>();

    private static boolean registered = false;

    private OreSlopperRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        // CE TileEntityMachineOreSlopper.java:149-151 / :180-181 — one BASE stack per type
        RECIPES.add(slop(BedrockOreType.LIGHT_METAL));
        RECIPES.add(slop(BedrockOreType.HEAVY_METAL));
        RECIPES.add(slop(BedrockOreType.RARE_EARTH));
        RECIPES.add(slop(BedrockOreType.ACTINIDE));
        RECIPES.add(slop(BedrockOreType.NON_METAL));
        RECIPES.add(slop(BedrockOreType.CRYSTALLINE));
    }

    private static OreSlopperRecipe slop(BedrockOreType type) {
        return new OreSlopperRecipe(type, new ItemStack(BedrockOreItems.get(type, BedrockOreGrade.BASE).get()));
    }

    public static List<OreSlopperRecipe> getAll() {
        register();
        return RECIPES;
    }

    public static boolean isInput(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemBedrockOreBase;
    }

    public static final class OreSlopperRecipe {
        public final BedrockOreType type;
        public final ItemStack output;

        public OreSlopperRecipe(BedrockOreType type, ItemStack output) {
            this.type = type;
            this.output = output;
        }
    }
}
