package com.hbm.blocks.network;

import com.hbm.inventory.fluid.FluidType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Cosmetic, bounded flood-fill contract for re-typing a whole connected run of not-yet-typed ducts at
 * once (Ctrl-click paste, see {@code PipeBaseBlockEntity#pasteSettings}) - ported unchanged from CE's
 * {@code com.hbm.blocks.network.IBlockFluidDuct} ({@code World}/{@code BlockPos} -&gt; {@code Level}/
 * {@code BlockPos}, already the modern type). Distinct from the logical {@code FluidNetMK2} graph -
 * see {@link FluidDuctBaseBlock}'s javadoc.
 */
public interface IBlockFluidDuct {

    void changeTypeRecursively(Level level, BlockPos pos, FluidType prevType, FluidType type, int loopsRemaining);
}
