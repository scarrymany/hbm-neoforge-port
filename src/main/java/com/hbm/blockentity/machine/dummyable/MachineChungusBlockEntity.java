package com.hbm.blockentity.machine.dummyable;

import com.hbm.blockentity.machine.TurbineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * CE {@code TileEntityChungus}. TurbineBase, tanks 1e9/1e9, efficiency 0.85, consume 100%.
 * TODO(CE: TileEntityChungus.java:115-163): client rotor/audio/CLOUD particles.
 * TODO(CE: TileEntityChungus.java:222-280): OpenComputers callbacks.
 * TODO(CE: TileEntityChungus.java:69-86): IConfigurableMachine JSON.
 * TODO(CE: RenderChungus.java:16): TESR.
 */
public class MachineChungusBlockEntity extends TurbineBaseBlockEntity {

    public static int inputTankSize = 1_000_000_000;
    public static int outputTankSize = 1_000_000_000;
    public static double efficiency = 0.85D;

    public int turnTimer;

    public MachineChungusBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 0);
        tanks = new FluidTankNTM[]{
                new FluidTankNTM(Fluids.STEAM, inputTankSize).withOwner(this),
                new FluidTankNTM(Fluids.SPENTSTEAM, outputTankSize).withOwner(this)
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineChungus");
    }

    @Override
    public double consumptionPercent() {
        return 1D;
    }

    @Override
    public double getEfficiency() {
        return efficiency;
    }

    @Override
    public boolean canConnect(Direction dir) {
        return dir != Direction.UP && dir != Direction.DOWN && dir != null;
    }

    private Direction coreDirection() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockDummyable.META)
                ? Direction.from3DDataValue(state.getValue(BlockDummyable.META) - BlockDummyable.offset)
                : Direction.NORTH;
    }

    @Override
    public DirPos[] getConPos() {
        Direction dir = coreDirection();
        Direction rot = dir.getCounterClockWise();
        BlockPos p = worldPosition;
        return new DirPos[]{
                new DirPos(p.getX() + dir.getStepX() * 5, p.getY() + 2, p.getZ() + dir.getStepZ() * 5, dir),
                new DirPos(p.getX() + rot.getStepX() * 3, p.getY(), p.getZ() + rot.getStepZ() * 3, rot),
                new DirPos(p.getX() - rot.getStepX() * 3, p.getY(), p.getZ() - rot.getStepZ() * 3, rot.getOpposite())
        };
    }

    @Override
    public DirPos[] getPowerPos() {
        Direction dir = coreDirection();
        return new DirPos[]{
                new DirPos(worldPosition.getX() - dir.getStepX() * 11, worldPosition.getY(),
                        worldPosition.getZ() - dir.getStepZ() * 11, dir.getOpposite())
        };
    }

    @Override
    protected void onServerTick() {
        turnTimer--;
        if (operational) turnTimer = 25;
    }

    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 6, worldPosition.getY(), worldPosition.getZ() - 6,
                worldPosition.getX() + 7, worldPosition.getY() + 9, worldPosition.getZ() + 7);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(this.turnTimer);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.turnTimer = buf.readInt();
    }
}
