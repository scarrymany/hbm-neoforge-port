package com.hbm.main;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import com.hbm.inventory.container.ModMenuTypes;
import com.hbm.inventory.gui.BatteryScreen;
import com.hbm.inventory.gui.CrateScreen;
import com.hbm.inventory.gui.FluidTankScreen;

/**
 * Client-only bootstrap mod class, mirroring the Neo Edition reference's
 * {@code com.hbm.main.NuclearTechModClient}. A second {@code @Mod} class with the same
 * modid but {@code dist = Dist.CLIENT} is NeoForge's supported pattern for a class that is
 * only constructed on the client physical side; it is discovered by the same annotation
 * scan as {@link MainRegistry} (no explicit reference in {@code neoforge.mods.toml} or
 * elsewhere is needed - confirmed by the reference doing the same, and by this project's
 * toml having no per-class wiring for {@code MainRegistry} either).
 *
 * <p>Closes the Phase 0 gap tracked in {@code docs/phase0/STATUS.md}: no client-only
 * bootstrap existed yet for any future client-only setup (item/block renderer
 * registration, tooltip hooks, {@code RegisterColorHandlersEvent}, particle providers,
 * etc). {@code com.hbm.handler.HbmKeybinds} was checked as part of this gap-fill and does
 * <b>not</b> need to be wired from here: it already self-registers its
 * {@code RegisterKeyMappingsEvent}/input/tick handlers via its own
 * {@code @EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT)} annotation
 * (same self-registering pattern the Neo Edition reference uses for its own
 * {@code HbmKeybinds}), so no manual registration call is dangling.
 *
 * <p>The {@link #onClientSetup} handler below is intentionally empty - Phase 0 has no
 * client-only setup work of its own. Add work here as the first area that needs it lands
 * (see {@code upstream/neo-edition/src/main/java/com/hbm/main/NuclearTechModClient.java}
 * for the confirmed real-world shape of what eventually belongs in this method: render
 * layer assignment via {@code ItemBlockRenderTypes}, {@code ItemProperties.register}
 * calls, entity/block-entity renderer registration, etc).
 */
@Mod(value = MainRegistry.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT)
public class ClientModRegistry {

    public ClientModRegistry(ModContainer modContainer) {
        MainRegistry.logger.info("HBM's Nuclear Tech - Community Edition (NeoForge port) client bootstrap initializing");
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Ready for the first area that needs client-only setup work to add to:
            // item/block renderer registration, tooltip hooks, entity/block-entity
            // renderer registration, RegisterColorHandlersEvent-adjacent follow-up, etc.
            // See NuclearTechModClient.onClientSetup in the Neo Edition reference for the
            // confirmed real-world shape once work lands here.
        });
    }

    /**
     * Client-side {@link net.minecraft.world.inventory.MenuType}-to-{@code Screen} binding, the
     * confirmed-real NeoForge counterpart to {@code com.hbm.inventory.container.ModMenuTypes}'
     * server/common-safe {@code DeferredRegister} - see Neo Edition's real
     * {@code CommonEvents.registerScreens} (cross-checked for API shape only) for the identical
     * {@code event.register(SOME_MENU_TYPE.get(), SomeScreen::new)} pattern every future Phase 2
     * machine Menu+Screen pair should add a line to here - the storage-machines package below is the
     * first concrete set (see {@code docs/phase2/machines_storage.md}).
     */
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        // Phase 2 storage-machines package (see docs/phase2/machines_storage.md) - first concrete
        // entries following this method's own original template comment.
        event.register(ModMenuTypes.CRATE.get(), CrateScreen::new);
        event.register(ModMenuTypes.BATTERY.get(), BatteryScreen::new);
        event.register(ModMenuTypes.FLUID_TANK.get(), FluidTankScreen::new);
    }
}
