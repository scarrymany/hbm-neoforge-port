package com.hbm.items.weapon.legacy;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Port of CE's {@code com.hbm.items.weapon.WeaponizedCell}/{@code GunB92Cell} - {@code gun_b92_ammo},
 * a passive charge-storage item (0-25) meant to be topped up by siphoning from a held
 * {@link ItemGunB92} at 1 charge/tick.
 * <p>
 * <b>Simplified relative to CE, documented rather than silently dropped:</b> the live per-tick
 * inventory-scanning siphon ({@code WeaponizedCell#onUpdate} walking the holder's inventory for a
 * {@code gun_b92} stack and draining 1 charge/tick into this item) is not wired - it is a minor,
 * self-contained flavor mechanic with no bearing on {@code gun_b92}'s own core charge/fire/self-detonate
 * loop (which is fully ported on {@link ItemGunB92} independent of any cell). This item is registered
 * as a real, holdable charge-counter (same {@link LegacyWeaponDataComponents#ENERGY} component
 * {@code gun_b92} itself uses) with a working {@link #getFullCell()} pre-charged factory matching
 * CE's own static helper; only the tick-driven transfer loop is a documented forward reference.
 */
public class ItemGunB92Cell extends Item {

    public ItemGunB92Cell(Properties properties) {
        super(properties);
    }

    /** Port of CE's {@code GunB92Cell.getFullCell()} - a cell pre-charged to the 25-point cap. */
    public static ItemStack getFullCell(ItemGunB92Cell item) {
        ItemStack stack = new ItemStack(item);
        stack.set(LegacyWeaponDataComponents.ENERGY.get(), 25);
        return stack;
    }

    public static int getEnergy(ItemStack stack) {
        return stack.getOrDefault(LegacyWeaponDataComponents.ENERGY.get(), 0);
    }

    // TODO(legacy-weapon-siphon): CE's WeaponizedCell#onUpdate scans the holder's inventory for a
    // gun_b92 stack and transfers 1 charge/tick (capped at 25 here, 10 there before b92 self-
    // detonates) - see class javadoc. Not wired; the cell still holds/persists its own charge count
    // correctly via LegacyWeaponDataComponents.ENERGY.
}
