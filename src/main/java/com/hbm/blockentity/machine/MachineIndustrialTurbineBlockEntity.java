package com.hbm.blockentity.machine;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.lib.DirPos;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.tileentity.IConfigurableMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;

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
 * {@link IConfigurableMachine} Exact CE {@code TileEntityMachineIndustrialTurbine.java:54-71}.
 * ROR: CE {@code TileEntityMachineIndustrialTurbine.java:251-262}.
 */
public class MachineIndustrialTurbineBlockEntity extends TurbineBaseBlockEntity implements IRORValueProvider, IConfigurableMachine {

    public static int inputTankSize = 750_000;
    public static int outputTankSize = 3_000_000;
    public static double efficiency = 1D;
    private static final double FLYWHEEL_MAX_ENERGY = 0.5e8;

    public double spin;
    private long maxPower;
    private long flywheelEnergy;

    public MachineIndustrialTurbineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 0);
        tanks = new FluidTankNTM[]{
                new FluidTankNTM(Fluids.STEAM, inputTankSize).withOwner(this),
                new FluidTankNTM(Fluids.SPENTSTEAM, outputTankSize).withOwner(this)
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

        // CE: TileEntityMachineIndustrialTurbine.onClientTick() - continuous AudioWrapper loop
        // (HBMSoundHandler.largeTurbineRunning, 20-tick keepAlive) while spinning, volume/pitch ramped
        // live by `spin` and range-gated to nearby players client-side. No looped-block-audio bridge
        // or per-client range gating ported yet (see ChemPlantBlockEntity's identical note);
        // substituted with a periodic server broadcast every 20 ticks, pitch approximating CE's
        // 0.5 + min(1, spin*2) * 0.5 ramp.
        if (this.spin > 0 && level != null && level.getGameTime() % 20 == 0) {
            float spinNum = (float) Math.min(1D, this.spin * 2D);
            level.playSound(null, worldPosition, HBMSoundHandler.largeTurbineRunning.get(), SoundSource.BLOCKS, 0.25F + spinNum * 0.75F, 0.5F + spinNum * 0.5F);
        }
    }

    @Override
    public double consumptionPercent() {
        return 0.2D;
    }

    @Override
    public double getEfficiency() {
        return efficiency;
    }

    @Override
    public String getConfigName() {
        return "steamturbineIndustrialMk2";
    }

    @Override
    public void readIfPresent(JsonObject obj) {
        readConfig(obj);
    }

    @Override
    public void writeConfig(JsonWriter writer) throws IOException {
        writeConfigStatic(writer);
    }

    static void readConfig(JsonObject obj) {
        // CE TileEntityMachineIndustrialTurbine.java:60-62
        inputTankSize = IConfigurableMachine.grab(obj, "I:inputTankSize", inputTankSize);
        outputTankSize = IConfigurableMachine.grab(obj, "I:outputTankSize", outputTankSize);
        efficiency = IConfigurableMachine.grab(obj, "D:efficiency", efficiency);
    }

    static void writeConfigStatic(JsonWriter writer) throws IOException {
        // CE TileEntityMachineIndustrialTurbine.java:67-70 — CE typo "availible" kept
        writer.name("INFO").value("industrial steam turbine consumes 20% of availible steam per tick");
        writer.name("I:inputTankSize").value(inputTankSize);
        writer.name("I:outputTankSize").value(outputTankSize);
        writer.name("D:efficiency").value(efficiency);
    }

    /** NeoForge BE has no no-arg ctor. MachineDynConfig Exact CE :44-48. */
    public static final class ConfigDummy implements IConfigurableMachine {
        @Override
        public String getConfigName() {
            return "steamturbineIndustrialMk2";
        }

        @Override
        public void readIfPresent(JsonObject obj) {
            readConfig(obj);
        }

        @Override
        public void writeConfig(JsonWriter writer) throws IOException {
            writeConfigStatic(writer);
        }
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

    @Override
    public String[] getFunctionInfo() {
        // CE :251-255
        return new String[]{
                PREFIX_VALUE + "output",
                PREFIX_VALUE + "flywheel"
        };
    }

    @Override
    public String provideRORValue(String name) {
        // CE :259-262
        if ((PREFIX_VALUE + "output").equals(name)) return "" + (int) this.powerBuffer;
        if ((PREFIX_VALUE + "flywheel").equals(name)) return "" + (int) (spin * 100);
        return null;
    }
}
