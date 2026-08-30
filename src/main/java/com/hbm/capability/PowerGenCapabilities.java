package com.hbm.capability;

import com.hbm.blockentity.machine.PowerGenBlockEntities;
import com.hbm.main.MainRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * {@code registerBlockEntity} capability wiring for this power-generation family's block entities,
 * per {@link com.hbm.blockentity.MachineBaseBlockEntity}'s own javadoc ("every Phase 2 generator
 * block entity should register {@code Capabilities.EnergyStorage.BLOCK ->
 * new NTMEnergyCapabilityWrapper(this)}..."). A separate {@code @EventBusSubscriber} class rather
 * than adding entries to {@code com.hbm.capability.ModCapabilities#register} directly, for the same
 * zero-shared-file-edit reason {@link com.hbm.inventory.gui.machine.PowerGenClientRegistry} gives for
 * screens - many Phase 2 machine areas land block-entity capabilities in the same wave, and
 * {@code ModCapabilities} is item-capability-only today (see its own javadoc) with no block-entity
 * precedent to extend safely without racing every other area doing the same.
 * <p>
 * Item-handler registration is skipped for the six zero-inventory producers (mini RTG, steam engine,
 * industrial turbine, solar boiler, solar mirror - solar mirror has no capability of any kind) since
 * an empty {@code ItemStackHandler} capability has no external consumer; fluid/energy are registered
 * only where the block entity actually implements the corresponding marker interface, matching
 * {@link com.hbm.blockentity.MachineBaseBlockEntity#getFluidHandlerCapability}/
 * {@link com.hbm.blockentity.MachineBaseBlockEntity#getEnergyStorageCapability}'s own gating
 * contract (a mismatched flag throws at first use, not at registration time).
 */
@EventBusSubscriber(modid = MainRegistry.MODID)
public final class PowerGenCapabilities {

    private PowerGenCapabilities() {
    }

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        // RTG: 15-slot pellet inventory + energy.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PowerGenBlockEntities.MACHINE_RTG.get(), (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, PowerGenBlockEntities.MACHINE_RTG.get(), (be, side) -> be.getEnergyStorageCapability(side));

        // Mini/power RTG: energy only, no inventory.
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, PowerGenBlockEntities.MACHINE_MINI_RTG.get(), (be, side) -> be.getEnergyStorageCapability(side));

        // Steam engine: fluid + energy, no inventory.
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, PowerGenBlockEntities.STEAM_ENGINE.get(), (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, PowerGenBlockEntities.STEAM_ENGINE.get(), (be, side) -> be.getEnergyStorageCapability(side));

        // Diesel generator: battery slot + fluid + energy.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PowerGenBlockEntities.MACHINE_DIESEL.get(), (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, PowerGenBlockEntities.MACHINE_DIESEL.get(), (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, PowerGenBlockEntities.MACHINE_DIESEL.get(), (be, side) -> be.getEnergyStorageCapability(side));

        // Combustion engine: piston + battery slots + fluid + energy.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PowerGenBlockEntities.COMBUSTION_ENGINE.get(), (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, PowerGenBlockEntities.COMBUSTION_ENGINE.get(), (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, PowerGenBlockEntities.COMBUSTION_ENGINE.get(), (be, side) -> be.getEnergyStorageCapability(side));

        // Small turbine: battery slot + fluid + energy.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PowerGenBlockEntities.MACHINE_TURBINE.get(), (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, PowerGenBlockEntities.MACHINE_TURBINE.get(), (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, PowerGenBlockEntities.MACHINE_TURBINE.get(), (be, side) -> be.getEnergyStorageCapability(side));

        // Large turbine: battery slot + fluid + energy.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PowerGenBlockEntities.LARGE_TURBINE.get(), (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, PowerGenBlockEntities.LARGE_TURBINE.get(), (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, PowerGenBlockEntities.LARGE_TURBINE.get(), (be, side) -> be.getEnergyStorageCapability(side));

        // Industrial turbine: fluid + energy, no inventory.
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, PowerGenBlockEntities.INDUSTRIAL_TURBINE.get(), (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, PowerGenBlockEntities.INDUSTRIAL_TURBINE.get(), (be, side) -> be.getEnergyStorageCapability(side));

        // Gas turbine: battery slot + fluid + energy.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PowerGenBlockEntities.TURBINE_GAS.get(), (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, PowerGenBlockEntities.TURBINE_GAS.get(), (be, side) -> be.getFluidHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, PowerGenBlockEntities.TURBINE_GAS.get(), (be, side) -> be.getEnergyStorageCapability(side));

        // Solar boiler: fluid only, no inventory, no HE.
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, PowerGenBlockEntities.SOLAR_BOILER.get(), (be, side) -> be.getFluidHandlerCapability(side));

        // Solar mirror: no capabilities at all (no inventory, no fluid, no HE).
    }
}
