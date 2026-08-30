package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.Receiver;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Direct port of CE's {@code WeaponModOverride} (24 lines) - a flat base-damage override (not a
 * multiplier). CE only ever wires this up for the {@code weapon_mod_test} debug items (see
 * {@code XWeaponModManager}'s {@code init()} javadoc for why this port doesn't register those) -
 * ported as a complete, correct, reusable {@link IWeaponMod} regardless, since it is explicitly named
 * in this task's concrete-class list and has no dependency on the debug items themselves.
 */
public class WeaponModOverride extends WeaponModBase {

    protected final float baseDamage;

    public WeaponModOverride(String id, float baseDamage, String... slots) {
        super(id, slots);
        this.baseDamage = baseDamage;
        this.setPriority(PRIORITY_SET);
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (Objects.equals(key, Receiver.F_BASEDAMAGE)) return cast(baseDamage, base);
        return base;
    }
}
