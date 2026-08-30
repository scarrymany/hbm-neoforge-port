package com.hbm.items.weapon.legacy;

import net.minecraft.world.item.Item;

/**
 * Decorative/holdable-only shell for CE's {@code gun_vortex} ({@code ItemGunVortex}), per
 * {@code docs/phase3/guns_and_ammo.md}'s Deferred-scope recommendation (option a) - see
 * {@link ItemGunSupershotgun}'s class javadoc for the full rationale (same dead legacy
 * {@code GunConfiguration}/{@code BulletConfigSyncingUtil} framework, {@code GunEnergyFactory
 * .getVortexConfig()} references {@code R556_STAR}, likewise not a real {@code EnumAmmo} member).
 * Unlike {@code gun_supershotgun}, CE's {@code gun_vortex} has no independently-working secondary
 * mechanic at all - it is registered purely as a holdable collectible, matching CE's fully
 * non-functional real-world behavior 1:1.
 */
public class ItemGunVortex extends Item {

    public ItemGunVortex(Properties properties) {
        super(properties);
    }
}
