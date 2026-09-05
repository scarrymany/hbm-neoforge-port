package com.hbm.blockentity.machine.fusion;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.lib.DirPos;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.uninos.UniNodespace;
import com.hbm.uninos.networkproviders.PlasmaNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * CE {@code TileEntityFusionBoiler}. WATER / SUPERHOTSTEAM 32k, heatReq from FT_Heatable first step.
 * TODO(CE: TileEntityFusionBoiler.java:208): OpenComputers ntm_fusion_boiler.
 */
public class FusionBoilerBlockEntity extends LoadedBaseBlockEntity
        implements ITickableBE, IFluidStandardTransceiverMK2, IFusionPowerReceiver {

    protected PlasmaNetwork.PlasmaNode plasmaNode;
    public long plasmaEnergy;
    public long plasmaEnergySync;
    public final FluidTankNTM[] tanks = new FluidTankNTM[2];

    public FusionBoilerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        tanks[0] = new FluidTankNTM(Fluids.WATER, 32_000).withOwner(this);
        tanks[1] = new FluidTankNTM(Fluids.SUPERHOTSTEAM, 32_000).withOwner(this);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        this.plasmaEnergySync = this.plasmaEnergy;
        this.plasmaEnergy = 0;
        for (DirPos pos : getConPos()) {
            if (tanks[0].getTankType() != Fluids.NONE) trySubscribe(tanks[0].getTankType(), level, pos);
            if (tanks[1].getFill() > 0) tryProvide(tanks[1], level, pos);
        }
        if (plasmaNode == null || plasmaNode.expired) {
            Direction dir = FusionFacing.of(this).getOpposite();
            BlockPos nodePos = worldPosition.offset(dir.getStepX() * 4, 2, dir.getStepZ() * 4);
            plasmaNode = UniNodespace.getNode(level, nodePos, PlasmaNetwork.THE_PROVIDER);
            if (plasmaNode == null) {
                plasmaNode = (PlasmaNetwork.PlasmaNode) new PlasmaNetwork.PlasmaNode(PlasmaNetwork.THE_PROVIDER, nodePos)
                        .setConnections(new DirPos(worldPosition.getX() + dir.getStepX() * 5,
                                worldPosition.getY() + 2, worldPosition.getZ() + dir.getStepZ() * 5, dir));
                UniNodespace.createNode(level, plasmaNode);
            }
        }
        if (plasmaNode != null && plasmaNode.hasValidNet()) plasmaNode.net.addReceiver(this);
        networkPackNT(50);
    }

    public DirPos[] getConPos() {
        Direction dir = FusionFacing.of(this);
        Direction rot = dir.getClockWise();
        BlockPos p = worldPosition;
        return new DirPos[]{
                new DirPos(p.getX() - dir.getStepX() + rot.getStepX() * 2, p.getY(), p.getZ() - dir.getStepZ() + rot.getStepZ() * 2, rot),
                new DirPos(p.getX() - dir.getStepX() - rot.getStepX() * 2, p.getY(), p.getZ() - dir.getStepZ() - rot.getStepZ() * 2, rot.getOpposite()),
                new DirPos(p.getX() + dir.getStepX() * 2 + rot.getStepX() * 2, p.getY(), p.getZ() + dir.getStepZ() * 2 + rot.getStepZ() * 2, rot),
                new DirPos(p.getX() + dir.getStepX() * 2 - rot.getStepX() * 2, p.getY(), p.getZ() + dir.getStepZ() * 2 - rot.getStepZ() * 2, rot.getOpposite())
        };
    }

    @Override
    public boolean receivesFusionPower() {
        return true;
    }

    @Override
    public void receiveFusionPower(long fusionPower, double neutronPower, float r, float g, float b) {
        this.plasmaEnergy = fusionPower;
        FT_Heatable heatable = tanks[0].getTankType().getTrait(FT_Heatable.class);
        if (heatable == null) return;
        int waterCycles = Math.min(tanks[0].getFill(), tanks[1].getMaxFill() - tanks[1].getFill());
        int steamCycles = (int) Math.min(fusionPower / heatable.getFirstStep().heatReq, waterCycles);
        if (steamCycles > 0 && level != null) {
            tanks[0].setFill(tanks[0].getFill() - steamCycles);
            tanks[1].setFill(tanks[1].getFill() + steamCycles);
            if (level.random.nextInt(200) == 0) {
                var groans = HBMSoundHandler.boilerGroanSounds();
                level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 2, worldPosition.getZ() + 0.5,
                        groans[level.random.nextInt(groans.length)], SoundSource.BLOCKS, 2.5F, 1.0F);
            }
        }
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
