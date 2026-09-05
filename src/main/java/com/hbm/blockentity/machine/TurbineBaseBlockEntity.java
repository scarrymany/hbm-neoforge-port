package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.lib.DirPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Ported from CE's {@code TileEntityTurbineBase} (abstract, no direct block, read in full): the
 * shared tick loop CE's own doc comment says is "intended for future multiblock turbines" but which,
 * in CE's actual shipped content, only {@code TileEntityMachineIndustrialTurbine} ends up using -
 * see {@link MachineIndustrialTurbineBlockEntity}, the sole concrete subclass in this pass.
 * <p>
 * Reads {@link FT_Coolable} off {@code tanks[0]}, bounds {@code ops} by
 * {@link #consumptionPercent()} (subclass-supplied), calls the abstract {@link #generatePower}
 * hook, then pushes HE/fluid over {@link #getPowerPos()}/{@link #getConPos()} (both subclass-
 * supplied, matching the multiblock's fixed connector geometry). CE's {@code onLeverPull} steam-
 * densification state machine (STEAM -&gt; HOTSTEAM -&gt; ... -&gt; back to STEAM) is kept as
 * {@link #onLeverPull()} — wired by {@link MachineIndustrialTurbineBlock} Exact CE
 * {@code MachineIndustrialTurbine.java:53-78}.
 */
public abstract class TurbineBaseBlockEntity extends MachineBaseBlockEntity
        implements IEnergyProviderMK2, IFluidStandardTransceiverMK2, ITickableBE {

    public FluidTankNTM[] tanks;
    public long powerBuffer;
    public boolean operational;

    protected TurbineBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int scount) {
        super(type, pos, state, scount, true, true);
    }

    public abstract double getEfficiency();

    public abstract DirPos[] getConPos();

    public abstract DirPos[] getPowerPos();

    public abstract double consumptionPercent();

    public boolean doesResizeCompressor() {
        return false;
    }

    protected void generatePower(long power, int steamConsumed) {
        this.powerBuffer += power;
    }

    protected void onServerTick() {
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        this.powerBuffer = 0;
        operational = false;

        FluidType in = tanks[0].getTankType();
        boolean valid = false;
        if (in.hasTrait(FT_Coolable.class)) {
            FT_Coolable trait = in.getTrait(FT_Coolable.class);
            double eff = trait.getEfficiency(FT_Coolable.CoolingType.TURBINE) * getEfficiency();
            if (eff > 0) {
                tanks[1].setTankType(trait.coolsTo);
                int inputOps = (int) (Math.min(Math.ceil(tanks[0].getFill() * consumptionPercent()), tanks[0].getFill()) / trait.amountReq);
                int outputOps = (tanks[1].getMaxFill() - tanks[1].getFill()) / trait.amountProduced;
                int ops = Math.min(inputOps, outputOps);
                if (ops > 0) {
                    tanks[0].setFill(tanks[0].getFill() - ops * trait.amountReq);
                    tanks[1].setFill(tanks[1].getFill() + ops * trait.amountProduced);
                    this.generatePower((long) (ops * trait.heatEnergy * eff), ops * trait.amountReq);
                }
                valid = true;
                operational = ops > 0;
            }
        }

        onServerTick();

        if (!valid) tanks[1].setTankType(Fluids.NONE);

        for (DirPos pos : getPowerPos()) {
            this.tryProvide(level, pos.getPos().getX(), pos.getPos().getY(), pos.getPos().getZ(), pos.getDir());
        }
        for (DirPos pos : getConPos()) {
            this.tryProvide(tanks[1], level, pos.getPos(), pos.getDir());
            this.trySubscribe(tanks[0].getTankType(), level, pos.getPos().getX(), pos.getPos().getY(), pos.getPos().getZ(), pos.getDir());
        }

        dataChanged();
        networkPackMK2(150);
    }

    public void onLeverPull() {
        FluidType type = tanks[0].getTankType();
        boolean resize = doesResizeCompressor();

        if (type == Fluids.STEAM) {
            tanks[0].setTankType(Fluids.HOTSTEAM);
            tanks[1].setTankType(Fluids.STEAM);
            if (resize) shrinkTanks(10);
        } else if (type == Fluids.HOTSTEAM) {
            tanks[0].setTankType(Fluids.SUPERHOTSTEAM);
            tanks[1].setTankType(Fluids.HOTSTEAM);
            if (resize) shrinkTanks(10);
        } else if (type == Fluids.SUPERHOTSTEAM) {
            tanks[0].setTankType(Fluids.ULTRAHOTSTEAM);
            tanks[1].setTankType(Fluids.SUPERHOTSTEAM);
            if (resize) shrinkTanks(10);
        } else if (type == Fluids.ULTRAHOTSTEAM) {
            tanks[0].setTankType(Fluids.STEAM);
            tanks[1].setTankType(Fluids.SPENTSTEAM);
            if (resize) growTanks(1000);
        } else {
            tanks[0].setTankType(Fluids.STEAM);
            tanks[1].setTankType(Fluids.SPENTSTEAM);
        }

        setChanged();
    }

    private void shrinkTanks(int factor) {
        tanks[0].changeTankSize(tanks[0].getMaxFill() / factor);
        tanks[1].changeTankSize(tanks[1].getMaxFill() / factor);
    }

    private void growTanks(int factor) {
        tanks[0].changeTankSize(tanks[0].getMaxFill() * factor);
        tanks[1].changeTankSize(tanks[1].getMaxFill() * factor);
    }

    @Override
    public long getPower() {
        return powerBuffer;
    }

    @Override
    public long getMaxPower() {
        return powerBuffer;
    }

    @Override
    public void setPower(long power) {
        this.powerBuffer = power;
    }

    @Override
    public List<FluidTankNTM> getSendingTanks() {
        return List.of(tanks[1]);
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(tanks[0]);
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(tanks);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tanks[0].writeToNBT(tag, "water");
        tanks[1].writeToNBT(tag, "steam");
        tag.putLong("power", powerBuffer);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tanks[0].readFromNBT(tag, "water");
        tanks[1].readFromNBT(tag, "steam");
        powerBuffer = tag.getLong("power");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
        buf.writeLong(powerBuffer);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
        powerBuffer = buf.readLong();
    }
}
