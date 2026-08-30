package com.hbm.saveddata.satellites;

import com.hbm.main.MainRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Locale;

/**
 * Ported from CE's {@code com.hbm.saveddata.satellites.SatelliteLaser} (94 lines, read in full) - a
 * representative {@code SAT_PANEL} satellite: a 5-minute cooldown gate around spawning a
 * death-blast payload at a clicked/commanded coordinate.
 * <p>
 * <b>Blocked, documented</b>: {@code com.hbm.entity.logic.EntityDeathBlast} (the actual payload
 * entity) is not ported anywhere in this tree (confirmed by grep of {@code com.hbm.entity.logic} -
 * that package holds {@code EntityBalefire}/{@code EntityExplosionChunkloading}/
 * {@code EntityNukeExplosionMK3}/{@code EntityNukeExplosionMK5}/{@code IChunkLoader}/
 * {@code NukeEntityTypes}, no death-blast entity), per this package's task brief which explicitly
 * flags this dependency and says to stub it. {@link #deathBlast} therefore only manages the
 * cooldown/dirty-flag bookkeeping and logs a warning instead of spawning anything - the addressing/
 * dispatch protocol (registration, {@code onClick}/{@code onCommandImpl} dual entry point,
 * cooldown NBT round-trip) is fully real and ready for the entity to be wired in once it lands.
 */
public class SatelliteLaser extends Satellite {

    public static final String CMD_FIRE = "fire";
    public static final String CMD_CANFIRE = "canfire";

    public static final int CHARGE_TIME = 5 * 60 * 20;

    public long lastOp;

    public SatelliteLaser() {
        this.ifaceAcs.add(InterfaceActions.HAS_MAP);
        this.ifaceAcs.add(InterfaceActions.SHOW_COORDS);
        this.ifaceAcs.add(InterfaceActions.CAN_CLICK);
        this.satIface = Interfaces.SAT_PANEL;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putLong("lastOp", lastOp);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        lastOp = nbt.getLong("lastOp");
    }

    @Override
    public Component[] getInfo(Level level) {
        boolean canFire = lastOp + CHARGE_TIME < level.getGameTime();
        int cooldown = (int) ((lastOp + CHARGE_TIME) - level.getGameTime());

        return new Component[]{
                Component.translatable("satellite.laser.name"),
                canFire ? Component.translatable("satellite.ready") : Component.translatable("satellite.cooldown", cooldown / 20 + "s")
        };
    }

    @Override
    public void onCommandImpl(Level level, String... cmd) {
        if (cmd.length <= 0) return;

        if (cmd[0].equals(CMD_FIRE)) {
            deathBlast(level, targetX, targetZ, null);
            return;
        }

        if (cmd[0].equals(CMD_CANFIRE)) {
            this.tx = ("" + (lastOp + CHARGE_TIME < level.getGameTime())).toUpperCase(Locale.US);
        }
    }

    @Override
    public void onClick(Level level, ServerPlayer player, int x, int z) {
        deathBlast(level, x, z, player);
    }

    public void deathBlast(Level level, int x, int z, ServerPlayer detonator) {
        if (!(level instanceof ServerLevel) || lastOp + CHARGE_TIME >= level.getGameTime()) return;

        lastOp = level.getGameTime();
        this.markDirty();

        // TODO(missile-launch-infra): com.hbm.entity.logic.EntityDeathBlast is not ported yet - see
        // class javadoc. Once it lands, spawn it here at (x, level.getHeight(...), z) with
        // detonator threaded through exactly like CE's own EntityDeathBlast#detonator field.
        MainRegistry.logger.info("[Satellite] Death-ray fire command received at {} / {}, but EntityDeathBlast is not yet ported - no-op.", x, z);
    }

    @Override
    public float[] getColor() {
        return new float[]{0.221F, 0.663F, 1.0F};
    }
}
