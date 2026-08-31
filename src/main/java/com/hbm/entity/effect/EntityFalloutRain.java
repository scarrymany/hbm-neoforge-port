package com.hbm.entity.effect;

import com.hbm.blocks.generic.FalloutBlocks;
import com.hbm.config.BombConfig;
import com.hbm.config.FalloutConfigJSON;
import com.hbm.config.FalloutConfigJSON.FalloutEntry;
import com.hbm.config.WorldConfig;
import com.hbm.entity.logic.EntityExplosionChunkloading;
import com.hbm.entity.logic.NukeEntityTypes;
import com.hbm.world.biome.ModCraterBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Ported from CE's {@code com.hbm.entity.effect.EntityFalloutRain} (1065 lines, read in full) -
 * {@code docs/phase4/fallout_rain_and_effects.md} Package B, the "single-threaded MVP" the report
 * explicitly recommends: CE's real off-heap/{@code ForkJoinPool}/{@code Unsafe}/lock-free-queue
 * concurrency layer ({@code BombForkJoinPool}, {@code ChunkUtil}, {@code com.hbm.lib.queues.*}) is
 * not ported (see Deferred scope / Package C in the research report - shared scope with Phase 3's
 * already-deferred {@code ExplosionNukeRayParallelized}). What survives is the actual per-column
 * terrain-mutation math ({@link #stompColumn}, a direct, faithful port of CE's real {@code
 * stompColumnToUpdates}) and the outer-rim/inner-disk chunk-set sampling ({@link #gatherChunks}, CE's
 * real {@code gatherChunks} geometry, unchanged), reimplemented against plain {@link Level#setBlock}
 * calls budgeted across ticks by the same real {@link BombConfig#FALLOUT_DELAY}/
 * {@link BombConfig#FALLOUT_CHUNK_SPEED} config values CE itself tunes this system with - this is
 * "the pragmatic 'make nukes leave scarred, irradiated craters' deliverable" the report names.
 * <p>
 * <b>Two real CE constructors, faithfully preserved</b> (see the research report's Headline finding
 * #3): {@link #EntityFalloutRain(Level)} is the only one CE's real logic ever needs; {@link
 * #EntityFalloutRain(Level, int)} silently discards its {@code int} argument and delegates to it -
 * confirmed by reading CE's own constructor body, not a guess. {@link
 * #EntityFalloutRain(EntityType, Level)} is the registry-factory constructor {@code EntityType.
 * Builder.of(...)} requires; CE has no equivalent since {@code @AutoRegister} generates it.
 * <p>
 * <b>Not ported</b> (see class-by-class notes below and the research report's own scope split):
 * structural-collapse falling-block spawning (CE's own {@code EntityFallingBlock} debris scatter for
 * unsupported soft blocks after a conversion) - a real but secondary visual/physics side effect of
 * {@code stompColumnToUpdates}, dropped here to keep this pass's scope to the core radiation/terrain
 * behavior; the {@code ModBlocks.volcano_core -> volcano_rad_core} hardcoded special case (neither
 * block exists anywhere in this port yet); the NBT-restore fast path notifying dynamic-tree-mod
 * neighbors ({@code CompatDynamicTrees}, already a documented non-port per the research report).
 * <p>
 * <b>{@code CompatibilityConfig.isWarDim} stub</b>: matches the already-committed convention at
 * {@code com.hbm.potion.HbmPotionEffects#isWarDim} (stubbed {@code true}, not {@code false} - see
 * that method's own javadoc for why CE's real default makes {@code true} the correct out-of-the-box
 * value) - duplicated here as a small package-local helper rather than widening that method's
 * visibility, since the two areas are otherwise unrelated.
 */
public class EntityFalloutRain extends EntityExplosionChunkloading {

    private static final int MAX_SOLID_DEPTH = 3;
    private static final int MIN_ANGLE_STEPS = 18;
    private static final int SPOKE_STEP_BLOCKS = 8;

    private static final EntityDataAccessor<Integer> SCALE =
            SynchedEntityData.defineId(EntityFalloutRain.class, EntityDataSerializers.INT);

    public UUID detonator;
    private boolean biomeChange = true;
    private boolean gathered = false;

    private final ArrayDeque<ChunkWork> pending = new ArrayDeque<>();

    public EntityFalloutRain(EntityType<? extends EntityFalloutRain> entityType, Level level) {
        super(entityType, level);
    }

    /** CE's real, only-ever-used constructor: {@code setSize(4.0F, 20.0F)} equivalent is the registry's {@code .sized(4F, 20F)}. */
    public EntityFalloutRain(Level level) {
        this(NukeEntityTypes.FALLOUT_RAIN.get(), level);
    }

    /** CE: {@code public EntityFalloutRain(World worldIn, int ignored) { this(worldIn); }} - the {@code int} is discarded. */
    public EntityFalloutRain(Level level, int ignored) {
        this(level);
    }

    /** Package-local stub matching {@code com.hbm.potion.HbmPotionEffects#isWarDim}'s established convention - see class javadoc. */
    // TODO(CompatibilityConfig.isWarDim, Phase 4 world-gen/dimension-config): stubbed true.
    private static boolean isWarDim(Level level) {
        return true;
    }

    public int getScale() {
        int scale = this.entityData.get(SCALE);
        return scale <= 0 ? 1 : scale;
    }

    public void setScale(int scale) {
        this.entityData.set(SCALE, scale);
    }

    /** CE has this exact dead-parameter overload alongside {@link #setScale(int)} - preserved faithfully. */
    public void setScale(int scale, int ignored) {
        this.entityData.set(SCALE, scale);
    }

    public void noBiomeChange() {
        this.biomeChange = false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SCALE, 1);
    }

    @Override
    public void tick() {
        super.tick();
        Level level = level();
        if (level.isClientSide()) return;

        if (!isWarDim(level)) {
            discard();
            return;
        }

        if (!gathered) {
            gatherChunks();
            loadChunk(chunkPosition().x, chunkPosition().z);
            gathered = true;
            if (pending.isEmpty()) {
                discard();
                return;
            }
        }

        int chunkSpeed = Math.max(1, BombConfig.FALLOUT_CHUNK_SPEED.get());
        if (this.tickCount % chunkSpeed != 0) return;

        long budgetNanos = Math.max(0L, (long) BombConfig.FALLOUT_DELAY.get()) * 1_000_000L;
        long deadline = System.nanoTime() + budgetNanos;
        while (!pending.isEmpty() && System.nanoTime() < deadline) {
            processChunk(level, pending.poll());
        }

        if (pending.isEmpty()) discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        clearChunkLoader();
        super.remove(reason);
    }

    // ==================== chunk-set sampling (CE gatherChunks, unchanged geometry) ====================

    private void gatherChunks() {
        int radius = getScale();
        int angleSteps = Math.max(MIN_ANGLE_STEPS, 20 * radius / 32);

        double px = getX();
        double pz = getZ();

        double[] cos = new double[angleSteps + 1];
        double[] sin = new double[angleSteps + 1];
        for (int step = 0; step <= angleSteps; step++) {
            double theta = step * (2.0 * Math.PI) / angleSteps;
            cos[step] = Math.cos(theta);
            sin[step] = Math.sin(theta);
        }

        Set<ChunkPos> outer = new LinkedHashSet<>();
        for (int step = 0; step <= angleSteps; step++) {
            double dx = radius * cos[step];
            double dz = -radius * sin[step];
            outer.add(new ChunkPos(((int) Math.floor(px + dx)) >> 4, ((int) Math.floor(pz + dz)) >> 4));
        }

        Set<ChunkPos> inner = new LinkedHashSet<>();
        for (int d = 0; d <= radius; d += SPOKE_STEP_BLOCKS) {
            for (int step = 0; step <= angleSteps; step++) {
                double dx = d * cos[step];
                double dz = -d * sin[step];
                ChunkPos cp = new ChunkPos(((int) Math.floor(px + dx)) >> 4, ((int) Math.floor(pz + dz)) >> 4);
                if (!outer.contains(cp)) inner.add(cp);
            }
        }

        pending.clear();
        for (ChunkPos cp : inner) pending.add(new ChunkWork(cp.x, cp.z, false));
        for (ChunkPos cp : outer) pending.add(new ChunkWork(cp.x, cp.z, true));
    }

    // ==================== per-chunk / per-column terrain mutation (CE stompColumnToUpdates) ====================

    private void processChunk(Level level, ChunkWork work) {
        int scale = getScale();
        double cx = getX();
        double cz = getZ();
        int minX = work.cx() << 4;
        int minZ = work.cz() << 4;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int lx = 0; lx < 16; lx++) {
            int x = minX + lx;
            for (int lz = 0; lz < 16; lz++) {
                int z = minZ + lz;
                double distance = Math.hypot(x - cx, z - cz);
                if (work.clampToRadius() && distance > scale) continue;

                double percent = scale <= 0 ? 100.0 : distance * 100.0 / scale;

                paintBiome(level, x, z, percent, scale);
                stompColumn(level, x, z, percent, pos, level.getRandom());
            }
        }
    }

    /**
     * CE {@code stompColumnToUpdates} (681-783), ported directly onto plain {@link Level} calls
     * instead of CE's raw {@code ExtendedBlockStorage} array poking - see class javadoc for the two
     * real omissions (volcano core, falling-block structural collapse).
     */
    private void stompColumn(Level level, int x, int z, double distPercent, BlockPos.MutableBlockPos pos, RandomSource random) {
        int solidDepth = 0;
        int minY = level.getMinY();
        int maxY = level.getMaxY();
        List<FalloutEntry> entries = FalloutConfigJSON.ENTRIES;

        for (int y = maxY; y >= minY; y--) {
            if (solidDepth >= MAX_SOLID_DEPTH) return;

            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.is(FalloutBlocks.FALLOUT.get())) continue;

            // CE's ModBlocks.volcano_core -> volcano_rad_core hardcoded branch is not portable -
            // neither block exists anywhere in this port yet (see class javadoc).

            int upY = y + 1;
            if (solidDepth == 0 && upY <= maxY) {
                pos.set(x, upY, z);
                BlockState upState = level.getBlockState(pos);
                boolean airOrReplaceable = upState.isAir() || (upState.canBeReplaced() && upState.getFluidState().isEmpty());
                if (airOrReplaceable) {
                    double d = distPercent / 100.0;
                    double chance = 0.1 - Math.pow(d - 0.7, 2.0);
                    if (chance >= random.nextDouble()) {
                        level.setBlock(pos, FalloutBlocks.FALLOUT.get().defaultBlockState(), 3);
                    }
                }
            }

            // CE: distPercent<65 && block.isFlammable(world, pos, UP) -> 1-in-5 chance to set the
            // block above alight. Not portable - CE's Block#isFlammable(IBlockAccess,BlockPos,
            // EnumFacing) has no single confirmed 1.21.1 replacement, matching this port's own
            // already-documented identical gap for the same CE API (see GrenadeFillingActions'
            // igniteAround note and ExplosionNukeGeneric#vaporDest); dropped rather than guessed at.

            pos.set(x, y, z);
            boolean transformed = false;
            for (int i = 0, size = entries.size(); i < size; i++) {
                BlockState result = entries.get(i).eval(y, state, distPercent, random);
                if (result != null) {
                    level.setBlock(pos, result, 3);
                    if (entries.get(i).isSolid()) solidDepth++;
                    transformed = true;
                    break;
                }
            }

            // CE's structural-collapse falling-block scatter (unsupported soft blocks after a
            // conversion) is not ported - see class javadoc.

            if (!transformed && state.canOcclude()) solidDepth++;
        }
    }

    // ==================== crater-biome painting (CE getBiomeChange) ====================

    @Nullable
    private static ResourceKey<Biome> getBiomeChangeKey(double distPercent, int scale, @Nullable ResourceKey<Biome> original) {
        if (!WorldConfig.ENABLE_CRATER_BIOMES.get()) return null;
        if (scale >= 150 && distPercent < 15) return ModCraterBiomes.CRATER_INNER;
        if (scale >= 100 && distPercent < 55 && original != ModCraterBiomes.CRATER_INNER) return ModCraterBiomes.CRATER;
        if (scale >= 25 && original != ModCraterBiomes.CRATER_INNER && original != ModCraterBiomes.CRATER) return ModCraterBiomes.CRATER_OUTER;
        return null;
    }

    /**
     * Server-side biome reassignment for the crater-biome system - this only paints the biome data
     * itself; the actual ambient-radiation consequence of standing in one is a separate concern,
     * handled by {@code com.hbm.handler.EntityEffectHandler#handleCraterRadiation} (landed later in
     * this same content wave - out of this package's own scope, but no longer a forward reference).
     * Writes every 4-block ("quart") vertical biome cell in the column, matching modern Minecraft's
     * 3D biome storage (CE's 1.12 chunk format only ever needed one biome byte per column).
     */
    private void paintBiome(Level level, int x, int z, double distPercent, int scale) {
        if (!biomeChange || !(level instanceof ServerLevel serverLevel)) return;

        ResourceKey<Biome> original = level.getBiome(new BlockPos(x, level.getSeaLevel(), z)).unwrapKey().orElse(null);
        ResourceKey<Biome> target = getBiomeChangeKey(distPercent, scale, original);
        if (target == null || target == original) return;

        Optional<Holder.Reference<Biome>> holderOpt = serverLevel.registryAccess().registryOrThrow(Registries.BIOME).getHolder(target);
        if (holderOpt.isEmpty()) return;
        Holder<Biome> holder = holderOpt.get();

        ChunkAccess chunk = serverLevel.getChunk(x >> 4, z >> 4);
        int qx = x >> 2;
        int qz = z >> 2;
        for (int qy = level.getMinY() >> 2; qy <= level.getMaxY() >> 2; qy++) {
            chunk.setBiome(qx, qy, qz, holder);
        }
    }

    // ==================== persistence ====================

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        markChunkLoaderRestoredFromNBT();
        setScale(tag.getInt("scale"));
        if (tag.hasUUID("detonator")) detonator = tag.getUUID("detonator");
        biomeChange = !tag.contains("noBiomeChange") || !tag.getBoolean("noBiomeChange");

        pending.clear();
        readWork(tag.getIntArray("chunks"), false);
        readWork(tag.getIntArray("outerChunks"), true);
        gathered = true;
    }

    private void readWork(int[] pairs, boolean clamp) {
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            pending.add(new ChunkWork(pairs[i], pairs[i + 1], clamp));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("scale", getScale());
        if (detonator != null) tag.putUUID("detonator", detonator);
        if (!biomeChange) tag.putBoolean("noBiomeChange", true);

        List<ChunkWork> inner = pending.stream().filter(w -> !w.clampToRadius()).toList();
        List<ChunkWork> outer = pending.stream().filter(ChunkWork::clampToRadius).toList();
        tag.putIntArray("chunks", toPairs(inner));
        tag.putIntArray("outerChunks", toPairs(outer));
    }

    private static int[] toPairs(List<ChunkWork> list) {
        int[] out = new int[list.size() * 2];
        int i = 0;
        for (ChunkWork w : list) {
            out[i++] = w.cx();
            out[i++] = w.cz();
        }
        return out;
    }

    private record ChunkWork(int cx, int cz, boolean clampToRadius) {
    }
}
