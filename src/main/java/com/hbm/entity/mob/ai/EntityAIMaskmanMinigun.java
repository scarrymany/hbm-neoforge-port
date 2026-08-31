package com.hbm.entity.mob.ai;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.LegacyMobBulletConfigs;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Ported from CE's {@code com.hbm.entity.mob.ai.EntityAIMaskmanMinigun} (59 lines, read in full) -
 * see {@code docs/phase4/entities_bosses.md}. A steady {@link LegacyMobBulletConfigs#MASKMAN_BULLET}
 * stream, active only in the 5-10 block band (below 5, nothing pulls MaskMan closer - it has no
 * melee goal at all, see {@link EntityAIMaskmanCasualApproach}; above 10,
 * {@link EntityAIMaskmanLasergun} takes over instead), fired every {@link #delay} ticks
 * ({@code EntityMaskMan}'s constructor builds this goal with {@code delay=3}, CE's own default).
 * <p>
 * CE's constructor also takes {@code checkSight}/{@code nearbyOnly} boolean parameters - confirmed by
 * direct read to be completely unused inside the real class body - dropped here, not reproduced as
 * dead fields. CE never calls {@code setMutexBits} for this goal either (stays at the default 0), so
 * - matching {@link EntityAIMaskmanLasergun} - no {@code setFlags} call is made here: this goal runs
 * concurrently with {@link EntityAIMaskmanCasualApproach}'s MOVE+LOOK goal.
 */
public class EntityAIMaskmanMinigun extends Goal {

    private final Monster mob;
    private final int delay;
    private LivingEntity target;
    private int timer;

    public EntityAIMaskmanMinigun(Monster mob, int delay) {
        this.mob = mob;
        this.delay = delay;
        this.timer = delay;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        this.target = target;
        double dist = this.mob.distanceTo(target);
        return dist > 5.0D && dist < 10.0D;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() || !this.mob.getNavigation().isDone();
    }

    @Override
    public void tick() {
        timer--;

        if (timer <= 0) {
            timer = delay;

            Level level = this.mob.level();
            if (!level.isClientSide) {
                EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(level, LegacyMobBulletConfigs.MASKMAN_BULLET, this.mob, this.target, 1.0F, 0F);
                level.addFreshEntity(bullet);
                this.mob.playSound(HBMSoundHandler.calShoot.get(), 1.0F, 1.0F);
            }
        }

        this.mob.setYRot(this.mob.getYHeadRot());
    }
}
