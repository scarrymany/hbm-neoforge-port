package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.MicrowaveMenu;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * CE {@code TileEntityMicrowave} — 50 HE/t, 300 ticks, speed 0–5. Speed 5 explodes.
 * Slot 2 battery {@code chargeTEFromItems} Exact CE {@code TileEntityMicrowave.java:65}.
 * Hopper sides Exact CE {@code :146-148}. GUI Exact CE {@code ContainerMicrowave}/{@code GUIMicrowave}
 * on existing {@code gui_microwave.png} — not invent.
 */
public class MachineMicrowaveBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, ITickableBE, MenuProvider {

    public static final long MAX_POWER = 50_000L;
    public static final int CONSUMPTION = 50;
    public static final int MAX_TIME = 300;
    public static final int MAX_SPEED = 5;
    public static final int SLOT_IN = 0;
    public static final int SLOT_OUT = 1;
    public static final int BATTERY_SLOT = 2;

    public long power;
    public int time;
    public int speed;

    public MachineMicrowaveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 3, false, true);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.microwave");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == BATTERY_SLOT) return Library.isBattery(stack);
        return slot == SLOT_IN && smelt(stack).isPresent();
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == SLOT_OUT;
    }

    /** Exact CE {@code TileEntityMicrowave.java:146-148}. */
    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return side == Direction.DOWN ? new int[]{SLOT_OUT} : new int[]{SLOT_IN};
    }

    public long getPowerScaled(int i) {
        return (power * i) / MAX_POWER;
    }

    public int getProgressScaled(int i) {
        return (time * i) / MAX_TIME;
    }

    public int getSpeedScaled(int i) {
        return (speed * i) / MAX_SPEED;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        // CE TileEntityMicrowave.java:63-65 — every tick, all faces + battery slot 2
        for (Direction d : Direction.values()) trySubscribe(level, worldPosition.relative(d), d);
        this.power = Library.chargeTEFromItems(inventory, BATTERY_SLOT, power, MAX_POWER);

        if (canProcess()) {
            if (speed >= MAX_SPEED) {
                BlockPos p = worldPosition;
                level.destroyBlock(p, false);
                level.explode(null, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                        7.5F, true, Level.ExplosionInteraction.TNT);
                return;
            }
            if (time >= MAX_TIME) {
                process();
                time = 0;
            }
            if (canProcess()) {
                power -= CONSUMPTION;
                time += speed * 2;
            }
        }

        dataChanged();
        networkPackMK2(50);
    }

    public void bumpSpeed(int delta) {
        speed = Math.max(0, Math.min(MAX_SPEED, speed + delta));
        setChanged();
    }

    private boolean canProcess() {
        if (speed <= 0 || power < CONSUMPTION) return false;
        Optional<ItemStack> out = smelt(inventory.getStackInSlot(SLOT_IN));
        if (out.isEmpty()) return false;
        ItemStack dest = inventory.getStackInSlot(SLOT_OUT);
        if (dest.isEmpty()) return true;
        ItemStack result = out.get();
        return ItemStack.isSameItemSameComponents(dest, result)
                && dest.getCount() + result.getCount() <= dest.getMaxStackSize();
    }

    private void process() {
        Optional<ItemStack> out = smelt(inventory.getStackInSlot(SLOT_IN));
        if (out.isEmpty()) return;
        ItemStack result = out.get();
        ItemStack dest = inventory.getStackInSlot(SLOT_OUT);
        if (dest.isEmpty()) inventory.setStackInSlot(SLOT_OUT, result.copy());
        else dest.grow(result.getCount());
        inventory.extractItem(SLOT_IN, 1, false);
    }

    private Optional<ItemStack> smelt(ItemStack input) {
        if (level == null || input.isEmpty()) return Optional.empty();
        Optional<RecipeHolder<SmeltingRecipe>> rec = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level);
        return rec.map(h -> h.value().assemble(new SingleRecipeInput(input), level.registryAccess()));
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
        tag.putInt("time", time);
        tag.putInt("speed", speed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        time = tag.getInt("time");
        speed = tag.getInt("speed");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(time);
        buf.writeInt(speed);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        time = buf.readInt();
        speed = buf.readInt();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MicrowaveMenu(id, inv, this);
    }
}
