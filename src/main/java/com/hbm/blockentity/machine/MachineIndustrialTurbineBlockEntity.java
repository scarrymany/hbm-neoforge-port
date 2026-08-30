package com.hbm.blockentity.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.lib.DirPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code TileEntityMachineIndustrialTurbine} (block
 * {@code MachineIndustrialTurbine}, regname {@code machine_industrial_turbine}, read in full): the
 * one turbine that actually uses {@link TurbineBaseBlockEntity}. No inventory, no GUI in CE either
 * (confirmed by source: it implements neither {@code IGUIProvider} nor holds an
 * {@code ItemStackHandler}) - a pure multiblock producer. Adds a flywheel spin-up model
 * ({@link #flywheelEnergy}/{@link #spin}) so output ramps rather than snapping to target:
 * {@link #generatePower} calculates the fluid type's theoretical max output into
 * {@link #maxPower} and banks the tick's actual energy into the flywheel; {@link #onServerTick}
 * drains the flywheel towards that target scaled by {@code spin} (dense steam types produce far
 * less energy per operation, so the flywheel of a turbine running e.g. ultra-hot steam spools up
 * much slower - CE's own comment). {@code consumptionPercent()}=0.2 (at most 20% of the input tank
 * per tick), {@code doesResizeCompressor()}=true.
 */
public class MachineIndustrialTurbineBlockEntity extends TurbineBaseBlockEntity {

    private static final double EFFICIENCY = 1D;
    private static final double FLYWHEEL_MAX_ENERGY = 0.5e8;

    public double spin;
    private long maxPower;
    private long flywheelEnergy;

    public MachineIndustrialTurbineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 0);
        tanks = new FluidTankNTM[]{
                new FluidTankNTM(Fluids.STEAM, 750_000).withOwner(this),
                new FluidTankNTM(Fluids.SPENTSTEAM, 3_000_000).withOwner(this)
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.industrialTurbine");
    }

    private Direction coreDirection() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockDummyable.META)
                ? Direction.from3DDataValue(state.getValue(BlockDummyable.META) - BlockDummyable.offset)
                : Direction.NORTH;
    }

    @Override
    protected void generatePower(long power, int steamConsumed) {
        FT_Coolable trait = tanks[0].getTankType().getTrait(FT_Coolable.class);
        double eff = trait.getEfficiency(FT_Coolable.CoolingType.TURBINE) * getEfficiency();
        int maxOps = (int) Math.ceil((tanks[0].getMaxFill() * consumptionPercent()) / trait.amountReq);
        this.maxPower = (long) (maxOps * trait.heatEnergy * eff);
        this.flywheelEnergy += power;
    }

    @Override
    protected void onServerTick() {
        this.spin = (double) flywheelEnergy / FLYWHEEL_MAX_ENERGY;
        long target = Math.min((long) (Math.max(this.spin, 0.05D) * maxPower), this.flywheelEnergy);
        this.flywheelEnergy -= target;
        this.powerBuffer = target;
    }

    @Override
    public double consumptionPercent() {
        return 0.2D;
    }

    @Override
    public double getEfficiency() {
        return EFFICIENCY;
    }

    @Override
    public boolean doesResizeCompressor() {
        return true;
    }

    @Override
    public DirPos[] getConPos() {
        Direction dir = coreDirection();
        Direction rot = dir.getClockWise(Direction.Axis.Y);
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();

        return new DirPos[]{
                new DirPos(x + dir.getStepX() * 3 + rot.getStepX() * 2, y, z + dir.getStepZ() * 3 + rot.getStepZ() * 2, rot),
                new DirPos(x + dir.getStepX() * 3 - rot.getStepX() * 2, y, z + dir.getStepZ() * 3 - rot.getStepZ() * 2, rot.getOpposite()),
                new DirPos(x - dir.getStepX() + rot.getStepX() * 2, y, z - dir.getStepZ() + rot.getStepZ() * 2, rot),
                new DirPos(x - dir.getStepX() - rot.getStepX() * 2, y, z - dir.getStepZ() - rot.getStepZ() * 2, rot.getOpposite()),
                new DirPos(x + dir.getStepX() * 3, y + 3, z + dir.getStepZ() * 3, Direction.UP),
                new DirPos(x - dir.getStepX(), y + 3, z - dir.getStepZ(), Direction.UP)
        };
    }

    @Override
    public DirPos[] getPowerPos() {
        Direction dir = coreDirection();
        return new DirPos[]{
                new DirPos(worldPosition.getX() - dir.getStepX() * 4, worldPosition.getY() + 1, worldPosition.getZ() - dir.getStepZ() * 4, dir.getOpposite())
        };
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("flywheel_energy", flywheelEnergy);
        tag.putLong("maxPower", maxPower);
        tag.putDouble("spin", spin);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        flywheelEnergy = tag.getLong("flywheel_energy");
        maxPower = tag.getLong("maxPower");
        spin = tag.getDouble("spin");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeDouble(spin);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        spin = buf.readDouble();
    }
}
