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
 * <p>{@link #CRATE}/{@link #BATTERY}/{@link #FLUID_TANK} are this registry's first concrete entries
 * (Phase 2's storage-machines package - see {@code docs/phase2/machines_storage.md}), added following
 * exactly the pattern this class's own javadoc originally sketched. Once a machine's {@link MenuBase}
 * subclass exists, register it here through {@link #reg(String, IContainerFactory)}, e.g.:
 * <pre>{@code
 * public static final DeferredHolder<MenuType<?>, MenuType<MachineFooMenu>> MACHINE_FOO =
 *         reg("machine_foo", MachineFooMenu::new);
 * }</pre>
 * then add the matching {@code event.register(ModMenuTypes.MACHINE_FOO.get(), MachineFooScreen::new)}
 * line to {@link com.hbm.main.ClientModRegistry#registerScreens} (already done below for these three).
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

    public static final DeferredHolder<MenuType<?>, MenuType<CrateMenu>> CRATE =
            reg("crate", CrateMenu::fromNetwork);
    public static final DeferredHolder<MenuType<?>, MenuType<BatteryMenu>> BATTERY =
            reg("machine_battery", BatteryMenu::fromNetwork);
    public static final DeferredHolder<MenuType<?>, MenuType<FluidTankMenu>> FLUID_TANK =
            reg("machine_fluidtank_basic", FluidTankMenu::fromNetwork);

    /** Phase 3 ({@code missile_launch_infra}): shared by both the small and large launch pad, matching CE's own {@code TileEntityLaunchPadBase.provideContainer}. */
    public static final DeferredHolder<MenuType<?>, MenuType<LaunchPadMenu>> LAUNCH_PAD =
            reg("launch_pad", LaunchPadMenu::fromNetwork);
    public static final DeferredHolder<MenuType<?>, MenuType<LaunchPadRustedMenu>> LAUNCH_PAD_RUSTED =
            reg("launch_pad_rusted", LaunchPadRustedMenu::fromNetwork);
    public static final DeferredHolder<MenuType<?>, MenuType<LaunchpadSoyuzMenu>> LAUNCHPAD_SOYUZ =
            reg("launchpad_soyuz", LaunchpadSoyuzMenu::fromNetwork);

    /** Phase 4 ({@code entities_vehicles_aircraft} - rail/train package): the two entity-backed cargo
     * menus, opened via {@link EntityMenuBase} rather than {@link MenuBase} since their inventory
     * owner is an {@code Entity}, not a {@code MachineBaseBlockEntity} - see that class's javadoc. */
    public static final DeferredHolder<MenuType<?>, MenuType<TrainCargoTramMenu>> TRAIN_CARGO_TRAM =
            reg("entity_ntm_cargo_tram", TrainCargoTramMenu::fromNetwork);
    public static final DeferredHolder<MenuType<?>, MenuType<TrainCargoTramTrailerMenu>> TRAIN_CARGO_TRAM_TRAILER =
            reg("entity_ntm_cargo_tram_trailer", TrainCargoTramTrailerMenu::fromNetwork);

    /** Phase 4 ({@code entities_vehicles_aircraft} - minecart package): the two entity-backed cargo
     * menus for {@code com.hbm.entity.cart}, same {@link EntityMenuBase} shape as the rail/train pair
     * above. */
    public static final DeferredHolder<MenuType<?>, MenuType<MinecartCrateMenu>> CART_CRATE =
            reg("entity_ntm_cart_crate", MinecartCrateMenu::fromNetwork);
    public static final DeferredHolder<MenuType<?>, MenuType<MinecartDestroyerMenu>> CART_DESTROYER =
            reg("entity_ntm_cart_destroyer", MinecartDestroyerMenu::fromNetwork);

    public static final DeferredHolder<MenuType<?>, MenuType<LemegetonMenu>> LEMEGETON =
            reg("lemegeton", LemegetonMenu::fromNetwork);

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
