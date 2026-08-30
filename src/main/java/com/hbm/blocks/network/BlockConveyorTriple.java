package com.hbm.blocks.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from CE's {@code com.hbm.blocks.network.BlockConveyorTriple} (read in full). Cosmetic/
 * throughput triple-wide variant. See {@link BlockConveyor}'s javadoc for the
 * {@code ItemConveyorWand} pick/drop deferral.
 */
public class BlockConveyorTriple extends BlockConveyorBendable {

    public BlockConveyorTriple(Properties properties) {
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
            zCenter += (posZ > zCenter + 0.15 ? 0.3125 : posZ < zCenter - 0.15 ? -0.3125 : 0);
        }
        if (dir.getAxis() == Direction.Axis.Z) {
            zCenter = posZ;
            xCenter += (posX > xCenter + 0.15 ? 0.3125 : posX < xCenter - 0.15 ? -0.3125 : 0);
        }

        return new Vec3(xCenter, pos.getY() + 0.25, zCenter);
    }
}
