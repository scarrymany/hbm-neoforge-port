package com.hbm.items.special;

import com.hbm.config.WeaponConfig;
import com.hbm.entity.effect.EntityQuasar;
import com.hbm.items.ItemBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
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
 * The dropped-item {@link EntityQuasar} spawn is now wired, per
 * docs/phase4/entities_vortex_gravity_wells.md's Headline finding 2 (this exact call site was the
 * report's own live, already-committed "this package's other real consumer" find) - CE's real gate is
 * {@code entityItem.onGround} only (unlike {@link ItemDrop}'s sibling gate, which also fires on a
 * burning item), and the spawned quasar's fixed size is CE's real {@code 5F}.
 * <p>
 * Still not ported: {@code ContaminationUtil.applyDigammaData} (CE's per-tick player contamination
 * accumulator, which would otherwise run from an {@code inventoryTick} override) -
 * {@code com.hbm.util.ContaminationUtil} has not been ported by any Phase 1 area yet. CE's own hazard
 * table binds no static entry for this item at all (verified against
 * {@code upstream/hbm-ce/.../hazard/HazardRegistry.java}: no {@code particle_digamma} call exists
 * there) - its radiation entirely comes from the deferred {@code ContaminationUtil} call, not
 * {@code HazardSystem.register(...)}, so no hazard binding is added for it here either.
 */
public class ItemDigamma extends ItemBase {

    private final int digamma;

    public ItemDigamma(Properties properties, int digamma) {
        super(properties);
        this.digamma = digamma;
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
