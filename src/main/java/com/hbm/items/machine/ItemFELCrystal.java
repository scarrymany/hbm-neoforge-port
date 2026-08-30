package com.hbm.items.machine;

import com.hbm.items.ItemBase;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Free-electron-laser crystal: tooltip-only descriptor item for a laser machine that doesn't
 * exist yet. Every CE instance is already its own registered item (one class per wavelength).
 */
public class ItemFELCrystal extends ItemBase {

    private final EnumWavelengths wavelength;

    public ItemFELCrystal(EnumWavelengths wavelength, Properties properties) {
        super(properties.stacksTo(1));
        this.wavelength = wavelength;
    }

    public EnumWavelengths getWavelength() {
        return this.wavelength;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String descKey = this.getDescriptionId() + ".desc";
        tooltip.add(Component.literal(I18nUtil.resolveKey(descKey)));
        tooltip.add(Component.literal(I18nUtil.resolveKey(this.wavelength.name) + " - " + I18nUtil.resolveKey(this.wavelength.wavelengthRange))
                .withStyle(this.wavelength.textColor));
    }

    public enum EnumWavelengths {
        NULL("la creatura", "6 dollar", 0x010101, 0x010101, ChatFormatting.WHITE),
        IR("wavelengths.name.ir", "wavelengths.waveRange.ir", 0xBB1010, 0xCC4040, ChatFormatting.RED),
        VISIBLE("wavelengths.name.visible", "wavelengths.waveRange.visible", 0, 0, ChatFormatting.GREEN),
        UV("wavelengths.name.uv", "wavelengths.waveRange.uv", 0x0A1FC4, 0x00EFFF, ChatFormatting.AQUA),
        GAMMA("wavelengths.name.gamma", "wavelengths.waveRange.gamma", 0x150560, 0xEF00FF, ChatFormatting.LIGHT_PURPLE),
        DRX("wavelengths.name.drx", "wavelengths.waveRange.drx", 0xFF0000, 0xFF0000, ChatFormatting.DARK_RED);

        public final String name;
        public final String wavelengthRange;
        public final int renderedBeamColor;
        public final int guiColor;
        public final ChatFormatting textColor;

        EnumWavelengths(String name, String wavelength, int color, int guiColor, ChatFormatting textColor) {
            this.name = name;
            this.wavelengthRange = wavelength;
            this.renderedBeamColor = color;
            this.guiColor = guiColor;
            this.textColor = textColor;
        }
    }
}
