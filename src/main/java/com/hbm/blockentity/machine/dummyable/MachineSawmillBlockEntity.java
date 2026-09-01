package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.tile.IHeatSource;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.SawmillMenu;
import com.hbm.inventory.recipes.SawmillRecipes;
import com.hbm.items.BilletPowderItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE {@code TileEntitySawmill.java}:77-110 — heat-driven wood process, processingTime 600.
 * Blade / entity shred / overspeed explosion skipped ({@code sawblade} unregistered → blade assumed).
 */
public class MachineSawmillBlockEntity extends MachineBaseBlockEntity implements ITickableBE, MenuProvider {

    public static final int PROCESS_TIME = 600;
    public static final double DIFFUSION = 0.1D;

    public int heat;
    public int progress;

    public MachineSawmillBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 3, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineSawmill");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && SawmillRecipes.isInput(stack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot > 0;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 2};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        tryPullHeat();
        if (heat >= 100) {
            ItemStack result = SawmillRecipes.getOutput(inventory.getStackInSlot(0), level);
            if (!result.isEmpty() && canFit(1, result)) {
                progress += heat / 10;
                if (progress >= PROCESS_TIME) {
                    progress = 0;
                    float chance = chanceFor(result);
                    inventory.extractItem(0, 1, false);
                    insertOut(1, result);
                    if (chance > 0F && level.random.nextFloat() < chance) {
                        ItemStack dust = new ItemStack(BilletPowderItems.POWDER_SAWDUST.get());
                        if (canFit(2, dust)) insertOut(2, dust);
                    }
                    setChanged();
                }
            } else {
                progress = 0;
            }
        } else {
            progress = 0;
        }

        dataChanged();
        networkPackMK2(50);
    }

    private float chanceFor(ItemStack output) {
        if (output.is(net.minecraft.world.item.Items.STICK)) return 0.1F;
        if (output.is(net.minecraft.tags.ItemTags.PLANKS)) return 0.5F;
        return 0F;
    }

    private boolean canFit(int slot, ItemStack stack) {
        ItemStack dest = inventory.getStackInSlot(slot);
        if (dest.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(dest, stack)) return false;
        return dest.getCount() + stack.getCount() <= dest.getMaxStackSize();
    }

    private void insertOut(int slot, ItemStack stack) {
        ItemStack dest = inventory.getStackInSlot(slot);
        if (dest.isEmpty()) inventory.setStackInSlot(slot, stack.copy());
        else dest.grow(stack.getCount());
    }

    private void tryPullHeat() {
        if (level == null) return;
        BlockEntity below = level.getBlockEntity(worldPosition.below());
        if (below instanceof IHeatSource source) {
            int pulled = (int) (source.getHeatStored() * DIFFUSION);
            if (pulled > 0) {
                source.useUpHeat(pulled);
                heat += pulled;
                return;
            }
        }
        heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("heat", heat);
        tag.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        heat = tag.getInt("heat");
        progress = tag.getInt("progress");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(heat);
        buf.writeInt(progress);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        heat = buf.readInt();
        progress = buf.readInt();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new SawmillMenu(id, inv, this);
    }
}
