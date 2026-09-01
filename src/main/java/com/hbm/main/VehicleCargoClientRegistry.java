package com.hbm.main;

import com.hbm.inventory.container.ModMenuTypes;
import com.hbm.inventory.gui.cart.MinecartCrateScreen;
import com.hbm.inventory.gui.cart.MinecartDestroyerScreen;
import com.hbm.inventory.gui.train.TrainCargoTramScreen;
import com.hbm.inventory.gui.train.TrainCargoTramTrailerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.hbm.inventory.gui.SafeMenuScreens;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client-side {@link net.minecraft.world.inventory.MenuType}-to-{@code Screen} binding for Phase 4's
 * {@code entities_vehicles_aircraft} rail/minecart cargo menus - the same
 * {@code SafeMenuScreens.bind(event, MENU_TYPE, Screen::new)} shape {@code com.hbm.main.ClientModRegistry}/
 * {@code com.hbm.main.LaunchInfraClientRegistry} already established (see either class's own javadoc),
 * kept as its own separate {@code @EventBusSubscriber} class for the same reason
 * {@code LaunchInfraClientRegistry} gives: avoids concurrent-edit collisions on the shared
 * {@code ClientModRegistry} class, and NeoForge's {@link RegisterMenuScreensEvent} broadcasts to every
 * subscriber regardless of how many separate classes listen for it.
 * <p>
 * <b>Review-pass fix (dead-wiring gap)</b>: {@code ModMenuTypes.TRAIN_CARGO_TRAM}/
 * {@code TRAIN_CARGO_TRAM_TRAILER}/{@code CART_CRATE}/{@code CART_DESTROYER} were all fully
 * registered {@link net.minecraft.world.inventory.MenuType}s with working {@code Menu} subclasses,
 * and every one of their 4 owning entities ({@code TrainCargoTram}/{@code TrainCargoTramTrailer}/
 * {@code EntityMinecartCrate}/{@code EntityMinecartDestroyer}) already calls
 * {@code player.openMenu(this, ...)} from a real {@code interact} override - but no
 * {@link RegisterMenuScreensEvent} subscriber anywhere bound any of the four to a client
 * {@code Screen}. {@link net.minecraft.client.gui.screens.MenuScreens} silently refuses to open a
 * menu type with no registered factory (logs a warning, does nothing) rather than crashing, so this
 * was a silent client-side no-op on right-click, not a server error - easy to miss without actually
 * opening one of the four GUIs in a running client. Fixed by adding this class alongside the 4 new
 * {@code Screen} classes it references.
 */
// bus = Bus.MOD required: RegisterMenuScreensEvent implements IModBusEvent and only fires on the mod
// bus - @EventBusSubscriber's bus() defaults to Bus.GAME and does not auto-detect IModBusEvent (same
// fix already applied to every other mod-bus-event subscriber in this port, e.g.
// LaunchInfraClientRegistry/ClientModRegistry).
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class VehicleCargoClientRegistry {

    private VehicleCargoClientRegistry() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        SafeMenuScreens.bind(event, ModMenuTypes.TRAIN_CARGO_TRAM, TrainCargoTramScreen::new);
        SafeMenuScreens.bind(event, ModMenuTypes.TRAIN_CARGO_TRAM_TRAILER, TrainCargoTramTrailerScreen::new);
        SafeMenuScreens.bind(event, ModMenuTypes.CART_CRATE, MinecartCrateScreen::new);
        SafeMenuScreens.bind(event, ModMenuTypes.CART_DESTROYER, MinecartDestroyerScreen::new);
    }
}
