package com.hbm.blockentity.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.generic.Phase8Blocks;
import com.hbm.inventory.container.machine.dummyable.PressMenu;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.items.machine.ItemStamp;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
/**
 * CE {@code TileEntityMachinePress}: burner stamp press, 13 slots.
 * Adjacent {@code press_preheater} {@code speed += 4} Exact CE {@code :60-71}.
 */
public class MachinePressBlockEntity extends MachineBaseBlockEntity implements ITickableBE, MenuProvider {

    public static final int MAX_SPEED = 400;
    public static final int PROGRESS_AT_MAX = 25;
    public static final int MAX_PROGRESS = 200;

    public static final int SLOT_FUEL = 0;
    public static final int SLOT_STAMP = 1;
    public static final int SLOT_IN = 2;
    public static final int SLOT_OUT = 3;

    public int speed;
    public int burnTime;
    public int progress;
    private boolean retracting;
    private int delay;

    public MachinePressBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 13, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.press");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_FUEL) return getBurnTime(stack) > 0;
        if (slot == SLOT_STAMP) return stack.getItem() instanceof ItemStamp;
        if (slot == SLOT_IN) return !PressRecipes.getOutput(stack, inventory.getStackInSlot(SLOT_STAMP)).isEmpty()
                || inventory.getStackInSlot(SLOT_STAMP).isEmpty();
        return slot >= 4;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == SLOT_OUT || slot >= 4;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        // Exact CE TileEntityMachinePress.java:60-67 / :71
        boolean preheated = false;
        for (Direction dir : Direction.values()) {
            if (level.getBlockState(worldPosition.relative(dir)).is(Phase8Blocks.PRESS_PREHEATER.get())) {
                preheated = true;
                break;
            }
        }

        boolean can = canProcess();
        if ((can || retracting) && burnTime >= 200) {
            speed = Math.min(MAX_SPEED, speed + (preheated ? 4 : 1));
        } else {
            speed = Math.max(0, speed - 1);
        }

        if (delay <= 0) {
            int stampSpeed = speed * PROGRESS_AT_MAX / MAX_SPEED;
            if (retracting) {
                progress -= stampSpeed;
                if (progress <= 0) {
                    progress = 0;
                    retracting = false;
                    delay = 5;
                }
            } else if (can) {
                progress += stampSpeed;
                if (progress >= MAX_PROGRESS) {
                    progress = MAX_PROGRESS;
                    ItemStack out = PressRecipes.getOutput(inventory.getStackInSlot(SLOT_IN), inventory.getStackInSlot(SLOT_STAMP));
                    if (!out.isEmpty()) {
                        if (inventory.getStackInSlot(SLOT_OUT).isEmpty()) {
                            inventory.setStackInSlot(SLOT_OUT, out.copy());
                        } else {
                            inventory.getStackInSlot(SLOT_OUT).grow(out.getCount());
                        }
                        inventory.extractItem(SLOT_IN, 1, false);
                        ItemStack stamp = inventory.getStackInSlot(SLOT_STAMP);
                        if (stamp.isDamageableItem()) {
                            stamp.setDamageValue(stamp.getDamageValue() + 1);
                            if (stamp.getDamageValue() >= stamp.getMaxDamage()) {
                                inventory.setStackInSlot(SLOT_STAMP, ItemStack.EMPTY);
                            }
                        }
                        if (burnTime >= 200) burnTime -= 200;
                        level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.4F, 1.2F);
                    }
                    retracting = true;
                    delay = 5;
                    setChanged();
                }
            }
        } else {
            delay--;
        }

        if (!can && !retracting && progress > 0) retracting = true;

        ItemStack fuel = inventory.getStackInSlot(SLOT_FUEL);
        if (!fuel.isEmpty() && burnTime < 200) {
            int bt = getBurnTime(fuel);
            if (bt > 0) {
                burnTime += bt;
                inventory.extractItem(SLOT_FUEL, 1, false);
                setChanged();
            }
        }

        dataChanged();
        networkPackMK2(50);
    }

    public boolean canProcess() {
        if (burnTime < 200) return false;
        ItemStack stamp = inventory.getStackInSlot(SLOT_STAMP);
        ItemStack in = inventory.getStackInSlot(SLOT_IN);
        if (stamp.isEmpty() || in.isEmpty()) return false;
        ItemStack out = PressRecipes.getOutput(in, stamp);
        if (out.isEmpty()) return false;
        ItemStack slot = inventory.getStackInSlot(SLOT_OUT);
        if (slot.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(slot, out) && slot.getCount() + out.getCount() <= slot.getMaxStackSize();
    }

    public static int getBurnTime(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        return stack.getBurnTime(RecipeType.SMELTING);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("speed", speed);
        tag.putInt("burn", burnTime);
        tag.putInt("prog", progress);
        tag.putBoolean("ret", retracting);
        tag.putInt("delay", delay);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        speed = tag.getInt("speed");
        burnTime = tag.getInt("burn");
        progress = tag.getInt("prog");
        retracting = tag.getBoolean("ret");
        delay = tag.getInt("delay");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(speed);
        buf.writeInt(burnTime);
        buf.writeInt(progress);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        speed = buf.readInt();
        burnTime = buf.readInt();
        progress = buf.readInt();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new PressMenu(id, inv, this);
    }
}
