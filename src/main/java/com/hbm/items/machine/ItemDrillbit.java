package com.hbm.items.machine;

import com.hbm.items.ItemBase;
import com.hbm.items.ItemEnums;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Drill bit: tooltip-only stat item, the actual drill tool logic lives on the tool item in
 * {@code items.tool} (out of this area's scope). CE's ten {@link ItemEnums.EnumDrillType}
 * metadata variants become ten registered instances of this class.
 */
public class ItemDrillbit extends ItemBase {

    private final ItemEnums.EnumDrillType type;

    public ItemDrillbit(ItemEnums.EnumDrillType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public ItemEnums.EnumDrillType getType() {
        return this.type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(I18nUtil.resolveKey("desc.speed") + " " + ((int) (this.type.speed * 100)) + "%").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal(I18nUtil.resolveKey("desc.tier", this.type.tier)).withStyle(ChatFormatting.YELLOW));
        if (this.type.fortune > 0) tooltip.add(Component.literal(I18nUtil.resolveKey("desc.fortune") + " " + this.type.fortune).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (this.type.vein) tooltip.add(Component.literal(I18nUtil.resolveKey("desc.veinminer")).withStyle(ChatFormatting.GREEN));
        if (this.type.silk) tooltip.add(Component.literal(I18nUtil.resolveKey("desc.silktouch")).withStyle(ChatFormatting.GREEN));
    }
}
