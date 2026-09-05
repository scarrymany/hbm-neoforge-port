package com.hbm.items.armor;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import net.minecraft.world.item.ItemStack;

/**
 * Exact CE {@code com.hbm.items.armor.IPAMelee} click + orchestra.
 * {@code playAnim}/{@code setupFirstPerson}/{@code renderFirstPerson} stay skipped (FP VFX).
 */
public interface IPAMelee {

    void clickPrimary(ItemStack stack, ItemGunBaseNT.LambdaContext ctx);

    void clickSecondary(ItemStack stack, ItemGunBaseNT.LambdaContext ctx);

    default void orchestra(ItemStack stack, ItemGunBaseNT.LambdaContext ctx) {}
}
