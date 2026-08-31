package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.NuclearTransmutationRecipes} (74 lines, read in
 * full upstream; see {@code docs/phase7/mrec_09_blastfurnace_misc.md}). Simplest of the four files
 * this task covers - CE itself keeps it as two parallel maps ({@code recipesOutput},
 * {@code recipesEnergy}) with no {@code SerializableRecipe}/JSON-loader inheritance, preserved here
 * as-is: a single {@code AStack} input key resolves to an {@link ItemStack} output plus a {@code long}
 * energy (HE) cost.
 * <p>
 * <b>All 3 of CE's recipes are ready</b> - every ingredient and output item already exists in this
 * port, per the research report's dependency check (independently re-confirmed here): this is the
 * single cheapest win of this task's four assigned files.
 * <p>
 * <b>Lazy registration</b>: see {@link CrystallizerRecipes#registerDefaults()}'s javadoc - the same
 * registry-not-populated-yet hazard applies here.
 * <p>
 * <b>Not yet built: the consuming machine/block entity.</b> The research report could not identify
 * which CE block/TE calls {@code NuclearTransmutationRecipes.getOutput()}/{@code getEnergy()} within
 * its own time budget (Open Questions #3) - not re-derived here either, out of this task's
 * recipe-data-only scope. This class is recipe data only, ready for whichever future pass identifies
 * and builds that consumer.
 */
public final class NuclearTransmutationRecipes {

    private static final Map<AStack, ItemStack> OUTPUTS = new LinkedHashMap<>();
    private static final Map<AStack, Long> ENERGY = new LinkedHashMap<>();

    private static boolean registered = false;

    private NuclearTransmutationRecipes() {
    }

    /** See class javadoc "Lazy registration". */
    public static synchronized void registerDefaults() {
        if (registered) return;
        registered = true;

        // #1 (CE line 25): crystal_uranium -> crystal_schraranium x1, 5,000,000 HE
        addRecipe(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_URANIUM.get()),
                new ItemStack(PlateCrystalWasteItems.CRYSTAL_SCHRARANIUM.get(), 1), 5_000_000L);

        // #2 (CE line 26): ingot_uranium -> ingot_schraranium x1, 5,000,000 HE
        addRecipe(new ComparableStack(IngotNuggetItems.INGOT_URANIUM.get()),
                new ItemStack(IngotNuggetItems.INGOT_SCHRARANIUM.get(), 1), 5_000_000L);

        // #3 (CE line 27): uranium_block -> schraranium_block, 50,000,000 HE
        addRecipe(new ComparableStack(hbmBlock("uranium_block")),
                new ItemStack(hbmBlock("schraranium_block"), 1), 50_000_000L);
    }

    /**
     * Resolves one of this port's own {@code MaterialBlockGenerator}-autogen blocks by registry
     * name - matches {@code CrystallizerRecipes#hbmBlock(String)}'s already-established lazy-lookup
     * pattern (see that method's own javadoc for the full safety reasoning). {@code uranium_block}/
     * {@code schraranium_block} have no hand-declared field to reference directly (both are
     * {@code Mats.MAT_URANIUM}/{@code MAT_SCHRARANIUM} {@code BLOCK}-autogen items, confirmed via
     * {@code MaterialBlockGenerator.java}'s {@code HAZARD_MATERIALS} set and {@code Mats.java}'s own
     * {@code setAutogen(..., BLOCK)} declarations for both materials).
     */
    private static Block hbmBlock(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }

    private static void addRecipe(AStack input, ItemStack output, long energy) {
        input.singulize();
        OUTPUTS.put(input, output);
        ENERGY.put(input, energy);
    }

    /** Matches CE's {@code getOutput(ItemStack)}: exact-item lookup, {@link ItemStack#EMPTY} on no match. */
    public static ItemStack getOutput(ItemStack stack) {
        registerDefaults();
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        ComparableStack key = new ComparableStack(stack).makeSingular();
        ItemStack out = OUTPUTS.get(key);
        return out == null ? ItemStack.EMPTY : out.copy();
    }

    /** Matches CE's {@code getEnergy(ItemStack)}: 0 on no match. */
    public static long getEnergy(ItemStack stack) {
        registerDefaults();
        if (stack == null || stack.isEmpty()) return 0L;
        ComparableStack key = new ComparableStack(stack).makeSingular();
        Long energy = ENERGY.get(key);
        return energy == null ? 0L : energy;
    }

    /**
     * Full-collection accessor, matching {@code CrystallizerRecipes#getAllRecipes()}'s established
     * JEI-enumeration precedent. Returns an unmodifiable view over {@link #OUTPUTS}; pair with
     * {@link #getEnergy(ItemStack)} for the matching energy cost of each key.
     */
    public static Map<AStack, ItemStack> getAllRecipes() {
        registerDefaults();
        return java.util.Collections.unmodifiableMap(OUTPUTS);
    }
}
