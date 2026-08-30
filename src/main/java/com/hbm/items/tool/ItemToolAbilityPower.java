package com.hbm.items.tool;

import com.hbm.api.energymk2.IBatteryItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.function.Supplier;

/**
 * Adds an electric charge tank on top of {@link ItemToolAbility} (used by the {@code elec_*} tools
 * and the {@code drax} drill line). Ported from CE's {@code com.hbm.items.tool.ItemToolAbilityPower}.
 *
 * <p>See {@link ToolDataComponents} for why this stores charge in a package-local
 * {@code hbm:tool_charge} component rather than the shared {@code hbm:battery_charge} component
 * {@link IBatteryItem}'s javadoc anticipates - no such shared component exists yet anywhere in the
 * tree.
 */
public class ItemToolAbilityPower extends ItemToolAbility implements IBatteryItem {

    protected final long maxPower;
    protected final long chargeRate;
    protected final long consumption;

    public ItemToolAbilityPower(Properties properties, Tier tier, ToolRole role, long maxPower, long chargeRate, long consumption) {
        super(properties, tier, role);
        this.maxPower = maxPower;
        this.chargeRate = chargeRate;
        this.consumption = consumption;
    }

    @Override
    public Supplier<DataComponentType<Long>> getChargeComponent() {
        return ToolDataComponents.TOOL_CHARGE::get;
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        return maxPower;
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return chargeRate;
    }

    @Override
    public long getDischargeRate(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean canOperate(ItemStack stack) {
        return getCharge(stack) >= consumption;
    }

    @Override
    protected void applyWear(ItemStack stack, Player player, int amount) {
        dischargeBattery(stack, (long) amount * consumption);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCharge(stack) < maxPower;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getCharge(stack) / maxPower);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        long power = getCharge(stack);
        tooltipComponents.add(Component.literal("Charge: " + getColor(power, maxPower) + shortNumber(power) + " §2/ " + shortNumber(maxPower)));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    public static String getColor(long a, long b) {
        float fraction = 100F * a / b;
        if (fraction > 75) return "§a";
        if (fraction > 25) return "§e";
        return "§c";
    }

    /**
     * Compact large-number formatting (e.g. {@code 2.5B}) matching CE's {@code Library.getShortNumber}.
     * Reimplemented locally: the port's current {@code Library} stub deliberately carries forward
     * only the one method other Phase-0 code needs (see its own javadoc) and this area's package
     * scope does not include {@code com.hbm.lib}.
     */
    private static String shortNumber(long value) {
        double abs = Math.abs((double) value);
        String sign = value < 0 ? "-" : "";

        if (abs >= 1_000_000_000_000L) return sign + trimmed(abs / 1_000_000_000_000D) + "T";
        if (abs >= 1_000_000_000L) return sign + trimmed(abs / 1_000_000_000D) + "B";
        if (abs >= 1_000_000L) return sign + trimmed(abs / 1_000_000D) + "M";
        if (abs >= 1_000L) return sign + trimmed(abs / 1_000D) + "K";
        return String.valueOf(value);
    }

    private static String trimmed(double value) {
        String formatted = String.format(java.util.Locale.ROOT, "%.1f", value);
        return formatted.endsWith(".0") ? formatted.substring(0, formatted.length() - 2) : formatted;
    }
}
