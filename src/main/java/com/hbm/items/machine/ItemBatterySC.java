package com.hbm.items.machine;

import com.hbm.api.energymk2.IBatteryItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.function.Supplier;

/**
 * Fixed-power radioisotope battery: no mutable charge state, always reports its type's fixed
 * power as both current charge and discharge rate. CE's ten isotope grades become ten registered
 * instances of this class instead of one item with ten metadata variants.
 */
public class ItemBatterySC extends Item implements IBatteryItem {

    private final EnumBatterySC type;

    public ItemBatterySC(EnumBatterySC type, Properties properties) {
        super(properties.stacksTo(1));
        this.type = type;
    }

    public EnumBatterySC getType() {
        return this.type;
    }

    @Override
    public Supplier<DataComponentType<Long>> getChargeComponent() {
        return MachineDataComponents.CHARGE;
    }

    @Override
    public void chargeBattery(ItemStack stack, long i) {}

    @Override
    public void setCharge(ItemStack stack, long i) {}

    @Override
    public void dischargeBattery(ItemStack stack, long i) {}

    @Override
    public long getCharge(ItemStack stack) {
        return this.type.power;
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        return this.type.power;
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return 0;
    }

    @Override
    public long getDischargeRate(ItemStack stack) {
        return this.type.power;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (this.type.power > 0) {
            tooltip.add(Component.literal("Discharge rate: " + MachineMathUtil.getShortNumber(this.type.power) + "HE/t").withStyle(ChatFormatting.YELLOW));
        }
    }

    public enum EnumBatterySC {
        EMPTY(0),
        WASTE(150),
        RA226(200),
        TC99(500),
        CO60(750),
        PU238(1_000),
        PO210(1_250),
        AU198(1_500),
        PB209(2_000),
        AM241(2_500);

        public static final EnumBatterySC[] VALUES = values();

        public final long power;

        EnumBatterySC(long power) {
            this.power = power;
        }
    }
}
