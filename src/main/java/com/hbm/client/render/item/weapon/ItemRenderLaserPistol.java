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

public class ItemRenderLaserPistol extends ItemRenderGunBase {

    public ResourceLocation texture;

    public ItemRenderLaserPistol(ResourceLocation texture) {
        this.texture = texture;
    }

    @Override
    protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F; }

    @Override
    public float getViewFOV(ItemStack stack, float fov) {
        float aimingProgress = net.minecraft.util.Mth.lerp(partialTick, ItemGunBaseNT.prevAimingProgress, ItemGunBaseNT.aimingProgress);
        return  fov * (1 - aimingProgress * 0.33F);
    }

    @Override
    protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack) {
        poseStack.translate(0F, 0F, 0.875F);

        float offset = 0.8F;
        standardAimingTransform(poseStack,
                -1.75F * offset, -2F * offset, 2.75F * offset,
                0, -10 / 8D, 1.25);
    }

    @Override
    protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        /* bind */ currentTex = texture;
        double scale = 0.375D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));

        double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
        double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
        double[] latch = GunAnimationClientState.getRelevantTransformation("LATCH");
        double[] lift = GunAnimationClientState.getRelevantTransformation("LIFT");
        double[] jolt = GunAnimationClientState.getRelevantTransformation("JOLT");
        double[] battery = GunAnimationClientState.getRelevantTransformation("BATTERY");
        double[] swirl = GunAnimationClientState.getRelevantTransformation("SWIRL");

        poseStack.translate(0F, -1F, -6F);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) equip[0])));
        poseStack.translate(0F, 1F, 6F);

        poseStack.translate(0F, 2F, -2F);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) lift[0])));
        poseStack.translate(0F, -2F, 2F);

        poseStack.translate(0F, -1F, -1F);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) swirl[0])));
        poseStack.translate(0F, 1F, 1F);

        poseStack.translate(0F, 0F, (float) recoil[2]);
        poseStack.translate((float) jolt[0], (float) jolt[1], (float) jolt[2]);

        

        renderPart(GunModels.obj("laser_pistol"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
        if(hasCapacitors(stack)) renderPart(GunModels.obj("laser_pistol"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Capacitors");
        if(hasTape(stack)) renderPart(GunModels.obj("laser_pistol"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Tape");

        poseStack.pushPose();
        poseStack.translate(1.125F, 0F, -1.9125F);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) ((float) latch[1])));
        poseStack.translate(-1.125F, 0F, 1.9125F);
        renderPart(GunModels.obj("laser_pistol"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Latch");
        poseStack.translate((float) battery[0], (float) battery[1], (float) battery[2]);
        renderPart(GunModels.obj("laser_pistol"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Battery");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0F, 2F, 4.75F);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (90F)));
        renderLaserFlash(poseStack, bufferSource, gun.lastShot[0], 150, 1.5D, hasEmerald(stack) ? 0x008000 : 0xff0000);
        poseStack.translate(0F, 0F, -0.25F);
        renderLaserFlash(poseStack, bufferSource, gun.lastShot[0], 150, 0.75D, hasEmerald(stack) ? 0x80ff00 : 0xff8000);
        poseStack.popPose();

        
    }

    @Override
    protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
        super.setupThirdPersonGun(stack, poseStack);
        double scale = 1.25D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.translate(0F, -0.5F, 1F);
    }

    @Override
    protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
        super.setupInventoryGun(stack, poseStack);
        double scale = 1.75D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (25F)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (45F)));
        poseStack.translate(0F, -0.5F, 0F);
    }

    @Override
    protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
        

        
        /* bind */ currentTex = texture;
        renderPart(GunModels.obj("laser_pistol"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
        renderPart(GunModels.obj("laser_pistol"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Latch");
        if(hasCapacitors(stack)) renderPart(GunModels.obj("laser_pistol"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Capacitors");
        if(hasTape(stack)) renderPart(GunModels.obj("laser_pistol"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Tape");
        
    }

    public boolean hasCapacitors(ItemStack stack) {
        return stack.getItem() == GunEnergyItems.GUN_LASER_PISTOL_PEW_PEW.get();
    }

    public boolean hasTape(ItemStack stack) {
        return stack.getItem() == GunEnergyItems.GUN_LASER_PISTOL_PEW_PEW.get();
    }

    public boolean hasEmerald(ItemStack stack) {
        return stack.getItem() == GunEnergyItems.GUN_LASER_PISTOL_MORNING_GLORY.get();
    }
}
