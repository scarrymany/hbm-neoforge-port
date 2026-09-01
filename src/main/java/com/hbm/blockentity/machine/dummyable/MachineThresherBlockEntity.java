package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.machine.MachineThresherBlock;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * CE {@code TileEntityMachineThresher} — WOODOIL 100 mB, 1 mB/s, harvest mature crops in front.
 * Arm animation / entity shred / tall-plant / cane TODO(CE: TileEntityMachineThresher.java:101-204).
 */
public class MachineThresherBlockEntity extends MachineBaseBlockEntity
        implements IFluidStandardReceiverMK2, ITickableBE {

    public final FluidTankNTM tank;
    public boolean isOn;
    public boolean isSuspended;

    public MachineThresherBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 0, true, false);
        this.tank = new FluidTankNTM(Fluids.WOODOIL, 100).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm.machine_thresher");
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        Direction facing = getBlockState().hasProperty(MachineThresherBlock.FACING)
                ? getBlockState().getValue(MachineThresherBlock.FACING) : Direction.NORTH;
        Direction rot = facing.getClockWise();

        if (!isSuspended && level.getGameTime() % 20 == 0) {
            if (tank.getFill() > 0) {
                tank.setFill(tank.getFill() - 1);
                isOn = true;
            } else {
                isOn = false;
            }
            trySubscribe(tank.getTankType(), level, new DirPos(worldPosition.relative(rot), rot));
            trySubscribe(tank.getTankType(), level, new DirPos(worldPosition.relative(rot.getOpposite()), rot.getOpposite()));
            trySubscribe(tank.getTankType(), level, new DirPos(worldPosition.below(), Direction.DOWN));
        }

        if (isOn && !isSuspended) {
            harvestCrops(facing, rot);
        }

        dataChanged();
        networkPackMK2(50);
    }

    private void harvestCrops(Direction facing, Direction rot) {
        if (!(level instanceof ServerLevel server)) return;
        BlockPos origin = worldPosition.relative(facing, 4);
        for (int i = -3; i <= 3; i++) {
            BlockPos hit = origin.relative(rot, i);
            BlockState state = server.getBlockState(hit);
            if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                Block.dropResources(state, server, hit);
                server.setBlock(hit, crop.getStateForAge(0), Block.UPDATE_ALL);
            } else if (state.is(Blocks.SUGAR_CANE) || state.is(Blocks.CACTUS)) {
                Block.dropResources(state, server, hit);
                server.removeBlock(hit, false);
            }
        }
    }

    public void toggleSuspended() {
        isSuspended = !isSuspended;
        setChanged();
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(tank);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(tank);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("suspended", isSuspended);
        tank.writeToNBT(tag, "tank");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        isSuspended = tag.getBoolean("suspended");
        tank.readFromNBT(tag, "tank");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(isOn);
        buf.writeBoolean(isSuspended);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        isOn = buf.readBoolean();
        isSuspended = buf.readBoolean();
        tank.deserialize(buf);
    }
}
