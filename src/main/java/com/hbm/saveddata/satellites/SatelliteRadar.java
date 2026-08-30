package com.hbm.saveddata.satellites;

import com.hbm.api.entity.IRadarDetectableNT;
import com.hbm.api.entity.IRadarDetectableNT.RadarScanParams;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ported from CE's {@code com.hbm.saveddata.satellites.SatelliteRadar} (read in full) - a
 * {@code SAT_PANEL} satellite that surveys nearby radar-detectable entities.
 * <p>
 * <b>Simplification, documented</b>: CE's {@code CMD_SURVEY} reads off
 * {@code TileEntityMachineRadarNT.matchingEntities}, a global cache populated by the (unported, per
 * {@code docs/phase3/missile_launch_infra.md}'s Deferred scope) radar-console tile entity. Since
 * that TE doesn't exist in this port yet, this class instead scans {@link ServerLevel#getEntities()}
 * directly for {@link IRadarDetectableNT} implementors within range - functionally equivalent (the
 * global cache CE reads from is itself just "every currently-loaded radar-detectable entity"), and
 * does not depend on the missing console TE ever existing to compile or to work correctly.
 */
public class SatelliteRadar extends Satellite {

    public static final int MAX_SCAN_RANGE = 1_000;
    public static final RadarScanParams SCAN_PARAMS = new RadarScanParams(true, true, true, false);

    public static final String CMD_SURVEY = "survey";
    public static final String CMD_FILTER = "filter";
    public static final String CMD_COUNT = "count";
    public static final String CMD_GETTARGETID = "gettargetid";
    public static final String CMD_GETPOSITION = "getposition";
    public static final String CMD_GETNAME = "getname";

    public final List<Entity> cachedRadarResults = new ArrayList<>();
    public List<Entity> filteredRadarResults = new ArrayList<>();

    public SatelliteRadar() {
        this.ifaceAcs.add(InterfaceActions.HAS_MAP);
        this.ifaceAcs.add(InterfaceActions.HAS_RADAR);
        this.satIface = Interfaces.SAT_PANEL;
    }

    @Override
    public String getType() {
        return "LEO_RADAR";
    }

    @Override
    public Component[] getInfo(Level level) {
        return new Component[]{Component.translatable("satellite.radar.name")};
    }

    @Override
    public void onCommandImpl(Level level, String... cmd) {
        if (cmd.length <= 0 || !(level instanceof ServerLevel serverLevel)) return;

        if (cmd[0].equals(CMD_SURVEY)) {
            cachedRadarResults.clear();

            // Bounded to a box around the target rather than scanning every loaded entity in the
            // level - Level#getEntitiesOfClass(Class, AABB) is the confirmed, long-standing vanilla
            // API for this (unlike the newer LevelEntityGetter#getAll(), not independently
            // re-verified in this pass).
            net.minecraft.world.phys.AABB searchBox = new net.minecraft.world.phys.AABB(
                    targetX - MAX_SCAN_RANGE, serverLevel.getMinY(), targetZ - MAX_SCAN_RANGE,
                    targetX + MAX_SCAN_RANGE, serverLevel.getMaxY() + 1, targetZ + MAX_SCAN_RANGE);

            for (Entity entity : serverLevel.getEntitiesOfClass(Entity.class, searchBox)) {
                if (!(entity instanceof IRadarDetectableNT radar)) continue;
                if (!radar.paramsApplicable(SCAN_PARAMS) || !radar.canBeSeenBy(this)) continue;

                int x = (int) Math.floor(entity.getX());
                int z = (int) Math.floor(entity.getZ());

                double dX = x - targetX;
                double dZ = z - targetZ;

                if (dX * dX + dZ * dZ <= (double) MAX_SCAN_RANGE * MAX_SCAN_RANGE) {
                    cachedRadarResults.add(entity);
                }
            }

            filteredRadarResults = new ArrayList<>(cachedRadarResults);
            return;
        }

        if (cmd[0].equals(CMD_FILTER) && cmd.length == 2) {
            filteredRadarResults.clear();
            String filter = cmd[1].toLowerCase(Locale.US);

            for (Entity entity : cachedRadarResults) {
                if (entity.isRemoved()) continue;
                String classname = entity.getClass().getSimpleName().toLowerCase(Locale.US);
                if (classname.contains(filter)) {
                    filteredRadarResults.add(entity);
                }
            }
            return;
        }

        if (cmd[0].equals(CMD_COUNT)) {
            this.tx = "" + filteredRadarResults.size();
            return;
        }

        if (cmd[0].equals(CMD_GETTARGETID) && cmd.length == 2) {
            Entity target = getTargetFromIndex(cmd[1]);
            this.tx = target == null ? "" : "" + target.getId();
            return;
        }

        if (cmd[0].equals(CMD_GETPOSITION) && cmd.length == 2) {
            Entity target = getTargetFromIndex(cmd[1]);
            this.tx = target == null ? "" : (int) Math.floor(target.getX()) + ";" + (int) Math.floor(target.getY()) + ";" + (int) Math.floor(target.getZ());
            return;
        }

        if (cmd[0].equals(CMD_GETNAME) && cmd.length == 2) {
            Entity target = getTargetFromIndex(cmd[1]);
            this.tx = target == null ? "" : target.getClass().getSimpleName().toLowerCase(Locale.US);
        }
    }

    public Entity getTargetFromIndex(String cmd) {
        if (filteredRadarResults.isEmpty()) return null;
        int index = IRORInteractive.parseInt(cmd, 1, filteredRadarResults.size()) - 1;
        if (index < 0 || index >= filteredRadarResults.size()) return null;
        Entity target = filteredRadarResults.get(index);
        return target.isRemoved() ? null : target;
    }

    @Override
    public float[] getColor() {
        return new float[]{0.134F, 1.0F, 0.134F};
    }
}
