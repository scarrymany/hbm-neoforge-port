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

public class ItemRenderUziAkimbo extends ItemRenderGunBase {

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
        // TODO(CE:1.12 leftover) Minecraft.getMinecraft().renderEngine.bindTexture(isSaturnite(stack, index) ? GunModels.tex("uzi_saturnite_tex") : GunModels.tex("uzi_tex"));

            poseStack.pushPose();
            standardAimingTransform(poseStack, -2.25F * offset * i, -1.5F * offset, 2.5F * offset, 0, -4.375 / 8D, 1);

            double scale = 0.25D;
            poseStack.scale((float)(scale), (float)(scale), (float)(scale));

            double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP", index);
            double[] stockFront = GunAnimationClientState.getRelevantTransformation("STOCKFRONT", index);
            double[] stockBack = GunAnimationClientState.getRelevantTransformation("STOCKBACK", index);
            double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL", index);
            double[] lift = GunAnimationClientState.getRelevantTransformation("LIFT", index);
            double[] mag = GunAnimationClientState.getRelevantTransformation("MAG", index);
            double[] bullet = GunAnimationClientState.getRelevantTransformation("BULLET", index);
            double[] slide = GunAnimationClientState.getRelevantTransformation("SLIDE", index);
            double[] yeet = GunAnimationClientState.getRelevantTransformation("YEET", index);
            double[] speen = GunAnimationClientState.getRelevantTransformation("SPEEN", index);

            poseStack.translate(yeet[0], yeet[1], yeet[2]);
            poseStack.mulPose(Axis.YP.rotationDegrees((float) (speen[0])));

            poseStack.translate(0, -2, -4);
            poseStack.mulPose(Axis.XP.rotationDegrees((float) (equip[0])));
            poseStack.translate(0, 2, 4);

            poseStack.translate(0, 0, -6);
            poseStack.mulPose(Axis.XP.rotationDegrees((float) (lift[0])));
            poseStack.translate(0, 0, 6);

            poseStack.translate(0, 0, recoil[2]);

            
            renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, index == 0 ? "GunMirror" : "Gun");

            boolean silenced = hasSilencer(stack, index);
            if(silenced) renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Silencer");

            poseStack.pushPose();
            poseStack.translate(0, 0.3125D, -5.75);
            poseStack.mulPose(Axis.XP.rotationDegrees((float) (180 - stockFront[0])));
            poseStack.translate(0, -0.3125D, 5.75);
            renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "StockFront");

            poseStack.translate(0, -0.3125D, -3);
            poseStack.mulPose(Axis.XP.rotationDegrees((float) (-200 - stockBack[0])));
            poseStack.translate(0, 0.3125D, 3);
            renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "StockBack");
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.translate(0, 0, slide[2]);
            renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Slide");
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.translate(mag[0], mag[1], mag[2]);
            renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Magazine");
            if(bullet[0] == 1) renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Bullet");
            poseStack.popPose();

            if(!silenced) {
                double smokeScale = 0.5;

                poseStack.pushPose();
                poseStack.translate(0, 0.75, 8.5);
                poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
                poseStack.scale((float)(smokeScale), (float)(smokeScale), (float)(smokeScale));
                /* TODO(CE:ItemRenderUziAkimbo:smokeNodes) smokeNodes not ported */
                poseStack.popPose();

                

                poseStack.pushPose();
                poseStack.translate(0, 0.75, 8.5);
                poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
                poseStack.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
                renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[index], 75, 7.5);
                poseStack.popPose();
            };

            poseStack.popPose();
        }
    }

    @Override
    protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
        super.setupThirdPersonGun(stack, poseStack);
        poseStack.translate(0, 1, 1);
    }

    @Override
    public void setupThirdPersonAkimbo(ItemStack stack, PoseStack poseStack) {
        super.setupThirdPersonAkimbo(stack, poseStack);
        poseStack.translate(0, 1, 1);
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
        ResourceLocation currentTex = this.defaultTex();

        
        // TODO(CE:1.12 leftover) Minecraft.getMinecraft().renderEngine.bindTexture(isSaturnite(stack, 1) ? GunModels.tex("uzi_saturnite_tex") : GunModels.tex("uzi_tex"));
        renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
        renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "StockBack");
        renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "StockFront");
        renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Slide");
        renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Magazine");
        if(hasSilencer(stack, 0)) renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Silencer");
        
    }

    public void renderEquippedAkimbo(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

        
        // TODO(CE:1.12 leftover) Minecraft.getMinecraft().renderEngine.bindTexture(isSaturnite(stack, 1) ? GunModels.tex("uzi_saturnite_tex") : GunModels.tex("uzi_tex"));
        renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "GunMirror");
        renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "StockBack");
        renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "StockFront");
        renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Slide");
        renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Magazine");
        if(hasSilencer(stack, 0)) renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Silencer");
        
    }

    public void renderModTable(ItemStack stack, int index, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
        

        
        // TODO(CE:1.12 leftover) Minecraft.getMinecraft().renderEngine.bindTexture(isSaturnite(stack, index) ? GunModels.tex("uzi_saturnite_tex") : GunModels.tex("uzi_tex"));
        renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, index == 0 ? "GunMirror" : "Gun");
        renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "StockBack");
        renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "StockFront");
        renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Slide");
        renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Magazine");
        if(hasSilencer(stack, index)) renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Silencer");
        
    }

    @Override
    protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
        
        
        boolean silencer0 = hasSilencer(stack, 1);
        boolean silencer1 = hasSilencer(stack, 0);
        boolean anySilenced = silencer0 || silencer1;

        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) (225)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
        poseStack.translate(0, 1, 0);
        if(anySilenced) {
            double scale = 0.625D;
            poseStack.scale((float)(scale), (float)(scale), (float)(scale));
            poseStack.translate(0, 0, -4);
        }
        // TODO(CE:1.12 leftover) Minecraft.getMinecraft().renderEngine.bindTexture(isSaturnite(stack, 1) ? GunModels.tex("uzi_saturnite_tex") : GunModels.tex("uzi_tex"));
        renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
        renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "StockBack");
        renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "StockFront");
        renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Slide");
        renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Magazine");
        if(silencer0) renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Silencer");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) (-225)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (180)));
        poseStack.translate(0, 1, 0);
        if(anySilenced) {
            double scale = 0.625D;
            poseStack.scale((float)(scale), (float)(scale), (float)(scale));
            poseStack.translate(0, 0, -4);
        }
        // TODO(CE:1.12 leftover) Minecraft.getMinecraft().renderEngine.bindTexture(isSaturnite(stack, 0) ? GunModels.tex("uzi_saturnite_tex") : GunModels.tex("uzi_tex"));
        renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "GunMirror");
        renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "StockBack");
        renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "StockFront");
        renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Slide");
        renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Magazine");
        if(silencer1) renderPart(GunModels.obj("uzi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Silencer");
        poseStack.popPose();

        
    }

    public boolean hasSilencer(ItemStack stack, int cfg) {
        return XWeaponModManager.hasUpgrade(stack, cfg, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "silencer"));
    }

    public boolean isSaturnite(ItemStack stack, int cfg) {
        return XWeaponModManager.hasUpgrade(stack, cfg, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "uzi_saturnite"));
    }
}
