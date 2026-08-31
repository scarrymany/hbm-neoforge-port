package com.hbm.entity.mob;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Direct port of CE's {@code com.hbm.entity.mob.botprime.EntityBurrowingNT} (60 lines, read in full) -
 * see {@code docs/phase4/entities_bosses.md}'s worm-boss table. The shared movement base for every
 * BOTPrime worm segment: no fall damage, hardcoded "not on a ladder," and a {@link #travel(Vec3)}
 * override that fully replaces vanilla's own gravity/friction model with one of two flat drag
 * constants ({@link #dragInAir}/{@link #dragInGround}, set per-instance by {@link EntityBOTPrimeBase}'s
 * constructor) depending on whether the segment is inside an opaque block/water/lava - body segments
 * ({@code !getIsHead()}) get an extra x0.9 drag multiplier on top. Because this override never
 * subtracts a gravity term (unlike vanilla {@code LivingEntity#travel}), the worm is implicitly
 * "no-gravity" by construction - matching CE, which never sets a separate no-gravity flag either.
 * <p>
 * <b>Porting notes:</b> CE's {@code isEntityInsideOpaqueBlock()} (an 8-corner-at-eye-height opaque-cube
 * probe) is functionally identical to vanilla {@link #isInWall()} (used both here and by
 * {@code EntityWormBaseNT#isCourseTraversable}), so this port reuses that one vanilla method instead of
 * reimplementing the corner probe. CE's {@code getEyeHeight()} (a simple {@code height * 0.5F} getter)
 * maps onto vanilla {@link net.minecraft.world.entity.Entity}'s own protected per-pose override
 * point, which in 1.21.1 is named {@code getEyeHeight(Pose, EntityDimensions)} - not {@code
 * getStandingEyeHeight}, an older/different-mapping name that does not exist on this version's
 * {@code Entity}/{@code LivingEntity} (confirmed by the real "does not override or implement a
 * method from a supertype" javac error against the old name; the real name/signature is
 * well-established Minecraft-modding knowledge for the 1.20+ era, not independently verified
 * against a compiled jar in this sandbox).
 */
public abstract class EntityBurrowingNT extends PathfinderMob {

    protected float dragInAir;
    protected float dragInGround;

    protected EntityBurrowingNT(EntityType<? extends EntityBurrowingNT> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    protected float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height() * 0.5F;
    }

    public boolean getIsHead() {
        return false;
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    public void travel(Vec3 travelVector) {
        float drag = this.dragInGround;

        if (!this.isInWall() && !this.isInWater() && !this.isInLava()) {
            drag = this.dragInAir;
        }
        // CE's own else-if branch here (a step-sound-on-random-tick roll) is fully commented out in
        // CE's own source (dead code even upstream) - not reproduced.

        if (!this.getIsHead()) {
            drag *= 0.9F;
        }

        this.moveRelative(0.02F, travelVector);
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(drag));
    }
}
