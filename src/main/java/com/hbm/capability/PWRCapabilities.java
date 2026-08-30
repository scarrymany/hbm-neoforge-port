package com.hbm.capability;

import com.hbm.blockentity.machine.PWRBlockEntities;
import com.hbm.main.MainRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * {@code registerBlockEntity} capability wiring for this PWR/breeding-reactor family's block
 * entities - see {@code PowerGenCapabilities}' own javadoc for why this is a standalone
 * {@code @EventBusSubscriber} rather than an edit to {@code com.hbm.capability.ModCapabilities}
 * (item-capability-only today, no block-entity precedent to extend safely without racing every other
 * Phase 2 area landing block-entity capabilities in the same wave).
 *
 * <p>{@link PWRBlockEntities#PWR_PROXY}'s fluid-handler lambda calls
 * {@link com.hbm.blockentity.machine.PWRProxyBlockEntity#getFluidHandlerCapability} directly (a plain
 * method on that class, not {@code MachineBaseBlockEntity}'s accessor - the proxy has no inventory
 * and extends {@code LoadedBaseBlockEntity} directly, see that class's own javadoc) rather than the
 * {@code (be, side) -> be.getFluidHandlerCapability(side)} shape used for every
 * {@code MachineBaseBlockEntity} subclass elsewhere - same call shape, different receiver type.
 */
@EventBusSubscriber(modid = MainRegistry.MODID)
public final class PWRCapabilities {

    private PWRCapabilities() {
    }

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        // Controller: fresh/hot fuel inventory + the two coolant tanks. No HE capability - the PWR
        // is a heat/steam source only (see PWRControllerBlockEntity's own javadoc / this package's
        // research report "HE energy is genuinely out of scope for the PWR itself").
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PWRBlockEntities.PWR_CONTROLLER.get(), (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, PWRBlockEntities.PWR_CONTROLLER.get(), (be, side) -> be.getFluidHandlerCapability(side));

        // Structural proxy: forwards the fluid-handler capability to the cached core when IO_ENABLED
        // (see PWRProxyBlockEntity's own javadoc); no item/energy capability of any kind.
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, PWRBlockEntities.PWR_PROXY.get(), (be, side) -> be.getFluidHandlerCapability(side));

        // Breeding reactor: 2-slot inventory only, no fluid/energy.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PWRBlockEntities.REACTOR_BREEDING.get(), (be, side) -> be.getItemHandlerCapability(side));
    }
}
