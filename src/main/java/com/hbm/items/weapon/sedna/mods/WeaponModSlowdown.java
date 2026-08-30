package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.Receiver;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** Direct port of CE's {@code WeaponModSlowdown} (21 lines) - a minigun barrel that trades rate of fire for accuracy. */
public class WeaponModSlowdown extends WeaponModBase {

    public WeaponModSlowdown(String id) {
        super(id, "SPEED");
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (Objects.equals(key, Receiver.I_DELAYAFTERFIRE)) return cast((Integer) base * 2, base);
        if (Objects.equals(key, Receiver.F_SPREADINNATE)) return cast(0F, base);
        return base;
    }
}
