package com.hbm.inventory.recipes;

import com.hbm.inventory.material.Mats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.CrucibleRecipe} (65 lines, read in full) as a
 * fresh, purpose-built class rather than extending this port's {@code recipes.loader.GenericRecipe}
 * compile-time stand-in - see {@link CrucibleRecipes}' own class javadoc for why (that stand-in's
 * own javadoc explicitly scopes this exact situation out and invites a real machine to build its own
 * shape instead). Fields mirror CE 1:1: a stable internal name, the raw {@link Mats.MaterialStack}
 * input/output pool (not discrete {@code ItemStack}s), a tick {@code frequency} the owning block
 * entity's steady-state conversion loop fires on, and a GUI icon.
 */
public final class CrucibleRecipe {

    private final String name;
    private final Mats.MaterialStack[] input;
    private final Mats.MaterialStack[] output;
    private final int frequency;
    private final ItemStack icon;

    public CrucibleRecipe(String name, int frequency, ItemStack icon, Mats.MaterialStack[] input, Mats.MaterialStack[] output) {
        this.name = name;
        this.frequency = Math.max(1, frequency);
        this.icon = icon;
        this.input = input;
        this.output = output;
    }

    public String getName() {
        return name;
    }

    public Mats.MaterialStack[] input() {
        return input;
    }

    public Mats.MaterialStack[] output() {
        return output;
    }

    public int frequency() {
        return frequency;
    }

    public ItemStack icon() {
        return icon;
    }

    /** Sum of every input material's amount - matches CE's {@code getInputAmount()}, used elsewhere as the recipe-stack-capacity ratio denominator. */
    public int getInputAmount() {
        int content = 0;
        for (Mats.MaterialStack stack : input) content += stack.amount;
        return content;
    }

    /** GUI/tooltip printout, ported from CE's {@code print()}/{@code input()}/{@code output()} (name + input list + output list). */
    public List<Component> print() {
        List<Component> lines = new ArrayList<>();
        lines.add(icon.isEmpty() ? Component.literal(name) : icon.getHoverName());

        lines.add(Component.translatable("gui.recipe.input").withStyle(ChatFormatting.BOLD));
        for (Mats.MaterialStack stack : input) {
            lines.add(Component.empty().append(stack.material.getName()).append(": ").append(Mats.formatAmount(stack.amount, false)));
        }

        lines.add(Component.translatable("gui.recipe.output").withStyle(ChatFormatting.BOLD));
        for (Mats.MaterialStack stack : output) {
            lines.add(Component.empty().append(stack.material.getName()).append(": ").append(Mats.formatAmount(stack.amount, false)));
        }

        return lines;
    }
}
