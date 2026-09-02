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

public class ItemRenderLAG extends ItemRenderGunBase {

    public ItemRenderLAG() {}

    @Override
    protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

    @Override
    public float getViewFOV(ItemStack stack, float fov) {
        float aimingProgress = net.minecraft.util.Mth.lerp(partialTick, ItemGunBaseNT.prevAimingProgress, ItemGunBaseNT.aimingProgress);
        return  fov * (1 - aimingProgress * 0.33F);
    }

    @Override
    protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack) {
        poseStack.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(poseStack,
                -1.5F * offset, -1F * offset, 1.5F * offset,
                0, -3.375 / 8D, 0.5);
    }

    @Override
    protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        /* bind */ currentTex = GunModels.tex("mike_hawk_tex");
        double scale = 0.25D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));

        double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
        double[] addTrans = GunAnimationClientState.getRelevantTransformation("ADD_TRANS");
        double[] addRot = GunAnimationClientState.getRelevantTransformation("ADD_ROT");

        poseStack.translate(4, -4, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float)-equip[0])));
        poseStack.translate(-4, 4, 0);

        poseStack.translate(addTrans[0], addTrans[1], addTrans[2]);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) addRot[2])));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) ((float)addRot[1])));

        

        poseStack.pushPose();
        GunAnimationClientState.applyRelevantTransformation(poseStack, "Grip");
        renderPart(GunModels.obj("mike_hawk"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Grip");

        poseStack.pushPose();
        GunAnimationClientState.applyRelevantTransformation(poseStack, "Slide");
		
		/*if(anim != null) {
			BusAnimationSequence slideSeq = anim.animation.getBus("Hammer");
			if(slideSeq != null) poseStack.translate(0, 0.75, 0);
		}*/

        renderPart(GunModels.obj("mike_hawk"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Slide");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(3.125, 0.125, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) (-25)));
        poseStack.translate(-3.125, -0.125, 0);
        GunAnimationClientState.applyRelevantTransformation(poseStack, "Hammer");
        renderPart(GunModels.obj("mike_hawk"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Hammer");
        poseStack.popPose();

        if(gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null) > 0) {
            poseStack.pushPose();
            GunAnimationClientState.applyRelevantTransformation(poseStack, "Bullet");
            renderPart(GunModels.obj("mike_hawk"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Bullet");
            poseStack.popPose();
        }

        poseStack.pushPose();
        GunAnimationClientState.applyRelevantTransformation(poseStack, "Magazine");
        renderPart(GunModels.obj("mike_hawk"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Magazine");
        poseStack.popPose();

        double smokeScale = 0.5;

        poseStack.pushPose();
        poseStack.translate(-10.25, 1, 0);
        poseStack.scale((float)(smokeScale), (float)(smokeScale), (float)(smokeScale));
        /* TODO(CE:ItemRenderLAG:smokeNodes) smokeNodes not ported */
        poseStack.popPose();

        

        poseStack.pushPose();
        poseStack.translate(-10.25, 1, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float)(90 * gun.shotRand))));
        renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[0], 75, 7.5);
        poseStack.popPose();

        poseStack.popPose();
    }

    @Override
    protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
        super.setupThirdPersonGun(stack, poseStack);
        poseStack.translate(0, 1, 1);

    }

    @Override
    protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
        super.setupInventoryGun(stack, poseStack);
        double scale = 1.5D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
        poseStack.translate(2.5, 1, 0);
    }

    @Override
    protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
        
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));

        
        /* bind */ currentTex = GunModels.tex("mike_hawk_tex");
        renderPart(GunModels.obj("mike_hawk"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Grip");
        renderPart(GunModels.obj("mike_hawk"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Slide");
        renderPart(GunModels.obj("mike_hawk"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Hammer");
        
    }
}
