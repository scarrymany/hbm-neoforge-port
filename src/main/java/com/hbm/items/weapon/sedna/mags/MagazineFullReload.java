package com.hbm.items.weapon.sedna.mags;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/** Port of CE's {@code MagazineFullReload} - uses individual bullets which are loaded all at once. */
public class MagazineFullReload extends MagazineSingleTypeBase {

    public MagazineFullReload(int index, int capacity) {
        super(index, capacity);
    }

    /** Reloads all rounds at once. If the mag is empty, the mag's type changes to the first valid ammo type found. */
    @Override
    public void reloadAction(ItemStack stack, @Nullable Container inventory) {
        standardReload(stack, inventory, this.capacity);
    }
}
