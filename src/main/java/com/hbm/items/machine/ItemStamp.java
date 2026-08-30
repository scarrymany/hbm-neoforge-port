package com.hbm.items.machine;

import com.hbm.items.ItemBase;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Die item for a (Phase 2) press machine. Only self-registers into a static lookup table for that
 * future machine to query - no tile entity reference of its own.
 */
public class ItemStamp extends ItemBase {

    /** CE's {@code stamps}: stamp type -> every registered stack that can produce it. */
    public static final Map<StampType, List<ItemStack>> STAMPS = new EnumMap<>(StampType.class);

    protected final StampType type;

    public ItemStamp(StampType type, Properties properties) {
        super(properties.stacksTo(1));
        this.type = type;
        if (type != null) addStampToList(this, type);
    }

    protected static void addStampToList(Item item, StampType type) {
        STAMPS.computeIfAbsent(type, key -> new ArrayList<>()).add(new ItemStack(item));
    }

    public StampType getStampType() {
        return this.type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int maxDamage = stack.getMaxDamage();
        if (maxDamage > 0 && stack.getDamageValue() == 0) {
            tooltip.add(Component.literal("Durability: " + maxDamage + " / " + maxDamage));
        }
    }

    public enum StampType {
        FLAT,
        PLATE,
        WIRE,
        CIRCUIT,
        C357,
        C44,
        C50,
        C9,
        PRINTING1,
        PRINTING2,
        PRINTING3,
        PRINTING4,
        PRINTING5,
        PRINTING6,
        PRINTING7,
        PRINTING8
    }
}
