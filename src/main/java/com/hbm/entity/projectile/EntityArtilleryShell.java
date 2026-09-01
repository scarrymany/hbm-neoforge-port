package com.hbm.entity.projectile;

import com.hbm.api.entity.IRadarDetectable;
import com.hbm.entity.logic.IChunkLoader;
import com.hbm.items.weapon.ArtilleryAmmo;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * CE {@code EntityArtilleryShell}. Course-corrects toward {@code target*} then detonates via {@link ArtilleryAmmo}.
 * TODO(CE: EntityArtilleryShell.java:240-293): ForgeChunkManager ticket — {@link IChunkLoader} stand-in.
 * TODO(CE: EntityArtilleryShell.java:353-366): cargo interact / stuck-in-block.
 * TODO(CE: RenderArtilleryShell — none dedicated): OBJ TESR stays empty this wave.
 */
public class EntityArtilleryShell extends EntityThrowableNT implements IChunkLoader, IRadarDetectable {

    private static final EntityDataAccessor<Integer> TYPE =
            SynchedEntityData.defineId(EntityArtilleryShell.class, EntityDataSerializers.INT);

    private double targetX;
    private double targetY;
    private double targetZ;
    private boolean shouldWhistle;
    private boolean didWhistle;
    private ChunkPos loadedChunkPos = new ChunkPos(0, 0);

    public EntityArtilleryShell(EntityType<? extends EntityArtilleryShell> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TYPE, 0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    public EntityArtilleryShell setType(int type) {
        entityData.set(TYPE, type);
        return this;
    }

    public int getShellType() {
        return entityData.get(TYPE);
    }

    public void setTarget(double x, double y, double z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
    }

    public double[] getTarget() {
        return new double[]{targetX, targetY, targetZ};
    }

    public double getTargetHeight() {
        return targetY;
    }

    public void setWhistle(boolean whistle) {
        this.shouldWhistle = whistle;
    }

    public boolean getWhistle() {
        return shouldWhistle;
    }

    public boolean didWhistle() {
        return didWhistle;
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            Vec3 motion = getDeltaMovement();
            double deltaX = targetX - getX();
            double deltaY = targetY - getY();
            double deltaZ = targetZ - getZ();
            double horizontalDist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            double currentHorizontalSpeed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            if (currentHorizontalSpeed < 0.1) currentHorizontalSpeed = 1.0;
            double timeToTarget = horizontalDist / currentHorizontalSpeed;
            double gravity = getGravityVelocity();
            double idealY = (deltaY + 0.5 * gravity * timeToTarget * timeToTarget) / timeToTarget;
            double newMY = motion.y + (idealY - motion.y) * 0.1;
            double newMX = motion.x;
            double newMZ = motion.z;
            if (horizontalDist > 0.5) {
                double idealX = deltaX / horizontalDist;
                double idealZ = deltaZ / horizontalDist;
                newMX += (idealX * currentHorizontalSpeed - motion.x) * 0.1;
                newMZ += (idealZ * currentHorizontalSpeed - motion.z) * 0.1;
                double newSpeedXZ = Math.sqrt(newMX * newMX + newMZ * newMZ);
                if (newSpeedXZ > 0.001) {
                    newMX = newMX * currentHorizontalSpeed / newSpeedXZ;
                    newMZ = newMZ * currentHorizontalSpeed / newSpeedXZ;
                }
            }
            setDeltaMovement(newMX, newMY, newMZ);
        }

        super.tick();

        if (!level().isClientSide) {
            if (!didWhistle && shouldWhistle) {
                Vec3 motion = getDeltaMovement();
                double speed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
                double dist = Math.sqrt((getX() - targetX) * (getX() - targetX) + (getZ() - targetZ) * (getZ() - targetZ));
                if (speed * 18 > dist) {
                    level().playSound(null, BlockPos.containing(targetX, targetY, targetZ),
                            HBMSoundHandler.mortarWhistle.get(), SoundSource.BLOCKS, 15.0F, 0.9F + random.nextFloat() * 0.2F);
                    didWhistle = true;
                }
            }
            updateChunkTicket(this);
        }
    }

    @Override
    protected void onImpact(HitResult mop) {
        if (level().isClientSide) return;
        if (mop instanceof EntityHitResult ehr && ehr.getEntity() instanceof EntityArtilleryShell) return;
        ArtilleryAmmo.onShellImpact(this, mop, getShellType());
    }

    public void killAndClear() {
        discard();
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        IChunkLoader.super.onAddedToLevel(this);
    }

    @Override
    public void onRemovedFromLevel() {
        IChunkLoader.super.onRemovedFromLevel(this);
        super.onRemovedFromLevel();
    }

    @Override
    protected float getAirDrag() {
        return 1.0F;
    }

    @Override
    public double getGravityVelocity() {
        return 9.81F * 0.05F;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public RadarTargetType getTargetType() {
        return RadarTargetType.ARTILLERY;
    }

    @Override
    public void setLoadedChunkPos(ChunkPos pos) {
        this.loadedChunkPos = pos;
    }

    @Override
    public ChunkPos getLoadedChunkPos() {
        return loadedChunkPos;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("type", getShellType());
        tag.putBoolean("shouldWhistle", shouldWhistle);
        tag.putBoolean("didWhistle", didWhistle);
        tag.putDouble("targetX", targetX);
        tag.putDouble("targetY", targetY);
        tag.putDouble("targetZ", targetZ);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setType(tag.getInt("type"));
        shouldWhistle = tag.getBoolean("shouldWhistle");
        didWhistle = tag.getBoolean("didWhistle");
        targetX = tag.getDouble("targetX");
        targetY = tag.getDouble("targetY");
        targetZ = tag.getDouble("targetZ");
    }
}
