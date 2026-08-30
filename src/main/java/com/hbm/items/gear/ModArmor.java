package com.hbm.items.gear;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;

/**
 * Ported from CE's {@code com.hbm.items.gear.ModArmor}. In CE this class's entire reason to exist
 * was {@code getArmorTexture(ItemStack, Entity, EntityEquipmentSlot, String)}: a big {@code ==}
 * dispatch table so several differently-named armor pieces sharing one 1.12 texture pair
 * (e.g. steel/titanium/alloy/cmb/paa/asbestos/security's helmet+chest+boots vs. their separate
 * legs texture) could each resolve to the right PNG.
 *
 * <p>That entire mechanism is gone in 1.21: {@link ArmorMaterial.Layer} carries one texture path
 * per material (see {@code com.hbm.main.MaterialRegistry}, already ported - every CE material this
 * class's dispatch table named already has its own {@code Holder<ArmorMaterial>} with that path
 * baked in), and vanilla's armor-layer renderer automatically resolves the "_1"/"_2" suffixed file
 * for non-leggings/leggings slots - exactly the same helmet+chest+boots-vs-legs split CE's texture
 * pairs encoded by hand. So {@code ModArmor} needed zero override methods to reproduce CE's texture
 * behavior; it exists only as a plain, named base class so leaf armor items (a later Phase 3
 * package) that had no other CE-side special behavior can extend something more specific than bare
 * {@link ArmorItem}, matching CE's own class hierarchy 1:1.
 */
public class ModArmor extends ArmorItem {

    public ModArmor(Holder<ArmorMaterial> material, Type type, Item.Properties properties) {
        super(material, type, properties);
    }
}
