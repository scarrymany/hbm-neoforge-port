package com.hbm.items.armor;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Ported from CE's {@code com.hbm.items.armor.IDamageHandler}. CE dispatched this from
 * {@code ModEventHandler#onEntityHurt(LivingHurtEvent)}; the confirmed real 1.21.1 replacement
 * dispatch point is {@link LivingDamageEvent.Pre}, fired just before HP is actually reduced - see
 * {@code docs/phase3/armor_equippable_framework.md}'s Key design decision #2 ("the IDamageHandler/
 * ItemArmorMod.modDamage/ArmorFSB.handleHurt dispatch point"). Implementing armor pieces adjust the
 * final damage amount via {@link LivingDamageEvent.Pre}'s own accessors (mirroring
 * {@code ArmorNo9.handleDamage}'s CE shape: a flat damage-threshold reduction) exactly the way CE's
 * {@code event.setAmount(event.getAmount() - x)} did. Dispatched centrally by
 * {@code com.hbm.handler.ArmorDamageHandler} - do not use {@code instanceof} checks anywhere else.
 */
public interface IDamageHandler {

    void handleDamage(LivingDamageEvent.Pre event, ItemStack stack);
}
