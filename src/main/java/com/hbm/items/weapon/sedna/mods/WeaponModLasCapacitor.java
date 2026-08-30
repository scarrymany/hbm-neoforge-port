package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Port of CE's {@code WeaponModLasCapacitor} (29 lines) - an underbarrel capacitor for the laser
 * rifle: +5% damage, +50% capacity. Constructs a fresh {@link MagazineFullReload} per {@link #eval}
 * call rather than reusing {@code WeaponModStackMag}'s shared dummy like CE does - see
 * {@link WeaponModCaliber}'s class javadoc for why.
 */
public class WeaponModLasCapacitor extends WeaponModBase {

    public WeaponModLasCapacitor(String id) {
        super(id, "UNDERBARREL");
        this.setPriority(PRIORITY_MULTIPLICATIVE);
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (Objects.equals(key, Receiver.F_BASEDAMAGE)) return cast((Float) base * 1.05F, base);
        if (Objects.equals(key, Receiver.O_MAGAZINE) && base instanceof MagazineFullReload original) {
            return cast(new MagazineFullReload(original.index, original.capacity * 3 / 2)
                    .addConfigs(original.acceptedBullets.toArray(new BulletConfig[0])), base);
        }
        return base;
    }
}
