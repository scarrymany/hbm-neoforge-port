package com.hbm.items.tool;

import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Exact CE {@code com.hbm.items.tool.ItemModDoor}: {@code door_metal}/{@code door_office}/
 * {@code door_bunker}/{@code door_red}, {@code maxStackSize = 1}. CE {@code onItemUse}/{@code
 * placeDoor} ({@code :31-106}) is 1.12 {@code ItemDoor} two-tall place. 1.21 has no
 * {@code DoorItem} — vanilla doors use {@link DoubleHighBlockItem} (lower +
 * {@code DoorBlock#setPlacedBy} upper, hinge/powered). Identity-check {@code this ==
 * ModItems.door_*} is gone — each instance is constructed against its own {@link Block}.
 */
public class ItemModDoor extends DoubleHighBlockItem {

    public ItemModDoor(Block block, Item.Properties properties) {
        super(block, properties.stacksTo(1));
    }
}
