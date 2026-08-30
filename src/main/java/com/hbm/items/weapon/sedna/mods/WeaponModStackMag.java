package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Port of CE's {@code WeaponModStackMag} (41 lines) - an extended magazine (+50% capacity).
 * Constructs a fresh magazine instance per {@link #eval} call rather than mutating a shared dummy -
 * see {@link WeaponModCaliber}'s class javadoc for why.
 */
public class WeaponModStackMag extends WeaponModBase {

    public WeaponModStackMag(String id) {
        super(id, "MAG");
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (Objects.equals(key, Receiver.O_MAGAZINE)) {
            if (base instanceof MagazineSingleReload original) {
                return cast(new MagazineSingleReload(original.index, original.capacity * 3 / 2).addConfigs(original.acceptedBullets.toArray(new BulletConfig[0])), base);
            }
            if (base instanceof MagazineFullReload original) {
                return cast(new MagazineFullReload(original.index, original.capacity * 3 / 2).addConfigs(original.acceptedBullets.toArray(new BulletConfig[0])), base);
            }
        }
        return base;
    }
}
