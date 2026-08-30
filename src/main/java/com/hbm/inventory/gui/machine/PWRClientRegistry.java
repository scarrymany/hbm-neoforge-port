package com.hbm.inventory.gui.machine;

import com.hbm.inventory.container.machine.PWRMenus;
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
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT)
public final class PWRClientRegistry {

    private PWRClientRegistry() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(PWRMenus.PWR_CONTROLLER.get(), PWRControllerScreen::new);
        event.register(PWRMenus.REACTOR_BREEDING.get(), MachineReactorBreedingScreen::new);
    }
}
