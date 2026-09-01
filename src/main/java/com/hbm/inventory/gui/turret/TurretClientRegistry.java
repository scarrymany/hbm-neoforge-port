package com.hbm.inventory.gui.turret;

import com.hbm.inventory.container.turret.TurretMenus;
import com.hbm.inventory.gui.SafeMenuScreens;
import com.hbm.main.MainRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client-side {@link net.minecraft.world.inventory.MenuType}-to-{@code Screen} binding for this
 * turret package, following the exact pattern {@code com.hbm.inventory.gui.machine.PowerGenClientRegistry}
 * documents (a new per-package class instead of editing {@code com.hbm.main.ClientModRegistry}
 * directly, to avoid racing every other Phase 3 area doing the same in this wave).
 */
// bus = Bus.MOD required: RegisterMenuScreensEvent implements IModBusEvent and only fires on the mod
// bus - @EventBusSubscriber's bus() defaults to Bus.GAME and does not auto-detect IModBusEvent
// (confirmed against real NeoForge 1.21.1 source and FancyModLoader's EventBusSubscriber javadoc).
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class TurretClientRegistry {

    private TurretClientRegistry() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        SafeMenuScreens.bind(event, TurretMenus.TURRET, (id, inv, title) -> {
            if (id.be instanceof com.hbm.blockentity.turret.TurretArtyBlockEntity
                    || id.be instanceof com.hbm.blockentity.turret.TurretHIMARSBlockEntity) {
                return new TurretArtilleryScreen(id, inv, title);
            }
            return new TurretScreen(id, inv, title);
        });
    }
}
