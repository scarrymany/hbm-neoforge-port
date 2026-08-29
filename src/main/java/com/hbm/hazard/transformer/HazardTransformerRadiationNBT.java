package com.hbm.hazard.transformer;

import com.hbm.hazard.HazardComponents;
import com.hbm.hazard.HazardEntry;
import com.hbm.hazard.HazardRegistry;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Reads the {@link HazardComponents#BONUS_RADIATION} data component (formerly the raw {@code hfrHazRadiation} NBT
 * float) and injects a bonus RADIATION entry when present.
 */
public class HazardTransformerRadiationNBT implements IHazardTransformer {

    @Override
    public void transformPre(final ItemStack stack, final List<HazardEntry> entries) {
    }

    @Override
    public void transformPost(final ItemStack stack, final List<HazardEntry> entries) {
        final Float bonus = stack.get(HazardComponents.BONUS_RADIATION.get());
        if (bonus != null && bonus > 0F) {
            entries.add(new HazardEntry(HazardRegistry.RADIATION, bonus));
        }
    }
}
