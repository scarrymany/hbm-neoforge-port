package com.hbm.particle;

import com.hbm.main.MainRegistry;
import com.hbm.packet.toclient.HbmEffectPacket;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Modern replacement for CE's {@code com.hbm.particle.helper.HbmEffectNT} (1503 lines) - a named,
 * network-broadcastable client VFX enum, each constant carrying its own client-only render handler.
 * Ported per {@code docs/phase5/particle_engine_and_generic_vfx.md}'s "Recommended architecture"
 * point 2: same enum-plus-per-value-handler <em>shape</em> as CE's original, deliberately not its
 * legacy string-keyed {@code EffectNTLegacyAdapter} companion (that report's Finding 4: this port has
 * no CE 1.12.2 save data to stay backward-compatible with, so the remapping half of that legacy path
 * has no value to port).
 * <p>
 * <b>Why 18 constants, not CE's full 87</b>: only the values with a real, already-committed caller
 * anywhere in this port today (this report's own "~30 call sites" survey) are built - see that
 * report's Key risk #3 for why building the full CE catalog speculatively was explicitly not
 * recommended. Add more constants on demand as future content wave passes need them.
 * <p>
 * <b>Dispatch entry points</b> - both go through this class, matching CE's own
 * {@code MainRegistry.proxy.effectNT(...)} "run this named VFX now" contract
 * ({@code upstream/hbm-ce/.../main/ClientProxy.java:392-394}):
 * <ul>
 *     <li>Over the network: a server sends {@link HbmEffectPacket} via {@link #sendPacket} (the
 *     common case - every current call site in this port), and that payload's
 *     {@code @OnlyIn(Dist.CLIENT) handleClient} calls {@link #summonParticle} once the packet
 *     arrives.</li>
 *     <li>Already-client-side: code that only ever runs on the client physical side (e.g.
 *     {@code EntityMist}'s {@code else} branch of its own {@code level().isClientSide()} check) can
 *     call {@link #summonParticle} directly, with no network round-trip - the same shape CE's own
 *     {@code BulletImpact} handler uses for its same-tick {@code Spark} re-dispatch
 *     ({@code HbmEffectNT.java:1152-1298}, confirmed by this report's own "CE's real
 *     AuxParticlePacketNT/HbmEffectNT system" section).</li>
 * </ul>
 * <p>
 * <b>Render substrate per constant</b> - two modern mechanisms, matching this report's "Recommended
 * architecture" point 3: (a) a real registered {@link com.hbm.particle.ModParticleTypes} entry (the
 * {@code f4-particle-registry-and-events} sibling task's already-committed registry + client
 * providers under {@code com.hbm.client.particle}), dispatched via the standard vanilla
 * {@code level.addParticle(ParticleOptions, ...)} entry point -
 * bypassing the vanilla network path entirely (no {@link net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket}
 * involved; this class's own {@link HbmEffectPacket} is the transport instead), exactly the bridge
 * pattern {@link ModParticleTypes}'s own class javadoc names as this class's job to build; or
 * (b) a plain vanilla {@link ParticleTypes} stand-in, for effects with no registered custom entry yet
 * - documented per-constant below, matching the precedent {@code packet/toclient/RadFogPayload.java}
 * already established (a vanilla-particle visual substitution, not a silently-invented one). No
 * constant here needs the {@link com.hbm.particle.engine.ParticleEngineNT} custom batch engine - every
 * effect currently wired is a modest, short-lived burst; that engine exists for the high-volume/
 * custom-blend effects future passes (the mushroom cloud, RBMK glow) will add.
 */
public enum HbmEffect {

    // --- Tier 1 (real ModParticleTypes-backed) ---------------------------------------------------

    /** CE {@code HbmEffectNT.PlasmaBlast} ({@code HbmEffectNT.java:449-454}). Data: r,g,b,pitch,yaw,scale (floats). */
    PLASMA_BLAST,
    /** CE {@code HbmEffectNT.Muke} ({@code HbmEffectNT.java:392-403}). Data: balefire (boolean, optional). */
    MUKE,
    /** CE {@code HbmEffectNT.TinyTot} ({@code HbmEffectNT.java:404-435}). No data. */
    TINY_TOT,
    /** CE {@code HbmEffectNT.BF} ({@code HbmEffectNT.java:1120-1123}), CE despawn() looped 150x - collapsed into one packet, see {@link #summonParticle} javadoc note below. Data: count (int). */
    BF,
    /** CE {@code HbmEffectNT.Tower} ({@code HbmEffectNT.java:911-930}), simplified (no lift/base/max/color NBT read yet). No data. */
    TOWER,

    // --- Tier 0 (vanilla-only, matches ModParticleTypes' own "needs ZERO custom entries" finding) --

    /** CE {@code HbmEffectNT.Vanilla} ({@code HbmEffectNT.java:1299-1306}), the wildcard escape hatch. Data: mode (string particle registry name, resolved bare like "flame" &rarr; minecraft:flame), mX/mY/mZ (doubles, optional motion), count (int, optional, default 1). */
    VANILLA,
    /** CE {@code HbmEffectNT.bnuuy} ({@code HbmEffectNT.java:976-1012}). Data: player (int, entity id). */
    BNUUY,
    /** CE {@code HbmEffectNT.Jetpack_BJ} ({@code HbmEffectNT.java:1013-1066}). Data: player (int). */
    JETPACK_BJ,
    /** CE {@code HbmEffectNT.Jetpack_DNS} ({@code HbmEffectNT.java:1067-1119}). Data: player (int). */
    JETPACK_DNS,
    /** CE {@code HbmEffectNT.Jetpack} ({@code HbmEffectNT.java:931-975}). Data: player (int), mode (int, optional, default 0). */
    JETPACK,
    /** CE {@code HbmEffectNT.VanillaExt_LargeExplode} ({@code HbmEffectNT.java:773-793}). Data: size (float), count (int). */
    VANILLA_EXT_LARGE_EXPLODE,
    /** CE {@code HbmEffectNT.VanillaBurst_BlockDust} ({@code HbmEffectNT.java:680-692}). Data: count (int), motion (double), block (string resource location). */
    VANILLA_BURST_BLOCK_DUST,
    /** CE {@code HbmEffectNT.AmatExplosion} ({@code HbmEffectNT.java:643-648}) - no registered {@link ModParticleTypes} entry exists for CE's real {@code ParticleAmatFlash} yet, vanilla stand-in used. Data: scale (float). */
    AMAT_EXPLOSION,
    /** CE {@code HbmEffectNT.Haze} ({@code HbmEffectNT.java:445-448}) - no registered {@link ModParticleTypes} entry exists for CE's real {@code ParticleHaze} fog-puff yet, vanilla stand-in used. No data. */
    HAZE,

    // --- Smoke family, backed by ModParticleTypes.EX_SMOKE (CE ParticleExSmoke) --------------------

    /** CE {@code HbmEffectNT.Smoke_Radial} ({@code HbmEffectNT.java:188-204}). Data: count (int). */
    SMOKE_RADIAL,
    /** CE {@code HbmEffectNT.Smoke_Cloud} ({@code HbmEffectNT.java:165-187}). Data: count (int). */
    SMOKE_CLOUD,
    /** CE {@code HbmEffectNT.Smoke_Shock} ({@code HbmEffectNT.java:219-240}). Data: count (int), strength (double). */
    SMOKE_SHOCK,
    /**
     * CE {@code HbmEffectNT.Smoke_FoamSplash} ({@code HbmEffectNT.java:292-312}) - <b>deliberately a
     * no-op handler, matching CE's own real shipped behavior</b>: CE's real handler body is itself
     * dead code, entirely commented out in the upstream source (only a {@code // TODO} block comment
     * remains where the real {@code ParticleFoam} spawn used to be) - CE ships this effect as a
     * network broadcast that visibly does nothing on a real CE client too. Registered anyway (rather
     * than omitted) so {@code ExplosionLarge.spawnFoam}'s call site can still send the packet,
     * preserving 1:1 which CE call sites broadcast a packet at all, without inventing a visual CE
     * itself never shipped.
     */
    SMOKE_FOAM_SPLASH;

    public static final HbmEffect[] VALUES = values();

    @FunctionalInterface
    public interface EffectHandler {
        void summonParticle(ClientLevel level, double x, double y, double z, CompoundTag data);
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    private EffectHandler handler;

    @OnlyIn(Dist.CLIENT)
    private void setHandler(EffectHandler handler) {
        this.handler = handler;
    }

    /**
     * Dispatch entry point - see class javadoc "Dispatch entry points". Only ever meaningfully called
     * client-side ({@link #handler} is never populated on a dedicated server, see
     * {@link #registerHandlers}); a missing handler is logged and ignored rather than throwing, so a
     * stale/mismatched client/server jar degrades gracefully instead of crashing the render thread.
     */
    @OnlyIn(Dist.CLIENT)
    public void summonParticle(ClientLevel level, double x, double y, double z, CompoundTag data) {
        if (this.handler == null) {
            MainRegistry.logger.warn("HbmEffect.{} has no registered client handler", this.name());
            return;
        }
        this.handler.summonParticle(level, x, y, z, data);
    }

    // ==================== server -> client dispatch helpers ====================
    // Mirrors CE's IParticleCreator.sendPacket / PacketThreading.createAllAroundThreadedPacket +
    // createSendToThreadedPacket pair (this report's Finding 5) - the two real
    // PacketDistributor shapes already in live use elsewhere in this port.

    /** Radius broadcast - the common case (CE: {@code PacketThreading.createAllAroundThreadedPacket}). */
    public static void sendPacket(ServerLevel level, HbmEffect effect, double x, double y, double z, double radius, @Nullable CompoundTag data) {
        PacketDistributor.sendToPlayersNear(level, null, x, y, z, radius,
                new HbmEffectPacket(effect, x, y, z, data == null ? new CompoundTag() : data));
    }

    /** {@link Level}-typed convenience overload - no-ops on a client {@link Level} instead of forcing every call site to instanceof-check. */
    public static void sendPacket(Level level, HbmEffect effect, double x, double y, double z, double radius, @Nullable CompoundTag data) {
        if (level instanceof ServerLevel serverLevel) {
            sendPacket(serverLevel, effect, x, y, z, radius, data);
        }
    }

    /** Single-player send (CE: {@code PacketThreading.createSendToThreadedPacket}) - e.g. an error marker only the interacting player should see. */
    public static void sendPacket(ServerPlayer player, HbmEffect effect, double x, double y, double z, @Nullable CompoundTag data) {
        PacketDistributor.sendToPlayer(player, new HbmEffectPacket(effect, x, y, z, data == null ? new CompoundTag() : data));
    }

    // ==================== client handler registration ====================

    /**
     * Registers every constant's client-only render handler. Called exactly once, client-side only,
     * from {@code ClientModRegistry.onClientSetup} - mirrors CE's own
     * {@code HbmEffectNT.registerClientHandlers()}, called once from CE's {@code ClientProxy.preInit}
     * ({@code HbmEffectNT.java:80-1471}).
     */
    @OnlyIn(Dist.CLIENT)
    public static void registerHandlers() {
        PLASMA_BLAST.setHandler((level, x, y, z, data) ->
                level.addParticle(
                        new HbmParticleOptions(ModParticleTypes.PLASMA_BLAST.get(), data), x, y, z, 0, 0, 0));

        MUKE.setHandler((level, x, y, z, data) -> {
            level.addParticle(ModParticleTypes.MUKE_WAVE.get(), x, y, z, 0, 0, 0);
            level.addParticle(ModParticleTypes.MUKE_FLASH.get(), x, y, z, 0, 0, 0);
            // CE also sets player.hurtTime/attackedAtYaw here for a full-screen hit-flash + head-jolt
            // (HbmEffectNT.java:400-402) - not reproduced: LivingEntity#hurtTime/attackedAtYaw are
            // protected vanilla fields with no public setter in 1.21.1 and this port has no access-
            // transformer infrastructure (same unavailable-accessor class of gap already documented by
            // EntityCreeperNuclear's own javadoc for an unrelated field). Purely a screen-shake/flash
            // cosmetic; the particle burst itself is real.
        });

        TINY_TOT.setHandler((level, x, y, z, data) -> {
            level.addParticle(ModParticleTypes.MUKE_WAVE.get(), x, y, z, 0, 0, 0);
            // CE's toroidal cloudlet swarm (HbmEffectNT.java:410-430) - column, top cap, dome; counts kept 1:1.
            for (double d = 0.0D; d <= 1.6D; d += 0.1D) {
                double mx = level.random.nextGaussian() * 0.05;
                double mz = level.random.nextGaussian() * 0.05;
                double my = d + level.random.nextGaussian() * 0.02;
                level.addParticle(ModParticleTypes.MUKE_CLOUD.get(), x, y, z, mx, my, mz);
            }
            for (int i = 0; i < 50; i++) {
                double mx = level.random.nextGaussian() * 0.5;
                double my = level.random.nextInt(5) == 0 ? 0.02 : 0;
                double mz = level.random.nextGaussian() * 0.5;
                level.addParticle(ModParticleTypes.MUKE_CLOUD.get(), x, y + 0.5, z, mx, my, mz);
            }
            for (int i = 0; i < 15; i++) {
                double ix = level.random.nextGaussian() * 0.2;
                double iz = level.random.nextGaussian() * 0.2;
                if (ix * ix + iz * iz > 0.75) {
                    ix *= 0.5;
                    iz *= 0.5;
                }
                double iy = 1.6 + (level.random.nextDouble() * 2 - 1) * (0.75 - (ix * ix + iz * iz)) * 0.5 + level.random.nextGaussian() * 0.02;
                level.addParticle(ModParticleTypes.MUKE_CLOUD.get(), x, y, z, ix, iy, iz);
            }
        });

        BF.setHandler((level, x, y, z, data) -> {
            // CE's real despawn() call site broadcasts 150 SEPARATE packets, one particle each, each
            // with its own randomized offset (EntityQuackos CE source, despawn()) - collapsed here into
            // one packet carrying count, with the client spawning the whole burst locally from the same
            // offset formula. Behaviorally identical visual result, ~150x less network traffic; see
            // this class's port-side caller (EntityQuackos#despawn) for the network-side half of this
            // documented deviation.
            int count = Math.max(1, data.getInt("count"));
            for (int i = 0; i < count; i++) {
                double ix = x + level.random.nextDouble() * 20 - 10;
                double iy = y + level.random.nextDouble() * 25;
                double iz = z + level.random.nextDouble() * 20 - 10;
                level.addParticle(ModParticleTypes.MUKE_CLOUD_BF.get(), ix, iy, iz, 0, 0, 0);
            }
        });

        TOWER.setHandler((level, x, y, z, data) ->
                level.addParticle(ModParticleTypes.COOLING_TOWER.get(), x, y, z, 0, 0, 0));

        VANILLA.setHandler((level, x, y, z, data) -> {
            ParticleOptions options = ParticleTypes.FLAME;
            ResourceLocation id = ResourceLocation.tryParse(data.getString("mode"));
            if (id != null) {
                ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(id);
                if (type instanceof SimpleParticleType simple) {
                    options = simple;
                }
            }
            double mx = data.getDouble("mX");
            double my = data.getDouble("mY");
            double mz = data.getDouble("mZ");
            int count = Math.max(1, data.getInt("count"));
            for (int i = 0; i < count; i++) {
                level.addParticle(options, x, y, z, mx, my, mz);
            }
        });

        BNUUY.setHandler((level, x, y, z, data) ->
                findPlayer(level, data).ifPresent(p -> {
                    double iy = p.getY() + p.getEyeHeight() - 1 + 0.4 + (p.isShiftKeyDown() ? 0.25 : 0);
                    for (int i = 0; i < 2; i++) {
                        double ox = (i == 0 ? -1 : 1) * 0.275;
                        level.addParticle(ParticleTypes.CLOUD, p.getX() + ox, iy, p.getZ(), 0, 0.01, 0);
                    }
                }));

        JETPACK_BJ.setHandler((level, x, y, z, data) ->
                findPlayer(level, data).ifPresent(p -> {
                    double iy = p.getY() + p.getEyeHeight() - 0.9375;
                    for (int i = 0; i < 2; i++) {
                        double ox = (i == 0 ? -1 : 1) * 0.125;
                        level.addParticle(new DustParticleOptions(new Vector3f(0.8F, 0.5F, 1.0F), 1.0F),
                                p.getX() + ox, iy, p.getZ(), 0, 0, 0);
                    }
                }));

        JETPACK_DNS.setHandler((level, x, y, z, data) ->
                findPlayer(level, data).ifPresent(p -> {
                    double iy = p.getY() - 0.5D;
                    for (int i = 0; i < 2; i++) {
                        double ox = (i == 0 ? -1 : 1) * 0.125;
                        level.addParticle(new DustParticleOptions(new Vector3f(0.01F, 1.0F, 1.0F), 1.0F),
                                p.getX() + ox, iy, p.getZ(), 0, 0, 0);
                    }
                }));

        JETPACK.setHandler((level, x, y, z, data) ->
                findPlayer(level, data).ifPresent(p -> {
                    double iy = p.getY() + p.getEyeHeight() - 1;
                    int mode = data.getInt("mode");
                    double mx = 0, my = 0, mz = 0;
                    if (mode == 0) {
                        my = -0.2;
                    } else if (mode == 1) {
                        mx = -p.getLookAngle().x * 0.1;
                        my = -p.getLookAngle().y * 0.1;
                        mz = -p.getLookAngle().z * 0.1;
                    }
                    for (int i = 0; i < 2; i++) {
                        double ox = (i == 0 ? -1 : 1) * 0.125;
                        level.addParticle(ParticleTypes.FLAME, p.getX() + ox, iy, p.getZ(), mx * 2, my * 2, mz * 2);
                        level.addParticle(ParticleTypes.LARGE_SMOKE, p.getX() + ox, iy, p.getZ(), mx * 3, my * 3, mz * 3);
                    }
                }));

        VANILLA_EXT_LARGE_EXPLODE.setHandler((level, x, y, z, data) -> {
            level.addParticle(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 0, 0, 0);
            int count = Math.max(0, data.getInt("count"));
            for (int i = 0; i < count; i++) {
                level.addParticle(ParticleTypes.POOF, x, y, z,
                        level.random.nextGaussian() * 0.05, level.random.nextGaussian() * 0.05, level.random.nextGaussian() * 0.05);
            }
        });

        VANILLA_BURST_BLOCK_DUST.setHandler((level, x, y, z, data) -> {
            ResourceLocation blockId = ResourceLocation.tryParse(data.getString("block"));
            Block block = blockId != null ? BuiltInRegistries.BLOCK.get(blockId) : Blocks.REDSTONE_BLOCK;
            BlockState state = block.defaultBlockState();
            int count = Math.max(0, data.getInt("count"));
            double motion = data.getDouble("motion");
            for (int i = 0; i < count; i++) {
                double mx = level.random.nextGaussian() * motion;
                double my = level.random.nextGaussian() * motion + 0.2;
                double mz = level.random.nextGaussian() * motion;
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), x, y, z, mx, my, mz);
            }
        });

        HAZE.setHandler((level, x, y, z, data) -> {
            for (int i = 0; i < 4; i++) {
                double ox = level.random.nextGaussian() * 0.4;
                double oz = level.random.nextGaussian() * 0.4;
                level.addParticle(ParticleTypes.CLOUD, x + ox, y + 0.2, z + oz, 0, 0.005, 0);
            }
        });

        AMAT_EXPLOSION.setHandler((level, x, y, z, data) -> {
            float scale = data.getFloat("scale");
            level.addParticle(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 0, 0, 0);
            int count = Math.max(4, (int) (scale * 2));
            for (int i = 0; i < count; i++) {
                level.addParticle(ParticleTypes.END_ROD, x, y, z,
                        level.random.nextGaussian() * 0.1, level.random.nextGaussian() * 0.1, level.random.nextGaussian() * 0.1);
            }
        });

        SMOKE_RADIAL.setHandler((level, x, y, z, data) -> {
            int count = Math.max(1, data.getInt("count"));
            for (int i = 0; i < count; i++) {
                double motion = 1 + count / 50D;
                spawnExSmoke(level, x, y, z, level.random.nextGaussian() * motion, level.random.nextGaussian() * motion, level.random.nextGaussian() * motion);
            }
        });

        SMOKE_CLOUD.setHandler((level, x, y, z, data) -> {
            int count = Math.max(1, data.getInt("count"));
            for (int i = 0; i < count; i++) {
                double my = level.random.nextGaussian() * (1 + count / 100D);
                double mx = level.random.nextGaussian() * (1 + count / 150D);
                double mz = level.random.nextGaussian() * (1 + count / 150D);
                if (level.random.nextBoolean()) my = Math.abs(my);
                spawnExSmoke(level, x, y, z, mx, my, mz);
            }
        });

        SMOKE_SHOCK.setHandler((level, x, y, z, data) -> {
            int count = Math.max(1, data.getInt("count"));
            double strength = data.getDouble("strength");
            double yawRad = Math.toRadians(level.random.nextInt(360));
            for (int i = 0; i < count; i++) {
                double mx = Math.cos(yawRad) * strength;
                double mz = Math.sin(yawRad) * strength;
                spawnExSmoke(level, x, y, z, mx, 0, mz);
            }
        });

        SMOKE_FOAM_SPLASH.setHandler((level, x, y, z, data) -> {
            // Deliberately a no-op - see this constant's own javadoc (matches CE's own dead handler).
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void spawnExSmoke(ClientLevel level, double x, double y, double z, double mx, double my, double mz) {
        level.addParticle(ModParticleTypes.EX_SMOKE.get(), x, y, z, mx, my, mz);
    }

    /** Resolves CE's own {@code data.getInteger("player")} entity-id-lookup convention (e.g. {@code HbmEffectNT.java:932,980,1017,1072}). */
    @OnlyIn(Dist.CLIENT)
    private static Optional<Player> findPlayer(ClientLevel level, CompoundTag data) {
        Entity entity = level.getEntity(data.getInt("player"));
        return entity instanceof Player player ? Optional.of(player) : Optional.empty();
    }
}
