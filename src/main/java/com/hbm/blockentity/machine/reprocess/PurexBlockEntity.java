package com.hbm.blockentity.machine.reprocess;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.container.machine.reprocess.PurexMenu;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.PUREXRecipes;
import com.hbm.inventory.recipes.PUREXRecipes.PUREXRecipe;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * CE {@code TileEntityMachinePUREX.java}: 1M HE floor, recipe.power*100 cap, 3×24k in / 1×24k out.
 * Auto-detects from inventory+tanks (no CE dropdown / blueprint).
 */
public class PurexBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, ITickableBE, IPersistentNBT, MenuProvider {

    private static final int ITEM_IN_START = 0;
    private static final int ITEM_OUT_START = 3;
    private static final int ITEM_OUT_COUNT = 6;
    private static final int BATTERY_SLOT = 9;

    public static final long MIN_MAX_POWER = 1_000_000L;
    public static final int TANK_CAPACITY = 24_000;

    public final FluidTankNTM[] inputTanks = new FluidTankNTM[3];
    public final FluidTankNTM outputTank;

    public long power;
    public long maxPower = MIN_MAX_POWER;
    public int progress;
    public boolean isProcessing;
    private String activeRecipeName;

    public PurexBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 10, true, true);
        for (int i = 0; i < 3; i++) {
            inputTanks[i] = new FluidTankNTM(Fluids.NONE, TANK_CAPACITY).withOwner(this);
        }
        outputTank = new FluidTankNTM(Fluids.NONE, TANK_CAPACITY).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machinePUREX");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == BATTERY_SLOT) return Library.isBattery(stack);
        return slot >= ITEM_IN_START && slot < ITEM_IN_START + 3;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot >= ITEM_OUT_START && slot < ITEM_OUT_START + ITEM_OUT_COUNT;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
    }

    public int getProgressScaled(int i) {
        PUREXRecipe recipe = findRecipe();
        int duration = recipe == null ? 1 : recipe.duration;
        return (progress * i) / Math.max(1, duration);
    }

    private PUREXRecipe findRecipe() {
        for (PUREXRecipe recipe : PUREXRecipes.RECIPES) {
            if (matchesItems(recipe) && matchesFluids(recipe)) return recipe;
        }
        return null;
    }

    @Nullable
    private PUREXRecipe findItemOnlyRecipe() {
        for (PUREXRecipe recipe : PUREXRecipes.RECIPES) {
            if (matchesItems(recipe)) return recipe;
        }
        return null;
    }

    private boolean matchesItems(PUREXRecipe recipe) {
        boolean[] used = new boolean[3];
        for (AStack key : recipe.inputItems) {
            boolean found = false;
            for (int i = 0; i < 3; i++) {
                if (used[i]) continue;
                if (key.matchesRecipe(inventory.getStackInSlot(ITEM_IN_START + i), false)) {
                    used[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    private FluidTankNTM findInputTank(FluidType type) {
        for (FluidTankNTM tank : inputTanks) {
            if (tank.getTankType() == type) return tank;
        }
        return null;
    }

    private boolean matchesFluids(PUREXRecipe recipe) {
        for (FluidStack need : recipe.inputFluids) {
            FluidTankNTM tank = findInputTank(need.type);
            if (tank == null || tank.getFill() < need.fill) return false;
        }
        return true;
    }

    private boolean hasOutputSpace(PUREXRecipe recipe) {
        for (int i = 0; i < recipe.outputItems.length && i < ITEM_OUT_COUNT; i++) {
            if (!inventory.insertItem(ITEM_OUT_START + i, recipe.outputItems[i], true).isEmpty()) return false;
        }
        if (recipe.outputFluid != null) {
            FluidStack out = recipe.outputFluid;
            if (!((outputTank.getTankType() == out.type || outputTank.getTankType() == Fluids.NONE)
                    && outputTank.getFill() + out.fill <= outputTank.getMaxFill())) {
                return false;
            }
        }
        return true;
    }

    private void process(PUREXRecipe recipe) {
        boolean[] used = new boolean[3];
        for (AStack key : recipe.inputItems) {
            for (int i = 0; i < 3; i++) {
                if (used[i]) continue;
                if (key.matchesRecipe(inventory.getStackInSlot(ITEM_IN_START + i), false)) {
                    inventory.extractItem(ITEM_IN_START + i, key.count(), false);
                    used[i] = true;
                    break;
                }
            }
        }
        for (FluidStack need : recipe.inputFluids) {
            FluidTankNTM tank = findInputTank(need.type);
            if (tank != null) tank.setFill(tank.getFill() - need.fill);
        }
        for (int i = 0; i < recipe.outputItems.length && i < ITEM_OUT_COUNT; i++) {
            inventory.insertItem(ITEM_OUT_START + i, recipe.outputItems[i].copy(), false);
        }
        if (recipe.outputFluid != null) {
            FluidStack out = recipe.outputFluid;
            if (outputTank.getTankType() == Fluids.NONE) outputTank.setTankType(out.type);
            outputTank.setFill(outputTank.getFill() + out.fill);
        }
    }

    private void retargetEmptyTanks(PUREXRecipe recipe) {
        for (FluidStack need : recipe.inputFluids) {
            if (findInputTank(need.type) != null) continue;
            for (FluidTankNTM tank : inputTanks) {
                if (tank.getFill() == 0) {
                    tank.setTankType(need.type);
                    break;
                }
            }
        }
    }

    public DirPos[] getConPos() {
        BlockPos p = worldPosition;
        return new DirPos[]{
                new DirPos(p.getX() + 1, p.getY(), p.getZ(), Direction.EAST),
                new DirPos(p.getX() - 1, p.getY(), p.getZ(), Direction.WEST),
                new DirPos(p.getX(), p.getY(), p.getZ() + 1, Direction.SOUTH),
                new DirPos(p.getX(), p.getY(), p.getZ() - 1, Direction.NORTH)
        };
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        power = Library.chargeTEFromItems(inventory, BATTERY_SLOT, power, maxPower);

        PUREXRecipe itemOnly = findItemOnlyRecipe();
        if (itemOnly != null) retargetEmptyTanks(itemOnly);

        for (DirPos dp : getConPos()) {
            trySubscribe(level, dp);
            for (FluidTankNTM tank : inputTanks) if (tank.getTankType() != Fluids.NONE) trySubscribe(tank.getTankType(), level, dp);
            if (outputTank.getFill() > 0) tryProvide(outputTank, level, dp);
        }

        PUREXRecipe recipe = findRecipe();
        if (recipe == null) {
            progress = 0;
            isProcessing = false;
            activeRecipeName = null;
            maxPower = Math.max(power, MIN_MAX_POWER);
        } else {
            activeRecipeName = recipe.name;
            maxPower = Math.max(Math.max(power, recipe.power * 100), MIN_MAX_POWER);

            if (power >= recipe.power && hasOutputSpace(recipe)) {
                isProcessing = true;
                progress++;
                power -= recipe.power;
                if (progress >= recipe.duration) {
                    process(recipe);
                    progress = 0;
                }
            } else {
                isProcessing = false;
            }
        }

        dataChanged();
        networkPackMK2(100);
    }

    public String getActiveRecipeName() {
        return activeRecipeName;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long i) {
        power = i;
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    public long getDemand(FluidType type, int pressure) {
        long amount = 0;
        for (FluidTankNTM tank : getReceivingTanks()) {
            if (tank.getPressure() != pressure) continue;
            if (tank.getTankType() == type || (tank.getTankType() == Fluids.NONE && tank.getFill() == 0)) {
                amount += tank.getMaxFill() - tank.getFill();
            }
        }
        return amount;
    }

    @Override
    public long transferFluid(FluidType type, int pressure, long amount) {
        for (FluidTankNTM tank : getReceivingTanks()) {
            if (tank.getPressure() != pressure) continue;
            if (tank.getTankType() == Fluids.NONE && tank.getFill() == 0) tank.setTankType(type);
            if (tank.getTankType() != type) continue;
            int toAdd = (int) Math.min(amount, tank.getMaxFill() - tank.getFill());
            tank.setFill(tank.getFill() + toAdd);
            amount -= toAdd;
            if (amount <= 0) break;
        }
        return amount;
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(inputTanks);
    }

    @Override
    public List<FluidTankNTM> getSendingTanks() {
        return List.of(outputTank);
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(inputTanks[0], inputTanks[1], inputTanks[2], outputTank);
    }

    @Override
    public boolean canConnect(FluidType type, Direction dir) {
        return dir != null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putLong("maxPower", maxPower);
        tag.putInt("progress", progress);
        for (int i = 0; i < 3; i++) inputTanks[i].writeToNBT(tag, "i" + i);
        outputTank.writeToNBT(tag, "o");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        maxPower = Math.max(tag.getLong("maxPower"), MIN_MAX_POWER);
        progress = tag.getInt("progress");
        for (int i = 0; i < 3; i++) inputTanks[i].readFromNBT(tag, "i" + i);
        outputTank.readFromNBT(tag, "o");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        for (FluidTankNTM tank : inputTanks) tank.serialize(buf);
        outputTank.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(maxPower);
        buf.writeBoolean(isProcessing);
        buf.writeInt(progress);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        for (FluidTankNTM tank : inputTanks) tank.deserialize(buf);
        outputTank.deserialize(buf);
        power = buf.readLong();
        maxPower = buf.readLong();
        isProcessing = buf.readBoolean();
        progress = buf.readInt();
    }

    @Override
    public void writeNBT(CompoundTag nbt) {
        for (int i = 0; i < 3; i++) inputTanks[i].writeToNBT(nbt, "ni" + i);
        outputTank.writeToNBT(nbt, "no");
    }

    @Override
    public void readNBT(CompoundTag nbt) {
        for (int i = 0; i < 3; i++) inputTanks[i].readFromNBT(nbt, "ni" + i);
        outputTank.readFromNBT(nbt, "no");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PurexMenu(containerId, playerInventory, this);
    }
}
