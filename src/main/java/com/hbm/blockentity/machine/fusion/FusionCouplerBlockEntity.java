package com.hbm.blockentity.machine.fusion;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.lib.DirPos;
import com.hbm.uninos.UniNodespace;
import com.hbm.uninos.networkproviders.KlystronNetwork;
import com.hbm.uninos.networkproviders.PlasmaNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;


/** CE {@code TileEntityFusionCoupler} — plasma in, klystron out. */
public class FusionCouplerBlockEntity extends LoadedBaseBlockEntity implements ITickableBE, IFusionPowerReceiver {

    protected KlystronNetwork.KlystronNode klystronNode;
    protected PlasmaNetwork.PlasmaNode plasmaNode;

    public FusionCouplerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        Direction dir = FusionFacing.of(this).getOpposite();
        Direction rot = dir.getClockWise();

        if (klystronNode == null || klystronNode.expired) {
            BlockPos nodePos = worldPosition.offset(rot.getStepX(), 2, rot.getStepZ());
            klystronNode = UniNodespace.getNode(level, nodePos, KlystronNetwork.THE_PROVIDER);
            if (klystronNode == null) {
                klystronNode = (KlystronNetwork.KlystronNode) new KlystronNetwork.KlystronNode(KlystronNetwork.THE_PROVIDER, nodePos)
                        .setConnections(new DirPos(worldPosition.getX() + rot.getStepX() * 2,
                                worldPosition.getY() + 2, worldPosition.getZ() + rot.getStepZ() * 2, rot));
                UniNodespace.createNode(level, klystronNode);
            }
        }
        if (plasmaNode == null || plasmaNode.expired) {
            BlockPos nodePos = worldPosition.offset(-rot.getStepX(), 2, -rot.getStepZ());
            plasmaNode = UniNodespace.getNode(level, nodePos, PlasmaNetwork.THE_PROVIDER);
            if (plasmaNode == null) {
                plasmaNode = (PlasmaNetwork.PlasmaNode) new PlasmaNetwork.PlasmaNode(PlasmaNetwork.THE_PROVIDER, nodePos)
                        .setConnections(new DirPos(worldPosition.getX() - rot.getStepX() * 2,
                                worldPosition.getY() + 2, worldPosition.getZ() - rot.getStepZ() * 2, rot.getOpposite()));
                UniNodespace.createNode(level, plasmaNode);
            }
        }
        if (klystronNode.net != null) klystronNode.net.addProvider(this);
        if (plasmaNode.net != null) plasmaNode.net.addReceiver(this);
    }

    @Override
    public boolean receivesFusionPower() {
        return true;
    }

    @Override
    public void receiveFusionPower(long fusionPower, double neutronPower, float r, float g, float b) {
        if (klystronNode == null || klystronNode.net == null) return;
        for (BlockEntity te : klystronNode.net.receiverEntries.keySet()) {
            if (te instanceof FusionTorusBlockEntity torus && torus.isLoaded() && !torus.isRemoved()) {
                torus.klystronEnergy += fusionPower;
                break;
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) {
            if (klystronNode != null) UniNodespace.destroyNode(level, klystronNode);
            if (plasmaNode != null) UniNodespace.destroyNode(level, plasmaNode);
        }
    }
}
