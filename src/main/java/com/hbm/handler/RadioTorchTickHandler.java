package com.hbm.handler;

import com.hbm.blockentity.network.RTTYSystem;
import com.hbm.main.MainRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** CE {@code ModEventHandler.serverTickFirst} — {@code RTTYSystem.updateBroadcastQueue} on tick START. */
@EventBusSubscriber(modid = MainRegistry.MODID)
public final class RadioTorchTickHandler {

    private RadioTorchTickHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        RTTYSystem.updateBroadcastQueue();
    }
}
