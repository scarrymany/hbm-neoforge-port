package com.hbm.inventory.gui.machine;

import com.hbm.inventory.container.machine.PWRMenus;
import com.hbm.inventory.gui.SafeMenuScreens;
import com.hbm.main.MainRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client-side {@link net.minecraft.world.inventory.MenuType}-to-{@code Screen} binding for this
 * PWR/breeding-reactor package's two GUI-bearing machines - see {@code PowerGenClientRegistry}'s own
 * javadoc for why this is a standalone {@code @EventBusSubscriber} rather than an edit to the shared
 * {@code com.hbm.main.ClientModRegistry#registerScreens}.
 */
// bus = Bus.MOD required: RegisterMenuScreensEvent implements IModBusEvent and only fires on the mod
// bus - @EventBusSubscriber's bus() defaults to Bus.GAME and does not auto-detect IModBusEvent
// (confirmed against real NeoForge 1.21.1 source and FancyModLoader's EventBusSubscriber javadoc).
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class PWRClientRegistry {

    private PWRClientRegistry() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        SafeMenuScreens.bind(event, PWRMenus.PWR_CONTROLLER, PWRControllerScreen::new);
        SafeMenuScreens.bind(event, PWRMenus.REACTOR_BREEDING, MachineReactorBreedingScreen::new);
    }
}
