package com.hbm.inventory.container.machine.rbmk;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * {@link MenuType} registry for this RBMK column-block work package's own Menus - kept separate from
 * the shared {@code com.hbm.inventory.container.ModMenuTypes} on purpose (many Phase 2 packages land
 * this same wave; a shared file would race). Registered from
 * {@code MainRegistry}/{@code ModItems}/{@code ModBlocks} per this task's own wiring-snippet
 * mechanism - see this task's {@code wiringSnippet} output for the one-line call needed.
 */
public final class RBMKMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, MainRegistry.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<RBMKRodMenu>> ROD = reg("rbmk_rod", RBMKRodMenu::fromNetwork);
    public static final DeferredHolder<MenuType<?>, MenuType<RBMKControlMenu>> CONTROL = reg("rbmk_control", RBMKControlMenu::fromNetwork);
    public static final DeferredHolder<MenuType<?>, MenuType<RBMKControlAutoMenu>> CONTROL_AUTO = reg("rbmk_control_auto", RBMKControlAutoMenu::fromNetwork);
    public static final DeferredHolder<MenuType<?>, MenuType<RBMKStorageMenu>> STORAGE = reg("rbmk_storage", RBMKStorageMenu::fromNetwork);
    public static final DeferredHolder<MenuType<?>, MenuType<RBMKBoilerMenu>> BOILER = reg("rbmk_boiler", RBMKBoilerMenu::fromNetwork);
    public static final DeferredHolder<MenuType<?>, MenuType<RBMKHeaterMenu>> HEATER = reg("rbmk_heater", RBMKHeaterMenu::fromNetwork);
    public static final DeferredHolder<MenuType<?>, MenuType<RBMKConsoleMenu>> CONSOLE = reg("rbmk_console", RBMKConsoleMenu::fromNetwork);
    public static final DeferredHolder<MenuType<?>, MenuType<RBMKAutoloaderMenu>> AUTOLOADER = reg("rbmk_autoloader", RBMKAutoloaderMenu::fromNetwork);
    public static final DeferredHolder<MenuType<?>, MenuType<RBMKOutgasserMenu>> OUTGASSER = reg("rbmk_outgasser", RBMKOutgasserMenu::fromNetwork);

    private RBMKMenuTypes() {
    }

    private static <T extends net.minecraft.world.inventory.AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> reg(
            String name, IContainerFactory<T> factory) {
        return MENU_TYPES.register(name, () -> IMenuTypeExtension.create(factory));
    }

    public static void register(IEventBus modEventBus) {
        MENU_TYPES.register(modEventBus);
    }
}
