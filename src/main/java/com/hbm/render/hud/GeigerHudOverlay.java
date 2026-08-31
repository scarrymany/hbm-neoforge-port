package com.hbm.render.hud;

import com.hbm.capability.HbmLivingAttachment;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.items.tool.ToolItems;
import com.hbm.main.MainRegistry;
import com.hbm.render.misc.RenderScreenOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Ported from CE's {@code ModEventHandlerClient.onOverlayRender}'s "HANDLE GEIGER COUNTER AND
 * JETPACK HUD" block (818-836) - just the two radiation gauges (jetpack HUD is a separate, larger,
 * out-of-scope system, see {@code docs/phase5/hud_overlays_geiger_armor_gun.md} Headline finding
 * 10). See that report's Area A, read in full before editing.
 * <p>
 * <b>Dispatch strategy</b>: {@link RenderGuiEvent.Post} (whole-frame, fires once regardless of
 * which vanilla layer is being drawn) rather than a per-layer {@code RenderGuiLayerEvent}, per the
 * report's own recommendation (finding 3) - both gauges are drawn at hardcoded absolute HUD-corner
 * coordinates in CE, never relative to another vanilla layer, so there is no specific layer to hook.
 * This is the same strategy Neo Edition's own confirmed-real port of this exact feature uses
 * ({@code NuclearTechModClient.onRenderGuiPost}, unconditional call, cross-checked for API shape
 * only). Because a whole-frame event still fires while the GUI is hidden (F1) or the camera is in
 * third person - unlike CE's original per-{@code ElementType} dispatch, which vanilla/Forge itself
 * skips in those cases - {@link RenderScreenOverlay#renderRadCounter}/{@link RenderScreenOverlay
 * #renderDigCounter} each carry their own explicit hideGui/first-person/spectator guards (see that
 * class's own javadoc) to reproduce CE's real on-screen behavior under this port's chosen event.
 */
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT)
public final class GeigerHudOverlay {

    private GeigerHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();

        // CE: !(ArmorFSB.hasFSBArmorHelmet(player) && ((ArmorFSB) helmet).customGeiger) - the HEV
        // suit's own built-in radiation readout (ArmorHazardHudOverlay/ArmorHEV) replaces this gauge
        // entirely while worn, so it is suppressed here rather than drawn underneath/behind it.
        boolean suppressedByCustomGeigerArmor = false;
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.getItem() instanceof ArmorFSB fsb && fsb.customGeiger && ArmorFSB.hasFSBArmorHelmet(player)) {
            suppressedByCustomGeigerArmor = true;
        }

        if (!suppressedByCustomGeigerArmor && carriesItem(player, ToolItems.GEIGER_COUNTER.get())) {
            double rads = HbmLivingAttachment.getData(player).getRads();
            RenderScreenOverlay.renderRadCounter(guiGraphics, (float) rads);
        }

        // Ungated by the customGeiger check - CE's digamma gauge is never suppressed by any armor.
        if (carriesItem(player, ToolItems.DIGAMMA_DIAGNOSTIC.get())) {
            double digamma = HbmLivingAttachment.getData(player).getDigamma();
            RenderScreenOverlay.renderDigCounter(guiGraphics, (float) digamma);
        }
    }

    /**
     * CE: {@code Library.hasInventoryItem(player.inventory, item)} (main inventory + armor +
     * offhand - CE's Baubles-slot alternative, {@code hasBauble}, is a dead branch for this port per
     * the already-made Phase 1 no-Curios/Baubles decision, see the research report's Sources
     * section). No standalone {@code InventoryUtil}/{@code Library} helper for this exists in this
     * port yet - reimplemented locally as the one check this call site needs, matching
     * {@code ArmorFSB#carriesGeigerOrDosimeter}'s own already-committed local reimplementation of
     * the identical CE pattern.
     */
    private static boolean carriesItem(LocalPlayer player, Item item) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == item) return true;
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.getItem() == item) return true;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() == item) return true;
        }
        return false;
    }
}
