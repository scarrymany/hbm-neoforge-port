package com.hbm.client.render.armor;

import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;

import com.hbm.main.MainRegistry;

/**
 * Client model for CE's HEV power-armor set ({@code com.hbm.items.armor.ArmorHEV} / this port's
 * {@link com.hbm.items.armor.ArmorHEV}, registered as {@code PoweredArmorItems.HEV_HELMET}/{@code
 * HEV_PLATE}/{@code HEV_LEGS}/{@code HEV_BOOTS}) - CE's real client model is {@code
 * render/model/ModelArmorHEV.java} (64 lines, read in full for this task - see {@code
 * docs/phase5/armor_humanoidmodel_rendering.md}'s "Sources read in full" list, where it is that
 * report's own chosen "representative concrete OBJ-per-slot leaf" example): a {@code type} int
 * (0=helmet, 1=chest+arms, 2=legs, 3=boots) dispatches to {@code ModelRendererObj}-wrapped OBJ mesh
 * groups ("Head"/"Body"/"LeftArm"/"RightArm"/"LeftLeg"/"RightLeg"/"LeftFoot"/"RightFoot") cut from
 * {@code ResourceManager.armor_hev} ({@code models/armor/hev.obj}), each bound to its own PNG
 * (helmet/chest/arm/leg textures, {@code ResourceManager.hev_{helmet,chest,arm,leg}}).
 *
 * <h2>Real geometry, via {@link ObjArmorModel}</h2>
 * This was originally this package's single hand-written "one clearly-documented placeholder
 * example" leaf (a prior task's own scope boundary). Task {@code c7-armor-model-rendering} widened
 * scope to every real CE armor set and, in the process, extracted the structural shape every
 * bucket-(a) OBJ-driven leaf shares (see {@link ObjArmorModel}'s own class javadoc for the full
 * design rationale, including its honest single-texture-per-slot limitation) into that one reusable
 * class - this class is now a thin, fixed-configuration subclass of it (kept as its own file/name
 * only so {@link ArmorRenderRegistry#registerHev}'s existing {@code HevArmorModel::new} factory
 * reference needs no edit), rather than one of the ~15 other sets' inline lambda registrations in
 * {@link ArmorRenderRegistry}.
 */
public final class HevArmorModel extends ObjArmorModel {

    private static final ResourceLocation OBJ = rl("models/armor/hev.obj");

    private static final Map<EquipmentSlot, SlotRecipe> RECIPES = Map.of(
            EquipmentSlot.HEAD, SlotRecipe.of(rl("textures/armor/hev_helmet.png"), "Head"),
            EquipmentSlot.CHEST, SlotRecipe.of(rl("textures/armor/hev_chest.png"), "Body", "LeftArm", "RightArm"),
            EquipmentSlot.LEGS, SlotRecipe.of(rl("textures/armor/hev_leg.png"), "LeftLeg", "RightLeg"),
            EquipmentSlot.FEET, SlotRecipe.of(rl("textures/armor/hev_leg.png"), "LeftFoot", "RightFoot")
    );

    public HevArmorModel(EquipmentSlot slot) {
        super(slot, OBJ, RECIPES);
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path);
    }
}
