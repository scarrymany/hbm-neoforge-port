package com.hbm.entity.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from CE's {@code com.hbm.entity.item.EntityDroneBase} (166 lines, read in full) - the
 * shared straight-line-homing flight base for the two logistics-drone entities
 * ({@link EntityDeliveryDrone}, {@link EntityRequestDrone}), per
 * {@code docs/phase4/entities_vehicles_aircraft.md}'s "Logistics-drone entity family" section
 * (entity-movement half only - the block/GUI dock/provider/requester network half is a separate,
 * still-unclaimed Phase 2 package, see that report's Deferred scope and this port's own
 * {@code com.hbm.items.tool.ItemDrone}/{@code ItemDroneLinker} stub javadocs).
 * <p>
 * Never registered/spawned directly - CE never {@code @AutoRegister}s this class itself, only its two
 * concrete subclasses - so it stays {@code abstract} here, matching this port's own established shape
 * for an unspawnable shared entity base (e.g. {@link EntityMovingConveyorObject}). CE's own base-class
 * {@code hitByEntity} override is dead code in practice (both real subclasses fully replace it), so it
 * is not re-created here - see each subclass's own {@code hurt(DamageSource, float)} override instead.
 * <p>
 * <b>The obstacle-avoidance hack below is CE's own, preserved byte-for-byte</b>: a wall collision
 * doesn't trigger any pathfinding, it just adds a flat {@code +1} to the vertical component of motion
 * for the next tick, lurching the drone a full block upward. This task's own brief calls this out
 * explicitly as "preserve exactly, do not improve" - CE itself never revisited this despite it being a
 * crude fix, and no smarter obstacle avoidance is invented here.
 * <p>
 * <b>Client-side interpolation</b>: CE's hand-rolled {@code setPositionAndRotationDirect}/
 * {@code syncPosX/Y/Z} lerp fields (which, read in full, never actually touch yaw/pitch despite the
 * override signature accepting them - CE's drones never rotate) are replaced with vanilla's own
 * {@code lerpTo}/{@code lerpMotion}/{@code lerpTargetX/Y/Z} hooks - the same substitution this port's
 * own sibling {@link EntityMovingConveyorObject} already made for a near-identical CE pattern
 * (confirmed real by Neo Edition's own {@code PlaneBase}/{@code Bomber} port per
 * {@code docs/phase4/entities_vehicles_aircraft.md}'s Key design decisions section). Rotation is
 * deliberately left un-overridden (no {@code lerpTargetXRot}/{@code lerpTargetYRot}), matching CE's
 * own choice never to interpolate it.
 * <p>
 * <b>Not ported</b>: CE's {@code canTriggerWalking() -> false} override - no confirmed-safe 1.21.1
 * equivalent hook was found in this port or Neo Edition (same judgment call already made by this
 * package's own {@link EntityTNTPrimedBase}), and it has no gameplay consequence for a flying entity
 * that never walks on the ground.
 */
public abstract class EntityDroneBase extends Entity {

    private static final EntityDataAccessor<Byte> APPEARANCE =
            SynchedEntityData.defineId(EntityDroneBase.class, EntityDataSerializers.BYTE);

    /** CE sentinel: {@code -1} means "no target set" (not a null target). */
    public double targetX = -1;
    public double targetY = -1;
    public double targetZ = -1;

    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;

    protected EntityDroneBase(EntityType<? extends EntityDroneBase> entityType, Level level) {
        super(entityType, level);
    }

    public void setTarget(double x, double y, double z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    /** Lets a player's melee swing target this entity - see the two subclasses' own {@code hurt} overrides. */
    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(APPEARANCE, (byte) 0);
    }

    /**
     * 0 = empty, 1 = crate, 2 = barrel - purely cosmetic synced appearance, no gameplay effect at
     * this base-class layer (CE's own doc comment on this method, preserved).
     */
    public void setAppearance(int style) {
        this.entityData.set(APPEARANCE, (byte) style);
    }

    public int getAppearance() {
        return this.entityData.get(APPEARANCE);
    }

    public double getSpeed() {
        return 0.125D;
    }

    /** Overridden by {@link EntityDeliveryDrone}; a no-op here, matching CE's own base-class stub. */
    protected void loadNeighboringChunks() {
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpSteps = 10;
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
        this.setDeltaMovement(x, y, z);
    }

    @Override
    public double lerpTargetX() {
        return this.lerpSteps > 0 ? this.lerpX : this.getX();
    }

    @Override
    public double lerpTargetY() {
        return this.lerpSteps > 0 ? this.lerpY : this.getY();
    }

    @Override
    public double lerpTargetZ() {
        return this.lerpSteps > 0 ? this.lerpZ : this.getZ();
    }

    @Override
    public void tick() {
        Level level = this.level();

        if (level.isClientSide()) {
            level.addParticle(ParticleTypes.SMOKE, this.getX() + 1.125, this.getY() + 0.75, this.getZ(), 0.0D, -0.2D, 0.0D);
            level.addParticle(ParticleTypes.SMOKE, this.getX() - 1.125, this.getY() + 0.75, this.getZ(), 0.0D, -0.2D, 0.0D);
            level.addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.75, this.getZ() + 1.125, 0.0D, -0.2D, 0.0D);
            level.addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.75, this.getZ() - 1.125, 0.0D, -0.2D, 0.0D);

            if (this.lerpSteps > 0) {
                this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.getYRot(), this.getXRot());
                this.lerpSteps--;
            } else {
                this.reapplyPosition();
            }
            return;
        }

        this.setDeltaMovement(Vec3.ZERO);

        if (this.targetY != -1) {
            Vec3 dist = new Vec3(this.targetX - this.getX(), this.targetY - this.getY(), this.targetZ - this.getZ());
            double speed = Math.min(this.getSpeed(), dist.length());
            this.setDeltaMovement(dist.normalize().scale(speed));
        }

        // CE's own crude obstacle-avoidance hack - preserved exactly, see class javadoc.
        if (this.horizontalCollision) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 1.0D, 0.0D));
        }

        this.loadNeighboringChunks();
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("tX", this.targetX);
        tag.putDouble("tY", this.targetY);
        tag.putDouble("tZ", this.targetZ);
        tag.putByte("app", this.entityData.get(APPEARANCE));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("tY")) {
            this.targetX = tag.getDouble("tX");
            this.targetY = tag.getDouble("tY");
            this.targetZ = tag.getDouble("tZ");
        }
        this.entityData.set(APPEARANCE, tag.getByte("app"));
    }
}
