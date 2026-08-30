package com.hbm.items.special;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Port of CE's {@code ItemWasteLong}: long-lived nuclear waste, hand-rolled metadata-multi over
 * {@link WasteClass} (5 constants, not {@code ItemEnumMulti}-based - CE used a private enum + manual
 * {@code getSubItems}). Per docs/phase1/items_special.md finding 1, each metadata variant becomes
 * its own registry entry with a fixed {@link WasteClass}; see {@link SpecialItems} for the flattened
 * registration of the {@code nuclear_waste_long} family named in this area's task scope.
 */
public class ItemWasteLong extends Item {

    private final WasteClass wasteClass;

    public ItemWasteLong(Properties properties, WasteClass wasteClass) {
        super(properties);
        this.wasteClass = wasteClass;
    }

    public WasteClass getWasteClass() {
        return wasteClass;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(wasteClass.displayName).withStyle(ChatFormatting.ITALIC));
    }

    /** All decayed versions include lead-types and classic nuclear waste. */
    public enum WasteClass {
        THORIUM("Thorium-232", 0, 0),
        URANIUM233("Uranium-233", 0, 50),
        URANIUM235("Uranium-235", 0, 0),
        NEPTUNIUM("Neptunium-237", 0, 100),
        SCHRABIDIUM("Schrabidium-326", 0, 250);

        public static final WasteClass[] VALUES = values();

        public final String displayName;
        public final int liquid;
        public final int gas;

        WasteClass(String displayName, int liquid, int gas) {
            this.displayName = displayName;
            this.liquid = liquid;
            this.gas = gas;
        }
    }
}
