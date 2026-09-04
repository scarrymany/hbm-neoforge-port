package com.hbm.handler;

import com.hbm.main.MainRegistry;
import com.hbm.saveddata.satellites.SatelliteDetector;
import com.hbm.saveddata.satellites.SatelliteRayScan;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * CE {@code ModEventHandler.worldTick} :673 Detector every tick, :692 RayScan
 * when {@code getGameTime() % 20 == 10}.
 */
@EventBusSubscriber(modid = MainRegistry.MODID)
public final class SatelliteRayScanTickHandler {

    private SatelliteRayScanTickHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            SatelliteDetector.updateSystem(level);
            if (level.getGameTime() % 20 == 10) {
                SatelliteRayScan.updateSystem(level);
            }
        }
    }
}
