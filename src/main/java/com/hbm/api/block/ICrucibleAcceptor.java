package com.hbm.api.block;

import com.hbm.inventory.material.Mats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Ported from CE's {@code com.hbm.api.block.ICrucibleAcceptor} (26 lines, read in full) - the
 * contract a block below/beside a Crucible (in CE's real design, primarily the separate Foundry
 * mold/basin block family - not ported in this pass, see {@code phase7/crucible_core.md}'s "Open
 * questions") implements to receive poured molten material. {@code ForgeDirection} maps onto
 * {@link Direction}; {@code World}/{@code BlockPos} onto NeoForge's own {@link Level}/{@link BlockPos};
 * method shapes and names are otherwise unchanged from CE.
 * <p>
 * Zero implementers exist anywhere in this port as of this pass - {@code MachineCrucibleBlock} is
 * the only implementer, and it only ever delegates to its own core block entity (a crucible cannot
 * pour into itself). A poured crucible therefore always hits {@code getPouringTarget() == null} and
 * falls through to {@code CrucibleUtil}'s safe-spill path (material retained, nothing lost) until a
 * future pass ports at least one real acceptor.
 */
public interface ICrucibleAcceptor {

    /*
     * Pouring: The metal leaves the channel/crucible and usually (but not always) falls down. The
     * additional double coords give a more precise impact location. Also useful for entities like
     * large crucibles since they are filled from the top.
     */
    boolean canAcceptPartialPour(Level level, BlockPos pos, double dX, double dY, double dZ, Direction side, Mats.MaterialStack stack);

    Mats.MaterialStack pour(Level level, BlockPos pos, double dX, double dY, double dZ, Direction side, Mats.MaterialStack stack);

    /*
     * Flowing: The "safe" transfer of metal using a channel or other means, usually from block to
     * block and usually horizontally (but not necessarily). May also be used for entities like
     * minecarts that could be loaded from the side. Not exercised by the Crucible itself - it
     * hardcodes both methods to a no-op, matching CE exactly.
     */
    boolean canAcceptPartialFlow(Level level, BlockPos pos, Direction side, Mats.MaterialStack stack);

    Mats.MaterialStack flow(Level level, BlockPos pos, Direction side, Mats.MaterialStack stack);
}
