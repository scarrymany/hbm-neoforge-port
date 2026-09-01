package com.hbm.blocks.machine.pile;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Flatten of CE {@code IBlockMulti} item metas 0/1/2 on {@code pile_device}.
 * Registry: {@code pile_device} / {@code pile_device_1} / {@code pile_device_2}.
 */
public class PileDeviceItem extends BlockItem {

    public final int itemMeta;

    public PileDeviceItem(Block block, int itemMeta, Item.Properties properties) {
        super(block, properties);
        this.itemMeta = itemMeta;
    }

    @Override
    public String getDescriptionId() {
        return "block.hbm.pile_device_" + itemMeta;
    }
}
