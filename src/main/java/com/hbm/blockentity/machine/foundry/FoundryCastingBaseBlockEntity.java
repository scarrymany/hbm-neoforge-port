package com.hbm.blockentity.machine.foundry;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.inventory.material.Mats;
import com.hbm.items.machine.ItemMold;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * NeoForge port of CE {@code TileEntityFoundryCastingBase} - base class for foundry casting BEs.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/tileentity/machine/TileEntityFoundryCastingBase.java
 * <p>
 * Handles mold insertion and casting logic (CE :68-97).
 * Inventory: Slot 0 = mold input, Slot 1 = item output.
 */
public abstract class FoundryCastingBaseBlockEntity extends FoundryBaseBlockEntity implements ICrucibleAcceptor {

    @NotNull
    public ItemStackHandler inventory;
    public int cooloff = 100;

    public FoundryCastingBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, pos, state, 2);
    }

    public FoundryCastingBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int scount) {
        super(type, pos, state);
        inventory = getNewInventory(scount);
    }

    public ItemStackHandler getNewInventory(int scount) {
        return new ItemStackHandler(scount) {
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return slot == 0 && stack.getItem() instanceof ItemMold;
            }
        };
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) return;

        if (this.amount > this.getCapacity()) {
            this.amount = this.getCapacity();
        }

        if (this.amount == 0) {
            this.type = null;
        }

        ItemMold.MoldEntry mold = this.getInstalledMold();

        if (mold != null && this.amount == this.getCapacity() && inventory.getStackInSlot(1).isEmpty()) {
            cooloff--;

            if (cooloff <= 0) {
                this.amount = 0;

                ItemStack out = mold.getOutput(type);

                if (!out.isEmpty()) {
                    inventory.setStackInSlot(1, out.copy());
                }

                cooloff = 200;
                this.setChanged();
            }

        } else {
            cooloff = 200;
        }
    }

    /** Checks slot 0 to see what mold type is installed. Returns null if no mold is found or an incorrect size was used. */
    @Nullable
    public ItemMold.MoldEntry getInstalledMold() {
        if (inventory.getStackInSlot(0).isEmpty()) return null;

        if (inventory.getStackInSlot(0).getItem() instanceof ItemMold) {
            ItemMold.MoldEntry mold = ItemMold.getMold(inventory.getStackInSlot(0));

            if (mold.large() == (this.getMoldSize() == 1)) {
                return mold;
            }
        }

        return null;
    }

    @Override
    public int getCapacity() {
        ItemMold.MoldEntry mold = this.getInstalledMold();
        return mold == null ? 0 : mold.getCost();
    }

    /**
     * Standard check for testing if this material stack can be added to the casting block. Checks:<br>
     * - type matching<br>
     * - amount being at max<br>
     * - whether a mold is installed<br>
     * - whether the output slot is empty<br>
     * - whether the mold can accept this type
     * <p>
     * CE: {@code TileEntityFoundryCastingBase.standardCheck} (:136-142)
     */
    public boolean standardCheck(Level world, BlockPos p, Direction side, Mats.MaterialStack stack) {
        if (this.type != null && this.type != stack.material) return false;
        if (this.amount >= this.getCapacity()) return false;
        if (!inventory.getStackInSlot(1).isEmpty()) return false;
        ItemMold.MoldEntry mold = this.getInstalledMold();
        if (mold == null) return false;

        return !mold.getOutput(stack.material).isEmpty();
    }

    /** Returns an integer determining the mold size, 0 for small molds and 1 for the basin */
    public abstract int getMoldSize();

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public boolean canAcceptPartialPour(Level world, BlockPos pos, double dX, double dY, double dZ, Direction side, Mats.MaterialStack stack) {
        return standardCheck(world, pos, side, stack);
    }

    @Override
    public Mats.MaterialStack pour(Level world, BlockPos pos, double dX, double dY, double dZ, Direction side, Mats.MaterialStack stack) {
        if (!canAcceptPartialPour(world, pos, dX, dY, dZ, side, stack)) return stack;

        if (this.type == null) this.type = stack.material;

        int space = this.getCapacity() - this.amount;
        int toFill = Math.min(space, stack.amount);
        this.amount += toFill;
        stack.amount -= toFill;

        this.setChanged();
        return stack;
    }

    @Override
    public boolean canAcceptPartialFlow(Level world, BlockPos pos, Direction side, Mats.MaterialStack stack) {
        return false;
    }

    @Override
    public Mats.MaterialStack flow(Level world, BlockPos pos, Direction side, Mats.MaterialStack stack) {
        return stack;
    }
}
