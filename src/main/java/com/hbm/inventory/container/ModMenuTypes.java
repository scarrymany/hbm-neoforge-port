package com.hbm.inventory.container;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central {@link MenuType} registry, mirroring {@code com.hbm.blocks.ModBlocks}/
 * {@code com.hbm.items.ModItems}'s established "one {@code DeferredRegister}, fields land here one
 * per concrete piece of content as later work adds it" convention (see {@code ModBlocks}'s own
 * javadoc for the precedent this follows).
 *
 * <p>Deliberately empty of actual {@link DeferredHolder} fields for now, same as {@code ModBlocks}
 * was in Phase 0: no concrete Phase 2 machine {@link MenuBase}/{@code Screen} pair exists yet (this
 * package is the shared-base prerequisite those pairs will build on, per
 * {@code docs/phase2/gui_framework.md}'s Deferred scope). Once a machine's {@link MenuBase} subclass
 * exists, register it here through {@link #reg(String, IContainerFactory)}, e.g.:
 * <pre>{@code
 * public static final DeferredHolder<MenuType<?>, MenuType<MachineFooMenu>> MACHINE_FOO =
 *         reg("machine_foo", MachineFooMenu::new);
 * }</pre>
 * then add the matching {@code event.register(ModMenuTypes.MACHINE_FOO.get(), MachineFooScreen::new)}
 * line to {@link com.hbm.main.ClientModRegistry#registerScreens}.
 *
 * <h2>Confirmed real NeoForge 1.21.1 API - not invented</h2>
 * Both the {@link DeferredRegister}/{@link #reg} shape and the
 * {@code RegisterMenuScreensEvent} handler this pairs with (see
 * {@link com.hbm.main.ClientModRegistry#registerScreens}) are copied verbatim (field/method shape,
 * not content) from Neo Edition's real, confirmed-compiling
 * {@code com.hbm.inventory.NtmMenuTypes}/{@code com.hbm.main.CommonEvents.registerScreens}.
 * {@link IMenuTypeExtension#create(IContainerFactory)} is used instead of calling
 * {@code new MenuType<>(factory, FeatureFlags...)} directly - this NeoForge extension point exists
 * specifically so the client-side {@code (int windowId, Inventory inv,
 * RegistryFriendlyByteBuf extraData)} factory signature can be used without also hand-rolling a
 * vanilla {@code MenuType.MenuSupplier} datagen entry.
 */
public final class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, MainRegistry.MODID);

    private ModMenuTypes() {
    }

    /**
     * Registration helper every future machine {@link MenuType} field should go through, instead of
     * constructing {@code MenuType} directly - see the class javadoc for why.
     */
    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> reg(String name, IContainerFactory<T> factory) {
        return MENU_TYPES.register(name, () -> IMenuTypeExtension.create(factory));
    }

    public static void register(IEventBus modEventBus) {
        MENU_TYPES.register(modEventBus);
    }
}
