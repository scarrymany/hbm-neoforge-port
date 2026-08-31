package com.hbm.client.particle;

import com.hbm.main.MainRegistry;
import com.hbm.particle.ModParticleTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/**
 * Client-only {@link net.minecraft.client.particle.ParticleProvider} registration for every entry in
 * {@link ModParticleTypes} - the {@code f4-particle-registry-and-events} Phase 5 task's second half
 * (the first half, the {@link net.minecraft.core.particles.ParticleType} registry itself, is
 * {@link ModParticleTypes}, a common-safe class registered from {@link MainRegistry}'s constructor).
 * Implements {@code docs/phase5/custom_particle_types_registry.md}'s "Registration convention this
 * port should follow" section and the sibling {@code particle_engine_and_generic_vfx.md} report's
 * "Recommended architecture" point 3 (real NeoForge 1.21.1 registration API shape).
 * <p>
 * <b>{@code bus = Bus.MOD} is not optional here - this is the exact gotcha both source reports flag as
 * this task's single highest risk.</b> {@code RegisterParticleProvidersEvent} is a one-shot
 * client-setup "register your factories" event fired during mod loading (the same category as
 * {@code RegisterMenuScreensEvent}, confirmed {@code IModBusEvent} per {@link
 * com.hbm.main.ClientModRegistry}'s own load-bearing Phase 0 comment) - {@code @EventBusSubscriber}'s
 * {@code bus()} defaults to {@code Bus.GAME} and does NOT auto-detect {@code IModBusEvent}.
 * {@code custom_particle_types_registry.md}'s own "Risk" section found that Neo Edition's real,
 * compiling {@code NuclearTechModClient.java:128} annotates its equivalent handler
 * {@code @EventBusSubscriber(value = Dist.CLIENT)} with NO {@code bus=} override at all (confirmed: 13
 * of 13 {@code @EventBusSubscriber} usages repo-wide in Neo Edition never specify {@code bus=}) - if
 * the {@code IModBusEvent} reasoning holds, Neo Edition's entire particle-provider registration is
 * silently dead code, invisible without launching a real client. This class explicitly sets
 * {@code bus = EventBusSubscriber.Bus.MOD} to not repeat that mistake, following this port's own
 * already-correct {@code ClientModRegistry} precedent instead of copying Neo Edition's annotation
 * line verbatim.
 * <p>
 * <b>Phase 6 whole-tree sweep fix</b>: this class used to also carry a {@code
 * TextureAtlasStitchedEvent} sprite-capture handler. That same source report (lines 151-157)
 * separately confirmed {@code TextureAtlasStitchedEvent} is a real game-bus event, not {@code
 * IModBusEvent} - the exact opposite of {@code RegisterParticleProvidersEvent} - so co-locating it
 * on this {@code bus = Bus.MOD}-only class made it silently unreachable (the same "one class, one
 * bus" gotcha this whole file exists to avoid, just pointed the other way). Moved to its own
 * {@link ParticleAtlasHook}, a plain {@code Bus.GAME}-default class - see that class's javadoc for
 * the full account. No current behavior change: nothing reads that field yet either way.
 * <p>
 * <b>Why every entry uses {@code registerSpecial}, none use {@code registerSpriteSet}</b> (a
 * deliberate, documented scope/risk decision, not an oversight - flagged per this task's ground rules
 * as something this sandbox could not verify): a {@code registerSpriteSet}-registered type needs a
 * matching {@code assets/hbm/particles/<name>.json} (confirmed real requirement,
 * {@code custom_particle_types_registry.md} finding 4) which this port has zero of today (zero
 * particle textures exist anywhere under this port's resources - confirmed by that same report's
 * asset-catalog check), and this sandbox cannot run {@code ./gradlew} or launch a client to confirm
 * whether NeoForge's {@code ParticleEngine} degrades gracefully (most likely, matching vanilla's
 * well-established graceful-fallback-to-checkerboard behavior for a missing PNG) or errors loudly for
 * a {@code registerSpriteSet} type with no matching JSON at all (a *missing file*, not merely a
 * missing texture inside an existing one - a meaningfully different, unverified case). Every entry
 * therefore uses {@code registerSpecial} instead, whose {@link net.minecraft.client.particle.ParticleProvider}
 * is fully self-contained and has no atlas-JSON dependency at all - confirmed real and equally valid
 * for both {@link net.minecraft.core.particles.SimpleParticleType} and typed
 * {@code ParticleType<HbmParticleOptions>} entries alike by Neo Edition's own real
 * {@code registerSpecial(NtmParticleTypes.AMAT.get(), new AmatFlashParticle.Provider())} call (a typed
 * entry registered via {@code registerSpecial}, cross-checked for API shape only). Once real textures
 * land (a separate asset-porting pass), swapping any of these to {@code registerSpriteSet} is a small,
 * independent follow-up - not blocked by anything in this file.
 * <p>
 * <b>Update (Phase 5 Content wave, {@code c14-custom-particle-content})</b>: every entry below now
 * points at a real, CE-behavior-accurate {@code Particle} subclass under this same package (color,
 * alpha, scale, motion, and lifetime transcribed from CE's real source, cited in each class's own
 * javadoc) instead of the foundation wave's shared {@code StubHbmParticle} no-visual placeholder -
 * that class is no longer referenced from here (kept in place, unused, in case a future entry needs a
 * harmless placeholder again before its own real renderer lands).
 */
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ModParticleProviders {

    private ModParticleProviders() {
    }

    /**
     * The sprite-stitching hook {@code particle_engine_and_generic_vfx.md}/
     * {@code custom_particle_types_registry.md} both name (finding 3 / the "Confirmed real NeoForge
     * 1.21.1 registration API shape" section) now lives on {@link ParticleAtlasHook#particleAtlas}
     * (moved out in the Phase 6 whole-tree sweep fix - see this class's own javadoc and {@link
     * ParticleAtlasHook}'s for why). No entry in this file uses {@code registerSpriteSet} yet (see
     * class javadoc), so nothing currently reads that field - it is captured there so the Content
     * wave's {@code c14} task has a ready, already-wired atlas reference to call {@code
     * TextureAtlas#getSprite(ResourceLocation)} against once real particle textures/JSON land,
     * instead of needing to rediscover this event.
     */
    @SubscribeEvent
    public static void registerProviders(RegisterParticleProvidersEvent event) {
        // --- Tier 1 (HbmEffectNT.java:449-454, 841-878, 1435-1441, 165-291, 205-218) ---
        // Real per-type Particle subclasses below are the c14-custom-particle-content task's own work
        // (Phase 5 Content wave) - see each class's own javadoc for its exact CE source/line citation.
        event.registerSpecial(ModParticleTypes.PLASMA_BLAST.get(), new PlasmaBlastParticle.Provider());
        event.registerSpecial(ModParticleTypes.SPARK.get(), new SparkParticle.Provider());          // CE ParticleSpark
        event.registerSpecial(ModParticleTypes.HADRON.get(), new HadronParticle.Provider());         // CE ParticleHadron (+ folded-in ParticleHbmSpark)
        event.registerSpecial(ModParticleTypes.EX_SMOKE.get(), new ExSmokeParticle.Provider());       // CE ParticleExSmoke
        event.registerSpecial(ModParticleTypes.DIGAMMA_SMOKE.get(), new DigammaSmokeParticle.Provider());  // CE ParticleDigammaSmoke

        // --- Tier 2, mushroom-cloud family (HbmEffectNT.java:392-431, 1120-1123) ---
        event.registerSpecial(ModParticleTypes.MUKE_WAVE.get(), new MukeWaveParticle.Provider());      // CE ParticleMukeWave
        event.registerSpecial(ModParticleTypes.MUKE_FLASH.get(), new MukeFlashParticle.Provider());     // CE ParticleMukeFlash
        event.registerSpecial(ModParticleTypes.MUKE_CLOUD.get(), new MukeCloudParticle.Provider());     // CE ParticleMukeCloud
        event.registerSpecial(ModParticleTypes.MUKE_CLOUD_BF.get(), new MukeCloudBfParticle.Provider());  // CE ParticleMukeCloudBF

        // --- Tier 3 (HbmEffectNT.java:911-930, 466-473, 141-148) ---
        event.registerSpecial(ModParticleTypes.COOLING_TOWER.get(), new CoolingTowerParticle.Provider());  // CE ParticleCoolingTower
        event.registerSpecial(ModParticleTypes.GAS_FLAME.get(), new GasFlameParticle.Provider());
        event.registerSpecial(ModParticleTypes.RADIATION_FOG.get(), new RadiationFogParticle.Provider());  // CE ParticleRadiationFog

        // --- Bullet-hit family (HbmEffectNT.java:1152-1298 BulletImpact, 1442+ Giblets) ---
        event.registerSpecial(ModParticleTypes.BLOOD.get(), new BloodParticle.Provider());          // CE bullet_hit.ParticleBloodParticle
        event.registerSpecial(ModParticleTypes.BULLET_IMPACT.get(), new BulletImpactParticle.Provider());  // CE bullet_hit.ParticleBulletImpact
        event.registerSpecial(ModParticleTypes.HIT_DEBRIS.get(), new HitDebrisParticle.Provider());     // CE bullet_hit.ParticleHitDebris
        event.registerSpecial(ModParticleTypes.SMOKE_ANIM.get(), new SmokeAnimParticle.Provider());     // CE bullet_hit.ParticleSmokeAnim
        event.registerSpecial(ModParticleTypes.GIBLETS.get(), new GibletParticle.Provider());        // CE ParticleGiblet (see ModParticleTypes javadoc correction)
    }
}
