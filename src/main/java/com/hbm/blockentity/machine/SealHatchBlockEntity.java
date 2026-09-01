package com.hbm.blockentity.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blocks.machine.BlockSeal;
import com.hbm.blocks.machine.SealBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE {@code TileEntityHatch} ({@code TileEntityHatch.java}). No caps / no GUI.
 * Tick: missing controller or {@code getFrameSize==0} → air.
 */
public class SealHatchBlockEntity extends BlockEntity implements ITickableBE {

    private BlockPos controller;

    public SealHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        if (controller == null || level == null || level.isClientSide) return;
        if (level.getBlockState(controller).getBlock() != SealBlocks.SEAL_CONTROLLER.get()
                || BlockSeal.getFrameSize(level, controller) == 0) {
            level.setBlock(worldPosition, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (controller != null) tag.putLong("controller", controller.asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("controller")) controller = BlockPos.of(tag.getLong("controller"));
    }

    public void setControllerPos(BlockPos pos) {
        this.controller = pos;
        setChanged();
    }
}
