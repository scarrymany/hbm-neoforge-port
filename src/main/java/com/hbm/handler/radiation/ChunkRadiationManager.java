package com.hbm.handler.radiation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Ported from CE's {@code com.hbm.handler.radiation.ChunkRadiationManager} (68 lines, read in full) -
 * CE's own class javadoc: "We only have one radiation system, unlike upstream this proxy is made to
 * make porting easier. Always call {@code RadiationSystemNT} directly when possible." Kept as a thin
 * static-proxy facade exactly as CE does, not folded into {@link RadiationSystemNT}, because roughly
 * 20 already-committed Phase 0-3 call sites across this port assume
 * {@code ChunkRadiationManager.proxy.X(...)} as the entry point (see
 * {@code docs/phase4/chunk_radiation_system.md}'s Headline finding #3 for the full list).
 * <p>
 * Every overload takes {@code double} throughout, matching {@link RadiationSystemNT}'s own internal
 * type exactly (CE's real {@code ProxyClass} is likewise all-{@code double}, not {@code float}) -
 * existing call sites passing {@code float}/{@code int} literals widen implicitly with no code change
 * needed.
 */
public final class ChunkRadiationManager {

    public static final ProxyClass proxy = new ProxyClass();

    private ChunkRadiationManager() {
    }

    public static final class ProxyClass {

        ProxyClass() {
        }

        /** Read-only; returns {@code 0} on the client or for an unloaded chunk/section. */
        public double getRadiation(Level level, BlockPos pos) {
            if (!(level instanceof ServerLevel serverLevel) || level.isClientSide()) return 0D;
            return RadiationSystemNT.getRadForCoord(serverLevel, pos);
        }

        public void setRadiation(Level level, BlockPos pos, double rad) {
            if (!(level instanceof ServerLevel serverLevel) || level.isClientSide()) return;
            RadiationSystemNT.setRadForCoord(serverLevel, pos, rad);
        }

        /** Uncapped - CE marks this {@code @DoNotCall("unless you know what you are doing")}. */
        public void incrementRad(Level level, BlockPos pos, double rad) {
            if (!(level instanceof ServerLevel serverLevel) || level.isClientSide()) return;
            RadiationSystemNT.incrementRad(serverLevel, pos, rad);
        }

        /** Capped variant - the one every RBMK/hazard/fluid-trait call site actually uses. */
        public void incrementRad(Level level, BlockPos pos, double rad, double max) {
            if (!(level instanceof ServerLevel serverLevel) || level.isClientSide()) return;
            RadiationSystemNT.incrementRad(serverLevel, pos, rad, max);
        }

        public void decrementRad(Level level, BlockPos pos, double rad) {
            if (!(level instanceof ServerLevel serverLevel) || level.isClientSide()) return;
            RadiationSystemNT.decrementRad(serverLevel, pos, rad);
        }

        public void clearSystem(Level level) {
            if (!(level instanceof ServerLevel serverLevel) || level.isClientSide()) return;
            RadiationSystemNT.jettisonData(serverLevel);
        }
    }
}
