package com.hbm.client.render.item.weapon;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.main.MainRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * CE {@code ItemRenderWeaponBase.getFOVModifier} caller.
 * TODO(CE:ItemRenderWeaponBase.java:116) setPerspectiveAndRender custom projection is 1.12-only;
 * this is the 1.21 equivalent for ADS zoom only.
 */
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT)
public final class GunClientEvents {

    private GunClientEvents() {
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
