package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.container.machine.dummyable.ExposureChamberMenu;
import com.hbm.inventory.recipes.ExposureChamberRecipes;
import com.hbm.inventory.recipes.ExposureChamberRecipes.ExposureChamberRecipe;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
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

/**
 * CE {@code TileEntityMachineExposureChamber}: 200t / 10k HE, 8 saved particles, SPEED/POWER/OVERDRIVE.
 */
public class MachineExposureChamberBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, ITickableBE, MenuProvider {

    public static final long MAX_POWER = 1_000_000;
    public static final int PROCESS_TIME_BASE = 200;
    public static final int CONSUMPTION_BASE = 10_000;
    public static final int MAX_PARTICLES = 8;

    public long power;
    public int progress;
    public int processTime = PROCESS_TIME_BASE;
    public int consumption = CONSUMPTION_BASE;
    public int savedParticles;
    public boolean isOn;

    public MachineExposureChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 8, false, true);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.exposureChamber");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 5) return Library.isBattery(stack);
        if (slot == 6 || slot == 7) return stack.getItem() instanceof ItemMachineUpgrade;
        if (slot == 2 || slot == 4) return false;
        return slot == 0 || slot == 3;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == 2 || slot == 4;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 2, 3, 4};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        isOn = false;
        power = Library.chargeTEFromItems(inventory, 5, power, MAX_POWER);

        if (level.getGameTime() % 20 == 0) {
            for (DirPos pos : getConPos()) trySubscribe(level, pos);
        }

        int speed = upgrade(UpgradeType.SPEED);
        int powerLevel = upgrade(UpgradeType.POWER);
        int overdrive = upgrade(UpgradeType.OVERDRIVE);
        consumption = CONSUMPTION_BASE;
        processTime = PROCESS_TIME_BASE - PROCESS_TIME_BASE / 4 * speed;
        consumption *= (speed / 2 + 1);
        processTime *= (powerLevel / 2 + 1);
        consumption /= (powerLevel + 1);
        processTime /= (overdrive + 1);
        consumption *= (overdrive * 2 + 1);

        if (inventory.getStackInSlot(1).isEmpty() && !inventory.getStackInSlot(0).isEmpty()
                && !inventory.getStackInSlot(3).isEmpty() && savedParticles <= 0) {
            ExposureChamberRecipe recipe = ExposureChamberRecipes.getRecipe(inventory.getStackInSlot(0), inventory.getStackInSlot(3));
            if (recipe != null) {
                ItemStack one = inventory.getStackInSlot(0).copy();
                one.setCount(1);
                inventory.setStackInSlot(1, one);
                inventory.extractItem(0, 1, false);
                savedParticles = MAX_PARTICLES;
            }
        }

        if (!inventory.getStackInSlot(1).isEmpty() && savedParticles > 0 && power >= consumption) {
            ExposureChamberRecipe recipe = ExposureChamberRecipes.getRecipe(inventory.getStackInSlot(1), inventory.getStackInSlot(3));
            if (recipe != null && canInsertOutput(recipe.output)) {
                progress++;
                power -= consumption;
                isOn = true;
                if (progress >= processTime) {
                    progress = 0;
                    savedParticles--;
                    inventory.extractItem(3, 1, false);
                    insertOutput(recipe.output.copy());
                }
            } else {
                progress = 0;
            }
        } else {
            progress = 0;
        }

        if (savedParticles <= 0) inventory.setStackInSlot(1, ItemStack.EMPTY);

        dataChanged();
        networkPackMK2(50);
    }

    private boolean canInsertOutput(ItemStack out) {
        ItemStack dest = inventory.getStackInSlot(4);
        if (dest.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(dest, out) && dest.getCount() + out.getCount() <= dest.getMaxStackSize();
    }

    private void insertOutput(ItemStack out) {
        ItemStack dest = inventory.getStackInSlot(4);
        if (dest.isEmpty()) inventory.setStackInSlot(4, out);
        else dest.grow(out.getCount());
    }

    private int upgrade(UpgradeType type) {
        int level = 0;
        for (int s = 6; s <= 7; s++) {
            ItemStack st = inventory.getStackInSlot(s);
            if (st.getItem() instanceof ItemMachineUpgrade u && u.getType() == type) {
                level = Math.max(level, u.getTier());
            }
        }
        return Math.min(level, 3);
    }

    public DirPos[] getConPos() {
        Direction dir = coreFacing();
        Direction rot = dir.getCounterClockWise();
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + rot.getStepX() * 7 + dir.getStepX() * 2, y, z + rot.getStepZ() * 7 + dir.getStepZ() * 2, dir),
                new DirPos(x + rot.getStepX() * 7 - dir.getStepX() * 2, y, z + rot.getStepZ() * 7 - dir.getStepZ() * 2, dir.getOpposite()),
                new DirPos(x + rot.getStepX() * 8 + dir.getStepX() * 2, y, z + rot.getStepZ() * 8 + dir.getStepZ() * 2, dir),
                new DirPos(x + rot.getStepX() * 8 - dir.getStepX() * 2, y, z + rot.getStepZ() * 8 - dir.getStepZ() * 2, dir.getOpposite()),
                new DirPos(x + rot.getStepX() * 9, y, z + rot.getStepZ() * 9, rot),
        };
    }

    private Direction coreFacing() {
        int meta = getBlockState().getValue(BlockDummyable.META);
        return meta >= 12 ? Direction.from3DDataValue(meta - BlockDummyable.offset) : Direction.NORTH;
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
        tag.putInt("savedParticles", savedParticles);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        progress = tag.getInt("progress");
        savedParticles = tag.getInt("savedParticles");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(progress);
        buf.writeInt(processTime);
        buf.writeInt(savedParticles);
        buf.writeBoolean(isOn);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        progress = buf.readInt();
        processTime = buf.readInt();
        savedParticles = buf.readInt();
        isOn = buf.readBoolean();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ExposureChamberMenu(id, inv, this);
    }
}
