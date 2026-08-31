package com.hbm.items.tool;

import com.hbm.entity.mob.EntityQuackos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Direct port of CE's {@code com.hbm.items.tool.ItemPeas} (44 lines, read in full) - {@link
 * EntityQuackos}'s sole removal path (see {@code docs/phase4/entities_bosses.md}'s Quackos row). Right
 * clicking scans a 50-block radius around the player for {@link EntityQuackos} instances and calls
 * {@link EntityQuackos#despawn()} on each, bypassing the normal death/loot pipeline entirely.
 */
public class ItemPeas extends Item {

    public ItemPeas(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            if (!player.isCreative()) {
                stack.shrink(1);
            }

            for (EntityQuackos quackos : level.getEntitiesOfClass(EntityQuackos.class, player.getBoundingBox().inflate(50))) {
                quackos.despawn();
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("He accepts your offering."));
    }
}
