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
        for (BedrockOreType type : BedrockOreType.VALUES) {
            RECIPES.add(new OreSlopperRecipe(type, new ItemStack(BedrockOreItems.get(type, BedrockOreGrade.BASE).get())));
        }
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
