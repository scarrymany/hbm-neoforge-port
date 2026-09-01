package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.machine.MachineDetectorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE {@code TileEntityMachineDetector} — HIGH priority, 30 HE, 1 HE/t to stay on. No CE container.
 */
public class MachineDetectorBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, ITickableBE {

    public static final long MAX_POWER = 30L;
    public long power;

    public MachineDetectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 0, false, true);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm.machine_detector");
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        for (Direction d : Direction.values()) trySubscribe(level, worldPosition.relative(d), d);

        boolean on = power > 0;
        if (on) power -= 1;

        BlockState state = getBlockState();
        if (state.hasProperty(MachineDetectorBlock.IS_ON) && state.getValue(MachineDetectorBlock.IS_ON) != on) {
            level.setBlock(worldPosition, state.setValue(MachineDetectorBlock.IS_ON, on), 3);
        }
        setChanged();
    }

    @Override
    public ConnectionPriority getPriority() {
        return ConnectionPriority.HIGH;
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
        return MAX_POWER;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
    }
}
