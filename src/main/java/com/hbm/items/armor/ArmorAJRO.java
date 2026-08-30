package com.hbm.items.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;

/**
 * Ported from CE's {@code com.hbm.items.armor.ArmorAJRO} (86 lines) - the "open helmet" T-45 AJR
 * variant. Same shape as {@link ArmorAJR}: CE's whole class is client-model/renderer plumbing
 * (Phase 5); no behavior of its own beyond {@link ArmorFSBPowered}'s. Shares
 * {@code MaterialRegistry.aMatAJR} with {@link ArmorAJR} in CE (a cosmetic reskin, not a distinct
 * material).
 */
public class ArmorAJRO extends ArmorFSBPowered {

    public ArmorAJRO(Holder<ArmorMaterial> material, Type type, Item.Properties properties,
                      long maxPower, long chargeRate, long consumption, long drain) {
        super(material, type, properties, maxPower, chargeRate, consumption, drain);
    }
}
