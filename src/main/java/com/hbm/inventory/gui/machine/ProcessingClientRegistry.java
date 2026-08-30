package com.hbm.inventory.gui.machine;

import com.hbm.inventory.container.machine.ProcessingMenus;
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
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT)
public final class ProcessingClientRegistry {

    private ProcessingClientRegistry() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ProcessingMenus.MACHINE_SHREDDER.get(), MachineShredderScreen::new);
        event.register(ProcessingMenus.MACHINE_ASSEMBLER.get(), MachineAssemblyMachineScreen::new);
        event.register(ProcessingMenus.MACHINE_CRYSTALLIZER.get(), MachineCrystallizerScreen::new);
        event.register(ProcessingMenus.MACHINE_MIXER.get(), MachineMixerScreen::new);
    }
}
