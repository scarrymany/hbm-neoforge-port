package com.hbm.explosion;

import com.hbm.config.BombConfig;
import com.hbm.interfaces.IExplosionRay;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ported from CE's {@code com.hbm.explosion.ExplosionNukeRayBatched} (330 lines, read in full) -
 * {@code BombConfig.explosionAlgorithm = 0} ("Legacy"), and the algorithm this pass ports for real
 * (see {@code docs/phase3/explosion_engine.md}'s "Deferred scope": the fully-threaded, off-heap,
 * {@code sun.misc.Unsafe}-based default algorithm {@code ExplosionNukeRayParallelized} is
 * explicitly deferred to a later, separately-verified pass, not attempted here).
 * <p>
 * Two-phase state machine, exactly matching CE:
 * <ol>
 *     <li>{@link #cacheChunksTick(int)} walks a Fibonacci/"generalized spiral" sphere point set,
 *     steps each ray outward from the epicenter eating a resistance budget, and buffers every
 *     surviving position into a per-{@link ChunkPos} {@link BitSet} (one bit per local
 *     {@code (invertedY, x, z)} index within that chunk's full column) instead of removing it
 *     immediately.</li>
 *     <li>{@link #destructionTick(int)} drains one chunk's {@code BitSet} at a time, nearest-
 *     epicenter-first.</li>
 * </ol>
 * <p>
 * <b>1.21.1 adaptation (the one deliberate behavior change from CE)</b>: CE hardcoded the Y bounds
 * to {@code [0, 255]} (1.12's fixed world height) both for the ray-cast bounds check and for the
 * per-chunk bit-index packing. This port instead reads {@link Level#getMinBuildHeight()}/
 * {@link Level#getMaxBuildHeight()} once at construction so the algorithm is correct on any
 * dimension's real height range (e.g. the default overworld's {@code [-64, 320)}) rather than
 * silently corrupting or truncating explosions above/below Y 255/0. The bit-packing scheme itself
 * (one {@link BitSet} per touched chunk, sized for that dimension's full column) is otherwise
 * identical to CE's.
 * <p>
 * <b>Block removal (PORT_SPEC's batching mandate)</b>: CE's own {@code processChunkBlocks} already
 * groups every position by chunk before removing anything, but its actual removal is still one
 * {@code World#setBlockToAir} call per block. This port keeps CE's exact chunk-grouping/time-
 * budgeting shape but replaces that final per-block vanilla write with a real batched section
 * write (see {@link NukeChunkBlockRemoval}) once each chunk's bitset is fully drained - one direct
 * {@code LevelChunkSection} write pass, one deferred light-engine recheck queue, one
 * {@code setUnsaved}, and one client-resync packet, per touched chunk (not per block, not even per
 * tick-slice of a chunk that takes several ticks to fully drain).
 */
public class ExplosionNukeRayBatched implements IExplosionRay {

    private final HashMap<ChunkPos, BitSet> perChunk = new HashMap<>();
    private final List<ChunkPos> orderedChunks = new ArrayList<>();
    private final CoordComparator comparator = new CoordComparator();
    private boolean isContained = true;
    private int posX;
    private int posY;
    private int posZ;
    private final Level level;

    private int strength;
    private int radius;

    private int gspNumMax;
    private int gspNum;
    private double gspX;
    private double gspY;

    /** 1.21.1 adaptation: real dimension height range instead of CE's hardcoded [0,255]. See class javadoc. */
    private final int minY;
    private final int maxY;

    private boolean isAusf3Complete = false;
    private int rayCheckInterval = 100;
    private UUID detonator;

    public ExplosionNukeRayBatched(Level level, int x, int y, int z, int strength, int radius) {
        this.level = level;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.strength = strength;
        this.radius = radius;
        this.minY = level.getMinBuildHeight();
        this.maxY = level.getMaxBuildHeight() - 1;

        // Total number of points
        this.gspNumMax = (int) (2.5 * Math.PI * Math.pow(this.strength, 2));
        this.gspNum = 1;

        // The beginning of the generalized spiral points
        this.gspX = Math.PI;
        this.gspY = 0.0;
        this.rayCheckInterval = radius > 0 ? Math.max(1, 10000 / radius) : 10000;
    }

    private void generateGspUp() {
        if (this.gspNum < this.gspNumMax) {
            int k = this.gspNum + 1;
            double hk = -1.0 + 2.0 * (k - 1.0) / (this.gspNumMax - 1.0);
            this.gspX = Math.acos(hk);

            double prevLon = this.gspY;
            double lon = prevLon + 3.6 / Math.sqrt(this.gspNumMax) / Math.sqrt(1.0 - hk * hk);
            this.gspY = lon % (Math.PI * 2);
        } else {
            this.gspX = 0.0;
            this.gspY = 0.0;
        }
        this.gspNum++;
    }

    /** Cartesian direction for the current spherical spiral point (90° X-axis rotation for cheaper chunk scanning, matching CE). */
    private record Dir(double x, double y, double z) {
    }

    private Dir getSpherical2cartesian() {
        double dx = Math.sin(this.gspX) * Math.cos(this.gspY);
        double dy = Math.sin(this.gspX) * Math.sin(this.gspY);
        double dz = Math.cos(this.gspX);
        return new Dir(dx, dy, dz);
    }

    /** Local column-relative bit index within a chunk's full-height {@link BitSet}. */
    private int localIndex(int localY, int localX, int localZ) {
        return (localY << 8) + (localX << 4) + localZ;
    }

    public void addPos(int x, int y, int z) {
        ChunkPos chunk = new ChunkPos(x >> 4, z >> 4);
        BitSet hitPositions = perChunk.computeIfAbsent(chunk, k -> new BitSet((maxY - minY + 1) * 256));

        // re-use the same pos instead of individualized per-chunk offsets, to save on RAM
        hitPositions.set(localIndex(maxY - y, x - chunk.getMinBlockX(), z - chunk.getMinBlockZ()));
    }

    private void cacheChunksTick(int time) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        long raysProcessed = 0;
        long start = System.currentTimeMillis();

        while (this.gspNumMax >= this.gspNum) {
            Dir vec = this.getSpherical2cartesian();

            int r0 = this.radius;
            float rayStrength = strength * 0.3F;

            for (int r = 0; r < r0 + 1; r++) {
                int iY = (int) Math.floor(posY + (vec.y() * r));

                if (iY < minY || iY > maxY) {
                    isContained = false;
                    break;
                }

                int iX = (int) Math.floor(posX + (vec.x() * r));
                int iZ = (int) Math.floor(posZ + (vec.z() * r));

                pos.set(iX, iY, iZ);
                BlockState blockState = level.getBlockState(pos);
                if (blockState.getBlock().getExplosionResistance() >= 2_000_000) break;

                // CE quirk, preserved exactly: `r = (r > 0) ? r : 1` reassigns the *loop* variable
                // itself (not a separate local), which - because of the for-loop's own `r++` -
                // means every ray silently skips r==1 (goes 0, [becomes 1 here], then increments
                // to 2). This looks like an accidental side effect of a terse one-liner rather than
                // a deliberate design, but per this project's "CE is the sole source of truth for
                // behavior, don't silently fix apparent bugs" rule, it's reproduced verbatim rather
                // than cleaned up into `effR = r > 0 ? r : 1` (which would NOT skip r==1).
                r = (r > 0) ? r : 1;
                rayStrength -= (float) (Math.pow(getNukeResistance(blockState) + 1, 3 * (float) r / (float) r0) - 1);

                if (rayStrength > 0) {
                    if (!blockState.is(Blocks.AIR)) {
                        addPos(iX, iY, iZ);
                    }
                    if (r >= r0) {
                        isContained = false;
                    }
                } else {
                    break;
                }
            }

            this.generateGspUp();
            raysProcessed++;
            if (raysProcessed % rayCheckInterval == 0 && System.currentTimeMillis() + 1 > start + time) {
                return;
            }
        }

        orderedChunks.addAll(perChunk.keySet());
        orderedChunks.sort(comparator);

        isAusf3Complete = true;
    }

    /** CE-confirmed special-cased overrides on top of {@code Block#getExplosionResistance()}. */
    public static float getNukeResistance(BlockState blockState) {
        if (!blockState.getFluidState().isEmpty()) {
            return 0.1F;
        }
        if (blockState.is(Blocks.SANDSTONE)) return 4F;
        if (blockState.is(Blocks.OBSIDIAN)) return 18F;
        return blockState.getBlock().getExplosionResistance();
    }

    @Override
    public void update(int processTimeMs) {
        // CE gates every update() call on CompatibilityConfig.isWarDim(world); this port's own
        // CompatibilityConfig deliberately drops that dimension-keyed gate (see
        // docs/phase3/explosion_engine.md's "Key design/API decisions" - always-true by default,
        // deferred to whichever phase re-introduces dimension-keyed config).
        if (isAusf3Complete) {
            destructionTick(processTimeMs);
        } else {
            cacheChunksTick(processTimeMs);
        }
    }

    @Override
    public void cancel() {
        isAusf3Complete = true;
        orderedChunks.clear();
        perChunk.clear();
        pendingSync.clear();
    }

    @Override
    public boolean isComplete() {
        return isAusf3Complete && perChunk.isEmpty();
    }

    @Override
    public boolean isContained() {
        return isContained;
    }

    @Override
    public void setDetonator(UUID detonator) {
        this.detonator = detonator;
    }

    /** Roughly sorts chunks by Manhattan distance to the epicenter chunk, nearest first. */
    private class CoordComparator implements Comparator<ChunkPos> {
        @Override
        public int compare(ChunkPos o1, ChunkPos o2) {
            int chunkX = ExplosionNukeRayBatched.this.posX >> 4;
            int chunkZ = ExplosionNukeRayBatched.this.posZ >> 4;

            int diff1 = Math.abs(chunkX - o1.x) + Math.abs(chunkZ - o1.z);
            int diff2 = Math.abs(chunkX - o2.x) + Math.abs(chunkZ - o2.z);

            return Integer.compare(diff1, diff2);
        }
    }

    private BitSet hitArray;
    private ChunkPos chunk;
    private boolean needsNewHitArray = true;
    private int index = 0;
    /** Positions removed so far from {@link #chunk}, flushed as one batch once its bitset fully drains. */
    private final List<BlockPos> pendingSync = new ArrayList<>();

    public void destructionTick(int time) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() < start + time) {
            processChunkBlocks(start, time);
            if (this.perChunk.isEmpty()) break;
        }
    }

    public void processChunkBlocks(long start, int time) {
        if (this.perChunk.isEmpty()) return;
        if (needsNewHitArray) {
            chunk = orderedChunks.get(0);
            hitArray = perChunk.get(chunk);
            index = hitArray.nextSetBit(0);
            needsNewHitArray = false;
        }

        int chunkX = chunk.getMinBlockX();
        int chunkZ = chunk.getMinBlockZ();

        int blocksRemoved = 0;
        while (index > -1) {
            int localY = index >> 8;
            int localX = (index >> 4) & 15;
            int localZ = index & 15;
            pendingSync.add(new BlockPos(chunkX + localX, maxY - localY, chunkZ + localZ));

            index = hitArray.nextSetBit(index + 1);
            blocksRemoved++;
            if (blocksRemoved % 256 == 0 && System.currentTimeMillis() + 1 > start + time) {
                break;
            }
        }

        if (index < 0) {
            // Chunk fully drained - flush the whole batch through one direct section write, one
            // deferred relight queue, and one client resync packet (see NukeChunkBlockRemoval).
            // NukeChunkBlockRemoval hands pendingSync's contents to a network payload that may
            // outlive this method call (packet encoding/send timing is not guaranteed synchronous),
            // so pass a defensive copy rather than the reused field - pendingSync is cleared and
            // reused for the next chunk immediately below.
            NukeChunkBlockRemoval.removeAndSync(level, chunk, new ArrayList<>(pendingSync));
            pendingSync.clear();

            perChunk.remove(chunk);
            orderedChunks.remove(0);
            needsNewHitArray = true;
        }
    }

    @Override
    public void readEntityFromNBT(CompoundTag nbt) {
        radius = nbt.getInt("radius");
        strength = nbt.getInt("strength");
        posX = nbt.getInt("posX");
        posY = nbt.getInt("posY");
        posZ = nbt.getInt("posZ");
        rayCheckInterval = radius > 0 ? Math.max(1, 10000 / radius) : 10000;
        gspNumMax = (int) (2.5 * Math.PI * Math.pow(strength, 2));

        if (nbt.contains("gspNum")) {
            gspNum = nbt.getInt("gspNum");
            isAusf3Complete = nbt.getBoolean("f3");
            isContained = nbt.getBoolean("isContained");

            int i = 0;
            while (nbt.contains("chunks" + i)) {
                CompoundTag c = nbt.getCompound("chunks" + i);
                perChunk.put(new ChunkPos(c.getInt("cX"), c.getInt("cZ")), BitSet.valueOf(c.getLongArray("cB")));
                i++;
            }
            if (isAusf3Complete) {
                orderedChunks.addAll(perChunk.keySet());
                orderedChunks.sort(comparator);
            }
        }
    }

    @Override
    public void writeEntityToNBT(CompoundTag nbt) {
        nbt.putInt("radius", radius);
        nbt.putInt("strength", strength);
        nbt.putInt("posX", posX);
        nbt.putInt("posY", posY);
        nbt.putInt("posZ", posZ);

        if (BombConfig.ENABLE_NUKE_NBT_SAVING.get()) {
            nbt.putInt("gspNum", gspNum);
            nbt.putBoolean("f3", isAusf3Complete);
            nbt.putBoolean("isContained", isContained);

            int i = 0;
            for (Map.Entry<ChunkPos, BitSet> e : perChunk.entrySet()) {
                CompoundTag c = new CompoundTag();
                c.putInt("cX", e.getKey().x);
                c.putInt("cZ", e.getKey().z);
                c.putLongArray("cB", e.getValue().toLongArray());
                nbt.put("chunks" + i, c);
                i++;
            }
        }
    }
}
