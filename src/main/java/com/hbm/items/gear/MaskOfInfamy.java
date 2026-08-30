package com.hbm.items.gear;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;

/**
 * Ported from CE's {@code com.hbm.items.gear.MaskOfInfamy} - 1 item ({@code mask_of_infamy},
 * {@link com.hbm.items.gear.SpecialArmorItems}), a trivial cosmetic {@code ItemArmor} subclass with
 * zero overridden behavior beyond a texture path, already handled generically by this item's own
 * material (see {@code com.hbm.items.gear.ModArmor}'s javadoc). Genuinely the simplest of the 7
 * files this package covers - no {@code ArmorUtil}/{@code ArmorRegistry}/{@code ISpecialArmor}
 * dependency of any kind.
 *
 * <p>CE's constructor never calls {@code setCreativeTab} (unlike every other file in this package),
 * and no recipe was found for it in the CE source read for this package's research pass - this
 * port preserves that: {@code SpecialArmorItems} does not add this item to any creative tab or
 * register a recipe for it, matching a give-command-only novelty item rather than inventing
 * obtainability CE never had.
 */
public class MaskOfInfamy extends ArmorItem {

    public MaskOfInfamy(Holder<ArmorMaterial> material, Type type, Item.Properties properties) {
        super(material, type, properties);
    }
}
