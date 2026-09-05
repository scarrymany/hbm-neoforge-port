package com.hbm.inventory.gui.bomb;

import com.hbm.inventory.container.bomb.NukeCasingMenus;
import com.hbm.inventory.gui.SafeMenuScreens;
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
        SafeMenuScreens.bind(event, NukeCasingMenus.NUKE_BOY, NukeBoyScreen::new);
        SafeMenuScreens.bind(event, NukeCasingMenus.NUKE_GADGET, NukeGadgetScreen::new);
        SafeMenuScreens.bind(event, NukeCasingMenus.NUKE_MAN, NukeManScreen::new);
        SafeMenuScreens.bind(event, NukeCasingMenus.NUKE_MIKE, NukeMikeScreen::new);
        SafeMenuScreens.bind(event, NukeCasingMenus.NUKE_TSAR, NukeTsarScreen::new);
        SafeMenuScreens.bind(event, NukeCasingMenus.NUKE_N2, NukeN2Screen::new);
        SafeMenuScreens.bind(event, NukeCasingMenus.NUKE_PROTOTYPE, NukePrototypeScreen::new);
        SafeMenuScreens.bind(event, NukeCasingMenus.NUKE_FLEIJA, NukeFleijaScreen::new);
        SafeMenuScreens.bind(event, NukeCasingMenus.NUKE_BALEFIRE, NukeBalefireScreen::new);
        SafeMenuScreens.bind(event, NukeCasingMenus.NUKE_SOLINIUM, NukeSoliniumScreen::new);
        SafeMenuScreens.bind(event, NukeCasingMenus.NUKE_CUSTOM, NukeCustomScreen::new);
    }
}
