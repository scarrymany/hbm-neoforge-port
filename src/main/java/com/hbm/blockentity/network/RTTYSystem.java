package com.hbm.blockentity.network;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CE {@code com.hbm.tileentity.network.RTTYSystem}: one-tick delayed broadcast map,
 * numeric summing, {@link RTTYSpecialSignal}. Melody on {@code "2012-08-06"} skipped —
 * CE {@code NoteBuilder} is not in this port (pager still hears empty ticks on that channel).
 */
public final class RTTYSystem {

    public static final Map<ChannelKey, RTTYChannel> broadcast = new ConcurrentHashMap<>();
    public static final Map<ChannelKey, Object> newMessages = new ConcurrentHashMap<>();

    private RTTYSystem() {
    }

    /**
     * Pushes a new signal to be used next tick. Only the last signal pushed will be used, unless
     * both the existing and new signal parse as numbers, in which case they are summed.
     */
    public static void broadcast(Level level, String channelName, Object signal) {
        ChannelKey identifier = new ChannelKey(level, channelName);

        if (NumberUtils.isCreatable("" + signal) && newMessages.containsKey(identifier)) {
            Object existing = newMessages.get(identifier);
            if (NumberUtils.isCreatable("" + existing)) {
                try {
                    long first = Long.parseLong("" + signal);
                    long second = Long.parseLong("" + existing);
                    newMessages.put(identifier, "" + (first + second));
                    return;
                } catch (Exception ignored) {
                }
            }
        }

        newMessages.put(identifier, signal);
    }

    /** Returns the RTTY channel with that name, or {@code null}. */
    public static RTTYChannel listen(Level level, String channelName) {
        return broadcast.get(new ChannelKey(level, channelName));
    }

    /**
     * Moves all new messages to the broadcast map, adding the appropriate timestamp and
     * clearing the new message queue. Call from {@code ServerTickEvent.Pre}.
     */
    public static void updateBroadcastQueue() {
        for (Entry<ChannelKey, Object> worldEntry : newMessages.entrySet()) {
            ChannelKey identifier = worldEntry.getKey();
            Object lastSignal = worldEntry.getValue();

            RTTYChannel channel = new RTTYChannel();
            channel.timeStamp = identifier.level().getGameTime();
            channel.signal = lastSignal;
            broadcast.put(identifier, channel);
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerLevel world : server.getAllLevels()) {
                long time = world.getGameTime();
                RTTYChannel chan = new RTTYChannel();
                chan.timeStamp = time;
                chan.signal = "";
                broadcast.put(new ChannelKey(world, "2012-08-06"), chan);
            }
        }

        newMessages.clear();
    }

    public static final class RTTYChannel {
        public long timeStamp = -1;
        public Object signal;
    }

    public enum RTTYSpecialSignal {
        BEGIN_TTY,
        STOP_TTY,
        PRINT_BUFFER
    }

    public record ChannelKey(Level level, String channel) {
        public ChannelKey {
            Objects.requireNonNull(level);
            Objects.requireNonNull(channel);
        }
    }
}
