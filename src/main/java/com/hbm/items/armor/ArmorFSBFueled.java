package com.hbm.items.armor;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.lib.Library;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.armor.ArmorFSBFueled} (157 lines) - {@link ArmorFSB} +
 * {@link IFillableItem}, the fuel-tank-gated sibling of {@link ArmorFSBPowered}: same
 * "whole set's bonuses turn off when empty" shape, gated on a flat {@code int} mB amount of one
 * fixed accepted {@link FluidType} instead of a battery charge. Extended by {@code ArmorDesh}/
 * {@code ArmorDiesel} - not this package's job, per this package's task brief item 4.
 *
 * <p>Fuel is stored via {@link ArmorDataComponents#ARMOR_FUEL} instead of CE's raw {@code "fuel"}
 * NBT tag (a distinct component id from {@link ArmorDataComponents#JETPACK_FUEL} even though both
 * are plain {@code int} amounts - see that class's javadoc). {@link #getFill} defaults an untagged
 * stack to full (CE: {@code stack.getTagCompound() == null -> setFill(stack, maxFuel)}), matching
 * CE exactly.
 */
public class ArmorFSBFueled extends ArmorFSB implements IFillableItem {

    public final FluidType fuelType;
    public final int maxFuel;
    public final int fillRate;
    public final int consumption;
    public final int drain;

    public ArmorFSBFueled(Holder<ArmorMaterial> material, Type type, Item.Properties properties,
                           FluidType fuelType, int maxFuel, int fillRate, int consumption, int drain) {
        super(material, type, properties);
        this.fuelType = fuelType;
        this.maxFuel = maxFuel;
        this.fillRate = fillRate;
        this.consumption = consumption;
        this.drain = drain;
    }

    @Override
    public int getFill(ItemStack stack) {
        return stack.getOrDefault(ArmorDataComponents.ARMOR_FUEL.get(), maxFuel);
    }

    public void setFill(ItemStack stack, int fill) {
        stack.set(ArmorDataComponents.ARMOR_FUEL.get(), Math.max(0, fill));
    }

    public int getMaxFill(ItemStack stack) {
        return this.maxFuel;
    }

    public int getLoadSpeed(ItemStack stack) {
        return this.fillRate;
    }

    public int getUnloadSpeed(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean isArmorEnabled(ItemStack stack) {
        return getFill(stack) > 0;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getFill(stack) < getMaxFill(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getFill(stack) / getMaxFill(stack));
    }

    @Override
    protected void onArmorTick(Level level, Player player, ItemStack stack) {
        // CE calls super.onArmorTick here (unlike ArmorFSBPowered) - preserved exactly.
        super.onArmorTick(level, player, stack);

        if (this.drain > 0 && ArmorFSB.hasFSBArmor(player) && !player.isCreative() && level.getGameTime() % 10 == 0) {
            this.setFill(stack, Math.max(this.getFill(stack) - this.drain, 0));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> components, TooltipFlag flag) {
        components.add(Component.empty().append(this.fuelType.getLocalizedName())
                .append(": " + Library.getShortNumber(getFill(stack)) + " / " + Library.getShortNumber(getMaxFill(stack))));
        super.appendHoverText(stack, context, components, flag);
    }

    @Override
    public boolean acceptsFluid(FluidType type, ItemStack stack) {
        return type == this.fuelType;
    }

    @Override
    public int tryFill(FluidType type, int amount, ItemStack stack) {
        if (!acceptsFluid(type, stack)) return amount;

        int toFill = Math.min(amount, this.fillRate);
        toFill = Math.min(toFill, this.maxFuel - this.getFill(stack));
        this.setFill(stack, this.getFill(stack) + toFill);

        return amount - toFill;
    }

    @Override
    public boolean providesFluid(FluidType type, ItemStack stack) {
        return false;
    }

    @Override
    public int tryEmpty(FluidType type, int amount, ItemStack stack) {
        return 0;
    }

    @Override
    public FluidType getFirstFluidType(ItemStack stack) {
        return null;
    }
}
