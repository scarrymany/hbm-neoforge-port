package com.hbm.handler.pollution;

import com.hbm.config.MobConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.main.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.handler.pollution.PollutionHandler} (396 lines, the entire CE
 * package - {@code PollutionData}/{@code PollutionPerWorld}/{@code PollutionType} are all nested
 * inside this one file in CE, replicated here for the same reason: no sibling classes needed).
 * <p>
 * <b>This class is not purely forward-looking infrastructure - it is a live compile-break fix.</b>
 * {@code com.hbm.inventory.fluid.Fluids} and {@code com.hbm.inventory.fluid.trait.FT_Polluting}
 * already {@code import} and directly call {@code PollutionHandler.incrementPollution}/the
 * {@code SOOT_PER_SECOND}/{@code HEAVY_METAL_PER_SECOND}/{@code POISON_PER_SECOND} constants and
 * the nested {@link PollutionType} enum, exactly matching the signatures below - see
 * {@code docs/phase4/pollution_system.md}'s Headline finding #1.
 * <p>
 * <b>{@link PollutionType} and {@link PollutionData} are intentionally nested inside this class,
 * not top-level siblings</b>: {@code FT_Polluting.java} and {@code Fluids.java} already contain
 * {@code import com.hbm.handler.pollution.PollutionHandler.PollutionType;} - a nested-class import
 * - so {@code PollutionType} must physically be a member type of {@code PollutionHandler} for the
 * existing tree to compile, matching CE's own nesting exactly.
 * <p>
 * <b>Storage is not per-Minecraft-chunk</b>: the lookup key is a 64-block coarse cell
 * ({@code new ChunkPos(pos.getX() >> 6, pos.getZ() >> 6)} - shift by 6, not 4), abusing
 * {@link ChunkPos} purely for its hashing, exactly as CE does. See {@link PollutionSavedData} for
 * the per-dimension persistence shape (a {@code SavedData}, not CE's raw file I/O).
 */
@EventBusSubscriber(modid = MainRegistry.MODID)
public class PollutionHandler {

    /** Baserate of soot generation for a furnace-equivalent machine per second. */
    public static final float SOOT_PER_SECOND = 1F / 25F;
    /** Baserate of heavy metal generation, balanced around the soot values of combustion engines. */
    public static final float HEAVY_METAL_PER_SECOND = 1F / 50F;
    /** Baserate for poison when spilled. */
    public static final float POISON_PER_SECOND = 1F / 50F;

    /**
     * CE: set by {@code PollutionHandler#rampantTargetSetter(PlayerSleepInBedEvent)} - the last bed
     * position a player slept in, used by Rampant Mode's {@code EntityGlyphidScout} guidance AI to
     * steer swarms toward player bases. Not consumed by anything in this port yet (no Glyphid mobs
     * exist) - see {@link #rampantTargetSetter(BlockPos)}.
     */
    @Nullable
    public static Vec3 targetCoords;

    private PollutionHandler() {
    }

    ///////////////////////
    /// UTILITY METHODS ///
    ///////////////////////

    /** CE: {@code new ChunkPos(pos.getX() >> 6, pos.getZ() >> 6)} - a 64-block coarse cell. */
    private static ChunkPos cellPos(BlockPos pos) {
        return new ChunkPos(pos.getX() >> 6, pos.getZ() >> 6);
    }

    public static void incrementPollution(Level level, BlockPos pos, PollutionType type, float amount) {
        if (!RadiationConfig.ENABLE_POLLUTION.get() || pos == null || level == null) return;
        if (!(level instanceof ServerLevel serverLevel)) return; // no client-side pollution cache, matches CE

        PollutionSavedData saved = PollutionSavedData.forLevel(serverLevel);
        PollutionData data = saved.pollution.computeIfAbsent(cellPos(pos), c -> new PollutionData());

        int idx = type.ordinal();
        // CE: MathHelper.clamp((float) (data.pollution[type.ordinal()] + amount * MobConfig.pollutionMult), 0F, 10_000F)
        // MobConfig.effectivePollutionMult() folds in CE's Rampant-Mode `pollutionMult==1 ? 3 : pollutionMult`
        // post-load override as a derived getter - do not reimplement that override here.
        data.pollution[idx] = Mth.clamp((float) (data.pollution[idx] + amount * MobConfig.effectivePollutionMult()), 0F, 10_000F);
        saved.setDirty();
    }

    public static void decrementPollution(Level level, BlockPos pos, PollutionType type, float amount) {
        incrementPollution(level, pos, type, -amount);
    }

    public static void setPollution(Level level, BlockPos pos, PollutionType type, float amount) {
        if (!RadiationConfig.ENABLE_POLLUTION.get() || pos == null || level == null) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        PollutionSavedData saved = PollutionSavedData.forLevel(serverLevel);
        PollutionData data = saved.pollution.computeIfAbsent(cellPos(pos), c -> new PollutionData());
        data.pollution[type.ordinal()] = amount;
        saved.setDirty();
    }

    public static float getPollution(Level level, BlockPos pos, PollutionType type) {
        if (!RadiationConfig.ENABLE_POLLUTION.get() || pos == null || level == null) return 0F;
        if (!(level instanceof ServerLevel serverLevel)) return 0F;

        PollutionSavedData saved = PollutionSavedData.forLevel(serverLevel);
        PollutionData data = saved.pollution.get(cellPos(pos));
        return data == null ? 0F : data.pollution[type.ordinal()];
    }

    @Nullable
    public static PollutionData getPollutionData(Level level, BlockPos pos) {
        if (!RadiationConfig.ENABLE_POLLUTION.get() || pos == null || level == null) return null;
        if (!(level instanceof ServerLevel serverLevel)) return null;

        PollutionSavedData saved = PollutionSavedData.forLevel(serverLevel);
        return saved.pollution.get(cellPos(pos));
    }

    //////////////////////////
    /// SYSTEM UPDATE LOOP ///
    //////////////////////////

    private static int eggTimer = 0;

    /**
     * CE: {@code PollutionHandler#updateSystem(TickEvent.ServerTickEvent)}, gated to
     * {@code Side.SERVER}/{@code Phase.END}. This port self-subscribes on {@code ServerTickEvent.Pre}
     * matching this port's own {@code com.hbm.handler.neutron.NeutronHandler} precedent (confirmed
     * real, no functional difference from {@code .Post} for a 60-tick-gated pass).
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        if (!RadiationConfig.ENABLE_POLLUTION.get()) return;

        handleWorldDestruction(event.getServer());

        eggTimer++;
        if (eggTimer < 60) return;
        eggTimer = 0;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            decayAndDiffuse(level);
        }
    }

    /**
     * The 60-tick decay/diffusion pass, per dimension. CE: {@code updateSystem}'s per-{@code World}
     * body. SOOT: above 10, siphons 5% to each of the 4 orthogonal neighbor cells then x0.8,
     * unconditionally x0.99 afterward. HEAVYMETAL: x0.9995 only - <b>never written into the
     * neighbor array at all</b>, so heavy metal never spreads geographically in CE, only decays in
     * place (a real, confirmed CE asymmetry - preserve it exactly). POISON: above 10, siphons 2.5%
     * then x0.9, else x0.995. Built into a fresh map from this pass's self-remainder plus incoming
     * neighbor contributions, then swapped in - not in-place mutation during iteration.
     */
    private static void decayAndDiffuse(ServerLevel level) {
        PollutionSavedData saved = PollutionSavedData.forLevel(level);
        if (saved.pollution.isEmpty()) return;

        Map<ChunkPos, PollutionData> newPollution = new HashMap<>();

        int S = PollutionType.SOOT.ordinal();
        int H = PollutionType.HEAVYMETAL.ordinal();
        int P = PollutionType.POISON.ordinal();

        for (Map.Entry<ChunkPos, PollutionData> chunk : saved.pollution.entrySet()) {
            ChunkPos pos = chunk.getKey();
            int x = pos.x;
            int z = pos.z;
            PollutionData data = chunk.getValue();

            float[] pollutionForNeighbors = new float[PollutionType.VALUES.length];

            /* CALCULATION */
            if (data.pollution[S] > 10F) {
                pollutionForNeighbors[S] = data.pollution[S] * 0.05F;
                data.pollution[S] *= 0.8F;
            }

            data.pollution[S] *= 0.99F;
            data.pollution[H] *= 0.9995F;

            if (data.pollution[P] > 10F) {
                pollutionForNeighbors[P] = data.pollution[P] * 0.025F;
                data.pollution[P] *= 0.9F;
            } else {
                data.pollution[P] *= 0.995F;
            }

            /* SPREADING */
            // apply new data to self
            PollutionData selfNew = newPollution.get(pos);
            if (selfNew == null) selfNew = new PollutionData();

            boolean shouldPut = false;
            for (int i = 0; i < selfNew.pollution.length; i++) {
                selfNew.pollution[i] += data.pollution[i];
                if (selfNew.pollution[i] > 0F) shouldPut = true;
            }
            if (shouldPut) newPollution.put(pos, selfNew);

            // apply neighbor data to neighboring cells
            int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] offset : offsets) {
                ChunkPos offPos = new ChunkPos(x + offset[0], z + offset[1]);
                PollutionData offsetData = newPollution.get(offPos);
                if (offsetData == null) offsetData = new PollutionData();

                boolean shouldPutOff = false;
                for (int i = 0; i < offsetData.pollution.length; i++) {
                    offsetData.pollution[i] += pollutionForNeighbors[i];
                    if (offsetData.pollution[i] > 0F) shouldPutOff = true;
                }
                if (shouldPutOff) newPollution.put(offPos, offsetData);
            }
        }

        saved.pollution.clear();
        saved.pollution.putAll(newPollution);
        saved.setDirty();
    }

    private static final float DESTRUCTION_THRESHOLD = 15F;
    private static final int DESTRUCTION_COUNT = 5;

    /**
     * CE: {@code PollutionHandler#handleWorldDestruction()} - runs every server tick (not gated by
     * the 60-tick timer), unconditionally, for every stored cell whose POISON exceeds 15: picks 5
     * random surface points inside that 64-block cell per tick and kills grass/plants there. This
     * is the only place in the whole system that checks whether the real chunk is loaded.
     */
    private static void handleWorldDestruction(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            PollutionSavedData saved = PollutionSavedData.forLevel(level);
            if (saved.pollution.isEmpty()) continue;

            for (Map.Entry<ChunkPos, PollutionData> entry : saved.pollution.entrySet()) {
                float poison = entry.getValue().pollution[PollutionType.POISON.ordinal()];
                if (poison < DESTRUCTION_THRESHOLD) continue;

                ChunkPos cell = entry.getKey();

                for (int i = 0; i < DESTRUCTION_COUNT; i++) {
                    int x = (cell.x << 6) + level.getRandom().nextInt(64);
                    int z = (cell.z << 6) + level.getRandom().nextInt(64);

                    if (level.hasChunk(x >> 4, z >> 4)) {
                        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - level.getRandom().nextInt(3) + 1;
                        BlockPos bPos = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(bPos);

                        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)) {
                            level.setBlock(bPos, Blocks.DIRT.defaultBlockState(), 3);
                        } else if (isPollutionSweepTarget(state)) {
                            level.setBlock(bPos, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    /**
     * CE: {@code b == Blocks.TALLGRASS || b.getMaterial(state) == Material.LEAVES ||
     * b.getMaterial(state) == Material.PLANTS} - the removed 1.12.2 {@code Material} system has no
     * 1.21.1 equivalent, so this replicates its membership with confirmed-real vanilla tags/block
     * constants instead of inventing a new custom tag (Neo Edition's own port invents
     * {@code NtmTags.Blocks.PLANTS} for this - not reused here per this survey's explicit
     * direction).
     * <p>
     * <b>Fixed CE-behavior mismatch (review pass):</b> the previous version of this method also
     * matched {@code Blocks.VINE}/{@code Blocks.DEAD_BUSH}/{@code Blocks.TALL_GRASS} (the modern
     * double-tall grass)/{@code Blocks.LARGE_FERN}, plus the full {@code BlockTags.FLOWERS} tag
     * (which nests {@code #minecraft:tall_flowers} - sunflower/lilac/rose_bush/peony - and several
     * newer, CE-unrelated blocks like {@code chorus_flower}/{@code mangrove_propagule}). Directly
     * checking real 1.12.2 MCP source ({@code BlockVine}/{@code BlockDeadBush}/
     * {@code BlockDoublePlant} - which backs vanilla's tall grass, large fern, <i>and</i> the tall
     * flowers - all construct with {@code super(Material.vine)}, not {@code Material.plants}) shows
     * CE's real {@code Material.LEAVES}/{@code Material.PLANTS} check never matches any of them;
     * only the plain single-block {@code Blocks.TALLGRASS} (short grass + fern, matched by CE's own
     * explicit {@code b == Blocks.TALLGRASS} identity check) is whitelisted outside the leaves/plants
     * material pair. Narrowed to {@link BlockTags#SMALL_FLOWERS} (single-block flowers only,
     * confirmed real in this exact 1.21.1 build) and the 2 real {@code Blocks.TALLGRASS} successors
     * ({@link Blocks#SHORT_GRASS}/{@link Blocks#FERN}) to match CE exactly - vines/dead bush/double
     * plants (sunflower, lilac, rose bush, peony, large fern, double tallgrass) are no longer
     * destroyed by heavy pollution, matching real CE.
     */
    private static boolean isPollutionSweepTarget(BlockState state) {
        return state.is(BlockTags.LEAVES)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.CROPS)
                || state.is(BlockTags.SMALL_FLOWERS)
                || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.FERN);
    }

    ////////////////////////////////
    /// GLYPHID-ADJACENT METHODS ///
    ////////////////////////////////
    // CE physically contains both of these inside PollutionHandler.java (confirmed: Neo Edition's
    // own pollution port - which also has no Glyphid mobs - dropped both for exactly the reason
    // documented below). Built here per docs/phase4/pollution_system.md's explicit recommendation
    // so the file matches CE's real shape and is ready the moment the Glyphid mob family lands.
    //
    // Neither is wired to a live NeoForge event: this survey could not confirm, against a real
    // 1.21.1 jar (this sandbox has no NeoForge dependency jar cached), the exact package/class for
    // either CE event these methods respond to (Forge's PlayerSleepInBedEvent and
    // WorldEvent.PotentialSpawns) - and rampantScoutPopulator additionally cannot construct
    // EntityGlyphidScout/EntityGlyphidDigger, since neither class exists anywhere in this port yet
    // (confirmed by exhaustive find/grep). Risking either wrong import here would reintroduce a
    // compile break in the exact package built to fix one, for methods with zero live callers
    // today. Both are kept as plain, directly-callable static methods carrying CE's exact gating
    // math, ready to be wired up by whichever future change adds the real event subscription and/or
    // the Glyphid entity classes - see docs/phase4/pollution_system.md's Deferred scope.

    /**
     * CE: {@code PollutionHandler#rampantTargetSetter(PlayerSleepInBedEvent)}. Call with the bed
     * position a player just slept in.
     */
    public static void rampantTargetSetter(BlockPos bedPos) {
        if (MobConfig.effectiveRampantGlyphidGuidance()) {
            targetCoords = new Vec3(bedPos.getX(), bedPos.getY(), bedPos.getZ());
        }
    }

    /**
     * CE: {@code PollutionHandler#rampantScoutPopulator(WorldEvent.PotentialSpawns)}. Ported as a
     * boolean gate rather than a void spawner: returns {@code true} exactly when CE's own logic
     * would have spawned an {@code EntityGlyphidScout}+{@code EntityGlyphidDigger} escort pair at
     * {@code pos} (Rampant Mode's natural-scout-spawn flag, overworld-only, sky-visible, RNG gate,
     * then the local soot threshold) - the caller is responsible for the actual entity construction
     * and CE's further {@code isValidLightLevel()}/{@code getCanSpawnHere()} checks once
     * {@code EntityGlyphidScout}/{@code EntityGlyphidDigger} exist in this port.
     *
     * @param spawnCanceled mirrors CE's {@code event.isCanceled()} check.
     */
    public static boolean rampantScoutPopulator(ServerLevel level, BlockPos pos, boolean spawnCanceled) {
        if (!MobConfig.effectiveRampantNaturalScoutSpawn() || spawnCanceled) return false;
        if (level.dimension() != Level.OVERWORLD) return false;
        if (!level.canSeeSky(pos)) return false;
        if (level.getRandom().nextInt(MobConfig.RAMPANT_SCOUT_SPAWN_CHANCE.get()) != 0) return false;

        float soot = getPollution(level, pos, PollutionType.SOOT);
        return soot >= MobConfig.RAMPANT_SCOUT_SPAWN_THRESH.get();
    }

    //////////////////////
    /// DATA STRUCTURE ///
    //////////////////////

    /**
     * CE: {@code PollutionHandler.PollutionData} - a per-cell {@code float[4]}, one slot per
     * {@link PollutionType} ordinal.
     */
    public static class PollutionData {
        public final float[] pollution = new float[PollutionType.VALUES.length];

        public static PollutionData fromNBT(CompoundTag tag) {
            PollutionData data = new PollutionData();
            for (int i = 0; i < PollutionType.VALUES.length; i++) {
                data.pollution[i] = tag.getFloat(PollutionType.VALUES[i].name().toLowerCase(Locale.US));
            }
            return data;
        }

        public void toNBT(CompoundTag tag) {
            for (int i = 0; i < PollutionType.VALUES.length; i++) {
                tag.putFloat(PollutionType.VALUES[i].name().toLowerCase(Locale.US), pollution[i]);
            }
        }
    }

    /**
     * CE: {@code PollutionHandler.PollutionType} - {@code SOOT}, {@code POISON}, {@code HEAVYMETAL},
     * {@code FALLOUT}, in that exact ordinal order (used as the NBT/array index everywhere - must be
     * preserved). {@link #name} is a translation key (e.g. {@code "trait.ptype.soot"}), not the enum
     * constant's own {@link Enum#name()} - both coexist, exactly as in CE, and both are used by
     * already-committed code ({@code FT_Polluting#addInfoHidden} reads the {@code name} field for
     * tooltips; {@code FT_Polluting#serializeJSON}/{@code deserializeJSON} call the inherited
     * {@code name()} method for JSON keys).
     * <p>
     * <b>{@code FALLOUT} is a real, deliberate CE dead slot</b>: no producer ever increments it and
     * the decay/spread loop never touches it - it always reads 0. Replicated as-is (always-zero, no
     * producer) rather than either wiring it to something new or removing it, per
     * {@code docs/phase4/pollution_system.md}'s explicit recommendation - CE itself never gave it
     * behavior, so inventing behavior here would be adding content the source mod doesn't have.
     */
    public enum PollutionType {
        SOOT("trait.ptype.soot"),
        POISON("trait.ptype.poison"),
        HEAVYMETAL("trait.ptype.heavymetal"),
        FALLOUT("trait.ptype.fallout");

        public static final PollutionType[] VALUES = values();

        public final String name;

        PollutionType(String name) {
            this.name = name;
        }
    }
}
