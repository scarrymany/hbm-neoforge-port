package com.hbm.blockentity.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE: TileEntityRBMKNumitron - numitron display panel with 2 numeric displays.
 * Each reads RTTY signal + shows as 7-segment numitron. Full CE: polling, labels, channels, OC.
 * 1.21: minimal stub - no RTTY yet, no GUI. Saves dummy data.
 * TODO(CE: TileEntityRBMKNumitron.java:1-293): RTTY polling, DisplayUnit[] state, GUI (GUIScreenRBMKDisplay).
 */
public class RBMKNumitronBlockEntity extends BlockEntity {

    public RBMKNumitronBlockEntity(BlockPos pos, BlockState state) {
        super(RBMKBlockEntities.RBMK_NUMITRON.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // TODO: DisplayUnit[2] state
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // TODO: DisplayUnit[2] state
    }
}
