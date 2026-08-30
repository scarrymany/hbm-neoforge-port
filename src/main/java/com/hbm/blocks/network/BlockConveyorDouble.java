package com.hbm.blocks.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from CE's {@code com.hbm.blocks.network.BlockConveyorDouble} (read in full). Cosmetic/
 * throughput double-wide variant - only the snapping-position math differs from
 * {@link BlockConveyorBendable}. See {@link BlockConveyor}'s javadoc for why the
 * {@code ItemConveyorWand}-damage-value pick/drop overrides are not ported (falls back to
 * self-drop/self-pick, matching every other {@code ModBlocks.BLOCKS} entry).
 */
public class BlockConveyorDouble extends BlockConveyorBendable {

    public BlockConveyorDouble(Properties properties) {
        super(properties);
    }

    @Override
    public Vec3 getClosestSnappingPosition(Level world, BlockPos pos, Vec3 itemPos) {

        Direction dir = this.getTravelDirection(world, pos, itemPos);

        double posX = Mth.clamp(itemPos.x, pos.getX(), pos.getX() + 1);
        double posZ = Mth.clamp(itemPos.z, pos.getZ(), pos.getZ() + 1);

        double xCenter = pos.getX() + 0.5;
        double zCenter = pos.getZ() + 0.5;

        if (dir.getAxis() == Direction.Axis.X) {
            xCenter = posX;
            zCenter += posZ > zCenter ? 0.25 : -0.25;
        }
        if (dir.getAxis() == Direction.Axis.Z) {
            zCenter = posZ;
            xCenter += posX > xCenter ? 0.25 : -0.25;
        }

        return new Vec3(xCenter, pos.getY() + 0.25, zCenter);
    }
}
