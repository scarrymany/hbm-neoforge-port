package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.GunConfig;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Port of CE's {@code WeaponModScope} (32 lines). Installs a scope on a compatible gun.
 * <p>
 * CE's {@code O_SCOPETEXTURE} branch (picks a per-gun/per-quality scope overlay texture) is not
 * ported: this port's {@link GunConfig} has no {@code scopeTexture_DNA} field/getter at all (Phase 5
 * rendering, see {@link GunConfig}'s own class javadoc) - the key string still exists for a future
 * mod-eval body to match on, this branch is just unreachable until that field/getter lands. Only the
 * real, load-bearing {@link GunConfig#B_HIDECROSSHAIR} branch (hide the plain crosshair while scoped)
 * is wired.
 */
public class WeaponModScope extends WeaponModBase {

    public WeaponModScope(String id) {
        super(id, "SCOPE");
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (Objects.equals(key, GunConfig.B_HIDECROSSHAIR)) return cast(true, base); // just in case
        return base;
    }
}
