package com.hbm.entity.missile;

import com.hbm.api.entity.IRadarDetectable;
import com.hbm.api.entity.IRadarDetectableNT;
import com.hbm.config.WeaponConfig;
import com.hbm.entity.logic.IChunkLoader;
import com.hbm.entity.projectile.EntityThrowableInterp;
import com.hbm.explosion.ExplosionLarge;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.entity.missile.EntityMissileAntiBallistic} (301 lines, read in
 * full) - the point-defense interceptor. <b>Not</b> an {@link EntityMissileBaseNT} subclass in CE
 * (independent predictive-targeting logic), preserved as its own hierarchy here too.
 * <p>
 * <b>Two independent proximity-fuse kill conditions, both preserved exactly</b> (CE's own quirk,
 * documented rather than "simplified" - see {@code docs/phase3/missile_framework.md}): one in
 * {@link #tick()} itself ({@code distance < 15} to the tracked target -> 51-damage AoE against
 * every nearby {@link EntityMissileBaseNT}, instantly overkilling the default 50 HP pool, then
 * self-destructs), and a second, tighter one inside {@link #aimAtTarget()}
 * ({@code distance < 10 && activationTimer >= 40} -> also self-destructs). Neither subsumes the
 * other in CE (grep-confirmed no dedup), so neither is removed here.
 * <p>
 * <b>Chunk loading</b>: CE duplicates its own copy of the {@code ForgeChunkManager} ticket scaffold
 * (a 3x3 chunk area, not the 1-chunk area {@link EntityMissileBaseNT} uses). Per {@link
 * IChunkLoader}'s own javadoc, this port's replacement is single-chunk only for every consumer -
 * this class gets the same simplification, not a hand-rolled 3x3 reproduction.
 * <p>
 * <b>Radar range</b>: CE reads {@code TileEntityMachineRadarNT.radarRange} (a static field on a
 * not-yet-ported machine block entity) for {@link #targetMissile}'s search box. {@link
 * WeaponConfig#RADAR_RANGE} (this port's own already-shipped config value, same underlying CE
 * option - "7.00_radarRange") is the confirmed real replacement.
 */
public class EntityMissileAntiBallistic extends EntityThrowableInterp implements IChunkLoader, IRadarDetectable, IRadarDetectableNT {

    private static final double BASE_SPEED = 1.5D;

    @Nullable
    public Entity tracking;
    public double velocity;
    private int activationTimer;
    private ChunkPos loadedChunkPos = new ChunkPos(0, 0);

    public EntityMissileAntiBallistic(EntityType<? extends EntityMissileAntiBallistic> type, Level level) {
        super(type, level);
        this.setDeltaMovement(0, BASE_SPEED, 0);
    }

    @Override
    protected double motionMult() {
        return velocity;
    }

    @Override
    public boolean doesImpactEntities() {
        return false;
    }

    @Override
    public void tick() {
        if (!level().isClientSide()) {
            if (velocity < 6) velocity += 0.1;

            if (activationTimer < 10) {
                activationTimer++;
                setDeltaMovement(getDeltaMovement().x, BASE_SPEED, getDeltaMovement().z);
            } else {
                Entity prevTracking = this.tracking;

                if (this.tracking == null || this.tracking.isRemoved()) this.targetMissile();

                if (prevTracking == null && this.tracking != null) {
                    ExplosionLarge.spawnShock(level(), getX(), getY(), getZ(), 24, 3F);
                }

                if (this.tracking != null) {
                    double distance = Math.sqrt(
                            Math.pow(this.tracking.getX() - this.getX(), 2) +
                                    Math.pow(this.tracking.getY() - this.getY(), 2) +
                                    Math.pow(this.tracking.getZ() - this.getZ(), 2));

                    if (distance < 15) {
                        List<Entity> explosionRadius = level().getEntitiesOfClass(Entity.class,
                                new AABB(getX() - 15, getY() - 15, getZ() - 15, getX() + 15, getY() + 15, getZ() + 15));

                        for (Entity entity : explosionRadius) {
                            if (entity instanceof EntityMissileBaseNT target) {
                                target.health -= 51; // default missile health is 50 - always lethal
                            }
                        }

                        this.discard(); // destroy the anti-missile
                        ExplosionLarge.explode(level(), getOwner(), getX(), getY(), getZ(), 20F, true, false, false);
                    }
                }

                if (this.tracking != null) {
                    this.aimAtTarget();
                } else if (this.tickCount > 600) {
                    this.discard();
                }
            }

            updateChunkTicket(this);

            if (this.getY() > 2000 && (this.tracking == null || this.tracking.isRemoved())) this.discard();
        }

        super.tick();

        float f2 = Mth.sqrt((float) (getDeltaMovement().x * getDeltaMovement().x + getDeltaMovement().z * getDeltaMovement().z));
        this.setYRot((float) (Math.atan2(getDeltaMovement().x, getDeltaMovement().z) * 180.0D / Math.PI));
        this.setXRot((float) (Math.atan2(getDeltaMovement().y, f2) * 180.0D / Math.PI) - 90);
    }

    /** Detects and caches the nearest {@link EntityMissileBaseNT} (excluding stealth-coated ones). */
    private void targetMissile() {
        Entity closest = null;
        double dist = 1_000;
        int radarRange = WeaponConfig.RADAR_RANGE.get();
        AABB boundingBox = new AABB(
                this.getX() - radarRange, this.getY(), this.getZ() - radarRange,
                this.getX() + radarRange, this.getY() + radarRange, this.getZ() + radarRange);

        List<Entity> entitiesWithinRange = level().getEntitiesOfClass(Entity.class, boundingBox);

        for (Entity e : entitiesWithinRange) {
            if (e.level() != this.level()) continue;
            if (!(e instanceof EntityMissileBaseNT)) continue; // can only lock onto missiles
            if (e instanceof EntityMissileStealth) continue; // cannot lock onto missiles with stealth coating

            Vec3 vec = new Vec3(e.getX() - getX(), e.getY() - getY(), e.getZ() - getZ());
            if (vec.length() < dist) {
                closest = e;
                dist = vec.length();
            }
        }
        this.tracking = closest;
    }

    /** Predictive targeting: leads the tracked target by its own last-tick delta, scaled by time-to-intercept. */
    private void aimAtTarget() {
        Entity target = this.tracking;
        if (target == null) return;

        Vec3 delta = new Vec3(target.getX() - getX(), target.getY() - getY(), target.getZ() - getZ());
        double intercept = delta.length() / (BASE_SPEED * this.velocity);
        Vec3 targetDelta = new Vec3(target.getX() - target.xo, target.getY() - target.yo, target.getZ() - target.zo);
        Vec3 predicted = new Vec3(target.getX() + targetDelta.x * intercept, target.getY() + targetDelta.y * intercept, target.getZ() + targetDelta.z * intercept);
        Vec3 motion = new Vec3(predicted.x - getX(), predicted.y - getY(), predicted.z - getZ()).normalize();

        if (delta.length() < 10 && activationTimer >= 40) {
            this.discard();
            ExplosionLarge.explode(level(), getOwner(), getX(), getY(), getZ(), 15F, true, false, false);
            return;
        }

        setDeltaMovement(motion.x * BASE_SPEED, motion.y * BASE_SPEED, motion.z * BASE_SPEED);
    }

    @Override
    protected void onImpact(HitResult result) {
        if (this.activationTimer >= 10) {
            this.discard();
            ExplosionLarge.explode(level(), getOwner(), getX(), getY(), getZ(), 20F, true, false, false);
        }
    }

    @Override
    public double getGravityVelocity() {
        return 0.0D;
    }

    @Override
    protected float getAirDrag() {
        return 1F;
    }

    @Override
    protected float getWaterDrag() {
        return 1F;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.velocity = nbt.getDouble("veloc");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putDouble("veloc", this.velocity);
    }

    // --- IChunkLoader -----------------------------------------------------------------------------

    @Override
    public void setLoadedChunkPos(ChunkPos pos) {
        this.loadedChunkPos = pos;
    }

    @Override
    public ChunkPos getLoadedChunkPos() {
        return this.loadedChunkPos;
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        this.onAddedToLevel((Entity) this);
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        this.onRemovedFromLevel((Entity) this);
    }

    // --- IRadarDetectable / IRadarDetectableNT -----------------------------------------------------

    @Override
    public RadarTargetType getTargetType() {
        return RadarTargetType.MISSILE_AB;
    }

    @Override
    public String getTranslationKey() {
        return "radar.target.abm";
    }

    @Override
    public int getBlipLevel() {
        return IRadarDetectableNT.TIER_AB;
    }

    @Override
    public boolean canBeSeenBy(Object radar) {
        return true;
    }

    @Override
    public boolean paramsApplicable(RadarScanParams params) {
        return params.scanMissiles;
    }

    @Override
    public boolean suppliesRedstone(RadarScanParams params) {
        return false;
    }
}
