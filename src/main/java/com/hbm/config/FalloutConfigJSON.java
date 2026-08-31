package com.hbm.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.blocks.generic.BlockSellafield;
import com.hbm.blocks.generic.WastelandVirusBlocks;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Nullable;

/**
 * Ported from CE's {@code com.hbm.config.FalloutConfigJSON} (1107 lines, read in full) -
 * {@code docs/phase4/fallout_rain_and_effects.md} Package A. A JSON-driven block-transform rule
 * table {@link com.hbm.entity.effect.EntityFalloutRain} evaluates per-column against the topmost
 * blocks it scans, plus the config-file read-or-write-default flow CE drives it with.
 * <p>
 * <b>Config plumbing</b>: reused verbatim against the already-real {@code
 * MainRegistry.configHbmDir}/{@code ExplosionNukeGeneric.loadSoliniumFromFile} "write a bespoke
 * config file into the mod's config subfolder, populate a fallback default table if it's missing"
 * pattern - a raw {@link Gson}/{@link JsonWriter} file, not a {@code ModConfigSpec}.
 * <p>
 * <b>Matcher engine, adapted for modern Minecraft</b>: CE's {@code net.minecraft.block.material.
 * Material} enum (the backbone of CE's {@code matchesMaterial}/generic-catch-all matching) was
 * removed entirely in modern Minecraft - there is no 1.21 equivalent class. {@link MaterialProxy}
 * below re-implements the handful of CE material buckets fallout's own default table actually
 * needs (wood/leaves/plants/vine/iron/rock/sand/ground/grass) as small {@link BlockState} predicates
 * using the closest real modern proxy for each (vanilla {@link BlockTags} where a precise tag
 * exists - {@code #logs}/{@code #planks}/{@code #leaves}/{@code #sand} - {@link SoundType} for the
 * rest, and {@code instanceof} checks against the real vanilla classes closest to CE's intent for
 * plants/vines, {@link BushBlock}/{@link VineBlock}). This is a faithful-in-spirit reconstruction of
 * CE's real distance-banded rule set, not a numbers change - every {@code minDist}/{@code maxDist}/
 * {@code chance} value below is copied from CE's real {@code initDefault()} unmodified. CE's generic
 * {@code preserveState}/{@code String[]} property-copy machinery is narrowed to the one property the
 * default table actually preserves ({@link RotatedPillarBlock#AXIS}, for wood-to-{@code waste_log}
 * petrification) - a user JSON config can still request it by name via {@code "preserveState":
 * ["axis"]}, just not an arbitrary property name, since no default entry needs more than this one.
 * CE's {@code matchesOreDict} (Forge {@code OreDictionary}, removed in NeoForge) is re-expressed as
 * an item tag match ({@code "matchesOreDict": ["c:some_tag"]}) - unused by any default entry, kept
 * only for JSON round-trip of a hand-authored config. {@code LookupResult}'s per-state memoization
 * cache is dropped entirely per the research report's own note that it is "portable as-is or dropped
 * entirely in the Package B single-threaded MVP" - the underlying {@link BlockState} still resolves
 * identically either way, just recomputed per call instead of cached.
 * <p>
 * <b>The 3 sellafield/bedrock guard clauses</b> (CE {@code FalloutEntry.eval():905-924}), adapted to
 * this port's real registered blocks: CE's {@code sellafield_slaked} (a separately-tiered, 10-meta
 * intermediate block in CE) and {@code sellafield} (CE's own 6-meta {@code BlockSellafield}) both
 * exist in this port under different roles than CE's naming suggests - see
 * {@link com.hbm.blocks.generic.WastelandVirusBlocks}'s own javadoc: this port's {@code sellafield}
 * (real, {@link BlockSellafield}, {@code LEVEL} 0-5, decays downward over real time) is the live
 * analogue of CE's tiered {@code sellafield_slaked}, and this port's {@code sellafield_slaked} (a
 * flat, stateless {@code BlockBase}) is the terminal state CE's own chain eventually decays into.
 * Guard 1 (never let {@code sellafield_slaked} regress) and guard 2 (never let {@code sellafield}'s
 * {@code LEVEL} regress) are both implemented against these real blocks in {@link
 * FalloutEntry#eval}. Guard 3 (force {@code y == 0} to always become {@code sellafield_bedrock}) is
 * <b>not</b> ported - {@code sellafield_bedrock} does not exist anywhere in this port yet (verified:
 * 0 hits for the name anywhere in {@code src/}) - flagged as a real, disclosed content gap rather
 * than substituted with an unrelated block.
 * <p>
 * <b>Default entries</b>: ports CE's real {@code initDefault()} table (39 entries) in CE's own
 * insertion order (first-match-wins list, order-sensitive - see the wood/leaves distance-banding
 * discussion in the research report), skipping only the entries whose <i>output</i> block is
 * genuinely absent from this port today (five distinct {@code ore_sellafield_*} ore-decoration
 * tiers, {@code sellafield_bedrock}, and {@code glyphid_spawner}'s irradiated variant) - each skip is
 * called out at its exact former call site below rather than silently dropped. Every other entry's
 * target block was independently re-checked by name against this port's real registries (not
 * guessed) before being wired in.
 */
public final class FalloutConfigJSON {

    public static final List<FalloutEntry> ENTRIES = new ArrayList<>();
    private static final Gson GSON = new Gson();
    private static final Map<String, MaterialProxy> MATERIAL_BY_KEY = new HashMap<>();

    static {
        for (MaterialProxy proxy : MaterialProxy.values()) MATERIAL_BY_KEY.put(proxy.key, proxy);
    }

    private static volatile boolean initialized = false;

    private FalloutConfigJSON() {
    }

    /** Idempotent - safe to call more than once (only the first call does any work). */
    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;

        File folder = MainRegistry.configHbmDir;
        File config = new File(folder, "hbmFallout.json");
        File template = new File(folder, "_hbmFallout.json");

        initDefault();

        if (!config.exists()) {
            writeDefault(template);
        } else {
            List<FalloutEntry> loaded = readConfig(config);
            if (loaded != null) {
                ENTRIES.clear();
                ENTRIES.addAll(loaded);
            }
        }
    }

    // ==================== default table ====================

    @Nullable
    private static Block resolveOurs(String path) {
        Block block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path)).orElse(null);
        if (block == null) {
            MainRegistry.logger.warn("FalloutConfigJSON: default entry target '{}' is not registered in this port (yet) - entry skipped.", path);
        }
        return block;
    }

    @SuppressWarnings("ObjectAllocationInLoop")
    private static void initDefault() {
        ENTRIES.clear();
        double woodRange = 65D;

        // Any vanilla log -> waste_log, axis preserved (CE's separate LOG/LOG2 entries collapse into
        // one #logs tag match - both always produced the identical output, the split was purely a
        // 1.12 16-metadata-id artifact, not a behavioral distinction).
        Block wasteLog = resolveOurs("waste_log");
        if (wasteLog != null) {
            add(FalloutEntry.builder().matchTag(BlockTags.LOGS).preserveAxis(true)
                    .addPrimary(wasteLog.defaultBlockState(), 1).max(woodRange).solid(true));

            // CE's meta-10 "all sides stem" huge-mushroom-cap special case is approximated with the
            // real dedicated vanilla stem block (the closest 1.21 analogue of "looks like a stem"),
            // forcing a Y axis exactly like CE's own withProperty(AXIS, Axis.Y).
            add(FalloutEntry.builder().matchBlock(Blocks.MUSHROOM_STEM).forceAxisY(true)
                    .addPrimary(wasteLog.defaultBlockState(), 1).max(woodRange).solid(true));
        }

        add(FalloutEntry.builder().matchBlock(Blocks.RED_MUSHROOM_BLOCK).addPrimary(Blocks.AIR.defaultBlockState(), 1).max(woodRange));
        add(FalloutEntry.builder().matchBlock(Blocks.BROWN_MUSHROOM_BLOCK).addPrimary(Blocks.AIR.defaultBlockState(), 1).max(woodRange));
        add(FalloutEntry.builder().matchBlock(Blocks.SNOW).addPrimary(Blocks.AIR.defaultBlockState(), 1).max(woodRange));

        Block wastePlanks = resolveOurs("waste_planks");
        if (wastePlanks != null) {
            add(FalloutEntry.builder().matchTag(BlockTags.PLANKS).addPrimary(wastePlanks.defaultBlockState(), 1).max(woodRange).solid(true));
        }

        // Catch-all: whatever wood/leaf/plant/vine-like material wasn't already petrified/harvested
        // above simply burns away. Evaluated in this exact order (wood, leaves, plants, vine)
        // matching CE's own insertion order - order matters for the leaves distance-banding below.
        add(FalloutEntry.builder().matchMaterial(MaterialProxy.WOOD).addPrimary(Blocks.AIR.defaultBlockState(), 1).max(woodRange));
        add(FalloutEntry.builder().matchMaterial(MaterialProxy.LEAVES).addPrimary(Blocks.AIR.defaultBlockState(), 1).max(woodRange));
        add(FalloutEntry.builder().matchMaterial(MaterialProxy.PLANTS).addPrimary(Blocks.AIR.defaultBlockState(), 1).max(woodRange));
        add(FalloutEntry.builder().matchMaterial(MaterialProxy.VINE).addPrimary(Blocks.AIR.defaultBlockState(), 1).max(woodRange));

        Block wasteLeaves = resolveOurs("waste_leaves");
        if (wasteLeaves != null) {
            // Already-fossilized leaves this close to ground zero burn away too.
            add(FalloutEntry.builder().matchBlock(wasteLeaves).addPrimary(Blocks.AIR.defaultBlockState(), 1).max(woodRange));
            // Further out (60-100%), leaves fossilize into waste_leaves instead of burning - the
            // MaterialProxy.LEAVES catch-all above (max 65) and this entry (min 60) share the exact
            // same real predicate (BlockTags.LEAVES); CE's own Blocks.LEAVES/LEAVES2 split collapses
            // the same way the LOG/LOG2 split did.
            add(FalloutEntry.builder().matchMaterial(MaterialProxy.LEAVES).addPrimary(wasteLeaves.defaultBlockState(), 1).min(woodRange - 5D));
        }

        add(FalloutEntry.builder().matchBlock(Blocks.MOSSY_COBBLESTONE).addPrimary(Blocks.COAL_ORE.defaultBlockState(), 1).solid(true));

        Block oreNetherUranium = resolveOurs("ore_nether_uranium");
        Block oreNetherSchrabidium = resolveOurs("ore_nether_schrabidium");
        Block oreNetherUraniumScorched = resolveOurs("ore_nether_uranium_scorched");
        if (oreNetherUranium != null && oreNetherSchrabidium != null && oreNetherUraniumScorched != null) {
            add(FalloutEntry.builder().matchBlock(oreNetherUranium)
                    .addPrimary(oreNetherSchrabidium.defaultBlockState(), 1).addPrimary(oreNetherUraniumScorched.defaultBlockState(), 99)
                    .solid(true));
        }

        Block glyphidBase = resolveOurs("glyphid_base");
        Block glyphidBaseRad = resolveOurs("glyphid_base_rad");
        if (glyphidBase != null && glyphidBaseRad != null) {
            add(FalloutEntry.builder().matchBlock(glyphidBase).addPrimary(glyphidBaseRad.defaultBlockState(), 1).solid(true));
        }
        // glyphid_spawner (and its irradiated variant) is not registered anywhere in this port -
        // CE's corresponding entry is skipped rather than invented (see class javadoc).

        // CE's tiered "material -> decayed sellafield ore" loop (i=1..10, m=10-i). This port's real
        // decaying-ore analogue (com.hbm.blocks.generic.BlockSellafield, registry name "sellafield")
        // only has 6 discrete LEVEL states (0-5) rather than CE's 10-meta sellafield_slaked, so CE's
        // m in [0,9] is linearly rescaled onto this port's [0,5] - see class javadoc. CE's
        // interleaved coal_ore/lignite/beryllium/uranium/diamond/bedrock branches in this same loop
        // all target ore_sellafield_* tiers or sellafield_bedrock, none of which exist in this port
        // yet - skipped (see class javadoc), leaving only the 5 material-based branches per tier.
        Block sellafieldTiered = WastelandVirusBlocks.SELLAFIELD.get();
        for (int i = 1; i <= 10; i++) {
            int m = 10 - i;
            int level = Math.round(m * 5.0F / 9.0F);
            double maxDist = i * 5.0;
            BlockState out = sellafieldTiered.defaultBlockState().setValue(BlockSellafield.LEVEL, level);

            add(FalloutEntry.builder().matchMaterial(MaterialProxy.IRON).addPrimary(out, 1).max(maxDist).opaque(true).solid(true));
            add(FalloutEntry.builder().matchMaterial(MaterialProxy.ROCK).addPrimary(out, 1).max(maxDist).opaque(true).solid(true));
            add(FalloutEntry.builder().matchMaterial(MaterialProxy.SAND).addPrimary(out, 1).max(maxDist).opaque(true).solid(true));
            add(FalloutEntry.builder().matchMaterial(MaterialProxy.GROUND).addPrimary(out, 1).max(maxDist).opaque(true).solid(true));
            if (i <= 9) {
                add(FalloutEntry.builder().matchMaterial(MaterialProxy.GRASS).addPrimary(out, 1).max(maxDist).opaque(true).solid(true));
            }
        }

        Block wasteMycelium = resolveOurs("waste_mycelium");
        if (wasteMycelium != null) {
            add(FalloutEntry.builder().matchBlock(Blocks.MYCELIUM).addPrimary(wasteMycelium.defaultBlockState(), 1).solid(true));
        }

        Block wasteTrinitite = resolveOurs("waste_trinitite");
        if (wasteTrinitite != null) {
            add(FalloutEntry.builder().matchBlock(Blocks.SAND).addPrimary(wasteTrinitite.defaultBlockState(), 1).primaryChance(0.05).solid(true));
        }
        Block wasteTrinititeRed = resolveOurs("waste_trinitite_red");
        if (wasteTrinititeRed != null) {
            add(FalloutEntry.builder().matchBlock(Blocks.RED_SAND).addPrimary(wasteTrinititeRed.defaultBlockState(), 1).primaryChance(0.05).solid(true));
        }

        // CE: Blocks.CLAY -> Blocks.HARDENED_CLAY (renamed Blocks.TERRACOTTA in modern Minecraft).
        add(FalloutEntry.builder().matchBlock(Blocks.CLAY).addPrimary(Blocks.TERRACOTTA.defaultBlockState(), 1).solid(true));
    }

    private static void add(FalloutEntry.Builder builder) {
        ENTRIES.add(builder.build());
    }

    // ==================== file I/O ====================

    private static void writeDefault(File file) {
        try {
            File parent = file.getParentFile();
            if (parent != null) //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
        } catch (Exception ignored) {
            // best-effort; JsonWriter below will surface any real failure
        }
        try (JsonWriter writer = new JsonWriter(new FileWriter(file))) {
            writer.setIndent("  ");
            writer.beginObject();
            writer.name("entries").beginArray();
            for (FalloutEntry entry : ENTRIES) {
                writer.beginObject();
                entry.write(writer);
                writer.endObject();
            }
            writer.endArray();
            writer.endObject();
        } catch (IOException e) {
            MainRegistry.logger.catching(e);
        }
    }

    @Nullable
    private static List<FalloutEntry> readConfig(File config) {
        try (FileReader reader = new FileReader(config)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null || !json.has("entries")) return null;
            JsonArray array = json.getAsJsonArray("entries");
            List<FalloutEntry> loaded = new ArrayList<>(array.size());
            for (JsonElement element : array) {
                FalloutEntry entry = FalloutEntry.read(element);
                if (entry != null) loaded.add(entry);
            }
            return loaded;
        } catch (Exception e) {
            MainRegistry.logger.catching(e);
        }
        return null;
    }

    // ==================== material proxy (CE Material -> modern BlockState predicate) ====================

    public enum MaterialProxy {
        WOOD("wood", state -> state.getSoundType() == SoundType.WOOD),
        LEAVES("leaves", state -> state.is(BlockTags.LEAVES)),
        PLANTS("plants", state -> state.getBlock() instanceof BushBlock),
        VINE("vine", state -> state.getBlock() instanceof VineBlock),
        IRON("iron", state -> state.getSoundType() == SoundType.METAL),
        ROCK("rock", state -> state.getSoundType() == SoundType.STONE),
        SAND("sand", state -> state.is(BlockTags.SAND)),
        GROUND("ground", FalloutConfigJSON::isGroundLike),
        GRASS("grass", state -> state.is(Blocks.GRASS_BLOCK));

        public final String key;
        public final Predicate<BlockState> test;

        MaterialProxy(String key, Predicate<BlockState> test) {
            this.key = key;
            this.test = test;
        }

        @Nullable
        static MaterialProxy byKey(String key) {
            return MATERIAL_BY_KEY.get(key);
        }
    }

    private static boolean isGroundLike(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.DIRT || block == Blocks.COARSE_DIRT || block == Blocks.FARMLAND
                || block == Blocks.PODZOL || block == Blocks.MYCELIUM || block == Blocks.ROOTED_DIRT;
    }

    // ==================== entry ====================

    public static final class FalloutEntry {

        @Nullable private Block matchBlock;
        @Nullable private TagKey<Block> matchTag;
        @Nullable private MaterialProxy matchMaterial;
        @Nullable private TagKey<Item> matchItemTag;
        private boolean requireOpaque;
        private boolean preserveAxis;
        private boolean forceAxisY;
        private boolean solid;

        private List<WeightedOutput> primary = Collections.emptyList();
        private List<WeightedOutput> secondary = Collections.emptyList();
        private double primaryChance = 1.0D;
        private double minDist;
        private double maxDist = 100.0D;
        private double falloffStart = 0.9D;

        private FalloutEntry() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public boolean isSolid() {
            return solid;
        }

        /**
         * CE {@code FalloutEntry.eval()} (matcher + weighted-random output + Gaussian falloff),
         * ported line-for-line onto this port's real block set - see class javadoc for what changed
         * and why.
         */
        @Nullable
        public BlockState eval(int yGlobal, BlockState state, double distPercent, RandomSource random) {
            if (distPercent > maxDist || distPercent < minDist) return null;

            Block block = state.getBlock();
            if (matchBlock != null && block != matchBlock) return null;
            if (matchTag != null && !state.is(matchTag)) return null;
            if (matchMaterial != null && !matchMaterial.test.test(state)) return null;
            if (requireOpaque && !state.canOcclude()) return null;
            if (matchItemTag != null) {
                Item item = block.asItem();
                if (item == net.minecraft.world.item.Items.AIR || !new ItemStack(item).is(matchItemTag)) return null;
            }

            if (distPercent > maxDist * falloffStart) {
                double t = (distPercent - maxDist * falloffStart) / (maxDist - maxDist * falloffStart);
                if (Math.abs(random.nextGaussian()) < t * t * 3.0) return null;
            }

            List<WeightedOutput> pool = (primaryChance >= 1.0D || random.nextDouble() < primaryChance) ? primary : secondary;
            BlockState conversion = chooseWeighted(pool, random);
            if (conversion == null) return null;

            Block sellafieldSlaked = WastelandVirusBlocks.SELLAFIELD_SLAKED.get();
            Block sellafieldTiered = WastelandVirusBlocks.SELLAFIELD.get();
            Block conversionBlock = conversion.getBlock();

            // Guard 1: this port's terminal "fully decayed" block never regresses into anything else.
            if (block == sellafieldSlaked && conversionBlock != sellafieldSlaked) return null;

            // Guard 2: never let a tiered sellafield conversion downgrade an already-more-decayed cell.
            if (conversionBlock == sellafieldTiered && block == sellafieldTiered) {
                int oldLevel = state.getValue(BlockSellafield.LEVEL);
                int newLevel = conversion.getValue(BlockSellafield.LEVEL);
                if (newLevel <= oldLevel) return null;
            }

            // Guard 3 (CE: y==0 always forces sellafield_bedrock) is not portable - that block does
            // not exist in this port yet (see class javadoc); falls through to the ordinary result.

            if (preserveAxis && state.hasProperty(RotatedPillarBlock.AXIS) && conversion.hasProperty(RotatedPillarBlock.AXIS)) {
                conversion = conversion.setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS));
            } else if (forceAxisY && conversion.hasProperty(RotatedPillarBlock.AXIS)) {
                conversion = conversion.setValue(RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.Y);
            }

            return conversion;
        }

        @Nullable
        private static BlockState chooseWeighted(List<WeightedOutput> pool, RandomSource random) {
            if (pool == null || pool.isEmpty()) return null;
            int totalWeight = 0;
            for (WeightedOutput w : pool) totalWeight += w.weight();
            if (totalWeight <= 0) return null;

            int r = random.nextInt(totalWeight);
            for (WeightedOutput w : pool) {
                r -= w.weight();
                if (r < 0) return w.state();
            }
            return pool.get(0).state();
        }

        // -------------------- JSON round trip --------------------

        void write(JsonWriter writer) throws IOException {
            if (matchBlock != null) writer.name("matchesBlock").value(BuiltInRegistries.BLOCK.getKey(matchBlock).toString());
            if (matchTag != null) writer.name("matchesTag").value(matchTag.location().toString());
            if (matchMaterial != null) writer.name("matchesMaterial").value(matchMaterial.key);
            if (matchItemTag != null) writer.name("matchesOreDict").value(matchItemTag.location().toString());
            if (requireOpaque) writer.name("mustBeOpaque").value(true);
            if (preserveAxis) {
                writer.name("preserveState").beginArray().value("axis").endArray();
            }
            if (forceAxisY) writer.name("forceAxisY").value(true);
            if (solid) writer.name("restrictDepth").value(true);

            if (!primary.isEmpty()) {
                writer.name("primarySubstitution");
                writeStateArray(writer, primary);
            }
            if (!secondary.isEmpty()) {
                writer.name("secondarySubstitutions");
                writeStateArray(writer, secondary);
            }
            if (primaryChance != 1.0D) writer.name("chance").value(primaryChance);
            if (minDist != 0.0D) writer.name("minimumDistancePercent").value(minDist);
            if (maxDist != 100.0D) writer.name("maximumDistancePercent").value(maxDist);
            if (falloffStart != 0.9D) writer.name("falloffStartFactor").value(falloffStart);
        }

        private static void writeStateArray(JsonWriter writer, List<WeightedOutput> list) throws IOException {
            writer.beginArray();
            for (WeightedOutput w : list) {
                writer.beginArray();
                writer.value(stateToString(w.state()));
                writer.value(w.weight());
                writer.endArray();
            }
            writer.endArray();
        }

        /**
         * Serializes a full {@link BlockState} (including any non-default property values, e.g.
         * {@code sellafield}'s {@code LEVEL}) via the same real, already-proven-in-this-port
         * {@link net.minecraft.nbt.NbtUtils#writeBlockState} codec {@code PWRProxyBlockEntity}
         * already round-trips block states through - far more robust than hand-parsing CE's own
         * bracket notation for an arbitrary, possibly-multi-property state.
         */
        private static String stateToString(BlockState state) {
            return net.minecraft.nbt.NbtUtils.writeBlockState(state).toString();
        }

        @Nullable
        static FalloutEntry read(JsonElement element) {
            if (!element.isJsonObject()) return null;
            JsonObject obj = element.getAsJsonObject();
            Builder b = builder();

            if (obj.has("matchesBlock")) {
                Block block = parseBlockByName(obj.get("matchesBlock").getAsString());
                if (block != null) b.matchBlock(block);
            }
            if (obj.has("matchesTag")) {
                b.matchTag(TagKey.create(net.minecraft.core.registries.Registries.BLOCK, ResourceLocation.parse(obj.get("matchesTag").getAsString())));
            }
            if (obj.has("matchesMaterial")) {
                MaterialProxy proxy = MaterialProxy.byKey(obj.get("matchesMaterial").getAsString());
                if (proxy != null) b.matchMaterial(proxy);
            }
            if (obj.has("matchesOreDict")) {
                JsonElement oreDict = obj.get("matchesOreDict");
                String tagName = oreDict.isJsonArray() ? oreDict.getAsJsonArray().get(0).getAsString() : oreDict.getAsString();
                b.matchItemTag(TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.parse(tagName)));
            }
            if (obj.has("mustBeOpaque")) b.opaque(obj.get("mustBeOpaque").getAsBoolean());
            if (obj.has("preserveState")) b.preserveAxis(true);
            if (obj.has("forceAxisY")) b.forceAxisY(obj.get("forceAxisY").getAsBoolean());
            if (obj.has("restrictDepth")) b.solid(obj.get("restrictDepth").getAsBoolean());
            if (obj.has("primarySubstitution")) readStateArray(obj.get("primarySubstitution"), b, true);
            if (obj.has("secondarySubstitutions")) readStateArray(obj.get("secondarySubstitutions"), b, false);
            if (obj.has("chance")) b.primaryChance(obj.get("chance").getAsDouble());
            if (obj.has("minimumDistancePercent")) b.min(obj.get("minimumDistancePercent").getAsDouble());
            if (obj.has("maximumDistancePercent")) b.max(obj.get("maximumDistancePercent").getAsDouble());
            if (obj.has("falloffStartFactor")) b.falloff(obj.get("falloffStartFactor").getAsDouble());

            return b.build();
        }

        private static void readStateArray(JsonElement element, Builder b, boolean primaryPool) {
            if (!element.isJsonArray()) return;
            for (JsonElement entryElement : element.getAsJsonArray()) {
                if (!entryElement.isJsonArray()) continue;
                JsonArray pair = entryElement.getAsJsonArray();
                BlockState state = parseBlockState(pair.get(0).getAsString());
                if (state == null) continue;
                int weight = pair.get(1).getAsInt();
                if (primaryPool) b.addPrimary(state, weight);
                else b.addSecondary(state, weight);
            }
        }

        /** Plain "namespace:path" lookup - used for {@code matchesBlock}, which never carries property state. */
        @Nullable
        private static Block parseBlockByName(String input) {
            Block block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(input)).orElse(null);
            if (block == null) {
                MainRegistry.logger.warn("FalloutConfigJSON: config references unknown block '{}' - entry skipped.", input);
            }
            return block;
        }

        /**
         * Full {@link BlockState} (including any property values, e.g. {@code sellafield}'s
         * {@code LEVEL}) via the same {@link net.minecraft.nbt.NbtUtils#readBlockState}/{@link
         * net.minecraft.nbt.TagParser} pair {@code PWRProxyBlockEntity} already round-trips block
         * states through elsewhere in this port - see {@link #stateToString} for the writer half.
         */
        @Nullable
        private static BlockState parseBlockState(String input) {
            try {
                net.minecraft.nbt.Tag tag = net.minecraft.nbt.TagParser.parseTag(input);
                if (!(tag instanceof net.minecraft.nbt.CompoundTag compound)) return null;
                return net.minecraft.nbt.NbtUtils.readBlockState(BuiltInRegistries.BLOCK, compound);
            } catch (Exception e) {
                MainRegistry.logger.warn("FalloutConfigJSON: could not parse block state '{}': {}", input, e.toString());
                return null;
            }
        }

        public static final class Builder {
            private final FalloutEntry entry = new FalloutEntry();
            private final List<WeightedOutput> primary = new ArrayList<>();
            private final List<WeightedOutput> secondary = new ArrayList<>();

            public Builder matchBlock(Block block) {
                entry.matchBlock = block;
                return this;
            }

            public Builder matchTag(@Nullable TagKey<Block> tag) {
                entry.matchTag = tag;
                return this;
            }

            public Builder matchMaterial(MaterialProxy proxy) {
                entry.matchMaterial = proxy;
                return this;
            }

            public Builder matchItemTag(TagKey<Item> tag) {
                entry.matchItemTag = tag;
                return this;
            }

            public Builder opaque(boolean opaque) {
                entry.requireOpaque = opaque;
                return this;
            }

            public Builder preserveAxis(boolean preserve) {
                entry.preserveAxis = preserve;
                return this;
            }

            public Builder forceAxisY(boolean force) {
                entry.forceAxisY = force;
                return this;
            }

            public Builder solid(boolean solid) {
                entry.solid = solid;
                return this;
            }

            public Builder addPrimary(BlockState state, int weight) {
                primary.add(new WeightedOutput(state, weight));
                return this;
            }

            public Builder addSecondary(BlockState state, int weight) {
                secondary.add(new WeightedOutput(state, weight));
                return this;
            }

            public Builder primaryChance(double chance) {
                entry.primaryChance = chance;
                return this;
            }

            public Builder min(double min) {
                entry.minDist = min;
                return this;
            }

            public Builder max(double max) {
                entry.maxDist = max;
                return this;
            }

            public Builder falloff(double falloffStart) {
                entry.falloffStart = falloffStart;
                return this;
            }

            public FalloutEntry build() {
                entry.primary = primary.isEmpty() ? Collections.emptyList() : new ArrayList<>(primary);
                entry.secondary = secondary.isEmpty() ? Collections.emptyList() : new ArrayList<>(secondary);
                return entry;
            }
        }
    }

    private record WeightedOutput(BlockState state, int weight) {
    }
}
