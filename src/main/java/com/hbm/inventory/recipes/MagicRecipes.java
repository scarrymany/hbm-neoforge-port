package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.bomb.NukeCasingItems;
import com.hbm.items.tool.ToolItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.MagicRecipes} (100 lines, read in full) - the
 * recipe list behind CE's held-item "Book of Shadows" / Lemegeton GUI ({@code ContainerBook}), not
 * a machine block. Matching is a custom, order-sensitive comparison over up to 4 <i>compacted</i>
 * (empty-slot-skipped) grid slots - {@link #matches(List)} below is CE's own {@code MagicRecipe
 * .matches(List<ComparableStack>)}, adapted to compare against {@link ItemStack}s directly
 * (this port's {@link AStack#isApplicable(ItemStack)} takes a raw stack, unlike CE's
 * {@code ComparableStack}-typed overload - see that method's own javadoc).
 * <p>
 * Kept as a plain hardcoded Java list, not a vanilla {@code Recipe<CraftingInput>}, for the same
 * reason the "Recommended implementation shape" section of
 * {@code docs/phase7/mrec_01_ammopress_misc.md} gives: CE's matching semantics (compacted,
 * order-sensitive, up to 4 slots, consumed by a held-item GUI rather than a crafting grid) don't map
 * onto vanilla crafting-table {@code Recipe} machinery, and 8 entries total is far too small to
 * justify a new {@code RecipeType} for.
 * <p>
 * <b>Scope: only 3 of CE's 8 entries are ported here</b> (recipes #1-3 in file order), per the
 * report's dependency check - the other 5 each need a missing ingredient/output item
 * ({@code ducttape}, {@code pellet_charged}, {@code gravel_diamond} block, {@code shimmer_handle},
 * {@code hadron_coil_mese}/{@code hadron_coil_chlorophyte} blocks, all confirmed absent). See the
 * implement-wave report back to the coordinator for the exact per-recipe blocker.
 * <p>
 * <b>Inert until a further, separate prerequisite lands</b> (documented, not silently ignored):
 * porting this class's <i>data</i> is cheap, but {@code items/tool/ItemBookLemegeton.java} (the
 * held-item consumer) is currently a bare tooltip-only stub with no {@code use()} override - no
 * {@code MenuProvider}/{@code Screen} pair for the book GUI exists yet in this port. That is a
 * genuinely separate, larger GUI-framework task (see the research report's "Recommended
 * implementation shape"), out of this recipe-porting task's scope - nothing here wires
 * {@link #registerDefaults()} into {@code ItemBookLemegeton} or any other consumer; a future GUI
 * pass should call {@link #getAllRecipes()}/{@link #matches(List)} once that GUI exists.
 * <p>
 * <b>{@code STEEL} ingot substitution</b>: CE keys recipe #3 on {@code new OreDictStack(STEEL.ingot())}
 * (any ore-dict-tagged steel ingot). This port has exactly one concrete steel-ingot item
 * ({@link IngotNuggetItems#INGOT_STEEL}) and no confirmed {@code c:ingots/steel} tag coverage for
 * it (the research report's open question #2 flags this port's two parallel, not-always-overlapping
 * material-shape naming/tagging conventions as a real risk) - a plain {@link ComparableStack} against
 * the concrete item is used instead, matching {@link CrystallizerRecipes}/{@link MixerRecipes}'s own
 * established preference for a concrete-item match over chasing down a tag whose coverage isn't
 * independently confirmed.
 */
public final class MagicRecipes {

    private static final List<MagicRecipe> RECIPES = new ArrayList<>();

    private static boolean registered = false;

    private MagicRecipes() {
    }

    /** See class javadoc. Idempotent, lazily populated on first real lookup (see {@link CrystallizerRecipes#registerDefaults()}'s javadoc for the registry-not-populated-yet hazard this avoids). */
    public static synchronized void registerDefaults() {
        if (registered) return;
        registered = true;

        // #1 (CE line 45): ingot_u238m2 <- {ingot_u238m2_elements, ingot_u238m2_arsenic, ingot_u238m2_vault} (CE's 3 damage-value variants, flattened to discrete items in this port)
        RECIPES.add(new MagicRecipe(new ItemStack(IngotNuggetItems.INGOT_U238M2.get()),
                new ComparableStack(IngotNuggetItems.INGOT_U238M2_ELEMENTS.get()),
                new ComparableStack(IngotNuggetItems.INGOT_U238M2_ARSENIC.get()),
                new ComparableStack(IngotNuggetItems.INGOT_U238M2_VAULT.get())));

        // #2 (CE line 46): rod_of_discord <- {ender_pearl, nugget_euphemium, blaze_rod}
        RECIPES.add(new MagicRecipe(new ItemStack(ToolItems.ROD_OF_DISCORD.get()),
                new ComparableStack(Items.ENDER_PEARL),
                new ComparableStack(IngotNuggetItems.NUGGET_EUPHEMIUM.get()),
                new ComparableStack(Items.BLAZE_ROD)));

        // #3 (CE line 47): balefire_and_steel <- {steel ingot (tag in CE, concrete item here - see class javadoc), egg_balefire_shard}
        RECIPES.add(new MagicRecipe(new ItemStack(ToolItems.BALEFIRE_AND_STEEL.get()),
                new ComparableStack(IngotNuggetItems.INGOT_STEEL.get()),
                new ComparableStack(NukeCasingItems.EGG_BALEFIRE_SHARD.get())));
    }

    /**
     * CE's {@code getRecipe(InventoryCrafting)}, adapted: {@code slots} must already be compacted
     * (empty slots removed, order preserved) by the caller, matching CE's own
     * {@code MagicRecipes.getRecipe} pre-processing loop. Returns {@link ItemStack#EMPTY} on no
     * match, same as CE.
     */
    public static ItemStack getRecipe(List<ItemStack> slots) {
        registerDefaults();
        for (MagicRecipe recipe : RECIPES) {
            if (recipe.matches(slots)) return recipe.getResult();
        }
        return ItemStack.EMPTY;
    }

    /** Full-collection accessor, matching {@link RefineryRecipes#getAllRefinery()}/{@link CrystallizerRecipes#getAllRecipes()}'s established shape for a future GUI/JEI consumer. */
    public static List<MagicRecipe> getAllRecipes() {
        registerDefaults();
        return java.util.Collections.unmodifiableList(RECIPES);
    }

    public static class MagicRecipe {
        public final List<AStack> in;
        public final ItemStack out;

        public MagicRecipe(ItemStack out, AStack... in) {
            this.out = out;
            this.in = List.of(in);
        }

        /** CE's {@code MagicRecipe.matches(List<ComparableStack>)} - order-sensitive, exact slot count. */
        public boolean matches(List<ItemStack> slots) {
            if (slots.size() != in.size()) return false;
            for (int i = 0; i < in.size(); i++) {
                if (!in.get(i).isApplicable(slots.get(i))) return false;
            }
            return true;
        }

        public ItemStack getResult() {
            return out.copy();
        }
    }
}
