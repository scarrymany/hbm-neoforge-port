package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.EPressMenu;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.items.machine.ItemStamp;
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

/**
 * CE {@code TileEntityMachineEPress}: 100 HE/t, 200 progress, SPEED upgrade via slot scan.
 * Stamp sound Exact CE {@code TileEntityMachineEPress.java:137}: {@code pressOperate} 1.5F/1.0F.
 */
public class MachineEPressBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, ITickableBE, MenuProvider {

    public static final long MAX_POWER = 50_000;
    public static final int MAX_PROGRESS = 200;

    public long power;
    public int progress;
    private boolean retracting;
    private int delay;

    public MachineEPressBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 5, false, true);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.epress");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        if (slot == 1) return stack.getItem() instanceof ItemStamp;
        if (slot == 2) return !(stack.getItem() instanceof ItemStamp) && !Library.isBattery(stack)
                && !(stack.getItem() instanceof ItemMachineUpgrade);
        if (slot == 4) return stack.getItem() instanceof ItemMachineUpgrade;
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == 3;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{1, 2, 3};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        for (Direction dir : Direction.values()) {
            trySubscribe(level, worldPosition.relative(dir), dir);
        }
        power = Library.chargeTEFromItems(inventory, 0, power, MAX_POWER);

        boolean can = canProcess();
        boolean hasPower = power >= 100;
        if ((can || retracting || delay > 0) && hasPower) {
            power -= 100;
            if (delay <= 0) {
                int speed = 1;
                ItemStack up = inventory.getStackInSlot(4);
                if (up.getItem() instanceof ItemMachineUpgrade u && u.getType() == UpgradeType.SPEED) {
                    speed = 1 + u.getTier();
                }
                double processSpeed = (retracting ? 20 : 45) * (1.0D + speed / 4.0D);
                if (retracting) {
                    progress -= (int) Math.round(processSpeed);
                    if (progress <= 0) {
                        progress = 0;
                        retracting = false;
                        delay = 5 - speed + 1;
                    }
                } else if (can) {
                    progress += (int) Math.round(processSpeed);
                    if (progress >= MAX_PROGRESS) {
                        progress = MAX_PROGRESS;
                        craftItem();
                        if (level != null) {
                            level.playSound(null, worldPosition, HBMSoundHandler.pressOperate.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
                        }
                        retracting = true;
                        delay = 5 - speed + 1;
                        setChanged();
                    }
                }
            } else {
                delay--;
            }
        } else if (progress > 0) {
            retracting = true;
        }
        dataChanged();
        networkPackMK2(25);
    }

    public boolean canProcess() {
        ItemStack stamp = inventory.getStackInSlot(1);
        ItemStack in = inventory.getStackInSlot(2);
        if (stamp.isEmpty() || in.isEmpty()) return false;
        ItemStack out = PressRecipes.getOutput(in, stamp);
        if (out.isEmpty()) return false;
        ItemStack slot = inventory.getStackInSlot(3);
        if (slot.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(slot, out) && slot.getCount() + out.getCount() <= slot.getMaxStackSize();
    }

    private void craftItem() {
        ItemStack in = inventory.getStackInSlot(2);
        ItemStack stamp = inventory.getStackInSlot(1);
        ItemStack out = PressRecipes.getOutput(in, stamp);
        if (out.isEmpty()) return;
        ItemStack dest = inventory.getStackInSlot(3);
        if (dest.isEmpty()) {
            inventory.setStackInSlot(3, out.copy());
        } else {
            dest.grow(out.getCount());
        }
        inventory.extractItem(2, 1, false);
        if (stamp.isDamageableItem()) {
            stamp.setDamageValue(stamp.getDamageValue() + 1);
            if (stamp.getDamageValue() >= stamp.getMaxDamage()) {
                inventory.setStackInSlot(1, ItemStack.EMPTY);
            }
        }
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putInt("progress", progress);
        tag.putBoolean("isRetracting", retracting);
        tag.putInt("delay", delay);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        progress = tag.getInt("progress");
        retracting = tag.getBoolean("isRetracting");
        delay = tag.getInt("delay");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(progress);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        progress = buf.readInt();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new EPressMenu(id, inv, this);
    }
}
