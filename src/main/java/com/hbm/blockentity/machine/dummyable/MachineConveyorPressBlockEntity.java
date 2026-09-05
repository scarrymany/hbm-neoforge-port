package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.entity.ConveyorEntityTypes;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.inventory.container.machine.dummyable.ConveyorPressMenu;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.items.machine.ItemStamp;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * CE {@code TileEntityConveyorPress}: 50k HE, 100 HE/t, stamp slot 0,
 * {@code PressRecipes.getOutput} on {@code EntityMovingItem} in the AABB above the core.
 * Stamp sound Exact CE {@code TileEntityConveyorPress.java:166}: {@code pressOperate} {@code getVolume(1.5F)}/1.0F.
 * This BE has no CE muffled flag — unmuffled volume is 1.5F.
 */
public class MachineConveyorPressBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, ITickableBE, MenuProvider {

    public static final long MAX_POWER = 50_000;
    public static final int USAGE = 100;
    public static final double SPEED = 0.125;

    public int usage = USAGE;
    public long power;
    public double press;
    public double renderPress;
    public double lastPress;
    private double syncPress;
    private int turnProgress;
    private boolean retracting;
    private int delay;
    public ItemStack syncStack = ItemStack.EMPTY;

    public MachineConveyorPressBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1, false, true);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.conveyor_press");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && stack.getItem() instanceof ItemStamp;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == 0;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0};
    }

    @Override
    public boolean canConnect(Direction dir) {
        return dir != Direction.DOWN;
    }

    @Override
    public void updateEntity() {
        if (level == null) return;

        if (!level.isClientSide) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                trySubscribe(level, worldPosition.relative(dir), dir);
            }

            if (delay <= 0) {
                if (retracting) {
                    if (canRetract()) {
                        press -= SPEED;
                        power -= usage;
                        if (press <= 0) {
                            press = 0;
                            retracting = false;
                            delay = 0;
                        }
                    }
                } else if (canExtend()) {
                    press += SPEED;
                    power -= usage;
                    if (press >= 1) {
                        press = 1;
                        retracting = true;
                        delay = 5;
                        process();
                    }
                }
            } else {
                delay--;
            }

            dataChanged();
            networkPackMK2(50);
        } else {
            lastPress = renderPress;
            if (turnProgress > 0) {
                renderPress = renderPress + ((syncPress - renderPress) / turnProgress);
                turnProgress--;
            } else {
                renderPress = syncPress;
            }
        }
    }

    private AABB itemBox() {
        return new AABB(
                worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(),
                worldPosition.getX() + 1, worldPosition.getY() + 1.5, worldPosition.getZ() + 1);
    }

    public boolean canExtend() {
        if (power < usage) return false;
        if (inventory.getStackInSlot(0).isEmpty()) return false;
        if (level == null) return false;

        List<EntityMovingItem> items = level.getEntitiesOfClass(EntityMovingItem.class, itemBox());
        if (items.isEmpty()) return false;

        for (EntityMovingItem item : items) {
            ItemStack stack = item.getItemStack();
            if (!PressRecipes.getOutput(stack, inventory.getStackInSlot(0)).isEmpty() && stack.getCount() == 1) {
                double d0 = 0.35;
                double d1 = 0.65;
                if (item.getX() > worldPosition.getX() + d0 && item.getX() < worldPosition.getX() + d1
                        && item.getZ() > worldPosition.getZ() + d0 && item.getZ() < worldPosition.getZ() + d1) {
                    item.setPos(worldPosition.getX() + 0.5, item.getY(), worldPosition.getZ() + 0.5);
                }
                return true;
            }
        }
        return false;
    }

    public void process() {
        if (level == null || level.isClientSide) return;

        List<EntityMovingItem> items = level.getEntitiesOfClass(EntityMovingItem.class, itemBox());
        for (EntityMovingItem item : items) {
            ItemStack stack = item.getItemStack();
            ItemStack output = PressRecipes.getOutput(stack, inventory.getStackInSlot(0));
            if (!output.isEmpty() && stack.getCount() == 1) {
                item.discard();
                EntityMovingItem out = new EntityMovingItem(ConveyorEntityTypes.MOVING_ITEM.get(), level);
                out.setPos(item.getX(), item.getY(), item.getZ());
                out.setItemStack(output.copy());
                level.addFreshEntity(out);
            }
        }

        level.playSound(null, worldPosition, HBMSoundHandler.pressOperate.get(), SoundSource.BLOCKS, 1.5F, 1.0F);

        ItemStack stamp = inventory.getStackInSlot(0);
        if (stamp.isDamageableItem()) {
            stamp.setDamageValue(stamp.getDamageValue() + 1);
            if (stamp.getDamageValue() >= stamp.getMaxDamage()) {
                inventory.setStackInSlot(0, ItemStack.EMPTY);
            }
        }
        setChanged();
    }

    public boolean canRetract() {
        return power >= usage;
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
        tag.putDouble("press", press);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        press = tag.getDouble("press");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeDouble(press);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, inventory.getStackInSlot(0));
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        syncPress = buf.readDouble();
        syncStack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        if (!syncStack.isEmpty()) {
            inventory.setStackInSlot(0, syncStack);
        }
        turnProgress = 2;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ConveyorPressMenu(id, inv, this);
    }
}
