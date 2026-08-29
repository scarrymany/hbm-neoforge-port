package com.hbm.api.energymk2;

import com.hbm.lib.DirPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface IEnergyConductorMK2 extends IEnergyConnectorMK2 {

    default Nodespace.PowerNode createNode() {
        BlockEntity self = (BlockEntity) this;
        BlockPos pos = self.getBlockPos();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        return new Nodespace.PowerNode(pos).setConnections(
                new DirPos(x + 1, y, z, Direction.EAST),
                new DirPos(x - 1, y, z, Direction.WEST),
                new DirPos(x, y + 1, z, Direction.UP),
                new DirPos(x, y - 1, z, Direction.DOWN),
                new DirPos(x, y, z + 1, Direction.SOUTH),
                new DirPos(x, y, z - 1, Direction.NORTH)
        );
    }
}
