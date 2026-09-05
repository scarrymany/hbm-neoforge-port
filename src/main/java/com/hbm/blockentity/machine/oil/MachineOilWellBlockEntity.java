package com.hbm.blockentity.machine.oil;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.DirPos;
import com.hbm.tileentity.IConfigurableMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;

/**
 * Ported from CE's {@code TileEntityMachineOilWell} (201 lines, read in full) - CE's own internal
 * config name for this block is literally {@code "derrick"} (see
 * {@code docs/phase2/oil_production_chain.md}'s headline finding). Cheapest/simplest of the three
 * concrete extractors: four cardinal connector points, no rotation math, {@link #getDimensions()}
 * (on the paired {@link com.hbm.blocks.machine.MachineOilWellBlock}) is {@code {9,0,1,1,1,1}}.
 *
 * {@link IConfigurableMachine} Exact CE {@code TileEntityMachineOilWell.java:138-163} ({@code derrick}).
 */
public class MachineOilWellBlockEntity extends OilDrillBaseBlockEntity implements IConfigurableMachine {

    public static int maxPower = 100_000;
    public static int consumption = 100;
    public static int delay = 50;
    public static int oilPerDeposit = 500;
    public static int gasPerDepositMin = 100;
    public static int gasPerDepositMax = 500;
    public static double drainChance = 0.05D;

    public MachineOilWellBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.oilWell");
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
    public void onSuck(BlockPos pos) {
        if (level == null) return;

        level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                SoundEvents.GENERIC_SWIM, SoundSource.BLOCKS, 2.0F, 0.5F);

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

    @Override
    public DirPos[] getConPos() {
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 1, y, z, Direction.EAST),
                new DirPos(x - 1, y, z, Direction.WEST),
                new DirPos(x, y, z + 1, Direction.SOUTH),
                new DirPos(x, y, z - 1, Direction.NORTH)
        };
    }

    @Override
    public String getConfigName() {
        return "derrick";
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
        // CE TileEntityMachineOilWell.java:144-150
        maxPower = IConfigurableMachine.grab(obj, "I:powerCap", maxPower);
        consumption = IConfigurableMachine.grab(obj, "I:consumption", consumption);
        delay = IConfigurableMachine.grab(obj, "I:delay", delay);
        oilPerDeposit = IConfigurableMachine.grab(obj, "I:oilPerDeposit", oilPerDeposit);
        gasPerDepositMin = IConfigurableMachine.grab(obj, "I:gasPerDepositMin", gasPerDepositMin);
        gasPerDepositMax = IConfigurableMachine.grab(obj, "I:gasPerDepositMax", gasPerDepositMax);
        drainChance = IConfigurableMachine.grab(obj, "D:drainChance", drainChance);
    }

    static void writeConfigStatic(JsonWriter writer) throws IOException {
        // CE TileEntityMachineOilWell.java:156-162
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
            return "derrick";
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
