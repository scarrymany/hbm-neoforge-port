package com.hbm.items.machine;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.main.MainRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.function.Supplier;

/**
 * Battery/capacitor pack. CE modeled the six battery grades and six capacitor grades as one
 * item's metadata variants; each grade is its own registered item here, carrying its own
 * {@link EnumBatteryPack} constant directly. The CE {@code IMetaItemTesr} TESR-on-item render
 * binding is a client rendering detail out of this area's scope, not a functional dependency.
 */
public class ItemBatteryPack extends Item implements IBatteryItem {

    private final EnumBatteryPack type;

    public ItemBatteryPack(EnumBatteryPack type, Properties properties) {
        super(properties.stacksTo(1));
        this.type = type;
    }

    public EnumBatteryPack getType() {
        return this.type;
    }

    public static ItemStack makeEmptyBattery(ItemStack stack) {
        stack.set(MachineDataComponents.CHARGE.get(), 0L);
        return stack;
    }

    public static ItemStack makeFullBattery(ItemStack stack) {
        if (stack.getItem() instanceof ItemBatteryPack pack) {
            stack.set(MachineDataComponents.CHARGE.get(), pack.type.capacity);
        }
        return stack;
    }

    @Override
    public Supplier<DataComponentType<Long>> getChargeComponent() {
        return MachineDataComponents.CHARGE;
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        return this.type.capacity;
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return this.type.chargeRate;
    }

    @Override
    public long getDischargeRate(ItemStack stack) {
        return this.type.dischargeRate;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCharge(stack) < getMaxCharge(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getCharge(stack) / (float) getMaxCharge(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float ratio = (float) getCharge(stack) / (float) getMaxCharge(stack);
        return Mth.hsvToRgb(ratio / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        long maxCharge = getMaxCharge(stack);
        long chargeRate = getChargeRate(stack);
        long dischargeRate = getDischargeRate(stack);
        long charge = getCharge(stack);

        tooltip.add(Component.literal("Energy stored: " + MachineMathUtil.getShortNumber(charge) + "/" + MachineMathUtil.getShortNumber(maxCharge)
                + "HE (" + (charge * 1000 / maxCharge / 10D) + "%)").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("Charge rate: " + MachineMathUtil.getShortNumber(chargeRate) + "HE/t").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("Discharge rate: " + MachineMathUtil.getShortNumber(dischargeRate) + "HE/t").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("Time for full charge: " + (maxCharge / chargeRate / 20 / 60D) + "min").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Charge lasts for: " + (maxCharge / dischargeRate / 20 / 60D) + "min").withStyle(ChatFormatting.GOLD));
    }

    public enum EnumBatteryPack {
        BATTERY_REDSTONE("battery_redstone", 100L, false),
        BATTERY_LEAD("battery_lead", 1_000L, false),
        BATTERY_LITHIUM("battery_lithium", 10_000L, false),
        BATTERY_SODIUM("battery_sodium", 50_000L, false),
        BATTERY_SCHRABIDIUM("battery_schrabidium", 250_000L, false),
        BATTERY_QUANTUM("battery_quantum", 1_000_000L, 20 * 60 * 60),

        CAPACITOR_COPPER("capacitor_copper", 1_000L, true),
        CAPACITOR_GOLD("capacitor_gold", 10_000L, true),
        CAPACITOR_NIOBIUM("capacitor_niobium", 100_000L, true),
        CAPACITOR_TANTALUM("capacitor_tantalum", 500_000L, true),
        CAPACITOR_BISMUTH("capacitor_bismuth", 2_500_000L, true),
        CAPACITOR_SPARK("capacitor_spark", 10_000_000L, true);

        public static final EnumBatteryPack[] VALUES = values();

        public final ResourceLocation texture;
        public final long capacity;
        public final long chargeRate;
        public final long dischargeRate;

        EnumBatteryPack(String tex, long dischargeRate, boolean capacitor) {
            this(tex,
                    capacitor ? (dischargeRate * 20 * 30) : (dischargeRate * 20 * 60 * 15),
                    capacitor ? dischargeRate : dischargeRate * 10,
                    dischargeRate);
        }

        EnumBatteryPack(String tex, long dischargeRate, long duration) {
            this(tex, dischargeRate * duration, dischargeRate * 10, dischargeRate);
        }

        EnumBatteryPack(String tex, long capacity, long chargeRate, long dischargeRate) {
            this.texture = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/models/machines/" + tex + ".png");
            this.capacity = capacity;
            this.chargeRate = chargeRate;
            this.dischargeRate = dischargeRate;
        }

        public boolean isCapacitor() {
            return this.ordinal() > BATTERY_QUANTUM.ordinal();
        }
    }
}
