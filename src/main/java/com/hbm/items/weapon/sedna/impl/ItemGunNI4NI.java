package com.hbm.items.weapon.sedna.impl;

import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.GunDataComponents;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.MainRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Exact CE {@code ItemGunNI4NI}: coin regen on inventory tick ({@code :31-54}) plus the
 * {@code coincount}/{@code coincharge} stack state the secondary throw reads.
 * Color {@code ICustomizable} command is unused anywhere in this port — skipped, no art.
 */
public class ItemGunNI4NI extends ItemGunBaseNT {

    public static final ResourceLocation UPGRADE_NICKEL =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "ni4ni_nickel");
    public static final ResourceLocation UPGRADE_DOUBLOONS =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "ni4ni_doubloons");

    public ItemGunNI4NI(Item.Properties properties, WeaponQuality quality, GunConfig... cfg) {
        super(properties, quality, cfg);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide()) return;

        int maxCoin = 4;
        if (XWeaponModManager.hasUpgrade(stack, 0, UPGRADE_NICKEL)) maxCoin += 2;
        if (XWeaponModManager.hasUpgrade(stack, 0, UPGRADE_DOUBLOONS)) maxCoin += 2;

        if (getCoinCount(stack) < maxCoin) {
            setCoinCharge(stack, getCoinCharge(stack) + 1);
            if (getCoinCharge(stack) >= 80) {
                setCoinCharge(stack, 0);
                int newCount = getCoinCount(stack) + 1;
                setCoinCount(stack, newCount);
                if (isSelected) {
                    level.playSound(null, entity.blockPosition(), HBMSoundHandler.techBoop.get(),
                            SoundSource.PLAYERS, 1.0F, 1F + newCount / (float) maxCoin);
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Now, don't get the wrong idea."));
        tooltip.add(Component.literal("I §cfucking hate §7this game."));
        tooltip.add(Component.literal("I didn't do this for you, I did it for sea."));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    public static int getCoinCount(ItemStack stack) {
        return stack.getOrDefault(GunDataComponents.COIN_COUNT.get(), 0);
    }

    public static void setCoinCount(ItemStack stack, int value) {
        stack.set(GunDataComponents.COIN_COUNT.get(), value);
    }

    public static int getCoinCharge(ItemStack stack) {
        return stack.getOrDefault(GunDataComponents.COIN_CHARGE.get(), 0);
    }

    public static void setCoinCharge(ItemStack stack, int value) {
        stack.set(GunDataComponents.COIN_CHARGE.get(), value);
    }
}
