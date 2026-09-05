package com.hbm.blockentity.machine.dummyable;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blockentity.machine.TurbineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.tileentity.IConfigurableMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.io.IOException;

/**
 * CE {@code TileEntityChungus}. TurbineBase, tanks 1e9/1e9, efficiency 0.85, consume 100%.
 * {@link IConfigurableMachine} Exact CE {@code TileEntityChungus.java:69-86}
 * ({@code steamturbineLeviathan} via {@link com.hbm.config.MachineDynConfig}).
 * TODO(CE: TileEntityChungus.java:115-163): client rotor/audio/CLOUD particles.
 * TODO(CE: TileEntityChungus.java:222-280): OpenComputers callbacks.
 * TODO(CE: RenderChungus.java:16): TESR.
 * ROR: CE {@code TileEntityChungus.java:284-293}.
 */
public class MachineChungusBlockEntity extends TurbineBaseBlockEntity implements IRORValueProvider, IConfigurableMachine {

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
    public String getConfigName() {
        return "steamturbineLeviathan";
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
        // CE TileEntityChungus.java:75-77
        inputTankSize = IConfigurableMachine.grab(obj, "I:inputTankSize", inputTankSize);
        outputTankSize = IConfigurableMachine.grab(obj, "I:outputTankSize", outputTankSize);
        efficiency = IConfigurableMachine.grab(obj, "D:efficiency", efficiency);
    }

    static void writeConfigStatic(JsonWriter writer) throws IOException {
        // CE TileEntityChungus.java:82-85 — CE typo "availible" kept
        writer.name("INFO").value("leviathan steam turbine consumes all availible steam per tick");
        writer.name("I:inputTankSize").value(inputTankSize);
        writer.name("I:outputTankSize").value(outputTankSize);
        writer.name("D:efficiency").value(efficiency);
    }

    /**
     * NeoForge BE has no no-arg ctor. {@link com.hbm.config.MachineDynConfig} Exact CE :44-48
     * instantiates this instead. Same schema as {@code TileEntityChungus.java:69-86}.
     */
    public static final class ConfigDummy implements IConfigurableMachine {
        @Override
        public String getConfigName() {
            return "steamturbineLeviathan";
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

    @Override
    public String[] getFunctionInfo() {
        // CE :284-287
        return new String[]{
                PREFIX_VALUE + "output"
        };
    }

    @Override
    public String provideRORValue(String name) {
        // CE :291-293
        if ((PREFIX_VALUE + "output").equals(name)) return "" + (int) this.powerBuffer;
        return null;
    }
}
