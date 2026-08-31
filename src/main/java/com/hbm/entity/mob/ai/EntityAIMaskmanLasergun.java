package com.hbm.entity.mob.ai;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.LegacyMobBulletConfigs;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from CE's {@code com.hbm.entity.mob.ai.EntityAIMaskmanLasergun} (114 lines, read in full) -
 * see {@code docs/phase4/entities_bosses.md}. Active only beyond 10 blocks (inside that range
 * {@link EntityAIMaskmanMinigun} takes over); a 3-way rotating attack that cycles after each mode's
 * own repeat count ({@link EnumLaserAttack#amount}):
 * <ul>
 *     <li><b>ORB</b> - a lobbed {@link LegacyMobBulletConfigs#MASKMAN_ORB} shot aimed at the target,
 *     with an added upward-arc nudge on top (CE: {@code orb.motionY += 0.5D}, an addition, not an
 *     overwrite - no re-alignment of the bullet's rotation to the adjusted motion afterward, matching
 *     CE's own real (slightly cosmetically inconsistent) behavior exactly rather than "fixing" it).</li>
 *     <li><b>MISSILE</b> - a {@link LegacyMobBulletConfigs#MASKMAN_ROCKET} shot. CE constructs it
 *     with the same aim-at-target call as the other two modes, then immediately <em>overwrites</em>
 *     its delta-movement with a shallow, mostly-horizontal, purely one-shot-computed arc toward the
 *     target's XZ - {@code MASKMAN_ROCKET} has no {@code onUpdate} lambda in
 *     {@link LegacyMobBulletConfigs}, so once fired it never re-aims: <b>no homing</b>, matching the
 *     legacy-bullet-system report's own correction (do not confuse this with
 *     {@link LegacyMobBulletConfigs#UFO_ROCKET}, which does home).</li>
 *     <li><b>SPLASH</b> - 5 simultaneous spread {@link LegacyMobBulletConfigs#MASKMAN_TRACER} shots
 *     (each independently deviated via the aim-at-target constructor's own {@code deviation}
 *     parameter, matching CE calling the same constructor 5 times with {@code deviation=0.05F}).</li>
 * </ul>
 * CE's constructor also takes {@code checkSight}/{@code nearbyOnly} boolean parameters - confirmed by
 * direct read to be completely unused inside the real class body - dropped here, not reproduced as
 * dead fields.
 */
public class EntityAIMaskmanLasergun extends Goal {

    private final Monster mob;
    private LivingEntity target;
    private EnumLaserAttack attack;
    private int timer;
    private int attackCount;

    public EntityAIMaskmanLasergun(Monster mob) {
        this.mob = mob;
        this.attack = EnumLaserAttack.VALUES[mob.getRandom().nextInt(3)];
        // CE never calls setMutexBits for this goal (mutexBits stays 0) - it deliberately runs
        // concurrently with EntityAIMaskmanCasualApproach's MOVE+LOOK goal, matching the default
        // empty Goal.Flag set here (no setFlags call needed).
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        this.target = target;
        return this.mob.distanceTo(target) > 10.0F;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() || !this.mob.getNavigation().isDone();
    }

    @Override
    public void tick() {
        timer--;

        if (timer <= 0) {
            timer = attack.delay;

            Level level = this.mob.level();
            if (!level.isClientSide) {
                fire(level);
            }

            attackCount++;
            if (attackCount >= attack.amount) {
                attackCount = 0;
                int newAtk = attack.ordinal() + this.mob.getRandom().nextInt(EnumLaserAttack.VALUES.length - 1);
                attack = EnumLaserAttack.VALUES[newAtk % EnumLaserAttack.VALUES.length];
            }
        }

        this.mob.setYRot(this.mob.getYHeadRot());
    }

    private void fire(Level level) {
        switch (attack) {
            case ORB -> {
                EntityBulletBaseMK4 orb = new EntityBulletBaseMK4(level, LegacyMobBulletConfigs.MASKMAN_ORB, this.mob, this.target, 2.0F, 0F);
                orb.setDeltaMovement(orb.getDeltaMovement().add(0D, 0.5D, 0D));
                level.addFreshEntity(orb);
                this.mob.playSound(HBMSoundHandler.teslaShoot.get(), 1.0F, 1.0F);
            }
            case MISSILE -> {
                EntityBulletBaseMK4 missile = new EntityBulletBaseMK4(level, LegacyMobBulletConfigs.MASKMAN_ROCKET, this.mob, this.target, 1.0F, 0F);

                double dx = this.target.getX() - this.mob.getX();
                double dz = this.target.getZ() - this.mob.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 1.0E-4D) {
                    dx /= len;
                    dz /= len;
                } else {
                    dx = 0D;
                    dz = 0D;
                }
                double vy = 0.5D + this.mob.getRandom().nextDouble() * 0.5D;
                missile.setDeltaMovement(new Vec3(dx * 0.05D, vy, dz * 0.05D));

                level.addFreshEntity(missile);
                this.mob.playSound(HBMSoundHandler.hkShoot.get(), 1.0F, 1.0F);
            }
            case SPLASH -> {
                for (int i = 0; i < 5; i++) {
                    EntityBulletBaseMK4 tracer = new EntityBulletBaseMK4(level, LegacyMobBulletConfigs.MASKMAN_TRACER, this.mob, this.target, 1.0F, 0.05F);
                    level.addFreshEntity(tracer);
                }
            }
        }
    }

    /** CE: {@code EntityAIMaskmanLasergun.EnumLaserAttack} - delay = ticks between shots while this
     *  mode is active, amount = how many shots fire before rotating to a (randomly chosen) new mode. */
    private enum EnumLaserAttack {
        ORB(60, 5), MISSILE(10, 10), SPLASH(40, 3);

        static final EnumLaserAttack[] VALUES = values();

        final int delay;
        final int amount;

        EnumLaserAttack(int delay, int amount) {
            this.delay = delay;
            this.amount = amount;
        }
    }
}
