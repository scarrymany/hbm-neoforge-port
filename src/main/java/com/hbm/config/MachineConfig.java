package com.hbm.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Port of CE's {@code MachineConfig}. Registered into {@link HbmConfig}'s COMMON spec.
 * <p>
 * {@code doorConf} is kept as a raw {@code "modid:door_name:MODE"} / {@code "ALL:MODE"} string
 * list, parsed into a plain {@code Map<String, String>} of mode names rather than
 * {@code com.hbm.interfaces.IDoor.Mode}: that interface is owned by a different area and isn't
 * ported yet. Whoever ports the door/machine framework should call {@link #doorConf()} and
 * resolve the string values against {@code IDoor.Mode.valueOf(...)}.
 */
public class MachineConfig {

    private static final String[] VALID_DOOR_MODES = { "DEFAULT", "TOOLABLE", "REDSTONE" };

    public static BooleanValue SCALE_RTG_POWER;
    public static BooleanValue DO_RTGS_DECAY;
    public static BooleanValue DISABLE_MACHINES;
    public static BooleanValue HOLD_DOOR_REDSTONE;
    public static IntValue CRATE_BYTE_SIZE;
    public static ConfigValue<List<? extends String>> DOOR_CONF_RAW;

    static void init(ModConfigSpec.Builder builder) {
        builder.push("machines");

        SCALE_RTG_POWER = builder
                .comment("Should RTG/Betavoltaic fuel power scale down as it decays? [CE: 9.01_scaleRTGPower]")
                .define("scaleRTGPower", false);
        DO_RTGS_DECAY = builder
                .comment("Should RTG/Betavoltaic fuel decay at all? [CE: 9.02_doRTGsDecay]")
                .define("doRTGsDecay", true);
        DISABLE_MACHINES = builder
                .comment("Prevent the mod from registering any machines? (WARNING: THIS WILL BREAK PREEXISTING WORLDS). [CE: 9.00_disableMachines]")
                .define("disableMachines", false);
        HOLD_DOOR_REDSTONE = builder
                .comment("Whether doors require a continuous redstone signal to stay open, or toggle on each activation. [CE: 9.99_CE_03_holdDoorRedstone]")
                .define("holdDoorRedstone", false);
        CRATE_BYTE_SIZE = builder
                .comment("Maximum compressed data size, in bytes, that a crate or lead box may carry as an item. Contents exceeding this are dropped instead of stored. [CE: 9.99_CE_04_crateByteSize]")
                .defineInRange("crateByteSize", 8192, 0, Integer.MAX_VALUE);
        DOOR_CONF_RAW = builder
                .comment("Configuration for door modes. Format: 'modid:door_name:MODE' (e.g. 'hbm:vault_door:REDSTONE') or 'ALL:MODE'. Modes: DEFAULT, TOOLABLE, REDSTONE. [CE: 9.99_CE_02_doorConf]")
                .defineListAllowEmpty("doorConf", () -> List.<String>of(), entry -> entry instanceof String);

        builder.pop();
    }

    /** Parses {@link #DOOR_CONF_RAW} into a map of "ALL" or "modid:door_name" -> mode name. */
    public static Map<String, String> doorConf() {
        Map<String, String> result = new HashMap<>();
        for (String entry : DOOR_CONF_RAW.get()) {
            if (entry == null) continue;
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) continue;

            String[] split = trimmed.split(":");
            if (split.length == 2 && "ALL".equalsIgnoreCase(split[0])) {
                putIfValidMode(result, "ALL", split[1]);
            } else if (split.length == 3) {
                putIfValidMode(result, split[0] + ":" + split[1], split[2]);
            }
        }
        return result;
    }

    private static void putIfValidMode(Map<String, String> result, String key, String mode) {
        String upperMode = mode.toUpperCase(Locale.ROOT);
        for (String valid : VALID_DOOR_MODES) {
            if (valid.equals(upperMode)) {
                result.put(key, upperMode);
                return;
            }
        }
    }
}
