package com.hbm.blockentity.network;

import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal stub of CE's {@code com.hbm.tileentity.network.RTTYSystem} (199 lines in CE), per
 * {@code docs/phase3/scattered_military_items.md}'s explicit recommendation: this is real,
 * cross-cutting infrastructure with 19 CE call sites spanning RBMK console peripherals, the
 * satellite save-data system, and a radio-torch/telex family - none of which are scoped by any
 * Phase 2/3 package this wave. {@link com.hbm.items.tool.ItemRTTYPager} is the only consumer wired
 * against this stub so far; whoever ports the real channel-broadcast network (melody generator,
 * one-tick publish delay, {@code RTTYSpecialSignal} enum, RBMK/satellite consumers) should replace
 * this class's body without needing to change {@code ItemRTTYPager}'s two-method
 * {@link #listen}/{@link #broadcast} call sites.
 * <p>
 * Lives under {@code com.hbm.blockentity.network} rather than a literal
 * {@code com.hbm.tileentity.network} mirror of CE's package - this port's established convention
 * (see {@code docs/phase2/multiblock_framework.md}) renames every CE {@code com.hbm.tileentity.*}
 * package to {@code com.hbm.blockentity.*}, and {@code com.hbm.blockentity.network} already exists
 * as the home for this port's other network-adjacent block-entity infrastructure (fluid ducts, pipes).
 * <p>
 * Simplified from CE in two ways, both documented rather than silently different:
 * <ul>
 *     <li>No one-tick publish delay - CE queues into {@code newMessages} and only makes a signal
 *     visible to {@link #listen} on the following tick (via a server-tick-event flush). This stub
 *     publishes immediately, so a same-tick {@code broadcast} is visible to a same-tick
 *     {@code listen}. Harmless for the only current consumer ({@code ItemRTTYPager} polls once per
 *     item tick, not the same tick it may itself broadcast).</li>
 *     <li>No numeric-signal summing, no melody generator, no {@code RTTYSpecialSignal} enum - none
 *     of CE's 19 real consumers exist in this port yet, so there is nothing yet that depends on
 *     those behaviors.</li>
 * </ul>
 */
public final class RTTYSystem {

    private static final Map<ChannelKey, RTTYChannel> CHANNELS = new ConcurrentHashMap<>();

    private RTTYSystem() {
    }

    /** Pushes a new signal onto the given channel, visible to {@link #listen} immediately (see class javadoc). */
    public static void broadcast(Level level, String channelName, Object signal) {
        RTTYChannel channel = new RTTYChannel();
        channel.timeStamp = level.getGameTime();
        channel.signal = signal;
        CHANNELS.put(new ChannelKey(level, channelName), channel);
    }

    /** Returns the RTTY channel with that name on that level, or {@code null} if nothing has ever broadcast to it. */
    public static RTTYChannel listen(Level level, String channelName) {
        return CHANNELS.get(new ChannelKey(level, channelName));
    }

    public static final class RTTYChannel {
        /** The level's game time at the moment of publishing. */
        public long timeStamp = -1;
        /** A signal can be anything - a number, an encoded string, whatever the broadcaster sent. */
        public Object signal;
    }

    private record ChannelKey(Level level, String channel) {
        private ChannelKey {
            Objects.requireNonNull(level);
            Objects.requireNonNull(channel);
        }
    }
}
