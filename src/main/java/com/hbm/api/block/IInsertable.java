package com.hbm.api.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface IInsertable {
    boolean insertItem(Level world, int x, int y, int z, Direction dir, ItemStack stack);
}
