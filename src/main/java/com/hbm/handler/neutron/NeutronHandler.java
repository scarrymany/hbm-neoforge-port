package com.hbm.handler.neutron;

import com.hbm.main.MainRegistry;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;

/**
 * Drives the neutron flux simulation once per server tick. CE:
 * {@code com.hbm.handler.neutron.NeutronHandler}, read in full - CE calls
 * {@code NeutronHandler.onServerTick()} from a plain {@code static} method reference inside
 * {@code ModEventHandler}'s own {@code ServerTickEvent} handler (alongside several unrelated
 * systems, run concurrently via a shared thread pool). This port self-subscribes instead
 * ({@link EventBusSubscriber}) on NeoForge's real {@code ServerTickEvent.Pre} (confirmed shape via
 * the Neo Edition reference, {@code NtmEventHandler#onServerTick}) so this package does not need to
 * modify a shared, multi-package event-handler aggregator file to wire itself up.
 * <p>
 * {@link #onServerTick()} (no-arg) is kept as a plain public static method, matching CE's own
 * shape, precisely so a future QA pass can drive one simulated world tick directly without an
 * event bus in play.
 */
@EventBusSubscriber(modid = MainRegistry.MODID)
public final class NeutronHandler {

    private NeutronHandler() {
    }

    private static int ticks = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        onServerTick();
    }

    public static void onServerTick() {
        // Freshen the node cache every `cacheTime` ticks to prevent huge RAM usage from idle nodes.
        int cacheTime = 20;
        boolean cacheClear = ticks >= cacheTime;
        if (cacheClear) ticks = 0;
        ticks++;

        // Remove StreamWorld objects if they have no streams.
        NeutronNodeWorld.removeEmptyWorlds();

        for (Map.Entry<ServerLevel, NeutronNodeWorld.StreamWorld> entry : NeutronNodeWorld.streamWorlds.entrySet()) {
            ServerLevel level = entry.getKey();
            NeutronNodeWorld.StreamWorld streamWorld = entry.getValue();

            // Refresh this world's dial snapshot once per tick - see RBMKNeutronHandler's class
            // javadoc for why this replaces CE's shared mutable static fields.
            streamWorld.setTickContext(RBMKNeutronHandler.TickContext.forLevel(level));

            streamWorld.runStreamInteractions(level);
            streamWorld.removeAllStreams();

            if (cacheClear) streamWorld.cleanNodes();
        }
    }
}
