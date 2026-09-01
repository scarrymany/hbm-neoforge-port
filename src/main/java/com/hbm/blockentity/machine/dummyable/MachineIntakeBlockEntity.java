package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.container.machine.dummyable.IntakeMenu;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * CE {@code TileEntityMachineIntake} — spend {@code maxPower/20} HE to fill AIR tank.
 * Audio / fan spin skipped.
 */
public class MachineIntakeBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardSenderMK2, ITickableBE, MenuProvider {

    public static final long MAX_POWER = 2_000L;
    public final FluidTankNTM compair;
    public long power;

    public MachineIntakeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 0, true, true);
        this.compair = new FluidTankNTM(Fluids.AIR, 1_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm.machine_intake");
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (power >= getMaxPower() / 20) {
            compair.setFill(compair.getMaxFill());
            power -= getMaxPower() / 20;
        }

        for (DirPos pos : getConPos()) {
            if (compair.getFill() > 0) tryProvide(compair, level, pos);
            trySubscribe(level, pos);
        }

        dataChanged();
        networkPackMK2(50);
    }

    public DirPos[] getConPos() {
        Direction dir = coreFacing();
        Direction rot = dir.getClockWise();
        BlockPos p = worldPosition;
        return new DirPos[]{
                new DirPos(p.relative(dir), dir),
                new DirPos(p.relative(dir).relative(rot), dir),
                new DirPos(p.relative(dir, -2), dir.getOpposite()),
                new DirPos(p.relative(dir, -2).relative(rot), dir.getOpposite()),
                new DirPos(p.relative(rot, 2), rot),
                new DirPos(p.relative(rot, 2).relative(dir, -1), rot),
                new DirPos(p.relative(rot, -1), rot.getOpposite()),
                new DirPos(p.relative(rot, -1).relative(dir, -1), rot.getOpposite())
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
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(compair);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(compair);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        compair.writeToNBT(tag, "compair");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        compair.readFromNBT(tag, "compair");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        compair.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        compair.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new IntakeMenu(id, inv, this);
    }
}
