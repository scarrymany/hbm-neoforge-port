package com.hbm.blockentity.machine.reprocess;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.machine.reprocess.LiquefactorMenu;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.LiquefactionRecipes;
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
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * CE {@code TileEntityMachineLiquefactor.java}: maxPower 100_000, usageBase 250, processTimeBase 60,
 * tank 24_000. Slots 2-3 upgrades Exact CE {@code :91-96} / {@code ContainerLiquefactor.java:39-40}.
 * IUpgradeInfoProvider stay skipped.
 */
public class LiquefactorBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, ITickableBE, IPersistentNBT, MenuProvider {

    private static final int SLOT_IN = 0;
    private static final int SLOT_BATTERY = 1;
    public static final int SLOT_UPGRADE_A = 2;
    public static final int SLOT_UPGRADE_B = 3;

    public static final long MAX_POWER = 100_000L;
    public static final int USAGE = 250;
    public static final int PROCESS_TIME = 60;
    public static final int TANK_CAPACITY = 24_000;

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = new EnumMap<>(UpgradeType.class);

    static {
        VALID_UPGRADES.put(UpgradeType.SPEED, 3);
        VALID_UPGRADES.put(UpgradeType.POWER, 3);
    }

    public final FluidTankNTM tank;
    public long power;
    public int progress;
    public int usage = USAGE;
    public int processTime = PROCESS_TIME;
    public boolean isProcessing;
    private final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(VALID_UPGRADES);

    public LiquefactorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 4, true, true);
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
                // CE TileEntityMachineLiquefactor.java:70-71
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
        return Component.translatable("container.machineLiquefactor");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return Library.isBattery(stack);
        if (slot == SLOT_UPGRADE_A || slot == SLOT_UPGRADE_B) return stack.getItem() instanceof ItemMachineUpgrade;
        return slot == SLOT_IN && LiquefactionRecipes.getOutput(stack) != null;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return false;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{SLOT_IN};
    }

    public int getProgressScaled(int i) {
        if (processTime <= 0) return 0;
        return (progress * i) / processTime;
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

        power = Library.chargeTEFromItems(inventory, SLOT_BATTERY, power, MAX_POWER);

        for (DirPos dp : getConPos()) {
            trySubscribe(level, dp);
        }

        // CE TileEntityMachineLiquefactor.java:91-96
        upgradeManager.checkSlots(inventory, SLOT_UPGRADE_A, SLOT_UPGRADE_B);
        int speed = Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3);
        int powerLevel = Math.min(upgradeManager.getLevel(UpgradeType.POWER), 3);
        this.processTime = PROCESS_TIME - (PROCESS_TIME / 4) * speed;
        this.usage = (USAGE + (USAGE * speed)) / (powerLevel + 1);

        if (canProcess()) {
            isProcessing = true;
            power -= usage;
            progress++;
            if (progress >= processTime) {
                FluidStack out = LiquefactionRecipes.getOutput(inventory.getStackInSlot(SLOT_IN));
                tank.setTankType(out.type);
                tank.setFill(tank.getFill() + out.fill);
                inventory.extractItem(SLOT_IN, 1, false);
                progress = 0;
            }
        } else {
            isProcessing = false;
            progress = 0;
        }

        for (DirPos dp : getConPos()) {
            if (tank.getFill() > 0) tryProvide(tank, level, dp);
        }

        dataChanged();
        networkPackMK2(50);
    }

    private boolean canProcess() {
        // CE TileEntityMachineLiquefactor.java:142-161
        if (power < usage) return false;
        ItemStack in = inventory.getStackInSlot(SLOT_IN);
        if (in.isEmpty()) return false;
        FluidStack out = LiquefactionRecipes.getOutput(in);
        if (out == null) return false;
        if (out.type != tank.getTankType() && tank.getFill() > 0) return false;
        return out.fill + tank.getFill() <= tank.getMaxFill();
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
        return MAX_POWER;
    }

    @Override
    public long getDemand(FluidType type, int pressure) {
        return 0;
    }

    @Override
    public long transferFluid(FluidType type, int pressure, long amount) {
        return amount;
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of();
    }

    @Override
    public List<FluidTankNTM> getSendingTanks() {
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
        tag.putInt("progress", progress);
        tank.writeToNBT(tag, "t");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        progress = tag.getInt("progress");
        tank.readFromNBT(tag, "t");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        tank.serialize(buf);
        buf.writeLong(power);
        buf.writeBoolean(isProcessing);
        buf.writeInt(progress);
        buf.writeInt(usage);
        buf.writeInt(processTime);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tank.deserialize(buf);
        power = buf.readLong();
        isProcessing = buf.readBoolean();
        progress = buf.readInt();
        usage = buf.readInt();
        processTime = buf.readInt();
    }

    @Override
    public void writeNBT(CompoundTag nbt) {
        tank.writeToNBT(nbt, "nt");
    }

    @Override
    public void readNBT(CompoundTag nbt) {
        tank.readFromNBT(nbt, "nt");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new LiquefactorMenu(containerId, playerInventory, this);
    }
}
