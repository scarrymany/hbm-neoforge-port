package com.hbm.blockentity.machine.oil;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.tileentity.IConfigurableMachine;
import com.hbm.world.feature.OilSpot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.util.List;

/**
 * Ported from CE's {@code TileEntityMachineFrackingTower} (246 lines, read in full). Adds a third
 * tank ({@code FRACKSOL}, pipe-only input — Exact CE has no third {@code unloadTank} pair),
 * drills through bedrock ({@link #getDrillDepth()} = 0),
 * can additionally suck {@code ore_bedrock_oil}, and - per CE - every successful suck calls
 * {@code OilSpot.generateOilSpot}, a Phase-2-safe block-mutation mechanic
 * (see {@link com.hbm.world.feature.OilSpot}, ported by this same pass per the task's "fracking's
 * block-manipulation mechanic is fully in-scope" instruction) - <b>not</b> the deferred world-gen
 * boundary itself (that boundary is only about how {@code ore_oil}/{@code ore_bedrock_oil} come to
 * exist in the world in the first place, see {@link OilDrillBaseBlockEntity}'s class javadoc).
 */
public class MachineFrackingTowerBlockEntity extends OilDrillBaseBlockEntity implements IConfigurableMachine {

    public static int maxPower = 5_000_000;
    public static int consumption = 5000;
    public static int solutionRequired = 10;
    public static int delay = 20;
    public static int oilPerDeposit = 1000;
    public static int gasPerDepositMin = 100;
    public static int gasPerDepositMax = 500;
    public static double drainChance = 0.02D;
    public static int oilPerBedrockDepsoit = 100;
    public static int gasPerBedrockDepositMin = 10;
    public static int gasPerBedrockDepositMax = 50;
    public static int destructionRange = 75;

    public MachineFrackingTowerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        tanks.add(new FluidTankNTM(Fluids.FRACKSOL, 64_000).withOwner(this));
    }

    public FluidTankNTM getFrackSolTank() {
        return tanks.get(2);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.frackingTower");
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
    public int getDrillDepth() {
        return 0;
    }

    @Override
    public boolean canPump() {
        boolean canPump = getFrackSolTank().getFill() >= solutionRequired;
        if (!canPump) this.indicator = 3;
        return canPump;
    }

    @Override
    public boolean canSuckBlock(Block b) {
        return super.canSuckBlock(b) || b == oreBedrockOil();
    }

    @Override
    public void doSuck(BlockPos pos) {
        super.doSuck(pos);
        if (level != null && level.getBlockState(pos).getBlock() == oreBedrockOil()) {
            onSuck(pos);
        }
    }

    @Override
    public void onSuck(BlockPos pos) {
        if (level == null) return;
        Block b = level.getBlockState(pos).getBlock();

        int oil;
        int gas;

        if (b == oreOil()) {
            getOilTank().setTankType(Fluids.OIL);
            oil = oilPerDeposit;
            gas = gasPerDepositMin + level.getRandom().nextInt(gasPerDepositMax - gasPerDepositMin + 1);

            if (level.getRandom().nextDouble() < drainChance) {
                level.setBlock(pos, oreOilEmpty().defaultBlockState(), 3);
            }
        } else if (b == oreBedrockOil()) {
            oil = oilPerBedrockDepsoit;
            gas = gasPerBedrockDepositMin + level.getRandom().nextInt(gasPerBedrockDepositMax - gasPerBedrockDepositMin + 1);
        } else {
            return;
        }

        getOilTank().setFill(getOilTank().getFill() + oil);
        getGasTank().setFill(getGasTank().getFill() + gas);
        getFrackSolTank().setFill(getFrackSolTank().getFill() - solutionRequired);

        OilSpot.generateOilSpot(level, worldPosition.getX(), worldPosition.getZ(), destructionRange, 10, false);
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(getFrackSolTank());
    }

    @Override
    protected void trySubscribeFluids(DirPos dp) {
        trySubscribe(getFrackSolTank().getTankType(), level, dp);
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
        return "frackingtower";
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
        // CE TileEntityMachineFrackingTower.java:176-187
        maxPower = IConfigurableMachine.grab(obj, "I:powerCap", maxPower);
        consumption = IConfigurableMachine.grab(obj, "I:consumption", consumption);
        solutionRequired = IConfigurableMachine.grab(obj, "I:solutionRequired", solutionRequired);
        delay = IConfigurableMachine.grab(obj, "I:delay", delay);
        oilPerDeposit = IConfigurableMachine.grab(obj, "I:oilPerDeposit", oilPerDeposit);
        gasPerDepositMin = IConfigurableMachine.grab(obj, "I:gasPerDepositMin", gasPerDepositMin);
        gasPerDepositMax = IConfigurableMachine.grab(obj, "I:gasPerDepositMax", gasPerDepositMax);
        drainChance = IConfigurableMachine.grab(obj, "D:drainChance", drainChance);
        oilPerBedrockDepsoit = IConfigurableMachine.grab(obj, "I:oilPerBedrockDeposit", oilPerBedrockDepsoit);
        gasPerBedrockDepositMin = IConfigurableMachine.grab(obj, "I:gasPerBedrockDepositMin", gasPerBedrockDepositMin);
        gasPerBedrockDepositMax = IConfigurableMachine.grab(obj, "I:gasPerBedrockDepositMax", gasPerBedrockDepositMax);
        destructionRange = IConfigurableMachine.grab(obj, "I:destructionRange", destructionRange);
    }

    static void writeConfigStatic(JsonWriter writer) throws IOException {
        // CE TileEntityMachineFrackingTower.java:192-203
        writer.name("I:powerCap").value(maxPower);
        writer.name("I:consumption").value(consumption);
        writer.name("I:solutionRequired").value(solutionRequired);
        writer.name("I:delay").value(delay);
        writer.name("I:oilPerDeposit").value(oilPerDeposit);
        writer.name("I:gasPerDepositMin").value(gasPerDepositMin);
        writer.name("I:gasPerDepositMax").value(gasPerDepositMax);
        writer.name("D:drainChance").value(drainChance);
        writer.name("I:oilPerBedrockDeposit").value(oilPerBedrockDepsoit);
        writer.name("I:gasPerBedrockDepositMin").value(gasPerBedrockDepositMin);
        writer.name("I:gasPerBedrockDepositMax").value(gasPerBedrockDepositMax);
        writer.name("I:destructionRange").value(destructionRange);
    }

    public static final class ConfigDummy implements IConfigurableMachine {
        @Override
        public String getConfigName() {
            return "frackingtower";
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
