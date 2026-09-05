package com.hbm.inventory.gui.network;

import com.hbm.inventory.container.network.RadioNetworkMenus;
import com.hbm.inventory.gui.SafeMenuScreens;
import com.hbm.inventory.gui.machine.KeypadScreen;
import com.hbm.main.MainRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class RadioNetworkClientRegistry {

    private RadioNetworkClientRegistry() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        SafeMenuScreens.bind(event, RadioNetworkMenus.RADIO_TORCH, RadioTorchScreen::new);
        SafeMenuScreens.bind(event, RadioNetworkMenus.RADIO_TORCH_COUNTER, RadioTorchCounterScreen::new);
        SafeMenuScreens.bind(event, RadioNetworkMenus.KEYPAD, KeypadScreen::new);
    }
}
