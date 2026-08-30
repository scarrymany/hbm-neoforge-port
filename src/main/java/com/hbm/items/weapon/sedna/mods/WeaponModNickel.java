package com.hbm.items.weapon.sedna.mods;

import net.minecraft.world.item.ItemStack;

/**
 * Direct port of CE's {@code WeaponModNickel} (13 lines) - a purely cosmetic skin swap for the
 * {@code n_i_4_n_i} gun (nickel/doubloon coin finishes); no eval-time effect in CE either.
 */
public class WeaponModNickel extends WeaponModBase {

    public WeaponModNickel(String id, String slot) {
        super(id, slot);
        this.setPriority(PRIORITY_SET);
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        return base;
    }
}
