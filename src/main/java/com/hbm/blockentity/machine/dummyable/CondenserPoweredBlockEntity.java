package com.hbm.blockentity.machine.dummyable;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.tileentity.IConfigurableMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;

/**
 * CE {@code TileEntityCondenserPowered} — 1M tanks, 10 HE/mB, 10M HE buffer.
 * {@link IConfigurableMachine} Exact CE {@code TileEntityCondenserPowered.java:51-68}
 * ({@code condenserPowered}). Spin / particles skipped.
 */
public class CondenserPoweredBlockEntity extends CondenserBlockEntity implements IEnergyReceiverMK2, IConfigurableMachine {

    public static long maxPower = 10_000_000L;
    public static int inputTankSizeP = 1_000_000;
    public static int outputTankSizeP = 1_000_000;
    public static int powerConsumption = 10;
    public long power;

    public CondenserPoweredBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, inputTankSizeP, outputTankSizeP, true);
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (level == null || level.isClientSide) return;
        if (level.getGameTime() % 20 == 0) {
            for (Direction d : Direction.values()) trySubscribe(level, worldPosition.relative(d), d);
        }
    }

    @Override
    protected boolean extraCondition(int convert) {
        return power >= (long) convert * powerConsumption;
    }

    @Override
    protected void postConvert(int convert) {
        power = Math.max(0, power - (long) convert * powerConsumption);
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
    public String getConfigName() {
        return "condenserPowered";
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
        // CE TileEntityCondenserPowered.java:56-59
        maxPower = IConfigurableMachine.grab(obj, "L:maxPower", maxPower);
        inputTankSizeP = IConfigurableMachine.grab(obj, "I:inputTankSize", inputTankSizeP);
        outputTankSizeP = IConfigurableMachine.grab(obj, "I:outputTankSize", outputTankSizeP);
        powerConsumption = IConfigurableMachine.grab(obj, "I:powerConsumption", powerConsumption);
    }

    static void writeConfigStatic(JsonWriter writer) throws IOException {
        // CE TileEntityCondenserPowered.java:64-67
        writer.name("L:maxPower").value(maxPower);
        writer.name("I:inputTankSize").value(inputTankSizeP);
        writer.name("I:outputTankSize").value(outputTankSizeP);
        writer.name("I:powerConsumption").value(powerConsumption);
    }

    /** NeoForge BE has no no-arg ctor. MachineDynConfig Exact CE :44-48. */
    public static final class ConfigDummy implements IConfigurableMachine {
        @Override
        public String getConfigName() {
            return "condenserPowered";
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
    }
}
