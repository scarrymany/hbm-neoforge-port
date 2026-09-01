package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.container.machine.dummyable.BigAssTankMenu;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.lib.DirPos;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * CE {@code TileEntityMachineBigAssTank} extends {@code TileEntityBarrel} — 16M, barrel GUI.
 * TODO(CE: TileEntityBarrel.java:247-286): UniNodespace buffer-mode node.
 * TODO(CE: TileEntityBarrel.java): tilt / floor pollute.
 * TODO(CE: RenderBigAssTank.java:1): TESR.
 */
public class MachineBigAssTankBlockEntity extends MachineBaseBlockEntity
        implements IFluidStandardTransceiverMK2, ITickableBE, MenuProvider {

    public static final int CAPACITY = 16_000_000;
    public static final short MODES = 4;

    public final FluidTankNTM tank;
    public short mode;

    public MachineBigAssTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 6, true, false);
        this.tank = new FluidTankNTM(Fluids.NONE, CAPACITY).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.bigAssTank");
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return null;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case 0, 1 -> stack.getItem() instanceof IItemFluidIdentifier;
            default -> true;
        };
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack itemStack) {
        return switch (slot) {
            case 1, 3, 5 -> false;
            default -> isItemValidForSlot(slot, itemStack);
        };
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
        return switch (slot) {
            case 1, 3, 5 -> true;
            default -> !isItemValidForSlot(slot, itemStack);
        };
    }

    public void cycleMode() {
        mode = (short) ((mode + 1) % MODES);
        setChanged();
    }

    @Override
    public long getReceiverSpeed(FluidType type, int pressure) {
        return Math.max(50_000, (tank.getMaxFill() - tank.getFill()) / 100);
    }

    @Override
    public long getProviderSpeed(FluidType type, int pressure) {
        return Math.max(50_000, tank.getFill() / 100);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        // TODO(CE: TileEntityMachineBigAssTank.java:31): checkTilt(UNAVOIDABLE)

        tank.setType(0, 1, inventory);
        tank.loadTank(2, 3, inventory);
        tank.unloadTank(4, 5, inventory);

        if (tank.getFill() > 0 && tank.getTankType().isAntimatter()) {
            BlockPos p = worldPosition;
            level.destroyBlock(p, false);
            level.explode(null, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                    10.0F, true, Level.ExplosionInteraction.TNT);
            return;
        }

        if (mode != 3) {
            for (DirPos pos : getConPos()) {
                if (mode == 0 || mode == 1) trySubscribe(tank.getTankType(), level, pos);
                if ((mode == 1 || mode == 2) && tank.getFill() > 0) tryProvide(tank, level, pos);
            }
        }

        dataChanged();
        networkPackMK2(50);
    }

    public DirPos[] getConPos() {
        Direction dir = coreFacing();
        return new DirPos[]{
                new DirPos(worldPosition.relative(dir, 7), dir),
                new DirPos(worldPosition.relative(dir, -7), dir.getOpposite())
        };
    }

    private Direction coreFacing() {
        int meta = getBlockState().getValue(BlockDummyable.META);
        return meta >= 12 ? Direction.from3DDataValue(meta - BlockDummyable.offset) : Direction.NORTH;
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return (mode == 0 || mode == 1) ? List.of(tank) : List.of();
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return (mode == 1 || mode == 2) ? List.of(tank) : List.of();
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(tank);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tank.writeToNBT(tag, "tank");
        tag.putShort("mode", mode);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(tag, "tank");
        mode = tag.getShort("mode");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeShort(mode);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        mode = buf.readShort();
        tank.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new BigAssTankMenu(id, inv, this);
    }
}
