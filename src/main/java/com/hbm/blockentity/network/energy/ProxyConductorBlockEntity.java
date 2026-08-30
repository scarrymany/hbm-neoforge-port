package com.hbm.blockentity.network.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code com.hbm.tileentity.TileEntityProxyConductor} (read in full: a one-line
 * class, {@code extends TileEntityProxyBase implements IEnergyConductorMK2}) - occupies the 4
 * "extra"-flagged corner dummy positions {@link com.hbm.blocks.network.energy.SubstationBlock}'s
 * {@code fillSpace} override marks, giving each corner its own plain conductor node so an adjacent
 * cable can plug into the substation from any of its 4 corners.
 *
 * <p><b>Simplification vs. CE</b>: CE's {@code TileEntityProxyBase} (not ported - a much larger
 * shared class bundling capability-forwarding-to-core plumbing this port has no other caller for, see
 * {@code MachineSteamEngineBlockEntity}'s javadoc for the identical precedent/reasoning) is not
 * needed here: {@link com.hbm.api.energymk2.IEnergyConductorMK2#createNode()}'s default
 * implementation already builds a plain 6-way node at this block entity's own position, which is all
 * a corner conductor needs to join the network - it does not need to forward capability queries back
 * to the substation core, only to exist as its own conductor node that the core's own node already
 * lists as one of its 8 fixed connection stubs (see {@link SubstationBlockEntity#createNode()}).
 */
public class ProxyConductorBlockEntity extends CableBaseBlockEntity {

    public ProxyConductorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
