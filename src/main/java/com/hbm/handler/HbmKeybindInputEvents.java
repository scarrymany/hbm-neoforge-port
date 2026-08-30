package com.hbm.handler;

import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.inventory.gui.CalculatorScreen;
import com.hbm.items.IKeybindReceiver;
import com.hbm.main.MainRegistry;
import com.hbm.packet.KeybindPacket;
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
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side keybind-state diffing and syncing, split out of {@link HbmKeybinds} because that class
 * must subscribe to the mod bus (for {@code RegisterKeyMappingsEvent}) while this class's events
 * ({@link InputEvent}, {@link ClientTickEvent}) are game-bus-only - a single {@code @EventBusSubscriber}
 * class can only declare one {@code bus()}, and the two families of events cannot share one. See
 * {@link HbmKeybinds}'s class javadoc for the confirmed-real NeoForge fact this split is based on.
 */
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT)
public class HbmKeybindInputEvents {

    private static final boolean[] clientKeysPressed = new boolean[EnumKeybind.VALUES.length];

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

        if (HbmKeybinds.calculatorKey.isDown()) {
            mc.setScreen(new CalculatorScreen());
        }

        // Mirrors CE's postClientTick: the tool-cycle keybind (ABILITY_CYCLE) shares its default binding with
        // vanilla's "use item" key. Sampling this at end-of-tick, rather than from the raw input event, is what
        // lets us tell the two apart once vanilla has had a chance to process the click for the tick.
        if (mc.options.keyUse.getKey().getValue() == HbmKeybinds.abilityCycle.getKey().getValue()) {
            boolean last = clientKeysPressed[EnumKeybind.ABILITY_CYCLE.ordinal()];
            boolean current = HbmKeybinds.abilityCycle.isDown();

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
                if (key == EnumKeybind.ABILITY_CYCLE && Minecraft.getInstance().options.keyUse.getKey().getValue() == HbmKeybinds.abilityCycle.getKey().getValue())
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
}
