package com.hbm.items.armor;

import com.hbm.items.gear.ArmorFSB;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;

/**
 * Ported from CE's {@code com.hbm.items.armor.ArmorBismuth} (94 lines) - the Bismuth dash-suit.
 * CE's whole class is client-model/renderer plumbing (Phase 5); the dash mechanic itself is
 * {@link ArmorFSB#dashCount}/{@link ArmorFSB#setDashCount}, which already exists on the ported
 * base class and is set from {@code PoweredArmorItems}' builder chain - this class contributes no
 * behavior of its own.
 */
public class ArmorBismuth extends ArmorFSB {

    public ArmorBismuth(Holder<ArmorMaterial> material, Type type, Item.Properties properties) {
        super(material, type, properties);
    }
}
