package com.hbm.explosion;

import com.hbm.config.BombConfig;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * Ported from CE's {@code com.hbm.explosion.ExplosionNukeSmall} (97 lines, read in full,
 * {@code @Deprecated} in CE itself) - a "mini-nuke" parameter-object builder used by several named
 * {@code MukeParams} presets (grenades/warheads at small-to-medium blast radii).
 * <p>
 * <b>Not ported (documented forward references)</b>: CE's networked particle burst ({@code
 * AuxParticlePacketNT}/{@code HbmEffectNT}, Phase 5) and the {@code params.miniNuke && !params.safe}
 * branch's {@code ExplosionNT} mini-explosion (that older, non-vanillant explosion class was out of
 * this pass's read set per {@code docs/phase3/explosion_engine.md} - a real, separately-scoped
 * forward reference, not guessed at). {@link #dealDamage}'s AoE (via {@link
 * ExplosionNukeGeneric#dealDamage}), the non-mini-nuke path's real {@link EntityNukeExplosionMK5}
 * spawn, and the mini-nuke chunk-radiation increment ({@code ChunkRadiationManager}, Phase 4) are all
 * fully ported and functional - the two presets actually flagged {@code miniNuke = false}
 * ({@link #PARAMS_HIGH}) already skip both stubbed/radiation branches entirely.
 */
public final class ExplosionNukeSmall {

    private ExplosionNukeSmall() {
    }

    public static void explode(Level level, double posX, double posY, double posZ, MukeParams params) {
        // TODO(AuxParticlePacketNT/HbmEffectNT, Phase 5): CE broadcasts a networked particle burst here.

        level.playSound(null, BlockPos.containing(posX, posY, posZ), HBMSoundHandler.mukeExplosion.get(), SoundSource.BLOCKS, 15.0F, 1.0F);

        if (params.shrapnelCount > 0) {
            ExplosionLarge.spawnShrapnels(level, posX, posY, posZ, params.shrapnelCount);
        }

        if (params.miniNuke && !params.safe) {
            // TODO(ExplosionNT): CE builds a small ExplosionNT here (blastRadius, resolution,
            // explosionAttribs) instead of a full mk5 ray explosion; that older explosion class is
            // a documented forward reference (see class javadoc) - skipped for now.
        }

        if (params.killRadius > 0) {
            ExplosionNukeGeneric.dealDamage(level, posX, posY, posZ, params.killRadius);
        }

        if (!params.miniNuke) {
            level.addFreshEntity(EntityNukeExplosionMK5.statFac(level, (int) params.blastRadius, posX, posY, posZ).forceSpawn());
        }

        if (params.miniNuke) {
            // CE: 5x5 chunk-cross ambient-radiation bump around the epicenter, falling off with
            // Manhattan distance, scaled by params.radiationLevel/3.
            float radMod = params.radiationLevel / 3F;
            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
            for (int i = -2; i <= 2; i++) {
                for (int j = -2; j <= 2; j++) {
                    if (Math.abs(i) + Math.abs(j) < 4) {
                        mutablePos.set((int) Math.floor(posX + i * 16), (int) Math.floor(posY), (int) Math.floor(posZ + j * 16));
                        ChunkRadiationManager.proxy.incrementRad(level, mutablePos,
                                (50F / (Math.abs(i) + Math.abs(j) + 1)) * radMod);
                    }
                }
            }
        }
    }

    public static final MukeParams PARAMS_SAFE = new MukeParams() {{
        safe = true;
        killRadius = 45F;
        radiationLevel = 2F;
    }};
    public static final MukeParams PARAMS_TOTS = new MukeParams() {{
        blastRadius = 10F;
        killRadius = 30F;
        shrapnelCount = 0;
        radiationLevel = 1;
    }};
    public static final MukeParams PARAMS_LOW = new MukeParams() {{
        blastRadius = 15F;
        killRadius = 45F;
        radiationLevel = 2;
    }};
    public static final MukeParams PARAMS_MEDIUM = new MukeParams() {{
        blastRadius = 20F;
        killRadius = 55F;
        radiationLevel = 3;
    }};
    public static final MukeParams PARAMS_HIGH = new MukeParams() {{
        miniNuke = false;
        blastRadius = BombConfig.FATMAN_RADIUS.get();
        shrapnelCount = 0;
    }};

    /** More sensible approach with more customization options (CE's own comment). */
    public static class MukeParams {
        public boolean miniNuke = true;
        public boolean safe = false;
        public float blastRadius;
        public float killRadius;
        public float radiationLevel = 1F;
        public int shrapnelCount = 25;
        public int resolution = 64;
    }
}
