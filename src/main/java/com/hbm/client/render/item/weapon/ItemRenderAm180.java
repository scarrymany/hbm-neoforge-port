package com.hbm.client.render.item.weapon;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.main.MainRegistry;
import com.hbm.render.loader.HbmObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Port of CE's {@code com.hbm.render.item.weapon.sedna.ItemRenderAm180} (171 lines, read in full) -
 * the {@code gun_am180} renderer. Every transform value/part/bus name below is copied verbatim from
 * that class's <b>non-legacy</b> branch only (CE's own {@code ClientConfig.GUN_ANIMS_LEGACY} config
 * flag toggles between a hand-authored fallback and this JSON-driven path at runtime; this port has
 * no equivalent config field and always uses the JSON-driven path, matching CE's own non-legacy
 * default) - see {@link GunAnimationRegistration#AM180_ANIM} for the animation-lookup lambda this
 * renderer's buses are sampled from.
 */
public class ItemRenderAm180 extends ItemRenderGunBase {

    private static final ResourceLocation SILENCER_ID = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "silencer");

    @Override
    protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F;
    }

    @Override
    public float getViewFOV(ItemStack stack, float fov) {
        // See ItemRenderSpas12.getViewFOV's own comment - no interpolated-partial-tick caller exists yet.
        return fov * (1 - ItemGunBaseNT.aimingProgress * 0.33F);
    }

    @Override
    protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack) {
        poseStack.translate(0.0, 0.0, 0.875);
        float offset = 0.8F;
        standardAimingTransform(poseStack,
                -1F * offset, -1F * offset, offset,
                0, -4.1875 / 8D, 0.25);
    }

    @Override
    protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource,
                                      int packedLight, int packedOverlay) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        HbmObjModel model = GunModels.am180();
        if (model == null) return;

        float scale = 0.1875F;
        poseStack.scale(scale, scale, scale);

        boolean silenced = hasSilencer(stack);

        double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
        double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
        double[] magazine = GunAnimationClientState.getRelevantTransformation("MAG");
        double[] magTurn = GunAnimationClientState.getRelevantTransformation("MAGTURN");
        double[] magSpin = GunAnimationClientState.getRelevantTransformation("MAGSPIN");
        double[] bolt = GunAnimationClientState.getRelevantTransformation("BOLT");
        double[] turn = GunAnimationClientState.getRelevantTransformation("TURN");

        poseStack.translate(0.0, -2.0, -6.0);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        poseStack.translate(0.0, 2.0, 6.0);

        poseStack.mulPose(Axis.ZP.rotationDegrees((float) turn[2]));

        poseStack.translate(0.0, 0.0, recoil[2]);

        GunAnimationClientState.applyRelevantTransformation(poseStack, "Gun");
        renderPart(model, poseStack, bufferSource, GunModels.AM180_TEX, packedLight, packedOverlay, "Gun");
        if (silenced) renderPart(model, poseStack, bufferSource, GunModels.AM180_TEX, packedLight, packedOverlay, "Silencer");

        poseStack.pushPose();
        GunAnimationClientState.applyRelevantTransformation(poseStack, "Trigger");
        renderPart(model, poseStack, bufferSource, GunModels.AM180_TEX, packedLight, packedOverlay, "Trigger");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0, 0.0, bolt[2]);
        GunAnimationClientState.applyRelevantTransformation(poseStack, "Bolt");
        renderPart(model, poseStack, bufferSource, GunModels.AM180_TEX, packedLight, packedOverlay, "Bolt");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(magazine[0], magazine[1], magazine[2]);

        poseStack.translate(0.0, 2.0625, 3.75);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) magTurn[0]));
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) magTurn[2]));
        poseStack.translate(0.0, -2.0625, -3.75);

        poseStack.translate(0.0, 2.3125, 1.5);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) magSpin[0]));
        poseStack.translate(0.0, -2.3125, -1.5);

        GunAnimationClientState.applyRelevantTransformation(poseStack, "Mag");

        poseStack.pushPose();
        LocalPlayer player = Minecraft.getInstance().player;
        int magAmount = player == null ? 0
                : gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, player.getInventory());
        poseStack.translate(0.0, 0.0, 1.5);
        poseStack.mulPose(Axis.YN.rotationDegrees((float) (magAmount / 59D * 360D)));
        poseStack.translate(0.0, 0.0, -1.5);
        renderPart(model, poseStack, bufferSource, GunModels.AM180_TEX, packedLight, packedOverlay, "Mag");
        poseStack.popPose();

        renderPart(model, poseStack, bufferSource, GunModels.AM180_TEX, packedLight, packedOverlay, "MagPlate");
        poseStack.popPose();

        // Smoke-node trail not ported - see ItemRenderGunBase's own javadoc (CE's smoke block, which
        // sat here between the magazine and muzzle-flash blocks, is skipped entirely).

        poseStack.pushPose();
        poseStack.translate(0.0, 1.875, silenced ? 16.75 : 12.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(90F));
        poseStack.mulPose(Axis.XP.rotationDegrees(90F * (float) gun.shotRand));
        double flashScale = silenced ? 0.5 : 0.75;
        poseStack.scale((float) flashScale, (float) flashScale, (float) flashScale);
        renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[0], silenced ? 75 : 50, silenced ? 5 : 7.5);
        poseStack.popPose();
    }

    @Override
    protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
        super.setupThirdPersonGun(stack, poseStack);
        poseStack.translate(0.0, -0.5, 3.0);
    }

    @Override
    protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
        super.setupInventoryGun(stack, poseStack);
        float scale = 0.75F;
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.XP.rotationDegrees(25F));
        poseStack.mulPose(Axis.YP.rotationDegrees(45F));
        poseStack.translate(1.5, 0.0, 0.0);
    }

    @Override
    protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource,
                                int packedLight, int packedOverlay) {
        HbmObjModel model = GunModels.am180();
        if (model == null) return;

        renderPart(model, poseStack, bufferSource, GunModels.AM180_TEX, packedLight, packedOverlay, "Gun");
        if (hasSilencer(stack)) renderPart(model, poseStack, bufferSource, GunModels.AM180_TEX, packedLight, packedOverlay, "Silencer");
        renderPart(model, poseStack, bufferSource, GunModels.AM180_TEX, packedLight, packedOverlay, "Trigger");
        renderPart(model, poseStack, bufferSource, GunModels.AM180_TEX, packedLight, packedOverlay, "Bolt");
        renderPart(model, poseStack, bufferSource, GunModels.AM180_TEX, packedLight, packedOverlay, "Mag");
        renderPart(model, poseStack, bufferSource, GunModels.AM180_TEX, packedLight, packedOverlay, "MagPlate");
    }

    public boolean hasSilencer(ItemStack stack) {
        return XWeaponModManager.hasUpgrade(stack, 0, SILENCER_ID);
    }
}
