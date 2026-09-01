package com.hbm.entity.projectile;

import com.hbm.api.entity.IRadarDetectable;
import com.hbm.entity.logic.IChunkLoader;
import com.hbm.items.weapon.ArtilleryAmmo;
import com.hbm.util.Vec3dUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * CE {@code EntityArtilleryRocket} + inlined {@code RocketSteeringBallisticArc}/{@code RocketTargetingPredictive}.
 * TODO(CE: EntityArtilleryRocket.java:153-208): ForgeChunkManager ticket — {@link IChunkLoader} stand-in.
 * TODO(CE: RenderArtilleryRocket.java:1): OBJ TESR from {@code turret_himars.obj}.
 * TODO(CE: EntityArtilleryRocket.java:131-142): ExKeroseneOld trail.
 */
public class EntityArtilleryRocket extends EntityThrowableNT implements IChunkLoader, IRadarDetectable {

    private static final EntityDataAccessor<Integer> TYPE =
            SynchedEntityData.defineId(EntityArtilleryRocket.class, EntityDataSerializers.INT);

    @Nullable
    public Entity targetEntity;
    public Vec3 lastTargetPos = Vec3.ZERO;
    private boolean steering = true;
    private final double[][] targetMotion = new double[20][3];
    private ChunkPos loadedChunkPos = new ChunkPos(0, 0);

    public EntityArtilleryRocket(EntityType<? extends EntityArtilleryRocket> type, Level level) {
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

    public EntityArtilleryRocket setType(int type) {
        entityData.set(TYPE, type);
        return this;
    }

    public int getRocketType() {
        return entityData.get(TYPE);
    }

    public EntityArtilleryRocket setTarget(Entity target) {
        this.targetEntity = target;
        return setTarget(target.getX(), target.getY() + target.getBbHeight() / 2D, target.getZ());
    }

    public EntityArtilleryRocket setTarget(double x, double y, double z) {
        this.lastTargetPos = new Vec3(x, y, z);
        return this;
    }

    public Vec3 getLastTarget() {
        return lastTargetPos;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        if (targetEntity == null) {
            if (lastTargetPos.subtract(position()).length() <= 15D) steering = false;
        } else {
            recalculateTargetPosition();
        }
        if (steering) adjustCourse(25D, 15D);
        updateChunkTicket(this);
    }

    private void recalculateTargetPosition() {
        if (targetEntity == null) return;
        Vec3 speed = getDeltaMovement();
        Vec3 delta = new Vec3(targetEntity.getX() - getX(), targetEntity.getY() - getY(), targetEntity.getZ() - getZ());
        double eta = delta.length() - speed.length();
        double motionX = targetEntity.getDeltaMovement().x;
        double motionY = targetEntity.getDeltaMovement().y;
        double motionZ = targetEntity.getDeltaMovement().z;
        for (int i = 1; i < 20; i++) {
            targetMotion[i - 1] = targetMotion[i];
            motionX += targetMotion[i][0];
            motionY += targetMotion[i][1];
            motionZ += targetMotion[i][2];
        }
        targetMotion[19][0] = targetEntity.getDeltaMovement().x;
        targetMotion[19][1] = targetEntity.getDeltaMovement().y;
        targetMotion[19][2] = targetEntity.getDeltaMovement().z;
        if (eta <= 1) {
            setTarget(targetEntity.getX(), targetEntity.getY() + targetEntity.getBbHeight() * 0.5D, targetEntity.getZ());
            return;
        }
        setTarget(
                targetEntity.getX() + (motionX / 20D) * eta,
                targetEntity.getY() + targetEntity.getBbHeight() * 0.5D + (motionY / 20D) * eta,
                targetEntity.getZ() + (motionZ / 20D) * eta);
    }

    private void adjustCourse(double speed, double maxTurn) {
        Vec3 direction = getDeltaMovement().normalize();
        Vec3 motion = getDeltaMovement();
        double horizontalMomentum = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        Vec3 targetPos = lastTargetPos;
        double deltaX = targetPos.x - getX();
        double deltaZ = targetPos.z - getZ();
        double horizontalDelta = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double stepsRequired = horizontalMomentum == 0 ? 1 : horizontalDelta / horizontalMomentum;
        Vec3 target = new Vec3(targetPos.x - getX(), targetPos.y - getY(), targetPos.z - getZ()).normalize();
        double rocketYaw = yaw(direction);
        double rocketPitch = pitch(direction);
        double targetYaw = yaw(target);
        double targetPitch = pitch(target);
        double turnSpeed = Math.min(maxTurn, 45D / stepsRequired);
        if (stepsRequired <= 1) turnSpeed = 180D;
        double deltaYaw = ((targetYaw - rocketYaw) + 180D) % 360D - 180D;
        double deltaPitch = ((targetPitch - rocketPitch) + 180D) % 360D - 180D;
        double turnYaw = Math.min(Math.abs(deltaYaw), turnSpeed) * Math.signum(deltaYaw);
        double turnPitch = Math.min(Math.abs(deltaPitch), turnSpeed) * Math.signum(deltaPitch);
        Vec3 velocity = new Vec3(speed, 0, 0);
        velocity = Vec3dUtil.rotateRoll(velocity, (float) -Math.toRadians(rocketPitch + turnPitch));
        velocity = velocity.yRot((float) Math.toRadians(rocketYaw + turnYaw + 90));
        setDeltaMovement(velocity);
    }

    private static double yaw(Vec3 vec) {
        boolean pos = vec.z >= 0;
        return Math.toDegrees(Math.atan(vec.x / (vec.z == 0 ? 1e-9 : vec.z))) + (pos ? 180 : 0);
    }

    private static double pitch(Vec3 vec) {
        return Math.toDegrees(Math.atan(vec.y / Math.sqrt(vec.x * vec.x + vec.z * vec.z)));
    }

    @Override
    protected void onImpact(HitResult mop) {
        if (!level().isClientSide) {
            ArtilleryAmmo.onRocketImpact(this, mop, getRocketType());
        }
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
        return steering ? 0F : 0.01F;
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
        tag.putDouble("targetX", lastTargetPos.x);
        tag.putDouble("targetY", lastTargetPos.y);
        tag.putDouble("targetZ", lastTargetPos.z);
        tag.putInt("type", getRocketType());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lastTargetPos = new Vec3(tag.getDouble("targetX"), tag.getDouble("targetY"), tag.getDouble("targetZ"));
        setType(tag.getInt("type"));
    }
}
