package com.hbm.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

/**
 * Port of CE's {@code ToolConfig}: veinminer recursion depth/stone/netherrack toggles and the
 * fixed set of tool ability enable flags. Registered into {@link HbmConfig}'s COMMON spec.
 */
public class ToolConfig {

    public static IntValue RECURSION_DEPTH;
    public static BooleanValue RECURSIVE_STONE;
    public static BooleanValue RECURSIVE_NETHERRACK;

    public static BooleanValue ABILITY_HAMMER;
    public static BooleanValue ABILITY_VEIN;
    public static BooleanValue ABILITY_LUCK;
    public static BooleanValue ABILITY_SILK;
    public static BooleanValue ABILITY_FURNACE;
    public static BooleanValue ABILITY_SHREDDER;
    public static BooleanValue ABILITY_CENTRIFUGE;
    public static BooleanValue ABILITY_CRYSTALLIZER;
    public static BooleanValue ABILITY_MERCURY;
    public static BooleanValue ABILITY_EXPLOSION;

    static void init(ModConfigSpec.Builder builder) {
        builder.push("tools");

        RECURSION_DEPTH = builder.comment("Limits veinminer's recursive function. Usually not an issue, but very deep recursion can trip up some server setups. [CE: 11.00_recursionDepth]")
                .defineInRange("recursionDepth", 1000, 0, Integer.MAX_VALUE);
        RECURSIVE_STONE = builder.comment("Determines whether veinminer can break stone. [CE: 11.01_recursionDepth]")
                .define("veinminerBreaksStone", false);
        RECURSIVE_NETHERRACK = builder.comment("Determines whether veinminer can break netherrack. [CE: 11.02_recursionDepth]")
                .define("veinminerBreaksNetherrack", false);

        ABILITY_HAMMER = builder.comment("Allows the AoE ability. [CE: 11.03_hammerAbility]").define("hammerAbility", true);
        ABILITY_VEIN = builder.comment("Allows the veinminer ability. [CE: 11.04_abilityVein]").define("abilityVein", true);
        ABILITY_LUCK = builder.comment("Allows the luck (fortune) ability. [CE: 11.05_abilityLuck]").define("abilityLuck", true);
        ABILITY_SILK = builder.comment("Allows the silk touch ability. [CE: 11.06_abilitySilk]").define("abilitySilk", true);
        ABILITY_FURNACE = builder.comment("Allows the auto-smelter ability. [CE: 11.07_abilityFurnace]").define("abilityFurnace", true);
        ABILITY_SHREDDER = builder.comment("Allows the auto-shredder ability. [CE: 11.08_abilityShredder]").define("abilityShredder", true);
        ABILITY_CENTRIFUGE = builder.comment("Allows the auto-centrifuge ability. [CE: 11.09_abilityCentrifuge]").define("abilityCentrifuge", true);
        ABILITY_CRYSTALLIZER = builder.comment("Allows the auto-crystallizer ability. [CE: 11.10_abilityCrystallizer]").define("abilityCrystallizer", true);
        ABILITY_MERCURY = builder.comment("Allows the mercury touch ability (digging redstone gives mercury). [CE: 11.11_abilityMercury]").define("abilityMercury", true);
        ABILITY_EXPLOSION = builder.comment("Allows the explosion ability. [CE: 11.12_abilityExplosion]").define("abilityExplosion", true);

        builder.pop();
    }
}
