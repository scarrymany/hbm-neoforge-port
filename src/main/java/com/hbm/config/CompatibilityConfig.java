package com.hbm.config;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Port of CE's {@code CompatibilityConfig}. Registered into {@link HbmConfig}'s COMMON spec.
 * <p>
 * <b>Scope note:</b> CE has ~60 per-dimension spawn-rate/structure/geyser/rad maps total, all
 * originally keyed by CE's integer Forge dimension ID - a concept that no longer exists in 1.21
 * (dimensions are identified by {@code ResourceKey<Level>} now). The ~39 that this mod's own
 * ordinary world-gen (ore veins, oil/bedrock-oil deposits, the passive ambient meteorite) actually
 * needs are re-keyed and ported below, by dimension {@code ResourceLocation} string (see "world_gen"
 * and "structures" sections) - see {@code docs/phase4/ore_veins_and_bedrock_ores.md} and
 * {@code docs/phase4/worldgen_oil_and_meteor_dungeons.md} for the exact CE line numbers and default
 * values these were re-keyed from. The remaining ~18 (CE leftover
 * {@code dimensionRad}, {@code peaceDimensions}, {@code fillCraterWithWater}, Galacticraft/
 * ExtraPlanets/other-mod-specific oil/gas maps such as {@code dunaOilSpawn}/{@code laytheOilSpawn}/
 * {@code eveGasSpawn}, and {@code isWarDim(World)}) are intentionally NOT ported here - they refer
 * to structures/dimensions this phase doesn't own or to other mods' dimension IDs that may not even
 * have a 1.21 NeoForge counterpart yet. Re-keying those based on guessed mod compatibility would be
 * worse than not porting them; that remains for whichever phase actually needs them.
 * <p>
 * <b>Delimiter note:</b> every dimension-keyed map below uses {@code "="} (not CE's own {@code ":"})
 * as the {@link ConfigUtil#toIntMap} delimiter, because a dimension key is itself a
 * {@code namespace:path} string containing a colon (e.g. {@code "minecraft:the_nether"}) - the same
 * reason {@link #MOB_RADRESISTANCE_RAW} below already uses {@code "="} for its {@code modid:entityid}
 * keys rather than {@code ":"}. A dimension with no entry in a map falls back to {@code 0}, matching
 * CE's own {@code HashMap#get} + null-checked-to-{@code 0} behavior at every real call site.
 * <p>
 * What else is ported here: mob radiation resistance/immunity (keyed by mod id / entity id, not
 * dimension), mob gear/loot toggles, the bedrock ore oredict blacklist, and the crater water-fill
 * toggle.
 */
public class CompatibilityConfig {

    public static ConfigValue<List<? extends String>> MOB_MOD_RADRESISTANCE_RAW;
    public static ConfigValue<List<? extends String>> MOB_MOD_RADIMMUNE_RAW;
    public static ConfigValue<List<? extends String>> MOB_RADRESISTANCE_RAW;
    public static ConfigValue<List<? extends String>> MOB_RADIMMUNE_RAW;
    public static BooleanValue MOB_GEAR;
    public static BooleanValue MOD_LOOT;
    public static ConfigValue<List<? extends String>> BEDROCK_ORE_BLACKLIST_RAW;
    public static BooleanValue DO_FILL_CRATER_WITH_WATER;

    // dimension-keyed ore-vein spawn-rate maps (Phase 4 world-gen; see class javadoc)
    public static ConfigValue<List<? extends String>> URANIUM_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> THORIUM_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> TITANIUM_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> SULFUR_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> ALUMINIUM_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> COPPER_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> FLUORITE_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> NITER_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> TUNGSTEN_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> LEAD_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> BERYLLIUM_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> RARE_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> LIGNITE_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> ASBESTOS_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> CINNABAR_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> COBALT_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> IRON_CLUSTER_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> TITANIUM_CLUSTER_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> ALUMINIUM_CLUSTER_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> COPPER_CLUSTER_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> AUSTRALIUM_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> NETHER_URANIUM_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> NETHER_TUNGSTEN_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> NETHER_SULFUR_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> NETHER_PHOSPHORUS_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> NETHER_COAL_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> NETHER_COBALT_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> NETHER_PLUTONIUM_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> END_TIXITE_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> GNEISS_IRON_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> GNEISS_GOLD_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> LITHIUM_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> GASSSHALE_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> GASBUBBLE_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> EXPLOSIVEBUBBLE_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> ALEXANDRITE_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> OIL_BUBBLE_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> BEDROCK_OIL_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> METEORITE_SPAWN_RAW;
    public static ConfigValue<List<? extends String>> ANTENNA_STRUCTURE_RAW;
    public static ConfigValue<List<? extends String>> BUNKER_STRUCTURE_RAW;
    public static ConfigValue<List<? extends String>> RADIO_STRUCTURE_RAW;
    public static ConfigValue<List<? extends String>> RADFREQ_RAW;
    public static ConfigValue<List<? extends String>> MINEFREQ_RAW;
    public static ConfigValue<List<? extends String>> DUD_STRUCTURE_RAW;
    public static ConfigValue<List<? extends String>> BARREL_STRUCTURE_RAW;
    public static ConfigValue<List<? extends String>> SPACESHIP_STRUCTURE_RAW;
    public static ConfigValue<List<? extends String>> SATELLITE_STRUCTURE_RAW;

    static void init(ModConfigSpec.Builder builder) {
        builder.push("mobs");

        MOB_MOD_RADRESISTANCE_RAW = builder
                .comment("Amount of radiation resistance all mobs of a given mod get. Resistance s is calculated as s=(1-0.1^r): a resistance of 3.0 blocks 99.9% of radiation. Format: 'mod=radresistance'. [CE: 12.01_mob_Mod_Radresistance]")
                .defineListAllowEmpty("mobModRadresistance", () -> List.of(
                        "srparasites=0.2",
                        "thaumcraft=0.75"
                ), entry -> entry instanceof String);
        MOB_MOD_RADIMMUNE_RAW = builder
                .comment("Mods whose entities should all be immune to radiation. Format: 'mod'. [CE: 12.03_mob_Mod_Radimmune]")
                .defineListAllowEmpty("mobModRadimmune", () -> List.of(
                        "biomesoplenty",
                        "galacticraftcore",
                        "galacticraftplanets",
                        "extraplanets",
                        "thaumicaugmentation",
                        "enderskills",
                        "thaumadditions",
                        "cyberware",
                        "rewired"
                ), entry -> entry instanceof String);
        MOB_RADRESISTANCE_RAW = builder
                .comment("Amount of radiation resistance a specific mob gets. Resistance s is calculated as s=(1-0.1^r): a resistance of 3.0 blocks 99.9% of radiation. Format: 'mod:mobid=radresistance'. [CE: 12.02_mob_Radresistance]")
                .defineListAllowEmpty("mobRadresistance", () -> List.of(
                        "minecraft:parrot=0.5",
                        "minecraft:rabbit=1.0",
                        "minecraft:enderman=1.5",
                        "minecraft:blaze=2.0",
                        "minecraft:bat=2.5",
                        "minecraft:ghast=3.0",
                        "minecraft:squid=3.5",
                        "minecraft:spider=4.0",
                        "minecraft:cave_spider=5.0",
                        "minecraft:silverfish=6.0",
                        "minecraft:endermite=7.0",
                        "minecraft:shulker=8.0",
                        "minecraft:ender_dragon=9.0"
                ), entry -> entry instanceof String);
        MOB_RADIMMUNE_RAW = builder
                .comment("Mobs that are immune to radiation. Format: 'mod:mobid'. [CE: 12.04_mob_Radimmune]")
                .defineListAllowEmpty("mobRadimmune", () -> List.of(
                        "minecraft:magma_cube",
                        "minecraft:slime",
                        "minecraft:vex",
                        "minecraft:villager_golem",
                        "minecraft:snowman",
                        "minecraft:witch"
                ), entry -> entry instanceof String);
        MOB_GEAR = builder
                .comment("If true, mobs will be given gear (armor/weapons/gasmasks) from this mod when spawned. [CE: 12.05_mobGear]")
                .define("mobGear", true);
        MOD_LOOT = builder
                .comment("If true, this mod will generate loot for chests. [CE: 12.06_modLoot]")
                .define("modLoot", true);

        builder.pop();

        builder.push("ores");

        BEDROCK_ORE_BLACKLIST_RAW = builder
                .comment("OreDict entries that should not have bedrock ores generated for them. [CE: 08.01_bedrockOreBlacklist]")
                .defineListAllowEmpty("bedrockOreBlacklist", () -> List.of(
                        "oreTh232",
                        "oreThorium232",
                        "oreVolcanic",
                        "oreSteel"
                ), entry -> entry instanceof String);

        builder.pop();

        builder.push("nukes");

        DO_FILL_CRATER_WITH_WATER = builder
                .comment("If true, nukes will fill the crater with water if it's in a wet place. Adds a bit of lag but looks better. [CE: 03.04_doFillCraterWithWater]")
                .define("doFillCraterWithWater", true);

        builder.pop();

        builder.comment(
                "Dimension-keyed ore/oil spawn-rate maps for this mod's own ordinary world-gen.",
                "Format per entry: 'dimension=amount', e.g. 'minecraft:overworld=7' or",
                "'minecraft:the_nether=8' - see the class javadoc for why '=' is used instead of CE's",
                "own ':'. A dimension with no entry defaults to 0 (matches CE's own null-to-0 fallback)."
        ).push("world_gen");

        URANIUM_SPAWN_RAW = builder
                .comment("Amount of uranium ore veins per chunk. [CE: 01.01_uraniumSpawnrate]")
                .defineListAllowEmpty("uraniumSpawn", () -> List.of("minecraft:overworld=7"), entry -> entry instanceof String);
        THORIUM_SPAWN_RAW = builder
                .comment("Amount of thorium ore veins per chunk. [CE: 01.11_thoriumSpawnrate]")
                .defineListAllowEmpty("thoriumSpawn", () -> List.of("minecraft:overworld=7"), entry -> entry instanceof String);
        TITANIUM_SPAWN_RAW = builder
                .comment("Amount of titanium ore veins per chunk. [CE: 01.02_titaniumSpawnrate]")
                .defineListAllowEmpty("titaniumSpawn", () -> List.of("minecraft:overworld=8"), entry -> entry instanceof String);
        SULFUR_SPAWN_RAW = builder
                .comment("Amount of sulfur ore veins per chunk. [CE: 01.03_sulfurSpawnrate]")
                .defineListAllowEmpty("sulfurSpawn", () -> List.of("minecraft:overworld=5"), entry -> entry instanceof String);
        ALUMINIUM_SPAWN_RAW = builder
                .comment("Amount of aluminium ore veins per chunk. [CE: 01.04_aluminiumSpawnrate]")
                .defineListAllowEmpty("aluminiumSpawn", () -> List.of("minecraft:overworld=7"), entry -> entry instanceof String);
        COPPER_SPAWN_RAW = builder
                .comment("Amount of copper ore veins per chunk. [CE: 01.05_copperSpawnrate]")
                .defineListAllowEmpty("copperSpawn", () -> List.of("minecraft:overworld=12"), entry -> entry instanceof String);
        FLUORITE_SPAWN_RAW = builder
                .comment("Amount of fluorite ore veins per chunk. [CE: 01.06_fluoriteSpawnrate]")
                .defineListAllowEmpty("fluoriteSpawn", () -> List.of("minecraft:overworld=6"), entry -> entry instanceof String);
        NITER_SPAWN_RAW = builder
                .comment("Amount of niter ore veins per chunk. [CE: 01.07_niterSpawnrate]")
                .defineListAllowEmpty("niterSpawn", () -> List.of("minecraft:overworld=6"), entry -> entry instanceof String);
        TUNGSTEN_SPAWN_RAW = builder
                .comment("Amount of tungsten ore veins per chunk. [CE: 01.08_tungstenSpawnrate]")
                .defineListAllowEmpty("tungstenSpawn", () -> List.of("minecraft:overworld=10"), entry -> entry instanceof String);
        LEAD_SPAWN_RAW = builder
                .comment("Amount of lead ore veins per chunk. [CE: 01.09_leadSpawnrate]")
                .defineListAllowEmpty("leadSpawn", () -> List.of("minecraft:overworld=6"), entry -> entry instanceof String);
        BERYLLIUM_SPAWN_RAW = builder
                .comment("Amount of beryllium ore veins per chunk. [CE: 01.10_berylliumSpawnrate]")
                .defineListAllowEmpty("berylliumSpawn", () -> List.of("minecraft:overworld=6"), entry -> entry instanceof String);
        RARE_SPAWN_RAW = builder
                .comment("""
                        Amount of rare earth ore veins per chunk. CE also reuses this same value, \
                        unmodified, for a second (likely copy-paste) ore_gneiss_asbestos placement pass \
                        instead of a dedicated ore_gneiss_rare rate - preserved as-is for CE parity, see \
                        docs/phase4/ore_veins_and_bedrock_ores.md's Open questions. [CE: 01.15_rareEarthSpawnRate]""")
                .defineListAllowEmpty("rareSpawn", () -> List.of("minecraft:overworld=6"), entry -> entry instanceof String);
        LIGNITE_SPAWN_RAW = builder
                .comment("Amount of lignite ore veins per chunk. [CE: 01.12_ligniteSpawnrate]")
                .defineListAllowEmpty("ligniteSpawn", () -> List.of("minecraft:overworld=2"), entry -> entry instanceof String);
        ASBESTOS_SPAWN_RAW = builder
                .comment("Amount of asbestos ore veins per chunk. [CE: 01.13_asbestosSpawnRate]")
                .defineListAllowEmpty("asbestosSpawn", () -> List.of("minecraft:overworld=2"), entry -> entry instanceof String);
        CINNABAR_SPAWN_RAW = builder
                .comment("Amount of cinnabar ore veins per chunk. [CE: 01.20_cinnabarSpawnRate]")
                .defineListAllowEmpty("cinnabarSpawn", () -> List.of("minecraft:overworld=1"), entry -> entry instanceof String);
        COBALT_SPAWN_RAW = builder
                .comment("Amount of cobalt ore veins per chunk. [CE: 01.21_cobaltSpawnRate]")
                .defineListAllowEmpty("cobaltSpawn", () -> List.of("minecraft:overworld=2"), entry -> entry instanceof String);
        IRON_CLUSTER_SPAWN_RAW = builder
                .comment("Amount of iron cluster veins per chunk. [CE: 01.22_ironClusterSpawn]")
                .defineListAllowEmpty("ironClusterSpawn", () -> List.of("minecraft:overworld=4"), entry -> entry instanceof String);
        TITANIUM_CLUSTER_SPAWN_RAW = builder
                .comment("Amount of titanium cluster veins per chunk. [CE: 01.23_titaniumClusterSpawn]")
                .defineListAllowEmpty("titaniumClusterSpawn", () -> List.of("minecraft:overworld=2"), entry -> entry instanceof String);
        ALUMINIUM_CLUSTER_SPAWN_RAW = builder
                .comment("Amount of aluminium cluster veins per chunk. [CE: 01.24_aluminiumClusterSpawn]")
                .defineListAllowEmpty("aluminiumClusterSpawn", () -> List.of("minecraft:overworld=3"), entry -> entry instanceof String);
        COPPER_CLUSTER_SPAWN_RAW = builder
                .comment("Amount of copper cluster veins per chunk. [CE: 01.24_copperClusterSpawn]")
                .defineListAllowEmpty("copperClusterSpawn", () -> List.of("minecraft:overworld=3"), entry -> entry instanceof String);
        AUSTRALIUM_SPAWN_RAW = builder
                .comment("""
                        Amount of australium ore veins per chunk. CE has no real overworld entry for this \
                        field at all (only a value keyed to -31, an unidentified modded dimension); CE's \
                        real overworld source of australium is a hardcoded x/z treasure-zone deposit, not \
                        this ordinary per-chunk vein - see docs/phase4/ore_veins_and_bedrock_ores.md Group \
                        B. Kept at 0 here for CE parity; listed explicitly for discoverability rather than \
                        left empty. [CE: 01.27_australiumSpawnRate]""")
                .defineListAllowEmpty("australiumSpawn", () -> List.of("minecraft:overworld=0"), entry -> entry instanceof String);
        NETHER_URANIUM_SPAWN_RAW = builder
                .comment("Amount of nether uranium (and scorched-uranium outgas) ore veins per chunk. [CE: 02.N00_uraniumSpawnrate]")
                .defineListAllowEmpty("netherUraniumSpawn", () -> List.of("minecraft:the_nether=8"), entry -> entry instanceof String);
        NETHER_TUNGSTEN_SPAWN_RAW = builder
                .comment("Amount of nether tungsten ore veins per chunk. [CE: 02.N01_tungstenSpawnrate]")
                .defineListAllowEmpty("netherTungstenSpawn", () -> List.of("minecraft:the_nether=10"), entry -> entry instanceof String);
        NETHER_SULFUR_SPAWN_RAW = builder
                .comment("Amount of nether sulfur ore veins per chunk. [CE: 02.N02_sulfurSpawnrate]")
                .defineListAllowEmpty("netherSulfurSpawn", () -> List.of("minecraft:the_nether=26"), entry -> entry instanceof String);
        NETHER_PHOSPHORUS_SPAWN_RAW = builder
                .comment("Amount of nether phosphorus (ore_nether_fire) ore veins per chunk. [CE: 02.N03_phosphorusSpawnrate]")
                .defineListAllowEmpty("netherPhosphorusSpawn", () -> List.of("minecraft:the_nether=24"), entry -> entry instanceof String);
        NETHER_COAL_SPAWN_RAW = builder
                .comment("Amount of nether coal ore veins per chunk. [CE: 02.N04_coalSpawnrate]")
                .defineListAllowEmpty("netherCoalSpawn", () -> List.of("minecraft:the_nether=24"), entry -> entry instanceof String);
        NETHER_COBALT_SPAWN_RAW = builder
                .comment("Amount of nether cobalt ore veins per chunk. [CE: 02.N06_cobaltSpawnrate]")
                .defineListAllowEmpty("netherCobaltSpawn", () -> List.of("minecraft:the_nether=2"), entry -> entry instanceof String);
        NETHER_PLUTONIUM_SPAWN_RAW = builder
                .comment("Amount of nether plutonium ore veins per chunk, if enabled (see GeneralConfig.ENABLE_PLUTONIUM_NETHER_ORE). [CE: 02.N05_plutoniumSpawnrate]")
                .defineListAllowEmpty("netherPlutoniumSpawn", () -> List.of("minecraft:the_nether=8"), entry -> entry instanceof String);
        END_TIXITE_SPAWN_RAW = builder
                .comment("Amount of end tixite ore veins per chunk. [CE: 03.E01_tixiteSpawnrate]")
                .defineListAllowEmpty("endTixiteSpawn", () -> List.of("minecraft:the_end=8"), entry -> entry instanceof String);
        GNEISS_IRON_SPAWN_RAW = builder
                .comment("Amount of iron ore veins per chunk inside the gneiss stratum. [CE: 01.34_gneissIronSpawnrate]")
                .defineListAllowEmpty("gneissIronSpawn", () -> List.of("minecraft:overworld=25"), entry -> entry instanceof String);
        GNEISS_GOLD_SPAWN_RAW = builder
                .comment("Amount of gold ore veins per chunk inside the gneiss stratum. [CE: 01.35_gneissGoldSpawnrate]")
                .defineListAllowEmpty("gneissGoldSpawn", () -> List.of("minecraft:overworld=10"), entry -> entry instanceof String);
        LITHIUM_SPAWN_RAW = builder
                .comment("Amount of schist lithium ore veins per chunk inside the gneiss stratum. [CE: 01.14_lithiumSpawnRate]")
                .defineListAllowEmpty("lithiumSpawn", () -> List.of("minecraft:overworld=6"), entry -> entry instanceof String);
        GASSSHALE_SPAWN_RAW = builder
                .comment("Amount of oil shale veins per chunk; also the base rate (x3) for the ore_gneiss_gas gneiss-stratum vein. [CE: 01.17_gasShaleSpawnRate]")
                .defineListAllowEmpty("gassshaleSpawn", () -> List.of("minecraft:overworld=5"), entry -> entry instanceof String);
        GASBUBBLE_SPAWN_RAW = builder
                .comment("Spawns a flammable gas bubble every Nth chunk (1-in-N chance), gated by GeneralConfig.ENABLE_FLAMMABLE_GAS. [CE: 01.19_gasBubbleSpawnRate]")
                .defineListAllowEmpty("gasbubbleSpawn", () -> List.of("minecraft:overworld=40"), entry -> entry instanceof String);
        EXPLOSIVEBUBBLE_SPAWN_RAW = builder
                .comment("Spawns an explosive gas bubble every Nth chunk (1-in-N chance), gated by GeneralConfig.ENABLE_EXPLOSIVE_GAS. [CE: 01.18_explosiveBubbleSpawnRate]")
                .defineListAllowEmpty("explosivebubbleSpawn", () -> List.of("minecraft:overworld=80"), entry -> entry instanceof String);
        ALEXANDRITE_SPAWN_RAW = builder
                .comment("Spawns an alexandrite vein every Nth chunk (1-in-N chance). [CE: 01.32_alexandriteSpawnRate]")
                .defineListAllowEmpty("alexandriteSpawn", () -> List.of("minecraft:overworld=100"), entry -> entry instanceof String);
        OIL_BUBBLE_SPAWN_RAW = builder
                .comment("Spawns an underground oil bubble every Nth chunk (1-in-N chance; further divided by 3 in hot/dry biomes per CE). [CE: 01.33_oilSpawnRate]")
                .defineListAllowEmpty("oilBubbleSpawn", () -> List.of("minecraft:overworld=100"), entry -> entry instanceof String);
        BEDROCK_OIL_SPAWN_RAW = builder
                .comment("Spawns a bedrock-layer (y=0) oil deposit every Nth chunk (1-in-N chance). [CE: 01.31_bedrockOilSpawnRate]")
                .defineListAllowEmpty("bedrockOilSpawn", () -> List.of("minecraft:overworld=200"), entry -> entry instanceof String);

        builder.pop();

        builder.push("structures");

        METEORITE_SPAWN_RAW = builder
                .comment("Spawns a fallen, ambient/passive meteorite every Nth chunk (1-in-N chance). [CE: 03.19_meteoriteSpawn]")
                .defineListAllowEmpty("meteoriteSpawn", () -> List.of("minecraft:overworld=200"), entry -> entry instanceof String);
        ANTENNA_STRUCTURE_RAW = builder
                .comment("Spawns CE's Antenna radio mast every Nth chunk (1-in-N chance). [CE CompatibilityConfig.antennaStructure default 0:750]")
                .defineListAllowEmpty("antennaStructure", () -> List.of("minecraft:overworld=750"), entry -> entry instanceof String);
        BUNKER_STRUCTURE_RAW = builder
                .comment("Spawns CE's underground Bunker every Nth chunk (1-in-N chance). [CE CompatibilityConfig.bunkerStructure default 0:1000]")
                .defineListAllowEmpty("bunkerStructure", () -> List.of("minecraft:overworld=1000"), entry -> entry instanceof String);
        RADIO_STRUCTURE_RAW = builder
                .comment("Spawns CE's Radio station (Radio01+Radio02) every Nth chunk (1-in-N chance). [CE CompatibilityConfig.radioStructure default 0:1000]")
                .defineListAllowEmpty("radioStructure", () -> List.of("minecraft:overworld=1000"), entry -> entry instanceof String);
        RADFREQ_RAW = builder
                .comment("Spawn a Sellafield radiation hotspot every Nth chunk (1-in-N). [CE CompatibilityConfig.radfreq 03.17_radHotsoptSpawn default 0:5000]")
                .defineListAllowEmpty("radfreq", () -> List.of("minecraft:overworld=5000"), entry -> entry instanceof String);
        MINEFREQ_RAW = builder
                .comment("Spawn an AP landmine every Nth chunk (1-in-N). [CE CompatibilityConfig.minefreq 03.15_landmineSpawn default 0:64]")
                .defineListAllowEmpty("minefreq", () -> List.of("minecraft:overworld=64"), entry -> entry instanceof String);
        DUD_STRUCTURE_RAW = builder
                .comment("Spawns CE's crashed-bomb Dud every Nth chunk (1-in-N chance). [CE CompatibilityConfig.dudStructure 03.11_dudSpawn default 0:500]")
                .defineListAllowEmpty("dudStructure", () -> List.of("minecraft:overworld=500"), entry -> entry instanceof String);
        BARREL_STRUCTURE_RAW = builder
                .comment("Spawns CE's waste-tank Barrel every Nth chunk (1-in-N chance). [CE CompatibilityConfig.barrelStructure 03.13_barrelSpawn default 0:5000]")
                .defineListAllowEmpty("barrelStructure", () -> List.of("minecraft:overworld=5000"), entry -> entry instanceof String);
        SPACESHIP_STRUCTURE_RAW = builder
                .comment("Spawns CE's crashed Spaceship every Nth chunk (1-in-N chance). [CE CompatibilityConfig.spaceshipStructure 03.12_spaceshipSpawn default 0:1000]")
                .defineListAllowEmpty("spaceshipStructure", () -> List.of("minecraft:overworld=1000"), entry -> entry instanceof String);
        SATELLITE_STRUCTURE_RAW = builder
                .comment("Spawns CE's Satellite dish every Nth chunk (1-in-N chance). [CE CompatibilityConfig.satelliteStructure 03.07_satelliteSpawn default 0:500]")
                .defineListAllowEmpty("satelliteStructure", () -> List.of("minecraft:overworld=500"), entry -> entry instanceof String);

        builder.pop();
    }

    public static Map<String, Float> mobModRadresistance() {
        return ConfigUtil.toFloatMap(MOB_MOD_RADRESISTANCE_RAW.get(), "=");
    }

    public static Set<String> mobModRadimmune() {
        return new HashSet<>(MOB_MOD_RADIMMUNE_RAW.get());
    }

    public static Map<String, Float> mobRadresistance() {
        return ConfigUtil.toFloatMap(MOB_RADRESISTANCE_RAW.get(), "=");
    }

    public static Set<String> mobRadimmune() {
        return new HashSet<>(MOB_RADIMMUNE_RAW.get());
    }

    public static Set<String> bedrockOreBlacklist() {
        return new HashSet<>(BEDROCK_ORE_BLACKLIST_RAW.get());
    }

    // --- dimension-keyed ore/oil/meteorite spawn-rate lookups (Phase 4 world-gen) ---

    private static final String DIM_DELIMITER = "=";

    /**
     * The map key every spawn-rate map below uses for a given dimension - just its
     * {@code ResourceLocation} string form (e.g. {@code "minecraft:the_nether"}).
     */
    public static String dimensionKey(ResourceKey<Level> dimension) {
        return dimension.location().toString();
    }

    /**
     * Looks up a per-dimension entry in one of the spawn-rate maps below, defaulting to {@code 0}
     * for a dimension with no entry - mirrors CE's own {@code HashMap#get} (returns {@code null})
     * plus its every call site's {@code null -> 0} fallback (see {@code HbmWorldGen.parseInt}).
     */
    public static int forDimension(Map<String, Integer> spawnMap, ResourceKey<Level> dimension) {
        Integer value = spawnMap.get(dimensionKey(dimension));
        return value != null ? value : 0;
    }

    private static Map<String, Integer> spawnMap(ConfigValue<List<? extends String>> raw) {
        return ConfigUtil.toIntMap(raw.get(), DIM_DELIMITER);
    }

    public static Map<String, Integer> uraniumSpawn() { return spawnMap(URANIUM_SPAWN_RAW); }
    public static Map<String, Integer> thoriumSpawn() { return spawnMap(THORIUM_SPAWN_RAW); }
    public static Map<String, Integer> titaniumSpawn() { return spawnMap(TITANIUM_SPAWN_RAW); }
    public static Map<String, Integer> sulfurSpawn() { return spawnMap(SULFUR_SPAWN_RAW); }
    public static Map<String, Integer> aluminiumSpawn() { return spawnMap(ALUMINIUM_SPAWN_RAW); }
    public static Map<String, Integer> copperSpawn() { return spawnMap(COPPER_SPAWN_RAW); }
    public static Map<String, Integer> fluoriteSpawn() { return spawnMap(FLUORITE_SPAWN_RAW); }
    public static Map<String, Integer> niterSpawn() { return spawnMap(NITER_SPAWN_RAW); }
    public static Map<String, Integer> tungstenSpawn() { return spawnMap(TUNGSTEN_SPAWN_RAW); }
    public static Map<String, Integer> leadSpawn() { return spawnMap(LEAD_SPAWN_RAW); }
    public static Map<String, Integer> berylliumSpawn() { return spawnMap(BERYLLIUM_SPAWN_RAW); }
    public static Map<String, Integer> rareSpawn() { return spawnMap(RARE_SPAWN_RAW); }
    public static Map<String, Integer> ligniteSpawn() { return spawnMap(LIGNITE_SPAWN_RAW); }
    public static Map<String, Integer> asbestosSpawn() { return spawnMap(ASBESTOS_SPAWN_RAW); }
    public static Map<String, Integer> cinnabarSpawn() { return spawnMap(CINNABAR_SPAWN_RAW); }
    public static Map<String, Integer> cobaltSpawn() { return spawnMap(COBALT_SPAWN_RAW); }
    public static Map<String, Integer> ironClusterSpawn() { return spawnMap(IRON_CLUSTER_SPAWN_RAW); }
    public static Map<String, Integer> titaniumClusterSpawn() { return spawnMap(TITANIUM_CLUSTER_SPAWN_RAW); }
    public static Map<String, Integer> aluminiumClusterSpawn() { return spawnMap(ALUMINIUM_CLUSTER_SPAWN_RAW); }
    public static Map<String, Integer> copperClusterSpawn() { return spawnMap(COPPER_CLUSTER_SPAWN_RAW); }
    public static Map<String, Integer> australiumSpawn() { return spawnMap(AUSTRALIUM_SPAWN_RAW); }
    public static Map<String, Integer> netherUraniumSpawn() { return spawnMap(NETHER_URANIUM_SPAWN_RAW); }
    public static Map<String, Integer> netherTungstenSpawn() { return spawnMap(NETHER_TUNGSTEN_SPAWN_RAW); }
    public static Map<String, Integer> netherSulfurSpawn() { return spawnMap(NETHER_SULFUR_SPAWN_RAW); }
    public static Map<String, Integer> netherPhosphorusSpawn() { return spawnMap(NETHER_PHOSPHORUS_SPAWN_RAW); }
    public static Map<String, Integer> netherCoalSpawn() { return spawnMap(NETHER_COAL_SPAWN_RAW); }
    public static Map<String, Integer> netherCobaltSpawn() { return spawnMap(NETHER_COBALT_SPAWN_RAW); }
    public static Map<String, Integer> netherPlutoniumSpawn() { return spawnMap(NETHER_PLUTONIUM_SPAWN_RAW); }
    public static Map<String, Integer> endTixiteSpawn() { return spawnMap(END_TIXITE_SPAWN_RAW); }
    public static Map<String, Integer> gneissIronSpawn() { return spawnMap(GNEISS_IRON_SPAWN_RAW); }
    public static Map<String, Integer> gneissGoldSpawn() { return spawnMap(GNEISS_GOLD_SPAWN_RAW); }
    public static Map<String, Integer> lithiumSpawn() { return spawnMap(LITHIUM_SPAWN_RAW); }
    public static Map<String, Integer> gassshaleSpawn() { return spawnMap(GASSSHALE_SPAWN_RAW); }
    public static Map<String, Integer> gasbubbleSpawn() { return spawnMap(GASBUBBLE_SPAWN_RAW); }
    public static Map<String, Integer> explosivebubbleSpawn() { return spawnMap(EXPLOSIVEBUBBLE_SPAWN_RAW); }
    public static Map<String, Integer> alexandriteSpawn() { return spawnMap(ALEXANDRITE_SPAWN_RAW); }
    public static Map<String, Integer> oilBubbleSpawn() { return spawnMap(OIL_BUBBLE_SPAWN_RAW); }
    public static Map<String, Integer> bedrockOilSpawn() { return spawnMap(BEDROCK_OIL_SPAWN_RAW); }
    public static Map<String, Integer> meteoriteSpawn() { return spawnMap(METEORITE_SPAWN_RAW); }
    public static Map<String, Integer> antennaStructure() { return spawnMap(ANTENNA_STRUCTURE_RAW); }
    public static Map<String, Integer> bunkerStructure() { return spawnMap(BUNKER_STRUCTURE_RAW); }
    public static Map<String, Integer> radioStructure() { return spawnMap(RADIO_STRUCTURE_RAW); }
    public static Map<String, Integer> radfreq() { return spawnMap(RADFREQ_RAW); }
    public static Map<String, Integer> minefreq() { return spawnMap(MINEFREQ_RAW); }
    public static Map<String, Integer> dudStructure() { return spawnMap(DUD_STRUCTURE_RAW); }
    public static Map<String, Integer> barrelStructure() { return spawnMap(BARREL_STRUCTURE_RAW); }
    public static Map<String, Integer> spaceshipStructure() { return spawnMap(SPACESHIP_STRUCTURE_RAW); }
    public static Map<String, Integer> satelliteStructure() { return spawnMap(SATELLITE_STRUCTURE_RAW); }
}
