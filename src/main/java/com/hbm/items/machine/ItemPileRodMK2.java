package com.hbm.items.machine;

import com.hbm.items.ItemBase;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Depletion-tracked pile reactor rod. CE's {@code react()} turned the stack into a different
 * metadata value ({@code EnumPileRod.turnsInto}) of the same registry entry on depletion; since
 * each {@link EnumPileRod} value is now its own registered item, {@link #BY_TYPE} maps the enum
 * back to its sibling instance so {@link #react} can still hand back the correct depleted stack.
 */
public class ItemPileRodMK2 extends ItemBase {

    private static final Map<EnumPileRod, ItemPileRodMK2> BY_TYPE = new EnumMap<>(EnumPileRod.class);

    public static final String KEY_NBT_DEPLETION = "depletion";

    private final EnumPileRod type;

    public ItemPileRodMK2(EnumPileRod type, Properties properties) {
        super(properties);
        this.type = type;
        BY_TYPE.put(type, this);
    }

    public EnumPileRod getType() {
        return this.type;
    }

    public static double getDepletionPercent(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemPileRodMK2 rod)) return 0D;
        double life = rod.type.life;
        if (life <= 0) return 0D;
        return (getDepletion(stack) / life) * 100;
    }

    public static double getDepletion(ItemStack stack) {
        return stack.getOrDefault(MachineDataComponents.PILE_ROD_DEPLETION.get(), 0D);
    }

    public static void setDepletion(ItemStack stack, double depletion) {
        stack.set(MachineDataComponents.PILE_ROD_DEPLETION.get(), depletion);
    }

    public static double getReactivity(ItemStack stack, double inFlux) {
        if (!(stack.getItem() instanceof ItemPileRodMK2 rod)) return 0D;
        double outFlux = rod.type.neutronSource;
        if (rod.type.reactionMult > 0) {
            outFlux += MachineMathUtil.squirt(inFlux) * rod.type.reactionMult;
        }
        return outFlux;
    }

    public static double getHeatPerNeutron(ItemStack stack) {
        return stack.getItem() instanceof ItemPileRodMK2 rod ? rod.type.heatMult : 0D;
    }

    public static ItemStack react(ItemStack stack, double inFlux) {
        if (!(stack.getItem() instanceof ItemPileRodMK2 rod) || rod.type.life <= 0) return stack;
        double dep = getDepletion(stack) + inFlux;

        if (dep < rod.type.life) {
            setDepletion(stack, dep);
            return stack;
        }
        ItemPileRodMK2 depleted = BY_TYPE.get(rod.type.getTurnsInto());
        return depleted != null ? new ItemStack(depleted) : stack;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return this.type.life > 0 && getDepletion(stack) > 0D;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return this.type.life <= 0 ? 0 : Math.round(13.0F * (float) (getDepletion(stack) / this.type.life));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xC08000;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (this.type.life > 0) {
            tooltip.add(Component.literal("Lifetime: " + Math.round(this.type.life)));
            double depletion = getDepletionPercent(stack);
            if (depletion > 0) tooltip.add(Component.literal("Depletion: " + Math.round(depletion) + "%"));
        }

        String descKey = this.getDescriptionId() + ".desc";
        if (I18nUtil.exist(descKey)) {
            for (String line : I18nUtil.resolveKey(descKey).split("\\$")) {
                tooltip.add(Component.literal(line).withStyle(ChatFormatting.YELLOW));
            }
        }
    }

    /**
     * Ordinals below reproduce CE's own {@code EnumPileRod(double, double, double, int turnsInto)}
     * constants verbatim (RA226BE=0, PO210BE=1, ZR=2, NU=3, PU239=4, RGP=5, WASTE=6, THORIUM=7,
     * THORIUM_FUEL=8) - kept as a plain ordinal index rather than a direct enum reference because a
     * Java enum constant cannot reference a sibling declared later in the same enum (NU depletes
     * into PU239, which is declared after it).
     */
    public enum EnumPileRod {
        RA226BE(1D),
        PO210BE(1D),
        ZR(0D, 0D, 0D, 2),
        NU(1D, 25_000D, 0.25D, 4),
        PU239(1D, 500D, 0.5D, 5),
        RGP(1D, 1_000D, 0.5D, 6),
        WASTE(1D, 0D, 1.5D, 6),
        THORIUM(1D, 35_000D, 0.25D, 8),
        THORIUM_FUEL(1D, 2_000D, 0.5D, 6);

        public static final EnumPileRod[] VALUES = values();

        public final double reactionMult;
        public final double life;
        public final double heatMult;
        public final double neutronSource;
        private final int turnsIntoOrdinal;

        EnumPileRod(double neutronSource) {
            this.neutronSource = neutronSource;
            this.reactionMult = 0;
            this.life = 0;
            this.heatMult = 0;
            this.turnsIntoOrdinal = this.ordinal();
        }

        EnumPileRod(double reaction, double life, double heat, int turnsIntoOrdinal) {
            this.reactionMult = reaction;
            this.life = life;
            this.heatMult = heat;
            this.neutronSource = 0;
            this.turnsIntoOrdinal = turnsIntoOrdinal;
        }

        public EnumPileRod getTurnsInto() {
            return VALUES[this.turnsIntoOrdinal];
        }
    }
}
