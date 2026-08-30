package com.hbm.blocks.gas;

import com.hbm.config.GeneralConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code BlockGasFlammable}: detects fire/lava/torch neighbors and ignites,
 * spreading that ignition outward through connected flammable-gas cells; gated end-to-end on
 * {@link GeneralConfig#ENABLE_FLAMMABLE_GAS}.
 * <p>
 * <b>Two tick paths sharing one body.</b> CE's fire-detection/decay/movement logic lives entirely in
 * {@code updateTick}, called both by the ambient random tick (movement/decay) and, indirectly, by
 * {@link #combust}'s {@code World.scheduleUpdate(neighborPos, block, 2)} calls (ignition
 * propagation - each newly-scheduled tick re-runs the same fire-neighbor check against the cell that
 * was just set alight). 1.21 splits ambient vs. scheduled ticks into {@link #randomTick} and
 * {@link #tick}; since CE runs identical logic on both paths, both overrides here simply forward to
 * the shared {@link #updateFlammableGas}.
 * <p>
 * <b>{@code neighborChanged}'s trailing parameter is {@code BlockPos fromPos}, not
 * {@code Orientation}</b> - confirmed against this toolchain by the Neo Edition reference
 * ({@code GasBaseBlock} and 14+ other override sites use the {@code BlockPos fromPos} form). Likewise
 * {@code isBurning()} -&gt; {@link Entity#isOnFire()} and {@code onEntityCollision} -&gt;
 * {@link #entityInside} are confirmed 1:1 API renames used the same way by
 * {@code com.hbm.blocks.generic.BlockClorine} elsewhere in this port.
 * <p>
 * <b>Fire-source list.</b> CE checks {@code Material.FIRE}, {@code Material.LAVA} and
 * {@code Blocks.TORCH} (which in 1.12 is a single block covering both standing and wall placement).
 * 1.21 splits standing/wall torches into two blocks, so {@link #isFireSource} checks both
 * {@link Blocks#TORCH} and {@link Blocks#WALL_TORCH} to preserve CE's actual coverage rather than
 * silently dropping wall-mounted torches. (The Neo Edition reference's own {@code GasFlammableBlock}
 * additionally treats {@code Blocks.JACK_O_LANTERN} as a fire source and flips {@link #isFlammable}
 * to {@code true} - both are that file's own redesign, not CE's behavior, and are deliberately not
 * copied here per this area's scope: API shape only, not content/balancing.)
 */
public class BlockGasFlammable extends BlockGasBase {

    public BlockGasFlammable(Properties properties) {
        super(properties, 0.8F, 0.8F, 0.2F);
    }

    @Override
    public Direction getFirstDirection(Level level, BlockPos pos) {
        RandomSource random = level.getRandom();
        if (random.nextInt(3) == 0) {
            return random.nextBoolean() ? Direction.DOWN : Direction.UP;
        }

        return randomHorizontal(random);
    }

    @Override
    public Direction getSecondDirection(Level level, BlockPos pos) {
        return randomHorizontal(level.getRandom());
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        updateFlammableGas(level, pos, random);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        updateFlammableGas(level, pos, random);
    }

    private void updateFlammableGas(ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.hasChunkAt(pos)) return;

        if (!GeneralConfig.ENABLE_FLAMMABLE_GAS.get()) {
            level.removeBlock(pos, false);
            return;
        }

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            if (!level.isLoaded(neighborPos)) continue;

            if (isFireSource(level.getBlockState(neighborPos))) {
                combust(level, pos);
                return;
            }
        }

        if (random.nextInt(20) == 0 && level.getBlockState(pos.below()).isAir()) {
            level.removeBlock(pos, false);
            return;
        }

        moveGas(level, pos, random);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        if (level.isClientSide()) return;
        if (!level.isLoaded(pos) || !level.isLoaded(fromPos)) return;

        if (isFireSource(level.getBlockState(fromPos))) {
            combust(level, pos);
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!GeneralConfig.ENABLE_FLAMMABLE_GAS.get()) return;

        if (!level.isClientSide() && entity.isOnFire()) {
            combust(level, pos);
        }
    }

    protected void combust(Level level, BlockPos pos) {
        level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            if (!level.isLoaded(neighborPos)) continue;

            BlockState neighborState = level.getBlockState(neighborPos);
            if (neighborState.getBlock() instanceof BlockGasFlammable) {
                level.scheduleTick(neighborPos, neighborState.getBlock(), 2);
            }
        }
    }

    public boolean isFireSource(BlockState state) {
        return state.is(Blocks.FIRE) || state.is(Blocks.LAVA) || state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH);
    }

    @Override
    public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return false;
    }

    @Override
    public int getDelay(LevelAccessor levelAccessor) {
        return levelAccessor.getRandom().nextInt(5) + 16;
    }
}
