package com.hbm.blockentity.network;

import com.hbm.util.NoteBuilder;
import com.hbm.util.NoteBuilder.Instrument;
import com.hbm.util.NoteBuilder.Note;
import com.hbm.util.NoteBuilder.Octave;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CE {@code com.hbm.tileentity.network.RTTYSystem}: one-tick delayed broadcast map,
 * numeric summing, {@link RTTYSpecialSignal}. Channel {@code "2012-08-06"} broadcasts
 * CE Song of Storms via {@link NoteBuilder}.
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
                Object signal = TEST_SENDER_MELODY[(int) (time % TEST_SENDER_MELODY.length)];
                RTTYChannel chan = new RTTYChannel();
                chan.timeStamp = time;
                chan.signal = signal;
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

    /** CE Song of Storms at 300 BPM — unset slots stay {@code ""} so idle ticks still broadcast. */
    private static final Object[] TEST_SENDER_MELODY;

    static {
        int tempo = 4;
        TEST_SENDER_MELODY = new Object[tempo * 160];
        Arrays.fill(TEST_SENDER_MELODY, "");

        Instrument flute = Instrument.PIANO;
        Instrument accordion = Instrument.BASSGUITAR;

        TEST_SENDER_MELODY[tempo * 0] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 2] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 4] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).end();

        TEST_SENDER_MELODY[tempo * 6] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 8] = NoteBuilder.start().add(accordion, Note.E, Octave.LOW).add(accordion, Note.G, Octave.LOW).add(accordion, Note.B, Octave.LOW).end();

        TEST_SENDER_MELODY[tempo * 12] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 14] = NoteBuilder.start().add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).add(accordion, Note.C, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 16] = NoteBuilder.start().add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).add(accordion, Note.C, Octave.MID).end();

        TEST_SENDER_MELODY[tempo * 18] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 20] = NoteBuilder.start().add(accordion, Note.E, Octave.LOW).add(accordion, Note.G, Octave.LOW).add(accordion, Note.B, Octave.LOW).end();

        TEST_SENDER_MELODY[tempo * 24] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 26] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 28] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).end();

        TEST_SENDER_MELODY[tempo * 30] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 32] = NoteBuilder.start().add(accordion, Note.E, Octave.LOW).add(accordion, Note.G, Octave.LOW).add(accordion, Note.B, Octave.LOW).end();

        TEST_SENDER_MELODY[tempo * 36] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 38] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 40] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).end();

        TEST_SENDER_MELODY[tempo * 42] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 44] = NoteBuilder.start().add(accordion, Note.E, Octave.LOW).add(accordion, Note.G, Octave.LOW).add(accordion, Note.B, Octave.LOW).end();

        TEST_SENDER_MELODY[tempo * 48] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(flute, Note.D, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 50] = NoteBuilder.start().add(flute, Note.F, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 52] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).add(flute, Note.D, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 54] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW);

        TEST_SENDER_MELODY[tempo * 56] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(flute, Note.D, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 58] = NoteBuilder.start().add(flute, Note.F, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 60] = NoteBuilder.start().add(accordion, Note.E, Octave.LOW).add(accordion, Note.G, Octave.LOW).add(accordion, Note.B, Octave.LOW).add(flute, Note.D, Octave.MID).end();

        TEST_SENDER_MELODY[tempo * 64] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(flute, Note.E, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 66] = NoteBuilder.start().add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).add(accordion, Note.C, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 67] = NoteBuilder.start().add(flute, Note.F, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 68] = NoteBuilder.start().add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).add(accordion, Note.C, Octave.MID).add(flute, Note.E, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 69] = NoteBuilder.start().add(flute, Note.F, Octave.MID).end();

        TEST_SENDER_MELODY[tempo * 70] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(flute, Note.E, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 71] = NoteBuilder.start().add(flute, Note.B, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 72] = NoteBuilder.start().add(accordion, Note.E, Octave.LOW).add(accordion, Note.G, Octave.LOW).add(accordion, Note.B, Octave.LOW).add(flute, Note.A, Octave.MID).end();

        TEST_SENDER_MELODY[tempo * 76] = NoteBuilder.start().add(accordion, Note.G, Octave.LOW).add(flute, Note.A, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 78] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.B, Octave.MID).add(flute, Note.D, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 80] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.B, Octave.MID).add(flute, Note.F, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 81] = NoteBuilder.start().add(flute, Note.G, Octave.MID).end();

        TEST_SENDER_MELODY[tempo * 82] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(flute, Note.A, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 84] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).end();

        TEST_SENDER_MELODY[tempo * 88] = NoteBuilder.start().add(accordion, Note.G, Octave.LOW).add(flute, Note.A, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 90] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.B, Octave.MID).add(flute, Note.D, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 92] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.B, Octave.MID).add(flute, Note.F, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 93] = NoteBuilder.start().add(accordion, Note.B, Octave.MID).add(flute, Note.G, Octave.MID).end();

        TEST_SENDER_MELODY[tempo * 94] = NoteBuilder.start().add(accordion, Note.F, Octave.LOW).add(flute, Note.E, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 96] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).end();

        TEST_SENDER_MELODY[tempo * 100] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(flute, Note.D, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 101] = NoteBuilder.start().add(flute, Note.F, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 102] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.B, Octave.MID).add(flute, Note.D, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 104] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.B, Octave.MID).end();

        TEST_SENDER_MELODY[tempo * 106] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(flute, Note.D, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 107] = NoteBuilder.start().add(flute, Note.F, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 108] = NoteBuilder.start().add(accordion, Note.E, Octave.LOW).add(accordion, Note.G, Octave.LOW).add(accordion, Note.B, Octave.MID).add(flute, Note.D, Octave.MID).end();

        TEST_SENDER_MELODY[tempo * 112] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(flute, Note.E, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 114] = NoteBuilder.start().add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).add(accordion, Note.C, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 115] = NoteBuilder.start().add(flute, Note.F, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 116] = NoteBuilder.start().add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).add(accordion, Note.C, Octave.MID).add(flute, Note.E, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 117] = NoteBuilder.start().add(flute, Note.F, Octave.MID).end();

        TEST_SENDER_MELODY[tempo * 118] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(flute, Note.E, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 119] = NoteBuilder.start().add(flute, Note.C, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 120] = NoteBuilder.start().add(accordion, Note.E, Octave.LOW).add(accordion, Note.G, Octave.LOW).add(accordion, Note.B, Octave.MID).add(flute, Note.A, Octave.MID).end();

        TEST_SENDER_MELODY[tempo * 124] = NoteBuilder.start().add(accordion, Note.G, Octave.LOW).add(flute, Note.A, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 126] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.MID).add(flute, Note.D, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 128] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.MID).add(flute, Note.F, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 129] = NoteBuilder.start().add(flute, Note.G, Octave.MID).end();

        TEST_SENDER_MELODY[tempo * 130] = NoteBuilder.start().add(accordion, Note.F, Octave.LOW).add(flute, Note.A, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 132] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(accordion, Note.E, Octave.LOW).add(accordion, Note.A, Octave.MID).add(accordion, Note.G, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 134] = NoteBuilder.start().add(flute, Note.A, Octave.MID).end();

        TEST_SENDER_MELODY[tempo * 136] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(flute, Note.D, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 138] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 140] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.MID).end();
    }
}
