package com.hbm.inventory.gui.bomb;

import com.hbm.inventory.container.bomb.NukeCasingMenus;
import com.hbm.main.MainRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client-side {@link net.minecraft.world.inventory.MenuType}-to-{@code Screen} binding for the 9
 * concrete nuke casings + {@code NukeCustom} (see {@code docs/phase3/bomb_blocks_and_detonators.md}
 * Section B). A separate {@code @EventBusSubscriber} class rather than editing
 * {@code com.hbm.main.ClientModRegistry} directly - see {@code PowerGenClientRegistry}'s own javadoc
 * for the exact same reasoning (many Phase 3 areas landing in the same wave).
 */
// bus = Bus.MOD required: RegisterMenuScreensEvent implements IModBusEvent and only fires on the mod
// bus - @EventBusSubscriber's bus() defaults to Bus.GAME and does not auto-detect IModBusEvent
// (confirmed against real NeoForge 1.21.1 source).
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class NukeCasingClientRegistry {

    private NukeCasingClientRegistry() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(NukeCasingMenus.NUKE_BOY.get(), NukeBoyScreen::new);
        event.register(NukeCasingMenus.NUKE_GADGET.get(), NukeGadgetScreen::new);
        event.register(NukeCasingMenus.NUKE_MAN.get(), NukeManScreen::new);
        event.register(NukeCasingMenus.NUKE_MIKE.get(), NukeMikeScreen::new);
        event.register(NukeCasingMenus.NUKE_TSAR.get(), NukeTsarScreen::new);
        event.register(NukeCasingMenus.NUKE_N2.get(), NukeN2Screen::new);
        event.register(NukeCasingMenus.NUKE_PROTOTYPE.get(), NukePrototypeScreen::new);
        event.register(NukeCasingMenus.NUKE_FLEIJA.get(), NukeFleijaScreen::new);
        event.register(NukeCasingMenus.NUKE_BALEFIRE.get(), NukeBalefireScreen::new);
        event.register(NukeCasingMenus.NUKE_CUSTOM.get(), NukeCustomScreen::new);
    }
}
