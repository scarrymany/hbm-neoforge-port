package com.hbm.handler;

import com.hbm.inventory.gui.CalculatorScreen;
import com.hbm.items.IKeybindReceiver;
import com.hbm.main.MainRegistry;
import com.hbm.packet.KeybindPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Ports CE's {@code com.hbm.handler.HbmKeybinds}: declares every keybind, registers them, and runs the
 * client-side keybind-state diff that syncs to the server via {@link KeybindPacket}.
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
 */
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT)
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

    private static final boolean[] clientKeysPressed = new boolean[EnumKeybind.VALUES.length];

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

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton.Pre event) {
        handleProps(event.getAction() == GLFW.GLFW_PRESS, event.getButton());
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        handleProps(event.getAction() == GLFW.GLFW_PRESS, event.getKey());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) return;

        if (calculatorKey.isDown()) {
            mc.setScreen(new CalculatorScreen());
        }

        // Mirrors CE's postClientTick: the tool-cycle keybind (ABILITY_CYCLE) shares its default binding with
        // vanilla's "use item" key. Sampling this at end-of-tick, rather than from the raw input event, is what
        // lets us tell the two apart once vanilla has had a chance to process the click for the tick.
        if (mc.options.keyUse.getKey().getValue() == abilityCycle.getKey().getValue()) {
            boolean last = clientKeysPressed[EnumKeybind.ABILITY_CYCLE.ordinal()];
            boolean current = abilityCycle.isDown();

            if (last != current) {
                clientKeysPressed[EnumKeybind.ABILITY_CYCLE.ordinal()] = current;
                PacketDistributor.sendToServer(new KeybindPacket(EnumKeybind.ABILITY_CYCLE, current));
                onPressedClient(player, EnumKeybind.ABILITY_CYCLE, current);
            }
        }
    }

    /** Handles keybind props: diffs every {@link EnumKeybind} against its last known state and syncs changes to the server. */
    public static void handleProps(boolean state, int keyCode) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        for (EnumKeybind key : EnumKeybind.VALUES) {
            boolean last = clientKeysPressed[key.ordinal()];
            boolean current = MainRegistry.proxy.getIsKeyPressed(key);

            if (last != current) {

                /// ABILITY HANDLING /// - see onClientTick for why this one is deferred to end-of-tick instead.
                if (key == EnumKeybind.ABILITY_CYCLE && Minecraft.getInstance().options.keyUse.getKey().getValue() == abilityCycle.getKey().getValue())
                    continue;

                clientKeysPressed[key.ordinal()] = current;
                PacketDistributor.sendToServer(new KeybindPacket(key, current));
                onPressedClient(player, key, current);
            }
        }
    }

    public static void onPressedClient(Player player, EnumKeybind key, boolean state) {
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (held.getItem() instanceof IKeybindReceiver rec) {
            if (rec.canHandleKeybind(player, held, key)) rec.handleKeybindClient(player, held, key, state);
        }
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
