package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.container.machine.dummyable.RadGenMenu;
import com.hbm.inventory.recipes.RadGenRecipes;
import com.hbm.inventory.recipes.RadGenRecipes.RadGenFuel;
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
 * CE {@code TileEntityMachineRadGen}: waste → HE over duration. Depleted leftovers skipped.
 */
public class MachineRadGenBlockEntity extends MachineBaseBlockEntity
        implements IEnergyProviderMK2, ITickableBE, MenuProvider {

    public static final long MAX_POWER = 1_000_000;

    public long power;
    public int burnTime;
    public int maxBurnTime;
    public int production;

    public MachineRadGenBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 24, false, true);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineRadGen");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot < 12 && RadGenRecipes.getFuel(stack) != null;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot >= 12;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (burnTime <= 0) {
            production = 0;
            for (int i = 0; i < 12; i++) {
                ItemStack in = inventory.getStackInSlot(i);
                RadGenFuel fuel = RadGenRecipes.getFuel(in);
                if (fuel == null) continue;
                if (!fuel.leftover.isEmpty() && !insertLeftover(fuel.leftover.copy())) continue;
                inventory.extractItem(i, 1, false);
                burnTime = maxBurnTime = fuel.duration;
                production = fuel.powerPerTick;
                setChanged();
                break;
            }
        }

        if (burnTime > 0) {
            burnTime--;
            power = Math.min(MAX_POWER, power + production);
        }

        power = Library.chargeItemsFromTE(inventory, 12, power, MAX_POWER);
        for (DirPos pos : getConPos()) {
            tryProvide(level, pos.getPos(), pos.getDir());
        }
        dataChanged();
        networkPackMK2(50);
    }

    private boolean insertLeftover(ItemStack out) {
        for (int i = 12; i < 24; i++) {
            ItemStack dest = inventory.getStackInSlot(i);
            if (dest.isEmpty()) {
                inventory.setStackInSlot(i, out);
                return true;
            }
            if (ItemStack.isSameItemSameComponents(dest, out) && dest.getCount() + out.getCount() <= dest.getMaxStackSize()) {
                dest.grow(out.getCount());
                return true;
            }
        }
        return false;
    }

    public DirPos[] getConPos() {
        Direction dir = coreFacing();
        return new DirPos[]{
                new DirPos(worldPosition.relative(dir, -5), dir.getOpposite()),
                new DirPos(worldPosition.relative(dir.getClockWise()), dir.getClockWise()),
                new DirPos(worldPosition.relative(dir.getCounterClockWise()), dir.getCounterClockWise()),
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
        tag.putInt("burn", burnTime);
        tag.putInt("maxBurn", maxBurnTime);
        tag.putInt("prod", production);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        burnTime = tag.getInt("burn");
        maxBurnTime = tag.getInt("maxBurn");
        production = tag.getInt("prod");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(burnTime);
        buf.writeInt(maxBurnTime);
        buf.writeInt(production);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        burnTime = buf.readInt();
        maxBurnTime = buf.readInt();
        production = buf.readInt();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new RadGenMenu(id, inv, this);
    }
}
