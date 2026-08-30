package com.hbm.blockentity.machine.rbmk;

import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
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
 * Standalone superheated-steam export pipe stub for a reactor's boiler columns - not an RBMK grid
 * column, mirrors {@link RBMKInletBlockEntity}. Ported from CE's {@code TileEntityRBMKOutlet}
 * (99 lines, signature-level survey).
 */
public class RBMKOutletBlockEntity extends LoadedBaseBlockEntity implements IFluidStandardSenderMK2, ITickableBE {

    public final FluidTankNTM steam;

    public RBMKOutletBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        steam = new FluidTankNTM(Fluids.SUPERHOTSTEAM, 32_000).withOwner(this);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        tryProvide(steam, level, worldPosition.below(), Direction.UP);
        dataChanged();
        networkPackMK2(25);
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(steam);
    }

    @Override
    public List<FluidTankNTM> getSendingTanks() {
        return List.of(steam);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        steam.writeToNBT(tag, "steam");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        steam.readFromNBT(tag, "steam");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        steam.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        steam.deserialize(buf);
    }
}
