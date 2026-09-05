package com.hbm.items.special;

import com.hbm.config.WeaponConfig;
import com.hbm.entity.effect.EntityQuasar;
import com.hbm.items.ItemBase;
import com.hbm.util.ContaminationUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Port of CE's {@code ItemDigamma} ({@code particle_digamma}). {@code digamma} is CE's per-instance
 * half-life parameter in ticks (note CE's own comment: for this class it means "ticks until half
 * life", the inverse of how the superclass' generic digamma-hazard interpretation reads a flat
 * value).
 * <p>
 * Exact CE {@code :29-34}: held {@code onUpdate} applies
 * {@link ContaminationUtil#applyDigammaData}{@code (player, 1.0 / digamma)} — CE hazard table has
 * no {@code particle_digamma} row; tick is the only holder dose. Dropped-item {@link EntityQuasar}
 * spawn is CE {@code :45-64} ({@code onGround} + {@code !isRemote}, size {@code 5F}).
 */
public class ItemDigamma extends ItemBase {

    private final int digamma;

    public ItemDigamma(Properties properties, int digamma) {
        super(properties);
        this.digamma = digamma;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        // CE ItemDigamma.java:29-34 — no isRemote gate
        if (entity instanceof Player player) {
            ContaminationUtil.applyDigammaData(player, 1.0 / digamma);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Half-life (particle): 1.67*10^34 a").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Half-life (holder): " + (digamma / 20.0) + "s").withStyle(ChatFormatting.RED));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("[Dangerous Drop]").withStyle(ChatFormatting.RED));
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        // CE: the whole onGround branch (including the entity discard + `return true`) is nested
        // under `!world.isRemote` too - unlike ItemDrop's sibling gate, which always discards once
        // landed regardless of side. Matched exactly: on the client this keeps returning false.
        Level level = entity.level();
        if (entity.onGround() && !level.isClientSide()) {
            if (WeaponConfig.DROP_SINGULARITY.get()) {
                EntityQuasar quasar = new EntityQuasar(level, 5F);
                quasar.setPos(entity.getX(), entity.getY(), entity.getZ());
                level.addFreshEntity(quasar);
            }

            entity.discard();
            return true;
        }

        return false;
    }
}
