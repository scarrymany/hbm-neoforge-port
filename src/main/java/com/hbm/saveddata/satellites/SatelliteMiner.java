package com.hbm.saveddata.satellites;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.saveddata.satellites.SatelliteMiner} (70 lines, read in full) - a
 * representative {@code Interfaces.NONE} satellite: no designator interaction at all, payload
 * arrives passively via {@link #getCargo()}, consumed by whatever the "asteroid miner" delivery
 * mechanism is (not itself in this package's scope).
 * <p>
 * <b>Pool-key convention (settled, not a gap anymore)</b>: CE's {@code CARGO} map stores a plain
 * {@code String} pool identifier resolved through {@code com.hbm.itempool.ItemPoolsSatellite}'s
 * {@code POOL_SAT_MINER}/{@code POOL_SAT_LUNAR} constants ({@code "POOL_SAT_MINER"}/
 * {@code "POOL_SAT_LUNAR"} in CE). This class deliberately keys its own {@code CARGO} map with
 * {@code "sat_miner"} instead (see {@link #registerCargo}) - per
 * {@code docs/phase4/satellites_followup_and_loot_pools.md}'s Headline finding #5 and Open
 * questions, whoever writes {@code ItemPoolsSatellite} against this port's now-real
 * {@code com.hbm.itempool.ItemPool} framework must define its pool-name constant with the value
 * {@code "sat_miner"} (not a verbatim transcription of CE's own constant value) so the two already-
 * committed lookup keys here continue to match. {@link #getCargo()}'s return value is a pool-name
 * {@code String} for {@code ItemPool.getPool(String)}, not resolved loot itself.
 */
public class SatelliteMiner extends Satellite {

    /** Pool-identifier registry, keyed by concrete class - see class javadoc. */
    private static final Map<Class<? extends SatelliteMiner>, String> CARGO = new HashMap<>();

    public long lastOp;

    public SatelliteMiner() {
        this.satIface = Interfaces.NONE;
    }

    @Override
    public String getType() {
        return "ASTEROID_MINER";
    }

    @Override
    public Component[] getInfo(Level level) {
        return new Component[]{Component.translatable("satellite.miner.name")};
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

    /** @param cargo the pool-identifier string this satellite class delivers - see class javadoc. */
    protected static void registerCargo(Class<? extends SatelliteMiner> minerSatelliteClass, String cargo) {
        CARGO.put(minerSatelliteClass, cargo);
    }

    /** @return the pool-identifier string this satellite currently carries, or {@code null} if none is registered. */
    public String getCargo() {
        return CARGO.get(getClass());
    }

    static {
        registerCargo(SatelliteMiner.class, "sat_miner");
    }

    @Override
    public float[] getColor() {
        return new float[]{0.0F, 0.0F, 0.0F};
    }
}
