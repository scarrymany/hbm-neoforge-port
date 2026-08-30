package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.render.misc.RenderScreenOverlay;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** Direct port of CE's {@code WeaponModLasShotgun} (26 lines) - a spread-beam choke for the laser rifle. */
public class WeaponModLasShotgun extends WeaponModBase {

    public WeaponModLasShotgun(String id) {
        super(id, "BARREL");
        this.setPriority(PRIORITY_MULTIPLICATIVE);
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (Objects.equals(key, Receiver.F_BASEDAMAGE)) return cast((Float) base * 0.35F, base);
        if (Objects.equals(key, Receiver.F_SPLITPROJECTILES)) return cast((Float) base * 3F, base);
        if (Objects.equals(key, Receiver.F_SPREADINNATE)) return cast((Float) base + 3F, base);
        if (Objects.equals(key, Receiver.F_SPREADHIPFIRE)) return cast(0F, base);
        if (Objects.equals(key, GunConfig.O_CROSSHAIR)) return cast(RenderScreenOverlay.Crosshair.L_CIRCLE, base);
        return base;
    }
}
