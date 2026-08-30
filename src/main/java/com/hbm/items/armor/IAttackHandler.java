package com.hbm.items.armor;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingAttackEvent;

/**
 * Ported unchanged from CE's {@code com.hbm.items.armor.IAttackHandler} - {@code LivingAttackEvent}
 * survives verbatim into NeoForge 1.21.1 (confirmed, see
 * {@code docs/phase3/armor_equippable_framework.md}'s Key design decision #2), so this interface's
 * shape needs no change beyond the package move from {@code net.minecraftforge.event.entity.living}
 * to {@code net.neoforged.neoforge.event.entity.living}. Dispatched centrally by
 * {@code com.hbm.handler.ArmorDamageHandler}, mirroring CE's
 * {@code ModEventHandler#onEntityAttacked(LivingAttackEvent)}.
 */
public interface IAttackHandler {

    void handleAttack(LivingAttackEvent event, ItemStack armor);
}
