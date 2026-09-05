package com.hbm.client.render.item.weapon;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.main.MainRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * CE {@code ModEventHandlerRenderer.onRenderSpecificHand} + {@code ItemRenderWeaponBase.getFOVModifier}.
 * Cancels vanilla first-person hand/item and draws the Sedna viewmodel.
 */
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT)
public final class GunClientEvents {

    private GunClientEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemGunBaseNT)) return;
        ItemRenderGunBase renderer = GunAnimationRegistration.RENDERERS.get(stack.getItem());
        if (renderer == null) return;

        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        renderer.setPerspectiveAndRender(
                stack,
                event.getPoseStack(),
                event.getMultiBufferSource(),
                event.getPackedLight(),
                event.getPartialTick());
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemGunBaseNT)) return;
        Item item = stack.getItem();
        ItemRenderGunBase renderer = GunAnimationRegistration.RENDERERS.get(item);
        if (renderer == null) return;
        event.setFOV(renderer.getViewFOV(stack, (float) event.getFOV()));
    }
}
