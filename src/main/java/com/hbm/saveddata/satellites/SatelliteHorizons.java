package com.hbm.saveddata.satellites;

import com.hbm.main.MainRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Locale;

/**
 * Ported from CE's {@code com.hbm.saveddata.satellites.SatelliteHorizons} (107 lines, read in
 * full) - the "gerald"/{@code sat_gerald} one-shot easter-egg satellite ({@code SAT_COORD}).
 * <p>
 * <b>Blocked, documented</b>: {@code com.hbm.entity.projectile.EntityTom} (the payload entity this
 * satellite spawns) is not ported anywhere in this tree (confirmed absent from
 * {@code com.hbm.entity.projectile}'s real file list - see this port's own
 * {@code EntityBulletBaseMK4}/{@code EntityThrowableInterp}/etc, no {@code EntityTom}). {@code
 * AdvancementManager} (CE's achievement hooks) is likewise not ported. {@link #theHorizons} manages
 * the "already used" one-shot flag and dirty-marking (the part of CE's behavior that's pure data,
 * not entity spawning) and logs rather than spawning anything - the addressing/dispatch protocol
 * itself (registration, {@code onCommandImpl}/{@code onCoordAction} dual entry point, NBT
 * round-trip) is fully real.
 */
public class SatelliteHorizons extends Satellite {

    public static final String CMD_FIRE = "fire";
    public static final String CMD_CANFIRE = "canfire";

    public boolean used = false;
    public long lastOp;

    public SatelliteHorizons() {
        this.satIface = Interfaces.SAT_COORD;
    }

    @Override
    public String getType() {
        return "PAYLOAD_UNKNOWN";
    }

    @Override
    public Component[] getInfo(Level level) {
        return new Component[]{
                Component.translatable("satellite.horizons.name"),
                used ? Component.translatable("satellite.spent") : Component.translatable("satellite.ready")
        };
    }

    @Override
    public void onOrbit(Level level, double x, double y, double z) {
        // TODO(advancements): com.hbm.main.AdvancementManager is not ported - CE grants horizonsStart here.
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putBoolean("used", used);
        nbt.putLong("lastOp", lastOp);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        used = nbt.getBoolean("used");
        lastOp = nbt.getLong("lastOp");
    }

    @Override
    public void onCommandImpl(Level level, String... cmd) {
        if (cmd.length <= 0) return;

        if (cmd[0].equals(CMD_FIRE)) {
            theHorizons(level, targetX, targetZ);
            return;
        }

        if (cmd[0].equals(CMD_CANFIRE)) {
            this.tx = ("" + !used).toUpperCase(Locale.US);
        }
    }

    @Override
    public void onCoordAction(Level level, ServerPlayer player, int x, int y, int z) {
        this.setTarget(x, z);
        this.theHorizons(level, x, z);
    }

    public void theHorizons(Level level, int x, int z) {
        if (used || !(level instanceof ServerLevel serverLevel)) return;

        used = true;
        this.markDirty();

        // TODO(missile-launch-infra): com.hbm.entity.projectile.EntityTom is not ported yet - see
        // class javadoc. Once it lands, spawn it here at (x, 600, z) and force-load its chunk.
        serverLevel.getChunkSource().getChunk(x >> 4, z >> 4, true);
        MainRegistry.logger.info("[Satellite] Horizons fire command received at {} / {}, but EntityTom is not yet ported - no-op.", x, z);
    }

    @Override
    public float[] getColor() {
        return new float[]{0.0F, 0.0F, 0.0F};
    }
}
