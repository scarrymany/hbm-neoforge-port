package com.hbm.items.tool;

import com.hbm.inventory.container.LemegetonMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Port of CE's {@code com.hbm.items.tool.ItemBookLemegeton} ({@code book_lemegeton}):
 * right-click opens {@code ContainerLemegeton}/{@code GUILemegeton}.
 */
public class ItemBookLemegeton extends Item {

    public ItemBookLemegeton(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Ars Goetia - the Lesser Key of Solomon."));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, ply) -> new LemegetonMenu(id, inv),
                    Component.translatable("container.hbm.lemegeton")));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
