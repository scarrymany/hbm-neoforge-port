package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.content.GunRifleItems;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** Direct port of CE's {@code WeaponModSilencer} (27 lines) - swaps the fire sound for a quiet one. */
public class WeaponModSilencer extends WeaponModBase {

    public WeaponModSilencer(String id) {
        super(id, "SILENCER");
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (Objects.equals(key, Receiver.S_FIRESOUND)) {
            if (gun.getItem() == GunRifleItems.GUN_AMAT.get()) return cast(HBMSoundHandler.silencerShoot.get(), base);
            return cast(HBMSoundHandler.fireSilenced.get(), base);
        }
        return base;
    }
}
