package com.hbm.inventory.gui.machine.rbmk;

import com.hbm.inventory.container.machine.rbmk.RBMKMenuTypes;
import com.hbm.inventory.gui.SafeMenuScreens;
import com.hbm.main.MainRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client-side {@code MenuType}-to-{@code Screen} binding for this RBMK column-block work package's
 * own {@link RBMKMenuTypes}, mirroring {@code com.hbm.main.ClientModRegistry#registerScreens}'
 * pattern in a package-local class rather than editing that shared file directly (many Phase 2
 * packages land this same wave - see {@link RBMKMenuTypes}'s own javadoc for the identical
 * reasoning). {@code @EventBusSubscriber} self-registers this class's {@code @SubscribeEvent} methods
 * without needing a line added anywhere else - confirmed real NeoForge behavior, already used
 * identically by {@code com.hbm.handler.HbmKeybinds} per {@code ClientModRegistry}'s own javadoc.
 */
// bus = Bus.MOD required: RegisterMenuScreensEvent implements IModBusEvent and only fires on the mod
// bus - @EventBusSubscriber's bus() defaults to Bus.GAME and does not auto-detect IModBusEvent
// (confirmed against real NeoForge 1.21.1 source and FancyModLoader's EventBusSubscriber javadoc).
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class RBMKClientRegistry {

    private RBMKClientRegistry() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        SafeMenuScreens.bind(event, RBMKMenuTypes.ROD, RBMKRodScreen::new);
        SafeMenuScreens.bind(event, RBMKMenuTypes.CONTROL, RBMKControlScreen::new);
        SafeMenuScreens.bind(event, RBMKMenuTypes.CONTROL_AUTO, RBMKControlAutoScreen::new);
        SafeMenuScreens.bind(event, RBMKMenuTypes.STORAGE, RBMKStorageScreen::new);
        SafeMenuScreens.bind(event, RBMKMenuTypes.BOILER, RBMKBoilerScreen::new);
        SafeMenuScreens.bind(event, RBMKMenuTypes.HEATER, RBMKHeaterScreen::new);
        SafeMenuScreens.bind(event, RBMKMenuTypes.CONSOLE, RBMKConsoleScreen::new);
        SafeMenuScreens.bind(event, RBMKMenuTypes.AUTOLOADER, RBMKAutoloaderScreen::new);
        SafeMenuScreens.bind(event, RBMKMenuTypes.OUTGASSER, RBMKOutgasserScreen::new);
    }
}
