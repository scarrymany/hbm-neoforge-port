package com.hbm.saveddata.satellites;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Ported from CE's {@code com.hbm.saveddata.satellites.SatelliteResonator} (read in full) - a
 * {@code SAT_COORD} teleportation remote ("Xenium Relay").
 */
public class SatelliteResonator extends Satellite {

    public SatelliteResonator() {
        this.coordAcs.add(CoordActions.HAS_Y);
        this.satIface = Interfaces.SAT_COORD;
    }

    @Override
    public String getType() {
        return "XEN_RELAY";
    }

    @Override
    public Component[] getInfo(Level level) {
        return new Component[]{Component.translatable("satellite.resonator.name")};
    }

    @Override
    public void onCoordAction(Level level, ServerPlayer player, int x, int y, int z) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (player.isPassenger()) player.stopRiding();
        serverLevel.getChunkSource().getChunk(x >> 4, z >> 4, true);

        int actualY = y < 0 ? serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) : y;
        player.teleportTo(x + 0.5D, actualY, z + 0.5D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    public float[] getColor() {
        return new float[]{1.0F, 0.646F, 0.181F};
    }
}
