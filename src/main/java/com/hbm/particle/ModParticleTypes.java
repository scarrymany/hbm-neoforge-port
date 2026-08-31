package com.hbm.particle;

import com.hbm.main.MainRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * NeoForge {@link ParticleType} registry for the custom (non-vanilla) subset of CE's particle
 * catalog - the {@code f4-particle-registry-and-events} Phase 5 task, implementing
 * {@code docs/phase5/custom_particle_types_registry.md} (the registry-scoped report) and, for the
 * network/dispatch context each entry ultimately serves, {@code docs/phase5/
 * particle_engine_and_generic_vfx.md} (the sibling report covering the {@code AuxParticlePacketNT}/
 * {@code HbmEffectNT}-equivalent dispatch layer, a separate task's scope - not built here).
 * <p>
 * <b>Common-safe, not client-only</b>: {@link ParticleType}/{@link SimpleParticleType} live in
 * {@code net.minecraft.core.particles} (no client-only class referenced anywhere in this file), and
 * {@link BuiltInRegistries#PARTICLE_TYPE} is a plain built-in registry exactly like
 * {@code SoundEvent}/{@code MobEffect} (confirmed by cross-checking Neo Edition's own
 * {@code com.hbm.particle.NtmParticleTypes}, a real compiling class at the same
 * {@code neo_version=21.1.228} this port targets, plus this port's own already-real
 * {@code HBMSoundHandler}/{@code HbmPotionEffects} using the identical
 * {@code DeferredRegister.create(BuiltInRegistries.X, MainRegistry.MODID)} shape) - so, like those two
 * registries, this class is registered from {@link MainRegistry}'s constructor
 * ({@code modEventBus}), not from the client-only bootstrap. The client-only half (a
 * {@link net.minecraft.client.particle.ParticleProvider} per entry, registered via
 * {@code net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent}) lives in
 * {@code com.hbm.client.particle.ModParticleProviders} - see that class's javadoc for the
 * {@code @EventBusSubscriber(bus = Bus.MOD)} gotcha this split exists to respect (a common-code class
 * cannot reference client-only {@code Particle}/{@code ParticleProvider} types without crashing a
 * dedicated server's classloader).
 * <p>
 * <b>Why these 17 entries, not CE's full 87-value {@code HbmEffectNT} catalog</b>: CE 1.12.2 has no
 * {@code ParticleType}/registry concept at all (a Minecraft-1.13+ addition) - every one of its 87
 * particle classes is spawned by direct Java construction, dispatched by the hand-rolled
 * {@code HbmEffectNT} enum, not any Forge registry. {@code custom_particle_types_registry.md}'s own
 * "Prioritized list" section (grounded in this port's ~30 already-committed particle-system TODO
 * call sites, not CE's raw catalog size) is what this table follows entry-for-entry:
 * <ul>
 *   <li><b>Tier 0 (the {@code Jetpack}/{@code Jetpack_BJ}/{@code Jetpack_DNS}/{@code bnuuy} family,
 *   {@code HbmEffectNT.java:931-1119}) needs ZERO entries here</b> - confirmed by the report to use
 *   exclusively vanilla {@code ParticleTypes} (FLAME/SMOKE/BLOCK/DUST). Deliberately absent from this
 *   registry; do not add it.</li>
 *   <li><b>Tier 1</b> ({@code custom_particle_types_registry.md} "Tier 1" table): {@link #PLASMA_BLAST}
 *   (CE {@code ParticlePlasmaBlast}, {@code HbmEffectNT.java:449-454}), {@link #SPARK} (CE
 *   {@code ParticleSpark}, {@code HbmEffectNT.java:841-878} - the {@code Spark} constant's real
 *   backing class, confirmed by direct read; the report's own citation of "ParticleHbmSpark" for this
 *   slot was imprecise, see {@link #HADRON}'s entry below for the correction), {@link #HADRON} (CE
 *   {@code ParticleHadron} - and CE's own {@code ParticleHbmSpark}, folded into {@link #SPARK} rather
 *   than given its own entry, see below - both back the {@code Tau} beam effect,
 *   {@code HbmEffectNT.java:1435-1441}), {@link #EX_SMOKE} (CE {@code ParticleExSmoke}, backing
 *   {@code Smoke_Cloud}/{@code Smoke_Radial}/{@code Smoke_Shock}/{@code Smoke_ShockRand}/
 *   {@code Smoke_Wave}, {@code HbmEffectNT.java:165-291}), {@link #DIGAMMA_SMOKE} (CE
 *   {@code ParticleDigammaSmoke}, backing {@code Smoke_RadialDigamma}, {@code HbmEffectNT.java:205-218}).</li>
 *   <li><b>Tier 2</b> (the {@code Muke}/{@code TinyTot}/{@code BF} mushroom-cloud family,
 *   {@code custom_particle_types_registry.md} "Tier 2"): {@link #MUKE_WAVE} (CE
 *   {@code ParticleMukeWave}, {@code HbmEffectNT.java:392-408}), {@link #MUKE_FLASH} (CE
 *   {@code ParticleMukeFlash}, {@code HbmEffectNT.java:395}), {@link #MUKE_CLOUD} (CE
 *   {@code ParticleMukeCloud}, the toroidal cloudlet swarm, {@code HbmEffectNT.java:407-431}),
 *   {@link #MUKE_CLOUD_BF} (CE {@code ParticleMukeCloudBF}, backing {@code BF},
 *   {@code HbmEffectNT.java:1120-1123}).</li>
 *   <li><b>Tier 3</b> ({@code custom_particle_types_registry.md} "Tier 3"): {@link #COOLING_TOWER}
 *   (CE {@code ParticleCoolingTower}, backing {@code Tower}, {@code HbmEffectNT.java:911-930}),
 *   {@link #GAS_FLAME} (CE {@code ParticleGasFlame}, {@code HbmEffectNT.java:466-473}),
 *   {@link #RADIATION_FOG} (CE {@code ParticleRadiationFog}, backing {@code RadFog},
 *   {@code HbmEffectNT.java:141-148} - already has a documented vanilla-{@code CLOUD} stand-in
 *   shipping in {@code packet/toclient/RadFogPayload.java}; this entry is the real sprite-backed
 *   replacement that future pass is waiting on, not urgent to wire into that payload yet). Excluded
 *   from Tier 3 (per the report's own hedge): "ground-fire" ({@code FlameCreator}, not confirmed
 *   custom - likely resolves to vanilla FLAME/SMOKE like Tier 0) and "legacy bullet trail" (CE's own
 *   description strongly implies vanilla {@code FLAME}) - the report explicitly flags both as
 *   unconfirmed-custom, not load-bearing evidence for a new registry entry.</li>
 *   <li><b>Bullet-hit family</b> ({@code custom_particle_types_registry.md} "Not yet surfaced by any
 *   committed TODO" section - named by CE class, not yet ranked by this port's own TODO signal, but
 *   real and explicitly enumerated): {@link #BLOOD} (CE {@code bullet_hit.ParticleBloodParticle},
 *   gated in CE on {@code GeneralConfig.bloodFX}), {@link #BULLET_IMPACT} (CE
 *   {@code bullet_hit.ParticleBulletImpact}, the impact decal), {@link #HIT_DEBRIS} (CE
 *   {@code bullet_hit.ParticleHitDebris}, material-tinted debris), {@link #SMOKE_ANIM} (CE
 *   {@code bullet_hit.ParticleSmokeAnim}), all four backing {@code HbmEffectNT.BulletImpact}
 *   ({@code HbmEffectNT.java:1152-1298}); and {@link #GIBLETS} backing {@code HbmEffectNT.Giblets}
 *   ({@code HbmEffectNT.java:1442+}) - <b>correction to the sibling report's own attribution</b>: direct
 *   reading of {@code Giblets.setHandler} shows it constructs CE's top-level
 *   {@code com.hbm.particle.ParticleGiblet}, not {@code bullet_hit.ParticleMobGib}
 *   as the report states; {@code ParticleMobGib} is real but backs a different, unrelated system
 *   ({@code packet/toclient/PacketSpecialDeath.java:179}, a "special death" ragdoll-gib packet, not
 *   {@code AuxParticlePacketNT}/{@code HbmEffectNT} at all) that is out of scope for both this task and
 *   both of its source reports - flagged here rather than silently registering the wrong class.</li>
 * </ul>
 * <p>
 * <b>{@code overrideLimiter}</b> (constructor parameter on {@link SimpleParticleType}/
 * {@link ParticleType}, CE's rough equivalent of "ignore the client's particle-count/distance
 * culling"): set {@code true} only for {@link #SPARK}, matching the one entry the report could
 * cross-check against Neo Edition's own real, compiling registration
 * ({@code custom_particle_types_registry.md}: "SPARK is already registered in neo-edition's
 * NtmParticleTypes (overrideLimiter=true)... a safe shape to follow from neo-edition"). Every other
 * entry defaults {@code false} - no comparably-direct evidence exists for the rest, and the report
 * itself flags {@code overrideLimiter}'s exact semantics as "inferred from usage pattern, not
 * confirmed from NeoForge docs/source" (its "Key risks" #2), so this file does not speculate further.
 * <p>
 * <b>Two genuinely-parameterized entries</b>: {@link #PLASMA_BLAST} and {@link #GAS_FLAME} are the
 * only two entries in this table whose CE constructor is directly cited by
 * {@code custom_particle_types_registry.md}/{@code HbmEffectNT.java} as needing real per-spawn data
 * (color+orientation, and velocity+scale, respectively) - both use {@link HbmParticleOptions} (see
 * that class's own javadoc) rather than {@link SimpleParticleType}. Every other entry is a
 * {@link SimpleParticleType}; per-spawn variation for those (where CE's handler reads further NBT
 * keys, e.g. {@code Spark}'s {@code count}/{@code angle}/{@code color} fields) is expected to flow
 * through the sibling {@code particle_engine_and_generic_vfx} report's own {@code HbmEffectPacket}
 * dispatch layer instead (a client-side handler that directly constructs and adds a {@code Particle}
 * to {@code Minecraft.getInstance().particleEngine}, bypassing the vanilla {@code ParticleType}
 * network path entirely, exactly as CE's own {@code HbmEffectNT} handlers do) - not this file's job to
 * build.
 */
public final class ModParticleTypes {

    private ModParticleTypes() {
    }

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, MainRegistry.MODID);

    // --- Tier 1 ---
    public static final DeferredHolder<ParticleType<?>, ParticleType<HbmParticleOptions>> PLASMA_BLAST =
            registerTyped("plasma_blast", false);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPARK = register("spark", true);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HADRON = register("hadron", false);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> EX_SMOKE = register("ex_smoke", false);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DIGAMMA_SMOKE = register("digamma_smoke", false);

    // --- Tier 2 (mushroom-cloud family) ---
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MUKE_WAVE = register("muke_wave", false);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MUKE_FLASH = register("muke_flash", false);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MUKE_CLOUD = register("muke_cloud", false);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MUKE_CLOUD_BF = register("muke_cloud_bf", false);

    // --- Tier 3 ---
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> COOLING_TOWER = register("cooling_tower", false);
    public static final DeferredHolder<ParticleType<?>, ParticleType<HbmParticleOptions>> GAS_FLAME =
            registerTyped("gas_flame", false);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RADIATION_FOG = register("radiation_fog", false);

    // --- Bullet-hit family ---
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLOOD = register("blood", false);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BULLET_IMPACT = register("bullet_impact", false);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HIT_DEBRIS = register("hit_debris", false);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SMOKE_ANIM = register("smoke_anim", false);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GIBLETS = register("giblets", false);

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> register(String name, boolean overrideLimiter) {
        return PARTICLE_TYPES.register(name, () -> new SimpleParticleType(overrideLimiter));
    }

    private static DeferredHolder<ParticleType<?>, ParticleType<HbmParticleOptions>> registerTyped(String name, boolean overrideLimiter) {
        return PARTICLE_TYPES.register(name, () -> new ParticleType<HbmParticleOptions>(overrideLimiter) {
            @Override
            public MapCodec<HbmParticleOptions> codec() {
                return HbmParticleOptions.codec(this);
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, HbmParticleOptions> streamCodec() {
                return HbmParticleOptions.streamCodec(this);
            }
        });
    }

    public static void register(IEventBus modEventBus) {
        PARTICLE_TYPES.register(modEventBus);
    }
}
