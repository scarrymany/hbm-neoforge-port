package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.BilletPowderItems;
import com.hbm.main.MainRegistry;
import com.hbm.util.WeightedRandom;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipe data for CE's {@code com.hbm.inventory.recipes.RockMillRecipes} - ported per
 * {@code docs/phase7/mrec_08_chemplant_misc.md}'s catalog and item/registry dependency check.
 * <p>
 * <b>Same {@code GenericRecipes<GenericRecipe>} base as {@code ChemicalPlantRecipes}</b> (confirmed
 * by direct source comparison - identical superclass, identical fluent builder), but its weighted
 * chance-output shape (CE's {@code IOutput}/{@code ChanceOutput}/{@code ChanceOutputMulti}, read from
 * {@code GenericRecipes.java} lines 223-339) has no equivalent anywhere in this port's existing
 * recipe infrastructure ({@code ChemPlantRecipe}/{@code ArcWelderRecipe}/{@code HbmSimpleRecipe} are
 * all deterministic-output) - {@link WeightedOutput}/{@link #pickOutput} below are a small, local,
 * from-scratch port of just that one piece (a single weighted pick is sufficient: every entry's
 * weights already sum to exactly 100 in CE's own data, i.e. literal percentages, and no entry passes
 * CE's optional 3-arg {@code ChanceOutput(stack, chance, weight)} - all use the 2-arg
 * {@code ChanceOutput(stack, weight)}, which defaults {@code chance=1F} - so the extra per-entry
 * {@code chance} float CE's shape carries is unused by every one of these 9 recipes and is not ported).
 * <p>
 * <b>No machine exists for this pass to wire into</b> (confirmed absent: no {@code MachineRockMill}
 * block/block-entity/menu/screen anywhere in this port) - this is recipe data only, following the
 * same "port the data now" precedent {@code ArcWelderRecipes} already established for a machine with
 * zero consuming block/BE. Building the Rock Mill itself (auto-recognition against this 9-entry table,
 * matching the Chemical Plant/Centrifuge convention, plus block/BE/menu/screen) is out of this
 * recipe-data task's scope - the research report calls it "the largest lift of the 4 files" this task
 * covers.
 * <p>
 * <b>7 of CE's 9 entries are ported</b>; 2 are not, both blocked by the exact same missing item -
 * {@code ModItems.dust} (a generic undifferentiated-dust item, confirmed absent anywhere in this
 * port - distinct from the {@link com.hbm.inventory.material.MaterialShapes#DUST} shape token):
 * <ul>
 *     <li><b>{@code rock.sand}</b> - {@code ModItems.dust} carries the dominant 90% weight share of
 *     this entry's 3-member output pool (the other 2 members, {@code powder_calcium} and
 *     {@code fluorite}, are individually portable - {@code fluorite} even has this port's established
 *     {@code CRYSTAL_FLUORITE} substitution). Dropping just the 90%-weight member while keeping the
 *     other 2 at their original 5%/5% weights would silently change CE's real drop odds (a rebalance,
 *     not a straight port) - not done. Left out entirely rather than guessed at.</li>
 *     <li><b>{@code rock.clay}</b> - {@code ModItems.dust} is a hard second input (not probabilistic
 *     here), so this entry cannot be ported at all without it.</li>
 * </ul>
 */
public final class RockMillRecipes {

    public static final List<RockMillRecipe> RECIPES = new ArrayList<>();

    private static boolean registered = false;

    private RockMillRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        int consumption = 25;
        int duraShort = 100;
        int duraLong = 200;

        RECIPES.add(new RockMillRecipe("rock.cobble", duraShort, consumption,
                new AStack[]{new ComparableStack(Blocks.COBBLESTONE.asItem(), 1)},
                new FluidStack(Fluids.WATER, 250),
                List.of(new WeightedOutput(new ItemStack(Blocks.GRAVEL), 95),
                        new WeightedOutput(new ItemStack(BilletPowderItems.POWDER_QUARTZ.get()), 5))));

        RECIPES.add(new RockMillRecipe("rock.gravel", duraShort, consumption,
                new AStack[]{new ComparableStack(Blocks.GRAVEL.asItem(), 1)},
                new FluidStack(Fluids.WATER, 250),
                List.of(new WeightedOutput(new ItemStack(Blocks.SAND), 75),
                        new WeightedOutput(new ItemStack(Items.FLINT), 20),
                        new WeightedOutput(new ItemStack(BilletPowderItems.POWDER_BORON.get()), 5))));

        // NOT PORTED: rock.sand (ModItems.dust carries the dominant 90% weight - see class javadoc).

        RECIPES.add(new RockMillRecipe("rock.netherrack", duraShort, consumption,
                new AStack[]{new ComparableStack(Blocks.NETHERRACK.asItem(), 1)},
                new FluidStack(Fluids.WATER, 250),
                List.of(new WeightedOutput(new ItemStack(Blocks.GRAVEL), 50),
                        new WeightedOutput(new ItemStack(Blocks.SOUL_SAND), 25),
                        new WeightedOutput(new ItemStack(Items.GLOWSTONE_DUST), 15),
                        new WeightedOutput(new ItemStack(BilletPowderItems.POWDER_QUARTZ.get()), 10))));

        RECIPES.add(new RockMillRecipe("rock.soulsand", duraShort, consumption,
                new AStack[]{new ComparableStack(Blocks.SOUL_SAND.asItem(), 1)},
                new FluidStack(Fluids.WATER, 250),
                List.of(new WeightedOutput(new ItemStack(Blocks.SAND), 50),
                        new WeightedOutput(new ItemStack(BilletPowderItems.POWDER_FIRE.get()), 25),
                        new WeightedOutput(new ItemStack(BilletPowderItems.POWDER_URANIUM.get()), 15),
                        new WeightedOutput(new ItemStack(Items.BLAZE_POWDER), 5),
                        new WeightedOutput(new ItemStack(Items.NETHER_WART), 5))));

        RECIPES.add(new RockMillRecipe("rock.schist", duraLong, consumption,
                new AStack[]{new ComparableStack(resolveItem("stone_gneiss"), 1)},
                new FluidStack(Fluids.WATER, 250),
                List.of(new WeightedOutput(new ItemStack(Blocks.GRAVEL), 50),
                        new WeightedOutput(new ItemStack(Blocks.SAND), 10),
                        new WeightedOutput(new ItemStack(BilletPowderItems.POWDER_LITHIUM.get()), 25),
                        new WeightedOutput(new ItemStack(BilletPowderItems.POWDER_NIOBIUM.get()), 5),
                        new WeightedOutput(new ItemStack(BilletPowderItems.POWDER_URANIUM.get()), 5),
                        new WeightedOutput(new ItemStack(BilletPowderItems.POWDER_GOLD.get()), 5))));

        // CE: HEMATITE.ore() == stone_resource[EnumStoneType.HEMATITE] (confirmed against CE's own
        // OreDictManager.java:527, "HEMATITE.ore(fromOne(stone_resource, EnumStoneType.HEMATITE))") -
        // not a separate ore-vein block, so this port's already-registered stone_resource_hematite
        // (GenericBlocks.java's EnumStoneType loop) is the direct, exact equivalent, not a substitute.
        RECIPES.add(new RockMillRecipe("rock.hematite", duraLong, consumption,
                new AStack[]{new ComparableStack(resolveItem("stone_resource_hematite"), 1)},
                new FluidStack(Fluids.WATER, 250),
                List.of(new WeightedOutput(new ItemStack(Blocks.GRAVEL), 65),
                        new WeightedOutput(new ItemStack(BilletPowderItems.POWDER_IRON.get()), 25),
                        new WeightedOutput(new ItemStack(BilletPowderItems.POWDER_TITANIUM.get()), 10))));

        // CE: BAUXITE.gem() == stone_resource[EnumStoneType.BAUXITE] (OreDictManager.java:530) - same
        // direct equivalence as rock.hematite above, not a substitute.
        RECIPES.add(new RockMillRecipe("rock.bauxite", duraLong, consumption,
                new AStack[]{new ComparableStack(resolveItem("stone_resource_bauxite"), 1)},
                new FluidStack(Fluids.WATER, 250),
                List.of(new WeightedOutput(new ItemStack(Blocks.GRAVEL), 25),
                        new WeightedOutput(new ItemStack(Items.CLAY_BALL), 25),
                        new WeightedOutput(new ItemStack(resolveItem("stone_resource_hematite")), 25),
                        new WeightedOutput(new ItemStack(resolveItem("ore_titanium")), 25))));

        // NOT PORTED: rock.clay (ModItems.dust hard second input - see class javadoc).
    }

    /** Same lazy-lookup-by-path idiom as {@code PUREXRecipes#resolveItem}/{@code FluidContainerRegistry#resolveItem}. */
    private static Item resolveItem(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }

    /**
     * Weighted-random selection from a recipe's output pool - CE's {@code ChanceOutputMulti}
     * simplified to a single pick (see class javadoc for why the extra per-entry {@code chance}
     * float is not needed here). Reuses {@link WeightedRandom}, this port's own existing shim for
     * CE's removed-from-modern-vanilla {@code net.minecraft.util.WeightedRandom}, rather than
     * hand-rolling a second weighted-pick algorithm.
     */
    public static ItemStack pickOutput(RockMillRecipe recipe, RandomSource random) {
        WeightedOutput picked = (WeightedOutput) WeightedRandom.getRandomItem(random, recipe.outputPool);
        return picked == null ? ItemStack.EMPTY : picked.stack.copy();
    }

    /** One weighted entry in a {@link RockMillRecipe}'s output pool - CE's {@code ChanceOutput(stack, weight)}. */
    public static final class WeightedOutput extends WeightedRandom.Item {
        public final ItemStack stack;

        public WeightedOutput(ItemStack stack, int weight) {
            super(weight);
            this.stack = stack;
        }
    }

    /**
     * Up to 3 {@link AStack} item inputs (CE's {@code inputItemLimit()}; every ported entry here uses
     * only 1), one {@link FluidStack} fluid input, a weighted {@link WeightedOutput} pool, duration
     * (ticks) + power (HE/tick), preserving CE's exact recipe data.
     */
    public static final class RockMillRecipe {
        public final String name;
        public final int duration;
        public final int power;
        public final AStack[] inputItems;
        public final FluidStack inputFluid;
        public final List<WeightedOutput> outputPool;

        public RockMillRecipe(String name, int duration, int power, AStack[] inputItems,
                               FluidStack inputFluid, List<WeightedOutput> outputPool) {
            this.name = name;
            this.duration = duration;
            this.power = power;
            this.inputItems = inputItems;
            this.inputFluid = inputFluid;
            this.outputPool = outputPool;
        }
    }
}
