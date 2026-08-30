package com.hbm.saveddata.satellites;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ported from CE's {@code com.hbm.saveddata.satellites.SatelliteMapper} (read in full) - a
 * {@code SAT_PANEL} satellite with a map/chunk-loaded overlay and three text commands.
 * <p>
 * {@code CMD_GETSMOG} is a documented forward-reference stub: CE's {@code PollutionHandler} (the
 * pollution/smog simulation) is not ported anywhere in this tree yet (confirmed by grep - only
 * {@code ItemPollutionDetector} references it, itself presumably carrying the same forward
 * reference). This command replies with an empty {@link #tx} until that system lands, rather than
 * throwing or fabricating a value.
 */
public class SatelliteMapper extends Satellite {

    public static final String CMD_TARGET_LOADED = "targetloaded";
    public static final String CMD_GETSMOG = "getsmog";
    public static final String CMD_SPOT_PLAYER = "spotplayers";

    public static final int SPOT_PLAYER_MAX_RANGE = 250;

    public SatelliteMapper() {
        this.ifaceAcs.add(InterfaceActions.HAS_MAP);
        this.satIface = Interfaces.SAT_PANEL;
    }

    @Override
    public String getType() {
        return "NOT_A_SPY_SATELLITE_:)";
    }

    @Override
    public Component[] getInfo(Level level) {
        return new Component[]{Component.translatable("satellite.mapper.name")};
    }

    @Override
    public void onCommandImpl(Level level, String... cmd) {
        if (cmd.length <= 0 || !(level instanceof ServerLevel serverLevel)) return;

        if (cmd[0].equals(CMD_TARGET_LOADED)) {
            this.tx = ("" + serverLevel.getChunkSource().hasChunk(targetX >> 4, targetZ >> 4)).toUpperCase(Locale.US);
            return;
        }

        if (cmd[0].equals(CMD_GETSMOG)) {
            // TODO(pollution-system): com.hbm.handler.pollution.PollutionHandler is not ported yet -
            // this command is a no-op stub until that simulation lands (see class javadoc).
            this.tx = "";
            return;
        }

        if (cmd[0].equals(CMD_SPOT_PLAYER)) {
            List<String> names = new ArrayList<>();

            for (Player player : serverLevel.players()) {
                int x = (int) Math.floor(player.getX());
                int z = (int) Math.floor(player.getZ());

                double dX = x - targetX;
                double dZ = z - targetZ;

                if (dX * dX + dZ * dZ <= (double) SPOT_PLAYER_MAX_RANGE * SPOT_PLAYER_MAX_RANGE) {
                    int height = serverLevel.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
                    if (height < player.getY() + 2) names.add(player.getGameProfile().getName());
                }
            }

            this.tx = names.isEmpty() ? "NONE" : String.join(";", names);
        }
    }

    @Override
    public float[] getColor() {
        return new float[]{0.538F, 1.0F, 0.523F};
    }
}
