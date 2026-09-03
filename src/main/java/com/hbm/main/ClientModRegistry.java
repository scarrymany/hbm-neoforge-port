package com.hbm.main;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import com.hbm.inventory.gui.SafeMenuScreens;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import com.hbm.blockentity.machine.rbmk.RBMKBlockEntities;
import com.hbm.client.render.ClientEntityRenderers;
import com.hbm.client.render.blockentity.rbmk.RBMKAutoloaderPistonRenderer;
import com.hbm.client.render.blockentity.rbmk.RBMKConsoleHeatmapRenderer;
import com.hbm.client.render.blockentity.rbmk.RBMKControlRodRenderer;
import com.hbm.client.render.blockentity.rbmk.RBMKDisplayRenderer;
import com.hbm.client.render.blockentity.rbmk.RBMKFuelColumnRenderer;
import com.hbm.inventory.container.ModMenuTypes;
import com.hbm.inventory.gui.BatteryScreen;
import com.hbm.inventory.gui.CrateScreen;
import com.hbm.inventory.gui.FluidTankScreen;
import com.hbm.inventory.gui.LemegetonScreen;
import com.hbm.particle.HbmEffect;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

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
// bus = Bus.MOD required: both FMLClientSetupEvent and RegisterMenuScreensEvent implement
// IModBusEvent and only fire on the mod bus - @EventBusSubscriber's bus() defaults to Bus.GAME and
// does not auto-detect IModBusEvent (confirmed against real NeoForge 1.21.1 source and
// FancyModLoader's EventBusSubscriber javadoc). Without this, no Phase 2+ machine screen would ever
// actually bind to its MenuType.
@Mod(value = MainRegistry.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ClientModRegistry {

    public ClientModRegistry(ModContainer modContainer) {
        MainRegistry.logger.info("HBM CE NeoForge (fork by scarrymany) client bootstrap initializing");
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Phase 5: bulk safe-fallback + bespoke EntityRenderers pass for every one of this port's
            // custom EntityTypes - see com.hbm.client.render.ClientEntityRenderers's own class javadoc.
            ClientEntityRenderers.registerAll();

            // Phase 5 (reactor_and_explosion_visual_effects / c1-rbmk-renderers): the 4 RBMK reactor
            // BlockEntityRenderers, each backing 1-2 concrete BlockEntityTypes.
            BlockEntityRenderers.register(RBMKBlockEntities.CONTROL_MANUAL.get(), new RBMKControlRodRenderer.Provider());
            BlockEntityRenderers.register(RBMKBlockEntities.CONTROL_AUTO.get(), new RBMKControlRodRenderer.Provider());
            BlockEntityRenderers.register(RBMKBlockEntities.ROD.get(), new RBMKFuelColumnRenderer.Provider());
            BlockEntityRenderers.register(RBMKBlockEntities.ROD_REASIM.get(), new RBMKFuelColumnRenderer.Provider());
            BlockEntityRenderers.register(RBMKBlockEntities.CONSOLE.get(), new RBMKConsoleHeatmapRenderer.Provider());
            BlockEntityRenderers.register(RBMKBlockEntities.AUTOLOADER.get(), new RBMKAutoloaderPistonRenderer.Provider());
            BlockEntityRenderers.register(RBMKBlockEntities.DISPLAY.get(), new RBMKDisplayRenderer.Provider());

            // Phase 5 (particle_engine_and_generic_vfx): registers every HbmEffect constant's
            // client-only render handler - must run once, client-side only, before any
            // HbmEffectPacket is handled.
            HbmEffect.registerHandlers();
        });
    }

    /**
     * Client-side {@link net.minecraft.world.inventory.MenuType}-to-{@code Screen} binding, the
     * confirmed-real NeoForge counterpart to {@code com.hbm.inventory.container.ModMenuTypes}'
     * server/common-safe {@code DeferredRegister} - see Neo Edition's real
     * {@code CommonEvents.registerScreens} (cross-checked for API shape only) for the identical
     * {@code SafeMenuScreens.bind(event, SOME_MENU_TYPE, SomeScreen::new)} pattern every future Phase 2
     * machine Menu+Screen pair should add a line to here - the storage-machines package below is the
     * first concrete set (see {@code docs/phase2/machines_storage.md}).
     */
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        // Phase 2 storage-machines package (see docs/phase2/machines_storage.md) - first concrete
        // entries following this method's own original template comment.
        SafeMenuScreens.bind(event, ModMenuTypes.CRATE, CrateScreen::new);
        SafeMenuScreens.bind(event, ModMenuTypes.BATTERY, BatteryScreen::new);
        SafeMenuScreens.bind(event, ModMenuTypes.FLUID_TANK, FluidTankScreen::new);
        SafeMenuScreens.bind(event, ModMenuTypes.LEMEGETON, LemegetonScreen::new);
        SafeMenuScreens.bind(event, ModMenuTypes.TAPE_DRIVE, com.hbm.client.gui.TapeDriveScreen::new);
        SafeMenuScreens.bind(event, ModMenuTypes.CRANE_INSERTER, com.hbm.client.screen.CraneInserterScreen::new);
        SafeMenuScreens.bind(event, ModMenuTypes.CRANE_EXTRACTOR, com.hbm.client.screen.CraneExtractorScreen::new);
        SafeMenuScreens.bind(event, ModMenuTypes.CRANE_GRABBER, com.hbm.client.screen.CraneGrabberScreen::new);
        SafeMenuScreens.bind(event, ModMenuTypes.CRANE_BOXER, com.hbm.client.screen.CraneBoxerScreen::new);
        SafeMenuScreens.bind(event, ModMenuTypes.CRANE_UNBOXER, com.hbm.client.screen.CraneUnboxerScreen::new);
        SafeMenuScreens.bind(event, ModMenuTypes.CRANE_ROUTER, com.hbm.client.screen.CraneRouterScreen::new);
        SafeMenuScreens.bind(event, ModMenuTypes.DRONE_CRATE_PROVIDER, com.hbm.inventory.gui.DroneCrateProviderScreen::new);
        SafeMenuScreens.bind(event, ModMenuTypes.DRONE_CRATE_REQUESTER, com.hbm.inventory.gui.DroneCrateRequesterScreen::new);

        // RBMK column-block Screens
        SafeMenuScreens.bind(event, com.hbm.inventory.container.machine.rbmk.RBMKMenuTypes.OUTGASSER, com.hbm.client.gui.screens.rbmk.RBMKOutgasserScreen::new);
        SafeMenuScreens.bind(event, com.hbm.inventory.container.machine.rbmk.RBMKMenuTypes.STORAGE, com.hbm.client.gui.screens.rbmk.RBMKStorageScreen::new);
        SafeMenuScreens.bind(event, com.hbm.inventory.container.machine.rbmk.RBMKMenuTypes.HEATER, com.hbm.client.gui.screens.rbmk.RBMKHeaterScreen::new);
        SafeMenuScreens.bind(event, com.hbm.inventory.container.machine.rbmk.RBMKMenuTypes.BOILER, com.hbm.client.gui.screens.rbmk.RBMKBoilerScreen::new);
        SafeMenuScreens.bind(event, com.hbm.inventory.container.machine.rbmk.RBMKMenuTypes.ROD, com.hbm.client.gui.screens.rbmk.RBMKRodScreen::new);
        SafeMenuScreens.bind(event, com.hbm.inventory.container.machine.rbmk.RBMKMenuTypes.AUTOLOADER, com.hbm.client.gui.screens.rbmk.RBMKAutoloaderScreen::new);
        SafeMenuScreens.bind(event, com.hbm.inventory.container.machine.rbmk.RBMKMenuTypes.CONTROL, com.hbm.client.gui.screens.rbmk.RBMKControlScreen::new);
        SafeMenuScreens.bind(event, com.hbm.inventory.container.machine.rbmk.RBMKMenuTypes.CONTROL_AUTO, com.hbm.client.gui.screens.rbmk.RBMKControlAutoScreen::new);

        // Processing machines (Shredder / Assembler / Crystallizer / Mixer)
        SafeMenuScreens.bind(event, com.hbm.inventory.container.machine.ProcessingMenus.MACHINE_SHREDDER, com.hbm.client.gui.screens.machine.ShredderScreen::new);
        SafeMenuScreens.bind(event, com.hbm.inventory.container.machine.ProcessingMenus.MACHINE_ASSEMBLER, com.hbm.inventory.gui.machine.MachineAssemblyMachineScreen::new);
        SafeMenuScreens.bind(event, com.hbm.inventory.container.machine.ProcessingMenus.MACHINE_CRYSTALLIZER, com.hbm.client.gui.screens.machine.CrystallizerScreen::new);
        SafeMenuScreens.bind(event, com.hbm.inventory.container.machine.ProcessingMenus.MACHINE_MIXER, com.hbm.client.gui.screens.machine.MixerScreen::new);

        BombClientRegistry.registerScreens(event);
    }
}
