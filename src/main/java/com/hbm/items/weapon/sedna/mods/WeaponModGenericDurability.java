package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.GunConfig;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Direct port of CE's {@code WeaponModGenericDurability} (25 lines) - a flat 2x durability material
 * upgrade, reused (one distinct id per gun-material tier) across every "material X durability kit"
 * attachment.
 */
public class WeaponModGenericDurability extends WeaponModBase {

    public WeaponModGenericDurability(String id) {
        super(id, "GENERIC_DURABILITY");
        this.setPriority(PRIORITY_MULTIPLICATIVE);
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (parent instanceof GunConfig && Objects.equals(key, GunConfig.F_DURABILITY) && base instanceof Float f) {
            return cast(f * 2F, base);
        }
        return base;
    }
}
