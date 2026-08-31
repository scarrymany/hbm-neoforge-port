package com.hbm.entity.mob.ai;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Ported from CE's {@code com.hbm.entity.mob.ai.EntityAIMaskmanCasualApproach} (164 lines, read in
 * full) - see {@code docs/phase4/entities_bosses.md}. Paths MaskMan to a standoff position roughly
 * 10 blocks from its target rather than closing to melee range.
 * <p>
 * <b>CE's own vanilla melee attack-on-arrival call is commented out in the real source</b>, confirmed
 * by direct read of the actual file:
 * <pre>{@code /*if(d0 <= d1 && this.attackTick <= 20) {
 *     this.attackTick = 20;
 *     if(this.attacker.getHeldItem() != null) this.attacker.swingItem();
 *     this.attacker.attackEntityAsMob(entitylivingbase);
 * }* /}</pre>
 * so this port does not add a melee attack here either - {@code EntityMaskMan}'s
 * {@code ATTACK_DAMAGE} attribute is set for CE parity but genuinely never invoked by any AI task,
 * matching CE exactly. This goal is pure positioning: it paths to a standoff point and looks at the
 * target; all of MaskMan's actual damage output lives in {@link EntityAIMaskmanLasergun}/
 * {@link EntityAIMaskmanMinigun}, which run concurrently (see those classes' own javadocs on why CE
 * never gives them a mutex flag).
 * <p>
 * <b>Simplified relative to CE</b> (documented, not silent): CE's own {@code failedPathFindingPenalty}
 * bookkeeping inspects the resolved {@code Path}'s final {@code PathPoint} - an API shape that no
 * longer exists by that name in 1.21.1 ({@code PathPoint} was renamed/restructured to {@code Node}
 * across several versions since), and this sandbox has no compiled jar to confirm the exact modern
 * equivalent against safely. The observable behavior that bookkeeping served - periodically retry
 * pathing toward a re-computed standoff position near the target, roughly every 4-10 ticks - is
 * preserved via a plain {@code PathNavigation#moveTo} call on the same cadence; only the internal
 * retry-backoff extension (which only ever padded that cadence further on repeated failure) is
 * dropped.
 */
public class EntityAIMaskmanCasualApproach extends Goal {

    private final Monster mob;
    private LivingEntity target;
    private int pathTimer;

    public EntityAIMaskmanCasualApproach(Monster mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        this.target = target;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null && this.target.isAlive() && this.mob.getTarget() == this.target;
    }

    @Override
    public void start() {
        this.pathTimer = 0;
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
        this.target = null;
    }

    @Override
    public void tick() {
        if (this.target == null) return;

        this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);

        if (--this.pathTimer <= 0) {
            RandomSource random = this.mob.getRandom();
            this.pathTimer = 4 + random.nextInt(7);

            Vec3 approach = getApproachPos();
            this.mob.getNavigation().moveTo(approach.x, approach.y, approach.z, 1.0D);
        }

        // CE's own vanilla attack-on-arrival call is commented out in the real source - see class
        // javadoc. Nothing else happens here; this goal is pure positioning.
    }

    /**
     * CE: {@code EntityAIMaskmanCasualApproach#getApproachPos} - a point along the vector from the
     * target to the mob, clamped to at most 20 blocks and offset 10 blocks short (i.e. a standoff
     * ring roughly 10 blocks out), with gaussian jitter on X/Z and a flat -5..+5 jitter on Y.
     */
    private Vec3 getApproachPos() {
        double dx = this.mob.getX() - this.target.getX();
        double dy = this.mob.getY() - this.target.getY();
        double dz = this.mob.getZ() - this.target.getZ();

        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double range = Math.min(len, 20D) - 10D;

        double nx = len > 1.0E-4D ? dx / len : 1D;
        double ny = len > 1.0E-4D ? dy / len : 0D;
        double nz = len > 1.0E-4D ? dz / len : 0D;

        RandomSource random = this.mob.getRandom();
        double x = this.mob.getX() + nx * range + random.nextGaussian() * 2D;
        double y = this.mob.getY() + ny - 5D + random.nextInt(11);
        double z = this.mob.getZ() + nz * range + random.nextGaussian() * 2D;

        return new Vec3(x, y, z);
    }
}
