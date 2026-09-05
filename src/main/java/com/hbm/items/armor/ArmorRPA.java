package com.hbm.items.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;

/**
 * Ported from CE's {@code com.hbm.items.armor.ArmorRPA} (74 lines) - the Remnant power-armor set.
 * CE's real body beyond client-model/renderer plumbing (Phase 5) is exactly the
 * {@link IPAWeaponsProvider} wiring below: a shared {@link ArmorRPAMelee} instance gated on the
 * full charged set being worn (CE: {@code hasFSBArmorIgnoreCharge}), no ranged component at all
 * (CE's own {@code getRangedComponent} unconditionally returns {@code null} - Remnant armor has no
 * built-in gun, only the melee "arm slam"). Melee click + orchestra are Exact CE
 * ({@code ArmorRPAMelee.java:24-70}).
 */
public class ArmorRPA extends ArmorFSBPowered implements IPAWeaponsProvider {

    private static final ArmorRPAMelee MELEE_COMPONENT = new ArmorRPAMelee();

    public ArmorRPA(Holder<ArmorMaterial> material, Type type, Item.Properties properties,
                     long maxPower, long chargeRate, long consumption, long drain) {
        super(material, type, properties, maxPower, chargeRate, consumption, drain);
    }

    @Override
    public IPAMelee getMeleeComponent(Player entity) {
        return hasFSBArmorIgnoreCharge(entity) ? MELEE_COMPONENT : null;
    }

    @Override
    public IPARanged getRangedComponent(Player entity) {
        return null;
    }
}
