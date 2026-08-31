package com.hbm.entity.logic;

import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.entity.mob.glyphid.EntityGlyphidNuclear;
import com.hbm.entity.mob.glyphid.EntityGlyphidScout;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import static com.hbm.entity.mob.glyphid.EntityGlyphid.TASK_BUILD_HIVE;

/**
 * CE {@code com.hbm.entity.logic.EntityWaypoint} (133 lines) —
 * {@code @AutoRegister(name = "entity_waypoint", sendVelocityUpdates = false)}.
 * Hive-path waypoint pointer is stored but {@code EntityGlyphid.setCurrentTask} only takes a byte
 * in this port (CE passed the next waypoint as a second arg).
 */
public class EntityWaypoint extends Entity {

    public static final EntityDataAccessor<Byte> WAYPOINT_TYPE =
            SynchedEntityData.defineId(EntityWaypoint.class, EntityDataSerializers.BYTE);

    public int maxAge = 2400;
    public int radius = 3;
    public boolean highPriority;
    protected EntityWaypoint additional;
    private boolean hasSpawned;

    public EntityWaypoint(EntityType<? extends EntityWaypoint> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(WAYPOINT_TYPE, (byte) 0);
    }

    public void setHighPriority() {
        this.highPriority = true;
    }

    public byte getWaypointType() {
        return this.entityData.get(WAYPOINT_TYPE);
    }

    public void setAdditionalWaypoint(EntityWaypoint waypoint) {
        this.additional = waypoint;
    }

    public void setWaypointType(byte waypointType) {
        this.entityData.set(WAYPOINT_TYPE, waypointType);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount >= maxAge) {
            this.discard();
            return;
        }
        if (this.level().isClientSide || this.tickCount % 40 != 0) {
            return;
        }
        AABB bb = this.getBoundingBox().inflate(radius);
        for (Entity e : this.level().getEntities(this, bb)) {
            if (!(e instanceof EntityGlyphid bug)) {
                continue;
            }
            if (additional != null && !hasSpawned) {
                this.level().addFreshEntity(additional);
                hasSpawned = true;
            }
            boolean skip = bug.getWaypoint() == this
                    || e instanceof EntityGlyphidScout
                    || e instanceof EntityGlyphidNuclear;
            if (!skip) {
                bug.setCurrentTask(getWaypointType(), additional);
            }
            if (getWaypointType() == TASK_BUILD_HIVE) {
                if (e instanceof EntityGlyphidScout) {
                    this.discard();
                }
            } else {
                this.discard();
            }
        }
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        this.setWaypointType(nbt.getByte("type"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putByte("type", getWaypointType());
    }
}
