package com.hbm.world.gen.nbt;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 1.12 CE structure-palette names → 1.21 ids. Vanilla flattenings plus this port's
 * metadata-flattened / suffix-first material blocks. Unknown {@code hbm:*} ids stay as-is
 * (placer falls back to air if unregistered).
 */
public final class StructureBlockRemap {

    private static final Map<String, String> MAP = new HashMap<>();

    static {
        vanilla("grass", "grass_block");
        vanilla("tallgrass", "short_grass");
        vanilla("double_plant", "tall_grass");
        vanilla("log", "oak_log");
        vanilla("leaves", "oak_leaves");
        vanilla("planks", "oak_planks");
        vanilla("wooden_slab", "oak_slab");
        vanilla("double_wooden_slab", "oak_planks");
        vanilla("stone_slab", "smooth_stone_slab");
        vanilla("double_stone_slab", "smooth_stone");
        vanilla("stone_stairs", "cobblestone_stairs");
        vanilla("web", "cobweb");
        vanilla("fence", "oak_fence");
        vanilla("wooden_pressure_plate", "oak_pressure_plate");
        vanilla("wall_sign", "oak_wall_sign");
        vanilla("bed", "red_bed");
        vanilla("skull", "skeleton_skull");
        vanilla("stained_glass", "white_stained_glass");
        vanilla("stained_glass_pane", "white_stained_glass_pane");
        vanilla("stained_hardened_clay", "white_terracotta");
        vanilla("wool", "white_wool");
        vanilla("brick_block", "bricks");
        vanilla("waterlily", "lily_pad");
        vanilla("unlit_redstone_torch", "redstone_torch");
        vanilla("redstone_lamp", "redstone_lamp");

        hbm("deco_crt", "deco_crt_clean");
        hbm("deco_computer", "deco_computer_ibm_300pl");
        hbm("deco_toaster", "deco_toaster_iron");
        hbm("hev_battery", "hev_battery_block");
        hbm("concrete_double_slab", "concrete_slab");
        hbm("concrete_brick_double_slab", "brick_concrete_slab");
        hbm("brick_double_slab", "brick_slab");
        hbm("lightstone", "lightstone_unrefined");
        hbm("concrete_colored", "concrete_white");
        hbm("concrete_colored_ext", "concrete_ext_machine");
    }

    private StructureBlockRemap() {
    }

    public static String remap(String id) {
        if (id == null || id.isEmpty()) return "minecraft:air";
        id = id.toLowerCase(Locale.ROOT);
        if (id.startsWith("!")) id = id.substring(1);
        String mapped = MAP.get(id);
        if (mapped != null) return mapped;
        if (id.endsWith("_double_slab") && id.startsWith("hbm:")) {
            return id.substring(0, id.length() - "_double_slab".length()) + "_slab";
        }
        return id;
    }

    private static void vanilla(String from, String to) {
        MAP.put("minecraft:" + from, "minecraft:" + to);
    }

    private static void hbm(String from, String to) {
        MAP.put("hbm:" + from, "hbm:" + to);
    }
}
