package com.hbm.handler;

import com.hbm.main.MainRegistry;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/**
 * Ports CE's {@code com.hbm.handler.HbmKeybinds}: declares every keybind and registers them. The
 * client-side keybind-state diff that syncs each press/release to the server lives in
 * {@code com.hbm.handler.HbmKeybindInputEvents} (split out for the bus reason below).
 *
 * <p>Two intentional deviations from CE, both mechanical consequences of the 1.21 input API:
 *
 * <p>1. CE's reflection-based keybind-overlap hack ({@code handleOverlap}, built on Forge-1.12-internal
 * {@code MethodHandleHelper}/{@code KeyBindingMap}) has no NeoForge equivalent and is dropped, matching the
 * Neo Edition reference (whose own {@code handleOverlap} is commented out and unused).
 *
 * <p>2. CE never registers an explicit keybind for {@code GUN_PRIMARY} - it polls {@code Mouse.isButtonDown(0)}
 * directly in {@code ClientProxy.getIsKeyPressed}. This port instead registers {@link #gunPrimaryKey} bound to
 * the left mouse button by default and polls it like every other binding, following the Neo Edition reference's
 * verified working approach rather than guessing at an unconfirmed raw-GLFW mouse-polling API. Behavior is
 * unchanged (still the left mouse button by default); only the plumbing differs from CE.
 *
 * <p>{@code bus = Bus.MOD} is required here: {@link RegisterKeyMappingsEvent} implements
 * {@code net.neoforged.fml.event.IModBusEvent} and only ever fires on the mod bus - confirmed against
 * real NeoForge 1.21.1 source ({@code RegisterKeyMappingsEvent extends Event implements IModBusEvent})
 * and against FancyModLoader's {@code EventBusSubscriber} javadoc, which states {@code bus()} defaults
 * to {@code Bus.GAME} and does not auto-detect {@code IModBusEvent}. The input/tick handlers that used
 * to live in this class were split out to {@code com.hbm.handler.HbmKeybindInputEvents} for exactly
 * this reason - a single {@code @EventBusSubscriber} class can only subscribe to one bus, and
 * {@code InputEvent}/{@code ClientTickEvent} are game-bus events.
 */
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class HbmKeybinds {

    public static final String category = "key.categories.hbm";

    public static final KeyMapping calculatorKey = new KeyMapping(category + ".calculator", InputConstants.Type.KEYSYM, InputConstants.KEY_N, category);
    public static final KeyMapping jetpackKey = new KeyMapping(category + ".toggleBack", InputConstants.Type.KEYSYM, InputConstants.KEY_C, category);
    public static final KeyMapping hudKey = new KeyMapping(category + ".toggleHUD", InputConstants.Type.KEYSYM, InputConstants.KEY_V, category);
    public static final KeyMapping magnetKey = new KeyMapping(category + ".toggleMagnet", InputConstants.Type.KEYSYM, InputConstants.KEY_Z, category);
    public static final KeyMapping reloadKey = new KeyMapping(category + ".reload", InputConstants.Type.KEYSYM, InputConstants.KEY_R, category);
    public static final KeyMapping dashKey = new KeyMapping(category + ".dash", InputConstants.Type.KEYSYM, InputConstants.KEY_LSHIFT, category);

    public static final KeyMapping craneUpKey = new KeyMapping(category + ".craneMoveUp", InputConstants.Type.KEYSYM, InputConstants.KEY_UP, category);
    public static final KeyMapping craneDownKey = new KeyMapping(category + ".craneMoveDown", InputConstants.Type.KEYSYM, InputConstants.KEY_DOWN, category);
    public static final KeyMapping craneLeftKey = new KeyMapping(category + ".craneMoveLeft", InputConstants.Type.KEYSYM, InputConstants.KEY_LEFT, category);
    public static final KeyMapping craneRightKey = new KeyMapping(category + ".craneMoveRight", InputConstants.Type.KEYSYM, InputConstants.KEY_RIGHT, category);
    public static final KeyMapping craneLoadKey = new KeyMapping(category + ".craneLoad", InputConstants.Type.KEYSYM, InputConstants.KEY_RETURN, category);

    public static final KeyMapping qmaw = new KeyMapping(category + ".qmaw", InputConstants.Type.KEYSYM, InputConstants.KEY_F1, category);

    public static final KeyMapping abilityCycle = new KeyMapping(category + ".ability", InputConstants.Type.MOUSE, InputConstants.MOUSE_BUTTON_RIGHT, category);
    public static final KeyMapping abilityAlt = new KeyMapping(category + ".abilityAlt", InputConstants.Type.KEYSYM, InputConstants.KEY_LALT, category);
    public static final KeyMapping copyToolAlt = new KeyMapping(category + ".copyToolAlt", InputConstants.Type.KEYSYM, InputConstants.KEY_LALT, category);
    public static final KeyMapping copyToolCtrl = new KeyMapping(category + ".copyToolCtrl", InputConstants.Type.KEYSYM, InputConstants.KEY_LCONTROL, category);
    public static final KeyMapping gunPrimaryKey = new KeyMapping(category + ".gunPrimary", InputConstants.Type.MOUSE, InputConstants.MOUSE_BUTTON_LEFT, category);
    public static final KeyMapping gunSecondaryKey = new KeyMapping(category + ".gunSecondary", InputConstants.Type.MOUSE, InputConstants.MOUSE_BUTTON_RIGHT, category);
    public static final KeyMapping gunTertiaryKey = new KeyMapping(category + ".gunTertitary", InputConstants.Type.MOUSE, InputConstants.MOUSE_BUTTON_MIDDLE, category);

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(calculatorKey);
        event.register(jetpackKey);
        event.register(hudKey);
        event.register(magnetKey);
        event.register(reloadKey);
        event.register(dashKey);

        event.register(gunPrimaryKey);
        event.register(gunSecondaryKey);
        event.register(gunTertiaryKey);

        event.register(craneUpKey);
        event.register(craneDownKey);
        event.register(craneLeftKey);
        event.register(craneRightKey);
        event.register(craneLoadKey);

        event.register(abilityCycle);
        event.register(abilityAlt);
        event.register(copyToolAlt);
        event.register(copyToolCtrl);
        event.register(qmaw);
    }

    /**
     * Preserved exactly from CE: member set and order matter, since {@link Enum#ordinal()} is the wire
     * format used by {@link KeybindPacket} and by {@link #clientKeysPressed}.
     */
    public enum EnumKeybind {
        JETPACK,
        TOGGLE_JETPACK,
        TOGGLE_HEAD,
        TOGGLE_MAGNET,
        RELOAD,
        DASH,
        CRANE_UP,
        CRANE_DOWN,
        CRANE_LEFT,
        CRANE_RIGHT,
        CRANE_LOAD,
        ABILITY_CYCLE,
        ABILITY_ALT,
        TOOL_ALT,
        TOOL_CTRL,
        GUN_PRIMARY,
        GUN_SECONDARY,
        GUN_TERTIARY;

        public static final EnumKeybind[] VALUES = values();
    }
}
