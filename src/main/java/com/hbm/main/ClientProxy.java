package com.hbm.main;

import com.hbm.handler.HbmKeybinds;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.particle.HbmEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Client-side proxy, replacing CE's {@code ClientProxy extends ServerProxy}. Only the keybind-relevant slice
 * of CE's much larger ClientProxy (renderer registration, particles, audio, GL caps) is ported here - the rest
 * belongs to other Phase 0 areas, which are expected to add their own {@code @Override}s to this class.
 */
public class ClientProxy extends ServerProxy {

    @Override
    public boolean getIsKeyPressed(EnumKeybind key) {
        return switch (key) {
            case JETPACK -> Minecraft.getInstance().options.keyJump.isDown();
            case TOGGLE_JETPACK -> HbmKeybinds.jetpackKey.isDown();
            case TOGGLE_HEAD -> HbmKeybinds.hudKey.isDown();
            case TOGGLE_MAGNET -> HbmKeybinds.magnetKey.isDown();
            case RELOAD -> HbmKeybinds.reloadKey.isDown();
            case DASH -> HbmKeybinds.dashKey.isDown();
            case CRANE_UP -> HbmKeybinds.craneUpKey.isDown();
            case CRANE_DOWN -> HbmKeybinds.craneDownKey.isDown();
            case CRANE_LEFT -> HbmKeybinds.craneLeftKey.isDown();
            case CRANE_RIGHT -> HbmKeybinds.craneRightKey.isDown();
            case CRANE_LOAD -> HbmKeybinds.craneLoadKey.isDown();
            case ABILITY_CYCLE -> HbmKeybinds.abilityCycle.isDown();
            case ABILITY_ALT -> HbmKeybinds.abilityAlt.isDown();
            case TOOL_ALT -> HbmKeybinds.copyToolAlt.isDown();
            case TOOL_CTRL -> HbmKeybinds.copyToolCtrl.isDown();
            case GUN_PRIMARY -> HbmKeybinds.gunPrimaryKey.isDown();
            case GUN_SECONDARY -> HbmKeybinds.gunSecondaryKey.isDown();
            case GUN_TERTIARY -> HbmKeybinds.gunTertiaryKey.isDown();
        };
    }

    @Override
    public Player me() {
        return Minecraft.getInstance().player;
    }

    /**
     * Real client-side dispatch, mirroring CE's {@code ClientProxy.effectNT(HbmEffectNT,x,y,z,
     * NBTTagCompound)} ({@code upstream/hbm-ce/.../main/ClientProxy.java:392-394}). {@link
     * ServerProxy}'s base is a correct no-op (server has no particle system) but this override was
     * missing entirely, so already-client-side callers that reach {@code MainRegistry.proxy.effectNT(...)}
     * without a network round-trip (e.g. {@code EntityMist}) silently did nothing.
     */
    @Override
    public void effectNT(HbmEffect effect, double x, double y, double z, CompoundTag data) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            effect.summonParticle(level, x, y, z, data);
        }
    }
}
