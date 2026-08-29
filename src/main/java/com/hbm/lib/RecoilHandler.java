package com.hbm.lib;

import com.hbm.main.MainRegistry;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * CE hooked {@code EntityViewRenderEvent.CameraSetup} (Forge 1.12). NeoForge's client-side camera
 * hook is {@link ViewportEvent.ComputeCameraAngles}, confirmed in the Neo Edition reference
 * ({@code NuclearTechModClient.onCameraRender}) with the same {@code getPitch()}/{@code setPitch()}
 * surface CE relied on.
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = MainRegistry.MODID)
public class RecoilHandler {

    private static long lastRenderTime;
    public static float verticalVelocity;
    public static float verticalRecoil;

    @SubscribeEvent
    public static void modifiyCamera(ViewportEvent.ComputeCameraAngles e) {
        long currentTime = System.currentTimeMillis();
        float scale = (currentTime - lastRenderTime) / 1000F;
        final float settle = 20F * Mth.clamp(verticalRecoil / 4, 0, 200);

        verticalRecoil = Math.max(0, verticalRecoil - scale * settle + verticalVelocity);
        verticalVelocity *= 0.35 * scale;
        e.setPitch((float) (e.getPitch() - verticalRecoil));
        lastRenderTime = currentTime;
    }
}
