package com.hbm.api.fluidmk2;

import com.hbm.lib.DirPos;
import com.hbm.uninos.GenNode;
import com.hbm.uninos.INetworkProvider;
import net.minecraft.core.BlockPos;

/**
 * Fluid-side counterpart to {@link com.hbm.api.energymk2.Nodespace.PowerNode}. Ported unchanged
 * from CE - a plain {@link GenNode} specialization, no CE behavior to translate.
 */
public class FluidNode extends GenNode<FluidNetMK2> {

    public FluidNode(INetworkProvider<FluidNetMK2> provider, BlockPos... positions) {
        super(provider, positions);
    }

    @Override
    public FluidNode setConnections(DirPos... connections) {
        super.setConnections(connections);
        return this;
    }
}
