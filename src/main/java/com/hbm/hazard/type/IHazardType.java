package com.hbm.hazard.type;

import com.hbm.config.RadiationConfig;
import com.hbm.hazard.modifier.IHazardModifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

/**
 * A single hazard effect (radiation, heat, blindness, ...): how it affects a holder, how it affects a dropped item,
 * and what it contributes to an item's tooltip.
 */
public interface IHazardType {

    int hazardRate = RadiationConfig.HAZARD_RATE.get();

    /**
     * Does the thing. Called by {@link com.hbm.hazard.HazardEntry#applyHazard}.
     *
     * @param target the holder
     * @param level  the final level after calculating all the modifiers
     */
    void onUpdate(LivingEntity target, double level, ItemStack stack);

    /**
     * Updates the hazard for dropped items. Used for things like explosive and hydroactive items.
     */
    void updateEntity(ItemEntity item, double level);

    /**
     * Adds item tooltip info.
     *
     * @param level     the base level, mods are passed separately
     * @param modifiers the modifier chain to evaluate the level against, if needed
     */
    @OnlyIn(Dist.CLIENT)
    void addHazardInformation(Player player, List<Component> list, double level, ItemStack stack, List<IHazardModifier> modifiers);

    @FunctionalInterface
    interface HazardInfoConsumer {
        void accept(Player player, List<Component> list, double level, ItemStack stack, List<IHazardModifier> modifiers);
    }
}
