package com.hbm.modules.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.machine.ItemBlueprints;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

/** CE {@code ModuleMachineBase} — shared generic-recipe process loop. */
public abstract class ModuleMachineBase {

    public int index;
    public IEnergyHandlerMK2 battery;
    public ItemStackHandler inventory;
    public int[] inputSlots;
    public int[] outputSlots;
    public FluidTankNTM[] inputTanks;
    public FluidTankNTM[] outputTanks;
    public String recipe = "null";
    public double progress;
    public boolean didProcess = false;
    public boolean markDirty = false;
    public boolean restrictedMode = false;

    public ModuleMachineBase(int index, IEnergyHandlerMK2 battery, ItemStackHandler inventory) {
        this.index = index;
        this.battery = battery;
        this.inventory = inventory;
    }

    public void setupTanks(GenericRecipe recipe) {
        if (recipe == null) return;
        for (int i = 0; i < inputTanks.length; i++) {
            if (recipe.inputFluid != null && recipe.inputFluid.length > i) inputTanks[i].conform(recipe.inputFluid[i]);
            else inputTanks[i].resetTank();
        }
        for (int i = 0; i < outputTanks.length; i++) {
            if (recipe.outputFluid != null && recipe.outputFluid.length > i) outputTanks[i].conform(recipe.outputFluid[i]);
            else outputTanks[i].resetTank();
        }
    }

    public boolean canProcess(GenericRecipe recipe, double speed, double power) {
        if (recipe == null) return false;

        if (recipe.autoSwitchGroup != null && inputSlots.length > 0 && !inventory.getStackInSlot(inputSlots[0]).isEmpty()) {
            ItemStack itemToSwitchBy = inventory.getStackInSlot(inputSlots[0]);
            List<GenericRecipe> recipes = this.getRecipeSet().autoSwitchGroups.get(recipe.autoSwitchGroup);
            if (recipes != null) {
                for (GenericRecipe nextRec : recipes) {
                    if (nextRec.getInternalName().equals(this.recipe)) continue;
                    if (nextRec.inputItem == null) continue;
                    if (nextRec.inputItem[0].matchesRecipe(itemToSwitchBy, true)) {
                        this.recipe = nextRec.getInternalName();
                        return false;
                    }
                }
            }
        }

        if (power != 1 && battery.getPower() < recipe.power * power) return false;
        if (power == 1 && battery.getPower() < recipe.power) return false;
        if (!hasInput(recipe)) return false;
        return canFitOutput(recipe);
    }

    protected boolean hasInput(GenericRecipe recipe) {
        if (recipe.inputItem != null) {
            for (int i = 0; i < Math.min(recipe.inputItem.length, inputSlots.length); i++) {
                if (!recipe.inputItem[i].matchesRecipe(inventory.getStackInSlot(inputSlots[i]), false)) return false;
            }
        }
        if (recipe.inputFluid != null) {
            for (int i = 0; i < Math.min(recipe.inputFluid.length, inputTanks.length); i++) {
                if (inputTanks[i].getFill() < recipe.inputFluid[i].fill) return false;
            }
        }
        return true;
    }

    protected boolean canFitOutput(GenericRecipe recipe) {
        if (recipe.outputItem != null) {
            for (int i = 0; i < Math.min(recipe.outputItem.length, outputSlots.length); i++) {
                if (findOutputSlot(recipe, i) < 0) return false;
            }
        }
        if (recipe.outputFluid != null) {
            for (int i = 0; i < Math.min(recipe.outputFluid.length, outputTanks.length); i++) {
                if (recipe.outputFluid[i].fill + outputTanks[i].getFill() > outputTanks[i].getMaxFill()) return false;
            }
        }
        return true;
    }

    protected int findOutputSlot(GenericRecipe recipe, int outputIndex) {
        return findOutputSlot(recipe, outputIndex, null);
    }

    protected int findOutputSlot(GenericRecipe recipe, int outputIndex, ItemStack resolved) {
        GenericRecipes.IOutput output = recipe.outputItem[outputIndex];
        boolean isLast = outputIndex == recipe.outputItem.length - 1;
        int toSlot = isLast ? outputSlots.length - 1 : outputIndex;
        ItemStack single = output.possibleMultiOutput() ? resolved : output.getSingle();

        int firstEmpty = -1;
        for (int s = outputIndex; s <= toSlot; s++) {
            ItemStack stack = inventory.getStackInSlot(outputSlots[s]);
            if (stack.isEmpty()) {
                if (firstEmpty < 0) firstEmpty = s;
                continue;
            }
            if (single == null || single.isEmpty()) continue;
            if (stack.getItem() != single.getItem()) continue;
            if (stack.getCount() + single.getCount() > stack.getMaxStackSize()) continue;
            return s;
        }
        return firstEmpty;
    }

    public void process(GenericRecipe recipe, double speed, double power) {
        if (this.restrictedMode) speed *= 0.25;
        this.battery.setPower(this.battery.getPower() - (power == 1 ? recipe.power : (long) (recipe.power * power)));
        double step = Math.min(speed / recipe.duration, 1D);
        this.progress += step;
        if (this.progress >= 1D) {
            consumeInput(recipe);
            produceItem(recipe);
            if (this.canProcess(recipe, speed, power)) this.progress -= 1D;
            else this.progress = 0D;
        }
    }

    protected void consumeInput(GenericRecipe recipe) {
        if (recipe.inputItem != null) {
            for (int i = 0; i < Math.min(recipe.inputItem.length, inputSlots.length); i++) {
                int idx = inputSlots[i];
                ItemStack in = inventory.getStackInSlot(idx);
                if (!in.isEmpty()) {
                    in.shrink(recipe.inputItem[i].stacksize);
                    if (in.getCount() <= 0) inventory.setStackInSlot(idx, ItemStack.EMPTY);
                    else inventory.setStackInSlot(idx, in);
                }
            }
        }
        if (recipe.inputFluid != null) {
            for (int i = 0; i < Math.min(recipe.inputFluid.length, inputTanks.length); i++) {
                inputTanks[i].setFill(inputTanks[i].getFill() - recipe.inputFluid[i].fill);
            }
        }
    }

    protected void produceItem(GenericRecipe recipe) {
        if (recipe.outputItem != null) {
            for (int i = 0; i < Math.min(recipe.outputItem.length, outputSlots.length); i++) {
                ItemStack collapse = recipe.outputItem[i].collapse();
                int slot = findOutputSlot(recipe, i, collapse);
                if (slot < 0) continue;
                int idx = outputSlots[slot];
                ItemStack out = inventory.getStackInSlot(idx);
                if (out.isEmpty()) {
                    inventory.setStackInSlot(idx, collapse == null ? ItemStack.EMPTY : collapse);
                } else if (collapse != null && !collapse.isEmpty()) {
                    out.grow(collapse.getCount());
                    inventory.setStackInSlot(idx, out);
                }
            }
        }
        if (recipe.outputFluid != null) {
            for (int i = 0; i < Math.min(recipe.outputFluid.length, outputTanks.length); i++) {
                outputTanks[i].setFill(outputTanks[i].getFill() + recipe.outputFluid[i].fill);
            }
        }
        this.markDirty = true;
    }

    public String getRecipeName() {
        return this.recipe;
    }

    public GenericRecipe getRecipe() {
        return getRecipeSet().recipeNameMap.get(this.recipe);
    }

    public void setRecipe(String name, boolean ror) {
        this.recipe = name;
        this.restrictedMode = ror;
    }

    public abstract GenericRecipes getRecipeSet();

    public void update(double speed, double power, boolean extraCondition, ItemStack blueprint) {
        GenericRecipe recipe = getRecipe();
        if (recipe != null && recipe.isPooled() && !recipe.isPartOfPool(ItemBlueprints.grabPool(blueprint))) {
            this.didProcess = false;
            this.progress = 0F;
            this.recipe = "null";
            return;
        }
        this.setupTanks(recipe);
        this.didProcess = false;
        this.markDirty = false;
        if (extraCondition && this.canProcess(recipe, speed, power)) {
            this.process(recipe, speed, power);
            this.didProcess = true;
        } else {
            this.progress = 0F;
        }
    }

    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeDouble(progress);
        buf.writeBoolean(restrictedMode);
        buf.writeUtf(recipe);
    }

    public void deserialize(RegistryFriendlyByteBuf buf) {
        this.progress = buf.readDouble();
        this.restrictedMode = buf.readBoolean();
        this.recipe = buf.readUtf();
    }

    public void readFromNBT(CompoundTag nbt) {
        this.progress = nbt.getDouble("progress" + index);
        this.recipe = nbt.getString("recipe" + index);
        this.restrictedMode = nbt.getBoolean("restrictedMode" + index);
    }

    public void writeToNBT(CompoundTag nbt) {
        nbt.putDouble("progress" + index, progress);
        nbt.putString("recipe" + index, recipe);
        nbt.putBoolean("restrictedMode" + index, restrictedMode);
    }
}
