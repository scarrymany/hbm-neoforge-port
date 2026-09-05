package com.hbm.blockentity.machine.fusion;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.lib.DirPos;
import com.hbm.uninos.UniNodespace;
import com.hbm.uninos.networkproviders.PlasmaNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** CE {@code TileEntityFusionCollector} — plasma receiver only, not {@link IFusionPowerReceiver}. */
public class FusionCollectorBlockEntity extends LoadedBaseBlockEntity implements ITickableBE {

    protected PlasmaNetwork.PlasmaNode plasmaNode;

    public FusionCollectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        if (plasmaNode == null || plasmaNode.expired) {
            Direction dir = FusionFacing.of(this).getOpposite();
            BlockPos nodePos = worldPosition.offset(dir.getStepX() * 2, 2, dir.getStepZ() * 2);
            plasmaNode = UniNodespace.getNode(level, nodePos, PlasmaNetwork.THE_PROVIDER);
            if (plasmaNode == null) {
                plasmaNode = (PlasmaNetwork.PlasmaNode) new PlasmaNetwork.PlasmaNode(PlasmaNetwork.THE_PROVIDER, nodePos)
                        .setConnections(new DirPos(worldPosition.getX() + dir.getStepX() * 3,
                                worldPosition.getY() + 2, worldPosition.getZ() + dir.getStepZ() * 3, dir));
                UniNodespace.createNode(level, plasmaNode);
            }
        }
        if (plasmaNode != null && plasmaNode.hasValidNet()) plasmaNode.net.addReceiver(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide && plasmaNode != null) {
            UniNodespace.destroyNode(level, plasmaNode);
        }
    }
}
