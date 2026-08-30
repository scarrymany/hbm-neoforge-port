package com.hbm.items.armor;

import com.hbm.items.gear.ArmorModel;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Ported from CE's {@code com.hbm.items.armor.ArmorHat} (19 lines) - the cosmetic "nossy_hat" item
 * (CE field name {@code hat}, real registry id {@code nossy_hat} - the item constructor's own
 * {@code s} name param, not the unrelated Java field name). Extends {@link ArmorModel} (custom-hat
 * model hook, Phase 5) rather than {@link com.hbm.items.gear.ArmorFSB} - this piece has no full-set
 * bonus mechanic of any kind, just CE's one behavior: a dropped hat instantly deletes itself instead
 * of sitting on the ground (CE: {@code onEntityItemUpdate} -&gt; {@code entityItem.setDead()}, this
 * port's confirmed-real {@code Item#onEntityItemUpdate(ItemStack, ItemEntity)} hook - already used
 * by {@code items.special.ItemRag}/{@code ItemDigamma}/{@code ItemCell}).
 */
public class ArmorHat extends ArmorModel {

    public ArmorHat(Holder<ArmorMaterial> material, Type type, Item.Properties properties) {
        super(material, type, properties);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        entity.discard();
        return true;
    }
}
