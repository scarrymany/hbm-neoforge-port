package com.hbm.entity.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * CE: {@code com.hbm.entity.mob.EntityUFOBase} (207 lines). Shared hover/waypoint AI for
 * {@link EntityFBIDrone}. {@link EntityUFO} is a separate FlyingMob, not a subclass.
 */
public abstract class EntityUFOBase extends FlyingMob implements Enemy {

    private static final EntityDataAccessor<Integer> WAYPOINT_X =
            SynchedEntityData.defineId(EntityUFOBase.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WAYPOINT_Y =
            SynchedEntityData.defineId(EntityUFOBase.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WAYPOINT_Z =
            SynchedEntityData.defineId(EntityUFOBase.class, EntityDataSerializers.INT);

    protected int scanCooldown;
    protected int courseChangeCooldown;
    protected Entity target;

    public EntityUFOBase(EntityType<? extends EntityUFOBase> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WAYPOINT_X, 0);
        builder.define(WAYPOINT_Y, 0);
        builder.define(WAYPOINT_Z, 0);
    }

    @Override
    protected void customServerAiStep() {
        if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
            this.discard();
            return;
        }

        this.setDeltaMovement(Vec3.ZERO);

        if (this.target != null && !this.target.isAlive()) {
            this.target = null;
        }

        this.scanForTarget();

        if (this.courseChangeCooldown <= 0) {
            this.setCourse();
        }
    }

    protected void scanForTarget() {
        int range = this.getScanRange();
        if (this.scanCooldown <= 0) {
            List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class,
                    this.getBoundingBox().inflate(range, range / 2.0D, range));
            this.target = null;
            for (LivingEntity entity : entities) {
                if (!entity.isAlive() || !this.canAttackType(entity.getType())) continue;
                if (entity instanceof Player player) {
                    if (player.getAbilities().instabuild) continue;
                    if (player.hasEffect(MobEffects.INVISIBILITY)) continue;
                    if (this.target == null || this.distanceToSqr(entity) < this.distanceToSqr(this.target)) {
                        this.target = entity;
                    }
                }
            }
            this.scanCooldown = this.getScanDelay();
        }
    }

    protected int getScanRange() {
        return 50;
    }

    protected int getScanDelay() {
        return 100;
    }

    protected boolean isCourseTraversable(double targetX, double targetY, double targetZ, double distance) {
        double stepX = (targetX - this.getX()) / distance;
        double stepY = (targetY - this.getY()) / distance;
        double stepZ = (targetZ - this.getZ()) / distance;
        AABB box = this.getBoundingBox();
        for (int i = 1; i < distance; ++i) {
            box = box.move(stepX, stepY, stepZ);
            if (!this.level().noCollision(this, box)) {
                return false;
            }
        }
        return true;
    }

    protected void approachPosition(double speed) {
        double deltaX = this.getWaypointX() - this.getX();
        double deltaY = this.getWaypointY() - this.getY();
        double deltaZ = this.getWaypointZ() - this.getZ();
        Vec3 delta = new Vec3(deltaX, deltaY, deltaZ);
        double len = delta.length();
        if (len > 5.0D) {
            if (this.isCourseTraversable(this.getWaypointX(), this.getWaypointY(), this.getWaypointZ(), len)) {
                this.setDeltaMovement(delta.normalize().scale(speed));
            } else {
                this.courseChangeCooldown = 0;
            }
        }
    }

    protected void setCourse() {
        if (this.target != null) {
            this.setCourseForTarget();
            this.courseChangeCooldown = 20 + this.random.nextInt(20);
        } else {
            this.setCourseWithoutTarget();
            this.courseChangeCooldown = 60 + this.random.nextInt(20);
        }
    }

    protected void setCourseForTarget() {
        Vec3 vec = new Vec3(this.getX() - this.target.getX(), 0.0D, this.getZ() - this.target.getZ());
        if (vec.lengthSqr() < 1.0E-6D) {
            vec = new Vec3(1.0D, 0.0D, 0.0D);
        }
        vec = vec.yRot((float) (Math.PI * 2 * this.random.nextFloat()));
        double length = vec.length();
        if (length < 1.0E-6D) length = 1.0D;
        double overshoot = 10.0D + this.random.nextDouble() * 10.0D;
        int wX = Mth.floor(this.target.getX() - vec.x / length * overshoot);
        int wZ = Mth.floor(this.target.getZ() - vec.z / length * overshoot);
        int surface = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, wX, wZ);
        int targetY = Math.max(surface, Mth.floor(this.target.getY())) + this.targetHeightOffset();
        this.setWaypoint(wX, targetY, wZ);
    }

    protected int targetHeightOffset() {
        return 2 + this.random.nextInt(2);
    }

    protected int wanderHeightOffset() {
        return 2 + this.random.nextInt(3);
    }

    protected void setCourseWithoutTarget() {
        int x = Mth.floor(this.getX() + this.random.nextGaussian() * 5.0D);
        int z = Mth.floor(this.getZ() + this.random.nextGaussian() * 5.0D);
        int y = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + this.wanderHeightOffset();
        this.setWaypoint(x, y, z);
    }

    public void setWaypoint(int x, int y, int z) {
        this.entityData.set(WAYPOINT_X, x);
        this.entityData.set(WAYPOINT_Y, y);
        this.entityData.set(WAYPOINT_Z, z);
    }

    public int getWaypointX() {
        return this.entityData.get(WAYPOINT_X);
    }

    public int getWaypointY() {
        return this.entityData.get(WAYPOINT_Y);
    }

    public int getWaypointZ() {
        return this.entityData.get(WAYPOINT_Z);
    }
}
