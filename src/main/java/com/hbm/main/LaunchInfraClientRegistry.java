package com.hbm.main;

import com.hbm.inventory.container.ModMenuTypes;
import com.hbm.inventory.gui.LaunchPadRustedScreen;
import com.hbm.inventory.gui.LaunchPadScreen;
import com.hbm.inventory.gui.LaunchpadSoyuzScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client-side {@link net.minecraft.world.inventory.MenuType}-to-{@code Screen} binding for
 * {@code docs/phase3/missile_launch_infra.md}'s three GUIs, following the exact
 * {@code event.register(MENU_TYPE.get(), Screen::new)} shape {@code com.hbm.main.ClientModRegistry}
 * already established (see that class's own javadoc). Kept as its own, separate
 * {@code @EventBusSubscriber} class per this package's own task brief (avoids concurrent-edit
 * collisions on the shared {@code ClientModRegistry} class during this wave) - NeoForge's
 * {@link RegisterMenuScreensEvent} broadcasts to every subscriber, so a second class subscribing to
 * it works identically to adding a line to the first.
 */
// bus = Bus.MOD required: RegisterMenuScreensEvent implements IModBusEvent and only fires on the mod
// bus - @EventBusSubscriber's bus() defaults to Bus.GAME and does not auto-detect IModBusEvent
// (same fix already applied to every other mod-bus-event subscriber in this port).
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class LaunchInfraClientRegistry {

    private LaunchInfraClientRegistry() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.LAUNCH_PAD.get(), LaunchPadScreen::new);
        event.register(ModMenuTypes.LAUNCH_PAD_RUSTED.get(), LaunchPadRustedScreen::new);
        event.register(ModMenuTypes.LAUNCHPAD_SOYUZ.get(), LaunchpadSoyuzScreen::new);
    }
}
