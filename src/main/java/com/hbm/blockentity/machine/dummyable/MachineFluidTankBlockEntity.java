package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.MachineFluidTankMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
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
 * CE {@code TileEntityMachineFluidTank}: 6 slots, 256000, mode 0=in / 1=both / 2=out / 3=off.
 * TODO(CE: TileEntityMachineFluidTank.java:198-235): UniNodespace pipe-mode node.
 * TODO(CE: TileEntityMachineFluidTank.java:70): OC / IControllable / IClimbable / IRepairable.
 * TODO(CE: TileEntityMachineFluidTank.java:253-256): ExplosionVNT.makeAmat.
 * TODO(CE: TileEntityMachineFluidTank.java:263-370): post-explode leak / fire / pollute.
 */
public class MachineFluidTankBlockEntity extends MachineBaseBlockEntity
        implements IFluidStandardTransceiverMK2, ITickableBE, MenuProvider {

    public static final int CAPACITY = 256_000;
    public static final short MODES = 4;

    public final FluidTankNTM tank;
    public short mode;
    public boolean hasExploded;

    public MachineFluidTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 6, true, false);
        this.tank = new FluidTankNTM(Fluids.NONE, CAPACITY).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.fluidtank");
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
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (!hasExploded) {
            tank.loadTank(2, 3, inventory);
            tank.setType(0, 1, inventory);
        }

        if (tank.getFill() > 0) {
            if (tank.getTankType().isAntimatter()) {
                // TODO(CE: TileEntityMachineFluidTank.java:253-256): ExplosionVNT.makeAmat
                level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5,
                        5.0F, true, Level.ExplosionInteraction.TNT);
                hasExploded = true;
                tank.setFill(0);
            }
            FT_Corrosive corrosive = tank.getTankType().getTrait(FT_Corrosive.class);
            if (corrosive != null && corrosive.isHighlyCorrosive()) {
                hasExploded = true;
            }
        }

        if (!hasExploded) {
            for (DirPos pos : getConPos()) {
                if (mode == 0 || mode == 1) trySubscribe(tank.getTankType(), level, pos);
                if ((mode == 1 || mode == 2) && tank.getFill() > 0) tryProvide(tank, level, pos);
            }
        }

        tank.unloadTank(4, 5, inventory);
        dataChanged();
        networkPackMK2(150);
    }

    public DirPos[] getConPos() {
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 2, y, z - 1, Direction.EAST),
                new DirPos(x + 2, y, z + 1, Direction.EAST),
                new DirPos(x - 2, y, z - 1, Direction.WEST),
                new DirPos(x - 2, y, z + 1, Direction.WEST),
                new DirPos(x - 1, y, z + 2, Direction.SOUTH),
                new DirPos(x + 1, y, z + 2, Direction.SOUTH),
                new DirPos(x - 1, y, z - 2, Direction.NORTH),
                new DirPos(x + 1, y, z - 2, Direction.NORTH),
        };
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        if (hasExploded) return List.of();
        return (mode == 0 || mode == 1) ? List.of(tank) : List.of();
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        if (hasExploded) return List.of();
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
        tag.putBoolean("hasExploded", hasExploded);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(tag, "tank");
        mode = tag.getShort("mode");
        hasExploded = tag.getBoolean("hasExploded");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeShort(mode);
        buf.writeBoolean(hasExploded);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        mode = buf.readShort();
        hasExploded = buf.readBoolean();
        tank.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MachineFluidTankMenu(id, inv, this);
    }
}
