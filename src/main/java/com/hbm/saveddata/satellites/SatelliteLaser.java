package com.hbm.saveddata.satellites;

import com.hbm.entity.logic.EntityDeathBlast;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Locale;

/**
 * Ported from CE's {@code com.hbm.saveddata.satellites.SatelliteLaser} (94 lines, read in full) - a
 * representative {@code SAT_PANEL} satellite: a 5-minute cooldown gate around spawning a
 * death-blast payload at a clicked/commanded coordinate.
 * <p>
 * {@link #deathBlast} spawns the real {@link EntityDeathBlast} payload, per
 * {@code docs/phase4/satellites_followup_and_loot_pools.md} - the addressing/dispatch protocol
 * (registration, {@code onClick}/{@code onCommandImpl} dual entry point, cooldown NBT round-trip)
 * was already real before that package landed.
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
        if (!(level instanceof ServerLevel serverLevel) || lastOp + CHARGE_TIME >= level.getGameTime()) return;

        lastOp = level.getGameTime();
        this.markDirty();

        int y = serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);

        EntityDeathBlast blast = new EntityDeathBlast(level);
        blast.setPos(x, y, z);
        blast.detonator = detonator;
        level.addFreshEntity(blast);
    }

    @Override
    public float[] getColor() {
        return new float[]{0.221F, 0.663F, 1.0F};
    }
}
