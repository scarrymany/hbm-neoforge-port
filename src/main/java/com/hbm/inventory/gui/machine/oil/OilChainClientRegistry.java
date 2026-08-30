package com.hbm.inventory.gui.machine.oil;

import com.hbm.inventory.container.machine.oil.OilChainMenus;
import com.hbm.main.MainRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client-side {@link net.minecraft.world.inventory.MenuType}-to-{@code Screen} binding for the oil
 * chain's two GUI-bearing families, matching {@code PowerGenClientRegistry}'s established shape (a
 * separate {@code @EventBusSubscriber} class rather than editing
 * {@code com.hbm.main.ClientModRegistry#registerScreens} directly, avoiding the same multi-agent
 * shared-file race).
 */
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT)
public final class OilChainClientRegistry {

    private OilChainClientRegistry() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(OilChainMenus.MACHINE_OIL_WELL.get(), MachineOilWellScreen::new);
        event.register(OilChainMenus.MACHINE_REFINERY.get(), MachineRefineryScreen::new);
    }
}
