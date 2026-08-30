package com.hbm.items.machine;

import com.hbm.api.energymk2.IBatteryItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.function.Supplier;

/**
 * Simple charge/discharge battery item. CE marked this {@code @Deprecated} in favor of
 * {@link ItemBatteryPack}, but every one of its ~30 registered instances is still real CE content
 * that {@code ModItems} needs, so it is ported unchanged in spirit - just not a template for new
 * battery content.
 * <p>
 * CE special-cased two display quirks by identity-checking against specific {@code ModItems}
 * fields ({@code battery_schrabidium}'s rarity, {@code fusion_core}/{@code energy_core}'s
 * percent-only tooltip). Both become plain constructor flags here instead of identity checks
 * against sibling registry entries, which would otherwise create a static-init-order hazard
 * between this class and {@code ModItems}.
 */
public class ItemBattery extends Item implements IBatteryItem {

    private final long maxCharge;
    private final long chargeRate;
    private final long dischargeRate;
    private final Rarity rarity;
    private final boolean percentOnlyTooltip;

    public ItemBattery(long maxCharge, long chargeRate, long dischargeRate, Properties properties) {
        this(maxCharge, chargeRate, dischargeRate, Rarity.COMMON, false, properties);
    }

    public ItemBattery(long maxCharge, long chargeRate, long dischargeRate, Rarity rarity, boolean percentOnlyTooltip, Properties properties) {
        super(properties.stacksTo(1));
        this.maxCharge = maxCharge;
        this.chargeRate = chargeRate;
        this.dischargeRate = dischargeRate;
        this.rarity = rarity;
        this.percentOnlyTooltip = percentOnlyTooltip;
    }

    @Override
    public Supplier<DataComponentType<Long>> getChargeComponent() {
        return MachineDataComponents.CHARGE;
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        return this.maxCharge;
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return this.chargeRate;
    }

    @Override
    public long getDischargeRate(ItemStack stack) {
        return this.dischargeRate;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return this.rarity;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getCharge(stack) / (float) this.maxCharge);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00A000;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        long charge = getCharge(stack);

        if (this.percentOnlyTooltip) {
            String percent = MachineMathUtil.getShortNumber((charge * 100) / this.maxCharge);
            tooltip.add(Component.literal("Charge: " + percent + "%").withStyle(ChatFormatting.DARK_GREEN));
            tooltip.add(Component.literal("(" + MachineMathUtil.getShortNumber(charge) + "/" + MachineMathUtil.getShortNumber(this.maxCharge) + "HE)"));
        } else {
            tooltip.add(Component.literal("Energy stored: " + MachineMathUtil.getShortNumber(charge) + "/" + MachineMathUtil.getShortNumber(this.maxCharge) + "HE").withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.literal("Charge rate: " + MachineMathUtil.getShortNumber(this.chargeRate * 20) + "HE/s").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("Discharge rate: " + MachineMathUtil.getShortNumber(this.dischargeRate * 20) + "HE/s").withStyle(ChatFormatting.RED));
    }
}
