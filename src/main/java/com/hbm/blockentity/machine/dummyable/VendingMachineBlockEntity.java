package com.hbm.blockentity.machine.dummyable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** CE {@code BlockVendingMachine.TileEntityVendingMachine} — render bbox only. */
public class VendingMachineBlockEntity extends BlockEntity {

    public VendingMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public AABB getRenderBoundingBox() {
        BlockPos pos = worldPosition;
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
    }
}
