package com.hbm.saveddata.satellites;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.blockentity.network.RTTYSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Ported from CE's {@code com.hbm.saveddata.satellites.SatelliteRelay} (read in full) - a
 * dimension-crossing redstone-over-radio relay ({@code Interfaces.NONE}, configured/fired entirely
 * via one {@code onCommandImpl} text command).
 * <p>
 * <b>Adapted, documented</b>: CE addressed the target world by a raw legacy Forge-1.12 int
 * dimension id ({@code net.minecraftforge.common.DimensionManager.getWorld(dim)}). 1.21.1 dimensions
 * are addressed by {@link net.minecraft.resources.ResourceKey}, not by int, and there is no general
 * int-&gt;dimension mapping any more (arbitrary mod-added dimensions have no numeric id at all). This
 * port maps only the three vanilla dimension ids CE's own int scheme universally supported
 * ({@code 0}=overworld, {@code -1}=the nether, {@code 1}=the end) - an arbitrary modded target
 * dimension (not expressible without a further protocol change to accept a namespaced id string
 * instead of an int) is out of scope for this pass and logs a warning rather than silently doing
 * nothing.
 * <p>
 * {@code AdvancementManager.grantAchievement} (CE's {@code onOrbit} hook) is dropped: that class is
 * not ported anywhere in this tree yet (advancement/achievement wiring is out of this package's
 * scope, matching this port's established policy of not inventing achievement plumbing incidentally).
 */
public class SatelliteRelay extends Satellite {

    public static final String CMD_RELAY = "relay";

    public SatelliteRelay() {
        this.satIface = Interfaces.NONE;
    }

    @Override
    public String getType() {
        return "DIMENSIONAL_RELAY";
    }

    @Override
    public Component[] getInfo(Level level) {
        return new Component[]{Component.translatable("satellite.relay.name")};
    }

    @Override
    public void onOrbit(Level level, double x, double y, double z) {
        // TODO(advancements): com.hbm.main.AdvancementManager is not ported - CE grants achFOEQ here.
    }

    @Override
    public void onCommandImpl(Level level, String... cmd) {
        if (cmd.length <= 0) return;

        if (cmd[0].equals(CMD_RELAY) && cmd.length > 3) {
            int dim = IRORInteractive.parseInt(cmd[1], Integer.MIN_VALUE, Integer.MAX_VALUE);
            String chan = cmd[2];

            if (!(level instanceof ServerLevel serverLevel)) return;
            MinecraftServer server = serverLevel.getServer();
            ServerLevel targetWorld = resolveLegacyDimension(server, dim);

            if (targetWorld != null) {
                StringBuilder signal = new StringBuilder();
                for (int i = 3; i < cmd.length; i++) {
                    if (i > 3) signal.append(" ");
                    signal.append(cmd[i]);
                }

                RTTYSystem.broadcast(targetWorld, chan, signal.toString());
            } else {
                com.hbm.main.MainRegistry.logger.warn("[Satellite] relay command targeted unmapped legacy dimension id {} - only 0/-1/1 (overworld/nether/end) are supported.", dim);
            }
        }
    }

    private static ServerLevel resolveLegacyDimension(MinecraftServer server, int dim) {
        return switch (dim) {
            case 0 -> server.getLevel(Level.OVERWORLD);
            case -1 -> server.getLevel(Level.NETHER);
            case 1 -> server.getLevel(Level.END);
            default -> null;
        };
    }

    @Override
    public float[] getColor() {
        return new float[]{0.0F, 0.0F, 0.0F};
    }
}
