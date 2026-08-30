package com.hbm.saveddata.satellites;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.saveddata.satellites.SatelliteDetector} (read in full) - a
 * {@code Interfaces.NONE} radiation-burst survey satellite, fully self-contained (no missing
 * dependency beyond adapting CE's raw int dimension id to {@link ResourceKey}, per this port's
 * standard dimension-identity idiom elsewhere in this package).
 */
public class SatelliteDetector extends Satellite {

    public final List<RadiationBurst> cachedResults = new ArrayList<>();

    public static final String CMD_SURVEY = "survey";
    public static final String CMD_COUNT = "count";
    public static final String CMD_GETTYPE = "gettype";
    public static final String CMD_GETPOSITION = "getposition";

    @Override
    public String getType() {
        return "UWB_EMISSION_DETECTOR";
    }

    @Override
    public Component[] getInfo(Level level) {
        return new Component[]{Component.translatable("satellite.detector.name")};
    }

    @Override
    public void onCommandImpl(Level level, String... cmd) {
        if (cmd.length <= 0) return;

        if (cmd[0].equals(CMD_SURVEY)) {
            cachedResults.clear();

            for (RadiationBurst burst : bursts) {
                if (level.dimension().equals(burst.dimension)) cachedResults.add(burst);
            }
            return;
        }

        if (cmd[0].equals(CMD_COUNT)) {
            this.tx = "" + cachedResults.size();
            return;
        }

        if (cmd[0].equals(CMD_GETTYPE) && cmd.length == 2) {
            RadiationBurst burst = getBurstFromIndex(cmd[1]);
            this.tx = burst == null ? "" : "" + burst.intensity.name();
            return;
        }

        if (cmd[0].equals(CMD_GETPOSITION) && cmd.length == 2) {
            RadiationBurst burst = getBurstFromIndex(cmd[1]);
            this.tx = burst == null ? "" : burst.x + ";" + burst.z;
        }
    }

    public RadiationBurst getBurstFromIndex(String cmd) {
        if (cachedResults.isEmpty()) return null;
        int index = IRORInteractive.parseInt(cmd, 1, cachedResults.size()) - 1;
        if (index < 0 || index >= cachedResults.size()) return null;
        return cachedResults.get(index);
    }

    @Override
    public float[] getColor() {
        return new float[]{0.8F, 0.4F, 1.0F};
    }

    public static final List<RadiationBurst> bursts = new ArrayList<>();

    public static final int DURATION_LOW = 15 * 20;
    public static final int DURATION_MEDIUM = 20 / 2;
    public static final int DURATION_HIGH = 60 * 20;

    public static final double INACCURACY_LOW = 10_000;
    public static final double INACCURACY_MEDIUM = 2_500;
    public static final double INACCURACY_HIGH = 500;

    public static void reportEvent(Level level, int lifetime, BurstIntensity intensity, double x, double z) {
        if (level == null || level.isClientSide()) return;
        bursts.add(new RadiationBurst(level, lifetime, intensity, (int) Math.floor(x), (int) Math.floor(z)));
    }

    public static void updateSystem(Level level) {
        bursts.removeIf(b -> level.dimension().equals(b.dimension) && level.getGameTime() > b.expiresOn);
    }

    public static class RadiationBurst {

        public final ResourceKey<Level> dimension;
        public final long expiresOn;
        public final BurstIntensity intensity;
        public int x;
        public int z;

        public RadiationBurst(Level level, int lifetime, BurstIntensity intensity, int x, int z) {
            this.dimension = level.dimension();
            this.expiresOn = level.getGameTime() + lifetime;
            this.intensity = intensity;
            this.x = x;
            this.z = z;

            double inaccuracy = switch (intensity) {
                case LOW -> INACCURACY_LOW;
                case MEDIUM -> INACCURACY_MEDIUM;
                case HIGH -> INACCURACY_HIGH;
            };

            this.x = (int) (this.x + Mth.clamp(level.getRandom().nextGaussian(), -1, 1) * inaccuracy);
            this.z = (int) (this.z + Mth.clamp(level.getRandom().nextGaussian(), -1, 1) * inaccuracy);
        }
    }

    public enum BurstIntensity {
        LOW,
        MEDIUM,
        HIGH
    }
}
