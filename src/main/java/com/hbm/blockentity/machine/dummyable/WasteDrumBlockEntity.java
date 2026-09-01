package com.hbm.blockentity.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.WasteDrumMenu;
import com.hbm.inventory.recipes.WasteDrumRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE {@code TileEntityWasteDrum}: 12 slots, water-adjacency cooling.
 */
public class WasteDrumBlockEntity extends MachineBaseBlockEntity implements ITickableBE, MenuProvider {

    private int water;

    public WasteDrumBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 12, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.wasteDrum");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return WasteDrumRecipes.isInput(stack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return !WasteDrumRecipes.isInput(stack);
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    }

    public void updateWater() {
        if (level == null) return;
        water = 0;
        for (Direction d : Direction.values()) {
            if (level.getBlockState(worldPosition.relative(d)).is(Blocks.WATER)
                    || level.getBlockState(worldPosition.relative(d)).is(Blocks.BUBBLE_COLUMN)) {
                water++;
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        updateWater();
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        if (water <= 0) return;
        int r = 60 * 60 * 20 / water;
        for (int i = 0; i < 12; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            if (level.random.nextInt(r) != 0) continue;
            ItemStack out = WasteDrumRecipes.getOutput(stack);
            if (!out.isEmpty()) {
                inventory.setStackInSlot(i, out);
                level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.0F);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("water", water);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        water = tag.getInt("water");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new WasteDrumMenu(id, inv, this);
    }
}
