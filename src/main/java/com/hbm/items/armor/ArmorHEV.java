package com.hbm.items.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;

/**
 * Ported from CE's {@code com.hbm.items.armor.ArmorHEV} (218 lines) - the HEV power-armor set.
 * CE's real body is almost entirely client-side: the custom armor/vanilla-armor-bar-hiding HUD
 * overlay ({@code handleOverlay}/{@code renderOverlay}, a {@code RenderGameOverlayEvent.Pre}
 * listener replacing the vanilla armor/health bars with a custom charge/radiation readout) and the
 * armor model/renderer - all Phase 5 per {@code docs/phase3/armor_equippable_framework.md}'s
 * Deferred scope. No non-rendering behavior of its own beyond {@link ArmorFSBPowered}'s.
 */
public class ArmorHEV extends ArmorFSBPowered {

    public ArmorHEV(Holder<ArmorMaterial> material, Type type, Item.Properties properties,
                     long maxPower, long chargeRate, long consumption, long drain) {
        super(material, type, properties, maxPower, chargeRate, consumption, drain);
    }
}
