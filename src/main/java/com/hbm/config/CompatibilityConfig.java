package com.hbm.config;

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
 * <b>Scope note:</b> CE's ~60 per-dimension ore/structure/meteor/geyser spawn-rate maps
 * ({@code uraniumSpawn}, {@code radioStructure}, {@code dimensionRad}, {@code peaceDimensions},
 * {@code fillCraterWithWater}, ...) and {@code isWarDim(World)} are intentionally NOT ported
 * here. They're all keyed by CE's integer Forge dimension ID, a concept that no longer exists in
 * 1.21: dimensions are identified by {@code ResourceKey<Level>} now, and CE's numeric keys mostly
 * referred to Galacticraft/ExtraPlanets/other mods' dimension IDs that may not even have a 1.21
 * NeoForge counterpart yet. Re-keying ~60 tables to dimension {@code ResourceLocation}s based on
 * guessed mod compatibility would be worse than not porting them; this belongs to whichever phase
 * owns world generation, once it knows the real set of dimensions and mod compat targets. That
 * phase can reuse {@link ConfigUtil#toIntMap} / {@link ConfigUtil#toFloatMap} for the same
 * "key:value" string-list encoding used below.
 * <p>
 * What IS ported here is everything not keyed by dimension ID: mob radiation resistance/immunity
 * (which is keyed by mod id / entity id, not dimension), mob gear/loot toggles, the bedrock ore
 * oredict blacklist, and the crater water-fill toggle.
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
}
