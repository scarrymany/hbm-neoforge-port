package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.items.weapon.sedna.content.GunEnergyItems;
import com.hbm.items.weapon.sedna.content.GunHeavyItems;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.AmmoPressRecipes} (1242 lines, read in full by
 * the research report) - the largest single machine-recipe file in this port. CE's file is a flat,
 * hand-written list of 89 {@code recipes.add(new AmmoPressRecipe(output, AStack[9]))} calls (no
 * generating loop), each recipe's 9-slot array positionally describing a 3x3 GUI grid
 * ("left to right, top to bottom" per CE's own comment).
 * <p>
 * <b>Scope: only 6 of CE's 89 entries are ported here.</b> Per
 * {@code docs/phase7/mrec_01_ammopress_misc.md}'s item/registry dependency check, a single missing
 * item family - the 7-variant {@code casing} family ({@code casing_small}, {@code casing_large},
 * {@code casing_small_steel}, {@code casing_large_steel}, {@code casing_shotshell},
 * {@code casing_buckshot}, {@code casing_buckshot_advanced}) - gates the overwhelming majority of
 * the file, and {@code ANY_SMOKELESS}/{@code ANY_HIGHEXPLOSIVE} (unresolved ore-dict aliases, see
 * the report's open question #1) gate almost everything else. Per this task's ground rules, no
 * missing item is invented or stubbed here. The 6 entries below are the ones whose <i>every</i>
 * ingredient and output item is already registered in this port, found by checking every one of
 * CE's 89 recipes individually against the report's dependency-check findings (not just the "not
 * blocked by casing" set the report names, which is a superset - {@code TAU_URANIUM} and the
 * {@code FLAME_*} family are also casing-free but were not singled out by the report's own count):
 * <ul>
 *     <li>{@code COIL_TUNGSTEN}, {@code COIL_FERROURANIUM} - single-ingredient (pos4 only) coilgun ammo.</li>
 *     <li>{@code TAU_URANIUM} - lead plate (pos1) + uranium ingot (pos4) + lead plate (pos7).</li>
 *     <li>{@code FLAME_DIESEL}, {@code FLAME_GAS}, {@code FLAME_BALEFIRE} - steel plate (pos1) +
 *     fuel fluid (pos4) + steel plate (pos7). {@code FLAME_NAPALM} (the family's 4th member) is
 *     NOT ported - its {@code canister_napalm} ingredient is confirmed not registered.</li>
 * </ul>
 * See this class's own dependency-blocked list (not repeated here - see the implement-wave report
 * back to the coordinator) for exactly what blocks the other 83 entries.
 * <p>
 * <b>Shape divergence from CE, and why</b>: the report's "Recommended implementation shape" section
 * floats a full JSON-datagen {@code Recipe<Input>}/{@code RecipeSerializer} pair (structurally like
 * this port's own {@code AssemblerRecipe}), but flags this as a genuine open design question (#3),
 * not a settled recommendation. This class instead follows the lighter-weight, already-established
 * {@link RefineryRecipes}/{@link CrystallizerRecipes}/{@link MixerRecipes} precedent (a plain
 * hardcoded Java list) for two concrete reasons specific to where this port actually stands right
 * now: (1) <b>no Ammo Press machine block/block-entity/menu/screen exists in this port at all</b>
 * (confirmed by the report's own exhaustive grep) - building a full vanilla {@code Recipe}/
 * {@code RecipeSerializer}/JSON-datagen pipeline with no consumer to wire it into or test it against
 * is speculative infrastructure, not recipe porting; and (2) only 6 of CE's 89 entries are actually
 * ready today, which does not carry the weight of a new {@code RecipeType} registration. A future
 * pass building the real Ammo Press machine should re-evaluate this shape once casing/smokeless/
 * high-explosive items exist and the bulk of the file becomes portable - at that point CE's real
 * positional (not bag/multiset) matching semantics (report open question #3) become the important
 * design decision, preserved conceptually here by keeping every recipe's ingredient list a
 * fixed-length 9-slot array rather than flattening it to an unordered bag.
 * <p>
 * <b>Fluid-slot representation is an explicit, disclosed simplification.</b> CE's real 1.12 Ammo
 * Press has no fluid tank - a "fluid" ingredient (e.g. {@code FLAME_DIESEL}'s diesel requirement) is
 * actually an ore-dict-tagged <i>item</i> slot accepting any filled fluid-container item
 * ({@code OreDictStack(Fluids.DIESEL.getDict(1000))} in CE's source, matching any item registered
 * under that fluid's ore-dict key). This port has no equivalent "any filled container for fluid X"
 * item-tag convention yet ({@code FluidType} here has no {@code getDict}-style accessor, and this
 * port's own fluid-container items - {@code ItemCanister}/{@code ItemGasCanister} - store their
 * contents via data components, not per-fluid distinct registered items an ore-dict tag could
 * group). Rather than invent that matching convention speculatively, the 3 {@code FLAME_*} recipes
 * below represent their fuel slot as a plain {@link FluidStack} (matching {@link PyroOvenRecipes}'
 * shape) - a placeholder a future Ammo Press implementation should replace with whatever real
 * container-matching mechanic it ends up using once the machine itself is built.
 */
public final class AmmoPressRecipes {

    private static final List<AmmoPressRecipe> RECIPES = new ArrayList<>();

    private static boolean registered = false;

    private AmmoPressRecipes() {
    }

    /** See class javadoc. Idempotent, lazily populated on first real lookup - never called eagerly from a block/mod registration bootstrap (see {@link CrystallizerRecipes#registerDefaults()}'s javadoc for the registry-not-populated-yet hazard this avoids). */
    public static synchronized void registerDefaults() {
        if (registered) return;
        registered = true;

        // ---- FLAME family (CE: qty 1, pos1 = steel plate, pos4 = fuel, pos7 = steel plate) ----
        ComparableStack steelPlate = new ComparableStack(PlateCrystalWasteItems.PLATE_STEEL.get());

        register(new ItemStack(GunHeavyItems.FLAME_DIESEL.get(), 1),
                null, steelPlate, null, null, new FluidStack(Fluids.DIESEL, 1_000), null, null, steelPlate, null);
        register(new ItemStack(GunHeavyItems.FLAME_GAS.get(), 1),
                null, steelPlate, null, null, new FluidStack(Fluids.GAS, 1_000), null, null, steelPlate, null);
        register(new ItemStack(GunHeavyItems.FLAME_BALEFIRE.get(), 1),
                null, steelPlate, null, null, new FluidStack(Fluids.BALEFIRE, 1_000), null, null, steelPlate, null);

        // ---- TAU_URANIUM (CE: qty 16, pos1 = lead plate, pos4 = uranium ingot, pos7 = lead plate) ----
        ComparableStack leadPlate = new ComparableStack(PlateCrystalWasteItems.PLATE_LEAD.get());
        register(new ItemStack(GunEnergyItems.TAU_URANIUM.get(), 16),
                null, leadPlate, null, null, new ComparableStack(IngotNuggetItems.INGOT_U238.get()), null, null, leadPlate, null);

        // ---- COIL family (CE: qty 4, pos4 only) ----
        register(new ItemStack(GunEnergyItems.COIL_TUNGSTEN.get(), 4),
                null, null, null, null, new ComparableStack(IngotNuggetItems.INGOT_TUNGSTEN.get()), null, null, null, null);
        register(new ItemStack(GunEnergyItems.COIL_FERROURANIUM.get(), 4),
                null, null, null, null, new ComparableStack(IngotNuggetItems.INGOT_FERROURANIUM.get()), null, null, null, null);
    }

    /** {@code slots} must be exactly 9 entries, each {@code null}, an {@link AStack}, or a {@link FluidStack} - matching CE's fixed 3x3-grid positional shape. */
    private static void register(ItemStack output, Object... slots) {
        if (slots.length != 9) throw new IllegalArgumentException("AmmoPressRecipe requires exactly 9 slots, got " + slots.length);
        RECIPES.add(new AmmoPressRecipe(output, slots.clone()));
    }

    /** Full-collection accessor, matching {@link RefineryRecipes#getAllRefinery()}/{@link CrystallizerRecipes#getAllRecipes()}'s established shape for a future JEI category / machine block entity. */
    public static List<AmmoPressRecipe> getAllRecipes() {
        registerDefaults();
        return java.util.Collections.unmodifiableList(RECIPES);
    }

    public static class AmmoPressRecipe {
        public final ItemStack output;
        /** Exactly 9 entries, index = GUI slot (0 = top-left "coat", ... matching CE's row-major 3x3 layout). Each entry is {@code null} (empty slot), an {@link AStack}, or a {@link FluidStack} (see class javadoc "Fluid-slot representation"). */
        public final Object[] slots;

        public AmmoPressRecipe(ItemStack output, Object[] slots) {
            this.output = output;
            this.slots = slots;
        }
    }
}
