package com.hbm.api.conveyor;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public interface IEnterableBlock {
    boolean canItemEnter(Level world, int x, int y, int z, Direction dir, IConveyorItem entity);

    void onItemEnter(Level world, int x, int y, int z, Direction dir, IConveyorItem entity);

    boolean canPackageEnter(Level world, int x, int y, int z, Direction dir, IConveyorPackage entity);

    void onPackageEnter(Level world, int x, int y, int z, Direction dir, IConveyorPackage entity);
}
