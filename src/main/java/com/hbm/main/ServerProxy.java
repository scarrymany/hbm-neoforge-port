package com.hbm.main;

import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.particle.HbmEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Dedicated-server-safe default proxy, replacing CE's {@code @SidedProxy}-selected {@code ServerProxy}
 * (NeoForge has no {@code @SidedProxy} annotation; side selection is done explicitly where {@code proxy} is
 * assigned - see this class's javadoc counterpart in the integration report).
 *
 * <p>This port only carries the keybind-polling slice owned by this area. Other areas will extend this class
 * with their own no-op defaults (rendering, audio, particles, GL caps, etc.) as they land.
 */
public class ServerProxy {

    public boolean getIsKeyPressed(EnumKeybind key) {
        return false;
    }

    public Player me() {
        return null;
    }

    /**
     * CE: {@code ServerProxy.effectNT(HbmEffectNT, x, y, z, NBTTagCompound)} - a no-op on the
     * dedicated-server/common side ({@code upstream/hbm-ce/.../main/ServerProxy.java:41,43-44}); the
     * server only ever broadcasts {@link com.hbm.packet.toclient.HbmEffectPacket} via
     * {@link HbmEffect#sendPacket}, it never runs a handler itself. See {@link ClientProxy}'s override
     * for the real client-side dispatch - {@code docs/phase5/particle_engine_and_generic_vfx.md}'s
     * "Recommended architecture" point 2.
     */
    public void effectNT(HbmEffect effect, double x, double y, double z, CompoundTag data) {
    }
}
