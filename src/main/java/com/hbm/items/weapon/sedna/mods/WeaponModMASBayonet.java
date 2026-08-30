package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.GunConfig;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Port of CE's {@code WeaponModMASBayonet} (80 lines) - a fixed bayonet for the MAS-36. Structurally
 * identical to {@link WeaponModCarbineBayonet} (same dropped-branch reasoning, same reused inspect
 * lambda) - see that class's javadoc.
 */
public class WeaponModMASBayonet extends WeaponModBase {

    public WeaponModMASBayonet(String id) {
        super(id, "BAYONET");
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (Objects.equals(key, GunConfig.I_INSPECTDURATION)) return cast(30, base);
        if (Objects.equals(key, GunConfig.CON_ONPRESSSECONDARY)) return cast(WeaponModCarbineBayonet.INSPECT_ON_SECONDARY, base);
        if (Objects.equals(key, GunConfig.I_INSPECTCANCEL)) return cast(false, base);
        return base;
    }
}
