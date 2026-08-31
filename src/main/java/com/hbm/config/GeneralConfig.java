package com.hbm.config;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

import java.util.Set;

/**
 * Port of CE's {@code GeneralConfig}: the catch-all bucket of misc gameplay/rendering toggles,
 * plus the 528 mode and LESS BULLSHIT MODE toggle blocks. Registered into {@link HbmConfig}'s
 * COMMON spec.
 * <p>
 * Not ported from CE:
 * <ul>
 *   <li>{@code trueExp()} - depended on {@code com.hbm.inventory.recipes.PrecAssRecipes.INSTANCE.modified},
 *   which is out of this area's scope. Whoever ports the recipe system should reintroduce this
 *   derived check against {@link #ENABLE_EXPENSIVE_MODE}.</li>
 *   <li>The GL 3.3 capability gating that force-disabled {@code instancedParticles} and the
 *   shader-driven effects ({@code depthEffects}, {@code bloom}, {@code heatDistortion}, etc.) on
 *   unsupported hardware, via {@code com.hbm.render.GLCompat}. <b>Resolved</b> (per
 *   {@code docs/phase5/particle_engine_and_generic_vfx.md} Finding 1/2): no such gate is needed at
 *   all - the 1.21.1 particle-batch replacement ({@link com.hbm.particle.engine.ParticleEngineNT}) is
 *   vanilla {@code RenderType}/{@code VertexConsumer} batching, not raw GL instancing, so there is no
 *   hardware capability left to probe for. {@code INSTANCED_PARTICLES} is instead redefined as a
 *   density knob: {@link com.hbm.particle.engine.ParticleEngineNT} halves its particle-count cap when
 *   this is {@code false}, rather than forking two render code paths - see that class's own
 *   javadoc.</li>
 *   <li>{@code leadSafeForgeContainerWhitelist}'s TOML-backed loader ({@code loadLeadSafeForgeContainerWhitelist}
 *   / {@code 1.99_CE_forgeFluidLeadSafeContainers} in CE) - CE entries are {@code modid:item:meta}
 *   triples, and item metadata no longer exists under the 1.21 Data Component model, so the string
 *   format needs a redesign (keyed by item id alone, as
 *   {@link com.hbm.capability.NTMFluidCapabilityHandler#isLeadSafeForgeContainer} already assumes)
 *   before a real config-list entry can be added here. Until then the field below
 *   is a plain empty set, not backed by a config value - which reproduces CE's own default exactly
 *   ("Default empty means generic Forge Fluid containers are not lead-safe.", CE
 *   {@code GeneralConfig#loadLeadSafeForgeContainerWhitelist}), just not yet player-configurable.</li>
 * </ul>
 */
public class GeneralConfig {

    /**
     * Set of {@code modid:item} ids treated as "lead-safe" generic Forge fluid containers (i.e. they
     * may hold {@link com.hbm.inventory.fluid.FluidType#needsLeadContainer()} fluids despite not being
     * one of NTM's own lead containers). Mirrors CE's {@code GeneralConfig.leadSafeForgeContainerWhitelist},
     * minus its {@code :meta} suffix (see class javadoc) and minus its TOML-backed loader - empty by
     * default, matching CE's own default. See {@link com.hbm.capability.NTMFluidCapabilityHandler#isLeadSafeForgeContainer}.
     */
    public static final Set<String> leadSafeForgeContainerWhitelist = new ObjectOpenHashSet<>();

    // networking
    public static BooleanValue ENABLE_PACKET_THREADING;
    public static IntValue PACKET_THREADING_CORE_COUNT;
    public static IntValue PACKET_THREADING_MAX_COUNT;
    public static BooleanValue PACKET_THREADING_ERROR_BYPASS;
    public static BooleanValue ENABLE_SERVER_RECIPE_SYNC;
    public static BooleanValue ENABLE_ZERO_COPY_COMPATIBILITY_MODE;
    public static BooleanValue ENABLE_THREADED_NODE_SPACE_UPDATE;

    // misc
    public static BooleanValue ENABLE_BLOCK_AUTO_REPLACING;
    public static BooleanValue ENABLE_ADVANCEMENTS;
    public static BooleanValue ENABLE_DEBUG_MODE;
    public static BooleanValue ENABLE_DEBUG_WORLD_GEN;
    public static BooleanValue ENABLE_SKYBOX;
    public static BooleanValue ENABLE_MYCELIUM_SPREAD;
    public static BooleanValue ENABLE_PLUTONIUM_NETHER_ORE;
    public static BooleanValue ENABLE_DUNGEON_SPAWN;
    public static BooleanValue ENABLE_ORES_IN_MODDED_DIMENSIONS;
    public static BooleanValue ENABLE_LANDMINE_SPAWN;
    public static BooleanValue ENABLE_RAD_HOTSPOT_SPAWN;
    public static BooleanValue ENABLE_NITAN_CHEST_SPAWN;
    public static BooleanValue ENABLE_AUTOMATIC_RAD_CLEANUP;
    public static BooleanValue ENABLE_BOMBER_SHORT_MODE;
    public static BooleanValue ENABLE_VAULT_SPAWN;
    public static BooleanValue ENABLE_RADIATION;
    public static BooleanValue ENABLE_CATACLYSM;
    public static BooleanValue ENABLE_EXTENDED_LOGGING;
    public static BooleanValue ENABLE_GUNS;
    public static BooleanValue ENABLE_VIRUS;
    public static BooleanValue ENABLE_CROSSHAIRS;
    public static BooleanValue ENABLE_SHADERS_2;
    public static BooleanValue SSG_ANIM_TYPE;
    public static BooleanValue INSTANCED_PARTICLES;
    public static BooleanValue DEPTH_BUFFER_EFFECTS;
    public static BooleanValue FLASHLIGHTS;
    public static BooleanValue FLASHLIGHT_VOLUMETRICS;
    public static BooleanValue BULLET_HOLE_NORMAL_MAPPING;
    public static IntValue FLOWING_DECAL_MAX;
    public static BooleanValue CALL_LIST_MODELS;
    public static BooleanValue ENABLE_REFLECTOR_COMPAT;
    public static BooleanValue ENABLE_COAL_DUST;
    public static BooleanValue ENABLE_ASBESTOS_DUST;
    public static BooleanValue ENABLE_RADON_GAS;
    public static BooleanValue ENABLE_CARBON_MONOXIDE;
    public static BooleanValue ENABLE_FLAMMABLE_GAS;
    public static BooleanValue ENABLE_EXPLOSIVE_GAS;
    public static BooleanValue ENABLE_MELTDOWN_GAS;
    public static BooleanValue ENABLE_RE_EVAL;
    public static BooleanValue ENABLE_RECIPES;
    public static BooleanValue REGISTER_TANKS;
    public static BooleanValue ENABLE_JEI;
    public static BooleanValue ENABLE_CHANGELOG;
    public static BooleanValue ENABLE_DUCK_BUTTON;
    public static BooleanValue ENABLE_BLOOM;
    public static BooleanValue ENABLE_HEAT_DISTORTION;
    public static BooleanValue ENABLE_ADVANCED_RADIATION;
    public static BooleanValue ENABLE_IMPACT_WORLD_PROVIDER;
    public static BooleanValue ENABLE_BLOOD_EFFECTS;
    public static IntValue CRUCIBLE_MAX_CHARGES;
    public static DoubleValue CONVERSION_RATE_HE_TO_RF;
    public static BooleanValue AUTO_CABLE_CONVERSION;
    public static BooleanValue ENABLE_MOTD;
    public static BooleanValue ENABLE_FLUID_CONTAINER_COMPAT;
    public static BooleanValue ENABLE_GUIDE_BOOK;
    public static IntValue DECO_TO_INGOT_CONVERSION_RATE;
    public static BooleanValue THREADED_ATMOSPHERES;
    public static BooleanValue ENABLE_KEYBIND_OVERLAP;
    public static BooleanValue ENABLE_MACHINE_GRAVITY;
    public static BooleanValue ENABLE_FLUID_CONTAINERS_V2;
    public static BooleanValue DYNAMIC_TREES_COMPAT_MODE;
    public static BooleanValue ENABLE_EXPENSIVE_MODE;

    // 528 mode
    public static BooleanValue ENABLE_528;
    public static BooleanValue X528_FORCE_REASIM_BOILERS;
    public static BooleanValue X528_ENABLE_COLTAN_DEPOSIT;
    public static BooleanValue X528_ENABLE_COLTAN_SPAWNING;
    public static BooleanValue X528_ENABLE_BOSNIA_SIMULATOR;
    public static BooleanValue X528_ENABLE_NETHER_BURN;
    public static BooleanValue X528_ENABLE_PRESSURIZED_RECIPES;
    public static BooleanValue X528_ENABLE_EXPLOSIVE_ENERGISTICS;
    public static BooleanValue X528_ENABLE_MACHINE_GRAVITY;
    public static IntValue X528_ORE_COLTAN_FREQUENCY;

    // LESS BULLSHIT MODE
    public static BooleanValue ENABLE_LBSM;
    public static BooleanValue LBSM_FULL_SCHRAB;
    public static BooleanValue LBSM_SHORT_DECAY;
    public static BooleanValue LBSM_RECIPE_SIMPLE_ARMOR;
    public static BooleanValue LBSM_RECIPE_SIMPLE_TOOL;
    public static BooleanValue LBSM_RECIPE_SIMPLE_ALLOY;
    public static BooleanValue LBSM_RECIPE_SIMPLE_CHEMISTRY;
    public static BooleanValue LBSM_RECIPE_SIMPLE_CENTRIFUGE;
    public static BooleanValue LBSM_RECIPE_UNLOCK_ANVIL;
    public static BooleanValue LBSM_RECIPE_SIMPLE_CRAFTING;
    public static BooleanValue LBSM_RECIPE_SIMPLE_MEDICINE;
    public static BooleanValue LBSM_SAFE_CRATES;
    public static BooleanValue LBSM_SAFE_ME_DRIVES;
    public static BooleanValue LBSM_IGEN;

    static void init(ModConfigSpec.Builder builder) {
        builder.push("general");

        ENABLE_PACKET_THREADING = builder
                .comment("Enables creation of a separate thread to increase packet processing speed on servers. Disable this if you are having anomalous crashes related to memory connections. [CE: 0.01_enablePacketThreading]")
                .define("enablePacketThreading", true);
        PACKET_THREADING_CORE_COUNT = builder
                .comment("Number of core threads to create for packets (recommended 1). [CE: 0.02_packetThreadingCoreCount]")
                .defineInRange("packetThreadingCoreCount", 1, 0, Integer.MAX_VALUE);
        PACKET_THREADING_MAX_COUNT = builder
                .comment("Maximum number of threads to create for packet threading. Must be greater than or equal to packetThreadingCoreCount. [CE: 0.03_packetThreadingMaxCount]")
                .defineInRange("packetThreadingMaxCount", 2, 0, Integer.MAX_VALUE);
        PACKET_THREADING_ERROR_BYPASS = builder
                .comment("Forces the bypassing of most packet threading errors, only enable this if directed to or if you know what you're doing. [CE: 0.04_packetThreadingErrorBypass]")
                .define("packetThreadingErrorBypass", false);
        ENABLE_SERVER_RECIPE_SYNC = builder
                .comment("Syncs any recipes customised via JSON to clients connecting to the server. [CE: 0.05_enableServerRecipeSync]")
                .define("enableServerRecipeSync", true);
        ENABLE_ZERO_COPY_COMPATIBILITY_MODE = builder
                .comment("Routes non-NTM packets back through the default networking path so mods with broken ByteBuf reference counting do not touch NTM's zero-copy hook. [CE: 0.06_enableZeroCopyCompatibilityMode]")
                .define("enableZeroCopyCompatibilityMode", false);
        ENABLE_THREADED_NODE_SPACE_UPDATE = builder
                .comment("Enables threaded updating of the nodespace. This can improve performance, but may cause issues with certain mods. [CE: 0.07_enableThreadedNodeSpaceUpdate]")
                .define("enableThreadedNodeSpaceUpdate", true);

        ENABLE_BLOCK_AUTO_REPLACING = builder
                .comment("""
                        Enables automatic block replacement for missing blocks to avoid giant holes in the ground when they got removed. This may severely impact chunkloading performance,
                        only enable when you are sure that we removed some blocks AND we added that to this replacement system AND you are absolutely sure about what you are doing.
                        Currently only works for hbm:waste_*. [CE: 0.99_CE_01_enableBlockAutoReplacing]""")
                .define("enableBlockAutoReplacing", false);
        ENABLE_ADVANCEMENTS = builder
                .comment("Set to false to disable all NTM advancements. [CE: 0.99_CE_02_enableAdvancements]")
                .define("enableAdvancements", true);
        ENABLE_DEBUG_MODE = builder
                .comment("Enable debugging mode. [CE: 1.00_enableDebugMode]")
                .define("enableDebugMode", false);
        ENABLE_DEBUG_WORLD_GEN = builder
                .comment("Enable debugging mode for phased structure generation. Separate from enableDebugMode. [CE: 1.00_enableDebugWorldGen]")
                .define("enableDebugWorldGen", false);
        ENABLE_SKYBOX = builder
                .comment("If enabled, will try to use NTM's custom skyboxes. [CE: 1.00_enableSkybox]")
                .define("enableSkybox", true);
        ENABLE_MYCELIUM_SPREAD = builder
                .comment("Allows glowing mycelium to spread. [CE: 1.01_enableMyceliumSpread]")
                .define("enableMyceliumSpread", false);
        ENABLE_PLUTONIUM_NETHER_ORE = builder
                .comment("Enables plutonium ore generation in the nether. [CE: 1.02_enablePlutoniumNetherOre]")
                .define("enablePlutoniumNetherOre", false);
        ENABLE_DUNGEON_SPAWN = builder
                .comment("Allows structures and dungeons to spawn. [CE: 1.03_enableDungeonSpawn]")
                .define("enableDungeonSpawn", true);
        ENABLE_ORES_IN_MODDED_DIMENSIONS = builder
                .comment("Allows NTM ores to generate in modded dimensions. [CE: 1.04_enableOresInModdedDimensions]")
                .define("enableOresInModdedDimensions", true);
        ENABLE_LANDMINE_SPAWN = builder
                .comment("Allows landmines to generate. [CE: 1.05_enableLandmineSpawn]")
                .define("enableLandmineSpawn", true);
        ENABLE_RAD_HOTSPOT_SPAWN = builder
                .comment("Allows radiation hotspots to generate. [CE: 1.06_enableRadHotspotSpawn]")
                .define("enableRadHotspotSpawn", true);
        ENABLE_NITAN_CHEST_SPAWN = builder
                .comment("Allows chests to spawn at specific coordinates full of powders. [CE: 1.07_enableNITANChestSpawn]")
                .define("enableNITANChestSpawn", true);
        ENABLE_AUTOMATIC_RAD_CLEANUP = builder
                .comment("Allows waste earth blocks (dirt, grass, mycelium) to turn back into dirt immediately. [CE: 1.09_enableAutomaticRadCleanup]")
                .define("enableAutomaticRadCleanup", false);
        ENABLE_BOMBER_SHORT_MODE = builder
                .comment("Has bomber planes spawn in closer to the target for use with smaller render distances. [CE: 1.14_enableBomberShortMode]")
                .define("enableBomberShortMode", false);
        ENABLE_VAULT_SPAWN = builder
                .comment("Allows locked safes to spawn. [CE: 1.15_enableVaultSpawn]")
                .define("enableVaultSpawn", true);
        ENABLE_RADIATION = builder
                .comment("GENERAL SWITCH: Enables radiation system. [CE: 1.16_enableRadiation]")
                .define("enableRadiation", true);
        ENABLE_CATACLYSM = builder
                .comment("Causes satellites to fall whenever a mob dies. [CE: 1.17_enableCataclysm]")
                .define("enableCataclysm", false);
        ENABLE_EXTENDED_LOGGING = builder
                .comment("Logs uses of the detonator, nuclear explosions, missile launches, grenades, etc. [CE: 1.18_enableExtendedLogging]")
                .define("enableExtendedLogging", false);
        ENABLE_GUNS = builder
                .comment("Prevents new system guns from being fired. [CE: 1.20_enableGuns]")
                .define("enableGuns", true);
        ENABLE_VIRUS = builder
                .comment("Allows virus blocks to spread. [CE: 1.21_enableVirus]")
                .define("enableVirus", false);
        ENABLE_CROSSHAIRS = builder
                .comment("Shows custom crosshairs when an NTM gun is being held. [CE: 1.22_enableCrosshairs]")
                .define("enableCrosshairs", true);
        ENABLE_SHADERS_2 = builder
                .comment("Enables the old NTM Reloaded shader pipeline. Legacy, NOT RECOMMENDED. [CE: 1.23_enableShaders2]")
                .define("enableShaders2", false);
        SSG_ANIM_TYPE = builder
                .comment("Which supershotgun reload animation to use. True is Drillgon's animation, false is Bob's animation. [CE: 1.24_ssgAnimType]")
                .define("ssgAnimType", true);
        INSTANCED_PARTICLES = builder
                .comment("Enables instanced particle rendering for supported particles (Torex cloudlets, RBMK particles), rendering them several times faster. May break with shaders. [CE: 1.25_instancedParticles]")
                .define("instancedParticles", true);
        DEPTH_BUFFER_EFFECTS = builder
                .comment("Enables effects that make use of reading from the depth buffer. [CE: 1.25_depthBufferEffects]")
                .define("depthBufferEffects", true);
        FLASHLIGHTS = builder
                .comment("Enables dynamic directional lights. [CE: 1.25_flashlights]")
                .define("flashlights", true);
        FLASHLIGHT_VOLUMETRICS = builder
                .comment("Enables volumetric lighting for directional lights. [CE: 1.25_flashlight_volumetrics]")
                .define("flashlightVolumetrics", true);
        BULLET_HOLE_NORMAL_MAPPING = builder
                .comment("Enables normal mapping on bullet holes, which can improve visuals. [CE: 1.25_bullet_hole_normal_mapping]")
                .define("bulletHoleNormalMapping", true);
        FLOWING_DECAL_MAX = builder
                .comment("The maximum number of 'flowing' decals that can exist at once (e.g. blood that flows down walls). [CE: 1.25_flowing_decal_max]")
                .defineInRange("flowingDecalMax", 20, 0, Integer.MAX_VALUE);
        CALL_LIST_MODELS = builder
                .comment("Enables call lists for a few models, making them render extremely fast. [CE: 1.26_callListModels]")
                .define("callListModels", true);
        ENABLE_REFLECTOR_COMPAT = builder
                .comment("Enable the old reflector oredict name (\"plateDenseLead\") instead of \"plateTungCar\". [CE: 1.24_enableReflectorCompat]")
                .define("enableReflectorCompat", false);
        ENABLE_COAL_DUST = builder
                .comment("Allows the coal gas to spawn (e.g. after breaking coal ore). [CE: 1.26_enableCoalDust]")
                .define("enableCoalDust", true);
        ENABLE_ASBESTOS_DUST = builder
                .comment("Allows the asbestos gas to spawn (e.g. after breaking asbestos ore or chrysotile). [CE: 1.26_enableAsbestosDust]")
                .define("enableAsbestosDust", true);
        ENABLE_RADON_GAS = builder
                .comment("Allows the radon gas to spawn (e.g. after breaking uranium ore). [CE: 1.26_enableRadonGas]")
                .define("enableRadonGas", true);
        ENABLE_CARBON_MONOXIDE = builder
                .comment("Allows the carbon monoxide gas to spawn (e.g. after breaking nether coal ore). [CE: 1.26_enableCarbonMonoxide]")
                .define("enableCarbonMonoxide", true);
        ENABLE_FLAMMABLE_GAS = builder
                .comment("Allows the flammable gas to spawn in the world. [CE: 1.26_enableFlammableGas]")
                .define("enableFlammableGas", true);
        ENABLE_EXPLOSIVE_GAS = builder
                .comment("Allows the explosive gas to spawn in the world. [CE: 1.26_enableExplosiveGas]")
                .define("enableExplosiveGas", true);
        ENABLE_MELTDOWN_GAS = builder
                .comment("Allows the meltdown gas to spawn (e.g. after ZIRNOX explosion). [CE: 1.26_enableMeltdownGas]")
                .define("enableMeltdownGas", true);
        ENABLE_RE_EVAL = builder
                .comment("Allows re-evaluating power networks on link remove instead of destroying and recreating. [CE: 1.27_enableReEval]")
                .define("enableReEval", true);
        ENABLE_RECIPES = builder
                .comment("A general switch for ALL crafting table/smelting recipes. If false, all recipes are disabled. [CE: 1.28_enableRecipes]")
                .define("enableRecipes", true);
        REGISTER_TANKS = builder
                .comment("A general switch for ALL tank items in the mod (universal fluid, lead, barrels, packed containers). If false, they aren't registered as items. [CE: 1.28_registerTanks]")
                .define("registerTanks", true);
        ENABLE_JEI = builder
                .comment("Enables JEI compatibility. [CE: 1.28_enableJei]")
                .define("enableJei", true);
        ENABLE_CHANGELOG = builder
                .comment("Enables the update notification in the chat. [CE: 1.28_enableChangelog]")
                .define("enableChangelog", true);
        ENABLE_DUCK_BUTTON = builder
                .comment("Allows you to summon the duck via pressing O. [CE: 1.28_enableDuckButton]")
                .define("enableDuckButton", true);
        ENABLE_BLOOM = builder
                .comment("Enables the bloom effect visible on the Crucible. Only active if enableShaders2 is true. [CE: 1.30_enableBloom]")
                .define("enableBloom", true);
        ENABLE_HEAT_DISTORTION = builder
                .comment("Enables the heat distortion effect. Only active if enableShaders2 is true. [CE: 1.30_enableHeatDistortion]")
                .define("enableHeatDistortion", true);
        ENABLE_ADVANCED_RADIATION = builder
                .comment("Enables a 3-dimensional version of the radiation system that also allows some blocks (like concrete bricks) to stop it from spreading. [CE: 1.31_enableAdvancedRadiation]")
                .define("enableAdvancedRadiation", true);
        ENABLE_IMPACT_WORLD_PROVIDER = builder
                .comment("If enabled, registers a custom overworld provider which modifies lighting and sky colors for post-impact effects. [CE: 1.32_enableImpactWorldProvider]")
                .define("enableImpactWorldProvider", true);
        ENABLE_BLOOD_EFFECTS = builder
                .comment("Enables the over-the-top blood visual effects for some weapons. [CE: 1.32_enable_blood_effects]")
                .define("enableBloodEffects", true);
        CRUCIBLE_MAX_CHARGES = builder
                .comment("How many times you can use the crucible before recharge. [CE: 1.33_crucible_max_charges]")
                .defineInRange("crucibleMaxCharges", 16, 1, Integer.MAX_VALUE);
        CONVERSION_RATE_HE_TO_RF = builder
                .comment("One HE is <value> RF. [CE: 1.35_conversionRateHeToRF]")
                .defineInRange("conversionRateHeToRF", 1.0D, 0.0D, Double.MAX_VALUE);
        AUTO_CABLE_CONVERSION = builder
                .comment("If enabled, NTM cables will automatically convert FE <-> HE. Note: makes all other mods' cables useless for NTM power. [CE: 1.35.1_autoCableConversion]")
                .define("autoCableConversion", true);
        ENABLE_MOTD = builder
                .comment("If enabled, shows the 'Loaded mod!' chat message as well as update notifications when joining a world. [CE: 1.36_enableMOTD]")
                .define("enableMOTD", true);
        ENABLE_FLUID_CONTAINER_COMPAT = builder
                .comment("If enabled, fluid containers will be oredicted and interchangeable in recipes with other mods' containers. [CE: 1.37_enableFluidContainerCompat]")
                .define("enableFluidContainerCompat", true);
        ENABLE_GUIDE_BOOK = builder
                .comment("If enabled, gives players the guide book when joining the world for the first time. [CE: 1.38_enableGuideBook]")
                .define("enableGuideBook", true);
        DECO_TO_INGOT_CONVERSION_RATE = builder
                .comment("Chance (percent) of successfully turning a deco block into an ingot. [CE: 1.39_decoToIngotConversionRate]")
                .defineInRange("decoToIngotConversionRate", 25, 0, 100);
        THREADED_ATMOSPHERES = builder
                .comment("If enabled, will run atmosphere blobbing in a separate thread for performance. [CE: 1.40_threadedAtmospheres]")
                .define("threadedAtmospheres", true);
        ENABLE_KEYBIND_OVERLAP = builder
                .comment("If enabled, will handle keybinds that would otherwise be ignored due to overlapping. [CE: 1.42_enableKeybindOverlap]")
                .define("enableKeybindOverlap", true);
        ENABLE_MACHINE_GRAVITY = builder
                .comment("Requires large machines to have a proper foundation, or else they tilt and break. Independent from the 528 version of this config. [CE: 1.44_enableMachineGravity]")
                .define("enableMachineGravity", false);
        ENABLE_FLUID_CONTAINERS_V2 = builder
                .comment("If enabled, adds 3 new enhanced fluid barrels that support partial fill and drain. [CE: 1.99_CE_enableFluidContainersV2]")
                .define("enableFluidContainersV2", false);
        DYNAMIC_TREES_COMPAT_MODE = builder
                .comment("Prevents HBM from re-enabling tree, big shroom and cactus generation that was disabled by Dynamic Trees. [CE: 1.67_dynamicTreesCompatMode]")
                .define("dynamicTreesCompatMode", false);
        ENABLE_EXPENSIVE_MODE = builder
                .comment("It does what the name implies. [CE: 1.99_enableExpensiveMode]")
                .define("enableExpensiveMode", false);

        builder.pop();

        builder.comment(
                "CAUTION",
                "528 Mode: Please proceed with caution!",
                "528-Modus: Lassen Sie Vorsicht walten!",
                "способ-528: действовать с осторожностью!"
        ).push("528");

        ENABLE_528 = builder
                .comment("The central toggle for 528 mode. [CE: enable528Mode]")
                .define("enable528Mode", false);
        X528_FORCE_REASIM_BOILERS = builder
                .comment("Keeps the RBMK dial for ReaSim boilers on, preventing use of non-ReaSim boiler columns and forcing the use of steam in-/outlets. [CE: X528_forceReasimBoilers]")
                .define("forceReasimBoilers", true);
        X528_ENABLE_COLTAN_DEPOSIT = builder
                .comment("Enables the coltan deposit. A large amount of coltan will spawn around a single random location in the world. Gates the rich coltan deposit in docs/phase4/ore_veins_and_bedrock_ores.md Group B. [CE field: enable528ColtanDeposit, toml key: X528_enableColtanDepsoit]")
                .define("enableColtanDeposit", true);
        X528_ENABLE_COLTAN_SPAWNING = builder
                .comment("Enables coltan ore as a random spawn in the world. Unlike the deposit option, coltan will not just spawn in one central location. Gates the ordinary ore_coltan vein in docs/phase4/ore_veins_and_bedrock_ores.md Group A. [CE field: enable528ColtanSpawn, toml key: X528_enableColtanSpawning]")
                .define("enableColtanSpawning", false);
        X528_ENABLE_BOSNIA_SIMULATOR = builder
                .comment("Enables anti tank mines spawning all over the world. [CE: X528_enableBosniaSimulator]")
                .define("enableBosniaSimulator", true);
        X528_ENABLE_NETHER_BURN = builder
                .comment("Whether players burn in the nether. [CE: X528_enable528NetherBurn]")
                .define("enableNetherBurn", true);
        X528_ENABLE_PRESSURIZED_RECIPES = builder
                .comment("Sets some recipes to require pressurized input fluid. [CE: X528_enable528PressurizedRecipes]")
                .define("enablePressurizedRecipes", true);
        X528_ENABLE_EXPLOSIVE_ENERGISTICS = builder
                .comment("Renders AE2 unusable. [CE: X528_enable528ExplosiveEnergistics]")
                .define("enableExplosiveEnergistics", true);
        X528_ENABLE_MACHINE_GRAVITY = builder
                .comment("Requires most large machines to have a proper foundation, or else they tilt and break. [CE: X528_enable528MachineGravity]")
                .define("enableMachineGravity", true);
        X528_ORE_COLTAN_FREQUENCY = builder
                .comment("How many coltan ore veins are to be expected in a chunk. Only applies if random coltan spawning is enabled. [CE field: coltanRate, toml key: X528_oreColtanFrequency]")
                .defineInRange("oreColtanFrequency", 2, 0, Integer.MAX_VALUE);

        builder.pop();

        builder.comment(
                "Will most likely break standard progression!",
                "However, the game gets generally easier and more enjoyable for casual players.",
                "Progression-breaking recipes are usually not too severe, so the mode is generally server-friendly!"
        ).push("less_bullshit_mode");

        ENABLE_LBSM = builder
                .comment("The central toggle for LBS mode. Forced OFF when 528 is enabled! [CE: enableLessBullshitMode]")
                .define("enableLessBullshitMode", false);
        LBSM_FULL_SCHRAB = builder
                .comment("Replaces schraranium with full schrabidium ingots in the transmutator's output. [CE: LBSM_fullSchrab]")
                .define("fullSchrab", true);
        LBSM_SHORT_DECAY = builder
                .comment("Highly accelerates the speed at which nuclear waste disposal drums decay their contents (60x faster than 528 mode, 5-12x faster than normal). [CE: LBSM_shortDecay]")
                .define("shortDecay", true);
        LBSM_RECIPE_SIMPLE_ARMOR = builder
                .comment("Simplifies the recipe for armor sets like starmetal or schrabidium. [CE: LBSM_recipeSimpleArmor]")
                .define("recipeSimpleArmor", true);
        LBSM_RECIPE_SIMPLE_TOOL = builder
                .comment("Simplifies the recipe for tool sets like starmetal or schrabidium. [CE: LBSM_recipeSimpleTool]")
                .define("recipeSimpleTool", true);
        LBSM_RECIPE_SIMPLE_ALLOY = builder
                .comment("Adds some blast furnace recipes to make certain things cheaper. [CE: LBSM_recipeSimpleAlloy]")
                .define("recipeSimpleAlloy", true);
        LBSM_RECIPE_SIMPLE_CHEMISTRY = builder
                .comment("Simplifies some chemical plant recipes. [CE: LBSM_recipeSimpleChemistry]")
                .define("recipeSimpleChemistry", true);
        LBSM_RECIPE_SIMPLE_CENTRIFUGE = builder
                .comment("Enhances centrifuge outputs to make rare materials more common. [CE: LBSM_recipeSimpleCentrifuge]")
                .define("recipeSimpleCentrifuge", true);
        LBSM_RECIPE_UNLOCK_ANVIL = builder
                .comment("All anvil recipes are available at tier 1. [CE: LBSM_recipeUnlockAnvil]")
                .define("recipeUnlockAnvil", true);
        LBSM_RECIPE_SIMPLE_CRAFTING = builder
                .comment("Some uncraftable or more expensive items get simple crafting recipes. Scorched uranium also becomes washable. [CE: LBSM_recipeSimpleCrafting]")
                .define("recipeSimpleCrafting", true);
        LBSM_RECIPE_SIMPLE_MEDICINE = builder
                .comment("Makes some medicine recipes (like ones that require bismuth) much more affordable. [CE: LBSM_recipeSimpleMedicine]")
                .define("recipeSimpleMedicine", true);
        LBSM_SAFE_CRATES = builder
                .comment("Prevents crates from becoming radioactive. [CE: LBSM_safeCrates]")
                .define("safeCrates", true);
        LBSM_SAFE_ME_DRIVES = builder
                .comment("Prevents ME Drives and Portable Cells from becoming radioactive. [CE: LBSM_safeMEDrives]")
                .define("safeMEDrives", true);
        LBSM_IGEN = builder
                .comment("Restores the industrial generator to pre-nerf power. [CE: LBSM_iGen]")
                .define("iGen", true);

        builder.pop();
    }

    /** Mirrors CE's {@code GeneralConfig.enable528}. */
    public static boolean enable528() {
        return ENABLE_528.get();
    }

    /**
     * Mirrors CE's {@code GeneralConfig.true528()}: whether every 528 sub-feature is enabled in
     * its "canonical" 528 configuration, used to gate content that only makes sense when 528 mode
     * is fully, un-tweaked, enabled.
     */
    public static boolean true528() {
        return ENABLE_528.get()
                && X528_FORCE_REASIM_BOILERS.get()
                && !X528_ENABLE_COLTAN_SPAWNING.get()
                && X528_ENABLE_BOSNIA_SIMULATOR.get()
                && X528_ENABLE_NETHER_BURN.get()
                && X528_ENABLE_PRESSURIZED_RECIPES.get()
                && X528_ENABLE_EXPLOSIVE_ENERGISTICS.get()
                && X528_ENABLE_MACHINE_GRAVITY.get()
                && X528_ORE_COLTAN_FREQUENCY.get() <= 2;
    }

    /**
     * Mirrors CE's post-load {@code if(enable528) enableLBSM = false;} mutation as a derived
     * getter instead of mutating the stored config value (528 always wins over LBSM).
     */
    public static boolean enableLBSM() {
        return ENABLE_LBSM.get() && !ENABLE_528.get();
    }
}
