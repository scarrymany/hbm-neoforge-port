package com.hbm.capability;

import com.hbm.blockentity.turret.TurretBlockEntities;
import com.hbm.main.MainRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * {@code registerBlockEntity} capability wiring for this turret package's 11 in-scope block
 * entities, per {@link com.hbm.blockentity.MachineBaseBlockEntity}'s own javadoc pattern - a
 * separate {@code @EventBusSubscriber} class rather than adding entries to
 * {@code com.hbm.capability.ModCapabilities} directly, matching the exact same zero-shared-file-edit
 * reasoning {@code PowerGenCapabilities}/{@code PowerGenClientRegistry} document for their own areas.
 * <p>
 * Every turret exposes {@code EnergyStorage} (all 11 implement {@link
 * com.hbm.api.energymk2.IEnergyReceiverMK2} via {@link com.hbm.blockentity.turret.TurretBaseBlockEntity}) and
 * an item handler (the 11-slot ammo/battery/chip inventory). Fritz additionally exposes
 * {@code FluidHandler} for its diesel fuel tank - the one turret overriding
 * {@code getFluidHandlerCapability}/{@code hasFluidHandlerCapability} directly (see
 * {@code TurretFritzBlockEntity}'s own javadoc for why the constructor-flag route doesn't apply to
 * it).
 */
@EventBusSubscriber(modid = MainRegistry.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class TurretCapabilities {

    private TurretCapabilities() {
    }

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        registerEnergyAndItems(event, TurretBlockEntities.SENTRY.get());
        registerEnergyAndItems(event, TurretBlockEntities.SENTRY_DAMAGED.get());
        registerEnergyAndItems(event, TurretBlockEntities.CHEKHOV.get());
        registerEnergyAndItems(event, TurretBlockEntities.FRIENDLY.get());
        registerEnergyAndItems(event, TurretBlockEntities.RICHARD.get());
        registerEnergyAndItems(event, TurretBlockEntities.JEREMY.get());
        registerEnergyAndItems(event, TurretBlockEntities.HOWARD.get());
        registerEnergyAndItems(event, TurretBlockEntities.HOWARD_DAMAGED.get());
        registerEnergyAndItems(event, TurretBlockEntities.MAXWELL.get());
        registerEnergyAndItems(event, TurretBlockEntities.TAUON.get());

        registerEnergyAndItems(event, TurretBlockEntities.FRITZ.get());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TurretBlockEntities.FRITZ.get(), (be, side) -> be.getFluidHandlerCapability(side));

        registerEnergyAndItems(event, TurretBlockEntities.ARTY.get());
        registerEnergyAndItems(event, TurretBlockEntities.HIMARS.get());
    }

    private static <T extends com.hbm.blockentity.turret.TurretBaseBlockEntity> void registerEnergyAndItems(
            RegisterCapabilitiesEvent event, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, (be, side) -> be.getEnergyStorageCapability(side));
    }
}
