package com.hbm.blockentity.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.container.machine.dummyable.DiFurnaceRtgMenu;
import com.hbm.inventory.recipes.BlastFurnaceRecipesNT;
import com.hbm.inventory.recipes.BlastFurnaceRecipesNT.BlastFurnaceRecipe;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE {@code TileEntityDiFurnaceRTG} — pellet heat + NT alloy. Decay / block-swap skipped.
 */
public class MachineDiFurnaceRtgBlockEntity extends MachineBaseBlockEntity implements ITickableBE, MenuProvider {

    public static final int MAX_HEAT = 6000;
    public static final int PROCESS = 2400;

    public int progress;
    public int heat;

    public MachineDiFurnaceRtgBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 9, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.diFurnaceRTG");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 2) return false;
        if (slot >= 3) return stack.getItem() instanceof ItemRTGPellet;
        return BlastFurnaceRecipesNT.INSTANCE.isIngredient(stack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == 2;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        if (side == Direction.DOWN) return new int[]{2};
        if (side == Direction.UP) return new int[]{0, 1};
        return new int[]{3, 4, 5, 6, 7, 8};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        heat = 0;
        for (int i = 3; i <= 8; i++) {
            ItemStack s = inventory.getStackInSlot(i);
            if (s.getItem() instanceof ItemRTGPellet pellet) heat += pellet.getHeat();
        }
        if (heat > MAX_HEAT) heat = MAX_HEAT;

        if (heat > 0 && canProcess()) {
            progress += heat;
            if (progress >= PROCESS) {
                progress = 0;
                process();
            }
        } else {
            progress = 0;
        }

        dataChanged();
        networkPackMK2(15);
    }

    private boolean canProcess() {
        ItemStack a = inventory.getStackInSlot(0);
        ItemStack b = inventory.getStackInSlot(1);
        if (a.isEmpty() || b.isEmpty()) return false;
        BlastFurnaceRecipe recipe = BlastFurnaceRecipesNT.INSTANCE.getRecipe(a, b);
        if (recipe == null || recipe.inputs.length < 2) return false;
        AStack in0 = recipe.inputs[0];
        AStack in1 = recipe.inputs[1];
        boolean qty = (in0.matchesRecipe(a, false) && in1.matchesRecipe(b, false))
                || (in0.matchesRecipe(b, false) && in1.matchesRecipe(a, false));
        if (!qty) return false;
        ItemStack dest = inventory.getStackInSlot(2);
        ItemStack out = recipe.outputs.length > 0 ? recipe.outputs[0] : ItemStack.EMPTY;
        if (out.isEmpty()) return false;
        if (dest.isEmpty()) return true;
        return ItemStack.isSameItem(dest, out) && dest.getCount() + out.getCount() <= dest.getMaxStackSize();
    }

    private void process() {
        BlastFurnaceRecipe recipe = BlastFurnaceRecipesNT.INSTANCE.getRecipe(inventory.getStackInSlot(0), inventory.getStackInSlot(1));
        if (recipe == null || recipe.inputs.length < 2) return;
        AStack in0 = recipe.inputs[0];
        AStack in1 = recipe.inputs[1];
        if (in0.matchesRecipe(inventory.getStackInSlot(0), false) && in1.matchesRecipe(inventory.getStackInSlot(1), false)) {
            inventory.extractItem(0, in0.count(), false);
            inventory.extractItem(1, in1.count(), false);
        } else {
            inventory.extractItem(0, in1.count(), false);
            inventory.extractItem(1, in0.count(), false);
        }
        ItemStack out = recipe.outputs[0].copy();
        ItemStack dest = inventory.getStackInSlot(2);
        if (dest.isEmpty()) inventory.setStackInSlot(2, out);
        else dest.grow(out.getCount());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("progress", progress);
        tag.putInt("rtgPower", heat);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        progress = tag.getInt("progress");
        heat = tag.getInt("rtgPower");
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
        return new DiFurnaceRtgMenu(id, inv, this);
    }
}
