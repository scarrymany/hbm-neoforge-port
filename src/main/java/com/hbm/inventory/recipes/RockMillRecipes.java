package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * CE {@code RockMillRecipes.java}:39-121. Weighted crush table + clay reconstitution.
 * Census: {@code .register(new } sites.
 */
public final class RockMillRecipes {

    public static final RockMillRecipes INSTANCE = new RockMillRecipes();
    public static final List<RockMillRecipe> RECIPES = new ArrayList<>();

    private static boolean registered = false;

    private RockMillRecipes() {
    }

    public void register(RockMillRecipe recipe) {
        RECIPES.add(recipe);
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        INSTANCE.registerDefaults();
    }

    public void registerDefaults() {
        int consumption = 25;
        int duraShort = 100;
        int duraLong = 200;

        // CE RockMillRecipes.java:39-121
        this.register(new RockMillRecipe("rock.cobble", duraShort, consumption,
                new AStack[]{new OreDictStack(ItemTags.STONE_CRAFTING_MATERIALS)},
                new FluidStack(Fluids.WATER, 250),
                chance(stack(Blocks.GRAVEL.asItem(), 1), 95, stack(item("powder_quartz"), 1), 5)));

        this.register(new RockMillRecipe("rock.gravel", duraShort, consumption,
                new AStack[]{new ComparableStack(Blocks.GRAVEL)},
                new FluidStack(Fluids.WATER, 250),
                chance(stack(Blocks.SAND.asItem(), 1), 75, stack(Items.FLINT, 1), 20, stack(item("powder_boron"), 1), 5)));

        this.register(new RockMillRecipe("rock.sand", duraShort, consumption,
                new AStack[]{new OreDictStack(ItemTags.SAND)},
                new FluidStack(Fluids.WATER, 250),
                chance(stack(item("dust"), 1), 90, stack(item("powder_calcium"), 1), 5, stack(item("fluorite"), 1), 5)));

        this.register(new RockMillRecipe("rock.netherrack", duraShort, consumption,
                new AStack[]{new ComparableStack(Blocks.NETHERRACK)},
                new FluidStack(Fluids.WATER, 250),
                chance(stack(Blocks.GRAVEL.asItem(), 1), 50, stack(Blocks.SOUL_SAND.asItem(), 1), 25,
                        stack(Items.GLOWSTONE_DUST, 1), 15, stack(item("powder_quartz"), 1), 10)));

        this.register(new RockMillRecipe("rock.soulsand", duraShort, consumption,
                new AStack[]{new ComparableStack(Blocks.SOUL_SAND)},
                new FluidStack(Fluids.WATER, 250),
                chance(stack(Blocks.SAND.asItem(), 1), 50, stack(item("powder_fire"), 1), 25,
                        stack(item("powder_uranium"), 1), 15, stack(Items.BLAZE_POWDER, 1), 5, stack(Items.NETHER_WART, 1), 5)));

        this.register(new RockMillRecipe("rock.schist", duraLong, consumption,
                new AStack[]{new ComparableStack(item("stone_gneiss"))},
                new FluidStack(Fluids.WATER, 250),
                chance(stack(Blocks.GRAVEL.asItem(), 1), 50, stack(Blocks.SAND.asItem(), 1), 10,
                        stack(item("powder_lithium"), 1), 25, stack(item("powder_niobium"), 1), 5,
                        stack(item("powder_uranium"), 1), 5, stack(item("powder_gold"), 1), 5)));

        this.register(new RockMillRecipe("rock.hematite", duraLong, consumption,
                new AStack[]{new ComparableStack(item("stone_resource_hematite"))},
                new FluidStack(Fluids.WATER, 250),
                chance(stack(Blocks.GRAVEL.asItem(), 1), 65, stack(item("powder_iron"), 1), 25, stack(item("powder_titanium"), 1), 10)));

        this.register(new RockMillRecipe("rock.bauxite", duraLong, consumption,
                new AStack[]{new ComparableStack(item("stone_resource_bauxite"))},
                new FluidStack(Fluids.WATER, 250),
                chance(stack(Blocks.GRAVEL.asItem(), 1), 25, stack(Items.CLAY_BALL, 1), 25,
                        stack(item("stone_resource_hematite"), 1), 25, stack(item("ore_titanium"), 1), 25)));

        this.register(new RockMillRecipe("rock.clay", duraLong, consumption,
                new AStack[]{new OreDictStack(ItemTags.SAND), new ComparableStack(item("dust"))},
                new FluidStack(Fluids.WATER, 1_000),
                new ChanceOutput[]{new ChanceOutput(new ItemStack(Items.CLAY_BALL, 4), 100)}));
    }

    public static RockMillRecipe find(ItemStack[] inputs, FluidStack tank) {
        register();
        for (RockMillRecipe recipe : RECIPES) {
            if (matches(inputs, recipe.inputs) && hasFluid(tank, recipe.fluid)) return recipe;
        }
        return null;
    }

    public static boolean isIngredient(ItemStack stack) {
        register();
        for (RockMillRecipe recipe : RECIPES) {
            for (AStack in : recipe.inputs) {
                if (in.matchesRecipe(stack, true)) return true;
            }
        }
        return false;
    }

    private static boolean matches(ItemStack[] slots, AStack[] keys) {
        boolean[] used = new boolean[slots.length];
        for (AStack key : keys) {
            boolean found = false;
            for (int i = 0; i < slots.length; i++) {
                if (used[i]) continue;
                if (key.matchesRecipe(slots[i], false)) {
                    used[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    private static boolean hasFluid(FluidStack tank, FluidStack need) {
        if (need == null) return true;
        return tank != null && tank.type == need.type && tank.fill >= need.fill;
    }

    private static ChanceOutput[] chance(Object... pairs) {
        List<ChanceOutput> list = new ArrayList<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            list.add(new ChanceOutput((ItemStack) pairs[i], (Integer) pairs[i + 1]));
        }
        return list.toArray(ChanceOutput[]::new);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    private static ItemStack stack(Item item, int n) {
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, n);
    }

    public static final class ChanceOutput {
        public final ItemStack stack;
        public final int weight;

        public ChanceOutput(ItemStack stack, int weight) {
            this.stack = stack;
            this.weight = weight;
        }
    }

    public static final class RockMillRecipe {
        public final String name;
        public final int duration;
        public final int power;
        public final AStack[] inputs;
        public final FluidStack fluid;
        public final ChanceOutput[] outputs;

        public RockMillRecipe(String name, int duration, int power, AStack[] inputs, FluidStack fluid, ChanceOutput[] outputs) {
            this.name = name;
            this.duration = duration;
            this.power = power;
            this.inputs = inputs;
            this.fluid = fluid;
            this.outputs = outputs;
        }

        public ItemStack roll(RandomSource rand) {
            int total = 0;
            for (ChanceOutput o : outputs) total += o.weight;
            if (total <= 0) return ItemStack.EMPTY;
            int roll = rand.nextInt(total);
            int acc = 0;
            for (ChanceOutput o : outputs) {
                acc += o.weight;
                if (roll < acc) return o.stack.copy();
            }
            return outputs[outputs.length - 1].stack.copy();
        }
    }
}
