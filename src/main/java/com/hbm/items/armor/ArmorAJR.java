package com.hbm.items.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;

/**
 * Ported from CE's {@code com.hbm.items.armor.ArmorAJR} (59 lines) - the T-45 AJR power-armor set.
 * CE's real body is entirely client-model/renderer plumbing ({@code getArmorModel}/
 * {@code getRenderer}, both Phase 5 per {@code docs/phase3/armor_equippable_framework.md}'s
 * Deferred scope); it adds no stat/behavior logic of its own beyond {@link ArmorFSBPowered}'s. Its
 * full stat table (maxPower/chargeRate/consumption/drain, potion effects, hazard class, rad-resist,
 * geiger/hard-landing/VATS flags, sounds) lives on the concrete item constants in
 * {@code PoweredArmorItems} (CE {@code ModItems.java:609-622}), not here - matching CE's own split
 * (the class is a bare constructor, the stat table is assembled at each field's declaration site).
 */
public class ArmorAJR extends ArmorFSBPowered {

    public ArmorAJR(Holder<ArmorMaterial> material, Type type, Item.Properties properties,
                     long maxPower, long chargeRate, long consumption, long drain) {
        super(material, type, properties, maxPower, chargeRate, consumption, drain);
    }
}
