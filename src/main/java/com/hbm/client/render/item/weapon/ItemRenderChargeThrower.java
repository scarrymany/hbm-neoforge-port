package com.hbm.client.render.item.weapon;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.content.GunEnergyItems;
import com.hbm.items.weapon.sedna.content.GunHeavyItems;
import com.hbm.items.weapon.sedna.content.GunLauncherItems;
import com.hbm.items.weapon.sedna.content.GunPistolItems;
import com.hbm.items.weapon.sedna.content.GunRifleItems;
import com.hbm.items.weapon.sedna.content.GunShotgunItems;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.main.MainRegistry;
import com.hbm.render.loader.HbmObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.hbm.items.weapon.sedna.content.XFactoryTool;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;

public class ItemRenderChargeThrower extends ItemRenderGunBase {

    @Override
    protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 0F : -0.5F; }

    @Override
    public float getViewFOV(ItemStack stack, float fov) {
        float aimingProgress = net.minecraft.util.Mth.lerp(partialTick, ItemGunBaseNT.prevAimingProgress, ItemGunBaseNT.aimingProgress);
        return  fov * (1 - aimingProgress * (isScoped(stack) ? 0.66F : 0.33F));
    }

    @Override
    protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack) {
        poseStack.translate(0F, 0F, 0.875F);

        float offset = 0.8F;
        float zoom = 0.5F;

        if(isScoped(stack)) standardAimingTransform(poseStack,
                -1.5F * offset, -1.25F * offset, 3.5F * offset,
                -0.15625, -6.5 / 8D, 1.6875);
        else standardAimingTransform(poseStack,
                -1.5F * offset, -1.25F * offset, 3.5F * offset,
                -1.5F * zoom, -1.25F * zoom, 3.5F * zoom);
    }

    @Override
    protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        boolean usingScope = this.isScoped(stack) && gun.aimingProgress == 1 && gun.prevAimingProgress == 1;
        MagazineFullReload mag = (MagazineFullReload) gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);

        if(usingScope) {
            double scale = 3.5D;
            poseStack.scale((float)(scale), (float)(scale), (float)(scale));
            poseStack.translate(-0.5F, -1.5F, -4F);
        } else {
            double scale = 0.5D;
            poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        }

        boolean reloading = GunAnimationClientState.getRelevantAnim(0) != null && GunAnimationClientState.getRelevantAnim(0).animation.getBus("AMMO") != null;
        double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
        double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
        double[] raise = GunAnimationClientState.getRelevantTransformation("RAISE");
        double[] ammo = GunAnimationClientState.getRelevantTransformation("AMMO");
        double[] twist = GunAnimationClientState.getRelevantTransformation("TWIST");
        double[] turn = GunAnimationClientState.getRelevantTransformation("TURN");
        double[] roll = GunAnimationClientState.getRelevantTransformation("ROLL");

        poseStack.translate(0F, 0F, -7F);
        poseStack.mulPose(Axis.XN.rotationDegrees((float) ((float) equip[0])));
        poseStack.translate(0F, 0F, 7F);

        poseStack.translate(0F, -7F, 4F);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) raise[0])));
        poseStack.translate(0F, 7F, -4F);

        poseStack.translate((float) recoil[0], (float) recoil[1], (float) recoil[2]);

        poseStack.translate(0F, 0F, -2F);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) ((float) turn[1])));
        poseStack.translate(0F, 0F, 2F);
        poseStack.translate(0F, -1F, 0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) roll[2])));
        poseStack.translate(0F, 1F, 0F);

        
        /* bind */ currentTex = GunModels.tex("charge_thrower_tex");
        renderPart(GunModels.obj("charge_thrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
        if(isScoped(stack) && !usingScope) renderPart(GunModels.obj("charge_thrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Scope");

        if(mag.getAmount(stack, null) > 0 || reloading) {

            poseStack.translate((float) ammo[0], (float) ammo[1], (float) ammo[2]);
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) twist[2])));

            if(mag.getType(stack, null) == XFactoryTool.ct_hook) {
                /* bind */ currentTex = GunModels.tex("charge_thrower_hook_tex");
                renderPart(GunModels.obj("charge_thrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Hook");
            }
            if(mag.getType(stack, null) == XFactoryTool.ct_mortar) {
                /* bind */ currentTex = GunModels.tex("charge_thrower_mortar_tex");
                renderPart(GunModels.obj("charge_thrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Mortar");
            }
            if(mag.getType(stack, null) == XFactoryTool.ct_mortar_charge) {
                /* bind */ currentTex = GunModels.tex("charge_thrower_mortar_tex");
                renderPart(GunModels.obj("charge_thrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Mortar");
                renderPart(GunModels.obj("charge_thrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Oomph");
            }
        }

        
    }

    @Override
    protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
        super.setupThirdPersonGun(stack, poseStack);
        double scale = 1.5D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.translate(0.75F, 1F, 4F);
    }

    @Override
    protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
        super.setupInventoryGun(stack, poseStack);
        double scale = 1.25D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (25F)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (45F)));
        poseStack.translate(0F, 0F, -0.625F);
    }

    @Override
    protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
        

        
        /* bind */ currentTex = GunModels.tex("charge_thrower_tex");
        renderPart(GunModels.obj("charge_thrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
        if(isScoped(stack)) renderPart(GunModels.obj("charge_thrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Scope");

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        MagazineFullReload mag = (MagazineFullReload) gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);

        if(mag.getAmount(stack, null) > 0) {

            if(mag.getType(stack, null) == XFactoryTool.ct_hook) {
                /* bind */ currentTex = GunModels.tex("charge_thrower_hook_tex");
                renderPart(GunModels.obj("charge_thrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Hook");
            }
            if(mag.getType(stack, null) == XFactoryTool.ct_mortar) {
                /* bind */ currentTex = GunModels.tex("charge_thrower_mortar_tex");
                renderPart(GunModels.obj("charge_thrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Mortar");
            }
            if(mag.getType(stack, null) == XFactoryTool.ct_mortar_charge) {
                /* bind */ currentTex = GunModels.tex("charge_thrower_mortar_tex");
                renderPart(GunModels.obj("charge_thrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Mortar");
                renderPart(GunModels.obj("charge_thrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Oomph");
            }
        }

        
    }

    public boolean isScoped(ItemStack stack) {
        return XWeaponModManager.hasUpgrade(stack, 0, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "scope"));
    }
}
