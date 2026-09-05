package com.hbm.uninos.networkproviders;

import com.hbm.uninos.GenNode;
import com.hbm.uninos.INetworkProvider;
import com.hbm.uninos.NodeNet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/** CE {@code KlystronNetwork} — ignition graph for the torus. */
public class KlystronNetwork extends NodeNet<BlockEntity, BlockEntity, KlystronNetwork.KlystronNode, KlystronNetwork> {

    public static final INetworkProvider<KlystronNetwork> THE_PROVIDER = KlystronNetwork::new;

    @Override
    public void update() {
    }

    public static class KlystronNode extends GenNode<KlystronNetwork> {
        public KlystronNode(INetworkProvider<KlystronNetwork> provider, BlockPos... positions) {
            super(provider, positions);
        }
    }
}
