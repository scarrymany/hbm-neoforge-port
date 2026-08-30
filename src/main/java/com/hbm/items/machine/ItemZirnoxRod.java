package com.hbm.items.machine;

import com.hbm.items.ItemBase;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.Arrays;
import java.util.List;

/**
 * Zirnox reactor rod. Life-counter NBT item, no tile entity reference. CE's eleven metadata
 * variants become eleven registered instances.
 */
public class ItemZirnoxRod extends ItemBase {

    private final EnumZirnoxType type;

    public ItemZirnoxRod(EnumZirnoxType type, Properties properties) {
        super(properties.stacksTo(1));
        this.type = type;
    }

    public EnumZirnoxType getType() {
        return this.type;
    }

    public static void incrementLifeTime(ItemStack stack) {
        setLifeTime(stack, getLifeTime(stack) + 1);
    }

    public static void setLifeTime(ItemStack stack, int time) {
        stack.set(MachineDataComponents.ZIRNOX_ROD_LIFE.get(), time);
    }

    public static int getLifeTime(ItemStack stack) {
        return stack.getOrDefault(MachineDataComponents.ZIRNOX_ROD_LIFE.get(), 0);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getLifeTime(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getLifeTime(stack) / (float) this.type.maxLife);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x60A060;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        double percent = ((int) (((double) getLifeTime(stack) / (double) this.type.maxLife) * 100000)) / 1000D;
        tooltip.add(Component.literal(ChatFormatting.YELLOW + I18nUtil.resolveKey("trait.rbmk.depletion", percent + "%")));

        String[] loc = this.type.breeding
                ? I18nUtil.resolveKeyArray("desc.item.zirnoxBreedingRod", MachineMathUtil.getShortNumber(this.type.maxLife))
                : I18nUtil.resolveKeyArray("desc.item.zirnoxRod", this.type.heat, MachineMathUtil.getShortNumber(this.type.maxLife));

        Arrays.stream(loc).map(Component::literal).forEach(tooltip::add);
    }

    public enum EnumZirnoxType {
        NATURAL_URANIUM_FUEL(250_000, 30),
        URANIUM_FUEL(200_000, 50),
        TH232_FUEL(20_000, 0, true),
        THORIUM_FUEL(200_000, 40),
        MOX_FUEL(165_000, 75),
        PLUTONIUM_FUEL(175_000, 65),
        U233_FUEL(150_000, 100),
        U235_FUEL(165_000, 85),
        LES_FUEL(150_000, 150),
        LITHIUM_FUEL(20_000, 0, true),
        ZFB_MOX_FUEL(50_000, 35);

        public static final EnumZirnoxType[] VALUES = values();

        public final int maxLife;
        public final int heat;
        public final boolean breeding;

        EnumZirnoxType(int life, int heat) {
            this(life, heat, false);
        }

        EnumZirnoxType(int life, int heat, boolean breeding) {
            this.maxLife = life;
            this.heat = heat;
            this.breeding = breeding;
        }
    }
}
