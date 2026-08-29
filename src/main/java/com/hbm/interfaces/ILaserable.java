package com.hbm.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public interface ILaserable {

    // CE's deprecated int-coordinate overload (which delegated through ForgeDirection.toEnumFacing())
    // is intentionally dropped here: it was already marked @Deprecated in favor of this BlockPos
    // overload, and com.hbm.lib.ForgeDirection's ported API surface is owned by the lib area, not this one.
    void addEnergy(Level world, BlockPos pos, long energy, Direction dir);

}
