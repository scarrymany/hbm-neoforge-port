package com.hbm.items.machine;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ItemBase;
import com.hbm.util.EnumUtil;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ICF (inertial confinement fusion) fuel pellet. Two independent fuel selections plus a muon
 * catalysis flag are stored as data components instead of a two-enum-selection NBT pair; all
 * physics ({@link #react}, {@link #getMaxDepletion}, {@link #getFusingDifficulty}) run entirely
 * over the ItemStack with no reference to the (Phase 2) ICF reactor tile entity.
 */
public class ItemICFPellet extends ItemBase {

    /** Fluid/material inputs the (Phase 2) ICF machine matches against a fuel type at insert time. */
    public static final Map<FluidType, EnumICFFuel> FLUID_MAP = new HashMap<>();
    public static final Map<NTMMaterial, EnumICFFuel> MATERIAL_MAP = new HashMap<>();

    static {
        FLUID_MAP.put(Fluids.HYDROGEN, EnumICFFuel.HYDROGEN);
        FLUID_MAP.put(Fluids.DEUTERIUM, EnumICFFuel.DEUTERIUM);
        FLUID_MAP.put(Fluids.TRITIUM, EnumICFFuel.TRITIUM);
        FLUID_MAP.put(Fluids.HELIUM3, EnumICFFuel.HELIUM3);
        FLUID_MAP.put(Fluids.HELIUM4, EnumICFFuel.HELIUM4);
        FLUID_MAP.put(Fluids.OXYGEN, EnumICFFuel.OXYGEN);
        FLUID_MAP.put(Fluids.CHLORINE, EnumICFFuel.CHLORINE);
        MATERIAL_MAP.put(Mats.MAT_LITHIUM, EnumICFFuel.LITHIUM);
        MATERIAL_MAP.put(Mats.MAT_BERYLLIUM, EnumICFFuel.BERYLLIUM);
        MATERIAL_MAP.put(Mats.MAT_BORON, EnumICFFuel.BORON);
        MATERIAL_MAP.put(Mats.MAT_GRAPHITE, EnumICFFuel.CARBON);
        MATERIAL_MAP.put(Mats.MAT_SODIUM, EnumICFFuel.SODIUM);
        MATERIAL_MAP.put(Mats.MAT_CALCIUM, EnumICFFuel.CALCIUM);
    }

    public ItemICFPellet(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static long getMaxDepletion(ItemStack stack) {
        double base = 50_000_000_000L;
        base /= getType(stack, true).depletionSpeed;
        base /= getType(stack, false).depletionSpeed;
        return (long) base;
    }

    public static long getFusingDifficulty(ItemStack stack) {
        double base = 10_000_000L;
        base *= getType(stack, true).fusingDifficulty * getType(stack, false).fusingDifficulty;
        if (Boolean.TRUE.equals(stack.get(MachineDataComponents.ICF_MUON.get()))) base /= 4;
        return (long) base;
    }

    public static long getDepletion(ItemStack stack) {
        return stack.getOrDefault(MachineDataComponents.ICF_PELLET_DEPLETION.get(), 0L);
    }

    public static long react(ItemStack stack, long heat) {
        long depletion = getDepletion(stack) + heat;
        stack.set(MachineDataComponents.ICF_PELLET_DEPLETION.get(), depletion);
        return (long) (heat * getType(stack, true).reactionMult * getType(stack, false).reactionMult);
    }

    public static ItemStack setup(ItemStack stack, EnumICFFuel type1, EnumICFFuel type2, boolean muon) {
        stack.set(MachineDataComponents.ICF_TYPE1.get(), type1.ordinal());
        stack.set(MachineDataComponents.ICF_TYPE2.get(), type2.ordinal());
        stack.set(MachineDataComponents.ICF_MUON.get(), muon);
        return stack;
    }

    public static EnumICFFuel getType(ItemStack stack, boolean first) {
        int defaultOrdinal = first ? EnumICFFuel.DEUTERIUM.ordinal() : EnumICFFuel.TRITIUM.ordinal();
        int ordinal = stack.getOrDefault(first ? MachineDataComponents.ICF_TYPE1.get() : MachineDataComponents.ICF_TYPE2.get(), defaultOrdinal);
        return EnumUtil.grabEnumSafely(EnumICFFuel.VALUES, ordinal);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getDepletion(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getDepletion(stack) / (float) getMaxDepletion(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFF6000;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        boolean muon = Boolean.TRUE.equals(stack.get(MachineDataComponents.ICF_MUON.get()));
        double depletionFraction = getDepletion(stack) / (double) getMaxDepletion(stack);

        tooltip.add(Component.literal(ChatFormatting.GREEN + "Depletion: " + String.format(Locale.US, "%.1f", depletionFraction * 100D) + "%"));
        tooltip.add(Component.literal(ChatFormatting.YELLOW + "Fuel: " + I18nUtil.resolveKey("icffuel." + getType(stack, true).name().toLowerCase(Locale.US)) +
                " / " + I18nUtil.resolveKey("icffuel." + getType(stack, false).name().toLowerCase(Locale.US))));
        tooltip.add(Component.literal(ChatFormatting.YELLOW + "Heat required: " + MachineMathUtil.getShortNumber(getFusingDifficulty(stack)) + "TU"));
        tooltip.add(Component.literal(ChatFormatting.YELLOW + "Reactivity multiplier: x" + (int) (getType(stack, true).reactionMult * getType(stack, false).reactionMult * 100) / 100D));
        if (muon) tooltip.add(Component.literal(ChatFormatting.DARK_AQUA + "Muon catalyzed!"));
    }

    public enum EnumICFFuel {
        HYDROGEN(0x4040FF, 1.00D, 0.85D, 1.00D),
        DEUTERIUM(0x2828CB, 1.25D, 1.00D, 1.00D),
        TRITIUM(0x000092, 1.50D, 1.00D, 1.05D),
        HELIUM3(0xFFF09F, 1.75D, 1.00D, 1.25D),
        HELIUM4(0xFF9B60, 2.00D, 1.00D, 1.50D),
        LITHIUM(0xE9E9E9, 1.25D, 0.85D, 2.00D),
        BERYLLIUM(0xA79D80, 2.00D, 1.00D, 2.50D),
        BORON(0x697F89, 3.00D, 0.50D, 3.50D),
        CARBON(0x454545, 2.00D, 1.00D, 5.00D),
        OXYGEN(0xB4E2FF, 1.25D, 1.50D, 7.50D),
        SODIUM(0xDFE4E7, 3.00D, 0.75D, 8.75D),
        CHLORINE(0xDAE598, 2.50D, 1.00D, 9.25D),
        CALCIUM(0xD2C7A9, 3.00D, 1.00D, 9.75D);

        public static final EnumICFFuel[] VALUES = values();

        public final int color;
        public final double reactionMult;
        public final double depletionSpeed;
        public final double fusingDifficulty;

        EnumICFFuel(int color, double react, double depl, double laser) {
            this.color = color;
            this.reactionMult = react;
            this.depletionSpeed = depl;
            this.fusingDifficulty = laser;
        }
    }
}
