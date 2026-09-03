package com.hbm.blockentity.machine.rbmk;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.interfaces.ICopiable;
import com.hbm.inventory.container.machine.rbmk.RBMKAutoloaderMenu;
import com.hbm.items.machine.ItemRBMKRod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mechanical fuel-rod feeder that sits directly above a fuel-rod channel and periodically swaps a
 * fresh rod in from its own hopper-fed inventory, pulling the spent one back out - not itself an
 * RBMK grid column (CE's {@code TileEntityRBMKAutoloader} {@code extends TileEntityMachineBase}).
 * Ported (simplified piston-animation timing; core swap logic preserved) from CE's
 * {@code TileEntityRBMKAutoloader} (313 lines, signature-level survey).
 */
public class RBMKAutoloaderBlockEntity extends MachineBaseBlockEntity implements ITickableBE, IControlReceiver, ICopiable, MenuProvider {

    public static final double SPEED = 0.005D;

    public double piston;
    public double lastPiston;
    public int cycle = 50;
    private int delay = 0;
    private boolean retracting = true;

    public RBMKAutoloaderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 2, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkAutoloader");
    }

    public boolean hasFuel() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (inventory.getStackInSlot(i).getItem() instanceof ItemRBMKRod) return true;
        }
        return false;
    }

    public boolean hasSpace() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (inventory.getStackInSlot(i).isEmpty()) return true;
        }
        return false;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        lastPiston = piston;

        BlockEntity below = level.getBlockEntity(worldPosition.below());
        if (!(below instanceof RBMKRodBlockEntity rod)) return;

        if (retracting) {
            piston = Math.max(0, piston - SPEED);
            if (piston <= 0 && delay-- <= 0) retracting = false;
        } else {
            piston = Math.min(1, piston + SPEED);
            if (piston >= 1) {
                trySwap(rod);
                retracting = true;
                delay = cycle;
            }
        }

        setChanged();
        dataChanged();
        networkPackMK2(25);
    }

    private void trySwap(RBMKRodBlockEntity rod) {
        if (rod.coldEnoughForAutoloader() && rod.canUnload() && hasSpace()) {
            ItemStack spent = rod.provideNext().copy();
            for (int i = 0; i < inventory.getSlots(); i++) {
                if (inventory.getStackInSlot(i).isEmpty()) {
                    inventory.setStackInSlot(i, spent);
                    break;
                }
            }
            rod.unload();
        }

        if (rod.canLoad(ItemStack.EMPTY)) {
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (stack.getItem() instanceof ItemRBMKRod) {
                    rod.load(stack.copy());
                    inventory.setStackInSlot(i, ItemStack.EMPTY);
                    break;
                }
            }
        }
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack stack) {
        return stack.getItem() instanceof ItemRBMKRod;
    }

    @Override
    public boolean hasPermission(Player player) {
        return true;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("cycle")) this.cycle = data.getInt("cycle");
        setChanged();
    }

    @Override
    public void receiveControl(ServerPlayer player, CompoundTag data) {
        receiveControl(data);
    }

    @Override
    public CompoundTag getSettings(Level world, BlockPos pos) {
        CompoundTag data = new CompoundTag();
        data.putInt("cycle", cycle);
        return data;
    }

    @Override
    public void pasteSettings(CompoundTag nbt, int index, Level world, Player player, BlockPos pos) {
        if (nbt.contains("cycle")) cycle = nbt.getInt("cycle");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("piston", piston);
        tag.putInt("cycle", cycle);
        tag.putBoolean("retracting", retracting);
        tag.putInt("delay", delay);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        piston = tag.getDouble("piston");
        if (tag.contains("cycle")) cycle = tag.getInt("cycle");
        retracting = tag.getBoolean("retracting");
        delay = tag.getInt("delay");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeDouble(piston);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        piston = buf.readDouble();
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RBMKAutoloaderMenu(containerId, playerInventory, this);
    }
}
