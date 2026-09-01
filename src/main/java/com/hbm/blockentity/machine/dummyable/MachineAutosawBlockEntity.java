package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.AutosawMenu;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * CE {@code TileEntityMachineAutosaw.java}:59-101 — WOODOIL 100 mB, 1 mB/s, log harvest.
 * Entity shred / arm animation skipped.
 */
public class MachineAutosawBlockEntity extends MachineBaseBlockEntity
        implements IFluidStandardReceiverMK2, ITickableBE, MenuProvider {

    public static final int MIN_DIST = 2;
    public static final int MAX_DIST = 9;

    public final FluidTankNTM tank;
    public boolean isOn;
    public boolean isSuspended;

    public MachineAutosawBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1, true, false);
        this.tank = new FluidTankNTM(Fluids.WOODOIL, 100).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineAutosaw");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && stack.getItem() instanceof IItemFluidIdentifier;
    }

    public static boolean acceptedFuel(FluidType type) {
        return type == Fluids.WOODOIL || type == Fluids.ETHANOL || type == Fluids.FISHOIL || type == Fluids.HEAVYOIL;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        ItemStack id = inventory.getStackInSlot(0);
        if (!id.isEmpty() && id.getItem() instanceof IItemFluidIdentifier ident) {
            FluidType type = ident.getType(level, worldPosition, id);
            if (acceptedFuel(type)) tank.setTankType(type);
        }

        if (level.getGameTime() % 20 == 0) {
            for (DirPos pos : getConPos()) trySubscribe(tank.getTankType(), level, pos);
        }

        isOn = false;
        if (!isSuspended && tank.getFill() > 0 && acceptedFuel(tank.getTankType())) {
            if (level.getGameTime() % 20 == 0) {
                tank.setFill(tank.getFill() - 1);
                harvestLog();
            }
            isOn = tank.getFill() > 0;
        }

        dataChanged();
        networkPackMK2(25);
    }

    private void harvestLog() {
        if (!(level instanceof ServerLevel server)) return;
        for (int dx = -MAX_DIST; dx <= MAX_DIST; dx++) {
            for (int dz = -MAX_DIST; dz <= MAX_DIST; dz++) {
                int dist = Math.max(Math.abs(dx), Math.abs(dz));
                if (dist < MIN_DIST || dist > MAX_DIST) continue;
                BlockPos target = worldPosition.offset(dx, 0, dz);
                BlockState state = server.getBlockState(target);
                if (state.is(BlockTags.LOGS)) {
                    Block.dropResources(state, server, target);
                    server.removeBlock(target, false);
                    return;
                }
            }
        }
    }

    public void toggleSuspended() {
        isSuspended = !isSuspended;
        setChanged();
    }

    public DirPos[] getConPos() {
        return new DirPos[]{
                new DirPos(worldPosition.below(), Direction.DOWN),
                new DirPos(worldPosition.north(), Direction.NORTH),
                new DirPos(worldPosition.south(), Direction.SOUTH),
                new DirPos(worldPosition.east(), Direction.EAST),
                new DirPos(worldPosition.west(), Direction.WEST),
        };
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
        tag.putBoolean("on", isOn);
        tag.putBoolean("susp", isSuspended);
        tank.writeToNBT(tag, "t");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        isOn = tag.getBoolean("on");
        isSuspended = tag.getBoolean("susp");
        tank.readFromNBT(tag, "t");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(isOn);
        buf.writeBoolean(isSuspended);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        isOn = buf.readBoolean();
        isSuspended = buf.readBoolean();
        tank.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AutosawMenu(id, inv, this);
    }
}
