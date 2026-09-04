package com.hbm.blockentity.machine.oil;

import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.world.feature.OilSpot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

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
public class MachineFrackingTowerBlockEntity extends OilDrillBaseBlockEntity {

    private static final long MAX_POWER = 5_000_000L;
    private static final int POWER_REQ = 5000;
    private static final int SOLUTION_REQUIRED = 10;
    private static final int DELAY = 20;
    private static final int OIL_PER_DEPOSIT = 1000;
    private static final int GAS_PER_DEPOSIT_MIN = 100;
    private static final int GAS_PER_DEPOSIT_MAX = 500;
    private static final double DRAIN_CHANCE = 0.02D;
    private static final int OIL_PER_BEDROCK_DEPOSIT = 100;
    private static final int GAS_PER_BEDROCK_DEPOSIT_MIN = 10;
    private static final int GAS_PER_BEDROCK_DEPOSIT_MAX = 50;
    private static final int DESTRUCTION_RANGE = 75;

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
        return MAX_POWER;
    }

    @Override
    public int getPowerReq() {
        return POWER_REQ;
    }

    @Override
    public int getDelay() {
        return DELAY;
    }

    @Override
    public int getDrillDepth() {
        return 0;
    }

    @Override
    public boolean canPump() {
        boolean canPump = getFrackSolTank().getFill() >= SOLUTION_REQUIRED;
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
            oil = OIL_PER_DEPOSIT;
            gas = GAS_PER_DEPOSIT_MIN + level.getRandom().nextInt(GAS_PER_DEPOSIT_MAX - GAS_PER_DEPOSIT_MIN + 1);

            if (level.getRandom().nextDouble() < DRAIN_CHANCE) {
                level.setBlock(pos, oreOilEmpty().defaultBlockState(), 3);
            }
        } else if (b == oreBedrockOil()) {
            oil = OIL_PER_BEDROCK_DEPOSIT;
            gas = GAS_PER_BEDROCK_DEPOSIT_MIN + level.getRandom().nextInt(GAS_PER_BEDROCK_DEPOSIT_MAX - GAS_PER_BEDROCK_DEPOSIT_MIN + 1);
        } else {
            return;
        }

        getOilTank().setFill(getOilTank().getFill() + oil);
        getGasTank().setFill(getGasTank().getFill() + gas);
        getFrackSolTank().setFill(getFrackSolTank().getFill() - SOLUTION_REQUIRED);

        OilSpot.generateOilSpot(level, worldPosition.getX(), worldPosition.getZ(), DESTRUCTION_RANGE, 10, false);
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
}
