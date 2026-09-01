package com.hbm.inventory.container;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * {@link MenuType} for CE {@code ContainerAnvil}. Appends to {@link ModMenuTypes#MENU_TYPES}
 * (same race-avoidance as {@code ProcessingMenus}).
 */
public final class AnvilMenus {

    public static DeferredHolder<MenuType<?>, MenuType<AnvilMenu>> ANVIL;

    private AnvilMenus() {
    }

    public static void registerAll() {
        ANVIL = reg("anvil", AnvilMenu::fromNetwork);
    }

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> reg(String name, IContainerFactory<T> factory) {
        return ModMenuTypes.MENU_TYPES.register(name, () -> IMenuTypeExtension.create(factory));
    }
}
