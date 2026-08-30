package com.hbm.items.special;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Port of CE's {@code ItemSiegeCoin} ({@code coin_siege}): metadata-multi keyed to
 * {@code SiegeTier.getLength()} rather than a fixed enum. Per docs/phase1/items_special.md, the real
 * count was confirmed by reading {@code upstream/hbm-ce/.../entity/siege/SiegeTier.java} directly
 * rather than guessed: {@code registerTiers()} constructs exactly 9 tiers (buff, clay, stone, iron,
 * silver, gold, desh, schrab, dnt), so this flattens into 9 registry entries - see
 * {@link SpecialItems} for the registration, one per {@link SiegeTier}. The siege subsystem itself
 * ({@code SiegeTier}, and whatever consumes these coins) is not part of Phase 1 and is not ported
 * here; this class only needs the tier index for its tooltip.
 */
public class ItemSiegeCoin extends Item {

    private final int tier;

    public ItemSiegeCoin(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tier " + (tier + 1)).withStyle(ChatFormatting.YELLOW));
    }

    /**
     * Mirrors CE's {@code SiegeTier} names in declaration order (confirmed against
     * {@code upstream/hbm-ce/.../entity/siege/SiegeTier.java}'s {@code registerTiers()}), used only
     * for this flattened item's registry-id suffix - the siege subsystem itself is not ported.
     */
    public enum SiegeTier {
        BUFF, CLAY, STONE, IRON, SILVER, GOLD, DESH, SCHRAB, DNT;

        public static final SiegeTier[] VALUES = values();
    }
}
