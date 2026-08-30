package com.hbm.blockentity.machine.rbmk;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Standalone water-feed pipe stub for a reactor's boiler columns - not itself an RBMK grid column
 * (CE's {@code TileEntityRBMKInlet} {@code extends TileEntityLoadedBase}, not
 * {@code TileEntityRBMKBase}). Ported from CE's {@code TileEntityRBMKInlet} (102 lines,
 * signature-level survey) onto this port's {@code LoadedBaseBlockEntity}/{@code fluidmk2} API.
 */
public class RBMKInletBlockEntity extends LoadedBaseBlockEntity implements IFluidStandardReceiverMK2, ITickableBE {

    public final FluidTankNTM water;

    public RBMKInletBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        water = new FluidTankNTM(Fluids.WATER, 32_000).withOwner(this);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        trySubscribe(water.getTankType(), level, worldPosition.above(), Direction.DOWN);
        dataChanged();
        networkPackMK2(25);
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(water);
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(water);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        water.writeToNBT(tag, "water");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        water.readFromNBT(tag, "water");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        water.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        water.deserialize(buf);
    }
}
