package com.hbm.capability;

import com.hbm.config.GeneralConfig;
import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.main.MainRegistry;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Bridges NTM's own fluid catalog ({@link FluidType}) with the vanilla {@link Fluid} registry, and
 * tracks which items are NTM fluid containers so {@link ModCapabilities} knows which items to
 * attach the item fluid-handler capability to.
 *
 * <p>Ported from CE's {@code NTMFluidCapabilityHandler}, minus its {@code AttachCapabilitiesEvent}
 * listener: NeoForge's item-capability registration model registers a capability provider per
 * item up front ({@link ModCapabilities#register}) rather than attaching one to every ItemStack as
 * it is constructed, so the listener itself no longer has a place to live. The static
 * fluid/container lookup tables it built are still needed by {@link NTMFluidContainerWrapper} and
 * {@link NTMFluidHandlerWrapper} and are kept here under the original class name.
 *
 * <p>CE's {@code isLeadSafeForgeContainer} keyed its whitelist on {@code "registryname:meta"},
 * since 1.12.2 items had numeric metadata variants. That concept does not exist in 1.21 (separate
 * items replace metadata variants entirely), so the whitelist here is keyed on the plain item id
 * instead; whoever ports {@code GeneralConfig.leadSafeForgeContainerWhitelist} needs to drop the
 * {@code ":meta"} suffix from its entries.
 */
public final class NTMFluidCapabilityHandler {

    private static final Set<Item> NTM_CONTAINERS = new ObjectOpenHashSet<>();
    private static final Set<Item> NTM_FULL_CONTAINERS = new ObjectOpenHashSet<>();
    private static final Set<Item> NTM_EMPTY_CONTAINERS = new ObjectOpenHashSet<>();
    private static final Object2ObjectOpenHashMap<Fluid, FluidType> FLUID_TO_NTM_MAP = new Object2ObjectOpenHashMap<>(256);
    private static boolean initialized = false;

    private NTMFluidCapabilityHandler() {}

    /**
     * Builds the fluid and container lookup tables. Idempotent, and safe to call from
     * {@link ModCapabilities#register} every time that event fires. Must run after both the
     * NTM fluid catalog ({@link Fluids}) and {@link FluidContainerRegistry} are populated.
     */
    public static void initialize() {
        if (initialized) return;
        initialized = true;

        for (FluidType type : Fluids.getAll()) {
            if (type == null || type == Fluids.NONE || type.getName() == null) continue;

            Fluid vanillaFluid = type.getFF();
            if (vanillaFluid != null) {
                FLUID_TO_NTM_MAP.put(vanillaFluid, type);
            } else {
                MainRegistry.logger.warn("Could not find matching Fluid for FluidType {}", type.getName());
            }
        }

        for (FluidContainerRegistry.FluidContainer container : FluidContainerRegistry.allContainers) {
            Item full = container.fullContainer().getItem();
            NTM_CONTAINERS.add(full);
            NTM_FULL_CONTAINERS.add(full);
            if (container.emptyContainer() != null && !container.emptyContainer().isEmpty()) {
                Item empty = container.emptyContainer().getItem();
                NTM_CONTAINERS.add(empty);
                NTM_EMPTY_CONTAINERS.add(empty);
            }
        }

        MainRegistry.logger.info("NTMFluidCapabilityHandler init: mapped {} fluids, tracking {} container items.",
                FLUID_TO_NTM_MAP.size(), NTM_CONTAINERS.size());
    }

    @Nullable
    public static FluidType getFluidType(Fluid vanillaFluid) {
        return FLUID_TO_NTM_MAP.get(vanillaFluid);
    }

    public static boolean canForgeContainerStoreFluid(ItemStack stack, @Nullable FluidType type) {
        return type != null && (!type.needsLeadContainer() || isLeadSafeForgeContainer(stack));
    }

    public static boolean isLeadSafeForgeContainer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && GeneralConfig.leadSafeForgeContainerWhitelist.contains(id.toString());
    }

    public static boolean isNtmFluidContainer(Item item) {
        return NTM_CONTAINERS.contains(item);
    }

    /**
     * @return true if the item ever appears as a full container.
     */
    public static boolean isFullNtmFluidContainer(Item item) {
        return NTM_FULL_CONTAINERS.contains(item);
    }

    /**
     * @return true if the item ever appears as an empty container.
     */
    public static boolean isEmptyNtmFluidContainer(Item item) {
        return NTM_EMPTY_CONTAINERS.contains(item);
    }
}
