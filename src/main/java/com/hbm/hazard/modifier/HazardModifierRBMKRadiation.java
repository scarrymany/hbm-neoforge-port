package com.hbm.hazard.modifier;

import com.hbm.hazard.HazardRegistry;
import com.hbm.items.machine.ItemRBMKPellet;
import com.hbm.items.machine.ItemRBMKRod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * RBMK rod/pellet specific radiation curve driven by enrichment and xenon poisoning state.
 */
public class HazardModifierRBMKRadiation implements IHazardModifier {

    private final double target;
    private final boolean linear;

    public HazardModifierRBMKRadiation(final double target, final boolean linear) {
        this.target = target;
        this.linear = linear;
    }

    @Override
    public double modify(final ItemStack stack, final LivingEntity holder, double level) {

        if (stack.getItem() instanceof ItemRBMKRod) {
            // Due to short-lived fission products, radioactivity rises quicker than depletion when applicable
            final double depletion = linear ? 1D - ItemRBMKRod.getEnrichment(stack) : 1D - Math.pow(ItemRBMKRod.getEnrichment(stack), 2);
            final double xenon = ItemRBMKRod.getPoisonLevel(stack);

            level = level + (this.target - level) * depletion;
            level += HazardRegistry.xe135 * xenon;

        } else if (stack.getItem() instanceof ItemRBMKPellet) {

            level = level + (target - level) * ((ItemRBMKPellet.rectify(stack) % 5) / 4F);

            if (ItemRBMKPellet.hasXenon(stack)) {
                level += HazardRegistry.xe135 * HazardRegistry.nugget;
            }
        }

        return level;
    }
}
