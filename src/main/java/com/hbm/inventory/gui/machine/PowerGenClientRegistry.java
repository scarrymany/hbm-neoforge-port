package com.hbm.inventory.gui.machine;

import com.hbm.inventory.container.machine.PowerGenMenus;
import com.hbm.inventory.gui.SafeMenuScreens;
import com.hbm.main.MainRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client-side {@link net.minecraft.world.inventory.MenuType}-to-{@code Screen} binding for this
 * power-generation package's six GUI-bearing machines. A separate {@code @EventBusSubscriber} class
 * rather than adding lines to {@code com.hbm.main.ClientModRegistry#registerScreens} directly: that
 * method's own javadoc invites exactly that kind of addition, but with many Phase 2 machine areas
 * landing in the same wave it would race the same way editing {@code ModBlocks}/{@code ModItems}
 * directly would - annotation-driven {@code @EventBusSubscriber} static listeners (the same pattern
 * already used by {@code HbmKeybinds}/{@code CommonEvents}/{@code ModDataGenerators} elsewhere in
 * this port) let every area register its own screens from its own file, with zero shared-file edits
 * and zero merge risk.
 */
// bus = Bus.MOD required: RegisterMenuScreensEvent implements IModBusEvent and only fires on the mod
// bus - @EventBusSubscriber's bus() defaults to Bus.GAME and does not auto-detect IModBusEvent
// (confirmed against real NeoForge 1.21.1 source and FancyModLoader's EventBusSubscriber javadoc).
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class PowerGenClientRegistry {

    private PowerGenClientRegistry() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        SafeMenuScreens.bind(event, PowerGenMenus.MACHINE_RTG, MachineRTGScreen::new);
        SafeMenuScreens.bind(event, PowerGenMenus.MACHINE_DIESEL, MachineDieselScreen::new);
        SafeMenuScreens.bind(event, PowerGenMenus.COMBUSTION_ENGINE, MachineCombustionEngineScreen::new);
        SafeMenuScreens.bind(event, PowerGenMenus.MACHINE_TURBINE, MachineTurbineScreen::new);
        SafeMenuScreens.bind(event, PowerGenMenus.MACHINE_LARGE_TURBINE, MachineLargeTurbineScreen::new);
        SafeMenuScreens.bind(event, PowerGenMenus.MACHINE_TURBINE_GAS, MachineTurbineGasScreen::new);
    }
}
