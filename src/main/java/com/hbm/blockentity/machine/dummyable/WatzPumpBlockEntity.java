package com.hbm.blockentity.machine.dummyable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** CE {@code WatzPump.TileEntityWatzPump} — render bbox only. */
public class WatzPumpBlockEntity extends BlockEntity {

    public WatzPumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public AABB getRenderBoundingBox() {
        BlockPos pos = worldPosition;
        return new AABB(pos.getX() - 1, pos.getY(), pos.getZ() - 1,
                pos.getX() + 2, pos.getY() + 2, pos.getZ() + 2);
    }
}
