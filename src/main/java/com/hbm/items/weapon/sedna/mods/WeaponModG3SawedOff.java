package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.GunConfig;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Port of CE's {@code WeapnModG3SawedOff} (32 lines, sic - CE's own filename misspells "Weapon"; not
 * preserved here since this is a class name, not a wire-compatible id). Removes the G3's stock for a
 * faster draw. CE's {@code GunConfig.FUN_ANIMNATIONS} branch (a stockless-draw animation) is dropped -
 * see {@link WeaponModSawedOff}'s class javadoc for why.
 */
public class WeaponModG3SawedOff extends WeaponModBase {

    public WeaponModG3SawedOff(String id) {
        super(id, "SHIELD");
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (Objects.equals(key, GunConfig.I_DRAWDURATION)) return cast(5, base);
        return base;
    }
}
