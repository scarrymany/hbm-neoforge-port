package com.hbm.saveddata.satellites;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * Ported from CE's {@code com.hbm.saveddata.satellites.SatelliteScanner} (29 lines, read in full) -
 * a near-trivial {@code SAT_PANEL} satellite with no commands of its own, matching the research
 * report's own characterization ("near-trivial stub").
 */
public class SatelliteScanner extends Satellite {

    public SatelliteScanner() {
        this.ifaceAcs.add(InterfaceActions.HAS_ORES);
        this.satIface = Interfaces.SAT_PANEL;
    }

    @Override
    public String getType() {
        return "DEPTH_SCANNER";
    }

    @Override
    public Component[] getInfo(Level level) {
        return new Component[]{Component.translatable("satellite.scanner.name")};
    }

    @Override
    public float[] getColor() {
        return new float[]{0.544F, 0.680F, 1.0F};
    }
}
