package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.block.ILockable;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.FileCabinetMenu;
import com.hbm.items.tool.ItemKey;
import com.hbm.items.tool.ItemKeyPin;
import com.hbm.lib.HBMSoundHandler;
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
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE {@code TileEntityFileCabinet} — 8 slots. Inherits {@code TileEntityLockableBase} lock Exact CE
 * {@code BlockDecoContainer.java:120-127}. Hopper off Exact CE {@code :130-144}.
 * TESR drawer render skipped. Extent+sound tick Exact CE {@code TileEntityFileCabinet.java:43-116}.
 * {@code tryPick} omitted — no pin item.
 */
public class FilingCabinetBlockEntity extends MachineBaseBlockEntity implements MenuProvider, ILockable, ITickableBE {

    /** CE {@code TileEntityLockableBase}: lock / isLocked / lockMod / cheesable. */
    private int lock;
    private boolean isLocked;
    private double lockMod = 0.1D;
    private boolean cheesable = true;

    private int timer;
    private int playersUsing;
    public float lowerExtent;
    public float upperExtent;

    public FilingCabinetBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 8, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.fileCabinet");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        // CE :132-133 is false (raw inventory GUI). MenuBase.tile is getCheckedInventory().
        return true;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack) {
        // CE :137-138
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        // CE :142-143
        return false;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[0];
    }

    /** CE {@code TileEntityLockableBase#canAccess(EntityPlayer)}. */
    public boolean canAccess(Player player) {
        if (!isLocked()) return true;
        if (player == null) return false;
        ItemStack held = player.getMainHandItem();
        int heldPins = held.getItem() instanceof ItemKeyPin ? ItemKeyPin.getPins(held) : 0;
        boolean ok = canAccess(heldPins, held.getItem() instanceof ItemKey);
        if (ok && level != null) {
            level.playSound(null, player.blockPosition(), HBMSoundHandler.lockOpen.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return ok;
    }

    /** Exact CE {@code TileEntityFileCabinet.java:43-45}. */
    public void openInventory(Player player) {
        if (level != null && !level.isClientSide && player != null && !player.isSpectator()) {
            playersUsing++;
        }
    }

    /** Exact CE {@code TileEntityFileCabinet.java:48-50}. */
    public void closeInventory(Player player) {
        if (level != null && !level.isClientSide && player != null && !player.isSpectator()) {
            playersUsing--;
        }
    }

    @Override
    public void updateEntity() {
        if (level == null) return;
        if (!level.isClientSide) {
            if (playersUsing > 0) {
                if (timer < 10) timer++;
            } else {
                timer = 0;
            }
            dataChanged();
            networkPackMK2(25);
        }

        // Exact CE TileEntityFileCabinet.java:80-116 (TESR interp skipped; sounds server-broadcast)
        float openSpeed = playersUsing > 0 ? 1F / 16F : 1F / 25F;
        float maxExtent = 0.8F;

        if (playersUsing > 0) {
            if (lowerExtent == 0F && upperExtent == 0F) {
                level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                        HBMSoundHandler.crateOpen.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
            } else {
                if (upperExtent + openSpeed >= maxExtent && lowerExtent < maxExtent) {
                    level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                            HBMSoundHandler.crateOpen.get(), SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.7F);
                }
                if (lowerExtent + openSpeed >= maxExtent && lowerExtent < maxExtent) {
                    level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                            HBMSoundHandler.crateOpen.get(), SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.7F);
                }
            }
            lowerExtent += openSpeed;
            if (timer >= 10) upperExtent += openSpeed;
        } else if (lowerExtent > 0) {
            if (upperExtent - openSpeed < maxExtent / 2 && upperExtent >= maxExtent / 2 && upperExtent != lowerExtent) {
                level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                        HBMSoundHandler.crateClose.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            if (lowerExtent - openSpeed < maxExtent / 2 && lowerExtent >= maxExtent / 2) {
                level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                        HBMSoundHandler.crateClose.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            upperExtent -= openSpeed;
            lowerExtent -= openSpeed;
        }
        lowerExtent = Mth.clamp(lowerExtent, 0F, maxExtent);
        upperExtent = Mth.clamp(upperExtent, 0F, maxExtent);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(timer);
        buf.writeInt(playersUsing);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        timer = buf.readInt();
        playersUsing = buf.readInt();
    }

    @Override
    public boolean isLocked() {
        return isLocked;
    }

    @Override
    public void lock() {
        if (!isLocked) {
            isLocked = true;
            dataChanged();
            setChanged();
        }
    }

    @Override
    public void unlock() {
        isLocked = false;
        setChanged();
    }

    @Override
    public void setPins(int pins) {
        if (lock != pins) {
            lock = pins;
            dataChanged();
            setChanged();
        }
    }

    @Override
    public int getPins() {
        return lock;
    }

    @Override
    public void setMod(double mod) {
        if (lockMod != mod) {
            lockMod = mod;
            dataChanged();
            setChanged();
        }
    }

    @Override
    public double getMod() {
        return lockMod;
    }

    @Override
    public boolean isCheesable() {
        return cheesable;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // CE TileEntityLockableBase.java:83-88
        tag.putInt("lock", lock);
        tag.putBoolean("cheesable", cheesable);
        tag.putBoolean("isLocked", isLocked);
        tag.putDouble("lockMod", lockMod);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        lock = tag.getInt("lock");
        cheesable = !tag.contains("cheesable") || tag.getBoolean("cheesable");
        isLocked = tag.getBoolean("isLocked");
        lockMod = tag.contains("lockMod") ? tag.getDouble("lockMod") : 0.1D;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FileCabinetMenu(id, inv, this);
    }
}
