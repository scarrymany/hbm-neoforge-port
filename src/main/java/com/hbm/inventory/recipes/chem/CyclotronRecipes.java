package com.hbm.inventory.recipes.chem;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.util.Tuple.Pair;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.CyclotronRecipes} - particle-accelerator
 * transmutation: a catalyst item ("particle") + a target {@link AStack} together produce one output
 * item plus an antimatter mB yield ({@code docs/phase2/machines_chemical_isotope.md}'s Cyclotron
 * section).
 * <p>
 * <b>Item substitution</b> (documented): CE's catalyst items ({@code part_lithium},
 * {@code part_beryllium}, {@code part_carbon}, {@code part_copper}, {@code part_plutonium} -
 * dedicated "atom smasher particle" items) are not registered in this port yet. This class
 * substitutes the corresponding elemental powder from {@link BilletPowderItems} (e.g.
 * {@code powder_lithium} for {@code part_lithium}) as the catalyst item, keeping every
 * target/output/antimatter-yield number from CE exactly - <b>TODO(items-followup)</b>: swap in the
 * real {@code part_*} items once that items area registers them. Target ores are matched via
 * NeoForge's common {@code c:dusts/*} tags in place of CE's 1.12 OreDictionary {@code dust*} strings.
 * <p>
 * <b>Phase 7 (docs/phase7/mrec_07_shredder_misc.md) gap-closing pass</b> extended the original 11
 * entries (Li x4, Be x3, Cu x3, Pu x1) with the remaining ready-to-port CE entries (Li +8, Be +3,
 * Cu +7, Pu +1 - 19 more, 30 total of CE's 42). <b>Target-matching correction found while extending
 * this class</b>: {@code OreDictStack.ofCommonTag("dusts/" + name)} (the convention the original 11
 * entries use) only actually resolves to a real item when {@link com.hbm.items.datagen.ModItemTagProvider}'s
 * legacy pass matches one of {@code NTMMaterial.names}' aliases verbatim against a registered
 * {@code powder_<alias>} id - and several materials' canonical alias disagrees with this port's own
 * {@code powder_x} spelling (e.g. {@code Mats.MAT_LANTHANIUM} is {@code n("Lanthanum")}, so its tag
 * is {@code c:dusts/lanthanum}, but the actually-registered item is {@code powder_lanthanium} under
 * the alias-that-was-never-tried {@code "lanthanium"} - the tag would carry zero members). Rather
 * than re-verify every one of this pass's ~15 new target materials against that alias/spelling
 * mismatch individually, every new entry below targets the exact {@code powder_x}/{@code nugget_x}
 * item directly via {@code new ComparableStack(...)} instead of a common tag - strictly safer (100%
 * reachable by the exact item this port actually registers) at the cost of the oredict-style "any
 * tagged item" flexibility CE's original 1.12 OreDictionary had, which NeoForge's tag system was
 * already a deliberate simplification of for this whole class (see the paragraph above). The
 * original 11 entries' own {@code ofCommonTag} calls are left untouched - not verified or corrected
 * by this pass, out of scope for a gap-closing extension.
 * <p>
 * <b>Not ported, cited exactly:</b>
 * <ul>
 *     <li>Li: {@code dustPhosphorus -> sulfur} (both {@code powder_phosphorus} and plain
 *     {@code sulfur} are unregistered - the only Li entry this pass could not add).</li>
 *     <li>Li: {@code dustGold -> ingot_mercury} is CE's own real entry at this position, but
 *     {@code ingot_mercury} does not exist under any name in this port (confirmed via
 *     {@code IngotNuggetItems}' own source comment: CE's field is misleadingly named
 *     {@code ingot_mercury} but its real registry id was always {@code nugget_mercury} - there never
 *     was a real "ingot_mercury" item in CE either). The pre-existing entry at this slot (substituting
 *     {@code nugget_uranium}) is left untouched rather than "fixed" to something equally invented.</li>
 *     <li>Be: {@code dustNetherQuartz -> sulfur} (plain {@code sulfur} output is unregistered - the
 *     target, {@code powder_quartz}, is fine on its own, unlike Li's phosphorus entry above).</li>
 *     <li>Carbon (all 8): CE's catalyst {@code part_carbon} has no {@code powder_carbon}/
 *     {@code MAT_CARBON}-derived dust substitute registered anywhere in this port (confirmed:
 *     {@code Mats.MAT_CARBON} exists but only autogens {@code WIRE}/{@code BLOCK}, no {@code DUST},
 *     and no hand-registered {@code powder_carbon} item exists) - the entire chain is catalyst-blocked.</li>
 *     <li>Pu: {@code dustPhosphorus -> powder_tennessine} ({@code powder_phosphorus} unregistered,
 *     same as Li/Be above), {@code pellet_charged -> nugget_schrabidium} ({@code pellet_charged} not
 *     registered anywhere in this port - also CE's single highest-value entry in the whole file,
 *     amat 1000).</li>
 * </ul>
 */
public final class CyclotronRecipes {

    public static final Map<Pair<ComparableStack, AStack>, Pair<ItemStack, Integer>> RECIPES = new LinkedHashMap<>();

    private static boolean registered = false;

    private CyclotronRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // lithium catalyst chain (part_lithium -> powder_lithium substitute), amat yield 50 each - exact CE values
        makeRecipe(BilletPowderItems.POWDER_LITHIUM, OreDictStack.ofCommonTag("dusts/beryllium"), new ItemStack(BilletPowderItems.POWDER_BORON.get()), 50);
        makeRecipe(BilletPowderItems.POWDER_LITHIUM, OreDictStack.ofCommonTag("dusts/boron"), new ItemStack(BilletPowderItems.POWDER_COAL.get()), 50);
        makeRecipe(BilletPowderItems.POWDER_LITHIUM, OreDictStack.ofCommonTag("dusts/iron"), new ItemStack(BilletPowderItems.POWDER_COBALT.get()), 50);
        makeRecipe(BilletPowderItems.POWDER_LITHIUM, OreDictStack.ofCommonTag("dusts/gold"), new ItemStack(IngotNuggetItems.NUGGET_URANIUM.get()), 50);
        // Phase 7 additions (CyclotronRecipes.java:33,36,39,41-45 in CE) - not Phosphorus/Gold, see class javadoc.
        makeRecipe(BilletPowderItems.POWDER_LITHIUM, new ComparableStack(BilletPowderItems.POWDER_LITHIUM.get()), new ItemStack(BilletPowderItems.POWDER_BERYLLIUM.get()), 50);
        makeRecipe(BilletPowderItems.POWDER_LITHIUM, new ComparableStack(BilletPowderItems.POWDER_QUARTZ.get()), new ItemStack(BilletPowderItems.POWDER_FIRE.get()), 50);
        makeRecipe(BilletPowderItems.POWDER_LITHIUM, new ComparableStack(BilletPowderItems.POWDER_STRONTIUM.get()), new ItemStack(BilletPowderItems.POWDER_ZIRCONIUM.get()), 50);
        makeRecipe(BilletPowderItems.POWDER_LITHIUM, new ComparableStack(BilletPowderItems.POWDER_POLONIUM.get()), new ItemStack(BilletPowderItems.POWDER_ASTATINE.get()), 50);
        makeRecipe(BilletPowderItems.POWDER_LITHIUM, new ComparableStack(BilletPowderItems.POWDER_LANTHANIUM.get()), new ItemStack(BilletPowderItems.POWDER_CERIUM.get()), 50);
        makeRecipe(BilletPowderItems.POWDER_LITHIUM, new ComparableStack(BilletPowderItems.POWDER_ACTINIUM.get()), new ItemStack(BilletPowderItems.POWDER_THORIUM.get()), 50);
        makeRecipe(BilletPowderItems.POWDER_LITHIUM, new ComparableStack(BilletPowderItems.POWDER_URANIUM.get()), new ItemStack(BilletPowderItems.POWDER_NEPTUNIUM.get()), 50);
        makeRecipe(BilletPowderItems.POWDER_LITHIUM, new ComparableStack(BilletPowderItems.POWDER_NEPTUNIUM.get()), new ItemStack(BilletPowderItems.POWDER_PLUTONIUM.get()), 50);

        // beryllium catalyst chain (part_beryllium -> powder_beryllium substitute)
        makeRecipe(BilletPowderItems.POWDER_BERYLLIUM, OreDictStack.ofCommonTag("dusts/lithium"), new ItemStack(BilletPowderItems.POWDER_BORON.get()), 25);
        makeRecipe(BilletPowderItems.POWDER_BERYLLIUM, OreDictStack.ofCommonTag("dusts/titanium"), new ItemStack(BilletPowderItems.POWDER_IRON.get()), 25);
        makeRecipe(BilletPowderItems.POWDER_BERYLLIUM, OreDictStack.ofCommonTag("dusts/cobalt"), new ItemStack(BilletPowderItems.POWDER_COPPER.get()), 25);
        // Phase 7 additions (CyclotronRecipes.java:55-57 in CE) - not NetherQuartz/sulfur, see class javadoc.
        makeRecipe(BilletPowderItems.POWDER_BERYLLIUM, new ComparableStack(BilletPowderItems.POWDER_STRONTIUM.get()), new ItemStack(BilletPowderItems.POWDER_NIOBIUM.get()), 25);
        makeRecipe(BilletPowderItems.POWDER_BERYLLIUM, new ComparableStack(BilletPowderItems.POWDER_CERIUM.get()), new ItemStack(BilletPowderItems.POWDER_NEODYMIUM.get()), 25);
        makeRecipe(BilletPowderItems.POWDER_BERYLLIUM, new ComparableStack(BilletPowderItems.POWDER_THORIUM.get()), new ItemStack(BilletPowderItems.POWDER_URANIUM.get()), 25);

        // copper catalyst chain (part_copper -> powder_copper substitute)
        makeRecipe(BilletPowderItems.POWDER_COPPER, OreDictStack.ofCommonTag("dusts/beryllium"), new ItemStack(BilletPowderItems.POWDER_QUARTZ.get()), 15);
        makeRecipe(BilletPowderItems.POWDER_COPPER, OreDictStack.ofCommonTag("dusts/iron"), new ItemStack(BilletPowderItems.POWDER_NIOBIUM.get()), 15);
        makeRecipe(BilletPowderItems.POWDER_COPPER, OreDictStack.ofCommonTag("dusts/gold"), new ItemStack(BilletPowderItems.POWDER_URANIUM.get()), 15);
        // Phase 7 additions (CyclotronRecipes.java:77-84 in CE) - full remaining copper chain, all ready.
        makeRecipe(BilletPowderItems.POWDER_COPPER, new ComparableStack(BilletPowderItems.POWDER_COAL.get()), new ItemStack(BilletPowderItems.POWDER_BROMINE.get()), 15);
        makeRecipe(BilletPowderItems.POWDER_COPPER, new ComparableStack(BilletPowderItems.POWDER_TITANIUM.get()), new ItemStack(BilletPowderItems.POWDER_STRONTIUM.get()), 15);
        makeRecipe(BilletPowderItems.POWDER_COPPER, new ComparableStack(BilletPowderItems.POWDER_BROMINE.get()), new ItemStack(BilletPowderItems.POWDER_IODINE.get()), 15);
        makeRecipe(BilletPowderItems.POWDER_COPPER, new ComparableStack(BilletPowderItems.POWDER_STRONTIUM.get()), new ItemStack(BilletPowderItems.POWDER_NEODYMIUM.get()), 15);
        makeRecipe(BilletPowderItems.POWDER_COPPER, new ComparableStack(BilletPowderItems.POWDER_NIOBIUM.get()), new ItemStack(BilletPowderItems.POWDER_CAESIUM.get()), 15);
        makeRecipe(BilletPowderItems.POWDER_COPPER, new ComparableStack(BilletPowderItems.POWDER_IODINE.get()), new ItemStack(BilletPowderItems.POWDER_POLONIUM.get()), 15);
        makeRecipe(BilletPowderItems.POWDER_COPPER, new ComparableStack(BilletPowderItems.POWDER_CAESIUM.get()), new ItemStack(BilletPowderItems.POWDER_ACTINIUM.get()), 15);

        // plutonium catalyst chain (part_plutonium -> powder_plutonium substitute) - large amat yield, exact CE value
        makeRecipe(BilletPowderItems.POWDER_PLUTONIUM, OreDictStack.ofCommonTag("dusts/plutonium"), new ItemStack(BilletPowderItems.POWDER_TENNESSINE.get()), 100);
        // Phase 7 addition (CyclotronRecipes.java:93 in CE) - not dustPhosphorus/pellet_charged, see class javadoc.
        makeRecipe(BilletPowderItems.POWDER_PLUTONIUM, new ComparableStack(BilletPowderItems.POWDER_TENNESSINE.get()), new ItemStack(BilletPowderItems.POWDER_AUSTRALIUM.get()), 100);
    }

    private static void makeRecipe(net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.Item> part, AStack in, ItemStack out, int amat) {
        RECIPES.put(new Pair<>(new ComparableStack(part.get()), in), new Pair<>(out, amat));
    }

    /**
     * Ported from CE's {@code CyclotronRecipes.getOutput}: linear scan for the first recipe whose
     * catalyst and target both match, returning {@code {output ItemStack, antimatter mB Integer}}.
     */
    public static Object[] getOutput(ItemStack target, ItemStack catalyst) {
        if (target == null || target.isEmpty() || catalyst == null || catalyst.isEmpty()) return null;

        ComparableStack catalystKey = new ComparableStack(catalyst).makeSingular();

        for (Entry<Pair<ComparableStack, AStack>, Pair<ItemStack, Integer>> entry : RECIPES.entrySet()) {
            if (entry.getKey().getKey().isApplicable(catalystKey.getStack()) && entry.getKey().getValue().isApplicable(target)) {
                return new Object[]{entry.getValue().getKey().copy(), entry.getValue().getValue()};
            }
        }
        return null;
    }
}
