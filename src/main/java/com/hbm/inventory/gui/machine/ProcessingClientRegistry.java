package com.hbm.inventory.gui.machine;

import com.hbm.inventory.container.machine.ProcessingMenus;
import com.hbm.inventory.gui.SafeMenuScreens;
import com.hbm.main.MainRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client-side {@link net.minecraft.world.inventory.MenuType}-to-{@code Screen} binding for the
 * shredder/assembler/crystallizer/mixer family - see {@code PowerGenClientRegistry}'s own javadoc
 * for why this is a separate {@code @EventBusSubscriber} rather than an edit to
 * {@code ClientModRegistry#registerScreens} (same multi-area-in-one-wave race that class avoids).
 */
// bus = Bus.MOD required: RegisterMenuScreensEvent implements IModBusEvent and only fires on the mod
// bus - @EventBusSubscriber's bus() defaults to Bus.GAME and does not auto-detect IModBusEvent
// (confirmed against real NeoForge 1.21.1 source and FancyModLoader's EventBusSubscriber javadoc).
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ProcessingClientRegistry {

    private ProcessingClientRegistry() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        SafeMenuScreens.bind(event, ProcessingMenus.MACHINE_SHREDDER, MachineShredderScreen::new);
        SafeMenuScreens.bind(event, ProcessingMenus.MACHINE_ASSEMBLER, MachineAssemblyMachineScreen::new);
        SafeMenuScreens.bind(event, ProcessingMenus.MACHINE_CRYSTALLIZER, MachineCrystallizerScreen::new);
        SafeMenuScreens.bind(event, ProcessingMenus.MACHINE_MIXER, MachineMixerScreen::new);
    }
}
