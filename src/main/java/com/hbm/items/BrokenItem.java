package com.hbm.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Item wrapping another item stack, used to represent a "broken" version of whatever it wraps.
 *
 * CE identified the wrapped item via two NBT keys, "itemID" (registry name string) and
 * "itemMeta" (damage value), and reconstructed an ItemStack from them on demand. 1.13's item
 * flattening removed meta-as-variant entirely, so instead of two loose NBT primitives this
 * stores the actual wrapped ItemStack (count normalized to 1) in the hbm:wrapped_item data
 * component (see HbmDataComponents.WRAPPED_ITEM).
 *
 * CE's client-side baked-model overlay compositing (BrokenItemModel / BrokenItemOverrideList /
 * CompositeModel, driven by ModelBakeEvent) is deliberately NOT ported: 1.21 renders items from
 * data-driven item model JSON (select/composite conditions keyed off components) instead of
 * runtime model baking, and wiring hbm:wrapped_item into such a model definition is a
 * client-rendering concern for a later phase, not this item class.
 */
public class BrokenItem extends ItemBase {

    public BrokenItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        ItemStack wrapped = stack.get(HbmDataComponents.WRAPPED_ITEM.get());
        if (wrapped == null || wrapped.isEmpty()) return super.getName(stack);

        return Component.translatable(this.getDescriptionId(stack) + ".prefix", wrapped.getHoverName());
    }

    /**
     * Wraps a copy of the given stack (count 1) into a broken_item stack, preserving the
     * original stack's count as the broken_item's own count - matching CE's make(ItemStack).
     *
     * Depends on SpecialItems.BROKEN_ITEM (registered as {@code hbm:broken_item}).
     */
    public static ItemStack make(ItemStack stack) {
        return make(stack, stack.getCount());
    }

    public static ItemStack make(Item item) {
        return make(new ItemStack(item), 1);
    }

    public static ItemStack make(ItemStack stack, int stackSize) {
        ItemStack broken = new ItemStack(com.hbm.items.special.SpecialItems.BROKEN_ITEM.get(), stackSize);
        broken.set(HbmDataComponents.WRAPPED_ITEM.get(), stack.copyWithCount(1));
        return broken;
    }
}
