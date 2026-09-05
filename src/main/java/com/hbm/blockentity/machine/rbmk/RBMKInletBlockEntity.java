package com.hbm.blockentity.machine.rbmk;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.api.rbmk.RBMKDials;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.blocks.machine.rbmk.RBMKBaseBlock;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Standalone water-feed pipe — not an RBMK grid column. Exact CE {@code TileEntityRBMKInlet.java:32-56}:
 * subscribe all 6 faces, and when {@code getReasimBoilers} push water into adjacent column cores
 * ({@code maxWater} room). {@code rbmk_loader} stays skipped.
 */
public class RBMKInletBlockEntity extends LoadedBaseBlockEntity implements IFluidStandardReceiverMK2, ITickableBE {

    /** CE {@code ForgeDirection.getOrientation(2..5)} — N/S/W/E. */
    private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    public final FluidTankNTM water;

    public RBMKInletBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        water = new FluidTankNTM(Fluids.WATER, 32_000).withOwner(this);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        for (Direction dir : Direction.values()) {
            trySubscribe(water.getTankType(), level, worldPosition.relative(dir), dir);
        }

        if (level instanceof ServerLevel serverLevel && RBMKDials.getReasimBoilers(serverLevel)) {
            for (Direction dir : HORIZONTAL) {
                BlockPos npos = worldPosition.relative(dir);
                if (level.getBlockState(npos).getBlock() instanceof RBMKBaseBlock rbmkBlock) {
                    BlockPos core = rbmkBlock.findCore(level, npos);
                    if (core != null && level.getBlockEntity(core) instanceof RBMKBaseBlockEntity rbmk) {
                        int prov = Math.min(RBMKBaseBlockEntity.maxWater - rbmk.reasimWater, water.getFill());
                        rbmk.reasimWater += prov;
                        water.setFill(water.getFill() - prov);
                    }
                }
            }
        }

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
