package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.Receiver;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** Direct port of CE's {@code WeaponModChoke} (21 lines) - tightens shotgun ammo spread. */
public class WeaponModChoke extends WeaponModBase {

    public WeaponModChoke(String id) {
        super(id, "BARREL");
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (Objects.equals(key, Receiver.F_SPREADAMMO)) return cast((Float) base * 0.5F, base);
        return base;
    }
}
