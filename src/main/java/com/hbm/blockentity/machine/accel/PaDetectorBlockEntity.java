package com.hbm.blockentity.machine.accel;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.accel.PaDetectorMenu;
import com.hbm.inventory.recipes.ParticleAcceleratorRecipes;
import com.hbm.inventory.recipes.ParticleAcceleratorRecipes.ParticleAcceleratorRecipe;
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

/**
 * CE {@code TileEntityPADetector.java}: usage 100_000 HE. Momentum is a minimum gate
 * ({@code particle.momentum < recipe.momentum} fails). Without the full beam sim, stored HE
 * converts to momentum 1:100 (CE source pumps particles; here the detector is the recipe table).
 */
public class PaDetectorBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, ITickableBE, MenuProvider {

    private static final int SLOT_BATTERY = 0;
    private static final int SLOT_IN_A = 1;
    private static final int SLOT_IN_B = 2;
    private static final int SLOT_OUT_A = 3;
    private static final int SLOT_OUT_B = 4;

    public static final long MAX_POWER = 10_000_000L;
    public static final long USAGE = 100_000L;
    public static final int PROCESS_TIME = 20;

    public long power;
    public int momentum;
    public int progress;
    public boolean isProcessing;

    public PaDetectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 5, true, true);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.paDetector");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return Library.isBattery(stack);
        return slot == SLOT_IN_A || slot == SLOT_IN_B;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == SLOT_OUT_A || slot == SLOT_OUT_B;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{SLOT_IN_A, SLOT_IN_B, SLOT_OUT_A, SLOT_OUT_B};
    }

    public int getProgressScaled(int i) {
        return (progress * i) / PROCESS_TIME;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        power = Library.chargeTEFromItems(inventory, SLOT_BATTERY, power, MAX_POWER);
        for (Direction dir : Direction.values()) {
            trySubscribe(level, worldPosition.relative(dir), dir);
        }

        if (power > 0) {
            int add = (int) Math.min(power / 100L, 10_000L);
            momentum = Math.min(100_000, momentum + add);
        }

        ParticleAcceleratorRecipe recipe = ParticleAcceleratorRecipes.getOutput(
                inventory.getStackInSlot(SLOT_IN_A), inventory.getStackInSlot(SLOT_IN_B));
        if (recipe != null && momentum >= recipe.momentum && power >= USAGE && canOutput(recipe)) {
            isProcessing = true;
            power -= USAGE;
            progress++;
            if (progress >= PROCESS_TIME) {
                inventory.extractItem(SLOT_IN_A, 1, false);
                inventory.extractItem(SLOT_IN_B, 1, false);
                mergeOut(SLOT_OUT_A, recipe.output1);
                if (!recipe.output2.isEmpty()) mergeOut(SLOT_OUT_B, recipe.output2);
                momentum = 0;
                progress = 0;
            }
        } else {
            isProcessing = false;
            progress = 0;
        }

        dataChanged();
        networkPackMK2(50);
    }

    private boolean canOutput(ParticleAcceleratorRecipe recipe) {
        return fits(SLOT_OUT_A, recipe.output1) && (recipe.output2.isEmpty() || fits(SLOT_OUT_B, recipe.output2));
    }

    private boolean fits(int slot, ItemStack stack) {
        ItemStack have = inventory.getStackInSlot(slot);
        if (have.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(have, stack) && have.getCount() + stack.getCount() <= have.getMaxStackSize();
    }

    private void mergeOut(int slot, ItemStack stack) {
        ItemStack have = inventory.getStackInSlot(slot);
        if (have.isEmpty()) {
            inventory.setStackInSlot(slot, stack.copy());
        } else {
            have.grow(stack.getCount());
        }
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
        tag.putInt("momentum", momentum);
        tag.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        momentum = tag.getInt("momentum");
        progress = tag.getInt("progress");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(momentum);
        buf.writeInt(progress);
        buf.writeBoolean(isProcessing);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        momentum = buf.readInt();
        progress = buf.readInt();
        isProcessing = buf.readBoolean();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PaDetectorMenu(containerId, playerInventory, this);
    }
}
