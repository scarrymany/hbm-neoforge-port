package com.hbm.blockentity.machine.fusion;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.uninos.UniNodespace;
import com.hbm.uninos.networkproviders.PlasmaNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * CE {@code TileEntityFusionMHDT}.
 * TODO(CE: TileEntityFusionMHDT.java:134): AudioWrapper largeTurbineRunning — VFX last.
 * TODO(CE: TileEntityFusionMHDT.java:284): OpenComputers ntm_fusion_mhdt.
 */
public class FusionMHDTBlockEntity extends LoadedBaseBlockEntity
        implements ITickableBE, IEnergyProviderMK2, IFluidStandardTransceiverMK2, IFusionPowerReceiver {

    public static final double PLASMA_EFFICIENCY = 1.35D;
    public static final int COOLANT_USE = 50;
    public static long MINIMUM_PLASMA = 5_000_000L;

    protected PlasmaNetwork.PlasmaNode plasmaNode;
    public long plasmaEnergy;
    public long plasmaEnergySync;
    public long power;
    public final FluidTankNTM[] tanks = new FluidTankNTM[2];

    public FusionMHDTBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        tanks[0] = new FluidTankNTM(Fluids.PERFLUOROMETHYL_COLD, 4_000).withOwner(this);
        tanks[1] = new FluidTankNTM(Fluids.PERFLUOROMETHYL, 4_000).withOwner(this);
    }

    public boolean hasMinimumPlasma() {
        return plasmaEnergy >= MINIMUM_PLASMA;
    }

    public boolean isCool() {
        return tanks[0].getFill() >= COOLANT_USE && tanks[1].getFill() + COOLANT_USE <= tanks[1].getMaxFill();
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        plasmaEnergySync = plasmaEnergy;
        if (isCool()) {
            power = (long) Math.floor(plasmaEnergy * PLASMA_EFFICIENCY);
            if (!hasMinimumPlasma()) power /= 2;
            tanks[0].setFill(tanks[0].getFill() - COOLANT_USE);
            tanks[1].setFill(tanks[1].getFill() + COOLANT_USE);
        }
        for (DirPos pos : getConPos()) {
            tryProvide(level, pos.getPos(), pos.getDir());
            if (tanks[0].getTankType() != Fluids.NONE) trySubscribe(tanks[0].getTankType(), level, pos);
            if (tanks[1].getFill() > 0) tryProvide(tanks[1], level, pos);
        }
        if (plasmaNode == null || plasmaNode.expired) {
            Direction dir = FusionFacing.of(this).getOpposite();
            BlockPos nodePos = worldPosition.offset(dir.getStepX() * 6, 2, dir.getStepZ() * 6);
            plasmaNode = UniNodespace.getNode(level, nodePos, PlasmaNetwork.THE_PROVIDER);
            if (plasmaNode == null) {
                plasmaNode = (PlasmaNetwork.PlasmaNode) new PlasmaNetwork.PlasmaNode(PlasmaNetwork.THE_PROVIDER, nodePos)
                        .setConnections(new DirPos(worldPosition.getX() + dir.getStepX() * 7,
                                worldPosition.getY() + 2, worldPosition.getZ() + dir.getStepZ() * 7, dir));
                UniNodespace.createNode(level, plasmaNode);
            }
        }
        if (plasmaNode != null && plasmaNode.hasValidNet()) plasmaNode.net.addReceiver(this);
        networkPackNT(150);
        plasmaEnergy = 0;
    }

    public DirPos[] getConPos() {
        Direction dir = FusionFacing.of(this);
        Direction rot = dir.getClockWise();
        BlockPos p = worldPosition;
        return new DirPos[]{
                new DirPos(p.getX() + dir.getStepX() * 4 + rot.getStepX() * 4, p.getY(), p.getZ() + dir.getStepZ() * 4 + rot.getStepZ() * 4, rot),
                new DirPos(p.getX() + dir.getStepX() * 4 - rot.getStepX() * 4, p.getY(), p.getZ() + dir.getStepZ() * 4 - rot.getStepZ() * 4, rot.getOpposite()),
                new DirPos(p.getX() + dir.getStepX() * 8, p.getY() + 1, p.getZ() + dir.getStepZ() * 8, dir)
        };
    }

    @Override
    public boolean receivesFusionPower() {
        return true;
    }

    @Override
    public void receiveFusionPower(long fusionPower, double neutronPower, float r, float g, float b) {
        plasmaEnergy = fusionPower;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(plasmaEnergySync);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        plasmaEnergy = buf.readLong();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tanks[0].writeToNBT(tag, "t0");
        tanks[1].writeToNBT(tag, "t1");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tanks[0].readFromNBT(tag, "t0");
        tanks[1].readFromNBT(tag, "t1");
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide && plasmaNode != null) {
            UniNodespace.destroyNode(level, plasmaNode);
        }
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return power;
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(tanks);
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(tanks[0]);
    }

    @Override
    public List<FluidTankNTM> getSendingTanks() {
        return List.of(tanks[1]);
    }
}
