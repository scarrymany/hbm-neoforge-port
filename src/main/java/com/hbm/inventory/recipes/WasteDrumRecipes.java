package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.items.PlateCrystalWasteItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Recipe data for the Waste Drum, ported from CE's {@code com.hbm.inventory.recipes.
 * WasteDrumRecipes} ({@code docs/phase7/mrec_04_arcwelder_misc.md}, 88 lines read in full upstream)
 * - a flat {@code Map<ComparableStack, ItemStack>} identity/decay table: a "hot"/contaminated item
 * goes in, the same item's "cold"/decontaminated form comes out. No duration/power/chance concept
 * at all in CE - {@code TileEntityWasteDrum} is a passive 12-slot block that re-rolls a per-tick,
 * per-slot decay chance (scaled by adjacent water-source count) and, on a hit, looks up this table.
 * <p>
 * <b>Scope trim (documented, not silent): 16 of CE's 31 entries are ported here.</b>
 * <ul>
 *     <li><b>All 16 literal waste-decay pairs</b> (the ones below) are fully item-ready - both the
 *     {@code _hot} and cold forms of every one are already registered, one field for one field, by
 *     {@link PlateCrystalWasteItems}.</li>
 *     <li><b>CE's 15-entry {@code EnumPWRFuel} loop</b> ({@code pwr_fuel_hot_&lt;type&gt;} -&gt;
 *     {@code pwr_fuel_depleted_&lt;type&gt;}) is <em>not</em> ported: the input half
 *     ({@code pwr_fuel_hot_*}) is registered by {@code com.hbm.items.machine.PWRHotFuelItems}, but
 *     the output half ({@code pwr_fuel_depleted_*}) does not exist anywhere in this port - confirmed
 *     by grep, and {@code MachineItems.java}'s own comment names this exact gap as out of that
 *     class's scope. Add this loop once those 15 items land; the shape is otherwise identical to the
 *     16 pairs below.</li>
 *     <li>CE's {@code ItemRBMKRod} special case (any RBMK rod sitting in the drum cools via
 *     {@code rod.updateHeat()}/{@code provideHeat()} instead of a table lookup) is not a data-table
 *     entry in CE either - it belongs in the Waste Drum's own block entity tick method, not this
 *     recipe class, once that block entity exists.</li>
 * </ul>
 * <p>
 * <b>Not yet built: the Waste Drum block/block entity/GUI itself</b> (confirmed absent by the
 * research report). This class is recipe data only, ready for whichever future pass builds
 * {@code com.hbm.blockentity.machine.WasteDrumBlockEntity} to consume via {@link #getOutput}.
 */
public final class WasteDrumRecipes {

    public static final Map<ComparableStack, ItemStack> RECIPES = new LinkedHashMap<>();

    private static boolean registered = false;

    private WasteDrumRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        addRecipe(PlateCrystalWasteItems.WASTE_NATURAL_URANIUM_HOT, PlateCrystalWasteItems.WASTE_NATURAL_URANIUM);
        addRecipe(PlateCrystalWasteItems.WASTE_URANIUM_HOT, PlateCrystalWasteItems.WASTE_URANIUM);
        addRecipe(PlateCrystalWasteItems.WASTE_THORIUM_HOT, PlateCrystalWasteItems.WASTE_THORIUM);
        addRecipe(PlateCrystalWasteItems.WASTE_MOX_HOT, PlateCrystalWasteItems.WASTE_MOX);
        addRecipe(PlateCrystalWasteItems.WASTE_PLUTONIUM_HOT, PlateCrystalWasteItems.WASTE_PLUTONIUM);
        addRecipe(PlateCrystalWasteItems.WASTE_U233_HOT, PlateCrystalWasteItems.WASTE_U233);
        addRecipe(PlateCrystalWasteItems.WASTE_U235_HOT, PlateCrystalWasteItems.WASTE_U235);
        addRecipe(PlateCrystalWasteItems.WASTE_SCHRABIDIUM_HOT, PlateCrystalWasteItems.WASTE_SCHRABIDIUM);
        addRecipe(PlateCrystalWasteItems.WASTE_ZFB_MOX_HOT, PlateCrystalWasteItems.WASTE_ZFB_MOX);
        addRecipe(PlateCrystalWasteItems.WASTE_PLATE_U233_HOT, PlateCrystalWasteItems.WASTE_PLATE_U233);
        addRecipe(PlateCrystalWasteItems.WASTE_PLATE_U235_HOT, PlateCrystalWasteItems.WASTE_PLATE_U235);
        addRecipe(PlateCrystalWasteItems.WASTE_PLATE_MOX_HOT, PlateCrystalWasteItems.WASTE_PLATE_MOX);
        addRecipe(PlateCrystalWasteItems.WASTE_PLATE_PU239_HOT, PlateCrystalWasteItems.WASTE_PLATE_PU239);
        addRecipe(PlateCrystalWasteItems.WASTE_PLATE_SA326_HOT, PlateCrystalWasteItems.WASTE_PLATE_SA326);
        addRecipe(PlateCrystalWasteItems.WASTE_PLATE_RA226BE_HOT, PlateCrystalWasteItems.WASTE_PLATE_RA226BE);
        addRecipe(PlateCrystalWasteItems.WASTE_PLATE_PU238BE_HOT, PlateCrystalWasteItems.WASTE_PLATE_PU238BE);

        // Not ported: the 15-entry EnumPWRFuel loop (pwr_fuel_depleted_<type> output items do not
        // exist yet - see class javadoc).
    }

    /**
     * {@code hot} at count 1 -&gt; {@code cold} at count 1, matching CE's own
     * {@code addRecipe(new ComparableStack(item, 1, 1), new ItemStack(item))} shape (CE's metadata
     * 1 = "hot", metadata 0 = "cold" - this port already flattened both into separate items).
     */
    private static void addRecipe(DeferredItem<Item> hot, DeferredItem<Item> cold) {
        RECIPES.put(new ComparableStack(hot.get(), 1), new ItemStack(cold.get()));
    }

    /** Ported from CE's own {@code recipes.get(comp)} lookup used by {@code TileEntityWasteDrum}. */
    public static ItemStack getOutput(ItemStack input) {
        register();
        if (input == null || input.isEmpty()) return null;

        for (Map.Entry<ComparableStack, ItemStack> entry : RECIPES.entrySet()) {
            if (entry.getKey().matchesRecipe(input, true)) {
                return entry.getValue().copy();
            }
        }
        return null;
    }
}
