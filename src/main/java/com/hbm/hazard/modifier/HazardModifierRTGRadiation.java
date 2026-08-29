package com.hbm.hazard.modifier;

import com.hbm.items.machine.ItemRTGPellet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Same depletion curve as {@link HazardModifierFuelRadiation}, specialized for {@link ItemRTGPellet}, which exposes
 * its own depletion fraction instead of relying on vanilla damage values.
 */
public class HazardModifierRTGRadiation implements IHazardModifier {

    private final double target;

    public HazardModifierRTGRadiation(final double target) {
        this.target = target;
    }

    @Override
    public double modify(final ItemStack stack, final LivingEntity holder, double level) {
        if (stack.getItem() instanceof ItemRTGPellet fuel) {
            final double depletion = fuel.getDurabilityForDisplay(stack);
            level = level + (this.target - level) * depletion;
        }
        return level;
    }
}
