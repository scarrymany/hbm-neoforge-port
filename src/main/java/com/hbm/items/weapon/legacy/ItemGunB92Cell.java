package com.hbm.items.weapon.legacy;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Exact CE {@code GunB92Cell} ({@code gun_b92_ammo}). Siphon 1 charge/tick from {@code gun_b92}
 * while {@code energy < 25} — {@code GunB92Cell.java:24-40}. {@code weaponized_starblaster_cell}
 * drop-bomb is a different CE item, not registered — skip invent.
 */
public class ItemGunB92Cell extends Item {

    public ItemGunB92Cell(Properties properties) {
        super(properties);
    }

    /** Port of CE's {@code GunB92Cell.getFullCell()} - a cell pre-charged to the 25-point cap. */
    public static ItemStack getFullCell(ItemGunB92Cell item) {
        ItemStack stack = new ItemStack(item);
        setEnergy(stack, 25);
        return stack;
    }

    public static int getEnergy(ItemStack stack) {
        return stack.getOrDefault(LegacyWeaponDataComponents.ENERGY.get(), 0);
    }

    public static void setEnergy(ItemStack stack, int value) {
        stack.set(LegacyWeaponDataComponents.ENERGY.get(), value);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        // Exact CE GunB92Cell.java:24-40
        if (!(entity instanceof Player player) || getEnergy(stack) >= 25) return;
        for (ItemStack inv : player.getInventory().items) {
            if (inv.getItem() instanceof ItemGunB92) {
                int p = ItemGunB92.getEnergy(inv);
                if (p > 1) {
                    ItemGunB92.setEnergy(inv, p - 1);
                    setEnergy(stack, getEnergy(stack) + 1);
                    return;
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // Exact CE GunB92Cell.java:44-51
        tooltip.add(Component.literal("Draws energy from the B92, allowing you to"));
        tooltip.add(Component.literal("reload it an additional 25 times."));
        tooltip.add(Component.literal("The cell will permanently hold it's charge,"));
        tooltip.add(Component.literal("it is not meant to be used as a battery enhancement"));
        tooltip.add(Component.literal("for the B92, but rather as a bomb."));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("Charges: " + getEnergy(stack) + " / 25"));
    }
}
