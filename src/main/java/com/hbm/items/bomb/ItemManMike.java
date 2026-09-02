package com.hbm.items.bomb;

import com.hbm.items.special.SpecialItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Port of CE {@code com.hbm.items.bomb.ItemManMike} - special tooltip for man_core/demon_core items.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/items/bomb/ItemManMike.java
 * <p>
 * Shows "Used in: nuke_man, nuke_mike, nuke_tsar" tooltip (CE :21-28).
 */
public class ItemManMike extends Item {

    public ItemManMike(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        // CE :22-27: "Used in:" + list of nukes
        tooltipComponents.add(Component.translatable("desc.usedin").withStyle(ChatFormatting.YELLOW));
        
        if (this == SpecialItems.MAN_CORE.get()) {
            tooltipComponents.add(Component.literal(" ").append(Component.translatable("tile.nuke_man.name")).withStyle(ChatFormatting.GRAY));
        }
        tooltipComponents.add(Component.literal(" ").append(Component.translatable("tile.nuke_mike.name")).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal(" ").append(Component.translatable("tile.nuke_tsar.name")).withStyle(ChatFormatting.GRAY));
        
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
