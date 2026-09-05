package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.HexTankMenu;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * CE {@code TileEntityMachineUF6Tank} / {@code TileEntityMachinePuF6Tank} — 64k fixed-type tank.
 * Canister {@code loadTank(0,1)}/{@code unloadTank(2,3)} Exact CE {@code :68-78}.
 * Comparator {@code lastRedstone} Exact CE {@code :71-78}.
 */
public class HexTankBlockEntity extends MachineBaseBlockEntity
        implements IFluidStandardTransceiverMK2, ITickableBE, MenuProvider {

    public static final int CAPACITY = 64_000;
    public final FluidTankNTM tank;
    /** CE {@code TileEntityMachineUF6Tank.java:27}. */
    public byte lastRedstone = 0;

    public static HexTankBlockEntity uf6(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        return new HexTankBlockEntity(type, pos, state, Fluids.UF6, "container.uf6_tank");
    }

    public static HexTankBlockEntity puf6(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        return new HexTankBlockEntity(type, pos, state, Fluids.PUF6, "block.hbm.machine_puf6_tank");
    }

    private final String nameKey;

    public HexTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, FluidType fluid, String nameKey) {
        super(type, pos, state, 4, true, false);
        this.tank = new FluidTankNTM(fluid, CAPACITY).withOwner(this);
        this.nameKey = nameKey;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(nameKey);
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack itemStack) {
        return slot != 1 && slot != 3 && isItemValidForSlot(slot, itemStack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
        return slot == 1 || slot == 3;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        // CE TileEntityMachineUF6Tank.java:68-69
        tank.loadTank(0, 1, inventory);
        tank.unloadTank(2, 3, inventory);

        // CE TileEntityMachineUF6Tank.java:71-78
        byte comp = tank.getRedstoneComparatorPower();
        if (comp != this.lastRedstone) {
            setChanged();
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
        this.lastRedstone = comp;

        if (level.getGameTime() % 20 == 0) {
            for (Direction d : Direction.values()) {
                DirPos p = new DirPos(worldPosition.relative(d), d);
                trySubscribe(tank.getTankType(), level, p);
                if (tank.getFill() > 0) tryProvide(tank, level, p);
            }
        }
        dataChanged();
        networkPackMK2(50);
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(tank);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(tank);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(tank);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tank.writeToNBT(tag, "tank");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(tag, "tank");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tank.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new HexTankMenu(id, inv, this);
    }
}
