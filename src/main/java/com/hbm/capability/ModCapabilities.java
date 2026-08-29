package com.hbm.capability;

import com.hbm.api.energymk2.IBatteryItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Registers NTM's item capability providers with NeoForge, replacing CE's
 * {@code AttachCapabilitiesEvent<ItemStack>} listeners ({@code NTMBatteryCapabilityHandler} and
 * {@code NTMFluidCapabilityHandler}).
 *
 * <p>CE attached a capability to any item implementing a marker interface as each ItemStack was
 * constructed. NeoForge's {@link RegisterCapabilitiesEvent#registerItem} instead wants an
 * up-front, explicit item list at registration time - it has no equivalent of "any item matching
 * this predicate, whenever one shows up". So instead of a per-stack listener, this class filters
 * the fully-populated item registry once and bulk-registers the matching items, which is the
 * standard NeoForge idiom for this exact situation (an interface-implementing-item capability with
 * no fixed item list of its own).
 *
 * <p>This class does not subscribe itself to any event bus - wire it in by adding a mod-bus
 * listener for {@link RegisterCapabilitiesEvent} that calls {@link #register} (see this area's
 * integration notes for the exact call site).
 */
public final class ModCapabilities {

    private ModCapabilities() {}

    public static void register(RegisterCapabilitiesEvent event) {
        NTMFluidCapabilityHandler.initialize();

        List<ItemLike> batteryItems = BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof IBatteryItem)
                .collect(Collectors.toList());
        if (!batteryItems.isEmpty()) {
            event.registerItem(
                    Capabilities.EnergyStorage.ITEM,
                    (stack, context) -> new NTMBatteryEnergyWrapper(stack),
                    batteryItems.toArray(new ItemLike[0])
            );
        }

        List<ItemLike> fluidContainerItems = BuiltInRegistries.ITEM.stream()
                .filter(NTMFluidCapabilityHandler::isNtmFluidContainer)
                .collect(Collectors.toList());
        if (!fluidContainerItems.isEmpty()) {
            event.registerItem(
                    Capabilities.FluidHandler.ITEM,
                    (stack, context) -> new NTMFluidContainerWrapper(stack),
                    fluidContainerItems.toArray(new ItemLike[0])
            );
        }
    }
}
