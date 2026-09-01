package com.hbm.blockentity.turret;

import com.hbm.blockentity.IRadarCommandReceiver;
import com.hbm.blocks.BlockDummyable;
import com.hbm.items.weapon.sedna.BulletConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CE {@code TileEntityTurretBaseArtillery}. Radar queue + no-LOS sky check + larger HE subscribe.
 * TODO(CE: TileEntityTurretBaseArtillery.java:85-129): OpenComputers / ROR enqueue.
 */
public abstract class TurretBaseArtilleryBlockEntity extends TurretBaseBlockEntity implements IRadarCommandReceiver {

    protected final List<Vec3> targetQueue = new ArrayList<>();

    protected TurretBaseArtilleryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean sendCommandPosition(int x, int y, int z) {
        enqueueTarget(x + 0.5, y, z + 0.5);
        return true;
    }

    @Override
    public boolean sendCommandEntity(Entity target) {
        enqueueTarget(target.getX(), target.getY(), target.getZ());
        return true;
    }

    public void enqueueTarget(double x, double y, double z) {
        Vec3 pos = getTurretPos();
        if (new Vec3(x - pos.x, y - pos.y, z - pos.z).length() <= getDecetorRange()) {
            targetQueue.add(new Vec3(x, y, z));
        }
    }

    public abstract boolean doLOSCheck();

    @Override
    public boolean entityInLOS(Entity e) {
        if (doLOSCheck()) {
            return super.entityInLOS(e);
        }
        if (level == null) return false;
        Vec3 pos = getTurretPos();
        Vec3 ent = getEntityPos(e);
        double length = ent.subtract(pos).length();
        if (length < getDecetorGrace() || length > getDecetorRange() * 1.1) return false;
        int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(e.getX()), (int) Math.floor(e.getZ()));
        return height < (e.getY() + e.getBbHeight());
    }

    @Override
    protected List<BulletConfig> getAmmoList() {
        return Collections.emptyList();
    }

    @Override
    protected void updateConnections() {
        if (level == null) return;
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof BlockDummyable) || !state.hasProperty(BlockDummyable.META)) return;
        int metaValue = state.getValue(BlockDummyable.META) - BlockDummyable.offset;
        if (metaValue < 0 || metaValue > 5) return;

        Direction dir = Direction.from3DDataValue(metaValue).getOpposite();
        Direction rot = dir.getClockWise(Direction.Axis.Y);
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 4; j++) {
                trySubscribe(level, x + dir.getStepX() * (-1 + j) + rot.getStepX() * -3, y + i, z + dir.getStepZ() * (-1 + j) + rot.getStepZ() * -3, Direction.SOUTH);
                trySubscribe(level, x + dir.getStepX() * (-1 + j) + rot.getStepX() * 2, y + i, z + dir.getStepZ() * (-1 + j) + rot.getStepZ() * 2, Direction.NORTH);
                trySubscribe(level, x + dir.getStepX() * -2 + rot.getStepX() * (1 - j), y + i, z + dir.getStepZ() * -2 + rot.getStepZ() * (1 - j), Direction.EAST);
                trySubscribe(level, x + dir.getStepX() * 3 + rot.getStepX() * (1 - j), y + i, z + dir.getStepZ() * 3 + rot.getStepZ() * (1 - j), Direction.WEST);
            }
        }
    }
}
