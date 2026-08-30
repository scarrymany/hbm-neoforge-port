package com.hbm.items.machine;

import com.hbm.items.ISatChip;
import com.hbm.items.ItemBase;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.Arrays;
import java.util.List;

/**
 * Satellite payload module. Same shape as {@link ItemSatChip}, just enum-driven in CE instead of
 * one class per instance; CE's fourteen metadata variants become fourteen registered instances.
 */
public class ItemSatellite extends ItemBase implements ISatChip {

    private final EnumSatType type;

    public ItemSatellite(EnumSatType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public EnumSatType getType() {
        return this.type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(ChatFormatting.AQUA + I18nUtil.resolveKey("satchip.frequency") + ": " + getFreq(stack)));
        Arrays.stream(I18nUtil.resolveKeyArray(this.type.descKey)).map(Component::literal).forEach(tooltip::add);
    }

    public enum EnumSatType {
        SPY("satchip.mapper"),
        SCANNER("satchip.scanner"),
        RADAR("satchip.radar"),
        MINER_ASTRO("satchip.miner"),
        MINER_LUNAR("satchip.lunar_miner"),
        PRECISION_LASER("satchip.precision_laser"),
        DEATH_RAY("satchip.laser"),
        XENIUM_RESONATOR("satchip.resonator"),
        RELAY("satchip.foeq"),
        DETECTOR("satchip.detector"),
        RAY_SCAN("satchip.ray_scanner"),
        SCIENCE("satchip.science"),
        SCIENCE_ASSEMBLER("satchip.science_assembler"),
        SCIENCE_SENSOR("satchip.science_sensor");

        public static final EnumSatType[] VALUES = values();

        public final String descKey;

        EnumSatType(String descKey) {
            this.descKey = descKey;
        }
    }
}
