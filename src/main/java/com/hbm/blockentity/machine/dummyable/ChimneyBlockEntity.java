package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.fluidmk2.IFluidReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.particle.HbmEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * CE {@code TileEntityChimneyBase}/{@code Brick}/{@code Industrial}.
 * Ashpit fly-ash feed TODO(CE: TileEntityChimneyBase.java:46-54) — ashpit BE has no ashLevel fields.
 */
public class ChimneyBlockEntity extends MachineBaseBlockEntity
        implements IFluidReceiverMK2, ITickableBE {

    private final double pollutionMod;
    private final boolean captureSoot;
    private final int particleY;
    public int onTicks;

    public static ChimneyBlockEntity brick(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        return new ChimneyBlockEntity(type, pos, state, 0.25D, false, 12);
    }

    public static ChimneyBlockEntity industrial(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        return new ChimneyBlockEntity(type, pos, state, 0.1D, true, 22);
    }

    public ChimneyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                              double pollutionMod, boolean captureSoot, int particleY) {
        super(type, pos, state, 0, false, false);
        this.pollutionMod = pollutionMod;
        this.captureSoot = captureSoot;
        this.particleY = particleY;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(captureSoot ? "block.hbm.chimney_industrial" : "block.hbm.chimney_brick");
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (level.getGameTime() % 20 == 0) {
            FluidType[] types = {Fluids.SMOKE, Fluids.SMOKE_LEADED, Fluids.SMOKE_POISON};
            for (FluidType type : types) {
                for (DirPos pos : getConPos()) trySubscribe(type, level, pos);
            }
        }

        if (onTicks > 0) {
            onTicks--;
            if (level.getGameTime() % 2 == 0) {
                HbmEffect.sendPacket(level, HbmEffect.TOWER,
                        worldPosition.getX() + 0.5, worldPosition.getY() + particleY, worldPosition.getZ() + 0.5,
                        150, null);
            }
        }

        dataChanged();
        networkPackMK2(50);
    }

    public DirPos[] getConPos() {
        BlockPos p = worldPosition;
        return new DirPos[]{
                new DirPos(p.relative(Direction.EAST, 2), Direction.EAST),
                new DirPos(p.relative(Direction.WEST, 2), Direction.WEST),
                new DirPos(p.relative(Direction.SOUTH, 2), Direction.SOUTH),
                new DirPos(p.relative(Direction.NORTH, 2), Direction.NORTH)
        };
    }

    @Override
    public boolean canConnect(FluidType type, Direction dir) {
        if (dir == null || dir.getAxis() == Direction.Axis.Y) return false;
        return type == Fluids.SMOKE || type == Fluids.SMOKE_LEADED || type == Fluids.SMOKE_POISON;
    }

    @Override
    public long transferFluid(FluidType type, int pressure, long fluid) {
        if (type != Fluids.SMOKE && type != Fluids.SMOKE_LEADED && type != Fluids.SMOKE_POISON) return fluid;
        onTicks = 20;
        long polluted = (long) (fluid * pollutionMod);
        if (type == Fluids.SMOKE) {
            PollutionHandler.incrementPollution(level, worldPosition, PollutionHandler.PollutionType.SOOT, polluted / 100F);
        } else if (type == Fluids.SMOKE_LEADED) {
            PollutionHandler.incrementPollution(level, worldPosition, PollutionHandler.PollutionType.HEAVYMETAL, polluted / 100F);
        } else {
            PollutionHandler.incrementPollution(level, worldPosition, PollutionHandler.PollutionType.POISON, polluted / 100F);
        }
        return 0;
    }

    @Override
    public long getDemand(FluidType type, int pressure) {
        return 1_000_000;
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(onTicks);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        onTicks = buf.readInt();
    }
}
