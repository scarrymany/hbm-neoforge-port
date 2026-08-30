package com.hbm.items.gear;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;

/**
 * Ported from CE's {@code com.hbm.items.gear.ArmorHazmat} - 12 live items across 3 color variants
 * plus the PAA variant ({@code hazmat_plate/_legs/_boots} + {@code _red}/{@code _grey}/{@code _paa}
 * suffixes, {@link com.hbm.items.gear.SpecialArmorItems}). No {@code ISpecialArmor}, no potion
 * effects, no full-set check - the simplest of the 5 live files in
 * {@code docs/phase3/armor_special_sets.md}, behaviorally identical to plain {@link ArmorItem}.
 *
 * <p>CE's own {@code getArmorTexture} 8-branch dispatch table and the client-only helmet-overlay
 * blur are both out of scope here: the texture split is already fully handled by this material's
 * own {@link ArmorMaterial.Layer} path (see {@code com.hbm.items.gear.ModArmor}'s javadoc - one
 * texture per material, vanilla resolves the legs-vs-other suffix automatically, exactly matching
 * CE's per-family texture pair), and the overlay is pure GL-immediate-mode rendering with no
 * confirmed 1.21 client-item-extension equivalent surveyed in this pass (Phase 5, same as
 * {@code ArmorGasMask}'s equivalent overlay).
 *
 * <p>The real "hazmat protection" value (radiation/gas hazard-class registration) is entirely
 * external to this class, exactly as in CE - see {@code com.hbm.handler.ArmorUtil#register()}'s
 * helmet-only {@code ArmorRegistry.registerHazard} calls (this package's own
 * {@code ArmorHazmatMask} pieces only) and {@code com.hbm.handler.HazmatRegistry}'s per-slot
 * multiplier table - chest/legs/boots pieces of this class contribute zero hazard protection or
 * rad-resist of their own, matching CE exactly.
 */
public class ArmorHazmat extends ArmorItem {

    public ArmorHazmat(Holder<ArmorMaterial> material, Type type, Item.Properties properties) {
        super(material, type, properties);
    }
}
