package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.MachineRTGMenu;
import com.hbm.items.machine.ItemRTGPellet;
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
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Ported from CE's {@code TileEntityMachineRTG} (regname {@code machine_rtg_grey}, read in full):
 * a 15-slot pellet grid whose combined, decay-scaled heat output (see
 * {@link ItemRTGPellet#getScaledPower}/{@link ItemRTGPellet#handleDecay}, this port's already-ported
 * data-component-based replacement for CE's {@code RTGUtil.updateRGs}/{@code ItemRTGPellet}'s old
 * NBT-tag decay) accumulates into a heat buffer, which in turn trickle-feeds a much larger power
 * buffer at a fixed 5x multiplier - CE's own {@code power += heat * 5L} unchanged.
 * <p>
 * CE's inventory was a raw, capability-only {@code ItemStackHandler} with no
 * {@code TileEntityMachineBase} inventory conventions; its no-extraction override is present in
 * source but commented out, so live CE actually allows normal withdrawal - this port matches that
 * real behavior (not the aspirational dead code) and does not block extraction either.
 */
public class MachineRTGBlockEntity extends MachineBaseBlockEntity implements IEnergyProviderMK2, ITickableBE, MenuProvider {

    public static final int SLOT_COUNT = 15;
    public static final int HEAT_MAX = 6000;
    public static final long MAX_POWER = 1_000_000L;

    public int heat;
    private long power;

    public MachineRTGBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SLOT_COUNT, false, true);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rtg");
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
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.getItem() instanceof ItemRTGPellet;
            }
        };
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack stack) {
        return stack.getItem() instanceof ItemRTGPellet;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        for (Direction dir : Direction.values()) {
            BlockPos target = worldPosition.relative(dir);
            this.tryProvide(level, target.getX(), target.getY(), target.getZ(), dir);
        }

        int newHeat = 0;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!(stack.getItem() instanceof ItemRTGPellet)) continue;
            newHeat += ItemRTGPellet.getScaledPower(stack);
            inventory.setStackInSlot(i, ItemRTGPellet.handleDecay(stack));
        }

        heat = Math.min(HEAT_MAX, newHeat);
        power = Math.min(MAX_POWER, power + heat * 5L);
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

    public long getPowerScaled(long scale) {
        return (power * scale) / MAX_POWER;
    }

    public int getHeatScaled(int scale) {
        return (heat * scale) / HEAT_MAX;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("heat", heat);
        tag.putLong("power", power);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        heat = tag.getInt("heat");
        power = tag.getLong("power");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(heat);
        buf.writeLong(power);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        heat = buf.readInt();
        power = buf.readLong();
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineRTGMenu(containerId, playerInventory, this);
    }
}
