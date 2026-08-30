package com.hbm.saveddata.satellites;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Ported from CE's {@code com.hbm.saveddata.satellites.SatelliteRayScan} (155 lines, read in full)
 * - a {@code SAT_PANEL} satellite surveying a static, globally-shared event log ({@link #rayEvent}),
 * fully self-contained. {@code DimPos.dim} is a {@link ResourceKey} rather than CE's raw int
 * dimension id, matching this port's standard idiom (see {@link SatelliteDetector}'s identical
 * adaptation).
 */
public class SatelliteRayScan extends Satellite {

    public final List<RayEvent> cachedResults = new ArrayList<>();

    public static final int MAX_SCAN_RANGE = 250;

    public static final String CMD_SURVEY = "survey";
    public static final String CMD_COUNT = "count";
    public static final String CMD_GETINFO = "getinfo";
    public static final String CMD_GETPOSITION = "getposition";

    public SatelliteRayScan() {
        this.ifaceAcs.add(InterfaceActions.HAS_MAP);
        this.satIface = Interfaces.SAT_PANEL;
    }

    @Override
    public String getType() {
        return "NB_RAY_SCANNER";
    }

    @Override
    public Component[] getInfo(Level level) {
        return new Component[]{Component.translatable("satellite.rayscan.name")};
    }

    @Override
    public void onCommandImpl(Level level, String... cmd) {
        if (cmd.length <= 0) return;

        if (cmd[0].equals(CMD_SURVEY)) {
            this.cachedResults.clear();

            for (Map.Entry<DimPos, RayEvent> entry : rayEvent.entrySet()) {
                DimPos pos = entry.getKey();
                if (!pos.dim.equals(level.dimension())) continue;
                int dX = pos.x - this.targetX;
                int dZ = pos.z - this.targetZ;

                if (dX * dX + dZ * dZ <= MAX_SCAN_RANGE * MAX_SCAN_RANGE) {
                    this.cachedResults.add(entry.getValue());
                }
            }
            return;
        }

        if (cmd[0].equals(CMD_COUNT)) {
            this.tx = "" + cachedResults.size();
            return;
        }

        if (cmd[0].equals(CMD_GETINFO) && cmd.length == 2) {
            RayEvent event = getEventFromIndex(cmd[1]);
            this.tx = event == null ? "" : "" + event.info;
            return;
        }

        if (cmd[0].equals(CMD_GETPOSITION) && cmd.length == 2) {
            RayEvent event = getEventFromIndex(cmd[1]);
            this.tx = event == null ? "" : event.x + ";" + event.z;
        }
    }

    public RayEvent getEventFromIndex(String cmd) {
        if (cachedResults.isEmpty()) return null;
        int index = IRORInteractive.parseInt(cmd, 1, cachedResults.size()) - 1;
        if (index < 0 || index >= cachedResults.size()) return null;
        return cachedResults.get(index);
    }

    @Override
    public float[] getColor() {
        return new float[]{0.4F, 1.0F, 0.8F};
    }

    public static final LinkedHashMap<DimPos, RayEvent> rayEvent = new LinkedHashMap<>();

    public static void reportEvent(Level level, int x, int y, int z, String info, int lifetime) {
        if (level == null || level.isClientSide()) return;
        rayEvent.put(new DimPos(x, y, z, level.dimension()), new RayEvent(level, lifetime, x, z, info));
    }

    public static void updateSystem(Level level) {
        rayEvent.entrySet().removeIf(entry -> level.dimension().equals(entry.getKey().dim) && level.getGameTime() > entry.getValue().expiresOn);
    }

    public static class DimPos {

        public final int x;
        public final int y;
        public final int z;
        public final ResourceKey<Level> dim;

        public DimPos(int x, int y, int z, ResourceKey<Level> dim) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.dim = dim;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DimPos other)) return false;
            return x == other.x && y == other.y && z == other.z && dim.equals(other.dim);
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z, dim);
        }
    }

    public static class RayEvent {

        public static final String INFO_ARC_FLASH = "ARC_FLASH";
        public static final String INFO_NUCLEAR = "NEUTRON_EMISSION";
        public static final String INFO_PARTICLE = "HIGH_ENERGY_PARTICLES";
        public static final String INFO_RADAR = "RADAR_WAVES";
        public static final String INFO_RADIO = "RADIO_WAVES";

        public final long expiresOn;
        public final String info;
        public final int x;
        public final int z;

        public RayEvent(Level level, int lifetime, int x, int z, String info) {
            this.expiresOn = level.getGameTime() + lifetime;
            this.x = x;
            this.z = z;
            this.info = info;
        }
    }
}
