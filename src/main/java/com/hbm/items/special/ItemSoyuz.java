package com.hbm.items.special;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Port of CE's {@code ItemSoyuz}: an {@code ItemEnumMulti}-style multi-metadata item over 3
 * {@link SoyuzSkinType} skins. Per docs/phase1/items_special.md finding 1, this flattens into one
 * registry entry per skin (see {@link SpecialItems}) instead of a single metadata-multiplexed item,
 * the same treatment as {@link ItemWasteLong}/{@link ItemSiegeCoin} elsewhere in this package. CE's
 * {@code getRarity} override (COMMON/RARE/EPIC per skin) has no per-instance hook left on modern
 * {@code Item} - each flattened registration bakes the matching rarity into its own
 * {@code Item.Properties.rarity(...)} instead, the same modern-API substitution
 * {@link ItemModRecord} uses for its own {@code getRarity} override.
 * <p>
 * Not ported: the actual Soyuz rocket placement/launch/staging behavior. No rail or rocket entity
 * system has been ported through Phase 1 (see {@link ItemTrain}'s javadoc for the same dependency),
 * so there is nothing yet for this item to place or launch. Registers as a plain shell item with
 * only the skin tooltip - which carries no such dependency - kept faithful; placement/use behavior
 * is deferred to whichever later phase ports the rail/rocket subsystem.
 */
public class ItemSoyuz extends Item {

    private final SoyuzSkinType skin;

    public ItemSoyuz(Properties properties, SoyuzSkinType skin) {
        super(properties);
        this.skin = skin;
    }

    public SoyuzSkinType getSkin() {
        return skin;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Skin:"));
        switch (skin) {
            case NORMAL -> tooltip.add(Component.literal(ChatFormatting.GOLD + "Original"));
            case LUNAR -> tooltip.add(Component.literal(ChatFormatting.BLUE + "Luna Space Center"));
            case POST_WAR -> tooltip.add(Component.literal(ChatFormatting.GREEN + "Post War"));
        }
    }

    /**
     * Mirrors CE's {@code ItemEnums.SoyuzSkinType} (3 constants), used both for this flattened
     * item family's registry-id suffixes and for the per-skin rarity CE assigned via
     * {@code getRarity} (see class javadoc) - NORMAL = common, LUNAR = rare, POST_WAR = epic.
     */
    public enum SoyuzSkinType {
        NORMAL,
        LUNAR,
        POST_WAR;

        public static final SoyuzSkinType[] VALUES = values();
    }
}
