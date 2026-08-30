package com.hbm.items.machine;

import com.hbm.items.ItemBase;

/**
 * Chemical dye / crayon. CE rendered these with a shared grayscale base texture and a runtime
 * client-side tint (one {@code IItemColor} keyed off metadata) for its 16 colors, across two base
 * items ({@code chemical_dye}, {@code crayon}). Each (base item, color) pair is now its own
 * registered item, so the runtime tint machinery is no longer needed - the 32 resulting items each
 * ship their own colored texture; {@link EnumChemDye#color} is kept only as descriptive/lore data
 * (recipe lookups, tooltips), not as a render-time tint source.
 */
public class ItemChemicalDye extends ItemBase {

    private final EnumChemDye dye;

    public ItemChemicalDye(EnumChemDye dye, Properties properties) {
        super(properties);
        this.dye = dye;
    }

    public EnumChemDye getDye() {
        return this.dye;
    }

    public enum EnumChemDye {
        BLACK(1973019, "Black"),
        RED(11743532, "Red"),
        GREEN(3887386, "Green"),
        BROWN(5320730, "Brown"),
        BLUE(2437522, "Blue"),
        PURPLE(8073150, "Purple"),
        CYAN(2651799, "Cyan"),
        SILVER(11250603, "LightGray"),
        GRAY(4408131, "Gray"),
        PINK(14188952, "Pink"),
        LIME(4312372, "Lime"),
        YELLOW(14602026, "Yellow"),
        LIGHTBLUE(6719955, "LightBlue"),
        MAGENTA(12801229, "Magenta"),
        ORANGE(15435844, "Orange"),
        WHITE(15790320, "White");

        public static final EnumChemDye[] VALUES = values();

        public final int color;
        public final String dictName;

        EnumChemDye(int color, String name) {
            this.color = color;
            this.dictName = name;
        }
    }
}
