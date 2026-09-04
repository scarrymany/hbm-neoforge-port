package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.container.machine.dummyable.AnnihilatorMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Polluting;
import com.hbm.inventory.fluid.trait.FluidTrait;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.lib.DirPos;
import com.hbm.saveddata.AnnihilatorSavedData;
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
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.List;

/**
 * CE {@code TileEntityMachineAnnihilator}: eats items/fluids into a named pool, pays milestones.
 * {@code tank.setType(1)} Exact CE {@code :71}. Slot 1 Exact CE {@code :194}.
 * {@code FT_Polluting.pollute(BURN, fill*2)} Exact CE {@code :91}.
 * Flame / audio stay skipped.
 */
public class MachineAnnihilatorBlockEntity extends MachineBaseBlockEntity
        implements IFluidStandardReceiverMK2, ITickableBE, MenuProvider {

    public String pool = "Recycling";
    public final FluidTankNTM tank;
    public BigInteger monitorBigInt = BigInteger.ZERO;

    public MachineAnnihilatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 11, true, false);
        this.tank = new FluidTankNTM(Fluids.NONE, 2_500_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.annihilator");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return true;
        // CE TileEntityMachineAnnihilator.java:194
        if (slot == 1) return stack.getItem() instanceof IItemFluidIdentifier;
        if (slot == 8 || slot == 9) return true;
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot >= 2 && slot <= 7;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 2, 3, 4, 5, 6, 7};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        // CE TileEntityMachineAnnihilator.java:71
        this.tank.setType(1, inventory);

        if (pool == null || pool.isEmpty()) return;

        for (DirPos pos : getConPos()) {
            if (tank.getTankType() != Fluids.NONE) trySubscribe(tank.getTankType(), level, pos);
        }

        AnnihilatorSavedData data = AnnihilatorSavedData.getData(level);
        ItemStack stack0 = inventory.getStackInSlot(0);
        if (!stack0.isEmpty()) {
            tryAddPayout(data.pushToPool(pool, stack0, false));
            inventory.setStackInSlot(0, ItemStack.EMPTY);
        }
        if (tank.getFill() > 0) {
            // CE TileEntityMachineAnnihilator.java:91
            FT_Polluting.pollute(level, worldPosition, tank.getTankType(),
                    FluidTrait.FluidReleaseType.BURN, tank.getFill() * 2);
            tryAddPayout(data.pushToPool(pool, tank.getTankType(), tank.getFill(), false));
            tank.setFill(0);
        }

        ItemStack stack8 = inventory.getStackInSlot(8);
        if (!stack8.isEmpty()) {
            AnnihilatorSavedData.AnnihilatorPool p = data.pools.get(this.pool);
            if (p != null) {
                BigInteger v = p.items.get(stack8.getItem());
                if (v == null) v = p.items.get(new com.hbm.inventory.RecipesCommon.ComparableStack(stack8.getItem()));
                monitorBigInt = v == null ? BigInteger.ZERO : v;
            } else {
                monitorBigInt = BigInteger.ZERO;
            }
        }

        ItemStack stack9 = inventory.getStackInSlot(9);
        if (!stack9.isEmpty()) {
            ItemStack single = stack9.copy();
            single.setCount(1);
            ItemStack payout = data.pushToPool(pool, single, true);
            inventory.extractItem(9, 1, false);
            if (!payout.isEmpty()) tryAddPayoutTo(10, payout);
        }

        dataChanged();
        networkPackMK2(25);
    }

    public DirPos[] getConPos() {
        Direction dir = coreFacing();
        Direction rot = dir.getClockWise();
        return new DirPos[]{
                new DirPos(worldPosition.relative(dir, 5), dir),
                new DirPos(worldPosition.relative(dir, 3).relative(rot, 2), rot),
                new DirPos(worldPosition.relative(dir, 3).relative(rot.getOpposite(), 2), rot.getOpposite())
        };
    }

    private Direction coreFacing() {
        int meta = getBlockState().getValue(BlockDummyable.META);
        return meta >= 12 ? Direction.from3DDataValue(meta - BlockDummyable.offset) : Direction.NORTH;
    }

    public void tryAddPayout(ItemStack payout) {
        if (payout == null || payout.isEmpty()) return;
        for (int i = 2; i <= 7; i++) {
            if (tryAddPayoutTo(i, payout)) return;
        }
    }

    private boolean tryAddPayoutTo(int slot, ItemStack payout) {
        ItemStack leftover = inventory.insertItem(slot, payout, false);
        return leftover.isEmpty();
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(tank);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(tank);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tank.writeToNBT(tag, "t");
        tag.putString("pool", pool == null ? "" : pool);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(tag, "t");
        pool = tag.getString("pool");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeUtf(pool == null ? "" : pool);
        byte[] array = monitorBigInt.toByteArray();
        buf.writeInt(array.length);
        buf.writeBytes(array);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        pool = buf.readUtf();
        byte[] array = new byte[buf.readInt()];
        buf.readBytes(array);
        monitorBigInt = new BigInteger(array);
        tank.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AnnihilatorMenu(id, inv, this);
    }
}
