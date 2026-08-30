package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.fluid.FluidType;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Ported from CE's {@code com.hbm.items.armor.ArmorDesh} (87 lines) - the Deshret ("Steamsuit")
 * fuel-tank armor set. Beyond client-model/renderer plumbing (Phase 5), CE's only non-rendering
 * behavior is a fixed -0.025 flat movement-speed penalty per worn piece (CE:
 * {@code getItemAttributeModifiers}, unconditionally active - ported as a static
 * {@link ItemAttributeModifiers} component, same idiom as {@link ArmorLiquidator}, sharing the same
 * per-slot fixed modifier id via {@link ArmorModHandler#getArmorSlotModifierId}).
 */
public class ArmorDesh extends ArmorFSBFueled {

    public ArmorDesh(Holder<ArmorMaterial> material, Type type, Item.Properties properties,
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
        // CE op code 1 = MULTIPLY_BASE (percentage of the attribute's base value), not a flat ADD -
        // confirmed against CE's real AttributeModifier(uuid, name, -0.025D, 1) call.
        ItemAttributeModifiers modifiers = ItemAttributeModifiers.builder()
                .add(Attributes.MOVEMENT_SPEED,
                        new AttributeModifier(ArmorModHandler.getArmorSlotModifierId(slot), -0.025D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                        ArmorModHandler.getArmorSlotGroup(slot))
                .build();
        return properties.attributes(modifiers);
    }
}
