package com.hbm.items.armor;

import com.hbm.items.gear.ArmorFSB;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;

/**
 * Ported from CE's {@code com.hbm.items.armor.ArmorTaurun} (84 lines) - the Taurun set. CE's whole
 * class is client-model/renderer plumbing (Phase 5); no behavior of its own beyond
 * {@link ArmorFSB}'s. CE's constructor forces {@code setMaxDamage(0)} (indestructible) - the caller
 * ({@code PoweredArmorItems}) passes {@code Item.Properties#durability(0)} to match, rather than
 * this class re-deriving it from the material.
 */
public class ArmorTaurun extends ArmorFSB {

    public ArmorTaurun(Holder<ArmorMaterial> material, Type type, Item.Properties properties) {
        super(material, type, properties);
    }
}
