package com.hbm.api.energymk2;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * Do not use instanceof checks on this interface!
 * <p>
 * Charge is stored as a {@code DataComponentType<Long>} instead of raw NBT (CE stored it under
 * the "charge" NBT key via {@code getChargeTagName()}). Registration of that component is out of
 * this area's scope; see {@link #getChargeComponent()}.
 *
 * @see com.hbm.lib.Library#isBattery
 */
public interface IBatteryItem {

    /**
     * The registered data component backing this item's stored charge. Implementations should
     * return a reference to the mod's shared {@code hbm:battery_charge} component
     * (a {@code DataComponentType<Long>}, persistent + network synchronized), registered by
     * whichever area owns the mod's data-component registry.
     */
    Supplier<DataComponentType<Long>> getChargeComponent();

    /**
     * Adds energy to the battery item.
     * The implementation should ensure the charge does not exceed {@link #getMaxCharge(ItemStack)}.
     *
     * @param stack The ItemStack to charge.
     * @param i     The amount of energy in HE (HBM Energy) to add.
     */
    default void chargeBattery(ItemStack stack, long i) {
        this.setCharge(stack, this.getCharge(stack) + i);
    }

    /**
     * Sets the energy level of the battery item to a specific value.
     * The implementation should ensure the charge does not exceed {@link #getMaxCharge(ItemStack)}.
     *
     * @param stack The ItemStack to modify.
     * @param i     The absolute amount of energy in HE to set.
     */
    default void setCharge(ItemStack stack, long i) {
        long clamped = Math.clamp(i, 0L, this.getMaxCharge(stack));
        stack.set(this.getChargeComponent().get(), clamped);
    }

    /**
     * Removes energy from the battery item.
     * The implementation should ensure the charge does not fall below 0.
     *
     * @param stack The ItemStack to discharge.
     * @param i     The amount of energy in HE to remove.
     */
    default void dischargeBattery(ItemStack stack, long i) {
        this.setCharge(stack, this.getCharge(stack) - i);
    }

    /**
     * Gets the current amount of stored energy in the item.
     *
     * @param stack The ItemStack to query.
     * @return The current charge in HE.
     */
    default long getCharge(ItemStack stack) {
        return stack.getOrDefault(this.getChargeComponent().get(), 0L);
    }

    /**
     * Gets the maximum amount of energy this item can store.
     *
     * @param stack The ItemStack to query.
     * @return The maximum charge (capacity) in HE.
     */
    long getMaxCharge(ItemStack stack);

    /**
     * Gets the maximum rate at which this item can receive energy.
     * This value is an intrinsic property of the battery type and does not depend
     * on the item's current charge level.
     *
     * @return The maximum charge rate in HE per tick.
     */
    long getChargeRate(ItemStack stack);

    /**
     * Gets the maximum rate at which this item can provide energy.
     * This value is an intrinsic property of the battery type and does not depend
     * on the item's current charge level. The actual amount of energy that can be
     * extracted per tick is also limited by the current charge.
     *
     * @return The maximum discharge rate in HE per tick.
     */
    long getDischargeRate(ItemStack stack);

    /**
     * Creates a copy of the given ItemStack with its charge set to 0.
     * The original ItemStack is not modified.
     *
     * @param stack The ItemStack to create an empty version of.
     * @return A new, empty battery ItemStack, or {@link ItemStack#EMPTY} if the input is not a valid {@link IBatteryItem}.
     */
    static ItemStack emptyBattery(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof IBatteryItem battery)) return ItemStack.EMPTY;
        ItemStack out = stack.copy();
        battery.setCharge(out, 0);
        return out;
    }

    /**
     * Creates a new, empty ItemStack from the given Item.
     *
     * @param item The Item to create an empty battery from.
     * @return A new, empty battery ItemStack, or {@link ItemStack#EMPTY} if the input is not a valid {@link IBatteryItem}.
     */
    static ItemStack emptyBattery(Item item) {
        return item instanceof IBatteryItem ? emptyBattery(new ItemStack(item)) : ItemStack.EMPTY;
    }
}
