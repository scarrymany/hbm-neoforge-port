package com.hbm.blockentity.bomb;

import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.bomb.BombMultiMenu;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.machine.Phase11ProcessItems;
import com.hbm.items.special.SpecialItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code TileEntityBombMulti} (195 lines). 6-slot inventory for custom-bomb formula:
 * slots 0,1,3,4 = TNT corners (must all be TNT for isLoaded), slots 2,5 = modifiers (gunpowder/TNT/pellet_cluster/powder_fire/powder_poison/pellet_gas).
 */
public class BombMultiBlockEntity extends MachineBaseBlockEntity implements MenuProvider {

    public BombMultiBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 6, true, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.bombMulti");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return false;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[0];
    }

    public boolean isLoaded() {
        ItemStack s0 = inventory.getStackInSlot(0);
        ItemStack s1 = inventory.getStackInSlot(1);
        ItemStack s3 = inventory.getStackInSlot(3);
        ItemStack s4 = inventory.getStackInSlot(4);

        return !s0.isEmpty() && s0.is(Blocks.TNT.asItem())
                && !s1.isEmpty() && s1.is(Blocks.TNT.asItem())
                && !s3.isEmpty() && s3.is(Blocks.TNT.asItem())
                && !s4.isEmpty() && s4.is(Blocks.TNT.asItem());
    }

    public int return2type() {
        ItemStack stack = inventory.getStackInSlot(2);
        if (stack.isEmpty()) return 0;
        if (stack.is(Items.GUNPOWDER)) return 1;
        if (stack.is(Blocks.TNT.asItem())) return 2;
        // CE TileEntityBombMulti.java:102-105 / :139-142
        if (stack.is(Phase11ProcessItems.PELLET_CLUSTER.get())) return 3;
        if (stack.is(BilletPowderItems.POWDER_FIRE.get())) return 4;
        if (stack.is(BilletPowderItems.POWDER_POISON.get())) return 5;
        // CE TileEntityBombMulti.java:117-120 / :154-157
        if (stack.is(SpecialItems.PELLET_GAS.get())) return 6;
        return 0;
    }

    public int return5type() {
        ItemStack stack = inventory.getStackInSlot(5);
        if (stack.isEmpty()) return 0;
        if (stack.is(Items.GUNPOWDER)) return 1;
        if (stack.is(Blocks.TNT.asItem())) return 2;
        // CE TileEntityBombMulti.java:102-105 / :139-142
        if (stack.is(Phase11ProcessItems.PELLET_CLUSTER.get())) return 3;
        if (stack.is(BilletPowderItems.POWDER_FIRE.get())) return 4;
        if (stack.is(BilletPowderItems.POWDER_POISON.get())) return 5;
        // CE TileEntityBombMulti.java:117-120 / :154-157
        if (stack.is(SpecialItems.PELLET_GAS.get())) return 6;
        return 0;
    }

    public void clearSlots() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new BombMultiMenu(id, inv, this);
    }
}
