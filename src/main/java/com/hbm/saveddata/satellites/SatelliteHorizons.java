package com.hbm.saveddata.satellites;

import com.hbm.entity.projectile.EntityTom;
import com.hbm.main.AdvancementManager;
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
 * {@link #onOrbit}/{@link #theHorizons} spawn the real {@link EntityTom} payload and grant the real
 * {@link AdvancementManager#horizonsStart}/{@link AdvancementManager#horizonsEnd} advancements, per
 * {@code docs/phase4/satellites_followup_and_loot_pools.md} - both dependencies were already real
 * (the foundation wave's own {@code AdvancementManager}, this package's own {@code EntityTom})
 * before this class's forward references were wired in. The addressing/dispatch protocol itself
 * (registration, {@code onCommandImpl}/{@code onCoordAction} dual entry point, NBT round-trip) was
 * already real before either landed.
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
        if (level instanceof ServerLevel serverLevel) {
            for (ServerPlayer p : serverLevel.players()) {
                AdvancementManager.grantAchievement(p, AdvancementManager.horizonsStart);
            }
        }
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

        serverLevel.getChunkSource().getChunk(x >> 4, z >> 4, true);

        EntityTom tom = new EntityTom(level);
        tom.setPos(x + 0.5, 600, z + 0.5);
        level.addFreshEntity(tom);

        for (ServerPlayer p : serverLevel.players()) {
            AdvancementManager.grantAchievement(p, AdvancementManager.horizonsEnd);
        }

        // CE: "not necessary but JUST to make sure" - a server-wide flavor-text broadcast, kept
        // faithfully rather than dropped as cosmetic (it's a one-line real API call, not a forward
        // reference).
        serverLevel.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("chat.gerald.detonated"), false);
    }

    @Override
    public float[] getColor() {
        return new float[]{0.0F, 0.0F, 0.0F};
    }
}
