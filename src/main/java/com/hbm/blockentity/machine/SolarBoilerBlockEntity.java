package com.hbm.blockentity.machine;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Ported from CE's {@code TileEntitySolarBoiler} (block {@code MachineSolarBoiler}, regname
 * {@code machine_solar_boiler}, dimensions {@code {2,0,1,1,1,1}}, read in full): converts
 * externally-supplied {@link #heatInput} (fed by a paired {@link SolarMirrorBlockEntity} via direct
 * field write, exactly like CE) into {@link Fluids#STEAM} at a fixed 1 heat unit : ~50 water : 100
 * steam ratio (CE's own {@code tryConvert}: {@code process = heat/50}, capped by both tanks). No
 * inventory, no GUI, no HE - this is a pure {@link IFluidStandardTransceiverMK2} fluid producer that
 * must be piped into a turbine to become power, per the research report's headline finding.
 */
public class SolarBoilerBlockEntity extends MachineBaseBlockEntity implements IFluidStandardTransceiverMK2, ITickableBE {

    public static final int MAX_HEAT = 320_000;

    public final FluidTankNTM[] tanks;
    public int heat;
    /** Written directly by a paired {@link SolarMirrorBlockEntity}, exactly like CE. */
    public int heatInput;

    public SolarBoilerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 0, true, false);
        tanks = new FluidTankNTM[]{
                new FluidTankNTM(Fluids.WATER, 16_000).withOwner(this),
                new FluidTankNTM(Fluids.STEAM, 1_600_000).withOwner(this)
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.solarBoiler");
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        BlockPos up3 = worldPosition.above(3);
        BlockPos down1 = worldPosition.below();
        this.trySubscribe(tanks[0].getTankType(), level, up3, Direction.DOWN);
        this.trySubscribe(tanks[0].getTankType(), level, down1, Direction.UP);

        int process = heat / 50;
        process = Math.min(process, tanks[0].getFill());
        process = Math.min(process, (tanks[1].getMaxFill() - tanks[1].getFill()) / 100);
        tanks[0].setFill(tanks[0].getFill() - process);
        tanks[1].setFill(tanks[1].getFill() + process * 100);
        heat = 0;

        this.tryProvide(tanks[1], level, up3, Direction.UP);
        this.tryProvide(tanks[1], level, down1, Direction.DOWN);

        heat += heatInput;
        if (heat > MAX_HEAT) heat = MAX_HEAT;
        heat = (int) (heat * 0.999D);
        heatInput = 0;

        dataChanged();
        networkPackMK2(25);
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
        tag.putInt("heat", heat);
        tanks[0].writeToNBT(tag, "tank0");
        tanks[1].writeToNBT(tag, "tank1");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        heat = tag.getInt("heat");
        tanks[0].readFromNBT(tag, "tank0");
        tanks[1].readFromNBT(tag, "tank1");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(heat);
        buf.writeInt(heatInput);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        heat = buf.readInt();
        heatInput = buf.readInt();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
    }
}
