package com.hbm.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface IStepTickReceiver {

    void onPlayerStep(Level level, BlockPos pos, Player player);
}
