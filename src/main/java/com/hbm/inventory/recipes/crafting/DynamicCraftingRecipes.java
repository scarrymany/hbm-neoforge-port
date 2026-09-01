package com.hbm.inventory.recipes.crafting;

import com.hbm.inventory.recipes.HbmRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * {@link RecipeSerializer} registrations for this package's dynamic, predicate-matched crafting-
 * table recipes (CE's {@code com.hbm.crafting.handlers.*} family - see
 * {@code docs/phase7/crafting_dynamic_handlers.md}). No new {@code RecipeType} is registered here,
 * unlike {@code ProcessingRecipes}'s machine recipes: every recipe in this package is a real
 * <em>crafting-table</em> recipe, so {@code getType()} returns vanilla's own
 * {@code RecipeType.CRAFTING} (confirmed real via this exact NeoForge checkout's
 * {@code ResultSlot.java.patch}, which itself calls
 * {@code recipeManager.getRemainingItemsFor(RecipeType.CRAFTING, ...)}) rather than a bespoke type
 * only this mod's own menu would ever query - registering a second, mod-owned
 * {@code RecipeType<?>} here would make these recipes invisible to the real crafting table and to
 * the recipe book, defeating the entire point of porting them as crafting-table recipes.
 * <p>
 * Follows {@code ProcessingRecipes}'s own established "append directly to {@link HbmRecipes}'s
 * static {@code DeferredRegister} fields, forced to load via a no-op {@link #bootstrap()}" pattern
 * (see that class's own javadoc for the full rationale) rather than owning a second
 * {@code DeferredRegister<RecipeSerializer<?>>} that would need its own
 * {@code register(modEventBus)} call wired into {@code MainRegistry} (a shared file this task does
 * not edit directly - see this task's {@code wiringSnippets} output for the exact one-line
 * {@link #bootstrap()} call {@code MainRegistry}'s constructor needs, placed anywhere before its
 * existing {@code HbmRecipes.register(modEventBus)} call).
 */
public final class DynamicCraftingRecipes {

    private DynamicCraftingRecipes() {
    }

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GrenadeCraftingRecipe>> GRENADE_SERIALIZER =
            HbmRecipes.RECIPE_SERIALIZERS.register("grenade_crafting", () -> GrenadeCraftingRecipe.Serializer.INSTANCE);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CargoShellCraftingRecipe>> CARGO_SHELL_SERIALIZER =
            HbmRecipes.RECIPE_SERIALIZERS.register("cargo_shell_crafting", () -> CargoShellCraftingRecipe.Serializer.INSTANCE);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RBMKFuelRecycleRecipe>> RBMK_FUEL_RECYCLE_SERIALIZER =
            HbmRecipes.RECIPE_SERIALIZERS.register("rbmk_fuel_recycle", () -> RBMKFuelRecycleRecipe.Serializer.INSTANCE);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ScrapSplitRecipe>> SCRAP_SPLIT_SERIALIZER =
            HbmRecipes.RECIPE_SERIALIZERS.register("scrap_split", () -> ScrapSplitRecipe.Serializer.INSTANCE);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ContainerUpgradeRecipe>> CONTAINER_UPGRADE_CRATE_DESH_SERIALIZER =
            HbmRecipes.RECIPE_SERIALIZERS.register("container_upgrade_crate_desh",
                    () -> new ContainerUpgradeRecipe.Serializer(ContainerUpgradeRecipe.CRATE_DESH));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ContainerUpgradeRecipe>> CONTAINER_UPGRADE_CRATE_TUNGSTEN_SERIALIZER =
            HbmRecipes.RECIPE_SERIALIZERS.register("container_upgrade_crate_tungsten",
                    () -> new ContainerUpgradeRecipe.Serializer(ContainerUpgradeRecipe.CRATE_TUNGSTEN));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ContainerUpgradeRecipe>> CONTAINER_UPGRADE_SAFE_SERIALIZER =
            HbmRecipes.RECIPE_SERIALIZERS.register("container_upgrade_safe",
                    () -> new ContainerUpgradeRecipe.Serializer(ContainerUpgradeRecipe.SAFE));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FluidContainerCraftingRecipe>> FLUID_CONTAINER_CRAFTING_SERIALIZER =
            HbmRecipes.RECIPE_SERIALIZERS.register("fluid_container_crafting",
                    () -> FluidContainerCraftingRecipe.Serializer.INSTANCE);

    /** No-op - see class javadoc. Forces this class's static fields (and therefore the registrations above) to run. */
    public static void bootstrap() {
    }
}
