package com.hbm.blockentity.machine.oil;

import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.DirPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code TileEntityMachineOilWell} (201 lines, read in full) - CE's own internal
 * config name for this block is literally {@code "derrick"} (see
 * {@code docs/phase2/oil_production_chain.md}'s headline finding). Cheapest/simplest of the three
 * concrete extractors: four cardinal connector points, no rotation math, {@link #getDimensions()}
 * (on the paired {@link com.hbm.blocks.machine.MachineOilWellBlock}) is {@code {9,0,1,1,1,1}}.
 *
 * <p>Constants are CE's own defaults, hardcoded rather than JSON-configurable (CE's
 * {@code IConfigurableMachine}/{@code getConfigName}/{@code readIfPresent}/{@code writeConfig}
 * triple is not ported - no per-machine JSON config system exists in this port yet, a confirmed gap
 * separate from Phase 0's mod-wide TOML config; see {@code docs/phase2/oil_production_chain.md}
 * Deferred scope #2). Matches this port's own {@code MachineDieselBlockEntity}/
 * {@code MachineCombustionEngineBlockEntity} precedent of hardcoding CE's tuning constants as plain
 * {@code static final} fields.</p>
 */
public class MachineOilWellBlockEntity extends OilDrillBaseBlockEntity {

    private static final long MAX_POWER = 100_000L;
    private static final int POWER_REQ = 100;
    private static final int DELAY = 50;
    private static final int OIL_PER_DEPOSIT = 500;
    private static final int GAS_PER_DEPOSIT_MIN = 100;
    private static final int GAS_PER_DEPOSIT_MAX = 500;
    private static final double DRAIN_CHANCE = 0.05D;

    public MachineOilWellBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.oilWell");
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
    public void onSuck(BlockPos pos) {
        if (level == null) return;

        level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                SoundEvents.GENERIC_SWIM, SoundSource.BLOCKS, 2.0F, 0.5F);

        if (level.getBlockState(pos).getBlock() != oreOil()) return;

        getOilTank().setTankType(Fluids.OIL);
        getGasTank().setTankType(Fluids.GAS);

        getOilTank().setFill(getOilTank().getFill() + OIL_PER_DEPOSIT);
        getGasTank().setFill(getGasTank().getFill() + GAS_PER_DEPOSIT_MIN
                + level.getRandom().nextInt(GAS_PER_DEPOSIT_MAX - GAS_PER_DEPOSIT_MIN + 1));

        if (level.getRandom().nextDouble() < DRAIN_CHANCE) {
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
}
