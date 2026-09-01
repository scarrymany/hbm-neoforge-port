package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.CondenserMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidIdentifier;
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
 * CE {@code TileEntityCondenser} / {@code TileEntityTowerSmall} / {@code TileEntityTowerLarge} —
 * spentsteam → water 1:1. Particles skipped.
 */
public class CondenserBlockEntity extends MachineBaseBlockEntity
        implements IFluidStandardTransceiverMK2, ITickableBE, MenuProvider {

    public final FluidTankNTM input;
    public final FluidTankNTM output;
    public int waterTimer;

    public static CondenserBlockEntity cube(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        return new CondenserBlockEntity(type, pos, state, 100, 100);
    }

    public static CondenserBlockEntity towerSmall(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        return new CondenserBlockEntity(type, pos, state, 1_000, 1_000);
    }

    public static CondenserBlockEntity towerLarge(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        return new CondenserBlockEntity(type, pos, state, 10_000, 10_000);
    }

    public CondenserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int inCap, int outCap) {
        this(type, pos, state, inCap, outCap, false);
    }

    public CondenserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int inCap, int outCap, boolean energy) {
        super(type, pos, state, 1, true, energy);
        this.input = new FluidTankNTM(Fluids.SPENTSTEAM, inCap).withOwner(this);
        this.output = new FluidTankNTM(Fluids.WATER, outCap).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineCondenser");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && stack.getItem() instanceof IItemFluidIdentifier;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        ItemStack id = inventory.getStackInSlot(0);
        if (!id.isEmpty() && id.getItem() instanceof IItemFluidIdentifier ident) {
            input.setTankType(ident.getType(level, worldPosition, id));
        }

        if (waterTimer > 0) waterTimer--;
        int convert = Math.min(input.getFill(), output.getMaxFill() - output.getFill());
        if (convert > 0 && extraCondition(convert)) {
            input.setFill(input.getFill() - convert);
            output.setFill(output.getFill() + convert);
            waterTimer = 20;
            postConvert(convert);
        }

        if (level.getGameTime() % 20 == 0) {
            for (Direction d : Direction.values()) {
                trySubscribe(input.getTankType(), level, worldPosition.relative(d), d);
                if (output.getFill() > 0) tryProvide(output, level, worldPosition.relative(d), d);
            }
        }

        dataChanged();
        networkPackMK2(50);
    }

    protected boolean extraCondition(int convert) {
        return true;
    }

    protected void postConvert(int convert) {
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(input);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(output);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(input, output);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        input.writeToNBT(tag, "in");
        output.writeToNBT(tag, "out");
        tag.putInt("wt", waterTimer);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        input.readFromNBT(tag, "in");
        output.readFromNBT(tag, "out");
        waterTimer = tag.getInt("wt");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        input.serialize(buf);
        output.serialize(buf);
        buf.writeInt(waterTimer);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        input.deserialize(buf);
        output.deserialize(buf);
        waterTimer = buf.readInt();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new CondenserMenu(id, inv, this);
    }
}
