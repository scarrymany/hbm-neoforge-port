package com.hbm.items.tool;

import com.hbm.items.weapon.WeaponMeleeItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ItemSwordMeteorite extends ItemSwordAbility {

    public ItemSwordMeteorite(float damage, double movement, Tier material) {
        super(damage, movement, material, new Properties().durability(0));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        if (this == WeaponMeleeItems.METEORITE_SWORD.get()) {
            tooltip.add(Component.literal("Forged from a fallen star").withStyle(ChatFormatting.ITALIC));
            tooltip.add(Component.literal("Sharper than most terrestrial blades").withStyle(ChatFormatting.ITALIC));
        }

        if (this == WeaponMeleeItems.METEORITE_SWORD_SEARED.get()) {
            tooltip.add(Component.literal("Fire strengthens the blade").withStyle(ChatFormatting.ITALIC));
            tooltip.add(Component.literal("Making it even more powerful").withStyle(ChatFormatting.ITALIC));
        }

        if (this == WeaponMeleeItems.METEORITE_SWORD_REFORGED.get()) {
            tooltip.add(Component.literal("The sword has been reforged").withStyle(ChatFormatting.ITALIC));
            tooltip.add(Component.literal("To rectify past imperfections").withStyle(ChatFormatting.ITALIC));
        }

        if (this == WeaponMeleeItems.METEORITE_SWORD_HARDENED.get()) {
            tooltip.add(Component.literal("Extremely high pressure has been used").withStyle(ChatFormatting.ITALIC));
            tooltip.add(Component.literal("To harden the blade further").withStyle(ChatFormatting.ITALIC));
        }

        if (this == WeaponMeleeItems.METEORITE_SWORD_ALLOYED.get()) {
            tooltip.add(Component.literal("Cobalt fills the fissures").withStyle(ChatFormatting.ITALIC));
            tooltip.add(Component.literal("Strengthening the sword").withStyle(ChatFormatting.ITALIC));
        }

        if (this == WeaponMeleeItems.METEORITE_SWORD_MACHINED.get()) {
            tooltip.add(Component.literal("Advanced machinery was used").withStyle(ChatFormatting.ITALIC));
            tooltip.add(Component.literal("To refine the blade even more").withStyle(ChatFormatting.ITALIC));
        }

        if (this == WeaponMeleeItems.METEORITE_SWORD_TREATED.get()) {
            tooltip.add(Component.literal("Chemicals have been applied").withStyle(ChatFormatting.ITALIC));
            tooltip.add(Component.literal("Making the sword more powerful").withStyle(ChatFormatting.ITALIC));
        }

        if (this == WeaponMeleeItems.METEORITE_SWORD_ETCHED.get()) {
            tooltip.add(Component.literal("Acids clean the material").withStyle(ChatFormatting.ITALIC));
            tooltip.add(Component.literal("To make this the perfect sword").withStyle(ChatFormatting.ITALIC));
        }

        if (this == WeaponMeleeItems.METEORITE_SWORD_BRED.get()) {
            tooltip.add(Component.literal("Immense heat and radiation").withStyle(ChatFormatting.ITALIC));
            tooltip.add(Component.literal("Compress the material").withStyle(ChatFormatting.ITALIC));
        }

        if (this == WeaponMeleeItems.METEORITE_SWORD_IRRADIATED.get()) {
            tooltip.add(Component.literal("The power of the Atom").withStyle(ChatFormatting.ITALIC));
            tooltip.add(Component.literal("Gives the sword might").withStyle(ChatFormatting.ITALIC));
        }

        if (this == WeaponMeleeItems.METEORITE_SWORD_FUSED.get()) {
            tooltip.add(Component.literal("This blade has met").withStyle(ChatFormatting.ITALIC));
            tooltip.add(Component.literal("With the forces of the stars").withStyle(ChatFormatting.ITALIC));
        }

        if (this == WeaponMeleeItems.METEORITE_SWORD_BALEFUL.get()) {
            tooltip.add(Component.literal("This sword has met temperatures").withStyle(ChatFormatting.ITALIC));
            tooltip.add(Component.literal("Far beyond what normal material can endure").withStyle(ChatFormatting.ITALIC));
        }
    }
}
