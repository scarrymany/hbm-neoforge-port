package com.hbm.items.machine;

import com.hbm.items.ItemBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/**
 * Particle accelerator coil: tooltip-only stat item for a particle accelerator that doesn't exist
 * yet, no tile entity reference. CE's four metadata grades become four registered instances.
 */
public class ItemPACoil extends ItemBase {

    private final EnumCoilType type;

    public ItemPACoil(EnumCoilType type, Properties properties) {
        super(properties.stacksTo(1));
        this.type = type;
    }

    public EnumCoilType getType() {
        return this.type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(ChatFormatting.BLUE + "Quadrupole operational range: " + ChatFormatting.RESET +
                String.format(Locale.US, "%,d", this.type.quadMin) + " - " + String.format(Locale.US, "%,d", this.type.quadMax)));
        tooltip.add(Component.literal(ChatFormatting.BLUE + "Dipole operational range: " + ChatFormatting.RESET +
                String.format(Locale.US, "%,d", this.type.diMin) + " - " + String.format(Locale.US, "%,d", this.type.diMax)));
        tooltip.add(Component.literal(ChatFormatting.BLUE + "Dipole minimum side length: " + ChatFormatting.RESET + this.type.diDistMin));
        tooltip.add(Component.literal(ChatFormatting.RED + "Minimums not met result in a power draw penalty!"));
        tooltip.add(Component.literal(ChatFormatting.RED + "Maximums exceeded result in the particle crashing!"));
        tooltip.add(Component.literal(ChatFormatting.RED + "Particles will crash in dipoles if both penalties take effect!"));
    }

    public enum EnumCoilType {
        GOLD(0, 2_200, 0, 2_200, 15),
        NIOBIUM(1_500, 8_400, 1_500, 8_400, 21),
        BSCCO(7_500, 15_000, 7_500, 15_000, 27),
        CHLOROPHYTE(14_500, 75_000, 14_500, 75_000, 51);

        public static final EnumCoilType[] VALUES = values();

        public final int quadMin;
        public final int quadMax;
        public final int diMin;
        public final int diMax;
        public final int diDistMin;

        EnumCoilType(int quadMin, int quadMax, int diMin, int diMax, int diDistMin) {
            this.quadMin = quadMin;
            this.quadMax = quadMax;
            this.diMin = diMin;
            this.diMax = diMax;
            this.diDistMin = diDistMin;
        }
    }
}
