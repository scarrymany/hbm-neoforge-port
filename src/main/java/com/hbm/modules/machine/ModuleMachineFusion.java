package com.hbm.modules.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.FusionRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.items.ItemStackHandler;

/** CE {@code ModuleMachineFusion} — consumes fluids during progress, collector bonus bar. */
public class ModuleMachineFusion extends ModuleMachineBase {

    public double processSpeed = 1D;
    public double bonusSpeed = 0D;
    public double bonus;

    public ModuleMachineFusion(int index, IEnergyHandlerMK2 battery, ItemStackHandler slots) {
        super(index, battery, slots);
        this.inputSlots = new int[0];
        this.outputSlots = new int[1];
        this.inputTanks = new FluidTankNTM[3];
        this.outputTanks = new FluidTankNTM[1];
    }

    @Override
    public GenericRecipes getRecipeSet() {
        return FusionRecipes.SET;
    }

    public ModuleMachineFusion itemOutput(int slot) {
        outputSlots[0] = slot;
        return this;
    }

    public ModuleMachineFusion fluidInput(FluidTankNTM a, FluidTankNTM b, FluidTankNTM c) {
        inputTanks[0] = a;
        inputTanks[1] = b;
        inputTanks[2] = c;
        return this;
    }

    public ModuleMachineFusion fluidOutput(FluidTankNTM a) {
        outputTanks[0] = a;
        return this;
    }

    public void preUpdate(double processSpeed, double bonusSpeed) {
        this.processSpeed = processSpeed;
        this.bonusSpeed = bonusSpeed;
    }

    @Override
    protected boolean hasInput(GenericRecipe recipe) {
        if (processSpeed <= 0) return false;
        if (recipe.inputFluid != null) {
            for (int i = 0; i < Math.min(recipe.inputFluid.length, inputTanks.length); i++) {
                if (inputTanks[i].getFill() > 0 && inputTanks[i].getFill() < (int) Math.ceil(recipe.inputFluid[i].fill * processSpeed)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void process(GenericRecipe recipe, double speed, double power) {
        this.battery.setPower(this.battery.getPower() - (long) Math.ceil((power == 1 ? recipe.power : (long) (recipe.power * power)) * processSpeed));
        double step = Math.min(speed / recipe.duration * processSpeed, 1D);
        this.progress += step;
        this.bonus += step * this.bonusSpeed;
        this.bonus = Math.min(this.bonus, 1.5D);

        if (recipe.inputFluid != null) {
            for (int i = 0; i < Math.min(recipe.inputFluid.length, inputTanks.length); i++) {
                inputTanks[i].setFill(Math.max(inputTanks[i].getFill() - (int) Math.ceil(recipe.inputFluid[i].fill * processSpeed), 0));
            }
        }

        if (this.progress >= 1D) {
            produceItem(recipe);
            if (this.canProcess(recipe, speed, power)) this.progress -= 1D;
            else this.progress = 0D;
        }

        if (this.bonus >= 1D && this.canFitOutput(recipe)) {
            produceItem(recipe);
            this.bonus -= 1D;
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeDouble(bonus);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.bonus = buf.readDouble();
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        this.bonus = nbt.getDouble("bonus" + index);
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putDouble("bonus" + index, bonus);
    }
}
