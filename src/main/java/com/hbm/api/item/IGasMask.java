package com.hbm.api.item;

import com.hbm.util.ArmorRegistry.HazardClass;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Contract only; how an implementing item stores its installed filter is up to that item.
 * The CE original stored the filter stack and its remaining durability directly in the mask's
 * NBT compound - that storage must become Data Components in this port (see IMPLEMENTATION NOTE
 * below), never raw NBT reads/writes on the mask ItemStack.
 *
 * IMPLEMENTATION NOTE for whoever implements this on a concrete gas mask item: register a
 * DataComponentType&lt;ItemStack&gt; (e.g. hbm:gas_mask_filter) to replace the old "Filter" NBT
 * compound tag, and fold filter damage into that same component (or a companion
 * DataComponentType&lt;Integer&gt;, e.g. hbm:gas_mask_filter_damage) to replace the old "FilterDamage"
 * NBT int tag. installFilter/damageFilter then become component set/update calls on the mask stack.
 */
public interface IGasMask {
    /**
     * Returns a list of HazardClasses which can not be protected against by this mask (e.g. chlorine gas for half masks)
     * @return an empty list if there's no blacklist
     */
    List<HazardClass> getBlacklist(ItemStack stack);

    /**
     * Returns the loaded filter, if there is any
     * @return empty stack if no filter is installed
     */
    @NotNull
    ItemStack getFilter(ItemStack stack);

    /**
     * Checks whether the provided filter can be screwed into the mask, does not take already applied filters into account (those get ejected)
     */
    boolean isFilterApplicable(ItemStack stack, ItemStack filter);

    /**
     * This will write the filter to the stack's data components, it ignores any previously installed filter and won't eject those
     */
    void installFilter(ItemStack stack, ItemStack filter);

    /**
     * Damages the installed filter, if there is one
     */
    void damageFilter(ItemStack stack, int damage);
}
