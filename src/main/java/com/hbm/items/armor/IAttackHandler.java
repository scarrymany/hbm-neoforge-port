package com.hbm.items.armor;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Ported from CE's {@code com.hbm.items.armor.IAttackHandler}. CE's {@code LivingAttackEvent} has
 * <b>no</b> NeoForge 1.21.1 successor of that name - confirmed by a source search of the real
 * {@code neoforged/NeoForge} repository (zero matches for {@code LivingAttackEvent} anywhere in the
 * org; the class was removed and its "cancel this attack before any damage math runs" role folded
 * into {@link LivingIncomingDamageEvent}, confirmed by that event's real accessors
 * {@code getEntity()}/{@code getSource()}/{@code getOriginalAmount()}/{@code setCanceled(boolean)}
 * matching CE's {@code LivingAttackEvent} 1:1). Dispatched centrally by
 * {@code com.hbm.handler.ArmorDamageHandler}, mirroring CE's
 * {@code ModEventHandler#onEntityAttacked(LivingAttackEvent)}.
 */
public interface IAttackHandler {

    void handleAttack(LivingIncomingDamageEvent event, ItemStack armor);
}
