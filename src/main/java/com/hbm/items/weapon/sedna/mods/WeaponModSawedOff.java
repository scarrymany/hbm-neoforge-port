package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.content.GunShotgunItems;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Port of CE's {@code WeaponModSawedOff} (32 lines) - shortens a shotgun's barrel: more spread and
 * damage, less accuracy. CE's {@code gun_maresleg}-specific {@code GunConfig.FUN_ANIMNATIONS} branch
 * (a shortened lever-action animation set) is dropped - this port's {@code GunConfig} has no
 * {@code animations_DNA} field/getter at all yet (Phase 5, see {@code GunConfig}'s own class javadoc).
 */
public class WeaponModSawedOff extends WeaponModBase {

    public WeaponModSawedOff(String id) {
        super(id, "BARREL");
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {

        if (Objects.equals(key, Receiver.F_SPREADINNATE)) return cast(Math.max(0.025F, (Float) base), base);
        if (Objects.equals(key, Receiver.F_SPREADAMMO)) return cast((Float) base * 1.5F, base);
        if (Objects.equals(key, Receiver.F_BASEDAMAGE)) return cast((Float) base * 1.35F, base);

        if (gun.getItem() == GunShotgunItems.GUN_MARESLEG.get()) {
            if (Objects.equals(key, GunConfig.I_DRAWDURATION)) return cast(5, base);
        }

        return base;
    }
}
