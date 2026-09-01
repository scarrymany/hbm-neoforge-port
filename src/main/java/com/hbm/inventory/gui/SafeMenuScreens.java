package com.hbm.inventory.gui;

import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

/**
 * Null-safe {@link RegisterMenuScreensEvent} bind. Menu families that assign
 * {@link DeferredHolder} fields from {@code registerAll()} can leave a holder null if that
 * family was never wired — a single NPE must not take down the whole client.
 */
public final class SafeMenuScreens {

    private SafeMenuScreens() {
    }

    public static <M extends AbstractContainerMenu> void bind(
            RegisterMenuScreensEvent event,
            @Nullable DeferredHolder<MenuType<?>, MenuType<M>> holder,
            MenuScreens.ScreenConstructor<M, ? extends AbstractContainerScreen<M>> factory) {
        if (holder == null) {
            MainRegistry.logger.warn("Skipping menu screen: DeferredHolder is null (family not registered)");
            return;
        }
        event.register(holder.get(), factory);
    }
}
