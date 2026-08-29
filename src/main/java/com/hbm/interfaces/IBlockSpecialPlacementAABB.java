package com.hbm.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public interface IBlockSpecialPlacementAABB {
    AABB getCollisionBoundingBoxForPlacement(Level worldIn, BlockPos pos, BlockState stateForPlacement, ItemStack stack);
}
