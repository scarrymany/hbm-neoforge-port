package com.hbm.items.tool;

import com.hbm.util.TagsUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Pin-coded key blank, ported from CE's {@code com.hbm.items.tool.ItemKeyPin} (read in full).
 * Targets {@link com.hbm.api.block.ILockable} - this port's own generic interface for the "lockable
 * block entity" concept CE's real target ({@code TileEntityLockableBase}) represents, since no
 * concrete block entity in this port implements it yet (see {@code ILockable}'s own javadoc for why
 * this is real, working, forward-compatible infrastructure rather than a stub). {@link ItemLock}/
 * {@link ItemKey}/{@link ItemCounterfeitKeys} are the other three members of this item family,
 * ported as one unit per {@code docs/phase2/items_tool_machine_coupling_and_recipe_system.md}'s Part
 * A.1 recommendation.
 * <p>
 * Pins are stored via {@link TagsUtil} rather than raw NBT (CE: {@code stack.getTagCompound()}
 * directly) - same idiom as every other not-yet-data-component-migrated per-stack field in this
 * port.
 */
public class ItemKeyPin extends Item {

    public ItemKeyPin(Properties properties) {
        super(properties);
    }

    public static int getPins(ItemStack stack) {
        return TagsUtil.getCustomData(stack).getInt("pins");
    }

    public static void setPins(ItemStack stack, int pins) {
        CompoundTag tag = TagsUtil.getCustomData(stack);
        tag.putInt("pins", pins);
        TagsUtil.putCustomData(stack, tag);
    }

    /** CE: {@code canTransfer} - {@code false} only for the universal {@code key_fake} counterfeit key. Overridden to always transfer here since counterfeits are a separate {@link ItemKey} instance in this port, not an identity check on this class. */
    public boolean canTransfer() {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int pins = getPins(stack);
        if (pins != 0) {
            tooltip.add(Component.literal("Pins: " + pins));
        } else {
            tooltip.add(Component.literal("No pins cut yet"));
        }
    }
}
