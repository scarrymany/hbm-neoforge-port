package com.hbm.items.weapon.sedna.mags;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/** Port of CE's {@code MagazineSingleReload} - uses individual bullets which are loaded one by one (revolvers, break-actions, tube-fed shotguns). */
public class MagazineSingleReload extends MagazineSingleTypeBase {

    public MagazineSingleReload(int index, int capacity) {
        super(index, capacity);
    }

    /** Reloads a single round per cycle. If the mag is empty, the mag's type changes to the first valid ammo type found. */
    @Override
    public void reloadAction(ItemStack stack, @Nullable Container inventory) {
        standardReload(stack, inventory, 1);
    }
}
