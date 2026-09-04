package com.hbm.blockentity.machine.oil;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.DirPos;
import com.hbm.tileentity.IConfigurableMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;

/**
 * Ported from CE's {@code TileEntityMachinePumpjack} (239 lines, read in full). Adds
 * {@link Direction}-dependent connector points (facing-aware, unlike the derrick's fixed cardinal
 * set) and a purely cosmetic client-side rotating-rod animation ({@link #rot}/{@link #prevRot}/
 * {@link #speed}, synced via {@link #serialize}/{@link #deserialize} exactly like CE's own
 * {@code ByteBuf} payload - the speed value itself, not a raw angle, so each client free-runs its own
 * interpolation between packets).
 */
public class MachinePumpjackBlockEntity extends OilDrillBaseBlockEntity implements IConfigurableMachine {

    public static int maxPower = 250_000;
    public static int consumption = 200;
    public static int delay = 25;
    public static int oilPerDeposit = 750;
    public static int gasPerDepositMin = 50;
    public static int gasPerDepositMax = 250;
    public static double drainChance = 0.025D;

    /** Client-side-only cosmetic rotation state - never persisted, only ever set from {@link #deserialize}. */
    public float rot = 0;
    public float prevRot = 0;
    public float speed = 0;

    public MachinePumpjackBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.pumpjack");
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    public int getPowerReq() {
        return consumption;
    }

    @Override
    public int getDelay() {
        return delay;
    }

    @Override
    public void updateEntity() {
        super.updateEntity();

        if (level != null && level.isClientSide) {
            this.prevRot = rot;

            if (this.indicator == 0) {
                this.rot += speed;
            }

            if (this.rot >= 360) {
                this.prevRot -= 360;
                this.rot -= 360;
            }
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeFloat(this.indicator == 0 ? (5F + (2F * this.speedLevel)) + (this.overLevel - 1F) * 10 : 0F);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.speed = buf.readFloat();
    }

    @Override
    public void onSuck(BlockPos pos) {
        if (level == null) return;
        if (level.getBlockState(pos).getBlock() != oreOil()) return;

        getOilTank().setTankType(Fluids.OIL);
        getGasTank().setTankType(Fluids.GAS);

        getOilTank().setFill(getOilTank().getFill() + oilPerDeposit);
        getGasTank().setFill(getGasTank().getFill() + gasPerDepositMin
                + level.getRandom().nextInt(gasPerDepositMax - gasPerDepositMin + 1));

        if (level.getRandom().nextDouble() < drainChance) {
            level.setBlock(pos, oreOilEmpty().defaultBlockState(), 3);
        }
    }

    /**
     * Facing-dependent connector points, ported from CE's {@code getConPos} ({@code
     * ForgeDirection.getRotation(DOWN)} -&gt; {@link Direction#getClockWise(Direction.Axis)}, matching
     * this port's own {@code MachineCombustionEngineBlockEntity#getConPos} precedent for the identical
     * CE idiom).
     */
    @Override
    public DirPos[] getConPos() {
        BlockState state = getBlockState();
        Direction dir = state.hasProperty(BlockDummyable.META)
                ? Direction.from3DDataValue(state.getValue(BlockDummyable.META) - BlockDummyable.offset)
                : Direction.NORTH;
        Direction rot = dir.getClockWise(Direction.Axis.Y);

        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();

        return new DirPos[]{
                new DirPos(x + rot.getStepX() * 2 + dir.getStepX() * 2, y, z + rot.getStepZ() * 2 + dir.getStepZ() * 2, dir),
                new DirPos(x + rot.getStepX() * 2 + dir.getStepX() * 2, y, z + rot.getStepZ() * 4 - dir.getStepZ() * 2, dir.getOpposite()),
                new DirPos(x + rot.getStepX() * 4 - dir.getStepX() * 2, y, z + rot.getStepZ() * 4 + dir.getStepZ() * 2, dir),
                new DirPos(x + rot.getStepX() * 4 - dir.getStepX() * 2, y, z + rot.getStepZ() * 2 - dir.getStepZ() * 2, dir.getOpposite())
        };
    }

    @Override
    public String getConfigName() {
        return "pumpjack";
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
        // CE TileEntityMachinePumpjack.java:191-197
        maxPower = IConfigurableMachine.grab(obj, "I:powerCap", maxPower);
        consumption = IConfigurableMachine.grab(obj, "I:consumption", consumption);
        delay = IConfigurableMachine.grab(obj, "I:delay", delay);
        oilPerDeposit = IConfigurableMachine.grab(obj, "I:oilPerDeposit", oilPerDeposit);
        gasPerDepositMin = IConfigurableMachine.grab(obj, "I:gasPerDepositMin", gasPerDepositMin);
        gasPerDepositMax = IConfigurableMachine.grab(obj, "I:gasPerDepositMax", gasPerDepositMax);
        drainChance = IConfigurableMachine.grab(obj, "D:drainChance", drainChance);
    }

    static void writeConfigStatic(JsonWriter writer) throws IOException {
        // CE TileEntityMachinePumpjack.java:202-208
        writer.name("I:powerCap").value(maxPower);
        writer.name("I:consumption").value(consumption);
        writer.name("I:delay").value(delay);
        writer.name("I:oilPerDeposit").value(oilPerDeposit);
        writer.name("I:gasPerDepositMin").value(gasPerDepositMin);
        writer.name("I:gasPerDepositMax").value(gasPerDepositMax);
        writer.name("D:drainChance").value(drainChance);
    }

    public static final class ConfigDummy implements IConfigurableMachine {
        @Override
        public String getConfigName() {
            return "pumpjack";
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
}
