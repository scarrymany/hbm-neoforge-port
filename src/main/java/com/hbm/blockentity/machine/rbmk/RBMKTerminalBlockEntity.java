package com.hbm.blockentity.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE: TileEntityRBMKTerminal - RBMK terminal display panel that scans 7x7 grid of RBMK columns.
 * Stores target coords, rotation, reads RBMKColumn data from nearby RBMK columns every 10 ticks.
 * 1.21: minimal stub - no RBMK scanning yet, no GUI.
 * TODO(CE: TileEntityRBMKTerminal.java:1-185): target coords, RBMK grid scan, RBMKColumn[], GUI.
 */
public class RBMKTerminalBlockEntity extends BlockEntity {
    private int targetX, targetY, targetZ;
    private byte rotation;

    public RBMKTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(RBMKBlockEntities.RBMK_TERMINAL.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("tX", targetX);
        tag.putInt("tY", targetY);
        tag.putInt("tZ", targetZ);
        tag.putByte("rotation", rotation);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        targetX = tag.getInt("tX");
        targetY = tag.getInt("tY");
        targetZ = tag.getInt("tZ");
        rotation = tag.getByte("rotation");
    }
}
