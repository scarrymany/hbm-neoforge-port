package com.hbm.items.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;

/**
 * Ported from CE's {@code com.hbm.items.armor.ArmorDigamma} (91 lines) - the Digamma ("fau")
 * power-armor set. CE's whole class is client-model/renderer plumbing (Phase 5); no behavior of
 * its own beyond {@link ArmorFSBPowered}'s.
 */
public class ArmorDigamma extends ArmorFSBPowered {

    public ArmorDigamma(Holder<ArmorMaterial> material, Type type, Item.Properties properties,
                         long maxPower, long chargeRate, long consumption, long drain) {
        super(material, type, properties, maxPower, chargeRate, consumption, drain);
    }
}
