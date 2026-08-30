package com.hbm.api.rbmk;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * The hook fired when {@link RBMKMeltdownTrigger#checkAndFire} detects that a column has crossed
 * its meltdown threshold.
 * <p>
 * <b>This package (the "rbmk_reactor" Phase 2 work package) deliberately implements ONLY the
 * trigger condition, not the meltdown itself.</b> CE's real meltdown event
 * ({@code TileEntityRBMKBase#meltdown()}, ~140 lines, plus {@code standardMelt}/{@code onMelt}
 * per column type) iteratively BFS-flood-fills every orthogonally-connected column from the
 * trigger point, converts each to debris or corium blocks based on its distance from the meltdown
 * footprint's edge, spawns {@code EntityRBMKDebris}/{@code EntitySpear} entities, optionally
 * vaporizes every fluid pipe/receiver transitively connected to a caught boiler, and fires
 * particle/sound/advancement side effects - reaching into {@code com.hbm.entity},
 * {@code com.hbm.particle}, and {@code com.hbm.handler.radiation.ChunkRadiationManager}
 * world-simulation systems that are Phase 4 scope per this package's own research report
 * (docs/phase2/rbmk_reactor.md, Package C). None of that belongs here.
 * <p>
 * Whichever package owns the actual world effect - the column-blocks package for the immediate
 * block-conversion piece, and/or a future Phase 4 radiation/fallout package for the rest - should
 * supply a real implementation of this interface to whatever code drives the reactor's tick loop.
 * Until one exists, passing {@code null} (or a no-op implementation) to
 * {@link RBMKMeltdownTrigger#checkAndFire} is completely safe: the trigger condition and the
 * "discard this tick's flux/heat output" contract still apply correctly, the reactor just silently
 * stops producing flux once melted instead of also converting into visible debris.
 */
@FunctionalInterface
public interface IRBMKMeltdownHandler {

    /**
     * @param level      the level the meltdown occurred in
     * @param originPos  the position of the column whose heat crossed {@link IRBMKColumn#maxHeat()}
     * @param origin     the column itself
     */
    void onMeltdownTriggered(ServerLevel level, BlockPos originPos, IRBMKColumn origin);
}
