package com.hbm.items.special;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of CE's {@code ItemConsumable}: base class for right-click/hit-triggered consumables
 * (syringes, med bags, gas mask filters). CE routed the actual effect (heal, cure, poison, ...) for
 * each concrete item through {@code ConsumableHandler.handleItemUse}/{@code handleHit}, keyed off
 * the stack's registry name. {@code com.hbm.handler.ConsumableHandler} lives outside this area's
 * package/file scope and has not been ported by any area yet, so {@link #use}/{@link #hurtEnemy}
 * are left as documented no-ops (matching the report's "not blocking registration" guidance) rather
 * than reimplementing that dispatch table here. The self-contained parts of CE's class - the glint
 * flag and the per-registry-name tooltip table - are ported faithfully since they carry no such
 * dependency.
 */
public class ItemConsumable extends Item {

    private static final Map<String, List<String>> TOOLTIP_LINES = new HashMap<>();

    static {
        TOOLTIP_LINES.put("syringe_antidote", Collections.singletonList("Removes all potion effects"));
        TOOLTIP_LINES.put("syringe_awesome", Collections.singletonList("Every good effect for 50 seconds"));
        TOOLTIP_LINES.put("syringe_metal_stimpak", Collections.singletonList("Heals 2.5 hearts"));
        TOOLTIP_LINES.put("syringe_metal_medx", Collections.singletonList("Resistance III for 4 minutes"));
        TOOLTIP_LINES.put("syringe_metal_psycho", Arrays.asList("Resistance I for 2 minutes", "Strength I for 2 minutes"));
        TOOLTIP_LINES.put("syringe_metal_super", Arrays.asList("Heals 25 hearts", "Slowness I for 10 seconds"));
        TOOLTIP_LINES.put("syringe_poison", Collections.singletonList("Deadly"));
        TOOLTIP_LINES.put("syringe_taint", Arrays.asList("Tainted I for 60 seconds", "Nausea I for 5 seconds", "Cloud damage + taint = ghoulified effect"));
        TOOLTIP_LINES.put("med_bag", Arrays.asList("Full heal, regardless of max health", "Removes negative effects"));
        TOOLTIP_LINES.put("gas_mask_filter_mono", Collections.singletonList("Repairs worn monoxide mask"));
        TOOLTIP_LINES.put("syringe_mkunicorn", Collections.singletonList("?"));
    }

    private final boolean hasEffect;

    public ItemConsumable(Properties properties) {
        this(properties, false);
    }

    public ItemConsumable(Properties properties, boolean hasEffect) {
        super(properties);
        this.hasEffect = hasEffect;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasEffect || super.isFoil(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Effect dispatch deferred - see class javadoc; ConsumableHandler is not ported yet.
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Effect dispatch deferred - see class javadoc; ConsumableHandler is not ported yet.
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (registryName.getNamespace().equals("hbm")) {
            List<String> lines = TOOLTIP_LINES.get(registryName.getPath());
            if (lines != null) {
                for (String line : lines) {
                    tooltip.add(Component.literal(line));
                }
            }
        }
    }
}
