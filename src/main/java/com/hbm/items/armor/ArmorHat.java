package com.hbm.items.armor;

import com.hbm.items.gear.ArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * CE {@code ArmorHat} / registry id {@code nossy_hat}. Client OBJ model lives in
 * {@link com.hbm.client.ArmorHatClient} so dedicated server RegisterEvent does not load
 * {@code net.minecraft.client.model.Model}.
 */
public class ArmorHat extends ArmorModel {

    public ArmorHat(Holder<ArmorMaterial> material, Type type, Item.Properties properties) {
        super(material, type, properties);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected Model getGenericArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
        return com.hbm.client.ArmorHatClient.model(entity, stack, slot, original);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        entity.discard();
        return true;
    }
}
