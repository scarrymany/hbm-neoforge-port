package com.hbm.inventory.gui.machine.oil;

import com.hbm.inventory.container.machine.oil.OilChainMenus;
import com.hbm.inventory.gui.SafeMenuScreens;
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
// bus = Bus.MOD required: RegisterMenuScreensEvent implements IModBusEvent and only fires on the mod
// bus - @EventBusSubscriber's bus() defaults to Bus.GAME and does not auto-detect IModBusEvent
// (confirmed against real NeoForge 1.21.1 source and FancyModLoader's EventBusSubscriber javadoc).
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class OilChainClientRegistry {

    private OilChainClientRegistry() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        SafeMenuScreens.bind(event, OilChainMenus.MACHINE_OIL_WELL, MachineOilWellScreen::new);
        SafeMenuScreens.bind(event, OilChainMenus.MACHINE_REFINERY, MachineRefineryScreen::new);
    }
}
