package com.hbm.blocks;

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
 * CE resolved the "hold shift" detail text via a bespoke {@code I18nUtil.resolveKeyArray}, splitting
 * one description into several lang-file lines. That utility does not exist yet in this port, so the
 * expanded branch here uses a single {@link Component#translatable} call instead; a later phase can
 * reintroduce multi-line resolution once the util package lands.
 */
public interface ITooltipProvider {

    default void addStandardInfo(List<Component> tooltip) {
        if (com.hbm.client.ClientScreens.hasShiftDown()) {
            tooltip.add(Component.translatable(((Block) this).getDescriptionId() + ".desc").withStyle(ChatFormatting.YELLOW));
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
