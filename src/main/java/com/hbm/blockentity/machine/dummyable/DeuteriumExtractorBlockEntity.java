package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.container.machine.dummyable.DeuteriumMenu;
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
 * CE {@code TileEntityDeuteriumExtractor} / {@code TileEntityDeuteriumTower} —
 * water → heavy water at 50:1, {@code maxPower/20} HE per convert tick.
 */
public class DeuteriumExtractorBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, ITickableBE, MenuProvider {

    public final FluidTankNTM water;
    public final FluidTankNTM heavyWater;
    public final long maxPower;
    public long power;
    private final boolean tower;

    public static DeuteriumExtractorBlockEntity cube(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        return new DeuteriumExtractorBlockEntity(type, pos, state, 1_000, 100, 10_000L, false);
    }

    public static DeuteriumExtractorBlockEntity tower(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        return new DeuteriumExtractorBlockEntity(type, pos, state, 50_000, 5_000, 100_000L, true);
    }

    public DeuteriumExtractorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                         int waterCap, int hwCap, long maxPower, boolean tower) {
        super(type, pos, state, 0, true, true);
        this.water = new FluidTankNTM(Fluids.WATER, waterCap).withOwner(this);
        this.heavyWater = new FluidTankNTM(Fluids.HEAVYWATER, hwCap).withOwner(this);
        this.maxPower = maxPower;
        this.tower = tower;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machine_deuterium");
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        updateConnections();

        if (hasPower() && hasEnoughWater() && heavyWater.getMaxFill() > heavyWater.getFill()) {
            int convert = Math.min(heavyWater.getMaxFill(), water.getFill()) / 50;
            convert = Math.min(convert, heavyWater.getMaxFill() - heavyWater.getFill());
            water.setFill(water.getFill() - convert * 50);
            heavyWater.setFill(heavyWater.getFill() + convert);
            power -= getMaxPower() / 20;
        }

        for (DirPos pos : getConPos()) {
            trySubscribe(water.getTankType(), level, pos);
            if (heavyWater.getFill() > 0) tryProvide(heavyWater, level, pos);
        }

        dataChanged();
        networkPackMK2(50);
    }

    protected void updateConnections() {
        for (DirPos pos : getConPos()) trySubscribe(level, pos);
    }

    public boolean hasPower() {
        return power >= getMaxPower() / 20;
    }

    public boolean hasEnoughWater() {
        return water.getFill() >= 100;
    }

    public DirPos[] getConPos() {
        if (!tower) {
            DirPos[] all = new DirPos[6];
            int i = 0;
            for (Direction d : Direction.values()) {
                all[i++] = new DirPos(worldPosition.relative(d), d);
            }
            return all;
        }
        Direction dir = coreFacing();
        Direction rot = dir.getCounterClockWise();
        BlockPos p = worldPosition;
        return new DirPos[]{
                new DirPos(p.relative(dir, -2), dir.getOpposite()),
                new DirPos(p.relative(dir, -2).relative(rot), dir.getOpposite()),
                new DirPos(p.relative(dir), dir),
                new DirPos(p.relative(dir).relative(rot), dir),
                new DirPos(p.relative(rot.getOpposite()), rot.getOpposite()),
                new DirPos(p.relative(dir, -1).relative(rot.getOpposite()), rot.getOpposite()),
                new DirPos(p.relative(rot, 2), rot),
                new DirPos(p.relative(dir).relative(rot, 2), rot)
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
        return maxPower;
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(water);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(heavyWater);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(water, heavyWater);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        water.writeToNBT(tag, "water");
        heavyWater.writeToNBT(tag, "heavyWater");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        water.readFromNBT(tag, "water");
        heavyWater.readFromNBT(tag, "heavyWater");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        water.serialize(buf);
        heavyWater.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        water.deserialize(buf);
        heavyWater.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new DeuteriumMenu(id, inv, this);
    }
}
