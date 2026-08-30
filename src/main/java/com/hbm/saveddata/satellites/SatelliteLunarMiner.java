package com.hbm.saveddata.satellites;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * Ported from CE's {@code com.hbm.saveddata.satellites.SatelliteLunarMiner} (24 lines, read in
 * full) - near-trivial subclass of {@link SatelliteMiner} with its own pool identifier.
 */
public class SatelliteLunarMiner extends SatelliteMiner {

    @Override
    public String getType() {
        return "LUNAR_MINER";
    }

    @Override
    public Component[] getInfo(Level level) {
        return new Component[]{Component.translatable("satellite.lunar_miner.name")};
    }

    static {
        registerCargo(SatelliteLunarMiner.class, "sat_lunar");
    }
}
