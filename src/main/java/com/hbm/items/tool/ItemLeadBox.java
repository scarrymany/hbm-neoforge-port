package com.hbm.items.tool;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Port of CE's {@code com.hbm.items.tool.ItemLeadBox} ({@code containment_box}): a 20-slot
 * lead-lined containment box viewable via right-click, backed by CE's
 * {@code InventoryLeadBox}/{@code ContainerLeadBox}/{@code GUILeadBox}. Follows the
 * {@link com.hbm.items.special.ItemBook} shell pattern - see that class's javadoc - since no generic
 * item-owned-inventory Menu/Screen framework exists in this port yet. CE's async NBT-size guard
 * (ejecting the box's contents if its serialized NBT would exceed
 * {@code MachineConfig.crateByteSize}) is a safeguard against an item-owned-inventory serialization
 * problem that does not apply until this item actually carries an inventory, so it is deferred
 * alongside the inventory itself rather than ported in isolation.
 */
public class ItemLeadBox extends Item {

    public ItemLeadBox(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("A lead-lined box for containing hazardous materials."));
    }

    // Menu-opening interaction deferred - see class javadoc. No use() override until a
    // MenuProvider/Screen equivalent of CE's ContainerLeadBox/GUILeadBox exists.
}
