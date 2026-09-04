package com.hbm.items.weapon.sedna.impl;

import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Exact CE {@code ItemGunDrill} harvest hooks ({@code :36-53}): always a correct tool,
 * destroy speed 50. Fluid/battery fill and {@code ICustomizable} stay skipped.
 */
public class ItemGunDrill extends ItemGunBaseNT {

    public ItemGunDrill(Item.Properties properties, WeaponQuality quality, GunConfig... cfg) {
        super(properties, quality, cfg);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return 50.0F;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return true;
    }
}
