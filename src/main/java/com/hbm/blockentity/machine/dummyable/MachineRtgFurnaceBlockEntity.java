package com.hbm.blockentity.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.RtgFurnaceMenu;
import com.hbm.items.machine.ItemRTGPellet;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * CE {@code TileEntityRtgFurnace} — pellet heat + vanilla smelt. Decay / block-swap skipped.
 */
public class MachineRtgFurnaceBlockEntity extends MachineBaseBlockEntity implements ITickableBE, MenuProvider {

    public static final int MAX_PROGRESS = 3000;

    public int progress;
    public int heat;

    public MachineRtgFurnaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 5, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rtgFurnace");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return smelt(stack).isPresent();
        if (slot >= 1 && slot <= 3) return stack.getItem() instanceof ItemRTGPellet;
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == 4 || (slot < 4 && !(stack.getItem() instanceof ItemRTGPellet));
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        if (side == Direction.DOWN) return new int[]{4};
        if (side == Direction.UP) return new int[]{0};
        return new int[]{1, 2, 3};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        heat = 0;
        for (int i = 1; i <= 3; i++) {
            ItemStack s = inventory.getStackInSlot(i);
            if (s.getItem() instanceof ItemRTGPellet pellet) heat += pellet.getHeat();
        }

        if (heat > 0 && canProcess()) {
            progress += heat;
            if (progress >= MAX_PROGRESS) {
                progress = 0;
                process();
            }
        } else {
            progress = 0;
        }

        dataChanged();
        networkPackMK2(50);
    }

    private boolean canProcess() {
        Optional<ItemStack> out = smelt(inventory.getStackInSlot(0));
        if (out.isEmpty()) return false;
        ItemStack dest = inventory.getStackInSlot(4);
        if (dest.isEmpty()) return true;
        ItemStack result = out.get();
        return ItemStack.isSameItemSameComponents(dest, result)
                && dest.getCount() + result.getCount() <= dest.getMaxStackSize();
    }

    private void process() {
        Optional<ItemStack> out = smelt(inventory.getStackInSlot(0));
        if (out.isEmpty()) return;
        ItemStack result = out.get();
        ItemStack dest = inventory.getStackInSlot(4);
        if (dest.isEmpty()) inventory.setStackInSlot(4, result.copy());
        else dest.grow(result.getCount());
        inventory.extractItem(0, 1, false);
    }

    private Optional<ItemStack> smelt(ItemStack input) {
        if (level == null || input.isEmpty()) return Optional.empty();
        Optional<RecipeHolder<SmeltingRecipe>> rec = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level);
        return rec.map(h -> h.value().assemble(new SingleRecipeInput(input), level.registryAccess()));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("progress", progress);
        tag.putInt("heat", heat);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        progress = tag.getInt("progress");
        heat = tag.getInt("heat");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(progress);
        buf.writeInt(heat);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        progress = buf.readInt();
        heat = buf.readInt();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new RtgFurnaceMenu(id, inv, this);
    }
}
