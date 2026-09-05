package com.hbm.inventory.container.bomb;

import com.hbm.inventory.container.MenuBase;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * {@link MenuType} registration for bomb-family container menus (Phase 3 bomb + launch infrastructure).
 */
public final class ModBombMenus {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(BuiltInRegistries.MENU, MainRegistry.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<BombMultiMenu>> BOMB_MULTI =
            MENU_TYPES.register("bomb_multi", () -> IMenuTypeExtension.create(BombMultiMenu::fromNetwork));

    private ModBombMenus() {
    }

    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        MENU_TYPES.register(modEventBus);
    }
}
