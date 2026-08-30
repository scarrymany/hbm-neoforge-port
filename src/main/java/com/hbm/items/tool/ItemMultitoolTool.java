package com.hbm.items.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.tool.ItemMultitoolTool}, scoped to {@code multitool_dig}
 * and {@code multitool_silk} only (the two Phase-1-safe rungs of CE's sneak-right-click upgrade
 * ladder). {@code multitool_ext} and the rest of {@code ItemMultitoolPassive}'s AoE-lightning/
 * terrain-deletion/combat content are Phase 3 - see {@code docs/phase1/items_tool.md}'s explicit
 * recommendation to port the whole ladder together later rather than split it. Consequently the
 * sneak-right-click upgrade action itself is not ported here (its upgrade target, {@code
 * multitool_ext}, does not exist); each of these two items instead behaves like the terminal state
 * CE's ladder would have produced for it.
 *
 * <p>{@code multitool_silk} gets its always-on silk touch by temporarily applying the real vanilla
 * Silk Touch enchantment around the harvest (same mechanism {@link com.hbm.handler.ability.IToolHarvestAbility#SILK}
 * uses), rather than a hand-rolled "drop the block itself" shortcut - this way block-specific silk
 * touch drop rules (double slabs, ores with custom silk drops, etc.) stay correct automatically.
 */
public class ItemMultitoolTool extends TieredItem {

    private final boolean silkTouch;

    public ItemMultitoolTool(Properties properties, Tier tier, boolean silkTouch) {
        super(tier, properties);
        this.silkTouch = silkTouch;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return getTier().getSpeed();
    }

    @Override
    public boolean isCorrectToolForDrops(BlockState state) {
        return true;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        if (!silkTouch || level.isClientSide()) {
            return super.mineBlock(stack, level, state, pos, miningEntity);
        }

        Holder<Enchantment> silk = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH);
        EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.set(silk, 1));
        try {
            return super.mineBlock(stack, level, state, pos, miningEntity);
        } finally {
            EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.set(silk, 0));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("Breaks blocks extremely fast"));
        tooltipComponents.add(Component.literal(silkTouch ? "Ores will drop themselves via silk touch" : "Extra drops for ores"));
    }
}
