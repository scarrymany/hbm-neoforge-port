package com.hbm.client;

import com.hbm.client.render.armor.ObjArmorModel;
import com.hbm.main.MainRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/** Client-only cache for {@code nossy_hat}. Must not be referenced from a static field on the item. */
public final class ArmorHatClient {

    private static final ResourceLocation OBJ =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "models/armor/hat.obj");
    private static final Map<EquipmentSlot, ObjArmorModel.SlotRecipe> RECIPES = Map.of(
            EquipmentSlot.HEAD, ObjArmorModel.SlotRecipe.wholeModel(
                    ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/armor/hat.png")));

    private static ObjArmorModel model;

    private ArmorHatClient() {
    }

    public static Model model(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
        if (slot != EquipmentSlot.HEAD) return original;
        if (model == null) model = new ObjArmorModel(slot, OBJ, RECIPES);
        model.getPropertiesFrom(original, entity);
        return model;
    }
}
