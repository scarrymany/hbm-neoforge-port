package com.hbm.hazard.transformer;

import com.hbm.hazard.HazardEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of fluid registry name -&gt; hazard entries; scales entries by tank contents for items exposing the
 * NeoForge item fluid-handler capability.
 * <p>
 * <b>Verification note:</b> {@link Capabilities.FluidHandler#ITEM} and the {@code stack.getCapability(...)} query
 * shape follow the well-established NeoForge 21.1 capabilities convention, but no live usage of the item fluid
 * capability was found in the Neo Edition reference at the time of writing. Confirm both against a working example
 * before relying on this class in production.
 */
public class HazardTransformerForgeFluid implements IHazardTransformer {

    // <Fluid's registry name, hazard entries>
    public static final Map<ResourceLocation, List<HazardEntry>> FLUID_HAZARDS = new HashMap<>();

    @Override
    public void transformPre(final ItemStack stack, final List<HazardEntry> entries) {
    }

    @Override
    public void transformPost(final ItemStack stack, final List<HazardEntry> entries) {
        if (FLUID_HAZARDS.isEmpty()) return;

        final IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) return;

        for (int tank = 0; tank < handler.getTanks(); tank++) {
            final FluidStack content = handler.getFluidInTank(tank);
            if (content.isEmpty()) continue;

            final ResourceLocation fluidName = BuiltInRegistries.FLUID.getKey(content.getFluid());
            final List<HazardEntry> hazardEntries = FLUID_HAZARDS.get(fluidName);
            if (hazardEntries == null) continue;

            final double modifier = content.getAmount() / 1000.0;
            for (final HazardEntry entry : hazardEntries) {
                entries.add(entry.clone(modifier));
            }
        }
    }
}
