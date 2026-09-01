package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.container.machine.dummyable.StrandCasterMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.machine.ItemMold;
import com.hbm.items.machine.ItemScraps;
import com.hbm.lib.DirPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * CE {@code TileEntityMachineStrandCaster} (412 lines). Foundry base inlined — this TE
 * overrides the basin cooloff path with the continuous 9-cast / 200-tick flush.
 */
public class MachineStrandCasterBlockEntity extends MachineBaseBlockEntity
        implements IFluidStandardTransceiverMK2, ICrucibleAcceptor, ITickableBE, MenuProvider {

    public final FluidTankNTM water;
    public final FluidTankNTM steam;
    public NTMMaterial type;
    public int amount;
    private NTMMaterial lastType;
    private int lastAmount;
    private long lastProgressTick;

    public MachineStrandCasterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 7, true, false);
        this.water = new FluidTankNTM(Fluids.WATER, 64_000).withOwner(this);
        this.steam = new FluidTankNTM(Fluids.SPENTSTEAM, 64_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineStrandCaster");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && stack.getItem() instanceof ItemMold;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot >= 1 && slot <= 6;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{1, 2, 3, 4, 5, 6};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (this.lastType != this.type || this.lastAmount != this.amount) {
            this.lastType = this.type;
            this.lastAmount = this.amount;
            setChanged();
        }

        if (amount > getCapacity()) {
            if (this.type != null) {
                ItemStack scrap = ItemScraps.create(new Mats.MaterialStack(this.type, Math.max(amount - getCapacity(), 0)), false);
                level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 2, worldPosition.getZ() + 0.5, scrap));
            }
            this.amount = getCapacity();
        }
        if (this.amount == 0) this.type = null;

        updateConnections();

        int moldsToCast = maxProcessable();
        if (moldsToCast > 0 && (moldsToCast >= 9 || level.getGameTime() >= lastProgressTick + 200)) {
            ItemMold.MoldEntry mold = getInstalledMold();
            if (mold == null) return;
            this.amount -= moldsToCast * mold.getCost();
            ItemStack out = mold.getOutput(this.type);
            int itemsPerCast = out.getCount();
            int remaining = itemsPerCast * moldsToCast;
            int itemMax = out.getMaxStackSize();

            for (int i = 1; i < 7 && remaining > 0; i++) {
                ItemStack slot = inventory.getStackInSlot(i);
                int limit = Math.min(inventory.getSlotLimit(i), itemMax);
                if (slot.isEmpty()) {
                    int toDeposit = Math.min(remaining, limit);
                    if (toDeposit > 0) {
                        ItemStack put = out.copy();
                        put.setCount(toDeposit);
                        inventory.setStackInSlot(i, put);
                        remaining -= toDeposit;
                    }
                    continue;
                }
                if (ItemStack.isSameItemSameComponents(slot, out)) {
                    int toDeposit = Math.min(remaining, limit - slot.getCount());
                    if (toDeposit > 0) {
                        slot.grow(toDeposit);
                        inventory.setStackInSlot(i, slot);
                        remaining -= toDeposit;
                    }
                }
            }

            int produced = itemsPerCast * moldsToCast - remaining;
            int castsMade = itemsPerCast == 0 ? 0 : produced / itemsPerCast;
            if (castsMade > 0) {
                water.setFill(water.getFill() - getWaterRequired() * castsMade);
                steam.setFill(steam.getFill() + getWaterRequired() * castsMade);
                lastProgressTick = level.getGameTime();
                setChanged();
            }
        }

        dataChanged();
        networkPackMK2(150);
    }

    private int maxProcessable() {
        ItemMold.MoldEntry mold = getInstalledMold();
        if (type == null || mold == null || mold.getOutput(type).isEmpty()) return 0;
        int freeSlots = 0;
        int stackLimit = mold.getOutput(type).getMaxStackSize();
        for (int i = 1; i < 7; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) freeSlots += stackLimit;
            else if (ItemStack.isSameItemSameComponents(stack, mold.getOutput(type))) {
                freeSlots += stackLimit - stack.getCount();
            }
        }
        int casts = amount / mold.getCost();
        casts = Math.min(casts, freeSlots / mold.getOutput(type).getCount());
        casts = Math.min(casts, water.getFill() / getWaterRequired());
        casts = Math.min(casts, (steam.getMaxFill() - steam.getFill()) / getWaterRequired());
        return casts;
    }

    public DirPos[] getFluidConPos() {
        Direction dir = coreDirection();
        Direction rot = dir.getClockWise();
        BlockPos p = worldPosition;
        return new DirPos[]{
                new DirPos(p.getX() + rot.getStepX() * 2 - dir.getStepX(), p.getY(), p.getZ() + rot.getStepZ() * 2 - dir.getStepZ(), rot),
                new DirPos(p.getX() - rot.getStepX() - dir.getStepX(), p.getY(), p.getZ() - rot.getStepZ() - dir.getStepZ(), rot.getOpposite()),
                new DirPos(p.getX() + rot.getStepX() * 2 - dir.getStepX() * 5, p.getY(), p.getZ() + rot.getStepZ() * 2 - dir.getStepZ() * 5, rot),
                new DirPos(p.getX() - rot.getStepX() - dir.getStepX() * 5, p.getY(), p.getZ() - rot.getStepZ() - dir.getStepZ() * 5, rot.getOpposite())
        };
    }

    public int[][] getMetalPourPos() {
        Direction dir = coreDirection();
        Direction rot = dir.getClockWise();
        BlockPos p = worldPosition;
        return new int[][]{
                {p.getX() + rot.getStepX() - dir.getStepX(), p.getY() + 2, p.getZ() + rot.getStepZ() - dir.getStepZ()},
                {p.getX() - dir.getStepX(), p.getY() + 2, p.getZ() - dir.getStepZ()},
                {p.getX() + rot.getStepX(), p.getY() + 2, p.getZ() + rot.getStepZ()},
                {p.getX(), p.getY() + 2, p.getZ()}
        };
    }

    private Direction coreDirection() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockDummyable.META)
                ? Direction.from3DDataValue(state.getValue(BlockDummyable.META) - BlockDummyable.offset)
                : Direction.NORTH;
    }

    public ItemMold.MoldEntry getInstalledMold() {
        ItemStack stack = inventory.getStackInSlot(0);
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemMold)) return null;
        return ItemMold.getMold(stack);
    }

    public int getCapacity() {
        ItemMold.MoldEntry mold = getInstalledMold();
        return mold == null ? 50_000 : mold.getCost() * 10;
    }

    private int getWaterRequired() {
        ItemMold.MoldEntry mold = getInstalledMold();
        return mold != null ? 5 * mold.getCost() : 50;
    }

    private void updateConnections() {
        for (DirPos pos : getFluidConPos()) {
            trySubscribe(water.getTankType(), level, pos);
            if (steam.getFill() > 0) tryProvide(steam, level, pos);
        }
    }

    public boolean standardCheck(Level world, BlockPos pos, Direction side, Mats.MaterialStack stack) {
        if (this.type != null && this.type != stack.material) return false;
        int limit = getInstalledMold() != null ? getInstalledMold().getCost() * 9 : getCapacity();
        return !(this.amount >= limit || getInstalledMold() == null);
    }

    public Mats.MaterialStack standardAdd(Level world, BlockPos pos, Direction side, Mats.MaterialStack stack) {
        this.type = stack.material;
        int limit = getInstalledMold() != null ? getInstalledMold().getCost() * 9 : getCapacity();
        if (stack.amount + this.amount <= limit) {
            this.amount += stack.amount;
            return null;
        }
        int required = limit - this.amount;
        this.amount = limit;
        stack.amount -= required;
        lastProgressTick = world.getGameTime();
        return stack;
    }

    @Override
    public boolean canAcceptPartialPour(Level level, BlockPos pos, double dX, double dY, double dZ, Direction side, Mats.MaterialStack stack) {
        if (side != Direction.UP) return false;
        for (int[] pourPos : getMetalPourPos()) {
            if (pourPos[0] == pos.getX() && pourPos[1] == pos.getY() && pourPos[2] == pos.getZ()) {
                return standardCheck(level, pos, side, stack);
            }
        }
        return false;
    }

    @Override
    public Mats.MaterialStack pour(Level level, BlockPos pos, double dX, double dY, double dZ, Direction side, Mats.MaterialStack stack) {
        return standardAdd(level, pos, side, stack);
    }

    @Override
    public boolean canAcceptPartialFlow(Level level, BlockPos pos, Direction side, Mats.MaterialStack stack) {
        return false;
    }

    @Override
    public Mats.MaterialStack flow(Level level, BlockPos pos, Direction side, Mats.MaterialStack stack) {
        return null;
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(steam);
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(water);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(water, steam);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        water.writeToNBT(tag, "w");
        steam.writeToNBT(tag, "s");
        tag.putLong("t", lastProgressTick);
        tag.putInt("type", this.type == null ? -1 : this.type.id);
        tag.putInt("amount", this.amount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        water.readFromNBT(tag, "w");
        steam.readFromNBT(tag, "s");
        lastProgressTick = tag.getLong("t");
        this.type = Mats.matById.get(tag.getInt("type"));
        this.amount = tag.getInt("amount");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        water.serialize(buf);
        steam.serialize(buf);
        buf.writeInt(this.type == null ? -1 : this.type.id);
        buf.writeInt(this.amount);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        water.deserialize(buf);
        steam.deserialize(buf);
        this.type = Mats.matById.get(buf.readInt());
        this.amount = buf.readInt();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new StrandCasterMenu(id, inv, this);
    }
}
