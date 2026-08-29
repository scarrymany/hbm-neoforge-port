package com.hbm.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface IRadResistantBlock {

	//Anything implementing this must override onBlockAdded/onPlace and neighborChanged/onRemove and call
	//RadiationSystemNT.markChunkForRebuild or it won't work

    /**
     * @implNote must not carry side effects
     */
	default boolean isRadResistant(Level worldIn, BlockPos blockPos) {
		return true;
	}
}
