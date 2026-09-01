package com.hbm.items.special;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Port of CE's {@code ItemWasteShort}: short-lived nuclear waste, hand-rolled metadata-multi over
 * {@link WasteClass} (8 constants). Same flattening treatment as {@link ItemWasteLong} - see
 * docs/phase1/items_special.md finding 1 and {@link SpecialItems} for the flattened registration of
 * the {@code nuclear_waste_short} family named in this area's task scope.
 */
public class ItemWasteShort extends Item {

    public enum WasteForm {
        BASE("item.hbm.nuclear_waste_short"),
        TINY("item.hbm.nuclear_waste_short_tiny"),
        DEPLETED("item.hbm.nuclear_waste_short_depleted"),
        DEPLETED_TINY("item.hbm.nuclear_waste_short_depleted_tiny");

        public final String descriptionId;

        WasteForm(String descriptionId) {
            this.descriptionId = descriptionId;
        }
    }

    private final WasteClass wasteClass;
    private final WasteForm form;

    public ItemWasteShort(Properties properties, WasteClass wasteClass) {
        this(properties, wasteClass, WasteForm.BASE);
    }

    public ItemWasteShort(Properties properties, WasteClass wasteClass, WasteForm form) {
        super(properties);
        this.wasteClass = wasteClass;
        this.form = form;
    }

    public WasteClass getWasteClass() {
        return wasteClass;
    }

    public WasteForm getForm() {
        return form;
    }

    @Override
    public String getDescriptionId() {
        return form.descriptionId;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(wasteClass.displayName).withStyle(ChatFormatting.ITALIC));
    }

    /** All decayed versions include lead-types and classic nuclear waste. */
    public enum WasteClass {
        URANIUM233("Uranium-233", 50, 100),
        URANIUM235("Uranium-235", 0, 100),
        NEPTUNIUM("Neptunium-237", 150, 500),
        PLUTONIUM239("Plutonium-239", 250, 1000),
        PLUTONIUM240("Plutonium-240", 350, 1000),
        PLUTONIUM241("Plutonium-241", 500, 1000),
        AMERICIUM242("Americium-242", 750, 1000),
        SCHRABIDIUM("Schrabidium-326", 1000, 1000);

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
