package com.hbm.blocks;

public class BlockEnums {

    public enum EnumStoneType {
        SULFUR,
        ASBESTOS,
        HEMATITE,
        MALACHITE,
        LIMESTONE,
        BAUXITE;

        public static final EnumStoneType[] VALUES = values();
    }

    public enum EnumMeteorType {
        IRON,
        COPPER,
        ALUMINIUM,
        RAREEARTH,
        COBALT;

        public static final EnumMeteorType[] VALUES = values();
    }

    public enum EnumStalagmiteType {
        SULFUR,
        ASBESTOS;

        public static final EnumStalagmiteType[] VALUES = values();
    }

    public enum EnumCMMaterials {
        STEEL,
        ALLOY,
        DESH,
        TCALLOY;

        public static final EnumCMMaterials[] VALUES = values();
    }

    public enum EnumCMEngines {
        STANDARD,
        DESH,
        BISMUTH;

        public static final EnumCMEngines[] VALUES = values();
    }

    public enum EnumCMCircuit {
        ALUMINIUM,
        COPPER,
        RED_COPPER,
        GOLD,
        SCHRABIDIUM;

        public static final EnumCMCircuit[] VALUES = values();
    }

    /** DECO / STRUCTURE ENUMS */

    public enum TileType {
        LARGE,
        SMALL;

        public static final TileType[] VALUES = values();
    }

    public enum LightstoneType {
        UNREFINED,
        TILE,
        BRICKS,
        BRICKS_CHISELED,
        CHISELED;

        public static final LightstoneType[] VALUES = values();
    }

    public enum DecoComputerEnum {
        IBM_300PL;

        public static final DecoComputerEnum[] VALUES = values();
    }

    public enum DecoCabinetEnum {
        GREEN,
        STEEL;

        public static final DecoCabinetEnum[] VALUES = values();
    }

    public enum DecoCRTEnum {
        CLEAN,
        BROKEN,
        BLINKING,
        BSOD;

        public static final DecoCRTEnum[] VALUES = values();
    }

    public enum DecoToasterEnum {
        IRON,
        STEEL,
        WOOD;

        public static final DecoToasterEnum[] VALUES = values();
    }

    /**
     * CE paired each entry with an {@code OreEnumUtil.OreEnum} constant to drive ore block drops.
     * That catalog is Phase 1 content (deferred alongside {@code ModItems}, see {@link OreEnumUtil}),
     * so only the overlay-texture identity survives here; the drop-function wiring is reintroduced
     * once {@code OreEnumUtil.OreEnum} exists.
     */
    public enum OreType {
        EMERALD("emerald"),
        DIAMOND("diamond"),
        RADGEM("radgem"),
        URANIUM("uranium"),
        SCHRABIDIUM("schrabidium");

        public static final OreType[] VALUES = values();

        public final String overlayTexture;

        public String getName() {
            return overlayTexture;
        }

        OreType(String overlayTexture) {
            this.overlayTexture = overlayTexture;
        }
    }

    /**
     * {@code getDrop()}/{@code getDropCount(int)} companions (CE: {@code sulfur}/{@code fluorite}/
     * {@code ingot_asbestos}/{@code gem_volcanic}/{@code powder_molysite}) are intentionally not
     * reintroduced here: {@code ModItems} carries no plain per-element fields for this catalog
     * (real registry homes are {@code PlateCrystalWasteItems}/{@code BilletPowderItems}/
     * {@code IngotNuggetItems}, wired directly by {@code OreBlocks}'s own drop tables), and block
     * drops are loot-table content in modern Minecraft regardless - there is no Java
     * {@code getDrops} hook left to hang a per-enum-constant item mapping on.
     */
    public enum EnumBasaltOreType {
        SULFUR,
        FLUORITE,
        ASBESTOS,
        GEM,
        MOLYSITE;

        public static final EnumBasaltOreType[] VALUES = values();
    }

    /**
     * {@code getDrop()}/{@code getDropCount()} companions (CE: {@code cap_nuka}/{@code cap_quantum}/
     * {@code cap_rad}/{@code cap_sparkle}/{@code cap_korl}/{@code cap_fritz}) are intentionally not
     * reintroduced here: those fields are private to {@code items.food.FoodItems} (and CE's own
     * {@code cap_fritz} was never actually constructed - see that class's javadoc), so there is no
     * public per-element item to map to from this package; block drops are loot-table content in
     * modern Minecraft regardless.
     */
    public enum EnumBlockCapType {
        NUKA,
        QUANTUM,
        RAD,
        SPARKLE,
        KORL,
        FRITZ;

        public static final EnumBlockCapType[] VALUES = values();
    }

    public enum LightType {
        INCANDESCENT,
        FLUORESCENT,
        HALOGEN
    }

    public enum EnumBiomeType {
        DESERT,
        WOODLAND;

        public static final EnumBiomeType[] VALUES = values();
    }

    public enum PlatemetalType {
        BASE,
        BLACK,
        WHITE,
        RED,
        GREEN,
        LIGHT_GRAY,
        BLUE,
        PURPLE,
        CYAN,
        PINK,
        LIME,
        YELLOW,
        LIGHT_BLUE,
        MAGENTA,
        ORANGE;

        public static final PlatemetalType[] VALUES = values();
    }
}
