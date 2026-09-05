package com.hbm.blockentity.machine.workshop;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.container.machine.workshop.AmmoPressMenu;
import com.hbm.inventory.recipes.AmmoPressRecipes;
import com.hbm.inventory.recipes.AmmoPressRecipes.AmmoPressRecipe;
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

import java.util.List;

/**
 * Exact CE {@code TileEntityMachineAmmoPress}: selected recipe index + 9-slot grid.
 * Animation TESR stay skipped.
 */
public class AmmoPressBlockEntity extends MachineBaseBlockEntity
        implements ITickableBE, MenuProvider, IControlReceiver {

    public static final int SLOT_OUT = 9;

    public int selectedRecipe = -1;

    public AmmoPressBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 10, true, true);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineAmmoPress");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        // CE TileEntityMachineAmmoPress.java:192-198
        if (slot > 8) return false;
        List<AmmoPressRecipe> recipes = AmmoPressRecipes.getAllRecipes();
        if (selectedRecipe < 0 || selectedRecipe >= recipes.size()) return false;
        AStack need = recipes.get(selectedRecipe).input(slot);
        if (need == null) return false;
        return need.matchesRecipe(stack, true);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == SLOT_OUT;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        performRecipe();
        dataChanged();
        networkPackMK2(25);
    }

    /** Exact CE {@code TileEntityMachineAmmoPress.performRecipe} :131-147. */
    public void performRecipe() {
        List<AmmoPressRecipe> recipes = AmmoPressRecipes.getAllRecipes();
        if (selectedRecipe < 0 || selectedRecipe >= recipes.size()) return;
        AmmoPressRecipe recipe = recipes.get(selectedRecipe);
        ItemStack stack = inventory.getStackInSlot(SLOT_OUT);
        if (!stack.isEmpty()) {
            if (!ItemStack.isSameItem(stack, recipe.output)) return;
            if (stack.getCount() + recipe.output.getCount() > stack.getMaxStackSize()) return;
        }
        if (hasIngredients(recipe)) {
            produceAmmo(recipe);
            performRecipe();
        }
    }

    public boolean hasIngredients(AmmoPressRecipe recipe) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            AStack need = recipe.input(i);
            if (need == null && stack.isEmpty()) continue;
            if (need != null && stack.isEmpty()) return false;
            if (need == null && !stack.isEmpty()) return false;
            if (need != null && !need.matchesRecipe(stack, false)) return false;
        }
        return true;
    }

    protected void produceAmmo(AmmoPressRecipe recipe) {
        for (int i = 0; i < 9; i++) {
            AStack need = recipe.input(i);
            if (need != null) inventory.extractItem(i, need.stacksize, false);
        }
        ItemStack dest = inventory.getStackInSlot(SLOT_OUT);
        if (dest.isEmpty()) inventory.setStackInSlot(SLOT_OUT, recipe.output.copy());
        else dest.grow(recipe.output.getCount());
    }

    @Override
    public boolean hasPermission(Player player) {
        return isUseableByPlayer(player);
    }

    /** Exact CE {@code TileEntityMachineAmmoPress.receiveControl} :233-238. */
    @Override
    public void receiveControl(CompoundTag data) {
        int newRecipe = data.getInt("selection");
        if (newRecipe == selectedRecipe) this.selectedRecipe = -1;
        else this.selectedRecipe = newRecipe;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("recipe", selectedRecipe);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        selectedRecipe = tag.getInt("recipe");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(selectedRecipe);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        selectedRecipe = buf.readInt();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AmmoPressMenu(id, inv, this);
    }
}
