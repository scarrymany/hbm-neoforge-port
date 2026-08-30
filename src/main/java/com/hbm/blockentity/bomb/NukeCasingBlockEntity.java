package com.hbm.blockentity.bomb;

import com.hbm.blockentity.MachineBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Shared base for the 9 concrete nuke-casing block entities (+ {@code NukeCustom}), ported from the
 * common shape every {@code TileEntityNuke*} in CE shares (read individually in full - see
 * {@code docs/phase3/bomb_blocks_and_detonators.md} Section B): a plain {@code ItemStackHandler}
 * inventory (owned by {@link MachineBaseBlockEntity}), a {@code placerID} UUID fallback used when the
 * casing is redstone-triggered rather than detonator-triggered, and a {@code clearSlots()} helper
 * called right before every detonation. {@link MenuProvider} is implemented here (rather than per
 * concrete class) since {@link MachineBaseBlockEntity#getDisplayName()} already supplies
 * {@code getDisplayName()} - each subclass need only add {@code createMenu(...)}.
 * <p>
 * {@code NukeBalefireBlockEntity} deliberately does NOT extend this class - CE's
 * {@code TileEntityNukeBalefire} is a countdown-timer {@code TileEntityMachineBase} subclass with its
 * own {@code started}/{@code timer} tick state and a different {@code placerID} save/load shape
 * layered onto that ticking base, closer to a generic Phase 2 machine than to the other 8 flat-check
 * casings - see that class's own javadoc.
 */
public abstract class NukeCasingBlockEntity extends MachineBaseBlockEntity implements MenuProvider {

    @Nullable
    public UUID placerID;

    protected NukeCasingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int scount) {
        super(type, pos, state, scount, 64, false, false);
    }

    protected NukeCasingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int scount, int slotlimit) {
        super(type, pos, state, scount, slotlimit, false, false);
    }

    public void clearSlots() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (placerID != null) tag.putUUID("placer", placerID);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("placer")) placerID = tag.getUUID("placer");
    }
}
