package com.hbm.world.gen;

import com.hbm.config.CompatibilityConfig;
import com.hbm.config.GeneralConfig;
import com.hbm.config.WorldConfig;
import com.hbm.main.MainRegistry;
import com.hbm.world.feature.AustraliumTreasureFeature;
import com.hbm.world.feature.BedrockOreFeature;
import com.hbm.world.feature.CaveOreFeature;
import com.hbm.world.feature.ChanceGatedDepositFeature;
import com.hbm.world.feature.ColtanDepositFeature;
import com.hbm.world.feature.DepthDepositFeature;
import com.hbm.world.feature.EllipsoidOreFeature;
import com.hbm.world.feature.GneissStratumFeature;
import com.hbm.world.feature.NoiseLayerOreFeature;
import com.hbm.world.feature.OreShapeUtil;
import com.hbm.world.feature.SmolderingOreFeature;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * {@link DeferredRegister}{@code <Feature<?>>} for docs/phase4/ore_veins_and_bedrock_ores.md's
 * entire ordinary-ore-vein + bedrock-ore world-gen roster - Groups A through E plus the bedrock-ore
 * y=0 mechanic. One {@link Feature} instance per real CE placement call site (~61 total), each a
 * thin wrapper around one of this package's sibling shape-family classes
 * ({@link EllipsoidOreFeature}/{@link ChanceGatedDepositFeature}/{@link DepthDepositFeature}/
 * {@link GneissStratumFeature}/{@link NoiseLayerOreFeature}/{@link CaveOreFeature}/
 * {@link SmolderingOreFeature}/{@link ColtanDepositFeature}/{@link AustraliumTreasureFeature}/
 * {@link BedrockOreFeature}), following this port's per-family {@code DeferredRegister} template
 * (see {@code com.hbm.entity.effect.EffectEntityTypes}).
 * <p>
 * Every entry is additionally filed into {@link #OVERWORLD}/{@link #NETHER}/{@link #END} (by
 * registry name) so {@link OreConfiguredFeatures}/{@link OrePlacedFeatures}/{@link OreBiomeModifiers}
 * can register the remaining three pipeline stages with one small loop per dimension group instead
 * of ~180 hand-written lines - every entry uses the exact same {@link NoneFeatureConfiguration} and
 * the exact same placement-modifier list (each {@code Feature} ignores the position it's handed and
 * does its own internal live-config-driven RNG rolls, mirroring neo-edition's own confirmed-real
 * {@code OilBubbleFeature.place()} pattern), so nothing distinguishes one registry entry from another
 * at that level besides which biome-tag group it belongs to.
 */
public final class OreWorldGenFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(BuiltInRegistries.FEATURE, MainRegistry.MODID);

    public static final Map<String, DeferredHolder<Feature<?>, ? extends Feature<?>>> OVERWORLD = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Feature<?>, ? extends Feature<?>>> NETHER = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Feature<?>, ? extends Feature<?>>> END = new LinkedHashMap<>();

    private static final Supplier<Block> TARGET_GNEISS = () -> OreShapeUtil.block("stone_gneiss");
    private static final Supplier<Block> TARGET_NETHERRACK = () -> Blocks.NETHERRACK;
    private static final Supplier<Block> TARGET_END_STONE = () -> Blocks.END_STONE;

    private OreWorldGenFeatures() {
    }

    public static void register(IEventBus modEventBus) {
        registerOverworldVeins();
        registerGneiss();
        registerNetherVeins();
        registerEndVeins();
        registerChanceGated();
        registerDepthDeposits();
        registerNoiseLayerAndCaves();
        registerBespoke();
        registerBedrockOre();

        FEATURES.register(modEventBus);
    }

    // ==================== Group A - overworld ellipsoid veins ====================

    private static void registerOverworldVeins() {
        ellipsoid(OVERWORLD, "uranium", forDim(CompatibilityConfig::uraniumSpawn), "ore_uranium", 5, 5, 20, null);
        ellipsoid(OVERWORLD, "thorium", forDim(CompatibilityConfig::thoriumSpawn), "ore_thorium", 5, 5, 25, null);
        ellipsoid(OVERWORLD, "titanium", forDim(CompatibilityConfig::titaniumSpawn), "ore_titanium", 6, 5, 30, null);
        ellipsoid(OVERWORLD, "sulfur", forDim(CompatibilityConfig::sulfurSpawn), "ore_sulfur", 8, 5, 30, null);
        ellipsoid(OVERWORLD, "aluminium", forDim(CompatibilityConfig::aluminiumSpawn), "ore_aluminium", 6, 5, 40, null);
        ellipsoid(OVERWORLD, "copper", forDim(CompatibilityConfig::copperSpawn), "ore_copper", 6, 5, 45, null);
        ellipsoid(OVERWORLD, "fluorite", forDim(CompatibilityConfig::fluoriteSpawn), "ore_fluorite", 4, 5, 45, null);
        ellipsoid(OVERWORLD, "niter", forDim(CompatibilityConfig::niterSpawn), "ore_niter", 6, 5, 30, null);
        ellipsoid(OVERWORLD, "tungsten", forDim(CompatibilityConfig::tungstenSpawn), "ore_tungsten", 8, 5, 30, null);
        ellipsoid(OVERWORLD, "lead", forDim(CompatibilityConfig::leadSpawn), "ore_lead", 9, 5, 30, null);
        ellipsoid(OVERWORLD, "beryllium", forDim(CompatibilityConfig::berylliumSpawn), "ore_beryllium", 4, 5, 30, null);
        ellipsoid(OVERWORLD, "rare", forDim(CompatibilityConfig::rareSpawn), "ore_rare", 5, 5, 20, null);
        ellipsoid(OVERWORLD, "lignite", forDim(CompatibilityConfig::ligniteSpawn), "ore_lignite", 24, 35, 25, null);
        ellipsoid(OVERWORLD, "asbestos", forDim(CompatibilityConfig::asbestosSpawn), "ore_asbestos", 4, 16, 16, null);
        ellipsoid(OVERWORLD, "cinnabar", forDim(CompatibilityConfig::cinnabarSpawn), "ore_cinnabar", 4, 8, 16, null);
        ellipsoid(OVERWORLD, "cobalt", forDim(CompatibilityConfig::cobaltSpawn), "ore_cobalt", 4, 4, 8, null);
        ellipsoid(OVERWORLD, "cluster_iron", forDim(CompatibilityConfig::ironClusterSpawn), "cluster_iron", 6, 15, 45, null);
        ellipsoid(OVERWORLD, "cluster_titanium", forDim(CompatibilityConfig::titaniumClusterSpawn), "cluster_titanium", 6, 15, 30, null);
        ellipsoid(OVERWORLD, "cluster_aluminium", forDim(CompatibilityConfig::aluminiumClusterSpawn), "cluster_aluminium", 6, 15, 35, null);
        ellipsoid(OVERWORLD, "cluster_copper", forDim(CompatibilityConfig::copperClusterSpawn), "cluster_copper", 6, 15, 20, null);
        // CE's dim-0 default is 0 - the real overworld australium source is the fixed treasure zone
        // (registerBespoke's AustraliumTreasureFeature) - kept registered for parity/tunability.
        ellipsoid(OVERWORLD, "australium", forDim(CompatibilityConfig::australiumSpawn), "ore_australium", 3, 14, 18, null);
        ellipsoid(OVERWORLD, "limestone", dim -> dim.equals(Level.OVERWORLD) ? WorldConfig.LIMESTONE_SPAWN.get() : 0,
                "stone_resource_limestone", 6, 15, 20, null);
        // "528 mode" ordinary coltan vein - CE default disabled (enable528ColtanSpawn == false).
        ellipsoid(OVERWORLD, "coltan_vein",
                dim -> dim.equals(Level.OVERWORLD) && GeneralConfig.X528_ENABLE_COLTAN_SPAWNING.get()
                        ? GeneralConfig.X528_ORE_COLTAN_FREQUENCY.get() : 0,
                "ore_coltan", 4, 15, 40, null);
    }

    // ==================== Group C - gneiss stratum + vein pass ====================

    private static void registerGneiss() {
        DeferredHolder<Feature<?>, GneissStratumFeature> stratum =
                FEATURES.register("gneiss_stratum", () -> new GneissStratumFeature(NoneFeatureConfiguration.CODEC));
        OVERWORLD.put("gneiss_stratum", stratum);

        ellipsoid(OVERWORLD, "gneiss_iron", forDim(CompatibilityConfig::gneissIronSpawn), "ore_gneiss_iron", 6, 30, 10, TARGET_GNEISS);
        ellipsoid(OVERWORLD, "gneiss_gold", forDim(CompatibilityConfig::gneissGoldSpawn), "ore_gneiss_gold", 6, 30, 10, TARGET_GNEISS);
        ellipsoid(OVERWORLD, "gneiss_uranium", forDim(CompatibilityConfig::uraniumSpawn, 3), "ore_gneiss_uranium", 6, 30, 10, TARGET_GNEISS);
        ellipsoid(OVERWORLD, "gneiss_copper", forDim(CompatibilityConfig::copperSpawn, 3), "ore_gneiss_copper", 6, 30, 10, TARGET_GNEISS);
        ellipsoid(OVERWORLD, "gneiss_asbestos_x3", forDim(CompatibilityConfig::asbestosSpawn, 3), "ore_gneiss_asbestos", 6, 30, 10, TARGET_GNEISS);
        ellipsoid(OVERWORLD, "gneiss_lithium", forDim(CompatibilityConfig::lithiumSpawn), "ore_gneiss_lithium", 6, 30, 10, TARGET_GNEISS);
        // Confirmed CE quirk (HbmWorldGen.java line 175): the 8th gneiss call targets
        // ore_gneiss_asbestos a SECOND time, gated by the rare-earth-ore rate rather than a
        // dedicated field - preserved verbatim rather than "fixed", per the research report's Open
        // questions. ore_gneiss_rare itself never gets a placement call anywhere in CE.
        ellipsoid(OVERWORLD, "gneiss_asbestos_rare_quirk", forDim(CompatibilityConfig::rareSpawn), "ore_gneiss_asbestos", 6, 30, 10, TARGET_GNEISS);
        ellipsoid(OVERWORLD, "gneiss_gas", forDim(CompatibilityConfig::gassshaleSpawn, 3), "ore_gneiss_gas", 10, 30, 10, TARGET_GNEISS);
    }

    // ==================== Group A - nether ellipsoid veins ====================

    private static void registerNetherVeins() {
        ellipsoid(NETHER, "nether_uranium", forDim(CompatibilityConfig::netherUraniumSpawn), "ore_nether_uranium", 6, 0, 127, TARGET_NETHERRACK);
        ellipsoid(NETHER, "nether_tungsten", forDim(CompatibilityConfig::netherTungstenSpawn), "ore_nether_tungsten", 10, 0, 127, TARGET_NETHERRACK);
        ellipsoid(NETHER, "nether_sulfur", forDim(CompatibilityConfig::netherSulfurSpawn), "ore_nether_sulfur", 12, 0, 127, TARGET_NETHERRACK);
        ellipsoid(NETHER, "nether_fire", forDim(CompatibilityConfig::netherPhosphorusSpawn), "ore_nether_fire", 6, 0, 127, TARGET_NETHERRACK);
        ellipsoid(NETHER, "nether_coal", forDim(CompatibilityConfig::netherCoalSpawn), "ore_nether_coal", 32, 16, 96, TARGET_NETHERRACK);
        ellipsoid(NETHER, "nether_cobalt", forDim(CompatibilityConfig::netherCobaltSpawn), "ore_nether_cobalt", 6, 100, 26, TARGET_NETHERRACK);
        ellipsoid(NETHER, "nether_plutonium",
                dim -> dim.equals(Level.NETHER) && GeneralConfig.ENABLE_PLUTONIUM_NETHER_ORE.get()
                        ? CompatibilityConfig.forDimension(CompatibilityConfig.netherPlutoniumSpawn(), dim) : 0,
                "ore_nether_plutonium", 4, 0, 127, TARGET_NETHERRACK);
    }

    // ==================== Group A - end ellipsoid vein ====================

    private static void registerEndVeins() {
        ellipsoid(END, "end_tikite", forDim(CompatibilityConfig::endTixiteSpawn), "ore_tikite", 6, 0, 127, TARGET_END_STONE);
    }

    // ==================== Group B - chance-gated deposits (oil/bedrock-oil owned elsewhere) ====================

    private static void registerChanceGated() {
        chanceGated(OVERWORLD, "gas_flammable",
                dim -> GeneralConfig.ENABLE_FLAMMABLE_GAS.get() ? CompatibilityConfig.forDimension(CompatibilityConfig.gasbubbleSpawn(), dim) : 0,
                "gas_flammable", 32, 30, 10, null);
        chanceGated(OVERWORLD, "gas_explosive",
                dim -> GeneralConfig.ENABLE_EXPLOSIVE_GAS.get() ? CompatibilityConfig.forDimension(CompatibilityConfig.explosivebubbleSpawn(), dim) : 0,
                "gas_explosive", 32, 30, 10, null);
        chanceGated(OVERWORLD, "alexandrite", forDim(CompatibilityConfig::alexandriteSpawn), "ore_alexandrite", 3, 10, 5, null);
    }

    // ==================== Group D - depth-ore sphere-blob family ====================

    private static void registerDepthDeposits() {
        depth(OVERWORLD, "depth_cluster_iron", new int[]{0}, 3, 24, 5, 0.6, "cluster_depth_iron", false, "stone_depth");
        depth(OVERWORLD, "depth_cluster_titanium", new int[]{0}, 3, 32, 5, 0.6, "cluster_depth_titanium", false, "stone_depth");
        depth(OVERWORLD, "depth_cluster_tungsten", new int[]{0}, 3, 32, 5, 0.6, "cluster_depth_tungsten", false, "stone_depth");
        depth(OVERWORLD, "depth_cinnabar", new int[]{0}, 3, 16, 5, 0.8, "ore_depth_cinnabar", false, "stone_depth");
        depth(OVERWORLD, "depth_zirconium", new int[]{0}, 3, 16, 5, 0.8, "ore_depth_zirconium", false, "stone_depth");
        depth(OVERWORLD, "depth_borax", new int[]{0}, 3, 16, 5, 0.8, "ore_depth_borax", false, "stone_depth");

        depth(NETHER, "depth_nether_neodymium", new int[]{0, 125}, 3, 16, 7, 0.6, "ore_depth_nether_neodymium", true, "stone_depth_nether");
        depth(NETHER, "depth_nether_nitan", new int[]{0, 125}, 3, 16, 7, 0.6, "ore_depth_nether_nitan", true, "stone_depth_nether");
    }

    // ==================== Group E - noise-layer BlockResourceStone family ====================

    private static void registerNoiseLayerAndCaves() {
        cave(OVERWORLD, "cave_sulfur", 0, 1.5, 20, 20, 30, "stone_resource_sulfur", "sulfur", WorldConfig.ENABLE_SULFUR_CAVE::get);
        cave(OVERWORLD, "cave_asbestos", 1, 1.75, 20, 20, 25, "stone_resource_asbestos", "asbestos", WorldConfig.ENABLE_ASBESTOS_CAVE::get);

        layer(OVERWORLD, "layer_hematite", 0, 0.04, 0.25, 230, "stone_resource_hematite", WorldConfig.ENABLE_HEMATITE::get);
        layer(OVERWORLD, "layer_bauxite", 1, 0.03, 0.15, 300, "stone_resource_bauxite", WorldConfig.ENABLE_BAUXITE::get);
        layer(OVERWORLD, "layer_malachite", 2, 0.1, 0.15, 275, "stone_resource_malachite", WorldConfig.ENABLE_MALACHITE::get);
    }

    // ==================== Group B - bespoke (non-ellipsoid) deposits ====================

    private static void registerBespoke() {
        DeferredHolder<Feature<?>, SmolderingOreFeature> smoldering =
                FEATURES.register("smoldering_ore", () -> new SmolderingOreFeature(NoneFeatureConfiguration.CODEC));
        NETHER.put("smoldering_ore", smoldering);

        DeferredHolder<Feature<?>, ColtanDepositFeature> coltanDeposit = FEATURES.register("coltan_deposit",
                () -> new ColtanDepositFeature(NoneFeatureConfiguration.CODEC, GeneralConfig.X528_ENABLE_COLTAN_DEPOSIT::get));
        OVERWORLD.put("coltan_deposit", coltanDeposit);

        DeferredHolder<Feature<?>, AustraliumTreasureFeature> australiumTreasure =
                FEATURES.register("australium_treasure", () -> new AustraliumTreasureFeature(NoneFeatureConfiguration.CODEC));
        OVERWORLD.put("australium_treasure", australiumTreasure);
    }

    // ==================== bedrock-ore y=0 mechanic ====================

    private static void registerBedrockOre() {
        // CE's own trigger roll is NOT nested inside a dimID==0 check - it fires in every dimension.
        // Registered for overworld+nether only: vanilla's End has no naturally-generated bedrock at
        // y=0 for it to ever match, so including it there would be a pure no-op every chunk.
        DeferredHolder<Feature<?>, BedrockOreFeature> overworldTier =
                FEATURES.register("bedrock_ore_overworld", () -> new BedrockOreFeature(NoneFeatureConfiguration.CODEC, false));
        OVERWORLD.put("bedrock_ore_overworld", overworldTier);
        NETHER.put("bedrock_ore_overworld", overworldTier);

        // CE's additional, independent dimID==-1-only weighted glowstone/phosphorus/quartz roll.
        DeferredHolder<Feature<?>, BedrockOreFeature> netherWeighted =
                FEATURES.register("bedrock_ore_nether", () -> new BedrockOreFeature(NoneFeatureConfiguration.CODEC, true));
        NETHER.put("bedrock_ore_nether", netherWeighted);
    }

    // ==================== registration helpers ====================

    private static void ellipsoid(Map<String, DeferredHolder<Feature<?>, ? extends Feature<?>>> group, String name,
                                   ToIntFunction<ResourceKey<Level>> veinCountFn, String oreBlockName,
                                   int amount, int minHeight, int variance, Supplier<Block> targetSupplier) {
        DeferredHolder<Feature<?>, EllipsoidOreFeature> holder = FEATURES.register(name, () ->
                new EllipsoidOreFeature(NoneFeatureConfiguration.CODEC, veinCountFn, oreBlockName, amount, minHeight, variance, targetSupplier));
        group.put(name, holder);
    }

    private static void chanceGated(Map<String, DeferredHolder<Feature<?>, ? extends Feature<?>>> group, String name,
                                     ToIntFunction<ResourceKey<Level>> chanceFn, String oreBlockName,
                                     int amount, int minHeight, int variance, Supplier<Block> targetSupplier) {
        DeferredHolder<Feature<?>, ChanceGatedDepositFeature> holder = FEATURES.register(name, () ->
                new ChanceGatedDepositFeature(NoneFeatureConfiguration.CODEC, chanceFn, oreBlockName, amount, minHeight, variance, targetSupplier));
        group.put(name, holder);
    }

    private static void depth(Map<String, DeferredHolder<Feature<?>, ? extends Feature<?>>> group, String name,
                               int[] yMins, int yDev, int chance, int size, double fill, String oreBlockName,
                               boolean nether, String fillerBlockName) {
        DeferredHolder<Feature<?>, DepthDepositFeature> holder = FEATURES.register(name, () ->
                new DepthDepositFeature(NoneFeatureConfiguration.CODEC, yMins, yDev, chance, size, fill, oreBlockName, nether, fillerBlockName));
        group.put(name, holder);
    }

    private static void cave(Map<String, DeferredHolder<Feature<?>, ? extends Feature<?>>> group, String name, int id,
                              double threshold, int rangeMult, int maxRange, int yLevel, String oreBlockName,
                              String stalagmiteTypeSuffix, java.util.function.BooleanSupplier enabledSupplier) {
        DeferredHolder<Feature<?>, CaveOreFeature> holder = FEATURES.register(name, () ->
                new CaveOreFeature(NoneFeatureConfiguration.CODEC, id, threshold, rangeMult, maxRange, yLevel, oreBlockName, stalagmiteTypeSuffix, enabledSupplier));
        group.put(name, holder);
    }

    private static void layer(Map<String, DeferredHolder<Feature<?>, ? extends Feature<?>>> group, String name, int id,
                               double scaleH, double scaleV, double threshold, String oreBlockName,
                               java.util.function.BooleanSupplier enabledSupplier) {
        DeferredHolder<Feature<?>, NoiseLayerOreFeature> holder = FEATURES.register(name, () ->
                new NoiseLayerOreFeature(NoneFeatureConfiguration.CODEC, id, scaleH, scaleV, threshold, oreBlockName, enabledSupplier));
        group.put(name, holder);
    }

    private static ToIntFunction<ResourceKey<Level>> forDim(Supplier<Map<String, Integer>> mapSupplier) {
        return dim -> CompatibilityConfig.forDimension(mapSupplier.get(), dim);
    }

    private static ToIntFunction<ResourceKey<Level>> forDim(Supplier<Map<String, Integer>> mapSupplier, int multiplier) {
        return dim -> CompatibilityConfig.forDimension(mapSupplier.get(), dim) * multiplier;
    }
}
