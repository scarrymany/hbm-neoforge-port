package com.hbm.inventory.gui.machine.rbmk;

import com.hbm.inventory.container.machine.rbmk.RBMKMenuTypes;
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
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT)
public final class RBMKClientRegistry {

    private RBMKClientRegistry() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(RBMKMenuTypes.ROD.get(), RBMKRodScreen::new);
        event.register(RBMKMenuTypes.CONTROL.get(), RBMKControlScreen::new);
        event.register(RBMKMenuTypes.CONTROL_AUTO.get(), RBMKControlAutoScreen::new);
        event.register(RBMKMenuTypes.STORAGE.get(), RBMKStorageScreen::new);
        event.register(RBMKMenuTypes.BOILER.get(), RBMKBoilerScreen::new);
        event.register(RBMKMenuTypes.CONSOLE.get(), RBMKConsoleScreen::new);
        event.register(RBMKMenuTypes.AUTOLOADER.get(), RBMKAutoloaderScreen::new);
    }
}
