package com.hbm.items.machine;

import com.hbm.inventory.fluid.trait.FT_Combustible;
import com.hbm.items.ItemBase;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Combustion engine piston: tooltip-only per-fuel-grade efficiency table, no tile entity reference
 * (only reads the {@code FT_Combustible} fluid trait's fuel grade list). CE's four metadata grades
 * become four registered instances.
 */
public class ItemPistons extends ItemBase {

    private final EnumPistonType type;

    public ItemPistons(EnumPistonType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public EnumPistonType getType() {
        return this.type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(ChatFormatting.YELLOW + "Fuel efficiency:"));
        FT_Combustible.FuelGrade[] grades = FT_Combustible.FuelGrade.VALUES;
        for (int i = 0; i < this.type.eff.length; i++) {
            tooltip.add(Component.literal(ChatFormatting.YELLOW + "-" + I18nUtil.resolveKey(grades[i].getGrade()) + ": " +
                    ChatFormatting.RED + (int) (this.type.eff[i] * 100) + "%"));
        }
    }

    public enum EnumPistonType {
        STEEL(1.00, 0.75, 0.25, 0.00, 0.00),
        DURA(0.50, 1.00, 0.90, 0.50, 0.00),
        DESH(0.00, 0.50, 1.00, 0.75, 0.00),
        STARMETAL(0.50, 0.75, 1.00, 0.90, 0.50);

        public static final EnumPistonType[] VALUES = values();

        public final double[] eff;

        EnumPistonType(double... eff) {
            int length = Math.min(FT_Combustible.FuelGrade.VALUES.length, eff.length);
            this.eff = new double[length];
            System.arraycopy(eff, 0, this.eff, 0, length);
        }
    }
}
