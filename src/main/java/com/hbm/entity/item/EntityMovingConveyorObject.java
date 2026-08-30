package com.hbm.entity.item;

import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.api.conveyor.IEnterableBlock;
import com.hbm.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.entity.item.EntityMovingConveyorObject} (read in full). Shared
 * tick loop for every object a conveyor belt carries: reads the block underneath, consults the
 * already-ported Phase 0 {@link IConveyorBelt}/{@link IEnterableBlock} contracts to compute motion,
 * and hands off to a subclass hook when the object either leaves the conveyor network entirely
 * ({@link #onLeaveConveyor()}) or steps onto an {@link IEnterableBlock} ({@link #enterBlock}).
 * <p>
 * Cross-checked against Neo Edition's real (confirmed-shape) {@code com.hbm.entity.item
 * .MovingConveyorObject} for the 1.21.1 {@code Entity} API surface ({@code tick()} instead of
 * CE's {@code onUpdate()}, {@code level()} instead of a raw {@code level} field access - Neo
 * Edition's own file inconsistently uses the raw field in one spot, which does not compile against
 * 1.21.1's actual {@code Entity} class; {@code level()} is used exclusively here, matching every
 * other confirmed-working Neo Edition entity such as {@code BlackHole}/{@code FallingBlockEntityNT}).
 * CE remains the source of truth for the actual conveyor-motion/cram behavior; Neo Edition's own copy
 * of this class does not even implement the cram check at all, so it is not a behavior reference here.
 * <p>
 * <b>Not ported</b>: CE's manual {@code turnProgress}/{@code syncPosX/Y/Z} client-side position lerp
 * and the {@code setPositionAndRotationDirect}/{@code setVelocity} hooks that fed it - those were
 * 1.12-era entity-tracker entry points with no direct 1.21.1 equivalent. In their place, the standard
 * modern {@code Entity#lerpTo}/{@code #lerpMotion}/{@code #lerpTargetX/Y/Z} hooks (the same ones Neo
 * Edition's own class implements) are ported instead - functionally the same "smooth over N ticks"
 * idea, just routed through vanilla's own interpolation entry points rather than a hand-rolled one.
 * Render/model work for these entities is out of scope here regardless (Phase 5 concern per
 * {@code docs/phase2/blocks_network_conveyor_crane.md}).
 * <p>
 * <b>Substituted</b>: CE's cram-jam handling spawns a {@code com.hbm.explosion.vanillant.ExplosionVNT}
 * (tiny, non-block-destroying) purely for player feedback. That package is not ported yet anywhere in
 * this port (confirmed by a fresh search - {@code docs/phase0/STATUS.md}'s gap list still has no
 * explosion-system entry), so a plain vanilla {@link Level#explode} call with
 * {@link Level.ExplosionInteraction#NONE} stands in for it: same "small pop, no block damage" effect,
 * with the actual optional block destruction still handled exactly like CE - a separate,
 * config-gated {@link Level#destroyBlock} call below.
 */
public abstract class EntityMovingConveyorObject extends Entity {

    protected int lerpSteps;
    protected double lerpX;
    protected double lerpY;
    protected double lerpZ;
    protected float lerpYRot;
    protected float lerpXRot;

    protected EntityMovingConveyorObject(EntityType<? extends EntityMovingConveyorObject> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yRot;
        this.lerpXRot = xRot;
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
    public float lerpTargetXRot() {
        return this.lerpSteps > 0 ? this.lerpXRot : this.getXRot();
    }

    @Override
    public float lerpTargetYRot() {
        return this.lerpSteps > 0 ? this.lerpYRot : this.getYRot();
    }

    @Override
    public void tick() {
        Level level = this.level();

        if (level.isClientSide) {
            if (this.lerpSteps > 0) {
                this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
                this.lerpSteps--;
            } else {
                this.reapplyPosition();
            }
            return;
        }

        this.tickCount++;

        if (this.tickCount <= 5) {
            return;
        }

        // cram check, every 20s
        if ((this.tickCount + this.getId()) % 400 == 0) {
            List<EntityMovingConveyorObject> jammed = level.getEntitiesOfClass(
                    EntityMovingConveyorObject.class, this.getBoundingBox().inflate(0.125, 0.125, 0.125));

            if (jammed.size() >= ServerConfig.CONVEYOR_CRAM_MAX.get()) {
                for (EntityMovingConveyorObject obj : jammed) {
                    obj.discard();
                }

                level.explode(this, this.getX(), this.getY() + 0.125, this.getZ(), 1.0F, false, Level.ExplosionInteraction.NONE);

                BlockPos cramPos = BlockPos.containing(this.getX(), this.getY(), this.getZ());
                if (level.getBlockState(cramPos).getBlock() instanceof IConveyorBelt
                        && this.tickCount > 400 && ServerConfig.CONVEYOR_CRAM_EXPLODE.get()) {
                    level.destroyBlock(cramPos, false);
                }
            }
        }

        int blockX = (int) Math.floor(this.getX());
        int blockY = (int) Math.floor(this.getY());
        int blockZ = (int) Math.floor(this.getZ());
        BlockPos blockPos = new BlockPos(blockX, blockY, blockZ);
        Block block = level.getBlockState(blockPos).getBlock();
        Vec3 itemPos = this.position();
        boolean isOnConveyor = block instanceof IConveyorBelt belt && belt.canItemStay(level, blockX, blockY, blockZ, itemPos);

        if (!isOnConveyor) {
            if (this.onLeaveConveyor()) {
                return;
            }
        } else {
            Vec3 target = ((IConveyorBelt) block).getTravelLocation(level, blockX, blockY, blockZ, itemPos, getMoveSpeed());
            this.setDeltaMovement(target.x - this.getX(), target.y - this.getY(), target.z - this.getZ());
        }

        BlockPos lastPos = this.blockPosition();
        this.move(MoverType.SELF, this.getDeltaMovement());
        BlockPos newPos = this.blockPosition();

        if (!lastPos.equals(newPos)) {
            BlockState newState = level.getBlockState(newPos);
            Block newBlock = newState.getBlock();

            if (newBlock instanceof IEnterableBlock enterable) {
                Direction dir = null;

                if (lastPos.getX() > newPos.getX() && lastPos.getY() == newPos.getY() && lastPos.getZ() == newPos.getZ()) dir = Direction.EAST;
                else if (lastPos.getX() < newPos.getX() && lastPos.getY() == newPos.getY() && lastPos.getZ() == newPos.getZ()) dir = Direction.WEST;
                else if (lastPos.getX() == newPos.getX() && lastPos.getY() > newPos.getY() && lastPos.getZ() == newPos.getZ()) dir = Direction.UP;
                else if (lastPos.getX() == newPos.getX() && lastPos.getY() < newPos.getY() && lastPos.getZ() == newPos.getZ()) dir = Direction.DOWN;
                else if (lastPos.getX() == newPos.getX() && lastPos.getY() == newPos.getY() && lastPos.getZ() > newPos.getZ()) dir = Direction.SOUTH;
                else if (lastPos.getX() == newPos.getX() && lastPos.getY() == newPos.getY() && lastPos.getZ() < newPos.getZ()) dir = Direction.NORTH;

                if (dir != null) {
                    this.enterBlock(enterable, newPos, dir);
                }
            } else if (!newState.isSolidRender(level, newPos)) {
                BlockState belowState = level.getBlockState(newPos.below());
                if (belowState.getBlock() instanceof IEnterableBlock enterable) {
                    this.enterBlockFalling(enterable, newPos);
                }
            }
        }
    }

    public abstract void enterBlock(IEnterableBlock enterable, BlockPos pos, Direction dir);

    public void enterBlockFalling(IEnterableBlock enterable, BlockPos pos) {
        this.enterBlock(enterable, pos.below(), Direction.UP);
    }

    /**
     * @return true if the update loop should end (the object has left the conveyor network)
     */
    public abstract boolean onLeaveConveyor();

    public double getMoveSpeed() {
        return 0.0625D;
    }
}
