package com.hbm.items.armor;

import java.util.Map;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.hbm.client.render.armor.ObjArmorModel;
import com.hbm.items.gear.ArmorModel;
import com.hbm.main.MainRegistry;

/**
 * Ported from CE's {@code com.hbm.items.armor.ArmorHat} (19 lines) - the cosmetic "nossy_hat" item
 * (CE field name {@code hat}, real registry id {@code nossy_hat} - the item constructor's own
 * {@code s} name param, not the unrelated Java field name). Extends {@link ArmorModel} (custom-hat
 * model hook, Phase 5) rather than {@link com.hbm.items.gear.ArmorFSB} - this piece has no full-set
 * bonus mechanic of any kind, just CE's one behavior: a dropped hat instantly deletes itself instead
 * of sitting on the ground (CE: {@code onEntityItemUpdate} -&gt; {@code entityItem.setDead()}, this
 * port's confirmed-real {@code Item#onEntityItemUpdate(ItemStack, ItemEntity)} hook - already used
 * by {@code items.special.ItemRag}/{@code ItemDigamma}/{@code ItemCell}).
 *
 * <p><b>Client model</b> (task {@code c7-armor-model-rendering}): CE's {@code
 * render/model/ModelHat.java} (29 lines, read in full) only ever renders its {@code type == 0}
 * (helmet) branch, drawing {@code ResourceManager.armor_hat} ({@code models/armor/hat.obj})
 * <i>whole</i> - CE constructs its one {@code ModelRendererObj} with no named-group argument at
 * all, unlike every other bucket-(a) set - reproduced here via {@link
 * ObjArmorModel.SlotRecipe#wholeModel(ResourceLocation)}. Uses the external-registration-hazard-
 * free item-level {@link #getGenericArmorModel} hook {@link ArmorModel} already provides, rather
 * than {@code com.hbm.client.render.armor.ArmorRenderRegistry} (this item already has its own
 * per-item hook from {@link ArmorModel}, so registering it a second time externally would risk a
 * double-registration conflict for zero benefit - see that registry's own updated class javadoc).
 */
public class ArmorHat extends ArmorModel {

    private static final ResourceLocation OBJ =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "models/armor/hat.obj");
    private static final Map<EquipmentSlot, ObjArmorModel.SlotRecipe> RECIPES = Map.of(
            EquipmentSlot.HEAD, ObjArmorModel.SlotRecipe.wholeModel(
                    ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/armor/hat.png")));

    private ObjArmorModel model;

    public ArmorHat(Holder<ArmorMaterial> material, Type type, Item.Properties properties) {
        super(material, type, properties);
    }

    @Override
    protected Model getGenericArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
        if (slot != EquipmentSlot.HEAD) return original;
        if (model == null) model = new ObjArmorModel(slot, OBJ, RECIPES);
        model.getPropertiesFrom(original, entity);
        return model;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        entity.discard();
        return true;
    }
}
