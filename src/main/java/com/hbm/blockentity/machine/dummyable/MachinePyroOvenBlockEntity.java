package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.container.machine.dummyable.PyroOvenMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.PyroOvenRecipes;
import com.hbm.inventory.recipes.PyroOvenRecipes.PyroOvenRecipe;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
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
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * CE {@code TileEntityMachinePyroOven}: 10k HE base, 2×24k tanks, SPEED/POWER/OVERDRIVE via slot scan.
 * Pollution / audio / particles skipped.
 */
public class MachinePyroOvenBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, ITickableBE, MenuProvider {

    public static final long MAX_POWER = 10_000_000;
    public static final int CONSUMPTION = 10_000;

    public final FluidTankNTM input;
    public final FluidTankNTM output;
    public long power;
    public boolean isProgressing;
    public float progress;
    private PyroOvenRecipe lastValidRecipe;

    public MachinePyroOvenBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 6, true, true);
        this.input = new FluidTankNTM(Fluids.NONE, 24_000).withOwner(this);
        this.output = new FluidTankNTM(Fluids.NONE, 24_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machinePyroOven");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        if (slot == 1) return !(stack.getItem() instanceof IItemFluidIdentifier) && !(stack.getItem() instanceof ItemMachineUpgrade);
        if (slot == 3) return stack.getItem() instanceof IItemFluidIdentifier;
        if (slot == 4 || slot == 5) return stack.getItem() instanceof ItemMachineUpgrade;
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == 2;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{1, 2};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        power = Library.chargeTEFromItems(inventory, 0, power, MAX_POWER);
        ItemStack id = inventory.getStackInSlot(3);
        if (!id.isEmpty() && id.getItem() instanceof IItemFluidIdentifier ident) {
            input.setTankType(ident.getType(level, worldPosition, id));
        }

        int speed = upgrade(UpgradeType.SPEED);
        int powerSaving = upgrade(UpgradeType.POWER);
        int overdrive = upgrade(UpgradeType.OVERDRIVE);

        isProgressing = false;
        if (canProcess(speed, powerSaving)) {
            PyroOvenRecipe recipe = getMatchingRecipe();
            progress += 1F / Math.max((recipe.duration - speed * (recipe.duration / 4)) / (overdrive * 2 + 1), 1);
            isProgressing = true;
            power -= getConsumption(speed + overdrive * 2, powerSaving);
            if (progress >= 1F) {
                progress = 0F;
                finishRecipe(recipe);
                setChanged();
            }
        } else {
            progress = 0F;
        }

        if (level.getGameTime() % 20 == 0) {
            for (DirPos pos : getConPos()) {
                trySubscribe(level, pos);
                if (input.getTankType() != Fluids.NONE) trySubscribe(input.getTankType(), level, pos);
            }
        }
        for (DirPos pos : getConPos()) {
            if (output.getFill() > 0) tryProvide(output, level, pos);
        }
        dataChanged();
        networkPackMK2(50);
    }

    public static int getConsumption(int speed, int powerSaving) {
        return (int) (CONSUMPTION * Math.pow(speed + 1, 2)) / (powerSaving + 1);
    }

    private int upgrade(UpgradeType type) {
        int level = 0;
        for (int s = 4; s <= 5; s++) {
            ItemStack st = inventory.getStackInSlot(s);
            if (st.getItem() instanceof ItemMachineUpgrade u && u.getType() == type) {
                level = Math.max(level, u.getTier());
            }
        }
        return Math.min(level, 3);
    }

    public PyroOvenRecipe getMatchingRecipe() {
        if (lastValidRecipe != null && doesRecipeMatch(lastValidRecipe)) return lastValidRecipe;
        for (PyroOvenRecipe rec : PyroOvenRecipes.getAllRecipes()) {
            if (doesRecipeMatch(rec)) {
                lastValidRecipe = rec;
                return rec;
            }
        }
        return null;
    }

    public boolean doesRecipeMatch(PyroOvenRecipe recipe) {
        if (recipe.inputFluid != null && input.getTankType() != recipe.inputFluid.type) return false;
        if (recipe.inputItem != null) {
            if (inventory.getStackInSlot(1).isEmpty()) return false;
            return recipe.inputItem.matchesRecipe(inventory.getStackInSlot(1), true);
        }
        return inventory.getStackInSlot(1).isEmpty();
    }

    public boolean canProcess(int speed, int powerSaving) {
        if (power < getConsumption(speed, powerSaving)) return false;
        PyroOvenRecipe recipe = getMatchingRecipe();
        if (recipe == null) return false;
        if (recipe.inputFluid != null && input.getFill() < recipe.inputFluid.fill) return false;
        if (recipe.inputItem != null && inventory.getStackInSlot(1).getCount() < recipe.inputItem.stacksize) return false;
        if (recipe.outputFluid != null) {
            if (output.getTankType() != Fluids.NONE && output.getTankType() != recipe.outputFluid.type) return false;
            if (recipe.outputFluid.fill + output.getFill() > output.getMaxFill()) return false;
        }
        if (recipe.outputItem != null && !inventory.getStackInSlot(2).isEmpty()) {
            ItemStack dest = inventory.getStackInSlot(2);
            if (!ItemStack.isSameItemSameComponents(dest, recipe.outputItem)) return false;
            return dest.getCount() + recipe.outputItem.getCount() <= dest.getMaxStackSize();
        }
        return true;
    }

    public void finishRecipe(PyroOvenRecipe recipe) {
        if (recipe.outputItem != null) {
            ItemStack dest = inventory.getStackInSlot(2);
            if (dest.isEmpty()) inventory.setStackInSlot(2, recipe.outputItem.copy());
            else dest.grow(recipe.outputItem.getCount());
        }
        if (recipe.outputFluid != null) {
            output.setTankType(recipe.outputFluid.type);
            output.setFill(output.getFill() + recipe.outputFluid.fill);
        }
        if (recipe.inputItem != null) inventory.extractItem(1, recipe.inputItem.stacksize, false);
        if (recipe.inputFluid != null) input.setFill(input.getFill() - recipe.inputFluid.fill);
    }

    public DirPos[] getConPos() {
        Direction dir = coreFacing();
        Direction rot = dir.getCounterClockWise();
        return new DirPos[]{
                new DirPos(worldPosition.getX() + dir.getStepX() * 2 + rot.getStepX() * 3, worldPosition.getY(), worldPosition.getZ() + dir.getStepZ() * 2 + rot.getStepZ() * 3, rot),
                new DirPos(worldPosition.getX() + dir.getStepX() + rot.getStepX() * 3, worldPosition.getY(), worldPosition.getZ() + dir.getStepZ() + rot.getStepZ() * 3, rot),
                new DirPos(worldPosition.getX() + rot.getStepX() * 3, worldPosition.getY(), worldPosition.getZ() + rot.getStepZ() * 3, rot),
                new DirPos(worldPosition.getX() - dir.getStepX() + rot.getStepX() * 3, worldPosition.getY(), worldPosition.getZ() - dir.getStepZ() + rot.getStepZ() * 3, rot),
                new DirPos(worldPosition.getX() - dir.getStepX() * 2 + rot.getStepX() * 3, worldPosition.getY(), worldPosition.getZ() - dir.getStepZ() * 2 + rot.getStepZ() * 3, rot),
        };
    }

    private Direction coreFacing() {
        int meta = getBlockState().getValue(BlockDummyable.META);
        return meta >= 12 ? Direction.from3DDataValue(meta - BlockDummyable.offset) : Direction.NORTH;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(input);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(output);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(input, output);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putFloat("prog", progress);
        input.writeToNBT(tag, "t0");
        output.writeToNBT(tag, "t1");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        progress = tag.getFloat("prog");
        input.readFromNBT(tag, "t0");
        output.readFromNBT(tag, "t1");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeBoolean(isProgressing);
        buf.writeFloat(progress);
        input.serialize(buf);
        output.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        isProgressing = buf.readBoolean();
        progress = buf.readFloat();
        input.deserialize(buf);
        output.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new PyroOvenMenu(id, inv, this);
    }
}
