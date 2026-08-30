package com.hbm.items.weapon.legacy;

import net.minecraft.world.item.Item;

/**
 * Decorative/holdable-only shell for CE's {@code gun_supershotgun} ({@code ItemGunShotty}), per
 * {@code docs/phase3/guns_and_ammo.md}'s explicit Deferred-scope recommendation (option a): CE's own
 * legacy {@code GunConfiguration}/{@code BulletConfigSyncingUtil} framework
 * ({@code Gun12GaugeFactory.getShottyConfig()} references {@code G12_SLEEK}, an ammo type that does
 * not exist anywhere in CE's own {@code EnumAmmo}) is confirmed dead/non-functional in current CE -
 * left-click fire is already a silent no-op upstream. This port does <b>not</b> build out that second
 * parallel gun-config engine (explicitly out of scope - see the report's "do not port the framework
 * at all" recommendation); the item is registered so it can be held/equipped/collected, matching
 * CE's actual observable behavior exactly (nothing happens on fire).
 * <p>
 * CE's independently-working meathook grapple (right-click, {@code ItemGunShotty
 * .rayTraceEntitiesInCone} + hooked-entity swing logic) is <b>not</b> reproduced here either - that
 * logic lives entirely inside the unported {@code ItemGunShotty} class itself, is unrelated to the
 * bullet-config system, and is melee/grapple-tool content rather than ammo/ballistics content; flagged
 * as a gap for the parity audit rather than guessed at.
 */
public class ItemGunSupershotgun extends Item {

    public ItemGunSupershotgun(Properties properties) {
        super(properties);
    }
}
