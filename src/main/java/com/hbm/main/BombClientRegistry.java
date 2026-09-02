package com.hbm.main;

import com.hbm.inventory.container.bomb.ModBombMenus;
import com.hbm.inventory.gui.bomb.BombMultiScreen;
import com.hbm.inventory.gui.SafeMenuScreens;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client-side Screen registration for bomb-family GUIs, matching this port's established
 * {@code SafeMenuScreens.bind(event, MENU_TYPE, Screen::new)} shape {@code ClientModRegistry}/
 * {@code DummyableProcessClientRegistry} et al.
 */
public final class BombClientRegistry {

    private BombClientRegistry() {
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        SafeMenuScreens.bind(event, ModBombMenus.BOMB_MULTI, BombMultiScreen::new);
    }
}
