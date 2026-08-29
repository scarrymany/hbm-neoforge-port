package com.hbm.blocks;

import com.hbm.items.ModItems;
import net.minecraft.world.item.Item;

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

    public enum EnumBasaltOreType {
        SULFUR,
        FLUORITE,
        ASBESTOS,
        GEM,
        MOLYSITE;

        public static final EnumBasaltOreType[] VALUES = values();

        public Item getDrop() {
            return switch (this) {
                case SULFUR -> ModItems.sulfur;
                case FLUORITE -> ModItems.fluorite;
                case ASBESTOS -> ModItems.ingot_asbestos;
                case GEM -> ModItems.gem_volcanic;
                case MOLYSITE -> ModItems.powder_molysite;
            };
        }

        public int getDropCount(int rand) {
            return rand + 1;
        }
    }

    public enum EnumBlockCapType {
        NUKA,
        QUANTUM,
        RAD,
        SPARKLE,
        KORL,
        FRITZ;

        public static final EnumBlockCapType[] VALUES = values();

        public Item getDrop() {
            return switch (this) {
                case NUKA -> ModItems.cap_nuka;
                case QUANTUM -> ModItems.cap_quantum;
                case RAD -> ModItems.cap_rad;
                case SPARKLE -> ModItems.cap_sparkle;
                case KORL -> ModItems.cap_korl;
                case FRITZ -> ModItems.cap_fritz;
            };
        }

        public int getDropCount() {
            return 128;
        }
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
