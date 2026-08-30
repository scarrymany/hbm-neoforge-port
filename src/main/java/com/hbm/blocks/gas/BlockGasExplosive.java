package com.hbm.blocks.gas;

import com.hbm.config.GeneralConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Ported from CE's {@code BlockGasExplosive}: extends {@link BlockGasFlammable} but replaces the
 * inherited (tick-scheduled, one-cell-at-a-time) {@link #combust} with an immediate, same-call
 * breadth-first flood-fill over every connected explosive-gas cell, igniting and exploding each one
 * (capped at 128 cells per trigger); gated end-to-end on
 * {@link GeneralConfig#ENABLE_EXPLOSIVE_GAS}.
 * <p>
 * Unlike {@link BlockGasFlammable#combust}, this does not use {@code scheduleTick} propagation - CE's
 * original is a plain in-memory queue/visited-set walk over {@code getBlock() == this} neighbors, all
 * processed within the single call that triggered it, so no tick-timing translation is needed here.
 * The {@code isCombusting} {@link ThreadLocal} guard is CE's own re-entrancy guard (an explosion or
 * block change triggered mid-flood-fill can synchronously re-invoke {@code combust} on another
 * explosive-gas cell before the first call returns) and is preserved as-is.
 * <p>
 * {@link Level#explode} here uses the confirmed
 * {@code (Entity, double, double, double, float, boolean, ExplosionInteraction)} overload (the
 * trailing {@code boolean} is the "fire" flag) - matching real call sites elsewhere in the reference
 * ({@code HazardTypeExplosive}/{@code HazardTypeHydroactive}) - with {@code fire=true} to mirror CE's
 * own {@code world.newExplosion(null, x, y, z, 3F, true, false)} (CE's trailing {@code isFlaming=true}
 * argument).
 */
public class BlockGasExplosive extends BlockGasFlammable {

    private static final int MAX_PROCESSED = 128;
    private static final float EXPLOSION_POWER = 3.0F;

    private static final ThreadLocal<Boolean> isCombusting = ThreadLocal.withInitial(() -> false);

    public BlockGasExplosive(Properties properties) {
        super(properties);
    }

    @Override
    protected void combust(Level level, BlockPos startPos) {
        if (isCombusting.get() || !GeneralConfig.ENABLE_EXPLOSIVE_GAS.get()) return;

        isCombusting.set(true);
        try {
            Queue<BlockPos> processQueue = new ArrayDeque<>();
            Set<BlockPos> visited = new HashSet<>();

            if (level.getBlockState(startPos).getBlock() == this) {
                processQueue.offer(startPos);
                visited.add(startPos);
            }

            int processedCount = 0;
            while (!processQueue.isEmpty() && processedCount < MAX_PROCESSED) {
                BlockPos currentPos = processQueue.poll();
                processedCount++;

                level.setBlock(currentPos, Blocks.FIRE.defaultBlockState(), 3);
                level.explode(null, currentPos.getX() + 0.5, currentPos.getY() + 0.5, currentPos.getZ() + 0.5,
                        EXPLOSION_POWER, true, Level.ExplosionInteraction.TNT);

                for (Direction facing : Direction.values()) {
                    BlockPos neighborPos = currentPos.relative(facing);
                    if (!visited.contains(neighborPos) && level.getBlockState(neighborPos).getBlock() == this) {
                        visited.add(neighborPos);
                        processQueue.offer(neighborPos);
                    }
                }
            }
        } finally {
            isCombusting.set(false);
        }
    }
}
