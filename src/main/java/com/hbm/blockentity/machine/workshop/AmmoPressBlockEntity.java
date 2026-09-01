package com.hbm.blockentity.machine.workshop;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.container.machine.workshop.AmmoPressMenu;
import com.hbm.inventory.fluid.FluidStack;
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

/**
 * CE {@code TileEntityMachineAmmoPress}: 9-slot positional grid + output. No energy.
 * Auto-matches first recipe whose 9 slots fit (CE uses a GUI recipe index).
 */
public class AmmoPressBlockEntity extends MachineBaseBlockEntity
        implements ITickableBE, MenuProvider {

    public static final int SLOT_OUT = 9;

    public boolean isProcessing;
    public int lastRecipe = -1;

    public AmmoPressBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 10, true, true);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineAmmoPress");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot < SLOT_OUT;
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
        AmmoPressRecipe recipe = findRecipe();
        if (recipe == null || !canOutput(recipe)) {
            isProcessing = false;
            lastRecipe = -1;
            return;
        }
        consume(recipe);
        inventory.insertItem(SLOT_OUT, recipe.output.copy(), false);
        isProcessing = true;
        lastRecipe = AmmoPressRecipes.getAllRecipes().indexOf(recipe);
    }

    private AmmoPressRecipe findRecipe() {
        for (AmmoPressRecipe recipe : AmmoPressRecipes.getAllRecipes()) {
            if (hasIngredients(recipe)) return recipe;
        }
        return null;
    }

    private boolean hasIngredients(AmmoPressRecipe recipe) {
        for (int i = 0; i < 9; i++) {
            Object slot = recipe.slots[i];
            ItemStack stack = inventory.getStackInSlot(i);
            if (slot == null) {
                if (!stack.isEmpty()) return false;
                continue;
            }
            if (slot instanceof FluidStack) return false;
            if (!(slot instanceof AStack key)) return false;
            if (!key.matchesRecipe(stack, false)) return false;
        }
        return true;
    }

    private boolean canOutput(AmmoPressRecipe recipe) {
        return inventory.insertItem(SLOT_OUT, recipe.output.copy(), true).isEmpty();
    }

    private void consume(AmmoPressRecipe recipe) {
        for (int i = 0; i < 9; i++) {
            if (recipe.slots[i] instanceof AStack key) {
                inventory.extractItem(i, key.count(), false);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("recipe", lastRecipe);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        lastRecipe = tag.getInt("recipe");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(isProcessing);
        buf.writeInt(lastRecipe);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        isProcessing = buf.readBoolean();
        lastRecipe = buf.readInt();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AmmoPressMenu(id, inv, this);
    }
}
