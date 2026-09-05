package com.hbm.blockentity.machine.workshop;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.machine.workshop.SolderingMenu;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.SolderingRecipes;
import com.hbm.inventory.recipes.SolderingRecipes.SolderingRecipe;
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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * CE {@code TileEntityMachineSolderingStation}: maxPower 2_000, slots 0-2 toppings / 3-4 pcb / 5 solder.
 * {@code tank.setType(8)} Exact CE {@code :123}. Slots 9-10 upgrades Exact CE {@code :156-168}
 * / {@code ContainerMachineSolderingStation.java:39-41}. Collision-prevention Exact CE
 * {@code TileEntityMachineSolderingStation} {@code :69}/{@code :229}/{@code :514-516}. Tau VFX stay skipped.
 */
public class SolderingBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardReceiverMK2, ITickableBE, IPersistentNBT, MenuProvider, IControlReceiver {

    public static final int SLOT_OUT = 6;
    public static final int SLOT_BATTERY = 7;
    public static final int SLOT_ID = 8;
    public static final int SLOT_UPGRADE_A = 9;
    public static final int SLOT_UPGRADE_B = 10;
    public static final long BASE_MAX = 2_000L;
    public static final int TANK_CAPACITY = 8_000;

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
    public boolean collisionPrevention;
    private final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(VALID_UPGRADES);

    public SolderingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 11, true, true);
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
                // CE TileEntityMachineSolderingStation.java:91-106
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
        return Component.translatable("container.machineSolderingStation");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return Library.isBattery(stack);
        // CE :281-301 returns false for slot 8; without this the ID never lands and setType is dead.
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
        return new int[]{0, 1, 2, 3, 4, 5, 6};
    }

    public int getProgressScaled(int i) {
        return (progress * i) / Math.max(1, processTime);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        power = Library.chargeTEFromItems(inventory, SLOT_BATTERY, getPower(), getMaxPower());
        // CE TileEntityMachineSolderingStation.java:123
        this.tank.setType(SLOT_ID, inventory);
        for (Direction d : Direction.values()) {
            trySubscribe(level, worldPosition.relative(d), d);
            if (tank.getTankType() != Fluids.NONE) {
                trySubscribe(tank.getTankType(), level, new DirPos(worldPosition.relative(d), d));
            }
        }

        ItemStack[] ins = new ItemStack[6];
        for (int i = 0; i < 6; i++) ins[i] = inventory.getStackInSlot(i);
        SolderingRecipe recipe = SolderingRecipes.getRecipe(ins);

        // CE TileEntityMachineSolderingStation.java:156-214
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
                    inventory.insertItem(SLOT_OUT, recipe.output.copy(), false);
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
    }

    private boolean hasFluid(SolderingRecipe recipe) {
        if (recipe.fluid == null) return true;
        return tank.getTankType() == recipe.fluid.type && tank.getFill() >= recipe.fluid.fill;
    }

    /** Exact CE {@code TileEntityMachineSolderingStation.canProcess} :220-238. */
    private boolean canProcess(SolderingRecipe recipe) {
        if (power < consumption) return false;
        if (!hasFluid(recipe)) return false;
        if (collisionPrevention && recipe.fluid == null && tank.getFill() > 0) return false;
        return canOutput(recipe);
    }

    private boolean canOutput(SolderingRecipe recipe) {
        return inventory.insertItem(SLOT_OUT, recipe.output.copy(), true).isEmpty();
    }

    private void consumeGroup(AStack[] keys, int start, int len) {
        boolean[] used = new boolean[len];
        for (AStack key : keys) {
            for (int i = 0; i < len; i++) {
                if (used[i]) continue;
                if (key.matchesRecipe(inventory.getStackInSlot(start + i), false)) {
                    inventory.extractItem(start + i, key.count(), false);
                    used[i] = true;
                    break;
                }
            }
        }
    }

    private void consume(SolderingRecipe recipe) {
        consumeGroup(recipe.toppings, 0, 3);
        consumeGroup(recipe.pcb, 3, 2);
        consumeGroup(recipe.solder, 5, 1);
        if (recipe.fluid != null) tank.setFill(tank.getFill() - recipe.fluid.fill);
    }

    @Override
    public long getPower() {
        // CE TileEntityMachineSolderingStation.java:404-406
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
        tag.putBoolean("collisionPrevention", collisionPrevention);
        tank.writeToNBT(tag, "t");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        if (tag.contains("maxPower")) maxPower = tag.getLong("maxPower");
        progress = tag.getInt("progress");
        if (tag.contains("processTime")) processTime = tag.getInt("processTime");
        collisionPrevention = tag.getBoolean("collisionPrevention");
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
        buf.writeBoolean(collisionPrevention);
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
        collisionPrevention = buf.readBoolean();
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
    public boolean hasPermission(Player player) {
        return isUseableByPlayer(player);
    }

    /** Exact CE {@code TileEntityMachineSolderingStation.receiveControl} :514-516. */
    @Override
    public void receiveControl(CompoundTag data) {
        this.collisionPrevention = !this.collisionPrevention;
        setChanged();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new SolderingMenu(id, inv, this);
    }
}
