package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.Receiver;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Port of CE's {@code WeaponModLasAuto} (25 lines) - converts the semi-auto laser rifle to full-auto
 * at reduced damage. CE's {@code GunConfig.O_SCOPETEXTURE} branch (clears the scope overlay) is
 * dropped - this port's {@code GunConfig} has no {@code scopeTexture_DNA} field/getter, see
 * {@link WeaponModScope}'s class javadoc.
 */
public class WeaponModLasAuto extends WeaponModBase {

    public WeaponModLasAuto(String id) {
        super(id, "RECEIVER");
        this.setPriority(PRIORITY_SET);
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (Objects.equals(key, Receiver.F_BASEDAMAGE)) return cast((Float) base * 0.66F, base);
        if (Objects.equals(key, Receiver.B_REFIREONHOLD)) return cast(true, base);
        if (Objects.equals(key, Receiver.I_DELAYAFTERFIRE)) return cast(5, base);
        return base;
    }
}
