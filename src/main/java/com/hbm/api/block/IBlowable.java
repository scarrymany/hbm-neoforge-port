package com.hbm.api.block;

import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface IBlowable { //sloppy toppy

    /** Called server-side when a fan blows on an IBlowable in range every tick. */
    void applyFan(Level world, BlockPos pos, ForgeDirection dir, int dist);
}
