package com.hbm.items.weapon;

import com.hbm.explosion.vanillant.ExplosionVNT;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Port of CE's {@code com.hbm.items.weapon.ItemGrenadeDynamite} ({@code stick_dynamite}, 17 lines).
 * <p>
 * CE itself calls vanilla's {@code World#newExplosion(...)} directly here, with no CE explosion-
 * framework involvement at all (the one genuinely "simpler bespoke" legacy case
 * {@code docs/phase3/grenades.md} identified). Per this task's own mandate ("Use the already-ported
 * {@code com.hbm.explosion.vanillant.ExplosionVNT} for any block-destroying grenade filling - never a
 * naive per-block loop") and PORT_SPEC's general "always route block destruction through the shared
 * vanillant engine" policy, this port routes the same size-3 blast through
 * {@link ExplosionVNT#makeStandard()} instead of a raw vanilla explosion call - same blast profile
 * (full block destruction, standard AoE damage, standard SFX), one shared engine.
 */
public class ItemGrenadeDynamite extends ItemGenericGrenade {

    public ItemGrenadeDynamite(int fuse, Properties properties) {
        super(fuse, properties);
    }

    @Override
    public void explode(Entity grenade, LivingEntity thrower, Level level, double x, double y, double z) {
        new ExplosionVNT(level, x, y + 0.25D, z, 3F, grenade).makeStandard().explode();
    }
}
