package com.hbm.entity.projectile;

import com.hbm.entity.effect.EntityCloudTom;
import com.hbm.entity.logic.EntityTomBlast;
import com.hbm.entity.logic.SatellitePayloadEntityTypes;
import com.hbm.interfaces.IConstantRenderer;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from CE's {@code com.hbm.entity.projectile.EntityTom} (83 lines, read in full) -
 * {@code SatelliteHorizons}'s "gerald" payload, per
 * {@code docs/phase4/satellites_followup_and_loot_pools.md}. CE extends vanilla 1.12
 * {@code EntityThrowable} but <b>entirely overrides its motion integration by hand</b> (manual
 * {@code lastTickPos*}/position update, a hardcoded {@code motionY = -0.5} "falling star" descent
 * every tick, an empty {@code onImpact} override) and never uses {@code EntityThrowable}'s
 * raytrace-driven impact dispatch at all.
 * <p>
 * <b>Base class and physics-bypass - matching two already-committed sibling precedents in this
 * exact port</b>, per the research report's Key design decisions: extends vanilla
 * {@link ThrowableProjectile} directly (the same call this port already made for the identical
 * situation in {@code EntityRubble}), and - since CE's own manual tick barely touches
 * {@code ThrowableProjectile}'s stock {@code tick()}/{@code onHit()} machinery either - never calls
 * {@code super.tick()} at all, incrementing {@link #tickCount} by hand instead (the same pattern
 * this port's own {@code EntityMovingConveyorObject} already established for an entity that skips
 * {@code super.tick()} but still needs a periodic modulo timer; the "skip {@code super.tick()}
 * entirely and hand-roll straight-line motion" shape itself matches this port's own
 * {@code EntityMIRV}, a different CE {@code EntityThrowable} subclass with the identical
 * "manual, non-{@code ThrowableProjectile} impact detection" situation).
 */
public class EntityTom extends ThrowableProjectile implements IConstantRenderer {

    public EntityTom(EntityType<? extends EntityTom> type, Level level) {
        super(type, level);
    }

    public EntityTom(Level level) {
        this(SatellitePayloadEntityTypes.TOM.get(), level);
    }

    /**
     * Package-local stub matching {@code com.hbm.potion.HbmPotionEffects#isWarDim}'s established
     * convention - see {@code EntityDeathBlast}'s own javadoc for the full reasoning; duplicated
     * here rather than shared since this class lives in a different package.
     */
    private static boolean isWarDim(Level level) {
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // CE has no custom entityInit() content either - see class javadoc.
    }

    @Override
    public void tick() {
        // No super.tick() call - see class javadoc. CE's own onUpdate() completely replaces
        // EntityThrowable's motion/impact dispatch; tickCount is advanced by hand purely so the
        // 100-tick chime timer below has something to key off of.
        this.tickCount++;

        Level level = level();

        // CE: lastTickPosX = prevPosX = posX (etc.) - snaps the render-interpolation "old position"
        // to the current position before this tick's jump, avoiding a visual lerp-through. Matches
        // this port's own EntityTNTPrimedBase/EntityMIRV precedent for the identical CE idiom.
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();

        Vec3 motion = getDeltaMovement();
        this.setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        if (this.tickCount % 100 == 0) {
            level.playSound(null, getX(), getY(), getZ(), HBMSoundHandler.chime.get(), SoundSource.HOSTILE, 10000.0F, 1.0F);
        }

        setDeltaMovement(motion.x, -0.5D, motion.z);

        BlockPos groundCheck = BlockPos.containing(getX(), getY(), getZ());
        if (!level.getBlockState(groundCheck).isAir() || getY() < 10) {
            if (!level.isClientSide()) {
                if (isWarDim(level)) {
                    EntityTomBlast blast = new EntityTomBlast(level);
                    blast.setPos(getX(), getY(), getZ());
                    blast.destructionRange = 600;
                    level.addFreshEntity(blast);

                    EntityCloudTom cloud = new EntityCloudTom(level, 500);
                    cloud.moveTo(getX(), getY(), getZ(), 0, 0);
                    level.addFreshEntity(cloud);
                }
            }
            this.discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        // CE's own onImpact(RayTraceResult) override is empty and, given this class's own manual
        // straight-line tick() above never invokes ThrowableProjectile's clip-and-dispatch path this
        // hook belongs to, unreachable in practice - kept only for parity with CE's explicit override.
    }
}
