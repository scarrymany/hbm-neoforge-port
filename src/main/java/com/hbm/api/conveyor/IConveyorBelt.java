package com.hbm.api.conveyor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface IConveyorBelt {

    /**
     * Returns true if the item should stay on the conveyor, false if the item should drop off
     */
    boolean canItemStay(Level world, int x, int y, int z, Vec3 itemPos);

    Vec3 getTravelLocation(Level world, int x, int y, int z, Vec3 itemPos, double speed);

    Vec3 getClosestSnappingPosition(Level world, BlockPos pos, Vec3 itemPos);
}
