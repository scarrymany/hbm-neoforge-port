package com.hbm.blockentity.machine.chem;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.recipes.chem.CentrifugeRecipes;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.inventory.container.machine.chem.CentrifugeMenu;
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

import java.util.EnumMap;
import java.util.Map;

/**
 * Ported from CE's {@code TileEntityMachineCentrifuge} - the plain item-only "ore washing" centrifuge
 * (distinct from the isotope-separation gas centrifuge, {@link GasCentrifugeBlockEntity}; see
 * {@code docs/phase2/machines_chemical_isotope.md}). Slot layout, speed/consumption math (SPEED
 * upgrade +1:1 speed/consumption, OVERDRIVE {@code 1+level*5} speed multiplier with a heavy
 * consumption penalty, POWER upgrade dividing consumption by {@code 1+level}) and the
 * recipe-driven progress loop are preserved from CE exactly.
 * <p>
 * <b>Not ported</b>: CE's {@code IConfigurableMachine} JSON-config-file override for
 * {@code maxPower}/{@code processingSpeed}/{@code baseConsumption} (a config-file layer separate from
 * the recipe/JSON system, out of this pass's scope - the three values are plain constants here) and
 * looped centrifuge audio (same precedent as {@code MachineRefineryBlockEntity}).
 */
public class CentrifugeBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, ITickableBE, IPersistentNBT, MenuProvider {

    private static final int INPUT_SLOT = 0;
    private static final int BATTERY_SLOT = 1;
    private static final int OUTPUT_START = 2;
    private static final int OUTPUT_END = 5;
    private static final int UPGRADE_START = 6;
    private static final int UPGRADE_END = 7;
    private static final int[] SLOT_IO = new int[]{0, 2, 3, 4, 5};

    public static final long MAX_POWER = 100_000L;
    public static final int PROCESSING_SPEED = 200;
    public static final int BASE_CONSUMPTION = 200;

    public final UpgradeManagerNT upgradeManager;
    public int progress;
    public long power;
    public boolean isProgressing;

    public CentrifugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 8, false, true);

        Map<UpgradeType, Integer> maxLevels = new EnumMap<>(UpgradeType.class);
        maxLevels.put(UpgradeType.SPEED, 3);
        maxLevels.put(UpgradeType.POWER, 3);
        maxLevels.put(UpgradeType.OVERDRIVE, 3);
        this.upgradeManager = new UpgradeManagerNT(maxLevels);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.centrifuge");
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemStack) {
        if (i == INPUT_SLOT) return CentrifugeRecipes.getOutput(itemStack) != null;
        if (i == BATTERY_SLOT) return Library.isBattery(itemStack);
        if (i >= UPGRADE_START && i <= UPGRADE_END) return itemStack.getItem() instanceof ItemMachineUpgrade;
        return false;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return SLOT_IO;
    }

    @Override
    public boolean canExtractItem(int i, ItemStack itemStack, int amount) {
        return i > BATTERY_SLOT;
    }

    public int getCentrifugeProgressScaled(int i) {
        return (progress * i) / PROCESSING_SPEED;
    }

    public long getPowerRemainingScaled(int i) {
        return (power * i) / MAX_POWER;
    }

    public boolean hasPower() {
        return power > 0;
    }

    public boolean isProcessing() {
        return progress > 0;
    }

    public boolean canProcess() {
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) return false;

        ItemStack[] out = CentrifugeRecipes.getOutput(input);
        if (out == null) return false;

        for (int i = 0; i < Math.min(4, out.length); i++) {
            if (out[i] == null || out[i].isEmpty()) continue;
            if (!inventory.insertItem(OUTPUT_START + i, out[i], true).isEmpty()) return false;
        }
        return true;
    }

    private void processItem() {
        ItemStack[] out = CentrifugeRecipes.getOutput(inventory.getStackInSlot(INPUT_SLOT));
        if (out == null) return;

        for (int i = 0; i < Math.min(4, out.length); i++) {
            if (out[i] != null && !out[i].isEmpty()) inventory.insertItem(OUTPUT_START + i, out[i], false);
        }
        inventory.extractItem(INPUT_SLOT, 1, false);
        setChanged();
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        for (Direction dir : Direction.values()) {
            trySubscribe(level, worldPosition.relative(dir), dir);
        }

        power = Library.chargeTEFromItems(inventory, BATTERY_SLOT, power, MAX_POWER);

        int consumption = BASE_CONSUMPTION;
        int speed = 1;

        upgradeManager.checkSlots(inventory, UPGRADE_START, UPGRADE_END);
        speed += upgradeManager.getLevel(UpgradeType.SPEED);
        consumption += upgradeManager.getLevel(UpgradeType.SPEED) * BASE_CONSUMPTION;

        speed *= (1 + upgradeManager.getLevel(UpgradeType.OVERDRIVE) * 5);
        consumption += upgradeManager.getLevel(UpgradeType.OVERDRIVE) * BASE_CONSUMPTION * 50;

        consumption /= (1 + upgradeManager.getLevel(UpgradeType.POWER));

        if (hasPower() && isProcessing()) {
            power -= consumption;
            if (power < 0) power = 0;
        }

        isProgressing = hasPower() && canProcess();

        if (isProgressing) {
            progress += speed;
            if (progress >= PROCESSING_SPEED) {
                progress = 0;
                processItem();
            }
        } else {
            progress = 0;
        }

        dataChanged();
        networkPackMK2(50);
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        progress = tag.getInt("progress");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(progress);
        buf.writeBoolean(isProgressing);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        progress = buf.readInt();
        isProgressing = buf.readBoolean();
    }

    @Override
    public void writeNBT(CompoundTag nbt) {
        // no persistent-through-break state beyond the inventory itself, which MachineBaseBlockEntity already round-trips
    }

    @Override
    public void readNBT(CompoundTag nbt) {
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CentrifugeMenu(containerId, playerInventory, this);
    }
}
