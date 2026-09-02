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

public class ItemRenderStarFAkimbo extends ItemRenderGunBase {

    @Override public boolean isAkimbo() { return true; }

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
    }

    @Override
    protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();

        float offset = 0.8F;

        for(int i = -1; i <= 1; i += 2) {
            int index = i == -1 ? 0 : 1;
            /* bind */ currentTex = GunModels.tex("star_f_elite_tex");

            poseStack.pushPose();
            standardAimingTransform(poseStack, -2F * offset * i, -1.75F * offset, 2.5F * offset, 0, -7.625 / 8D, 1);

            double scale = 0.25D;
            poseStack.scale((float)(scale), (float)(scale), (float)(scale));

            double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP", index);
            double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL", index);
            double[] hammer = GunAnimationClientState.getRelevantTransformation("HAMMER", index);
            double[] tilt = GunAnimationClientState.getRelevantTransformation("TILT", index);
            double[] turn = GunAnimationClientState.getRelevantTransformation("TURN", index);
            double[] mag = GunAnimationClientState.getRelevantTransformation("MAG", index);
            double[] bullet = GunAnimationClientState.getRelevantTransformation("BULLET", index);
            double[] slide = GunAnimationClientState.getRelevantTransformation("SLIDE", index);

            poseStack.translate(0, -2, -8);
            poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) equip[0])));
            poseStack.translate(0, 2, 8);

            poseStack.translate(0, 1, -3);
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) (turn[2] * i))));
            poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) tilt[0])));
            poseStack.translate(0, -1, 3);

            poseStack.translate(0, 0, recoil[2]);

            
            renderPart(GunModels.obj("star_f"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");

            poseStack.pushPose();
            poseStack.translate(0, 1.75, -4.25);
            poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) (60 * (hammer[0] - 1)))));
            poseStack.translate(0, -1.75, 4.25);
            renderPart(GunModels.obj("star_f"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Hammer");
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.translate(0, 0, slide[2] * 2.3125);
            renderPart(GunModels.obj("star_f"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Slide");
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.translate(mag[0], mag[1], mag[2]);
            renderPart(GunModels.obj("star_f"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Mag");
            poseStack.translate(bullet[0], bullet[1], bullet[2]);
            renderPart(GunModels.obj("star_f"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Bullet");
            poseStack.popPose();

            if(hasSilencer(stack, index)) {
                poseStack.pushPose();
                poseStack.translate(0, 2.375, -0.25);
                /* bind */ currentTex = GunModels.tex("uzi_tex");
                renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Silencer");
                poseStack.popPose();

            } else {
                double smokeScale = 0.5;

                poseStack.pushPose();
                poseStack.translate(0, 3, 6.125);
                poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
                poseStack.scale((float)(smokeScale), (float)(smokeScale), (float)(smokeScale));
                /* TODO(CE:ItemRenderStarFAkimbo:smokeNodes) smokeNodes not ported */
                poseStack.popPose();

                

                poseStack.pushPose();
                poseStack.translate(0, 3, 6.125);
                poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
                poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) (90 * gun.shotRand))));
                renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[index], 75, 7.5);
                poseStack.popPose();
            }

            poseStack.popPose();
        }
    }

    @Override
    protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
        super.setupThirdPersonGun(stack, poseStack);
        poseStack.translate(0, -0.25, 1.75);
        double scale = 0.75D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
    }

    @Override
    public void setupThirdPersonAkimbo(ItemStack stack, PoseStack poseStack) {
        super.setupThirdPersonAkimbo(stack, poseStack);
        poseStack.translate(0, -0.25, 1.75);
        double scale = 0.75D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
    }

    @Override
    protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
        // TODO(CE:1.12 leftover) GlStateManager.enableAlpha();
        poseStack.scale((float)(1), (float)(1), (float)(-1));
        poseStack.translate(8, 8, 0);
        double scale = 1.5D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
    }

    public void renderEquipped(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderStandardGun(stack, 1, poseStack, bufferSource, packedLight, packedOverlay);
    }

    public void renderEquippedAkimbo(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderStandardGun(stack, 0, poseStack, bufferSource, packedLight, packedOverlay);
    }

    public void renderModTable(ItemStack stack, int index, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
        

        
        /* bind */ currentTex = GunModels.tex("star_f_elite_tex");
        renderPart(GunModels.obj("star_f"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
        renderPart(GunModels.obj("star_f"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Slide");
        renderPart(GunModels.obj("star_f"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Mag");
        renderPart(GunModels.obj("star_f"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Hammer");
        if(hasSilencer(stack, index)) {
            poseStack.translate(0, 2.375, -0.25);
            /* bind */ currentTex = GunModels.tex("uzi_tex");
            renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Silencer");
        }
        
    }

    public void renderEntity(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        boolean anySilenced = hasSilencer(stack, 0) || hasSilencer(stack, 1);
        if (anySilenced) {
            poseStack.scale(0.75F, 0.75F, 0.75F);
        }
        poseStack.pushPose();
        poseStack.translate(-1, 1, 0);
        renderStandardGun(stack, 1, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(1, 1, 0);
        renderStandardGun(stack, 0, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
        
        

        boolean anySilenced = hasSilencer(stack, 0) || hasSilencer(stack, 1);

        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) (225)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
        poseStack.translate(0.5, 0, 0);
        if(anySilenced) {
            double scale = 0.625D;
            poseStack.scale((float)(scale), (float)(scale), (float)(scale));
            poseStack.translate(0, 0, -4);
        }
        renderStandardGun(stack, 1, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.translate(0, 0, 5);

        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) (225)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (-90)));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (-90)));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (-45)));
        poseStack.translate(-0.5, 0, 0);
        if(anySilenced) {
            double scale = 0.625D;
            poseStack.scale((float)(scale), (float)(scale), (float)(scale));
            poseStack.translate(0, 0, -4);
        }
        renderStandardGun(stack, 0, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    public boolean hasSilencer(ItemStack stack, int cfg) {
        return XWeaponModManager.hasUpgrade(stack, cfg, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "silencer"));
    }

    public void renderStandardGun(ItemStack stack, int index, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = GunModels.tex("star_f_elite_tex");
        renderPart(GunModels.obj("star_f"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
        renderPart(GunModels.obj("star_f"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Slide");
        renderPart(GunModels.obj("star_f"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Mag");
        renderPart(GunModels.obj("star_f"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Hammer");
        boolean silenced = hasSilencer(stack, index);
        if(silenced) {
            poseStack.translate(0, 2.375, -0.25);
            /* bind */ currentTex = GunModels.tex("uzi_tex");
            renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Silencer");
        }
        
    }
}
