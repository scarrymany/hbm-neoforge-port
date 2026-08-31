package com.hbm.render.hud;

import com.hbm.items.gear.ArmorFSB;
import com.hbm.main.MainRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

/**
 * Dispatches {@link ArmorFSB#handleOverlay} against the player's equipped helmet each time a
 * vanilla GUI layer fires - the 1.21.1 replacement for CE's
 * {@code ModEventHandlerClient.onOverlayRender}'s two (redundant in CE) call sites
 * ({@code if (helmet.getItem() instanceof ArmorFSB) ((ArmorFSB) helmet).handleOverlay(event,
 * player)}, CE lines ~948-950 and ~962-964) - collapsed to one real call site here, since
 * {@link RenderGuiLayerEvent.Pre} already fires once per named layer, matching CE's own
 * once-per-{@code ElementType} semantics without CE's incidental duplicate call.
 * <p>
 * See {@link com.hbm.items.armor.ArmorHEV} for this port's only current real
 * {@code handleOverlay} override (the HEV suit's built-in armor/health HUD replacement) and
 * {@code docs/phase5/hud_overlays_geiger_armor_gun.md} Area B for the full research.
 */
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT)
public final class ArmorHazardHudOverlay {

    private ArmorHazardHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.getItem() instanceof ArmorFSB fsb) {
            fsb.handleOverlay(event, player);
        }
    }
}
