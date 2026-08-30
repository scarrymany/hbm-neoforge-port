package com.hbm.items.tool;

import com.hbm.items.ItemBase;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Consumable durability-repair item. Ported from CE's {@code com.hbm.items.tool.ItemRepairKit}
 * ({@code gun_kit_1}/{@code gun_kit_2}).
 *
 * <p><b>Stubbed pending {@code ConsumableHandler}.</b> CE's {@code onItemRightClick} delegates
 * entirely to {@code com.hbm.handler.ConsumableHandler.handleItemUse}, a generic
 * "consume this item to repair/refill whatever's in the other hand" dispatcher. No
 * {@code ConsumableHandler} (or {@code com.hbm.items.special.ItemConsumable} equivalent dispatch
 * table) exists in this port yet - per the port plan's "stub with a documented TODO rather than
 * blocking" rule, the item is registered with its own durability (matching CE's {@code dura - 1}
 * max-damage convention) but the repair interaction itself is deferred.
 */
public class ItemRepairKit extends ItemBase {

    public ItemRepairKit(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // TODO(cross-area follow-up): once ConsumableHandler exists, delegate to
        // ConsumableHandler.handleItemUse(level, player, hand, this) as CE does.
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }
}
