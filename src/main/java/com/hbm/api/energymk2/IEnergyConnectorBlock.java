package com.hbm.api.energymk2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;

/**
 * Interface for all blocks that should visually connect to cables without having an
 * {@link IEnergyConnectorMK2} block entity. This is meant for plain Blocks, used for
 * cable-visual rendering only.
 */
public interface IEnergyConnectorBlock {

    /**
     * Same as {@link IEnergyConnectorMK2#canConnect(Direction)} but for regular blocks that
     * might not even have a block entity. Used for rendering only!
     */
    boolean canConnect(BlockGetter level, BlockPos pos, Direction dir);
}
