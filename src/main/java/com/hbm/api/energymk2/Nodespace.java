package com.hbm.api.energymk2;

import com.hbm.lib.DirPos;
import com.hbm.uninos.GenNode;
import com.hbm.uninos.INetworkProvider;
import com.hbm.uninos.UniNodespace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * The dead corpse of nodespace MK1.
 * A fantastic proof of concept, but ultimately it was killed for being just not that versatile.
 * This class is mostly just a compatibility husk that should allow uninodespace to slide into the mod with as much lubrication as it deserves.
 *
 * @author hbm
 */
public class Nodespace {

    public static final INetworkProvider<PowerNetMK2> THE_POWER_PROVIDER = PowerNetMK2::new;

    @Deprecated
    public static PowerNode getNode(Level level, BlockPos pos) {
        return (PowerNode) UniNodespace.getNode(level, pos, THE_POWER_PROVIDER);
    }

    @Deprecated
    public static void createNode(Level level, PowerNode node) {
        UniNodespace.createNode(level, node);
    }

    @Deprecated
    public static void destroyNode(Level level, BlockPos pos) {
        UniNodespace.destroyNode(level, pos, THE_POWER_PROVIDER);
    }

    public static class PowerNode extends GenNode<PowerNetMK2> {

        public PowerNode(BlockPos... positions) {
            super(THE_POWER_PROVIDER, positions);
        }

        @Override
        public PowerNode setConnections(DirPos... connections) {
            super.setConnections(connections);
            return this;
        }
    }
}
