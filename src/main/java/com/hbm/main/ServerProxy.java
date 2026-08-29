package com.hbm.main;

import com.hbm.handler.HbmKeybinds.EnumKeybind;
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
}
