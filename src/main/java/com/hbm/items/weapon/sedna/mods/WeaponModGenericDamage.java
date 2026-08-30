package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.Receiver;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Direct port of CE's {@code WeaponModGenericDamage} (25 lines) - a flat +15% base-damage material
 * upgrade, reused (one distinct id per gun-material tier - see
 * {@code XWeaponModManager}'s {@code init()}) across every "material X damage kit" attachment.
 */
public class WeaponModGenericDamage extends WeaponModBase {

    public WeaponModGenericDamage(String id) {
        super(id, "GENERIC_DAMAGE");
        this.setPriority(PRIORITY_MULTIPLICATIVE);
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (parent instanceof Receiver && Objects.equals(key, Receiver.F_BASEDAMAGE) && base instanceof Float f) {
            return cast(f * 1.15F, base);
        }
        return base;
    }
}
