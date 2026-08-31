package com.hbm.items.armor;

import com.hbm.capability.HbmLivingAttachment;
import com.hbm.items.gear.ArmorFSB;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Ported from CE's {@code com.hbm.items.armor.ArmorHEV} (218 lines) - the HEV power-armor set.
 * This class's own armor-model/3D-renderer half (CE: {@code getArmorModel}/{@code getRenderer},
 * {@code IItemRendererProvider}) is a separate, out-of-scope concern belonging to whichever Phase 5
 * package owns armor {@code HumanoidModel}/renderer registration (see
 * {@code docs/phase5/armor_humanoidmodel_rendering.md}); this class carries only the 2D HUD-overlay
 * half CE's own {@code handleOverlay}/{@code renderOverlay} implement, per
 * {@code docs/phase5/hud_overlays_geiger_armor_gun.md} Area B (read in full before editing) -
 * confirmed the <b>only</b> real override of {@link ArmorFSB#handleOverlay} anywhere in CE.
 * <p>
 * While a full matching HEV set is worn ({@link ArmorFSB#hasFSBArmorIgnoreCharge}), this class
 * cancels vanilla's armor-icon row entirely and replaces vanilla's health/food row with a from-
 * scratch ASCII-art readout: current health x5 as a raw number, average armor-piece charge
 * percentage, and a 10-segment radiation gauge - all built from the same accumulated total-body dose
 * pool ({@link HbmLivingAttachment#getRads()}) area A's Geiger-counter bar reads, plus the same
 * 1-second-sampled RAD/s delta text. {@code ArmorFSB.customGeiger}/{@code setHasCustomGeiger(true)}
 * (set on the HEV helmet at item-registration time, out of this HUD-rendering package's own scope)
 * is what makes this override meaningful: it is not a second, different radiation readout, it is
 * "this specific suit's own screen takes over the normal HUD/Geiger bar" - see
 * {@code com.hbm.render.hud.GeigerHudOverlay}'s own suppression check for the other half of that
 * relationship.
 */
public class ArmorHEV extends ArmorFSBPowered {

    public ArmorHEV(Holder<ArmorMaterial> material, Type type, Item.Properties properties,
                     long maxPower, long chargeRate, long consumption, long drain) {
        super(material, type, properties, maxPower, chargeRate, consumption, drain);
    }

    private static long lastSurvey;
    private static double prevResult;
    private static double lastResult;

    /**
     * <b>Unverified in this sandbox</b> ({@code docs/phase5/hud_overlays_geiger_armor_gun.md} Key
     * risk 5 / Open question 2): {@link VanillaGuiLayers#ARMOR_LEVEL}/{@link VanillaGuiLayers
     * #PLAYER_HEALTH} are not demonstrated by any real, compiling source this port could read (only
     * {@code HOTBAR}/{@code CROSSHAIR} are directly confirmed, via Neo Edition's own
     * {@code HUDComponentDurabilityBar}/{@code GunBaseNTItem}). Their names/existence are otherwise
     * well-established NeoForge 1.21.x modding knowledge (NeoForge's {@code VanillaGuiLayers}
     * exposes a {@link net.minecraft.resources.ResourceLocation} constant per vanilla
     * {@code LayeredDraw.Layer}, matching {@code minecraft:player_health}/{@code minecraft:armor_level}
     * 1:1 with vanilla's own internal layer ids) - used here on that basis, but flagged explicitly
     * per this task's ground rules since no cached NeoForge sources jar was available to confirm
     * them directly. If either constant name turns out not to exist under this exact spelling, this
     * class fails to compile at this reference (a build-time signal, not a silent runtime failure) -
     * see this task's structured-output notes for the same caveat.
     */
    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleOverlay(RenderGuiLayerEvent.Pre event, Player player) {
        if (!ArmorFSB.hasFSBArmorIgnoreCharge(player)) return;

        if (event.getName().equals(VanillaGuiLayers.ARMOR_LEVEL)) {
            event.setCanceled(true);
            return;
        }
        if (event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)) {
            event.setCanceled(true);
            renderOverlay(event, player);
        }
    }

    /**
     * CE: {@code ArmorHEV#renderOverlay} (77-180, full). Pixel positions/thresholds/colors are
     * copied verbatim from CE's real numbers. 1.21.1 API swap: CE's {@code GlStateManager.scale(2,
     * 2, 2)} around the health/armor/radiation block (everything except the final RAD/s delta line,
     * which CE explicitly un-scales back to {@code 1D} first) becomes a
     * {@code guiGraphics.pose().pushPose()/scale(2F,2F,2F)/popPose()} bracket around just that block.
     */
    @OnlyIn(Dist.CLIENT)
    private void renderOverlay(RenderGuiLayerEvent.Pre event, Player player) {
        double in = HbmLivingAttachment.getData(player).getRads();

        double radiation = lastResult - prevResult;
        if (System.currentTimeMillis() >= lastSurvey + 1000) {
            lastSurvey = System.currentTimeMillis();
            prevResult = lastResult;
            lastResult = in;
        }

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        float scale = 2F;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, scale);

        int hX = (int) (8 / scale);
        int hY = (int) ((screenHeight - 18 - 2) / scale);
        int healthColor = player.getHealth() * 5 > 15 ? 0xFF8000 : 0xFF0000;
        guiGraphics.drawString(font, "+" + (int) (player.getHealth() * 5), hX, hY, healthColor);

        double totalCharge = 0D;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) continue;
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.getItem() instanceof ArmorFSBPowered powered) {
                totalCharge += (double) powered.getCharge(armor) / (double) powered.getMaxCharge(armor);
            }
        }

        int aX = (int) (70 / scale);
        int aY = (int) ((screenHeight - 18 - 2) / scale);
        int armorColor = totalCharge * 25 > 15 ? 0xFF8000 : 0xFF0000;
        guiGraphics.drawString(font, "||" + (int) (totalCharge * 25), aX, aY, armorColor);

        StringBuilder rad = new StringBuilder("☢ [");
        for (int i = 0; i < 10; i++) {
            if (in / 100 > i) {
                int mid = (int) (in - i * 100);
                if (mid < 33) rad.append("..");
                else if (mid < 67) rad.append("|.");
                else rad.append("||");
            } else {
                rad.append(" ");
            }
        }
        rad.append("]");

        int rX = (int) (8 / scale);
        int rY = (int) ((screenHeight - 40) / scale);
        int radColor = in < 800 ? 0xFF8000 : 0xFF0000;
        guiGraphics.drawString(font, rad.toString(), rX, rY, radColor);

        guiGraphics.pose().popPose();

        if (radiation > 0) {
            int dX = 32;
            int dY = screenHeight - 55;

            String delta = String.valueOf(Math.round(radiation));
            if (radiation > 1000) delta = ">1000";
            else if (radiation < 1) delta = "<1";

            guiGraphics.drawString(font, delta + " RAD/s", dX, dY, 0xFF0000);
        }
    }
}
