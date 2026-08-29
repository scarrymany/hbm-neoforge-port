package com.hbm.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Contract for blocks that expose extra debug info to an analyzer-style tool. Not implemented by
 * anything in this area's scope; ported for the concrete blocks (fluid network pipes/tanks) that
 * will implement it in a later phase.
 */
public interface IAnalyzable {

    List<String> getDebugInfo(Level level, BlockPos pos);
}
