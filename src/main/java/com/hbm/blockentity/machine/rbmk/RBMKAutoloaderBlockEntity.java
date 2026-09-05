package com.hbm.blockentity.machine.rbmk;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.machine.rbmk.RBMKBaseBlock;
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
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Autoloader. Exact CE {@code TileEntityRBMKAutoloader.java:60-133/:195-287}: 18-slot hopper
 * (0-8 in / 9-17 out), enrichment {@code cycle} threshold, piston delay 40, {@code findCore}
 * into the rod column, minus/plus ±5 clamp 5-95. Lift audio / Tower VFX / TESR lerp stay skipped.
 */
public class RBMKAutoloaderBlockEntity extends MachineBaseBlockEntity implements ITickableBE, IControlReceiver, ICopiable, MenuProvider {

    public static final double SPEED = 0.005D;
    private static final int[] ALL_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17};
    private static final int[] NO_SLOTS = new int[0];

    public double piston;
    public double lastPiston;
    public int cycle = 50;
    private int delay = 0;
    private boolean isRetracting = true;

    public RBMKAutoloaderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 18, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkAutoloader");
    }

    // CE :195-204
    public boolean hasFuel() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemRBMKRod
                    && ItemRBMKRod.getEnrichment(stack) * 100 >= cycle) {
                return true;
            }
        }
        return false;
    }

    // CE :206-209
    public boolean hasSpace() {
        for (int i = 9; i < 18; i++) {
            if (inventory.getStackInSlot(i).isEmpty()) return true;
        }
        return false;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (delay > 0) delay--;

        if (delay <= 0 && this.isRetracting && this.piston > 0D) {
            this.piston -= SPEED;
            if (this.piston <= 0) {
                this.piston = 0;
                this.delay = 40;
            }
        }

        // CE :76-89 — every 20t, start extending if fuel + space + cold rod below enrichment
        if (isRetracting && level.getGameTime() % 20 == 0 && this.hasFuel() && this.hasSpace()) {
            RBMKRodBlockEntity rod = findRodBelow();
            if (rod != null && rod.coldEnoughForAutoloader()) {
                ItemStack loaded = rod.inventory.getStackInSlot(0);
                if (loaded.isEmpty() || (loaded.getItem() instanceof ItemRBMKRod
                        && ItemRBMKRod.getEnrichment(loaded) * 100 < cycle)) {
                    this.isRetracting = false;
                }
            }
        }

        if (delay <= 0 && !this.isRetracting && this.piston < 1D) {
            this.piston += SPEED;
            if (this.piston >= 1) {
                this.piston = 1;
                this.delay = 40;
            }
        }

        // CE :100-133 — swap at full extension
        if (!isRetracting && this.piston >= 1D) {
            this.piston = 1D;
            RBMKRodBlockEntity rod = findRodBelow();
            if (rod != null) {
                if (!rod.inventory.getStackInSlot(0).isEmpty() && this.hasSpace()) {
                    for (int i = 9; i < 18; i++) {
                        if (inventory.getStackInSlot(i).isEmpty()) {
                            inventory.setStackInSlot(i, rod.inventory.getStackInSlot(0).copy());
                            rod.inventory.setStackInSlot(0, ItemStack.EMPTY);
                            rod.setChanged();
                            break;
                        }
                    }
                }
                if (rod.inventory.getStackInSlot(0).isEmpty()) {
                    for (int i = 0; i < 9; i++) {
                        ItemStack stack = inventory.getStackInSlot(i);
                        if (!stack.isEmpty() && stack.getItem() instanceof ItemRBMKRod
                                && ItemRBMKRod.getEnrichment(stack) * 100 >= cycle) {
                            rod.inventory.setStackInSlot(0, stack.copy());
                            inventory.setStackInSlot(i, ItemStack.EMPTY);
                            rod.setChanged();
                            break;
                        }
                    }
                }

                this.isRetracting = true;
                this.delay = 40;
                setChanged();
            }
        }

        networkPackMK2(100);
    }

    /** CE {@code :77-81}/{:104-106} — {@code RBMKBase.findCore} under this block. */
    private RBMKRodBlockEntity findRodBelow() {
        if (level == null) return null;
        BlockPos down = worldPosition.below();
        if (level.getBlockState(down).getBlock() instanceof RBMKBaseBlock rbmkBase) {
            BlockPos corePos = rbmkBase.findCore(level, down);
            if (corePos != null && level.getBlockEntity(corePos) instanceof RBMKRodBlockEntity rod) {
                return rod;
            }
        }
        return null;
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack stack) {
        // CE :228-230
        return stack.getItem() instanceof ItemRBMKRod && ItemRBMKRod.getEnrichment(stack) * 100 >= cycle && i < 9;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        // CE :233-235 — hopper locked while piston is out
        return this.piston <= 0 ? ALL_SLOTS : NO_SLOTS;
    }

    @Override
    public boolean canExtractItem(int i, ItemStack itemStack, int amount) {
        return i >= 9;
    }

    @Override
    public boolean hasPermission(Player player) {
        return this.isUseableByPlayer(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        // CE :282-286
        if (data.contains("minus") && this.cycle > 5) this.cycle -= 5;
        if (data.contains("plus") && this.cycle < 95) this.cycle += 5;
        this.cycle = Mth.clamp(cycle, 5, 95);
        setChanged();
    }

    @Override
    public CompoundTag getSettings(Level world, BlockPos pos) {
        CompoundTag data = new CompoundTag();
        data.putInt("cycle", cycle);
        return data;
    }

    @Override
    public void pasteSettings(CompoundTag nbt, int index, Level world, Player player, BlockPos pos) {
        if (nbt.contains("cycle")) {
            this.cycle = Mth.clamp(nbt.getInt("cycle"), 5, 95);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("piston", piston);
        tag.putBoolean("ret", isRetracting);
        tag.putInt("delay", delay);
        tag.putInt("cycle", cycle);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        piston = tag.getDouble("piston");
        isRetracting = tag.contains("ret") ? tag.getBoolean("ret") : tag.getBoolean("retracting");
        delay = tag.getInt("delay");
        if (tag.contains("cycle")) cycle = tag.getInt("cycle");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeDouble(this.piston);
        buf.writeInt(this.cycle);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.lastPiston = this.piston;
        this.piston = buf.readDouble();
        this.cycle = buf.readInt();
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
