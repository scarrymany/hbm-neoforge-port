package com.hbm.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface ICustomSelectionBox {

	boolean renderBox(Level world, Player player, BlockState state, BlockPos pos, double x, double y, double z, float partialTicks);
}
