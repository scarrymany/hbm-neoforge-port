package com.hbm.blocks;

import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Shared "hold shift for more info" tooltip convention, ported from CE. The Mojang-mapped source
 * tree makes CE's original doc comment (warning that the mod interface's {@code addInformation}
 * member and the inherited vanilla block override would collide under MCP/SRG remapping) moot: with
 * official mappings there is no separate obfuscated name to diverge from, so that warning is not
 * carried over. As in CE, this interface is intentionally not called through directly - keep a
 * static type of {@link Block} (or a subclass) so tooltip dispatch stays a normal virtual override.
 * <p>
 * Exact CE {@code ITooltipProvider.java:44-54}: LSHIFT expands {@code getDescriptionId() + ".desc"}
 * via {@link I18nUtil#resolveKeyArray} ({@code $} line breaks).
 */
public interface ITooltipProvider {

    default void addStandardInfo(List<Component> tooltip) {
        // Exact CE ITooltipProvider.java:44-54
        if (com.hbm.client.ClientScreens.hasShiftDown()) {
            for (String s : I18nUtil.resolveKeyArray(((Block) this).getDescriptionId() + ".desc")) {
                tooltip.add(Component.literal(s).withStyle(ChatFormatting.YELLOW));
            }
        } else {
            tooltip.add(Component.literal("Hold <")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
                    .append(Component.literal("LSHIFT").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC))
                    .append(Component.literal("> to display more info").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
        }
    }

    default Rarity getRarity(ItemStack stack) {
        return Rarity.COMMON;
    }
}
