package com.hbm.items.special;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Port of CE's {@code ItemPolaroid} ({@code polaroid}): a trivial Easter-egg item that grants a
 * short Resistance buff to a low-health holder, plus flavor tooltip text.
 * <p>
 * Not ported: the 18-way {@code switch} on CE's static {@code MainRegistry.polaroidID} (a
 * once-per-world-load random Easter-egg roll that also alters other items' tooltips/glint elsewhere
 * in CE). No equivalent counter exists in the port yet - that roll mechanism is a small, self-
 * contained piece of shared state with no other Phase 1 dependency, so it is deliberately left as an
 * open follow-up here rather than guessed at (there is no "first" flavor text to fall back to
 * faithfully; CE's own {@code default} case is silence).
 */
public class ItemPolaroid extends Item {

    private static final int RESISTANCE_DURATION_TICKS = 10;
    private static final int RESISTANCE_AMPLIFIER = 2;
    private static final float LOW_HEALTH_THRESHOLD = 10F;

    public ItemPolaroid(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (entity instanceof Player player && player.getHealth() < LOW_HEALTH_THRESHOLD) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, RESISTANCE_DURATION_TICKS, RESISTANCE_AMPLIFIER));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Fate chosen"));
        tooltip.add(Component.empty());
    }
}
