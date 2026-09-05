package com.hbm.blockentity.machine.chem;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.recipes.chem.CentrifugeRecipes;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.inventory.container.machine.chem.CentrifugeMenu;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import com.hbm.tileentity.IConfigurableMachine;
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

import java.io.IOException;
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
 * {@link IConfigurableMachine} Exact CE {@code TileEntityMachineCentrifuge.java:89-107}
 * ({@code centrifuge} via {@link com.hbm.config.MachineDynConfig}). Audio loop skipped.
 */
public class CentrifugeBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, ITickableBE, IPersistentNBT, MenuProvider, IConfigurableMachine {

    private static final int INPUT_SLOT = 0;
    private static final int BATTERY_SLOT = 1;
    private static final int OUTPUT_START = 2;
    private static final int OUTPUT_END = 5;
    private static final int UPGRADE_START = 6;
    private static final int UPGRADE_END = 7;
    private static final int[] SLOT_IO = new int[]{0, 2, 3, 4, 5};

    /** CE {@code TileEntityMachineCentrifuge} configurable statics. */
    public static int maxPower = 100000;
    public static int processingSpeed = 200;
    public static int baseConsumption = 200;

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
                // CE TileEntityMachineCentrifuge.java:80-81
                if (!stack.isEmpty() && slot >= UPGRADE_START && slot <= UPGRADE_END
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
        return i >= OUTPUT_START && i <= OUTPUT_END;
    }

    public int getCentrifugeProgressScaled(int i) {
        return (progress * i) / processingSpeed;
    }

    public long getPowerRemainingScaled(int i) {
        return (power * i) / maxPower;
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

        power = Library.chargeTEFromItems(inventory, BATTERY_SLOT, power, maxPower);

        int consumption = baseConsumption;
        int speed = 1;

        upgradeManager.checkSlots(inventory, UPGRADE_START, UPGRADE_END);
        speed += upgradeManager.getLevel(UpgradeType.SPEED);
        consumption += upgradeManager.getLevel(UpgradeType.SPEED) * baseConsumption;

        speed *= (1 + upgradeManager.getLevel(UpgradeType.OVERDRIVE) * 5);
        consumption += upgradeManager.getLevel(UpgradeType.OVERDRIVE) * baseConsumption * 50;

        consumption /= (1 + upgradeManager.getLevel(UpgradeType.POWER));

        if (hasPower() && isProcessing()) {
            power -= consumption;
            if (power < 0) power = 0;
        }

        isProgressing = hasPower() && canProcess();

        if (isProgressing) {
            progress += speed;
            if (progress >= processingSpeed) {
                progress = 0;
                processItem();
            }
        } else {
            progress = 0;
        }

        // CE: TileEntityMachineCentrifuge.getLoopedSound() - continuous AudioWrapper loop
        // (HBMSoundHandler.centrifugeOperate, 20-tick keepAlive) while spinning. No looped-block-audio
        // bridge ported yet (see ChemPlantBlockEntity's identical note); substituted with a periodic
        // broadcast every 20 ticks while progressing.
        if (isProgressing && level.getGameTime() % 20 == 0) {
            level.playSound(null, worldPosition, HBMSoundHandler.centrifugeOperate.get(), SoundSource.BLOCKS, 1F, 1.0F);
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
        return maxPower;
    }

    @Override
    public String getConfigName() {
        return "centrifuge";
    }

    @Override
    public void readIfPresent(JsonObject obj) {
        readConfig(obj);
    }

    @Override
    public void writeConfig(JsonWriter writer) throws IOException {
        writeConfigStatic(writer);
    }

    static void readConfig(JsonObject obj) {
        // CE TileEntityMachineCentrifuge.java:96-98
        maxPower = IConfigurableMachine.grab(obj, "I:powerCap", maxPower);
        processingSpeed = IConfigurableMachine.grab(obj, "I:timeToProcess", processingSpeed);
        baseConsumption = IConfigurableMachine.grab(obj, "I:consumption", baseConsumption);
    }

    static void writeConfigStatic(JsonWriter writer) throws IOException {
        // CE TileEntityMachineCentrifuge.java:104-106
        writer.name("I:powerCap").value(maxPower);
        writer.name("I:timeToProcess").value(processingSpeed);
        writer.name("I:consumption").value(baseConsumption);
    }

    /** NeoForge BE has no no-arg ctor. MachineDynConfig Exact CE :44-48. */
    public static final class ConfigDummy implements IConfigurableMachine {
        @Override
        public String getConfigName() {
            return "centrifuge";
        }

        @Override
        public void readIfPresent(JsonObject obj) {
            readConfig(obj);
        }

        @Override
        public void writeConfig(JsonWriter writer) throws IOException {
            writeConfigStatic(writer);
        }
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
