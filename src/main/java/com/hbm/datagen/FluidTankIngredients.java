package com.hbm.datagen;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.machine.ItemFluidTank;
import com.hbm.items.machine.MachineDataComponents;
import com.hbm.main.MainRegistry;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

/**
 * NeoForge 1.21 DataComponent fluid tank ingredient helper.
 * CE CraftingManager fluid_barrel_full(TYPE) / Fluids.*.getDict() → DataComponentIngredient for filled tanks.
 * Reuses existing {@link ItemFluidTank} + {@link MachineDataComponents} API.
 * <p>
 * Examples from CE CraftingManager that need this:
 * <ul>
 *   <li>CE :524 {@code upgrade_crystallizer} (PEROXIDE tank)</li>
 *   <li>CE :659 {@code solid_fuel_presto} (HEATINGOIL tank)</li>
 *   <li>CE :745 {@code canister_fuel} (HEATINGOIL barrel)</li>
 *   <li>CE :930 {@code bdcl} (WATER barrel + ANY_TAR)</li>
 *   <li>CE :491 {@code barbed_wire_acid} (PEROXIDE tank) — handled by {@link com.hbm.inventory.recipes.crafting.FluidContainerCraftingRecipe}</li>
 * </ul>
 */
public final class FluidTankIngredients {

    private FluidTankIngredients() {}

    /**
     * CE {@code fluid_tank_full(type, minAmount)}
     * @param type FluidType for the filled tank
     * @param minAmount Minimum fluid amount in mb (usually 1000 for tanks, 16000 for barrels)
     * @return Ingredient matching {@code fluid_tank_full} with FLUID_ID + FLUID_AMOUNT components
     */
    public static Ingredient tankFull(FluidType type, int minAmount) {
        Item tankItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "fluid_tank_full"));
        if (tankItem == Items.AIR) throw new IllegalStateException("fluid_tank_full not registered");
        return DataComponentIngredient.of(
                false, // strict=false: allow additional components (e.g. custom_name)
                DataComponentPredicate.builder()
                        .expect(MachineDataComponents.FLUID_ID.get(), type.getID())
                        .expect(MachineDataComponents.FLUID_AMOUNT.get(), minAmount)
                        .build(),
                tankItem
        );
    }

    /**
     * CE {@code fluid_barrel_full(type, minAmount)}
     * @param type FluidType for the filled barrel
     * @param minAmount Minimum fluid amount in mb (usually 16000 for barrels)
     * @return Ingredient matching {@code fluid_barrel_full} with FLUID_ID + FLUID_AMOUNT components
     */
    public static Ingredient barrelFull(FluidType type, int minAmount) {
        Item barrelItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "fluid_barrel_full"));
        if (barrelItem == Items.AIR) throw new IllegalStateException("fluid_barrel_full not registered");
        return DataComponentIngredient.of(
                false,
                DataComponentPredicate.builder()
                        .expect(MachineDataComponents.FLUID_ID.get(), type.getID())
                        .expect(MachineDataComponents.FLUID_AMOUNT.get(), minAmount)
                        .build(),
                barrelItem
        );
    }

    /**
     * CE {@code fluid_tank_full(type)} with default 1000mb
     */
    public static Ingredient tankFull(FluidType type) {
        return tankFull(type, 1000);
    }

    /**
     * CE {@code fluid_barrel_full(type)} with default 16000mb
     */
    public static Ingredient barrelFull(FluidType type) {
        return barrelFull(type, 16000);
    }
}
