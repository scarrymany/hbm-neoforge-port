package com.hbm.items.special;

import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Port of CE's {@code ItemDepletedFuel} ({@code ItemEnumMulti}-style, 2 raw metadata values: 0 =
 * already cooled, 1 = freshly generated and still cooling). Per docs/phase1/items_special.md finding
 * 1, this becomes two distinct registry entries per CE field - a cooled item and a {@code _hot}
 * companion.
 * <p>
 * All 16 CE fields backed by this class ({@code waste_natural_uranium}, {@code waste_uranium}, ...,
 * {@code waste_plate_pu238be}) were already registered, independently, by
 * {@code com.hbm.items.PlateCrystalWasteItems} (a different concurrent Phase 1 area) as plain
 * {@link net.minecraft.world.item.Item} pairs before this class existed - see this area's final
 * report for the integration follow-up to point those registrations at this class instead. This
 * class is provided now as the faithful, ready-to-use behavior (cooling tooltip; CE's item-color
 * tint is a client-rendering registration deferred alongside every other Phase 1 area's dynamic-
 * model work, see docs/phase1/items_special.md finding 6).
 */
public class ItemDepletedFuel extends ItemNuclearWaste {

    private final boolean hot;

    public ItemDepletedFuel(Properties properties, boolean hot) {
        super(properties);
        this.hot = hot;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (hot) {
            tooltip.add(Component.literal(I18nUtil.resolveKey("desc.item.wasteCooling")).withStyle(ChatFormatting.GOLD));
        }
    }
}
