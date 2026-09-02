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

public class ItemRenderAmat extends ItemRenderGunBase {

    public ResourceLocation texture;

    public ItemRenderAmat(ResourceLocation texture) {
        this.texture = texture;
    }

    @Override
    protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F; }

    @Override
    public float getViewFOV(ItemStack stack, float fov) {
        float aimingProgress = net.minecraft.util.Mth.lerp(partialTick, ItemGunBaseNT.prevAimingProgress, ItemGunBaseNT.aimingProgress);
        return  fov * (1 - aimingProgress * (isScoped(stack) ? 0.8F : 0.33F));
    }

    @Override
    protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack) {
        poseStack.translate(0, 0, 0.875);

        float offset = 0.8F;

        standardAimingTransform(poseStack,
                -1F * offset, -1F * offset, 3.25F * offset,
                0, -4.875 / 8D, 1.875);
    }

    @Override
    protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
        boolean isScoped = isScoped(stack);
        if(isScoped && ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1) return;

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        /* bind */ currentTex = texture;
        double scale = 0.375D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));

        boolean deployed = GunAnimationClientState.getRelevantAnim(0) == null || GunAnimationClientState.getRelevantAnim(0).animation.getBus("BIPOD") == null;
        double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
        double[] bipod = GunAnimationClientState.getRelevantTransformation("BIPOD");
        double[] lift = GunAnimationClientState.getRelevantTransformation("LIFT");
        double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
        double[] boltTurn = GunAnimationClientState.getRelevantTransformation("BOLT_TURN");
        double[] boltPull = GunAnimationClientState.getRelevantTransformation("BOLT_PULL");
        double[] mag = GunAnimationClientState.getRelevantTransformation("MAG");
        double[] scopeThrow = GunAnimationClientState.getRelevantTransformation("SCOPE_THROW");
        double[] scopeSpin = GunAnimationClientState.getRelevantTransformation("SCOPE_SPIN");

        poseStack.translate(0, 0, recoil[2]);

        poseStack.translate(0, -3, -8);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (equip[0])));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (lift[0])));
        poseStack.translate(0, 3, 8);

        
        renderPart(GunModels.obj("amat"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");

        if(isScoped(stack)) {
            poseStack.pushPose();
            poseStack.translate(scopeThrow[0], scopeThrow[1], scopeThrow[2]);
            poseStack.translate(0, 1.5, -4.5);
            poseStack.mulPose(Axis.XP.rotationDegrees((float) (scopeSpin[0])));
            poseStack.translate(0, -1.5, 4.5);
            renderPart(GunModels.obj("amat"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Scope");
            poseStack.popPose();
        }

        poseStack.pushPose();
        poseStack.translate(0, 0.625, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) (boltTurn[2])));
        poseStack.translate(0, -0.625, 0);
        poseStack.translate(0, 0, boltPull[2]);
        renderPart(GunModels.obj("amat"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Bolt");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(mag[0], mag[1], mag[2]);
        renderPart(GunModels.obj("amat"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Magazine");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.3125, -0.625, -1);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) (deployed ? 25 : bipod[1])));
        poseStack.translate(-0.3125, 0.625, 1);
        renderPart(GunModels.obj("amat"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "BipodHingeLeft");
        poseStack.translate(0.3125, -0.625, -1);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (deployed ? 80 : bipod[0])));
        poseStack.translate(-0.3125, 0.625, 1);
        renderPart(GunModels.obj("amat"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "BipodLeft");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(-0.3125, -0.625, -1);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) (deployed ? -25 : -bipod[1])));
        poseStack.translate(0.3125, 0.625, 1);
        renderPart(GunModels.obj("amat"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "BipodHingeRight");
        poseStack.translate(-0.3125, -0.625, -1);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (deployed ? 80 : bipod[0])));
        poseStack.translate(0.3125, 0.625, 1);
        renderPart(GunModels.obj("amat"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "BipodRight");
        poseStack.popPose();

        if(isSilenced(stack)) {
            poseStack.translate(0, 0.625, -4.3125);
            poseStack.scale((float)(1.25), (float)(1.25), (float)(1.25));
            /* bind */ currentTex = GunModels.tex("g3_attachments");
            renderPart(GunModels.obj("g3"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Silencer");

            
        } else {
            renderPart(GunModels.obj("amat"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "MuzzleBrake");

            double smokeScale = 0.5;

            poseStack.pushPose();
            poseStack.translate(0, 0.625, 12);
            poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
            poseStack.scale((float)(smokeScale), (float)(smokeScale), (float)(smokeScale));
            /* TODO(CE:ItemRenderAmat:smokeNodes) smokeNodes not ported */
            poseStack.popPose();

            

            poseStack.pushPose();
            poseStack.translate(0, 0.5, 11);
            poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
            poseStack.scale((float)(0.75), (float)(0.75), (float)(0.75));
            renderGapFlash(poseStack, bufferSource, gun.lastShot[0]);
            poseStack.popPose();
        }
    }

    @Override
    protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
        super.setupThirdPersonGun(stack, poseStack);
        double scale = 1.25D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.translate(0, 0.5, 6.75);
    }

    @Override
    protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
        super.setupInventoryGun(stack, poseStack);
        if(isSilenced(stack)) {
            double scale = 0.8175D;
            poseStack.scale((float)(scale), (float)(scale), (float)(scale));
            poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
            poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
            poseStack.translate(-0.5, 0.5, -1);
        } else {
            double scale = 0.9375D;
            poseStack.scale((float)(scale), (float)(scale), (float)(scale));
            poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
            poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
            poseStack.translate(-0.5, 0.5, 0);
        }
    }

    @Override
    protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
        

        
        /* bind */ currentTex = texture;
        renderPart(GunModels.obj("amat"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
        renderPart(GunModels.obj("amat"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Bolt");
        renderPart(GunModels.obj("amat"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Magazine");
        renderPart(GunModels.obj("amat"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "BipodLeft");
        renderPart(GunModels.obj("amat"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "BipodHingeLeft");
        renderPart(GunModels.obj("amat"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "BipodRight");
        renderPart(GunModels.obj("amat"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "BipodHingeRight");
        if(isScoped(stack)) renderPart(GunModels.obj("amat"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Scope");
        if(isSilenced(stack)) {
            poseStack.translate(0, 0.625, -4.3125);
            poseStack.scale((float)(1.25), (float)(1.25), (float)(1.25));
            /* bind */ currentTex = GunModels.tex("g3_attachments");
            renderPart(GunModels.obj("g3"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Silencer");
        } else {
            renderPart(GunModels.obj("amat"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "MuzzleBrake");
        }
        
    }

    public boolean isScoped(ItemStack stack) {
        return true;
    }

    public boolean isSilenced(ItemStack stack) {
        return stack.getItem() == GunRifleItems.GUN_AMAT_PENANCE.get() || XWeaponModManager.hasUpgrade(stack, 0, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "silencer"));
    }
}
