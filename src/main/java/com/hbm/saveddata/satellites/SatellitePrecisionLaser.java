package com.hbm.saveddata.satellites;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.entity.logic.EntityOrbitalLaser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Locale;

/**
 * Ported from CE's {@code com.hbm.saveddata.satellites.SatellitePrecisionLaser} (read in full) - a
 * {@code SAT_COORD} entity-tracking laser satellite ("Orbital Tattoo Remover").
 * <p>
 * {@link #deathBlast} spawns and detonates the real {@link EntityOrbitalLaser} payload, per
 * {@code docs/phase4/satellites_followup_and_loot_pools.md} - the entity-tracking/dispatch protocol
 * itself was already real before that package landed.
 */
public class SatellitePrecisionLaser extends Satellite {

    public static final String CMD_FIRE = "fire";
    public static final String CMD_CANFIRE = "canfire";
    public static final String CMD_SETENTITYTARGET = "setentitytarget";

    public static final int MAX_TARGET_RANGE = 1_000;
    public static final int CHARGE_TIME = 5 * 20;

    public long lastShot;
    public int targetedEntity = -1;

    public SatellitePrecisionLaser() {
        this.ifaceAcs.add(InterfaceActions.HAS_MAP);
        this.ifaceAcs.add(InterfaceActions.SHOW_COORDS);
        this.satIface = Interfaces.SAT_COORD;
    }

    @Override
    public String getType() {
        return "ORBITAL_TATOO_REMOVER";
    }

    @Override
    public Component[] getInfo(Level level) {
        boolean canFire = lastShot + CHARGE_TIME < level.getGameTime();
        int cooldown = (int) ((lastShot + CHARGE_TIME) - level.getGameTime());

        return new Component[]{
                Component.translatable("satellite.precision_laser.name"),
                canFire ? Component.translatable("satellite.ready") : Component.translatable("satellite.cooldown", cooldown / 20 + "s")
        };
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putLong("lastShot", lastShot);
        nbt.putInt("targetedEntity", targetedEntity);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        lastShot = nbt.getLong("lastShot");
        targetedEntity = nbt.getInt("targetedEntity");
    }

    @Override
    public void onCommandImpl(Level level, String... cmd) {
        if (cmd.length <= 0 || !(level instanceof ServerLevel serverLevel)) return;

        if (cmd[0].equals(CMD_FIRE)) {
            if (this.targetedEntity != -1) {
                Entity e = serverLevel.getEntity(this.targetedEntity);
                this.targetedEntity = -1;

                if (e == null || e.isRemoved()) return;

                int x = (int) Math.floor(e.getX());
                int z = (int) Math.floor(e.getZ());

                double dX = x - targetX;
                double dZ = z - targetZ;

                if (dX * dX + dZ * dZ <= (double) MAX_TARGET_RANGE * MAX_TARGET_RANGE) {
                    double offX = level.getRandom().nextDouble() * 0.05 - 0.025;
                    double offY = level.getRandom().nextDouble() * 0.05 - 0.025;
                    double offZ = level.getRandom().nextDouble() * 0.05 - 0.025;
                    this.deathBlast(level, e.getX() + offX, e.getY() + offY, e.getZ() + offZ);
                    return;
                }
            }

            deathBlast(level, targetX, targetZ);
            return;
        }

        if (cmd[0].equals(CMD_CANFIRE)) {
            this.tx = ((lastShot + CHARGE_TIME < level.getGameTime()) + "").toUpperCase(Locale.US);
            return;
        }

        if (cmd[0].equals(CMD_SETENTITYTARGET) && cmd.length == 2) {
            this.targetedEntity = IRORInteractive.parseInt(cmd[1], Integer.MIN_VALUE, Integer.MAX_VALUE);
        }
    }

    @Override
    public void onCoordAction(Level level, ServerPlayer player, int x, int y, int z) {
        this.setTarget(x, z);
        this.deathBlast(level, targetX, targetZ);
    }

    public void deathBlast(Level level, int x, int z) {
        int y = level instanceof ServerLevel serverLevel ? serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) : 64;
        deathBlast(level, x + 0.5, y, z + 0.5);
    }

    public void deathBlast(Level level, double x, double y, double z) {
        if (lastShot + CHARGE_TIME >= level.getGameTime()) return;

        lastShot = level.getGameTime();

        EntityOrbitalLaser blast = new EntityOrbitalLaser(level);
        blast.setPos(x, y, z);
        blast.explode();
        level.addFreshEntity(blast);
    }

    @Override
    public float[] getColor() {
        return new float[]{1.0F, 0.221F, 0.221F};
    }
}
