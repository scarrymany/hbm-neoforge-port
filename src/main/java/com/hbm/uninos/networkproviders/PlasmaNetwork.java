package com.hbm.uninos.networkproviders;

import com.hbm.uninos.GenNode;
import com.hbm.uninos.INetworkProvider;
import com.hbm.uninos.NodeNet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/** CE {@code PlasmaNetwork} — plasma output graph for the torus. */
public class PlasmaNetwork extends NodeNet<BlockEntity, BlockEntity, PlasmaNetwork.PlasmaNode, PlasmaNetwork> {

    public static final INetworkProvider<PlasmaNetwork> THE_PROVIDER = PlasmaNetwork::new;

    @Override
    public void update() {
    }

    public static class PlasmaNode extends GenNode<PlasmaNetwork> {
        public PlasmaNode(INetworkProvider<PlasmaNetwork> provider, BlockPos... positions) {
            super(provider, positions);
        }
    }
}
