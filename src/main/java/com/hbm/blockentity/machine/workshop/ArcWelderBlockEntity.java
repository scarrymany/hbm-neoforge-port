package com.hbm.blockentity.machine.workshop;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.machine.workshop.ArcWelderMenu;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.ArcWelderRecipes;
import com.hbm.inventory.recipes.ArcWelderRecipes.ArcWelderRecipe;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * CE {@code TileEntityMachineArcWelder}: maxPower 2_000 (grows with recipe), 3 inputs + out + battery.
 * {@code tank.setType(5)} Exact CE {@code :121}. Slot 5 Exact CE
 * {@code ContainerMachineArcWelder.java:43}. Slots 6-7 upgrades Exact CE {@code :149-206}
 * / {@code ContainerMachineArcWelder.java:45-46}. Tau/Hadron VFX / IUpgradeInfoProvider stay skipped.
 */
public class ArcWelderBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardReceiverMK2, ITickableBE, IPersistentNBT, MenuProvider {

    public static final int SLOT_OUT = 3;
    public static final int SLOT_BATTERY = 4;
    public static final int SLOT_ID = 5;
    public static final int SLOT_UPGRADE_A = 6;
    public static final int SLOT_UPGRADE_B = 7;
    public static final long BASE_MAX = 2_000L;
    public static final int TANK_CAPACITY = 24_000;

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = new EnumMap<>(UpgradeType.class);

    static {
        VALID_UPGRADES.put(UpgradeType.SPEED, 3);
        VALID_UPGRADES.put(UpgradeType.POWER, 3);
        VALID_UPGRADES.put(UpgradeType.OVERDRIVE, 3);
    }

    public final FluidTankNTM tank;
    public long power;
    public long maxPower = BASE_MAX;
    public int progress;
    public int processTime = 1;
    public long consumption;
    public boolean isProcessing;
    private final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(VALID_UPGRADES);

    public ArcWelderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 8, true, true);
        tank = new FluidTankNTM(Fluids.NONE, TANK_CAPACITY).withOwner(this);
    }

    @Override
    protected ItemStackHandler getNewInventory(int scount, int slotlimit) {
        return new ItemStackHandler(scount) {
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                setChanged();
            }

            @Override
            public void setStackInSlot(int slot, ItemStack stack) {
                super.setStackInSlot(slot, stack);
                // CE TileEntityMachineArcWelder.java:91-104
                if (!stack.isEmpty() && slot >= SLOT_UPGRADE_A && slot <= SLOT_UPGRADE_B
                        && stack.getItem() instanceof ItemMachineUpgrade && level != null && !level.isClientSide) {
                    level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                            HBMSoundHandler.upgradePlug.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }

            @Override
            public int getSlotLimit(int slot) {
                return slotlimit;
            }
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineArcWelder");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return Library.isBattery(stack);
        // CE :372-374 returns false for slot 5; without this the ID never lands and setType is dead.
        if (slot == SLOT_ID) return stack.getItem() instanceof IItemFluidIdentifier;
        if (slot == SLOT_UPGRADE_A || slot == SLOT_UPGRADE_B) return stack.getItem() instanceof ItemMachineUpgrade;
        return slot < SLOT_OUT;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == SLOT_OUT;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 2, 3};
    }

    public int getProgressScaled(int i) {
        return (progress * i) / Math.max(1, processTime);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        power = Library.chargeTEFromItems(inventory, SLOT_BATTERY, getPower(), getMaxPower());
        // CE TileEntityMachineArcWelder.java:121
        this.tank.setType(SLOT_ID, inventory);
        for (Direction d : Direction.values()) {
            trySubscribe(level, worldPosition.relative(d), d);
            if (tank.getTankType() != Fluids.NONE) {
                trySubscribe(tank.getTankType(), level, new DirPos(worldPosition.relative(d), d));
            }
        }

        ArcWelderRecipe recipe = ArcWelderRecipes.getRecipe(
                inventory.getStackInSlot(0), inventory.getStackInSlot(1), inventory.getStackInSlot(2));

        // CE TileEntityMachineArcWelder.java:149-206
        upgradeManager.checkSlots(inventory, SLOT_UPGRADE_A, SLOT_UPGRADE_B);
        int redLevel = upgradeManager.getLevel(UpgradeType.SPEED);
        int blueLevel = upgradeManager.getLevel(UpgradeType.POWER);
        int blackLevel = upgradeManager.getLevel(UpgradeType.OVERDRIVE);

        long intendedMaxPower;
        if (recipe != null) {
            processTime = recipe.duration - (recipe.duration * redLevel / 6) + (recipe.duration * blueLevel / 3);
            consumption = recipe.consumption + (recipe.consumption * redLevel) - (recipe.consumption * blueLevel / 6);
            consumption *= (long) Math.pow(2, blackLevel);
            intendedMaxPower = consumption * 20;

            if (canProcess(recipe)) {
                isProcessing = true;
                progress += (1 + blackLevel);
                power -= consumption;
                if (progress >= processTime) {
                    progress = 0;
                    consume(recipe);
                    ItemStack have = inventory.getStackInSlot(SLOT_OUT);
                    if (have.isEmpty()) {
                        inventory.setStackInSlot(SLOT_OUT, recipe.output.copy());
                    } else {
                        have.grow(recipe.output.getCount());
                    }
                }
            } else {
                progress = 0;
                isProcessing = false;
            }
        } else {
            progress = 0;
            isProcessing = false;
            consumption = 100;
            intendedMaxPower = BASE_MAX;
        }
        maxPower = Math.max(intendedMaxPower, power);
        // CE :181-194 Tau/Hadron AuxParticle — VFX skip
        dataChanged();
        networkPackMK2(25);
    }

    private boolean hasFluid(ArcWelderRecipe recipe) {
        if (recipe.fluid == null) return true;
        return tank.getTankType() == recipe.fluid.type && tank.getFill() >= recipe.fluid.fill;
    }

    private boolean canProcess(ArcWelderRecipe recipe) {
        if (power < consumption) return false;
        if (!hasFluid(recipe)) return false;
        ItemStack have = inventory.getStackInSlot(SLOT_OUT);
        if (have.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(have, recipe.output)
                && have.getCount() + recipe.output.getCount() <= have.getMaxStackSize();
    }

    private void consume(ArcWelderRecipe recipe) {
        List<AStack> left = new ArrayList<>(List.of(recipe.ingredients));
        for (int i = 0; i < 3 && !left.isEmpty(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            for (int j = 0; j < left.size(); j++) {
                AStack key = left.get(j);
                if (key.matchesRecipe(stack, true) && stack.getCount() >= key.count()) {
                    inventory.extractItem(i, key.count(), false);
                    left.remove(j);
                    break;
                }
            }
        }
        if (recipe.fluid != null) tank.setFill(tank.getFill() - recipe.fluid.fill);
    }

    @Override
    public long getPower() {
        // CE TileEntityMachineArcWelder.java:347-348
        return Math.max(Math.min(power, maxPower), 0);
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
        if (tank.getPressure() != pressure) return 0;
        if (tank.getTankType() == type || (tank.getTankType() == Fluids.NONE && tank.getFill() == 0)) {
            return tank.getMaxFill() - tank.getFill();
        }
        return 0;
    }

    @Override
    public long transferFluid(FluidType type, int pressure, long amount) {
        if (tank.getPressure() != pressure) return amount;
        if (tank.getTankType() == Fluids.NONE && tank.getFill() == 0) tank.setTankType(type);
        if (tank.getTankType() != type) return amount;
        int toAdd = (int) Math.min(amount, tank.getMaxFill() - tank.getFill());
        tank.setFill(tank.getFill() + toAdd);
        return amount - toAdd;
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(tank);
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(tank);
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
        tag.putInt("processTime", processTime);
        tank.writeToNBT(tag, "t");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        if (tag.contains("maxPower")) maxPower = tag.getLong("maxPower");
        progress = tag.getInt("progress");
        if (tag.contains("processTime")) processTime = tag.getInt("processTime");
        tank.readFromNBT(tag, "t");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        tank.serialize(buf);
        buf.writeLong(power);
        buf.writeBoolean(isProcessing);
        buf.writeInt(progress);
        buf.writeInt(processTime);
        buf.writeLong(maxPower);
        buf.writeLong(consumption);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tank.deserialize(buf);
        power = buf.readLong();
        isProcessing = buf.readBoolean();
        progress = buf.readInt();
        processTime = buf.readInt();
        maxPower = buf.readLong();
        consumption = buf.readLong();
    }

    @Override
    public void writeNBT(CompoundTag nbt) {
        tank.writeToNBT(nbt, "nt");
    }

    @Override
    public void readNBT(CompoundTag nbt) {
        tank.readFromNBT(nbt, "nt");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ArcWelderMenu(id, inv, this);
    }
}
