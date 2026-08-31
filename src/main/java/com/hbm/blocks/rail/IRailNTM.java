package com.hbm.blocks.rail;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Block-side rail-traversal contract, ported from CE's {@code com.hbm.blocks.rail.IRailNTM} (48
 * lines, read in full per {@code docs/phase4/entities_vehicles_aircraft.md}'s rail/train section).
 * {@link com.hbm.entity.train.EntityRailCarBase} calls {@link #getTravelLocation} every tick to walk
 * a car (and, twice more per car per tick, its front/rear axle) along whatever rail block currently
 * occupies its anchor position - the rail block itself decides curve/slope/switch routing and hands
 * back a new position plus overshoot distance/new anchor/yaw via the mutable {@link RailContext}/
 * {@link MoveContext} helper objects; the entity only ever walks the answer it is given.
 * <p>
 * <b>No concrete implementor exists in this port yet.</b> Per this task's own scope boundary, the 13
 * real rail blocks ({@code com.hbm.blocks.rail.*}, all {@code BlockDummyable} multiblock-based, per
 * {@code docs/phase1/blocks_network_rail.md}'s already-thorough survey) are a separate, still-pending
 * Phase 1/2 package gated on the multiblock framework - this interface is created here, in CE's exact
 * package/name, only so the entity-side rail-car code in {@link com.hbm.entity.train} has a real
 * contract to compile against. {@code block instanceof IRailNTM} checks in that package will simply
 * never match anything until that rail-block package lands; that is expected, not a bug here.
 * <p>
 * Types are updated 1:1 for 1.21.1: {@code World} -> {@link Level}, {@code Vec3d} -> {@link Vec3}
 * (CE's {@code Vec3d.rotateYaw}/{@code rotatePitch} extension methods - themselves a well-known
 * Forge-1.12.2 backport of vanilla 1.13+'s {@code Vec3d} rotation helpers - map directly onto modern
 * {@link Vec3#yRot(float)}/{@link Vec3#xRot(float)}, same underlying math, different method name only).
 * {@code BlockPos} is unchanged (still {@code net.minecraft.core.BlockPos}, just a different package).
 */
public interface IRailNTM {

    Vec3 getSnappingPos(Level level, int x, int y, int z, double trainX, double trainY, double trainZ);

    Vec3 getTravelLocation(Level level, int x, int y, int z, double trainX, double trainY, double trainZ,
                            double motionX, double motionY, double motionZ, double speed,
                            RailContext info, MoveContext context);

    TrackGauge getGauge(Level level, int x, int y, int z);

    enum TrackGauge {
        STANDARD,
        NARROW
    }

    /** Mutable out-param CE's {@code getTravelLocation} writes to: the new anchor block, how much
     * requested travel distance is left over (the caller loops until this reaches 0), and the new
     * heading. */
    class RailContext {
        public float yaw;
        public double overshoot;
        public BlockPos pos;

        public RailContext yaw(float y) {
            this.yaw = y;
            return this;
        }

        public RailContext dist(double d) {
            this.overshoot = d;
            return this;
        }

        public RailContext pos(BlockPos d) {
            this.pos = d;
            return this;
        }
    }

    /** Mutable in/out-param describing which of a car's three query points (core anchor, front axle,
     * rear axle) is being walked, and how far off the anchor that axle sits (its "collision bogie
     * distance") - lets a rail block apply per-axle collision/derail checks differently from the
     * core query. */
    class MoveContext {
        public RailCheckType type;
        public double collisionBogieDistance;
        public boolean collision = false;
        public double overshoot;

        public MoveContext(RailCheckType type, double collisionBogieDistance) {
            this.type = type;
            this.collisionBogieDistance = collisionBogieDistance;
        }
    }

    enum RailCheckType {
        CORE,
        FRONT,
        BACK,
        OTHER
    }
}
