package com.hbm.render.hud;

import com.hbm.interfaces.IHoldableWeapon;
import com.hbm.interfaces.IItemHUD;
import com.hbm.main.MainRegistry;
import com.hbm.render.misc.RenderScreenOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Generic held-item HUD dispatch loop - CE's {@code ModEventHandlerClient.onOverlayRender}'s
 * "HANDLE GUN AND AMMO OVERLAYS" block (main-hand checked first, offhand only if main-hand doesn't
 * qualify, lines 812-816) had no dedicated class of its own in CE (it lived inline in the giant
 * per-frame overlay dispatcher); this port gives it one so
 * {@link com.hbm.items.weapon.sedna.ItemGunBaseNT#renderHUD} (and any future {@link IItemHUD}
 * implementor) actually gets called by something.
 * <p>
 * Two independent, CE-real dispatch mechanisms live here side by side:
 * <ul>
 *     <li>{@link IItemHUD} - the modern Sedna-gun hook, called for every layer the held item's own
 *     {@code renderHUD} wants to see (it filters internally, see {@code ItemGunBaseNT.renderHUD}'s
 *     own javadoc).</li>
 *     <li>{@link IHoldableWeapon} - the legacy crosshair-only hook (CE: {@code ItemGunBase
 *     .renderHUD}'s own {@code IHoldableWeapon} branch, ported generically here rather than
 *     per-item since this port's only current implementor, {@code ItemLaserDetonator}, is a plain
 *     {@code Item}, not an {@code ItemGunBase} subclass with its own {@code renderHUD} override to
 *     put this logic in). Only reached on the {@code minecraft:crosshair} layer, and only for a
 *     held item that does <b>not</b> already implement {@link IItemHUD} (this port's two interfaces
 *     have no current implementor in common, so this is not a double-dispatch risk today, but the
 *     structure keeps it that way even if one arises later).</li>
 * </ul>
 */
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT)
public final class ItemHudDispatcher {

    private ItemHudDispatcher() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        ResourceLocation layer = event.getName();
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();

        if (main.getItem() instanceof IItemHUD hud) {
            hud.renderHUD(event, layer, player, main, InteractionHand.MAIN_HAND);
        } else if (off.getItem() instanceof IItemHUD hud) {
            hud.renderHUD(event, layer, player, off, InteractionHand.OFF_HAND);
        }

        if (!layer.equals(VanillaGuiLayers.CROSSHAIR)) return;

        IHoldableWeapon weapon = null;
        ItemStack weaponStack = ItemStack.EMPTY;
        if (main.getItem() instanceof IHoldableWeapon w) {
            weapon = w;
            weaponStack = main;
        } else if (off.getItem() instanceof IHoldableWeapon w) {
            weapon = w;
            weaponStack = off;
        }
        if (weapon == null) return;

        event.setCanceled(true);
        RenderScreenOverlay.renderCustomCrosshairs(event.getGuiGraphics(), weapon.getCrosshair());
        if (weapon.hasCustomHudElement()) {
            // No confirmed real per-frame partial-tick accessor on RenderGuiLayerEvent in this
            // sandbox (see this task's structured-output notes) - harmless today since no current
            // IHoldableWeapon implementor overrides renderHud's default no-op.
            weapon.renderHud(event.getGuiGraphics(), weaponStack, 1.0F);
        }
    }
}
