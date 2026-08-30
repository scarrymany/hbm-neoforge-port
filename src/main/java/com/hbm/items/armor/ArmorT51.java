package com.hbm.items.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;

/**
 * Ported from CE's {@code com.hbm.items.armor.ArmorT51} (63 lines) - the T-51 power-armor set.
 * Like {@link ArmorAJR}, CE's whole class is client-model/renderer plumbing (Phase 5); no behavior
 * of its own beyond {@link ArmorFSBPowered}'s.
 */
public class ArmorT51 extends ArmorFSBPowered {

    public ArmorT51(Holder<ArmorMaterial> material, Type type, Item.Properties properties,
                     long maxPower, long chargeRate, long consumption, long drain) {
        super(material, type, properties, maxPower, chargeRate, consumption, drain);
    }
}
