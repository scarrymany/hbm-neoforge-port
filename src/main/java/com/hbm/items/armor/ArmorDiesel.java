package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.particle.HbmEffect;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

/**
 * Ported from CE's {@code com.hbm.items.armor.ArmorDiesel} (147 lines) - the Diesel fuel-tank
 * armor set. Beyond client-model/renderer plumbing (Phase 5), CE's non-rendering behavior is:
 * <ul>
 *     <li>a fixed -2.5% knockback-resistance penalty per worn piece (CE:
 *     {@code getItemAttributeModifiers}, op code 1 = {@code ADD_MULTIPLIED_BASE}) - same static
 *     {@link ItemAttributeModifiers} idiom as {@link ArmorDesh}/{@link ArmorLiquidator};</li>
 *     <li>accepting both {@code DIESEL} and {@code DIESEL_CRACK} as fuel (CE overrides
 *     {@code acceptsFluid} beyond {@link ArmorFSBFueled}'s single-{@code fuelType} default);</li>
 *     <li>a cosmetic particle-trail tick on the legs piece while the full set is worn, every 3
 *     ticks (CE: {@code AuxParticlePacketNT}/{@code HbmEffectNT.bnuuy}) - wired via
 *     {@link com.hbm.particle.HbmEffect#BNUUY}, radius 100, matching CE's own call site 1:1.</li>
 * </ul>
 */
public class ArmorDiesel extends ArmorFSBFueled {

    public ArmorDiesel(Holder<ArmorMaterial> material, Type type, Item.Properties properties,
                        FluidType fuelType, int maxFuel, int fillRate, int consumption, int drain) {
        super(material, type, attributeProperties(properties, type), fuelType, maxFuel, fillRate, consumption, drain);
    }

    private static Item.Properties attributeProperties(Item.Properties properties, Type type) {
        EquipmentSlot slot = switch (type) {
            case HELMET -> EquipmentSlot.HEAD;
            case CHESTPLATE -> EquipmentSlot.CHEST;
            case LEGGINGS -> EquipmentSlot.LEGS;
            case BOOTS -> EquipmentSlot.FEET;
            default -> EquipmentSlot.CHEST;
        };
        ItemAttributeModifiers modifiers = ItemAttributeModifiers.builder()
                .add(Attributes.KNOCKBACK_RESISTANCE,
                        new AttributeModifier(ArmorModHandler.getArmorSlotModifierId(slot), -0.025D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                        ArmorModHandler.getArmorSlotGroup(slot))
                .build();
        return properties.attributes(modifiers);
    }

    @Override
    public boolean acceptsFluid(FluidType type, ItemStack stack) {
        return type == Fluids.DIESEL || type == Fluids.DIESEL_CRACK;
    }

    @Override
    protected void onArmorTick(Level level, Player player, ItemStack stack) {
        super.onArmorTick(level, player, stack);

        if (this.getType() != Type.LEGGINGS) return;
        if (level.isClientSide() || !ArmorFSB.hasFSBArmor(player)) return;
        if (level.getGameTime() % 3 != 0) return;

        CompoundTag data = new CompoundTag();
        data.putInt("player", player.getId());
        HbmEffect.sendPacket(level, HbmEffect.BNUUY, player.getX(), player.getY(), player.getZ(), 100, data);
    }
}
