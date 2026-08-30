package com.hbm.inventory.gui.machine;

import com.hbm.inventory.container.machine.PowerGenMenus;
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
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT)
public final class PowerGenClientRegistry {

    private PowerGenClientRegistry() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(PowerGenMenus.MACHINE_RTG.get(), MachineRTGScreen::new);
        event.register(PowerGenMenus.MACHINE_DIESEL.get(), MachineDieselScreen::new);
        event.register(PowerGenMenus.COMBUSTION_ENGINE.get(), MachineCombustionEngineScreen::new);
        event.register(PowerGenMenus.MACHINE_TURBINE.get(), MachineTurbineScreen::new);
        event.register(PowerGenMenus.MACHINE_LARGE_TURBINE.get(), MachineLargeTurbineScreen::new);
        event.register(PowerGenMenus.MACHINE_TURBINE_GAS.get(), MachineTurbineGasScreen::new);
    }
}
