package com.hbm.blockentity.machine.chem;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.machine.chem.CyclotronMenu;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.chem.CyclotronRecipes;
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
 * Ported from CE's {@code TileEntityMachineCyclotron} - a 6-block-tall, ±3-block-footprint
 * multiblock ({@code docs/phase2/machines_chemical_isotope.md}'s Cyclotron section) with 3
 * independent transmutation lanes (catalyst slots 0-2, target slots 3-5, output slots 6-8), each
 * checked against {@link CyclotronRecipes}. Coolant loop (water in / spent steam out), antimatter
 * accumulation, and the speed/consumption/coolant-consumption upgrade formulas are preserved exactly
 * from CE.
 */
public class CyclotronBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, ITickableBE, IPersistentNBT, MenuProvider {

    private static final int CATALYST_START = 0;
    private static final int TARGET_START = 3;
    private static final int OUTPUT_START = 6;
    private static final int BATTERY_SLOT = 9;
    private static final int UPGRADE_START = 10;
    private static final int UPGRADE_END = 11;

    public static final long MAX_POWER = 100_000_000L;
    public static final int BASE_CONSUMPTION = 1_000_000;
    public static final int DURATION = 690;

    public final FluidTankNTM[] tanks = new FluidTankNTM[3];
    public final UpgradeManagerNT upgradeManager;

    public long power;
    public int progress;

    public CyclotronBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 12, true, true);
        tanks[0] = new FluidTankNTM(Fluids.WATER, 32_000).withOwner(this);
        tanks[1] = new FluidTankNTM(Fluids.SPENTSTEAM, 32_000).withOwner(this);
        tanks[2] = new FluidTankNTM(Fluids.AMAT, 8_000).withOwner(this);

        Map<UpgradeType, Integer> maxLevels = new EnumMap<>(UpgradeType.class);
        maxLevels.put(UpgradeType.SPEED, 3);
        maxLevels.put(UpgradeType.POWER, 3);
        maxLevels.put(UpgradeType.EFFECT, 3);
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
                // CE TileEntityMachineCyclotron.java:78-79
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
        return Component.translatable("container.cyclotron");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot >= CATALYST_START && slot < CATALYST_START + 3) {
            for (var key : CyclotronRecipes.RECIPES.keySet()) {
                if (key.getKey().matchesRecipe(stack, true)) return true;
            }
        } else if (slot >= TARGET_START && slot < TARGET_START + 3) {
            for (var key : CyclotronRecipes.RECIPES.keySet()) {
                if (key.getValue().matchesRecipe(stack, true)) return true;
            }
        } else if (slot == BATTERY_SLOT) {
            return Library.isBattery(stack);
        } else if (slot >= UPGRADE_START && slot <= UPGRADE_END) {
            return stack.getItem() instanceof ItemMachineUpgrade;
        }
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot >= OUTPUT_START && slot < OUTPUT_START + 3;
    }

    public DirPos[] getConPos() {
        BlockPos p = worldPosition;
        return new DirPos[]{
                new DirPos(p.getX() + 3, p.getY(), p.getZ() + 1, Direction.EAST),
                new DirPos(p.getX() + 3, p.getY(), p.getZ() - 1, Direction.EAST),
                new DirPos(p.getX() - 3, p.getY(), p.getZ() + 1, Direction.WEST),
                new DirPos(p.getX() - 3, p.getY(), p.getZ() - 1, Direction.WEST),
                new DirPos(p.getX() + 1, p.getY(), p.getZ() + 3, Direction.SOUTH),
                new DirPos(p.getX() - 1, p.getY(), p.getZ() + 3, Direction.SOUTH),
                new DirPos(p.getX() + 1, p.getY(), p.getZ() - 3, Direction.NORTH),
                new DirPos(p.getX() - 1, p.getY(), p.getZ() - 3, Direction.NORTH)
        };
    }

    public int getSpeed() {
        return upgradeManager.getLevel(UpgradeType.SPEED) + 1;
    }

    public int getConsumption() {
        return BASE_CONSUMPTION - 100_000 * upgradeManager.getLevel(UpgradeType.POWER);
    }

    public int getCoolantConsumption() {
        int effect = upgradeManager.getLevel(UpgradeType.EFFECT);
        return 500 / (effect + 1) * getSpeed();
    }

    public boolean canProcess() {
        if (power < getConsumption()) return false;

        int convert = getCoolantConsumption();
        if (tanks[0].getFill() < convert) return false;
        if (tanks[1].getFill() + convert > tanks[1].getMaxFill()) return false;

        for (int i = 0; i < 3; i++) {
            Object[] res = CyclotronRecipes.getOutput(inventory.getStackInSlot(TARGET_START + i), inventory.getStackInSlot(CATALYST_START + i));
            if (res == null) continue;
            ItemStack out = (ItemStack) res[0];
            if (out == null) continue;

            ItemStack existing = inventory.getStackInSlot(OUTPUT_START + i);
            if (existing.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(existing, out) && existing.getCount() < out.getMaxStackSize()) return true;
        }
        return false;
    }

    public void process() {
        for (int i = 0; i < 3; i++) {
            Object[] res = CyclotronRecipes.getOutput(inventory.getStackInSlot(TARGET_START + i), inventory.getStackInSlot(CATALYST_START + i));
            if (res == null) continue;
            ItemStack out = (ItemStack) res[0];
            if (out == null) continue;

            ItemStack existing = inventory.getStackInSlot(OUTPUT_START + i);
            if (existing.isEmpty()) {
                inventory.extractItem(CATALYST_START + i, 1, false);
                inventory.extractItem(TARGET_START + i, 1, false);
                inventory.setStackInSlot(OUTPUT_START + i, out.copy());
                tanks[2].setFill(tanks[2].getFill() + (Integer) res[1]);
            } else if (ItemStack.isSameItemSameComponents(existing, out) && existing.getCount() < out.getMaxStackSize()) {
                inventory.extractItem(CATALYST_START + i, 1, false);
                inventory.extractItem(TARGET_START + i, 1, false);
                existing.grow(1);
                tanks[2].setFill(tanks[2].getFill() + (Integer) res[1]);
            }
        }
    }

    public long getPowerScaled(long i) {
        return (power * i) / MAX_POWER;
    }

    public int getProgressScaled(int i) {
        return (progress * i) / DURATION;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        for (DirPos dp : getConPos()) {
            trySubscribe(level, dp);
            trySubscribe(tanks[0].getTankType(), level, dp);
        }

        power = Library.chargeTEFromItems(inventory, BATTERY_SLOT, power, MAX_POWER);
        upgradeManager.checkSlots(inventory, UPGRADE_START, UPGRADE_END);

        if (canProcess()) {
            progress += getSpeed();
            power -= getConsumption();

            int convert = getCoolantConsumption();
            tanks[0].setFill(tanks[0].getFill() - convert);
            tanks[1].setFill(tanks[1].getFill() + convert);

            if (progress >= DURATION) {
                process();
                progress = 0;
                setChanged();
            }
        } else {
            progress = 0;
        }

        for (int i = 1; i < 3; i++) {
            if (tanks[i].getFill() > 0) {
                for (DirPos dp : getConPos()) tryProvide(tanks[i], level, dp);
            }
        }

        dataChanged();
        networkPackMK2(25);
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
    public List<FluidTankNTM> getSendingTanks() {
        return List.of(tanks[1], tanks[2]);
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(tanks[0]);
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(tanks);
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
        for (int i = 0; i < 3; i++) tanks[i].writeToNBT(tag, "tank" + i);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        progress = tag.getInt("progress");
        for (int i = 0; i < 3; i++) tanks[i].readFromNBT(tag, "tank" + i);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(progress);
        for (FluidTankNTM tank : tanks) tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        progress = buf.readInt();
        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
    }

    @Override
    public void writeNBT(CompoundTag nbt) {
        boolean empty = true;
        for (FluidTankNTM tank : tanks) if (tank.getFill() > 0) empty = false;
        if (empty) return;
        for (int i = 0; i < 3; i++) tanks[i].writeToNBT(nbt, "t" + i);
    }

    @Override
    public void readNBT(CompoundTag nbt) {
        for (int i = 0; i < 3; i++) tanks[i].readFromNBT(nbt, "t" + i);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CyclotronMenu(containerId, playerInventory, this);
    }
}
