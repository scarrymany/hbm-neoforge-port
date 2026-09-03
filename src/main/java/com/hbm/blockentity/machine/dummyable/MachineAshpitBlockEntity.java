package com.hbm.blockentity.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.AshpitMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE {@code TileEntityAshpit} — 5 output slots, door animation.
 * TODO(CE): ash→powder conversion deferred (powder_ash items not registered yet).
 */
public class MachineAshpitBlockEntity extends MachineBaseBlockEntity
        implements ITickableBE, MenuProvider {

    public int playersUsing = 0;
    public float doorAngle = 0;
    public float prevDoorAngle = 0;

    public MachineAshpitBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 5, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.ashpit");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return true;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 2, 3, 4};
    }

    @Override
    public void updateEntity() {
        if (level == null) return;

        if (!level.isClientSide) {
            // TODO(CE): ash→powder conversion deferred (powder_ash items not registered yet)
            dataChanged();
            networkPackMK2(50);
        } else {
            // CE TileEntityAshpit.java:102-114: door animation
            prevDoorAngle = doorAngle;
            float swingSpeed = (doorAngle / 10F) + 3;

            if (playersUsing > 0) {
                doorAngle += swingSpeed;
            } else {
                doorAngle -= swingSpeed;
            }

            doorAngle = Mth.clamp(doorAngle, 0F, 135F);
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AshpitMenu(id, inv, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("playersUsing", playersUsing);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        playersUsing = tag.getInt("playersUsing");
    }
}
